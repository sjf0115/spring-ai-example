package com.example.mcp;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.time.Duration;
import java.util.Set;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：SSE（Server-Sent Events）方式接入远程 MCP 服务器示例
 * 场景：演示 MCP 的「有状态」远程传输方式 —— SSE。
 *      与 StdIO 由 AgentScope 拉起本地子进程不同，SSE/HTTP 属于「连接一个已经在运行的远程服务」。
 *      这里连接免鉴权的 GitMCP（gitmcp.io），它把任意公开 GitHub 仓库的文档/代码检索能力
 *      暴露为 MCP 工具（fetch_*_documentation / search_*_documentation / search_*_code / fetch_url_content），
 *      我们把这些工具统一注册到 AgentScope 的 Toolkit，由 ReActAgent 调用完成任务。
 *
 * 为什么不用 DeepWiki 演示 SSE：
 *   DeepWiki 的 /sse 端点已被服务端废弃（返回 HTTP 410 "SSE transport is deprecated"），
 *   这是行业趋势（Context7 等的 /sse 也在陆续下线），降低客户端库版本无法解决对端返回的 410。
 *   GitMCP 目前仍在仓库基础 URL 上提供 SSE 服务，适合演示 SSE 传输。
 *
 * 传输方式对比：
 *   - SSE             ：有状态（stateful），客户端先建立一条长连接订阅服务端事件（本示例，GitMCP）
 *   - Streamable HTTP ：无状态（stateless），每次请求独立（见 HttpTransportMcpExample，DeepWiki /mcp）
 *
 * GitMCP 的 SSE 端点即仓库基础 URL：https://gitmcp.io/{owner}/{repo}（无需 /sse 后缀）
 *
 * 前置条件：
 *   - 网络可访问 https://gitmcp.io
 *   - 设置环境变量 DASHSCOPE_API_KEY
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/26 11:00
 */
@Deprecated
public class SseTransportMcpExample {

    public static void main(String[] args) {
        // 1. 通过 SSE 连接远程 GitMCP 服务器
        //    注意：这里不启动任何本地子进程，只是连接一个已经在线的远程 URL
        //    GitMCP 的 SSE 端点就是仓库基础 URL（无需 /sse 后缀）
        McpClientWrapper gitmcpClient = McpClientBuilder.create("gitmcp-sse")
                .sseTransport("https://gitmcp.io/spring-projects/spring-ai")
                .timeout(Duration.ofSeconds(120))                 // 请求超时
                .initializationTimeout(Duration.ofSeconds(30))    // 初始化超时
                .buildAsync()
                .block();

        try {
            // 2. 把 MCP server 暴露的所有工具一次性注册到 Toolkit
            //    registerMcpClient 内部会调用 listTools 并把每个工具适配为 AgentTool
            Toolkit toolkit = new Toolkit();
            toolkit.registerMcpClient(gitmcpClient).block();

            Set<String> toolNames = toolkit.getToolNames();
            System.out.println("可用工具: " + toolNames);

            // 3. 创建 Agent，绑定 toolkit
            ReActAgent agent = ReActAgent.builder()
                    .name("代码库问答助手")
                    .sysPrompt("你是一个代码库问答助手，可以借助 GitMCP 工具查询公开 GitHub 仓库的文档与代码，"
                            + "并把最终结果用一句话清晰地告知用户。")
                    .model(DashScopeChatModel.builder()
                            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                            .modelName(MODEL_NAME)
                            .build())
                    .toolkit(toolkit)
                    .build();

            // 4. 调用智能体：向 GitMCP 提问某公开仓库的特性（预计触发 search_*_documentation 工具调用）
            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent("请查询 GitHub 仓库 spring-projects/spring-ai，"
                            + "用一句话告诉我它的 ChatClient 是用来做什么的。")
                    .build();

            Msg response = agent.call(msg).block();
            if (response != null) {
                System.out.println("助手: " + response.getTextContent());
            }
        } finally {
            // 5. 关闭 MCP 客户端，释放 SSE 长连接
            if (gitmcpClient != null) {
                gitmcpClient.close();
            }
        }
    }
}
