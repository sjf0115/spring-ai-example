package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;

import static com.example.utils.AgentUtil.startChat;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/4/19 17:18
 */
public class ToolCallingExample {

    public static void main(String[] args) throws Exception {
        // 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SimpleTools());

        System.out.println("Registered tools:");
        System.out.println("  - get_current_time: Get current time in a timezone");
        System.out.println("  - calculate: Evaluate simple math expressions");

        // 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                        .name("ToolAgent")
                        .sysPrompt("You are a helpful assistant with access to tools. "
                                + "Use tools when needed to answer questions accurately. "
                                + "Always explain what you're doing when using tools.")
                        .model(DashScopeChatModel.builder()
                                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                                .modelName("qwen3-max-2026-01-23")
                                .stream(true)
                                .enableThinking(false)
                                .formatter(new DashScopeChatFormatter())
                                .build())
                        .toolkit(toolkit)
                        .memory(new InMemoryMemory())
                        .build();
        // 开始交互
        startChat(agent);
    }
}
