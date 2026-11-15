package com.example.skills.repository;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;

import java.util.List;

/**
 * 功能：从 classpath 加载技能（ClasspathSkillRepository）。
 *      约定：资源目录下每个技能是一个子目录，内含带 YAML frontmatter 的 SKILL.md。
 *      本示例读取 classpath:/skills（对应 src/main/resources/skills），
 *      打印仓库信息、全部技能名，以及每个技能的详细内容。
 * 前置条件：src/main/resources/skills 下已放置样例子目录（如 data-analysis/SKILL.md）。
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:24
 */
public class ClasspathSkillRepositoryExample {
    public static void main(String[] args) {
        // try-with-resources：ClasspathSkillRepository 实现了 AutoCloseable（jar 环境下需关闭 FileSystem）
        try (ClasspathSkillRepository repository = new ClasspathSkillRepository("skills")) {

            // 1. 仓库基本信息
            AgentSkillRepositoryInfo info = repository.getRepositoryInfo();
            System.out.println("========== ClasspathSkillRepository ==========");
            System.out.println("type     : " + info.getType());
            System.out.println("location : " + info.getLocation());
            System.out.println("writable : " + info.isWritable());   // classpath 仓库通常只读
            System.out.println("jarEnv   : " + repository.isJarEnvironment());
            System.out.println("source   : " + repository.getSource());

            // 2. 全部技能名
            List<String> names = repository.getAllSkillNames();
            System.out.println("技能名列表 : " + names);

            if (names.isEmpty()) {
                System.out.println("（未加载到任何技能，请确认 classpath:/skills 下存在 <技能目录>/SKILL.md）");
                return;
            }

            // 3. 遍历打印每个技能的详细内容
            List<AgentSkill> allSkills = repository.getAllSkills();
            for (AgentSkill skill : allSkills) {
                printSkill(skill);
            }

            // 4. 按名精确获取单个技能
            AgentSkill dataAnalysis = repository.getSkill("data-analysis");
            if (dataAnalysis != null) {
                System.out.println("精确获取 data-analysis 成功，描述: " + dataAnalysis.getDescription());
            } else {
                System.out.println("未找到名为 data-analysis 的技能");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printSkill(AgentSkill skill) {
        System.out.println("---------- skill: " + skill.getName() + " ----------");
        System.out.println("skillId      : " + skill.getSkillId());
        System.out.println("description  : " + skill.getDescription());
        System.out.println("source       : " + skill.getSource());
        System.out.println("resourcePaths: " + skill.getResourcePaths());
        System.out.println("----- skillContent -----");
        System.out.println(skill.getSkillContent());
    }
}
