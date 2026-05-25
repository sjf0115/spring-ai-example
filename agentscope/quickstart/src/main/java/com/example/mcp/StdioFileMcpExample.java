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

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：StdIO 方式接入 MCP 文件系统服务器示例
 * 场景：通过 stdio 启动官方 @modelcontextprotocol/server-filesystem，
 *      把它暴露的文件操作工具（read_file / write_file / list_directory 等）
 *      统一注册到 AgentScope 的 Toolkit，由 ReActAgent 调用完成任务。
 *
 * 前置条件：
 *   - 已安装 Node.js / npx
 *   - 网络可达 npm registry（首次会自动下载 @modelcontextprotocol/server-filesystem）
 *   - 设置环境变量 DASHSCOPE_API_KEY
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/26 07:15
 */
public class StdioFileMcpExample {

    public static void main(String[] args) throws IOException {
        // 1. 准备沙箱工作目录，MCP filesystem server 只能在该目录内读写
        Path workspace = Paths.get("agentscope/quickstart/target/mcp-fs-workspace")
                .toAbsolutePath().normalize();
        Files.createDirectories(workspace);
        String baseDir = workspace.toString();

        // 2. 通过 stdio 启动 MCP filesystem server
        //    npx 会拉起一个子进程，AgentScope 通过标准输入输出与其通信
        McpClientWrapper fsClient = McpClientBuilder.create("fs-mcp")
                .stdioTransport("npx", "-y", "@modelcontextprotocol/server-filesystem", baseDir)
                .buildAsync()
                .block();

        try {
            // 3. 把 MCP server 暴露的所有工具一次性注册到 Toolkit
            //    registerMcpClient 内部会调用 listTools 并把每个工具适配为 AgentTool
            Toolkit toolkit = new Toolkit();
            toolkit.registerMcpClient(fsClient).block();

            // 4. 创建 Agent，绑定 toolkit
            ReActAgent agent = ReActAgent.builder()
                    .name("文件助手")
                    .sysPrompt("你是一个文件操作助手，并把最终结果用一句话告知用户。")
                    .model(DashScopeChatModel.builder()
                            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                            .modelName(MODEL_NAME)
                            .build())
                    .toolkit(toolkit)
                    .build();

            // 5. 调用智能体：组合任务（预计触发 write → list → read 三次 MCP 工具调用）
            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent("把《Hello MCP，来自 AgentScope》写入 mcp-demo.txt，"
                            + "再列出当前目录下的所有文件，最后读取 mcp-demo.txt 的内容返回给我。")
                    .build();

            Msg response = agent.call(msg).block();
            if (response != null) {
                System.out.println("助手: " + response.getTextContent());
            }
        } finally {
            // 6. 关闭 MCP 客户端，停止 npx 子进程
            if (fsClient != null) {
                fsClient.close();
            }
        }
    }
}
