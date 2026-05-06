package com.example.hitl;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：Human-in-the-Loop 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 22:59
 */
public class HumanInTheLoopExample {
    public static void main(String[] args) {
        // 暂停 Hook
        Hook stopHook = new Hook() {
            // 定义需要审批的敏感工具名单
            private static final List<String> SENSITIVE_TOOLS = List.of("delete_file", "send_email");

            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                if (event instanceof PostReasoningEvent postReasoning) {
                    // 1. 推理后暂停
                    Msg reasoningMsg = postReasoning.getReasoningMessage();
                    // 调用的工具
                    List<ToolUseBlock> toolCalls = reasoningMsg.getContentBlocks(ToolUseBlock.class);
                    // 如果包含敏感工具，暂停等待确认
                    boolean hasSensitive = toolCalls.stream()
                            .anyMatch(t -> SENSITIVE_TOOLS.contains(t.getName()));
                    if (hasSensitive) {
                        // 暂停
                        postReasoning.stopAgent();
                    }
                } else if (event instanceof PostActingEvent postActing) {
                    // 2. 行动后暂停
                }
                return Mono.just(event);
            }
        };

        // 4. 创建对话模型: 使用百练对话模型
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 5. 创建智能体
        ReActAgent agent = ReActAgent.builder()
                .name("RAGAssistant")
                .sysPrompt(
                        "You are a helpful assistant with access to a knowledge retrieval"
                                + " tool. When you need information from the knowledge base,"
                                + " use the retrieve_knowledge tool. Always explain what you're"
                                + " doing.")
                .model(chatModel)
                .memory(new InMemoryMemory())
                .hook(stopHook)
                .build();


        // 6. 交互
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("AgentScope Java 的核心范式是什么")
                        .build()
        ).block();

        // 检查是否有待确认的工具调用
        while (response.hasContentBlocks(ToolUseBlock.class)) {
            // 展示待执行的工具
            List<ToolUseBlock> pending = response.getContentBlocks(ToolUseBlock.class);
            for (ToolUseBlock tool : pending) {
                System.out.println("工具: " + tool.getName());
                System.out.println("参数: " + tool.getInput());
            }

            if (userConfirms()) {
                // 用户确认，继续执行
                response = agent.call().block();
            } else {
                // 用户拒绝，返回取消信息
                Msg cancelResult = Msg.builder()
                        .role(MsgRole.TOOL)
                        .content(pending.stream()
                                .map(t -> ToolResultBlock.of(t.getId(), t.getName(),
                                        TextBlock.builder().text("操作已取消").build()))
                                .toArray(ToolResultBlock[]::new))
                        .build();
                response = agent.call(cancelResult).block();
            }
        }

        // 最终响应
        System.out.println(response.getTextContent());
    }
}
