package com.example.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import reactor.core.publisher.Mono;

/**
 * 功能：MonitoringHook 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/8 12:18
 */
public class MonitoringHookExample {

    private static class MonitoringHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PreCallEvent preCall) {
                // 1. PreCallEvent
                System.out.println("\n[HOOK] PreCallEvent - Agent started: " + preCall.getAgent().getName());
            } else if (event instanceof ReasoningChunkEvent reasoningChunk) {
                // 2. ReasoningChunkEvent
                Msg chunk = reasoningChunk.getIncrementalChunk();
                String text = null; //MsgUtils.getTextContent(chunk);
                if (text != null && !text.isEmpty()) {
                    System.out.print(text);
                }
            } else if (event instanceof PreActingEvent preActing) {
                // 3. PreActingEvent
                System.out.println(
                        "\n[HOOK] PreActingEvent - Tool: "
                                + preActing.getToolUse().getName()
                                + ", Input: "
                                + preActing.getToolUse().getInput());
            } else if (event instanceof ActingChunkEvent actingChunk) {
                // 4. ActingChunkEvent
                ToolResultBlock chunk = actingChunk.getChunk();
                String output =
                        chunk.getOutput().isEmpty() ? "" : chunk.getOutput().get(0).toString();
                System.out.println(
                        "[HOOK] ActingChunkEvent - Tool: "
                                + actingChunk.getToolUse().getName()
                                + ", Progress: "
                                + output);
            } else if (event instanceof PostActingEvent postActing) {
                // 5. PostActingEvent
                ToolResultBlock result = postActing.getToolResult();
                String output =
                        result.getOutput().isEmpty() ? "" : result.getOutput().get(0).toString();
                System.out.println(
                        "[HOOK] PostActingEvent - Tool: "
                                + postActing.getToolUse().getName()
                                + ", Result: "
                                + output);
            } else if (event instanceof PostCallEvent) {
                // 6. PostCallEvent
                System.out.println("[HOOK] PostCallEvent - Agent execution finished\n");
            }
            return Mono.just(event);
        }
    }
}
