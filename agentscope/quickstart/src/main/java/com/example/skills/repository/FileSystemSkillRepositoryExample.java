package com.example.skills.repository;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.tool.Toolkit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：核心演示 FileSystemSkillRepository（文件系统技能仓库）的读取能力——
 *      以本地目录为后端，加载已有的 AgentSkill。
 *
 * <p>目录约定：baseDir 下每个技能是一个子目录，内含带 YAML frontmatter 的 SKILL.md，可附 references/ 等资源：
 * <pre>
 *   src/main/resources/skills/
 *   ├── data-analysis/
 *   │   ├── SKILL.md
 *   │   └── references/formulas.md
 *   └── skill-creator/SKILL.md
 * </pre>
 *
 * <p>本示例聚焦仓库读取 API，不涉及 Agent 运行，因此无需 DASHSCOPE_API_KEY 即可直接运行：
 * getRepositoryInfo / getSource / isWriteable / getAllSkillNames / skillExists / getSkill。
 *
 * <p>注意：FileSystemSkillRepository 构造时要求 baseDir <b>必须已存在且为目录</b>，否则抛 IllegalArgumentException。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:03
 */
public class FileSystemSkillRepositoryExample {

    /** 技能根目录：直接指定即可 */
    private static final Path SKILLS_DIR = Paths.get("agentscope/quickstart/src/main/resources/skills");

    public static void main(String[] args) {
        try (FileSystemSkillRepository repo = new FileSystemSkillRepository(SKILLS_DIR, false)) {
            System.out.println("仓库信息 : " + repo.getRepositoryInfo());
            System.out.println("source   : " + repo.getSource());
            System.out.println("可写     : " + repo.isWriteable());

            List<String> names = repo.getAllSkillNames();
            System.out.println("技能清单 : " + names);
            for (String name : names) {
                System.out.println("  - skillExists(" + name + ") = " + repo.skillExists(name));
            }

            // 取出 data-analysis 技能
            AgentSkill skill = repo.getSkill("data-analysis");
            if (skill == null) {
                System.out.println("⚠️ 未找到 data-analysis 技能，请确认 " + SKILLS_DIR + "/data-analysis/SKILL.md 是否存在。");
                return;
            }
            System.out.println("--- getSkill(\"data-analysis\") ---");
            System.out.println("  name       : " + skill.getName());
            System.out.println("  description: " + skill.getDescription());
            System.out.println("  skillId    : " + skill.getSkillId());
            System.out.println("  source     : " + skill.getSource());
            System.out.println("  资源路径   : " + skill.getResourcePaths());

            // 2. 把技能挂载进 SkillBox（SkillBox 会把技能作为可激活能力接入 Toolkit）
            Toolkit toolkit = new Toolkit();
            SkillBox skillBox = new SkillBox(toolkit);
            skillBox.registerSkill(skill);

            // 3. 构建 ReActAgent：必须同时传 toolkit 和 skillBox，技能才会被真正激活。
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

            // 4. 运行一个与 data_analysis 技能匹配的真实任务
            String task = "请使用 data_analysis 技能分析这组数据: 12, 15, 20, 18, 22，"
                    + "计算均值、中位数、标准差，并给出简要结论。";
            System.out.println("\n用户: " + task);

            Msg response = agent.call(
                    Msg.builder().role(MsgRole.USER).textContent(task).build()
            ).block();

            System.out.println("\n========== Agent 响应 ==========");
            System.out.println(response != null ? response.getTextContent() : "(无响应)");
        }
    }
}
