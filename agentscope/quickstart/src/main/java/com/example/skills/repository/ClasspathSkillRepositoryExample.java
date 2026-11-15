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
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;

import java.io.IOException;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

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

            // 3. 按名精确获取单个技能
            AgentSkill skill = repository.getSkill("data-analysis");
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
            //    显式 MULTIMODAL 端点：qwen3.8 属多模态系列，AUTO 启发式不识别会误判为纯文本导致 url error。
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

            System.out.println("\n已注册技能ID : " + skillBox.getAllSkillIds());

            // 6. 运行一个与 data_analysis 技能匹配的真实任务
            String task = "请使用 data_analysis 技能分析这组数据: 12, 15, 20, 18, 22，"
                    + "计算均值、中位数、标准差，并给出简要结论。";
            System.out.println("\n用户: " + task);

            Msg response = agent.call(
                    Msg.builder().role(MsgRole.USER).textContent(task).build()
            ).block();

            System.out.println("\n========== Agent 响应 ==========");
            System.out.println(response != null ? response.getTextContent() : "(无响应)");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
