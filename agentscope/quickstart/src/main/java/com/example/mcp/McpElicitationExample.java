package com.example.mcp;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：MCP Elicitation 交互式补充示例
 * 场景：MCP 协议规定了「Elicitation」能力 —— 服务端在执行某个工具时，可以反向向客户端发起请求，
 *      要求用户补充额外信息（如：缺少的参数、敏感操作的确认、动态字段等）。
 *      客户端收到 ElicitRequest 后，需要弹出对话/控制台与最终用户交互，
 *      把用户回答打包成 ElicitResult 返回给服务端，服务端再据此继续工具执行。
 *
 * AgentScope 在 McpClientBuilder 上提供了两种注册方式：
 *   - asyncElicitation(Function<ElicitRequest, Mono<ElicitResult>>)  异步处理
 *   - syncElicitation (Function<ElicitRequest, ElicitResult>)         同步处理
 *
 * ElicitResult.Action 三种状态：
 *   - ACCEPT  ：用户已填写并同意，content 携带回答字段
 *   - DECLINE ：用户拒绝补充信息（不一定是取消，可能是不愿透露）
 *   - CANCEL  ：用户取消整个工具执行流程
 *
 * 提示：本示例连接的 @modelcontextprotocol/server-filesystem 本身并不会主动发起 elicitation，
 *      所以 handler 在普通运行流程下不会被触发。代码完整展示了「客户端如何注册并响应 elicitation」，
 *      未来对接到支持 elicitation 的 server（如某些表单类 / 工单类 MCP 服务），可直接复用本模式。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/26 10:00
 */
public class McpElicitationExample {

    /** 共享一个 Scanner，避免被多次 close */
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        // 1. 沙箱目录
        Path workspace = Paths.get("agentscope/quickstart/target/mcp-fs-workspace")
                .toAbsolutePath().normalize();
        Files.createDirectories(workspace);
        String baseDir = workspace.toString();

        // 2. 启动 MCP server（这里仍用 filesystem server 演示连接 + handler 注册）
        //    使用 asyncElicitation 注册「服务端 → 客户端」的反向请求处理函数
        McpClientWrapper fsClient = McpClientBuilder.create("fs-mcp")
                .stdioTransport("npx", "-y", "@modelcontextprotocol/server-filesystem", baseDir)
                .asyncElicitation(McpElicitationExample::handleElicitation)
                .buildAsync()
                .block();

        try {
            // 3. 注册 MCP 工具到 toolkit（全量注册）
            Toolkit toolkit = new Toolkit();
            toolkit.registerMcpClient(fsClient).block();
            System.out.println("可用工具: " + toolkit.getToolNames());

            // 4. 创建 Agent
            ReActAgent agent = ReActAgent.builder()
                    .name("文件助手")
                    .sysPrompt("你是一个文件操作助手，并把最终结果用一句话告知用户。")
                    .model(DashScopeChatModel.builder()
                            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                            .modelName(MODEL_NAME)
                            .build())
                    .toolkit(toolkit)
                    .build();

            // 5. 触发一次会话：若 server 在工具执行中发起 elicitation，
            //    handleElicitation 会自动被回调，与控制台用户交互
            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent("把《Hello Elicitation》写入 elicit-demo.txt，然后读取它的内容返回给我。")
                    .build();

            Msg response = agent.call(msg).block();
            if (response != null) {
                System.out.println("\n助手: " + response.getTextContent());
            }
        } finally {
            if (fsClient != null) {
                fsClient.close();
            }
        }
    }

    /**
     * Elicitation 处理回调：当 server 在工具执行过程中发起反向请求时，会调用本函数。
     * <ul>
     *   <li>request.message()         —— server 给用户看的提示文案，例如 "请补充收件人邮箱"</li>
     *   <li>request.requestedSchema() —— server 期望客户端返回的字段（JSON Schema 形式）</li>
     * </ul>
     * 客户端的职责：根据 schema 与用户交互，收集字段值，按 ACCEPT/DECLINE/CANCEL 三态返回。
     */
    private static Mono<McpSchema.ElicitResult> handleElicitation(McpSchema.ElicitRequest request) {
        return Mono.fromCallable(() -> {
            System.out.println("\n========== [Elicitation] 服务端请求补充信息 ==========");
            System.out.println("提示: " + request.message());
            System.out.println("期望字段 schema: " + request.requestedSchema());
            System.out.println("（输入 'cancel' 可取消整个工具调用，输入 'decline' 表示拒绝补充）");

            // 解析 requestedSchema 中的 properties，逐字段询问用户
            Map<String, Object> answers = new LinkedHashMap<>();
            Map<String, Object> properties = extractProperties(request.requestedSchema());

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String fieldName = entry.getKey();
                System.out.print("请输入 [" + fieldName + "]: ");
                String input = SCANNER.hasNextLine() ? SCANNER.nextLine().trim() : "";

                if ("cancel".equalsIgnoreCase(input)) {
                    System.out.println("[Elicitation] 用户取消");
                    return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.CANCEL, null);
                }
                if ("decline".equalsIgnoreCase(input)) {
                    System.out.println("[Elicitation] 用户拒绝补充");
                    return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.DECLINE, null);
                }
                answers.put(fieldName, input);
            }

            System.out.println("[Elicitation] 用户接受，回填: " + answers);
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, answers);
        });
    }

    /** 从 ElicitRequest 的 requestedSchema 中安全提取 properties；schema 为空时返回空 map */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractProperties(Map<String, Object> schema) {
        if (schema == null) {
            return new HashMap<>();
        }
        Object props = schema.get("properties");
        if (props instanceof Map) {
            return (Map<String, Object>) props;
        }
        return new HashMap<>();
    }
}
