package com.example.hook;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 09:58
 */
public class ThinkingHookExample {
    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .enableThinking(true) // 开启思考模式
                .defaultOptions(GenerateOptions.builder()
                        .thinkingBudget(500)  // 控制思考 token 预算
                        .build())
                .modelName("qwen3.6-plus")
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
