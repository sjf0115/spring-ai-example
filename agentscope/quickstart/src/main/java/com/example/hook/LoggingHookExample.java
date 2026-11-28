package com.example.hook;

import com.example.tool.WeatherService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：日志 Hook
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 12:01
 */
public class LoggingHookExample {
    // 日志 Hook
    public static class LoggingHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PreCallEvent e) {
                System.out.println("[日志] Agent 启动: " + e.getAgent().getName());
            }
            if (event instanceof PostCallEvent e) {
                System.out.println("[日志] Agent 完成");
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
                .hook(new LoggingHook()) // 注册 Hook
                .build();

        // 3. 调用
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("北京明天天气如何")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
