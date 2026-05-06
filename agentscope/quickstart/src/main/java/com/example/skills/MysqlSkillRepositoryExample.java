package com.example.skills;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.mysql.MysqlSkillRepository;

import javax.sql.DataSource;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:28
 */
public class MysqlSkillRepositoryExample {
    public static void main(String[] args) {
        // 使用简单构造函数（使用默认数据库/表名）
        DataSource dataSource = null;  // 你的数据源

        MysqlSkillRepository repo = MysqlSkillRepository.builder(dataSource)
                .databaseName("my_database")
                .skillsTableName("my_skills")
                .resourcesTableName("my_resources")
                .createIfNotExist(true)   // 表不存在时自动创建
                .writeable(true)          // 允许写入
                .build();

        AgentSkill loaded = repo.getSkill("data_analysis");
    }
}
