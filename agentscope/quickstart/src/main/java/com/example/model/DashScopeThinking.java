package com.example.model;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;

/**
 * 功能：DashScope 思考模式
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 09:18
 */
public class DashScopeThinking {
    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen3.6-plus")
                .stream(false)
                .enableThinking(true) // 开启思考模式
                .defaultOptions(GenerateOptions.builder()
                        .thinkingBudget(500)  // 控制思考 token 预算
                        .build())
                .build();

        // 2. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(model)
                .build();

        // 3. 调用
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("为什么 1+1=2？")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
