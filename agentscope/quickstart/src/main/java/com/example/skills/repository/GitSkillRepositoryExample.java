package com.example.skills.repository;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.skill.repository.GitSkillRepository;
import io.agentscope.core.tool.Toolkit;

import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

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
        // 1. 构建 GitSkillRepository
        GitSkillRepository repo = new GitSkillRepository(repoUrl);
        AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
        System.out.println("========== GitSkillRepository (auto-sync) ==========");
        System.out.println("type     : " + info.getType());
        System.out.println("location : " + info.getLocation());
        System.out.println("writable : " + info.isWritable());
        System.out.println("source   : " + repo.getSource());

        // 2. 所有技能
        List<String> names = repo.getAllSkillNames();
        System.out.println("技能名列表 : " + names);
        for (AgentSkill skill : repo.getAllSkills()) {
            System.out.println("---------- skill: " + skill.getName() + " ----------");
            System.out.println("description  : " + skill.getDescription());
            System.out.println("resourcePaths: " + skill.getResourcePaths());
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

    // 2. 手动同步模式：构造时关闭自动同步（第二个参数 false）
    public static void manualSyncLoadSkill() {
        // 1. 构建 GitSkillRepository
        GitSkillRepository repo = new GitSkillRepository(repoUrl, false);
        System.out.println("========== GitSkillRepository (manual-sync) ==========");
        AgentSkillRepositoryInfo info = repo.getRepositoryInfo();
        System.out.println("type     : " + info.getType());
        System.out.println("location : " + info.getLocation());
        System.out.println("writable : " + info.isWritable());
        System.out.println("source   : " + repo.getSource());

        // 手动模式下读取前必须先 sync()：clone 到本地临时目录并定位技能根，否则 getAllSkillNames/getSkill 会抛 IllegalStateException。
        repo.sync();

        // 2. 所有技能
        List<String> names = repo.getAllSkillNames();
        System.out.println("技能名列表 : " + names);
        for (AgentSkill skill : repo.getAllSkills()) {
            System.out.println("---------- skill: " + skill.getName() + " ----------");
            System.out.println("description  : " + skill.getDescription());
            System.out.println("resourcePaths: " + skill.getResourcePaths());
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
        // autoSyncLoadSkill();
        manualSyncLoadSkill();
    }
}

