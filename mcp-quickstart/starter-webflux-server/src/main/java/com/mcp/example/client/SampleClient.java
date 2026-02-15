package com.mcp.example.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * 功能：SampleClient
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/15 14:32
 */
public class SampleClient {
    private final McpClientTransport transport;

    public SampleClient(McpClientTransport transport) {
        this.transport = transport;
    }

    public void run() {

        var client = McpClient.sync(this.transport).build();

        client.initialize();
        client.ping();

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
