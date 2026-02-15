package com.mcp.example.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.WebClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 功能：StreamableHttpClient
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/15 14:36
 */
public class StreamableHttpClient {
    public static void main(String[] args) {

        var transport = WebClientStreamableHttpTransport.builder(WebClient.builder().baseUrl("http://localhost:8000"))
                .build();

        var client = McpClient.sync(transport).build();

        client.initialize();

        client.ping();

        // List and demonstrate tools
        McpSchema.ListToolsResult toolsList = client.listTools();
        System.out.println("Available Tools = " + toolsList);

        McpSchema.CallToolResult weatherForcastResult = client.callTool(
                new McpSchema.CallToolRequest("getWeatherByCity",
                        Map.of("cityName", "北京"))
        );
        System.out.println("Weather Forcast: " + weatherForcastResult);

        client.closeGracefully();
    }
}
