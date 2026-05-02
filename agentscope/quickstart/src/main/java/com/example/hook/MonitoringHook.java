package com.example.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.memory.autocontext.MsgUtils;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 功能：监控 Hook
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:23
 */
public class MonitoringHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreCallEvent preCall) {
            // 1. Agent 调用前
            System.out.println("[HOOK] PreCallEvent - Agent call started: " + preCall.getAgent().getName());
        } else if (event instanceof PreReasoningEvent preReasoning) {
            // 2. LLM 推理前
            System.out.println("[HOOK] PreReasoningEvent - Agent reasoning started: " + preReasoning.getAgent().getName());
        } else if (event instanceof ReasoningChunkEvent reasoningChunk) {
            // 3. LL 推理流式期间
            Msg chunk = reasoningChunk.getIncrementalChunk();
            String text = chunk.getTextContent();
            if (text != null && !text.isEmpty()) {
                System.out.println("[HOOK] ReasoningChunkEvent - Agent reasoning chunk: " + text);
            }
        } else if (event instanceof PostReasoningEvent postReasoning) {
            // 4. LLM 推理完成后
            System.out.println("[HOOK] PostReasoningEvent - Agent reasoning finished: " + postReasoning.getAgent().getName());
        } else if (event instanceof PreActingEvent preActing) {
            // 5. 工具执行前
            String toolName = preActing.getToolUse().getName();
            Map<String, Object> input = preActing.getToolUse().getInput();
            System.out.println("[HOOK] PreActingEvent - Tool acting started: " + toolName + ", Input: " + input);
        } else if (event instanceof ActingChunkEvent actingChunk) {
            // 6. 工具流式期间
            ToolResultBlock chunk = actingChunk.getChunk();
            String toolName = actingChunk.getToolUse().getName();
            String output = chunk.getOutput().isEmpty() ? "" : chunk.getOutput().get(0).toString();
            System.out.println("[HOOK] ActingChunkEvent - Tool acting chunk: " + toolName + ", Progress: " + output);
        } else if (event instanceof PostActingEvent postActing) {
            // 7. 工具执行完成后
            ToolResultBlock result = postActing.getToolResult();
            String toolName = postActing.getToolUse().getName();
            String output = result.getOutput().isEmpty() ? "" : result.getOutput().get(0).toString();
            System.out.println("[HOOK] PostActingEvent - Tool acting finished: " + toolName + ", Result: " + output);
        } else if (event instanceof PostCallEvent postCall) {
            // 8. PostCallEvent
            System.out.println("[HOOK] PostCallEvent - Agent call finished: " + postCall.getAgent().getName());
        }
        return Mono.just(event);
    }
}
