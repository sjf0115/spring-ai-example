package com.example.skills.repository;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.GitSkillRepository;

import java.util.List;

/**
 * 功能：从 Git 仓库加载技能（GitSkillRepository）。
 *      GitSkillRepository 会 clone/拉取远端仓库，并按「每个技能一个子目录 + SKILL.md」的约定解析。
 *      演示两种同步模式：
 *        - 自动同步（默认）：每次读取时检查远端更新
 *        - 手动同步：构造时关闭自动同步，需要时调用 sync() 刷新
 * 前置条件：
 *   - 设置环境变量 GIT_SKILLS_REPO 指向一个真实的技能仓库，例如：
 *     export GIT_SKILLS_REPO=https://github.com/your-org/your-skills-repo.git
 *   - 网络可访问该仓库（私有仓库需提前配置好 git 凭据）
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:28
 */
public class GitSkillRepositoryExample {
    public static void main(String[] args) {
        // 1. 从环境变量读取仓库地址，未配置时给出清晰提示并退出（避免 clone 占位地址报错）
        String repoUrl = System.getenv("GIT_SKILLS_REPO");
        if (repoUrl == null || repoUrl.isBlank()) {
            System.err.println("未配置 GIT_SKILLS_REPO 环境变量。");
            System.err.println("请先设置一个真实的技能仓库地址后重试，例如：");
            System.err.println("  export GIT_SKILLS_REPO=https://github.com/your-org/your-skills-repo.git");
            return;
        }

        // 2. 自动同步模式（默认）：每次读取时检查远端更新
        try (GitSkillRepository repo = new GitSkillRepository(repoUrl)) {
            AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
            System.out.println("========== GitSkillRepository (auto-sync) ==========");
            System.out.println("type     : " + info.getType());
            System.out.println("location : " + info.getLocation());
            System.out.println("writable : " + info.isWritable());
            System.out.println("source   : " + repo.getSource());

            List<String> names = repo.getAllSkillNames();
            System.out.println("技能名列表 : " + names);

            for (AgentSkill skill : repo.getAllSkills()) {
                System.out.println("---------- skill: " + skill.getName() + " ----------");
                System.out.println("description  : " + skill.getDescription());
                System.out.println("resourcePaths: " + skill.getResourcePaths());
            }

            AgentSkill one = repo.getSkill(names.isEmpty() ? "data-analysis" : names.get(0));
            if (one != null) {
                System.out.println("精确获取技能成功: " + one.getName());
            }
        } catch (Exception e) {
            System.err.println("自动同步模式加载失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. 手动同步模式：构造时关闭自动同步（第二个参数 false），需要时再调用 sync() 刷新
        try (GitSkillRepository manualRepo = new GitSkillRepository(repoUrl, false)) {
            System.out.println("========== GitSkillRepository (manual-sync) ==========");
            System.out.println("手动刷新前技能名: " + manualRepo.getAllSkillNames());

            manualRepo.sync();   // 主动从远端拉取最新
            System.out.println("手动 sync() 后技能名: " + manualRepo.getAllSkillNames());
        } catch (Exception e) {
            System.err.println("手动同步模式加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

