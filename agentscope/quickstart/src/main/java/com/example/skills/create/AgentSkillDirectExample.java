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

import java.util.Map;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：直接通过构造函数创建 AgentSkill（不使用 Builder），并打印其全部字段。
 *      演示两个重载：4 参（name/description/skillContent/resources）与
 *      5 参（额外指定 source 来源）。
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 15:28
 */
public class AgentSkillDirectExample {
    public static void main(String[] args) {
        // 1. 四参构造：resources 允许为 null
        Map<String, String> resources = Map.of(
                "references/formulas.md", "# 常用公式\nmean = Σx / n",
                "examples/sample.csv", "name,value\nA,100\nB,200"
        );
        AgentSkill skill = new AgentSkill(
                "data_analysis",                    // name
                "Use when analyzing data, computing statistics, or generating reports",       // description
                "# 数据分析技能\n\n本技能用于对表格数据做描述性统计并生成简要报告。\n\n## 工作流程\n1. 读取数据\n2. 计算均值 / 方差 / 标准差\n3. 总结趋势与异常值", // skillContent
                resources,  // resources（可为 null）
                "builtin"   // 可选 source
        );

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
