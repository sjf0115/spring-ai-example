package com.example.skills.create;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：完整的 Agent Skill 示例（Builder 版）——从「用 Builder 链式 API 构建技能」
 *      到「注册进 SkillBox → 构建 ReActAgent → 运行真实任务」的端到端闭环。
 *      既演示 name/description/metadata/skillContent/resources/source 的设置与读取，
 *      也演示技能如何被 Agent 真正激活使用。
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 15:26
 */
public class AgentSkillBuilderExample {

    public static void main(String[] args) {
        // 1. 通过 Builder 链式构建一个技能
        //    - putMetadata：写入自定义元数据（键值对）
        //    - addResource：为技能附加参考资源（相对路径 -> 文本内容）
        AgentSkill skill = AgentSkill.builder()
                .name("data_analysis")
                .description("Use when analyzing data, computing statistics, or generating reports")
                .putMetadata("homepage", "https://example.com/docs")
                .putMetadata("version", "1.0.0")
                .skillContent("# 数据分析技能\n\n本技能用于对表格数据做描述性统计并生成简要报告。\n\n## 工作流程\n1. 读取数据\n2. 计算均值 / 方差 / 标准差\n3. 总结趋势与异常值")
                .addResource("references/formulas.md", "# 常用公式\nmean = Σx / n")
                .addResource("examples/sample.csv", "name,value\nA,100\nB,200")
                .source("custom")
                .build();

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
