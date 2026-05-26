package com.example.mcp;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：MCP 工具过滤示例（白名单 + 黑名单）
 * 场景：MCP filesystem server 默认会暴露 read_file / write_file / list_directory /
 *      create_directory / move_file / edit_file / search_files / get_file_info /
 *      list_allowed_directories 等十多个工具。一次性把它们全部塞给 LLM 会带来两个问题：
 *        1) tool schema 占用大量上下文，影响理解和成本；
 *        2) 危险工具（move_file / edit_file / write_file 等）容易被 LLM 误用。
 *      因此推荐做法是 —— 在注册时同时使用「白名单 + 黑名单」精准过滤工具。
 *
 * 实现方式：使用 Toolkit#registration() 提供的链式 DSL
 *   toolkit.registration()
 *          .mcpClient(mcpClient)
 *          .enableTools(enableTools)    // 白名单：只注册这些工具（不在白名单的 server 工具直接被忽略）
 *          .disableTools(disableTools)  // 黑名单：在白名单基础上再剔除黑名单中的工具
 *          .apply();
 * 过滤优先级：disableTools 优先级高于 enableTools，即「同时出现在两个列表的工具最终被禁用」。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/26 09:00
 */
public class McpToolFilterExample {

    public static void main(String[] args) throws IOException {
        // 1. 准备沙箱目录
        Path workspace = Paths.get("agentscope/quickstart/target/mcp-fs-workspace")
                .toAbsolutePath().normalize();
        Files.createDirectories(workspace);
        String baseDir = workspace.toString();

        // 2. 启动 MCP filesystem server（stdio）
        McpClientWrapper fsClient = McpClientBuilder.create("fs-mcp")
                .stdioTransport("npx", "-y", "@modelcontextprotocol/server-filesystem", baseDir)
                .buildAsync()
                .block();

        try {
            // 白名单与黑名单结合
            //    enableTools  ：声明哪些 MCP 工具会被注册（不在列表中的工具直接被忽略）
            //    disableTools ：在白名单的基础上再屏蔽（最终对 LLM 不可见）
            //    delete_file 同时出现在白/黑名单 → 最终仍被禁用（黑名单优先级更高）
            //    list_allowed_directories "自我发现"机制，给 LLM 用来回答"我能在哪些目录里操作"的，解决了相对路径问题
            List<String> enableTools = List.of("read_file", "write_file", "list_directory", "list_allowed_directories", "delete_file");
            List<String> disableTools = List.of("delete_file");

            // 4. 使用 Toolkit#registration() DSL 一次性完成「注册 MCP 客户端 + 白/黑名单过滤」
            Toolkit toolkit = new Toolkit();
            toolkit.registration()
                    .mcpClient(fsClient)
                    .enableTools(enableTools)
                    .disableTools(disableTools)
                    .apply();

            // 5. 打印最终对 LLM 可见的工具集，验证过滤效果：
            //    预期 → [read_file, write_file, list_directory]，delete_file 被剔除
            System.out.println("===== 过滤后对 LLM 可见的工具 =====");
            toolkit.getToolNames().forEach(name -> System.out.println("  - " + name));

            // 6. 创建 Agent，绑定过滤后的 toolkit
            ReActAgent agent = ReActAgent.builder()
                    .name("文件助手")
                    .sysPrompt("你是一个文件操作助手，并把最终结果用一句话告知用户。")
                    .model(DashScopeChatModel.builder()
                            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                            .modelName(MODEL_NAME)
                            .build())
                    .toolkit(toolkit)
                    .build();

            // 7. 调用智能体：组合任务（预计触发 write_file → list_directory → read_file）
            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent("把《Hello MCP，来自 AgentScope》写入 mcp-demo.txt，"
                            + "再列出当前目录下的所有文件，最后读取 mcp-demo.txt 的内容返回给我。")
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
}
