package com.example.hook;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 功能：Hook 优先级示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 12:18
 */
public class PriorityHookExample {
    // ToolApprovalHook
    private static class ToolApprovalHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            // 此钩子在优先级 > 10 的钩子之前执行
            if (event instanceof PostCallEvent postCall) {
                System.out.println("ToolApprovalHook: " + postCall.getAgent().getName());
            }
            return Mono.just(event);
        }

        @Override
        public int priority() {
            return 10;  // 数字越小优先级越高（默认为 100）
        }
    }

    // PromptEnhancingHook
    private static class PromptEnhancingHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            // 此钩子在优先级 > 50 的钩子之前执行
            if (event instanceof PostCallEvent postCall) {
                System.out.println("PromptEnhancingHook: " + postCall.getAgent().getName());
            }
            return Mono.just(event);
        }

        @Override
        public int priority() {
            return 50;  // 数字越小优先级越高（默认为 100）
        }
    }

    // MonitoringHook
    private static class MonitoringHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            // 此钩子在优先级 > 100 的钩子之前执行
            if (event instanceof PostCallEvent postCall) {
                System.out.println("MonitoringHook: " + postCall.getAgent().getName());
            }
            return Mono.just(event);
        }

        @Override
        public int priority() {
            return 100;  // 数字越小优先级越高（默认为 100）
        }
    }

    // ErrorHandlingHook
    private static class ErrorHandlingHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            // 此钩子在优先级 > 100 的钩子之前执行
            if (event instanceof PostCallEvent postCall) {
                System.out.println("ErrorHandlingHook: " + postCall.getAgent().getName());
            }
            return Mono.just(event);
        }

        @Override
        public int priority() {
            return 100;  // 数字越小优先级越高（默认为 100）
        }
    }

    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen3.5-35b-a3b")
                .build();

        // 2. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(model)
                // 配置 Hook
                .hooks(List.of(
                        new ToolApprovalHook(),
                        new PromptEnhancingHook(),
                        new MonitoringHook(),
                        new ErrorHandlingHook()))
                .build();

        // 3. 调用
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("用三句话介绍一下 AgentScope Java")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}


