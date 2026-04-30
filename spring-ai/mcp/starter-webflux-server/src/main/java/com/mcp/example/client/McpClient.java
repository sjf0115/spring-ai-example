package com.mcp.example.client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 功能：ClientSse
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/15 14:31
 */
public class McpClient {
    public static void main(String[] args) {
        // 1. 创建 SSE 传输层，指向 MCP Server
        WebFluxSseClientTransport transport = new WebFluxSseClientTransport(
                WebClient.builder().baseUrl("http://localhost:8888"),
                McpJsonMapper.createDefault()
        );
        // 2. 创建同步客户端
        McpSyncClient client = io.modelcontextprotocol.client.McpClient.sync(transport).build();

        // 3. 初始化连接
        client.initialize();
        client.ping();

        // 4. 列出 Server 暴露的所有工具
        McpSchema.ListToolsResult tools = client.listTools();
        System.out.println("可用工具: " + tools);

        // 5. 调用 get_weather 工具
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(
                        "get_weather",           // Tool 名称
                        Map.of("cityName", "北京")  // 参数
                )
        );
        System.out.println("天气结果: " + result.content());

        // 6. 关闭连接
        client.closeGracefully();
    }
}
