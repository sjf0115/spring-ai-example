package com.example.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能：PromptEnhancingHook
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:24
 */
public class PromptEnhancingHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent e) {
            // 获取当前输入消息列表
            List<Msg> messages = new ArrayList<>(e.getInputMessages());

            // 在消息列表开头注入额外的系统提示
            messages.add(0, Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .content(List.of(TextBlock.builder()
                            .text("请逐步思考，先分析再回答。")
                            .build()))
                    .build());

            // 修改事件中的消息列表
            e.setInputMessages(messages);
        }
        return Mono.just(event);
    }
}
