package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
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
@RequestMapping("/api/ollama")
public class ChatController {
    @Autowired
    private OllamaChatModel ollamaChatModel;

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "你是谁") String message) {
        // String response = this.ollamaChatModel.call(message);
        Prompt prompt = new Prompt(new UserMessage(message));
        String response = ollamaChatModel.call(prompt).getResult().getOutput().getText();
        log.info("response : {}", response);
        return response;
    }

    @GetMapping(value = "/chat/stream", produces = "text/plain;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam(value = "message", defaultValue = "你是谁") String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        // 使用 stream 方法返回 Flux<ChatResponse>
        return ollamaChatModel.stream(prompt)
                .map(response -> response.getResult().getOutput().getText());
    }
}
