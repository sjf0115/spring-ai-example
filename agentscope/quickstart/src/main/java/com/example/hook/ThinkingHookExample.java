package com.example.hook;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.hook.*;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.model.GenerateOptions;
import reactor.core.publisher.Mono;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 09:58
 */
public class ThinkingHookExample {
    public static class ThinkingHook implements Hook {
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

    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .enableThinking(true) // 开启思考模式
                .defaultOptions(GenerateOptions.builder()
                        .thinkingBudget(500)  // 控制思考 token 预算
                        .build())
                .modelName(MODEL_NAME)
                .endpointType(EndpointType.MULTIMODAL)
                .build();

        // 2. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(model)
                .sysPrompt("你是一位资深的国内旅游推荐官")
                .hook(new ThinkingHook())
                .build();

        // 3. 消息
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("十一假期带6岁娃国内游推荐一个最适合的地方")
                .build();

        // 4. 流式输出
        // 配置流式选项：同时接收推理事件和最终结果
        StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                .incremental(true)        // 增量输出，像打字机效果
                .includeReasoningResult(true)  // 包含推理结果
                .build();

        agent.stream(msg, streamOptions)
                .doOnNext(event -> {
                    Msg chunk = event.getMessage();
                    for (ContentBlock block : chunk.getContent()) {
                        if (block instanceof ThinkingBlock t) {
                            System.out.print(t.getThinking());  // 实时打印思考内容
                        } else if (block instanceof TextBlock text) {
                            System.out.print(text.getText());   // 实时打印回答内容
                        }
                    }
                })
                .doOnComplete(() -> System.out.println("\n\n✅ 完成"))
                .blockLast();
    }
}
