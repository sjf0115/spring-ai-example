package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;

/**
 * 功能：天气工具调用示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 15:45
 */
public class WeatherToolExample {
    public static void main(String[] args) throws Exception {
        // 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherService());

        // 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("天气助手")
                .sysPrompt("你是一个天气助手，当询问跟天气有关的问题时，需要调用天气工具来回答用户问题")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName("qwen3-max-2026-01-23")
                        .build())
                .toolkit(toolkit) // 绑定工具集
                .build();

        // 调用智能体
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("北京明天的天气如何")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
