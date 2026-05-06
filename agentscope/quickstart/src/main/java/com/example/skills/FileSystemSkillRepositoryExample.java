package com.example.skills;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.coding.ShellCommandTool;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：FileSystemSkillRepository 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 16:03
 */
public class FileSystemSkillRepositoryExample {

    private static final String RESOURCES_DIR = "spring-ai-example/agentscope/quickstart/src/main/resources/skills";
    private static final String OUTPUT_DIR = "spring-ai-example/agentscope/quickstart/target/skill-output";

    public static void main(String[] args) {


        Path outputDir = Paths.get(OUTPUT_DIR).toAbsolutePath().normalize();

        // 1. 加载 Skill
        Path resourcesDir = Paths.get(RESOURCES_DIR).toAbsolutePath().normalize();
        FileSystemSkillRepository repository = new FileSystemSkillRepository(resourcesDir, false);
        AgentSkill skillCreator = repository.getSkill("skill-creator");

        // 2. 注册 Skill
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registration().skill(skillCreator).apply();

        // 代码执行能力
        Scanner scanner = new Scanner(System.in);
        ShellCommandTool shellCommandTool = new ShellCommandTool(
                        Set.of("python", "ls", "cat"),
                        cmd -> {
                            System.out.println("Enter y/n to approve or deny execution:");
                            System.out.println(cmd);
                            System.out.println();
                            String response = scanner.nextLine();
                            return response.trim().equalsIgnoreCase("y");
                        });

        skillBox.codeExecution()
                .workDir(outputDir.toString())
                .withShell(shellCommandTool)
                .withRead()
                .withWrite()
                .enable();

        // 4. 创建对话模型: 使用百练对话模型
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                        .name("SkillCreator")
                        .sysPrompt(buildSystemPrompt(outputDir))
                        .model(chatModel)
                        .toolkit(toolkit)
                        .skillBox(skillBox)
                        .memory(new InMemoryMemory())
                        .build();

        // 6. 交互
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("")
                        .build()
        ).block();

        System.out.println(response.getTextContent());

        scanner.close();
    }

    private static String buildSystemPrompt(Path outputDir) {
        return """
        You are a skill creation assistant. Use the skill-creator skill when asked to create or
        update a skill. Prefer concise SKILL.md content and put detailed guidance in references.

        File tools are available. Write new skills under this output directory:
        %s

        Use write_text_file to create files and include a valid YAML frontmatter with name and
        description.
        """.formatted(outputDir.toString());
    }
}
