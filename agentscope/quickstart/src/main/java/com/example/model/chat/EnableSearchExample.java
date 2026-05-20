package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/21 07:33
 */
public class EnableSearchExample {
    public static void main(String[] args) {
        // 模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .enableSearch(true) // 启用联网搜索，获取实时信息
                .build();

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("你是一个可以联网搜索的助手，能够回答最新的信息。")
                .model(model)
                .build();

        // 调用智能体
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好，请问一下截止到2026年5月21号马来西亚羽毛球大师赛最新结果，简单几句话说一下结果")
                .build();
        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
