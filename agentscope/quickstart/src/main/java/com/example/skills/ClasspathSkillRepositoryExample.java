package com.example.skills;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;

import java.util.List;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:24
 */
public class ClasspathSkillRepositoryExample {
    public static void main(String[] args) {
        try (ClasspathSkillRepository repository = new ClasspathSkillRepository("skills")) {
            AgentSkill skill = repository.getSkill("data-analysis");
            List<AgentSkill> allSkills = repository.getAllSkills();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
