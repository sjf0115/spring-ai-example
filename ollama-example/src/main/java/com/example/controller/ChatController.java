package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能：ChatController
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/2/21 20:00
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private OllamaChatModel ollamaChatModel;

    @GetMapping("/ollama")
    public String chat(@RequestParam(value = "message", defaultValue = "你是谁") String message) {
        String response = this.ollamaChatModel.call(message);
        log.info("response : {}", response);
        return response;
    }
}
