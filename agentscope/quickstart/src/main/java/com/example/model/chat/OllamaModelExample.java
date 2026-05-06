package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.ollama.OllamaOptions;

/**
 * 功能：与 Ollama 自托管模型集成示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/7 23:14
 */
public class OllamaModelExample {
    public static void main(String[] args) {
        // 生成参数
        OllamaOptions options = OllamaOptions.builder()
                .numCtx(4096)           // 上下文窗口大小
                .temperature(0.7)       // 生成随机性
                .topK(40)               // Top-K 采样
                .topP(0.9)              // 核采样
                .repeatPenalty(1.1)     // 重复惩罚
                .build();

        // Ollama 自托管千问模型
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("qwen2.5:7b")
                .baseUrl("http://localhost:11434")  // 默认值
                .defaultOptions(options)
                .build();

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(model)
                .build();

        // 调用智能体
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好，请介绍一下自己")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
