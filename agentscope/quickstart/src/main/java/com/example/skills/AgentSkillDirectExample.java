package com.example.skills;

import io.agentscope.core.skill.AgentSkill;

/**
 * 功能：直接构造 Skill
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 15:28
 */
public class AgentSkillDirectExample {
    public static void main(String[] args) {
        AgentSkill skill = new AgentSkill(
                "data_analysis",                    // name
                "Use when analyzing data...",       // description
                "# Data Analysis\n...",             // skillContent
                null                            // resources (可为 null)
        );
    }
}
