package com.example.hook;

import com.example.tool.WeatherService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：PromptEnhancingHook
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:24
 */
public class PromptEnhancingHookExample {
    // 动态修改输入消息
    public static class PromptEnhancingHook implements Hook {
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

    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName(MODEL_NAME)
                .endpointType(EndpointType.MULTIMODAL)
                .build();

        // 2. 创建 Agent
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherService());

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(model)
                .toolkit(toolkit) // 注册天气查询工具
                .hook(new PromptEnhancingHook()) // 注册 Hook
                .build();

        // 3. 调用
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("北京今天天气如何")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
