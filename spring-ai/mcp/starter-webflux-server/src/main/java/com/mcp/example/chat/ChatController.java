package com.mcp.example.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * 功能：ChatController
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/21 20:00
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/")
public class ChatController {
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private MethodToolCallbackProvider toolCallbackProvider;

    @GetMapping(value = "/chat", produces = "text/plain;charset=UTF-8")
    public Flux<String> askWeather(
            @RequestParam(value = "message", defaultValue = "你是谁") String message) {

        // 从 MethodToolCallbackProvider 获取所有已注册的 MCP Tool
        List<ToolCallback> tools = Arrays.asList(toolCallbackProvider.getToolCallbacks());
        for (ToolCallback toolCallback : tools) {
            log.info("注册工具：{}", toolCallback.getToolDefinition().name());
        }

        Flux<String> result = chatClient
                .prompt()
                .user(message)
                .toolCallbacks(tools.toArray(new ToolCallback[0]))  // ToolCallback 用 toolCallbacks() 方法
                .stream()
                .content();
        log.info("chat: {}", message);
        return result;
    }
}
