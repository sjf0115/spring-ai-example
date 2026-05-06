package com.example.skills;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.util.SkillUtil;

import java.util.Map;

/**
 * 功能：从 Markdown 创建 Skill
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 15:27
 */
public class AgentSkillMarkdownExample {
    public static void main(String[] args) {
        String skillMd = """
            ---
            name: data_analysis
            description: Use this skill when analyzing data, calculating statistics, or generating reports
            ---
            # 技能名称
            Content...
            """;

        Map<String, String> resources = Map.of(
                "references/formulas.md", "# 常用公式\n...",
                "examples/sample.csv", "name,value\nA,100\nB,200"
        );

        AgentSkill skill = SkillUtil.createFrom(skillMd, resources);
    }
}
