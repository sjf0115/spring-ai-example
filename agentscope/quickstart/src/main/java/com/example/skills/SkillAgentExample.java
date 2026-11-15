package com.example.skills;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：集成到 ReActAgent：SkillBox
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 15:29
 */
public class SkillAgentExample {
    public static void main(String[] args) {
        // 1. 创建 Toolkit 和 SkillBox
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);

        // 或者 SkillBox skillBox = new SkillBox(new Toolkit());

        // 2. 创建 Skill
        AgentSkill skill = new AgentSkill(
                "data_analysis",                    // name
                "Use when analyzing data...",       // description
                "# Data Analysis\n...",             // skillContent
                null                            // resources (可为 null)
        );

        // 3. 注册 Skill
        skillBox.registerSkill(skill);

        // 4. 创建 ChatModel
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                // qwen3.8 属多模态系列，但 AUTO 启发式不识别 qwen3.8 会误判为纯文本，
                // 导致 DashScope 返回 "url error, please check url"，故显式指定 MULTIMODAL 端点。
                .endpointType(EndpointType.MULTIMODAL)
                .build();

        // 5. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("MultiSkillAgent")
                .model(chatModel)
                .toolkit(toolkit)
                .skillBox(skillBox)  // 自动注册 skill 加载工具和 hook
                .memory(new InMemoryMemory())
                .build();

        // 6. 交互
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("AgentScope Java 的核心范式是什么")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
