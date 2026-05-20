package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;

/**
 * 功能：DashScope 快速入门
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 09:18
 */
public class DashScopeQuickStart {
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
                .build();

        // 3. 调用
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好，请用三句话介绍一下 AgentScope Java")
                .build();
        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
