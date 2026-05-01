package com.mcp.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 功能：ChatController
 * 说明：通过 MCP Client 协议远程调用天气 MCP Server 的工具
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/")
public class ChatController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @GetMapping(value = "/chat", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(
            @RequestParam(value = "message", defaultValue = "你是谁") String message) {

        Flux<String> result = chatClient
                .prompt()
                .user(message)
                .toolCallbacks(toolCallbackProvider)
                .stream()
                .content();

        log.info("chat: {}", message);
        return result;
    }
}
