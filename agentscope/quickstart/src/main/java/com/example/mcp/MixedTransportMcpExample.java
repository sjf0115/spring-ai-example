package com.example.mcp;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：混合综合示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/11/14 14:08
 */
public class MixedTransportMcpExample {

    public static void main(String[] args) throws IOException {
        // 1. 准备沙箱工作目录，filesystem server 只能在该目录内读写
        Path workspace = Paths.get("agentscope/quickstart/target/mcp-fs-workspace")
                .toAbsolutePath().normalize();
        Files.createDirectories(workspace);
        String baseDir = workspace.toString();

        // 2. 本地 StdIO：拉起 filesystem MCP Server 子进程
        McpClientWrapper fsClient = McpClientBuilder.create("fs-mcp")
                .stdioTransport("npx", "-y", "@modelcontextprotocol/server-filesystem", baseDir)
                .timeout(Duration.ofSeconds(120))                  // 单次工具调用超时
                .initializationTimeout(Duration.ofSeconds(30))     // 连接 + 握手超时
                .buildAsync()
                .block();

        // 3. 远程 Streamable HTTP：连接已在线的 DeepWiki MCP Server（不启动任何子进程）
        McpClientWrapper wikiClient = McpClientBuilder.create("deepwiki-http")
                .streamableHttpTransport("https://mcp.deepwiki.com/mcp")
                .timeout(Duration.ofSeconds(120))
                .initializationTimeout(Duration.ofSeconds(30))
                .buildAsync()
                .block();

        try {
            // 4. 创建两个工具组，把不同来源的工具隔离开
            Toolkit toolkit = new Toolkit();
            toolkit.createToolGroup("filesystem", "本地沙箱目录的文件读写工具", true);
            toolkit.createToolGroup("wiki", "公开仓库 Wiki 问答工具", true);

            // 5. filesystem：白名单收敛，只保留读写/建目录/列目录/允许目录查询，
            //    edit_file、move_file 等危险工具不注册，LLM 根本看不到
            toolkit.registration()
                    .mcpClient(fsClient)
                    .enableTools(List.of("read_file", "write_file", "create_directory",
                            "list_directory", "list_allowed_directories"))
                    .group("filesystem")
                    .apply();

            // 6. DeepWiki：全量注册（其工具本身都是只读问答类，无危险操作）
            toolkit.registration()
                    .mcpClient(wikiClient)
                    .group("wiki")
                    .apply();

            // 7. 打印最终对 LLM 可见的工具集，验证分组与过滤效果
            System.out.println("===== 对 LLM 可见的工具 =====");
            toolkit.getToolNames().forEach(name -> System.out.println("  - " + name));

            // 8. 创建 Agent，绑定聚合了两个 Server 工具的 toolkit
            ReActAgent agent = ReActAgent.builder()
                    .name("研究助手")
                    .sysPrompt("你是一个研究助手：可以用 ask_question 等工具查询公开 GitHub 仓库文档，"
                            + "用 read_file/write_file 等工具在沙箱目录读写文件，"
                            + "完成任务后用一句话告知用户结果。")
                    .model(DashScopeChatModel.builder()
                            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                            .modelName(MODEL_NAME)
                            .endpointType(EndpointType.MULTIMODAL)
                            .build())
                    .toolkit(toolkit)
                    .maxIters(10)
                    .build();

            // 9. 跨 Server 任务：wiki 检索 → 写入文件 → 读回验证
            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent("请先查询 GitHub 仓库 spring-projects/spring-ai 中 ChatClient 是用来做什么的，"
                            + "然后把结论写入 notes/spring-ai-chatclient.md，最后读回该文件内容验证。")
                    .build();

            Msg response = agent.call(msg).block();
            if (response != null) {
                System.out.println("\n助手: " + response.getTextContent());
            }
        } finally {
            // 10. 关闭两个 MCP 客户端：停止 npx 子进程、释放 HTTP 连接
            if (fsClient != null) {
                fsClient.close();
            }
            if (wikiClient != null) {
                wikiClient.close();
            }
        }
    }
}
