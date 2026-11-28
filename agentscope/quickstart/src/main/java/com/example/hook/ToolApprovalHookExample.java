package com.example.hook;

import com.example.tool.WeatherService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：使用 Hook 完成「工具调用审批（人机协作 / Human-in-the-Loop）」的示例。
 *
 * 场景：Agent 要执行敏感操作（如转账）前，先暂停下来询问管理员是否放行；
 *      管理员批准则正常执行，拒绝则让敏感工具根本不被执行，并把"被拒绝"的反馈回传给模型。
 *
 * 机制说明（以 AgentScope 1.0.12 源码为准）——ReActAgent.acting() 的三步链路：
 *   1) notifyPreActingHooks：逐个工具触发 PreActingEvent，Hook 只能调用 setToolUse(...) 改写/替换本次工具调用；
 *   2) executeToolCalls -> toolkit.callTools(...)：真正执行（可能已被改写的）工具调用；
 *   3) notifyPostActingHook：触发 PostActingEvent，此处才有 setToolResult(...) 覆盖结果、stopAgent() 暂停。
 *
 *   关键结论：PreActingEvent（执行前）阶段既不能注入结果、也不能跳过执行——被它放行的工具调用一定会执行。
 *   因此要"拒绝"敏感工具，正确做法是用 setToolUse 把这次调用【重定向】到一个已登记、无副作用的占位工具
 *   （approval_denied），敏感工具便不会被执行；重定向时保持 id 不变，才能与模型发起的 tool_call 正确配对。
 *
 *   对比：若审批规则由"工具内部业务"决定（如金额阈值/风控），更推荐用 ToolSuspendException 主动挂起
 *   （见 com.example.tool.ToolSuspendExample），由调用方在挂起后决定放行或拒绝再恢复执行。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:26
 */
public class ToolApprovalHookExample {

    /** 需要人工审批的敏感工具名集合 */
    private static final Set<String> SENSITIVE_TOOLS = Set.of("transfer_money", "delete_user");

    /** 拒绝时的重定向目标：一个不产生任何副作用的占位工具名 */
    private static final String DENIED_TOOL = "approval_denied";

    /** 全局复用一个 Scanner，避免每次事件都 new（且不关闭 System.in） */
    private static final Scanner SCANNER = new Scanner(System.in);

    // 1. 敏感工具：转账（需要执行前审批）
    public static class BankService {
        @Tool(name = "transfer_money", description = "向指定账户转账，金额单位元。当用户要求转账/汇款时使用。")
        public String transfer(
                @ToolParam(name = "to", description = "收款人") String to,
                @ToolParam(name = "amount", description = "转账金额") double amount) {
            return "已向 " + to + " 转账 " + amount + " 元";
        }
    }

    // 2. 拒绝占位工具：被重定向到这里时只返回一句拒绝说明，无任何副作用
    public static class ApprovalDeniedTool {
        @Tool(name = "approval_denied", description = "内部占位工具：敏感操作被管理员拒绝时返回拒绝说明，模型不应主动调用。")
        public String denied(@ToolParam(name = "reason", description = "拒绝原因") String reason) {
            return "操作已被管理员拒绝：" + reason;
        }
    }

    // 3. 审批 Hook：在工具执行前（PreActingEvent）拦截敏感工具，交由管理员确认是否放行
    public static class ToolApprovalHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            // 只关心「工具执行前」事件，其它事件原样放行
            if (!(event instanceof PreActingEvent e)) {
                return Mono.just(event);
            }
            String toolName = e.getToolUse().getName();
            if (!SENSITIVE_TOOLS.contains(toolName)) {
                return Mono.just(event); // 非敏感工具直接放行
            }
            // 控制台交互是阻塞 IO，放到 boundedElastic 线程，避免阻塞 Reactor 事件循环
            return Mono.<T>fromRunnable(() -> askApproval(e))
                    .subscribeOn(Schedulers.boundedElastic())
                    .thenReturn(event);
        }

        private void askApproval(PreActingEvent e) {
            ToolUseBlock call = e.getToolUse();
            String toolName = call.getName();
            Map<String, Object> input = call.getInput();

            System.out.println("⚠️ 敏感操作待审批！");
            System.out.println("工具: " + toolName);
            System.out.println("参数: " + input);
            System.out.print("是否执行？(y/n): ");

            String decision = SCANNER.nextLine().trim().toLowerCase();
            if ("y".equals(decision)) {
                // 批准：不改写工具调用，敏感工具将照常执行
                System.out.println("已批准，继续执行 " + toolName + "。");
            } else {
                // 拒绝：把本次调用重定向到 approval_denied（保持 id 不变），敏感工具将不会被执行
                e.setToolUse(ToolUseBlock.builder()
                        .id(call.getId())
                        .name(DENIED_TOOL)
                        .input(Map.of("reason", "管理员拒绝执行 " + toolName))
                        .build());
                System.out.println("已拒绝，敏感工具不会执行。");
            }
        }
    }

    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName(MODEL_NAME)
                .endpointType(EndpointType.MULTIMODAL)
                .build();

        // 2. 注册工具：敏感工具(transfer_money) + 拒绝占位工具(approval_denied) + 非敏感工具(get_weather)
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new BankService());
        toolkit.registerTool(new ApprovalDeniedTool());
        toolkit.registerTool(new WeatherService());

        // 3. 创建 Agent 并挂载审批 Hook
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("你是一个助手。转账请用 transfer_money 工具，查询天气请用 get_weather 工具；"
                        + "根据工具返回结果用一句话回复用户。")
                .model(model)
                .toolkit(toolkit)
                .hook(new ToolApprovalHook())
                .memory(new InMemoryMemory())
                .build();

        // 4. 触发一次敏感工具调用（将进入人工审批）
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("给张三转账 5000 元")
                        .build()
        ).block();

        System.out.println("\n助手: " + (response != null ? response.getTextContent() : "(无响应)"));
    }
}
