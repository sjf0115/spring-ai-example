package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：通过 DashScopeChatModel 来集成通义千问系列模型
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/7 22:02
 */
public class DashScopeModelExample {
    public static void main(String[] args) {
        // 模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .stream(true) // 是否启用流式输出，默认 true
                .enableThinking(true) // 启用思考模式，模型会展示推理过程
                .enableSearch(true) // 启用联网搜索，获取实时信息
                .build();

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("你是一位资深的")
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
