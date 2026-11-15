package com.example.skills.repository;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.mysql.MysqlSkillRepository;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.DriverManager;
import java.util.List;

/**
 * 功能：从 MySQL 加载/保存技能（MysqlSkillRepository）。
 *      演示完整读写回路：建表 -> 保存一个技能 -> 列出全部技能 -> 按名获取 -> 打印。
 * 前置条件：
 *   - classpath 上需有 MySQL 驱动（本模块未内置，请自行添加 mysql-connector-j 依赖）
 *   - 设置环境变量：
 *     export MYSQL_URL=jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=Asia/Shanghai
 *     export MYSQL_USER=root
 *     export MYSQL_PASSWORD=your_password
 *     export MYSQL_DB=agentscope_skills   # 可选，默认 agentscope_skills
 *   说明：为了免依赖具体驱动类，本示例用 JDK 动态代理基于 DriverManager 构造了一个
 *   最简 DataSource；生产环境请改用 HikariCP 等连接池。
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:28
 */
public class MysqlSkillRepositoryExample {
    public static void main(String[] args) {
        // 1. 从环境变量读取数据库连接配置，未配置时给出清晰提示并退出
        String url = System.getenv("MYSQL_URL");
        if (url == null || url.isBlank()) {
            System.err.println("未配置 MYSQL_URL 环境变量，无法连接 MySQL。");
            System.err.println("请先设置（并确保 classpath 上有 MySQL 驱动）：");
            System.err.println("  export MYSQL_URL=jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=Asia/Shanghai");
            System.err.println("  export MYSQL_USER=root");
            System.err.println("  export MYSQL_PASSWORD=your_password");
            return;
        }
        String user = System.getenv("MYSQL_USER");
        String password = System.getenv("MYSQL_PASSWORD");
        String database = System.getenv().getOrDefault("MYSQL_DB", "agentscope_skills");

        // 2. 构造 DataSource（演示用：基于 DriverManager 的动态代理）
        DataSource dataSource = createDataSource(url, user, password);

        // 3. 构建 MysqlSkillRepository（表不存在时自动创建，允许写入）
        try (MysqlSkillRepository repo = MysqlSkillRepository.builder(dataSource)
                .databaseName(database)
                .skillsTableName("agent_skills")
                .resourcesTableName("agent_skill_resources")
                .createIfNotExist(true)   // 表不存在时自动创建
                .writeable(true)          // 允许写入
                .build()) {

            AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
            System.out.println("========== MysqlSkillRepository ==========");
            System.out.println("type     : " + info.getType());
            System.out.println("location : " + info.getLocation());
            System.out.println("writable : " + info.isWritable());
            System.out.println("source   : " + repo.getSource());

            // 4. 写入一个演示技能（writeable=true 时可写）
            AgentSkill demo = AgentSkill.builder()
                    .name("data_analysis")
                    .description("Use when analyzing data, computing statistics, or generating reports")
                    .skillContent("# Data Analysis\n来自 MySQL 仓库的演示技能。")
                    .addResource("references/formulas.md", "# 常用公式\nmean = Σx / n")
                    .source("mysql-demo")
                    .build();
            boolean saved = repo.save(List.of(demo), true); // 第二个参数 true = 覆盖同名技能
            System.out.println("保存演示技能结果: " + saved);

            // 5. 列出全部技能名并逐个打印
            List<String> names = repo.getAllSkillNames();
            System.out.println("技能名列表 : " + names);
            for (AgentSkill skill : repo.getAllSkills()) {
                System.out.println("---------- skill: " + skill.getName() + " ----------");
                System.out.println("description  : " + skill.getDescription());
                System.out.println("source       : " + skill.getSource());
                System.out.println("resourcePaths: " + skill.getResourcePaths());
            }

            // 6. 按名精确获取
            AgentSkill loaded = repo.getSkill("data_analysis");
            System.out.println("按名获取 data_analysis: " + (loaded != null ? loaded.getDescription() : "未找到"));
        } catch (Exception e) {
            System.err.println("MySQL 技能仓库操作失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 用 JDK 动态代理构造一个最简 DataSource，getConnection() 委托给 DriverManager。
     * 仅用于演示，避免编译期依赖具体驱动/连接池；生产环境请用 HikariCP 等。
     */
    private static DataSource createDataSource(String url, String user, String password) {
        return (DataSource) Proxy.newProxyInstance(
                MysqlSkillRepositoryExample.class.getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, methodArgs) -> {
                    switch (method.getName()) {
                        case "getConnection":
                            return (methodArgs == null || methodArgs.length == 0)
                                    ? DriverManager.getConnection(url, user, password)
                                    : DriverManager.getConnection(url, (String) methodArgs[0], (String) methodArgs[1]);
                        case "toString":
                            return "SimpleDataSource(" + url + ")";
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == methodArgs[0];
                        default:
                            throw new UnsupportedOperationException("不支持的方法: " + method.getName());
                    }
                });
    }
}
