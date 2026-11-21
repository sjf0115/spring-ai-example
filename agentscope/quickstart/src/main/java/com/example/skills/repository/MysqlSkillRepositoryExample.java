package com.example.skills.repository;

import com.zaxxer.hikari.HikariDataSource;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.skill.repository.mysql.MysqlSkillRepository;
import io.agentscope.core.tool.Toolkit;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.DriverManager;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：从 MySQL 加载/保存技能（MysqlSkillRepository）
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:28
 */
public class MysqlSkillRepositoryExample {

    public static void simpleExample() {
        // 1. 构造 DataSource
        HikariDataSource ds = new HikariDataSource();
        // 默认创建到 agentscope 数据库下，自定义数据库无用
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/agentscope");
        ds.setUsername("root");
        ds.setPassword("root");

        // 2. 构建 MysqlSkillRepository
        // 第二个参数 createIfNotExist = true，会自动创建数据库和表;
        MysqlSkillRepository repo = new MysqlSkillRepository(ds, true, true);
        AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
        System.out.println("========== ClasspathSkillRepository ==========");
        System.out.println("type     : " + info.getType());
        System.out.println("location : " + info.getLocation());
        System.out.println("writable : " + info.isWritable());   // classpath 仓库通常只读
        System.out.println("source   : " + repo.getSource());

        List<String> names = repo.getAllSkillNames();
        System.out.println("技能名列表 : " + names);
        if (names.isEmpty()) {
            System.out.println("（未加载到任何技能，请确认数据库下存在技能");
            return;
        }

        // 3. 获取技能
        AgentSkill skill = repo.getSkill("data-analysis");
        if (skill != null) {
            System.out.println("精确获取 data-analysis 成功，描述: " + skill.getDescription());
        } else {
            System.out.println("未找到名为 data-analysis 的技能");
        }

        // 4. 把技能挂载进 SkillBox（SkillBox 会把技能作为可激活能力接入 Toolkit）
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registerSkill(skill);

        // 5. 构建 ReActAgent：必须同时传 toolkit 和 skillBox，技能才会被真正激活。
        ReActAgent agent = ReActAgent.builder()
                .name("Friday")
                .sysPrompt("You are a helpful assistant called Friday. 当任务匹配已注册技能时，请激活该技能并按其说明执行。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .endpointType(EndpointType.MULTIMODAL)
                        .build())
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();

        // 6. 运行一个与 data_analysis 技能匹配的真实任务
        String task = "请使用 data_analysis 技能分析这组数据: 12, 15, 20, 18, 22，"
                + "计算均值、中位数、标准差，并给出简要结论。";
        System.out.println("\n用户: " + task);

        Msg response = agent.call(
                Msg.builder().role(MsgRole.USER).textContent(task).build()
        ).block();

        System.out.println("\n========== Agent 响应 ==========");
        System.out.println(response != null ? response.getTextContent() : "(无响应)");
    }

    public static void builderExample() {
        // 1. 构造 DataSource
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        ds.setUsername("root");
        ds.setPassword("root");

        // 2. 构建 MysqlSkillRepository
        // 自定义数据库与表名
        MysqlSkillRepository repo = MysqlSkillRepository.builder(ds)
                .databaseName("test")
                .skillsTableName("skills")
                .resourcesTableName("skill_resources")
                .createIfNotExist(true)   // 表不存在时自动创建
                .writeable(true)          // 允许写入
                .build();

        AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
        System.out.println("========== ClasspathSkillRepository ==========");
        System.out.println("type     : " + info.getType());
        System.out.println("location : " + info.getLocation());
        System.out.println("writable : " + info.isWritable());   // classpath 仓库通常只读
        System.out.println("source   : " + repo.getSource());

        List<String> names = repo.getAllSkillNames();
        System.out.println("技能名列表 : " + names);
        if (names.isEmpty()) {
            System.out.println("（未加载到任何技能，请确认数据库下存在技能");
            return;
        }

        // 3. 获取技能
        AgentSkill skill = repo.getSkill("data-analysis");
        if (skill != null) {
            System.out.println("精确获取 data-analysis 成功，描述: " + skill.getDescription());
        } else {
            System.out.println("未找到名为 data-analysis 的技能");
        }

        // 4. 把技能挂载进 SkillBox（SkillBox 会把技能作为可激活能力接入 Toolkit）
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registerSkill(skill);

        // 5. 构建 ReActAgent：必须同时传 toolkit 和 skillBox，技能才会被真正激活。
        ReActAgent agent = ReActAgent.builder()
                .name("Friday")
                .sysPrompt("You are a helpful assistant called Friday. 当任务匹配已注册技能时，请激活该技能并按其说明执行。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .endpointType(EndpointType.MULTIMODAL)
                        .build())
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();

        // 6. 运行一个与 data_analysis 技能匹配的真实任务
        String task = "请使用 data_analysis 技能分析这组数据: 12, 15, 20, 18, 22，"
                + "计算均值、中位数、标准差，并给出简要结论。";
        System.out.println("\n用户: " + task);

        Msg response = agent.call(
                Msg.builder().role(MsgRole.USER).textContent(task).build()
        ).block();

        System.out.println("\n========== Agent 响应 ==========");
        System.out.println(response != null ? response.getTextContent() : "(无响应)");
    }

    public static void main(String[] args) {
//        simpleExample();
        builderExample();
    }
}


//INSERT INTO skills (name, description, skill_content, source)
//VALUES (
//    'data-analysis',
//            'Use this skill when analyzing tabular data, computing descriptive statistics, or generating simple data reports. It provides common formulas and a CSV reading workflow.',
//            '# 数据分析技能
//
//    本技能用于对表格数据做描述性统计并生成简要报告。
//
//            ## 工作流程
//            1. 读取数据
//            2. 计算均值 / 方差 / 标准差
//            3. 总结趋势与异常值
//
//            ## 参考资源
//            - `references/formulas.md`:常用统计公式速查',
//            'local'
//);
//
//
//INSERT INTO skill_resources (id, resource_path, resource_content)
//VALUES (
//        LAST_INSERT_ID(),
//    'references/formulas.md',
//            '# 常用统计公式速查
//
//            ## 均值 (Mean)
//```
//mean = (x1 + x2 + ... + xn) / n
//```
//
//        ## 方差 (Variance)
//```
//variance = Σ(xi - mean)^2 / n
//```
//
//        ## 标准差 (Standard Deviation)
//```
//std = sqrt(variance)
//```
//
//        ## 中位数 (Median)
//- 奇数个:排序后取中间值
//- 偶数个:排序后取中间两个值的平均
//
//## 分位数 (Percentile)
//```
//P_k = 排序后第 (k/100 * n) 个位置的值
//```'
//        );