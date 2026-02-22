package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 功能：ChatController
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/21 20:00
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ChatController {
    @Autowired
    private ChatClient chatClient;

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "id") String conversationId,
                       @RequestParam(value = "message", defaultValue = "你是谁") String message) {
        String response = chatClient
                .prompt()
                // 通过监听器传入会话ID
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
        log.info("response : {}", response);
        return response;
    }

    @GetMapping(value = "/chat/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam(value = "id") String conversationId,
                                   @RequestParam(value = "message", defaultValue = "你是谁") String message) {
        return chatClient
                .prompt()
                // 通过监听器传入会话ID
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .stream()
                .content();
    }
}
