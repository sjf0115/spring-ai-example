package com.example.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ThinkingBlock;
import reactor.core.publisher.Mono;

/**
 * 功能：捕获并展示思考过程
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 14:25
 */
public class ThinkingHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent preReasoning) {
            // LLM 推理前 - 开始思考
            String name = preReasoning.getAgent().getName();
            System.out.println("\n--------------------------------------------");
            System.out.println("Agent [" + name + "] 开始思考 ....");
        } else if (event instanceof PostReasoningEvent postReasoning) {
            // LLM 推理完成后 - 输出思考内容
            String name = postReasoning.getAgent().getName();
            Msg reasoningMsg = postReasoning.getReasoningMessage();
            for (ContentBlock block : reasoningMsg.getContent()) {
                if (block instanceof ThinkingBlock thinking) {
                    System.out.println("\n--------------------------------------------");
                    System.out.println("Agent [" + name + "] 思考\n" + thinking.getThinking());
                }
            }
        } else if (event instanceof PostCallEvent postCall) {
            // Agent 调用完成 - 最终回答
            String name = postCall.getAgent().getName();
            String result = postCall.getFinalMessage().getTextContent();
            System.out.println("\n--------------------------------------------");
            System.out.println("Agent [" + name + "] 最终回答\n" + result);
        }
        return Mono.just(event);
    }
}
