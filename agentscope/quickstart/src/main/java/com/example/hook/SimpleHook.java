package com.example.hook;

import io.agentscope.core.hook.*;
import reactor.core.publisher.Mono;

/**
 * 功能：实现 Hook 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 12:01
 */
public class SimpleHook implements Hook {
    // 统一事件处理入口
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreCallEvent preCall) {
            // Agent 执行前
            System.out.println("Agent 调用前: " + preCall.getAgent().getName());
        } else if (event instanceof PreReasoningEvent preReasoning) {
            // LLM 推理前
            System.out.println("LLM 推理前: " + preReasoning.getAgent().getName());
        } else if (event instanceof PostReasoningEvent postReasoning) {
            // LLM 推理完成后
            System.out.println("LLM 推理完成后: " + postReasoning.getAgent().getName());
        }
        else if (event instanceof PostCallEvent postCall) {
            // Agent 调用完成后
            System.out.println("Agent 调用完成后: " + postCall.getAgent().getName());
        }
        return Mono.just(event);
    }
}
