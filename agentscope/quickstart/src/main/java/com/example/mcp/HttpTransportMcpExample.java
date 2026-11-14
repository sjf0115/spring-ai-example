package com.example.mcp;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.time.Duration;
import java.util.Set;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：Streamable HTTP 方式接入远程 MCP 服务器示例
 * 场景：演示 MCP 的「无状态」远程传输方式 —— Streamable HTTP。
 *      与 StdIO 由 AgentScope 拉起本地子进程不同，SSE/HTTP 属于「连接一个已经在运行的远程服务」。
 *      这里连接免鉴权的 DeepWiki MCP（Devin/Cognition 出品），它把公开 GitHub 仓库的问答能力
 *      暴露为 MCP 工具（read_wiki_structure / read_wiki_contents / ask_question），
 *      我们把这些工具统一注册到 AgentScope 的 Toolkit，由 ReActAgent 调用完成任务。
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/26 11:30
 */
public class HttpTransportMcpExample {

    public static void main(String[] args) {
        // 1. 通过 Streamable HTTP 连接远程 DeepWiki MCP 服务器
        //    注意：这里不启动任何本地子进程，只是连接一个已经在线的远程 URL
        McpClientWrapper deepwikiClient = McpClientBuilder.create("deepwiki-http")
                .streamableHttpTransport("https://mcp.deepwiki.com/mcp")
                .timeout(Duration.ofSeconds(120))                 // 请求超时
                .initializationTimeout(Duration.ofSeconds(30))    // 初始化超时
                .buildAsync()
                .block();

        try {
            // 2. 把 MCP server 暴露的所有工具一次性注册到 Toolkit
            //    registerMcpClient 内部会调用 listTools 并把每个工具适配为 AgentTool
            Toolkit toolkit = new Toolkit();
            toolkit.registerMcpClient(deepwikiClient).block();

            Set<String> toolNames = toolkit.getToolNames();
            System.out.println("可用工具: " + toolNames);

            // 3. 创建 Agent，绑定 toolkit
            ReActAgent agent = ReActAgent.builder()
                    .name("代码库问答助手")
                    .sysPrompt("你是一个代码库问答助手，可以借助 DeepWiki 工具查询公开 GitHub 仓库，"
                            + "并把最终结果用一句话清晰地告知用户。")
                    .model(DashScopeChatModel.builder()
                            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                            .modelName(MODEL_NAME)
                            .endpointType(EndpointType.MULTIMODAL)
                            .build())
                    .toolkit(toolkit)
                    .build();

            // 4. 调用智能体：向 DeepWiki 提问某公开仓库的特性（预计触发 ask_question 工具调用）
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
            // 5. 关闭 MCP 客户端，释放 HTTP 连接
            if (deepwikiClient != null) {
                deepwikiClient.close();
            }
        }
    }
}
