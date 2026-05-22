package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.OllamaChatModel;

/**
 * 功能：OllamaChatModel 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/22 23:20
 */
public class OllamaQuickStart {
    public static void main(String[] args) {
        // 1. 创建模型（无需 API Key！）
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("qwen3:8b")
                .baseUrl("http://localhost:11434")  // 默认值，可省略
                .build();

        // 2. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("LocalAssistant")
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
