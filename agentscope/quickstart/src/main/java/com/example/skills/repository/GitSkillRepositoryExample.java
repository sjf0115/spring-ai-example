package com.example.skills.repository;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.GitSkillRepository;

import java.util.List;

/**
 * 功能：从 Git 仓库加载技能（GitSkillRepository，只读）
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:28
 */
public class GitSkillRepositoryExample {
    private static final String repoUrl = "https://github.com/sjf0115/spring-ai-example.git";

    // 1. 自动同步模式（默认）：每次读取时检查远端更新
    public static void autoSyncLoadSkill() {
        try (GitSkillRepository repo = new GitSkillRepository(repoUrl)) {
            AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
            System.out.println("========== GitSkillRepository (auto-sync) ==========");
            System.out.println("type     : " + info.getType());
            System.out.println("location : " + info.getLocation());
            System.out.println("writable : " + info.isWritable());
            System.out.println("source   : " + repo.getSource());

            // 所有技能
            List<String> names = repo.getAllSkillNames();
            System.out.println("技能名列表 : " + names);
            for (AgentSkill skill : repo.getAllSkills()) {
                System.out.println("---------- skill: " + skill.getName() + " ----------");
                System.out.println("description  : " + skill.getDescription());
                System.out.println("resourcePaths: " + skill.getResourcePaths());
            }

            // 精确获取技能
            AgentSkill skill = repo.getSkill("weekly-report");
            if (skill != null) {
                System.out.println("精确获取技能成功: " + skill.getDescription());
            }
        } catch (Exception e) {
            System.err.println("自动同步模式加载失败: " + e.getMessage());
        }
    }

    // 2. 手动同步模式：构造时关闭自动同步（第二个参数 false）
    public static void manualSyncLoadSkill() {
        try (GitSkillRepository repo = new GitSkillRepository(repoUrl, false)) {
            System.out.println("========== GitSkillRepository (manual-sync) ==========");
            AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
            System.out.println("type     : " + info.getType());
            System.out.println("location : " + info.getLocation());
            System.out.println("writable : " + info.isWritable());
            System.out.println("source   : " + repo.getSource());

            // 手动模式下读取前必须先 sync()：clone 到本地临时目录并定位技能根，否则 getAllSkillNames/getSkill 会抛 IllegalStateException。
            repo.sync();
            List<String> names = repo.getAllSkillNames();
            System.out.println("技能名列表 : " + names);

            for (AgentSkill skill : repo.getAllSkills()) {
                System.out.println("---------- skill: " + skill.getName() + " ----------");
                System.out.println("description  : " + skill.getDescription());
                System.out.println("resourcePaths: " + skill.getResourcePaths());
            }
        } catch (Exception e) {
            System.err.println("手动同步模式加载失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // autoSyncLoadSkill();
        manualSyncLoadSkill();
    }
}

