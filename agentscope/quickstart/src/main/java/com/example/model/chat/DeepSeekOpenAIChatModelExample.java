package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.ToolChoice;

/**
 * 功能：OpenAI 兼容平台 - DeepSeek
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/22 22:15
 */
public class DeepSeekOpenAIChatModelExample {
    public static void main(String[] args) {
        // 1. 模型
        GenerateOptions options = GenerateOptions.builder()
                .temperature(0.7)           // // 随机性 (0.0-2.0)
                .topP(0.9)                  // 核采样(0.0-1.0)
                .topK(40)                   // Top-K 采样
                .maxTokens(2000)            // 最大输出 token 数
                .seed(42L)                  // 随机种子
                .toolChoice(new ToolChoice.Auto())  // 工具选择策略
                .build();

        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .modelName("deepseek-v4-flash")
                .baseUrl("https://api.deepseek.com")
                .generateOptions(options)
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
