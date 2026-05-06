package com.example.skills;

import io.agentscope.core.skill.AgentSkill;

/**
 * 功能：使用 Builder 创建 Skill
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 15:26
 */
public class AgentSkillBuilderExample {
    public static void main(String[] args) {
        AgentSkill skill = AgentSkill.builder()
                .name("data_analysis")
                .description("Use when analyzing data...")
                .putMetadata("homepage", "https://example.com/docs")
                .skillContent("# Data Analysis\n...")
                .addResource("references/formulas.md", "# 常用公式\n...")
                .source("custom")
                .build();
    }
}
