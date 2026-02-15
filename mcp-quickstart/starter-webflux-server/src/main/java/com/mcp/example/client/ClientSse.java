package com.mcp.example.client;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 功能：ClientSse
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/15 14:31
 */
public class ClientSse {
    public static void main(String[] args) {
        var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:8000"), McpJsonMapper.createDefault());
        new SampleClient(transport).run();
    }
}
