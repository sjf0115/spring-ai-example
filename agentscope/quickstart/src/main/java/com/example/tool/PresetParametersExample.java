package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;

import java.util.Map;

/**
 * 功能：注册时预设敏感参数
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 16:01
 */
public class PresetParametersExample {
    public static void main(String[] args) throws Exception {
        // 注册工具
        Toolkit toolkit = new Toolkit();
        // 注册时预设敏感参数
        toolkit.registration()
                .tool(new EmailService())
                .presetParameters(
                        Map.of("send", Map.of("apiKey", System.getenv("EMAIL_API_KEY")))
                )
                .apply();

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
