package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import io.agentscope.core.tool.Toolkit;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：工具挂起（ToolSuspendException）示例
 * 场景：当工具执行需要"外部干预"才能完成时（如大额审批、人工复核、跨系统异步处理），
 *      工具方法可抛出 ToolSuspendException 主动暂停 Agent 执行：
 *        1. 框架将异常转为带 METADATA_SUSPENDED 标记的 ToolResultBlock（其 TextBlock 内容 = exception.reason）
 *        2. agent.call(...) 返回的 Msg.generateReason = TOOL_SUSPENDED
 *        3. 调用方从 Msg 中取出待执行的 ToolUseBlock + 挂起说明 ToolResultBlock，外部完成后构造新的 ToolResultBlock
 *        4. 通过 agent.call(toolResultMsg) 恢复 Agent 执行
 *
 * ToolSuspendException(reason) 中 reason 的作用：
 *   - 框架会将 reason 字符串包装为 TextBlock 放入挂起态 ToolResultBlock.output
 *   - 调用方可以通过 response.getContentBlocks(ToolResultBlock.class) 取回，
 *     作为“为什么被挂起”的业务说明，驱动 UI 提示、风控告警、审批工单等
 *
 * 与 HITL（PreActingEvent + Hook 拦截）的区别：
 *   - Hook 拦截：在工具执行"前"由外部决定是否放行，工具内部并不知情
 *   - ToolSuspendException：由工具方法"内部"根据业务规则主动决定是否挂起，更贴合"金额阈值/风控规则"
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 17:00
 */
public class ToolSuspendExample {
    /**
     * 审批工具：金额超阈值时抛出 ToolSuspendException 挂起，等待外部人工确认
     */
    public static class ApprovalTool {@Tool(name = "submit_approval", description = "提交审批申请，金额单位元")
        public ToolResultBlock submitApproval(@ToolParam(name = "amount", description = "审批金额") double amount) {
            if (amount > 10000) {
                // 大额审批 → 挂起，由外部人工确认后恢复执行
                throw new ToolSuspendException("大额审批需要人工确认，金额: " + amount);
            }
            // 小额审批 → 工具内部直接通过
            return ToolResultBlock.text("审批已通过，金额: " + amount);
        }
    }

    public static void main(String[] args) {
        // 1. 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ApprovalTool());

        // 2. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("审批助手")
                .sysPrompt("你是一个审批助手。当用户提出审批申请时，使用 submit_approval 工具，"
                        + "并将用户给出的金额作为参数。如果工具返回审批结果，请用一句话告知用户。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .build())
                .toolkit(toolkit)
                .build();

        // 3. 用户发起一笔大额审批（> 10000，将触发挂起）
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("帮我提交一笔 50000 元的报销审批")
                .build();

        Msg response = agent.call(userMsg).block();
        Scanner scanner = new Scanner(System.in);

        // 4. 检查是否被挂起（GenerateReason.TOOL_SUSPENDED）
        while (response != null && response.getGenerateReason() == GenerateReason.TOOL_SUSPENDED) {

            System.out.println("====== Agent 执行被挂起，等待外部确认 ======");

            // 4.1 取出本轮所有待执行的工具调用
            List<ToolUseBlock> pendingTools = response.getContentBlocks(ToolUseBlock.class);

            // 取出挂起态的 ToolResultBlock（包含 ToolSuspendException 传入的 reason），以 toolUseId 建索引
            Map<String, String> suspendReasons = response.getContentBlocks(ToolResultBlock.class).stream()
                    .filter(ToolResultBlock::isSuspended)
                    .collect(Collectors.toMap(
                            ToolResultBlock::getId,
                            r -> r.getOutput().stream()
                                    .filter(b -> b instanceof TextBlock)
                                    .map(b -> ((TextBlock) b).getText())
                                    .collect(Collectors.joining("\n"))));

            for (ToolUseBlock toolUse : pendingTools) {
                /*System.out.println("待审批工具: " + toolUse.getName());
                System.out.println("调用参数  : " + toolUse.getInput());*/
                // 展示挂起原因（= ToolSuspendException(reason) 中的文本）
                System.out.println("助手: " + suspendReasons.getOrDefault(toolUse.getId(), ""));

                // 4.2 在外部完成实际执行（这里模拟人工确认）
                System.out.print("助手：是否人工通过？(y/n): ");
                String decision = scanner.nextLine().trim().toLowerCase();
                String externalResult = "y".equals(decision) ? "人工审批已通过" : "人工审批被拒绝";

                // 4.3 构造工具结果消息，注入到 Agent
                Msg toolResult = Msg.builder()
                        .role(MsgRole.TOOL)
                        .content(ToolResultBlock.builder()
                                .id(toolUse.getId())
                                .name(toolUse.getName())
                                .output(List.of(TextBlock.builder()
                                        .text(externalResult)
                                        .build()))
                                .build())
                        .build();

                // 4.4 恢复 Agent 执行（继续 ReAct 循环，让模型基于工具结果作答）
                response = agent.call(toolResult).block();
            }
        }

        // 5. 最终回答
        if (response != null) {
            System.out.println("助手: " + response.getTextContent());
        }
    }
}
