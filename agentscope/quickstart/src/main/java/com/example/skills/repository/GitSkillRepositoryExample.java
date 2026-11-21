package com.example.skills.repository;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.GitSkillRepository;

import java.util.List;

/**
 * 功能：从 Git 仓库加载技能（GitSkillRepository，只读）。
 *      用 JGit clone/拉取远端仓库到本地临时目录，再按约定解析技能：
 *        - 目录约定：仓库根若存在 skills/ 子目录则以它为技能根，否则以仓库根为技能根；
 *          每个技能是一个子目录，内含带 YAML frontmatter 的 SKILL.md（可附 references/ 等资源）。
 *      演示两种同步模式：
 *        - 自动同步（默认）：每次读取前用 ls-remote 轻量比对远端 HEAD，变化才 pull
 *        - 手动同步（autoSync=false）：读取前必须先调用 sync() 初始化，否则抛 IllegalStateException
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
        String repoUrl = "git@github.com:sjf0115/spring-ai-example.git";

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

        // 3. 手动同步模式：构造时关闭自动同步（第二个参数 false）
        //    注意：手动模式下「读取前必须先 sync()」，否则 getSkill/getAllSkillNames 等会抛
        //    IllegalStateException("Repository is not synchronized. Call sync() before reading skills.")
        try (GitSkillRepository manualRepo = new GitSkillRepository(repoUrl, false)) {
            System.out.println("========== GitSkillRepository (manual-sync) ==========");

            manualRepo.sync();   // 首次 sync()：clone 到本地临时目录并定位技能根
            System.out.println("首次 sync() 后技能名: " + manualRepo.getAllSkillNames());

            manualRepo.sync();   // 之后按需再次刷新：仅当远端 HEAD 变化才真正 pull
            System.out.println("再次 sync() 后技能名: " + manualRepo.getAllSkillNames());
        } catch (Exception e) {
            System.err.println("手动同步模式加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

