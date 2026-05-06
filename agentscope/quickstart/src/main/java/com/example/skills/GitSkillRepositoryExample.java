package com.example.skills;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.GitSkillRepository;

import java.util.List;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:28
 */
public class GitSkillRepositoryExample {
    public static void main(String[] args) {
        // 自动同步（默认）：每次读取时检查远端更新
        AgentSkillRepository repo = new GitSkillRepository(
                "https://github.com/your-org/your-skills-repo.git");

        AgentSkill skill = repo.getSkill("data-analysis");
        List<AgentSkill> allSkills = repo.getAllSkills();

        // 手动同步模式
        GitSkillRepository manualRepo = new GitSkillRepository(
                "https://github.com/your-org/your-skills-repo.git",
                false);  // 关闭自动同步

        // 需要时手动刷新
        manualRepo.sync();
    }
}
