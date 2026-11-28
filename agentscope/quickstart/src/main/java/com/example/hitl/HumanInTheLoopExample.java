package com.example.hitl;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;
import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：Human-in-the-Loop（人工确认）实战示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 22:59
 */
public class HumanInTheLoopExample {

    /** 需要人工确认的敏感工具名单 */
    private static final List<String> SENSITIVE_TOOLS = List.of("delete_file");

    /** 模拟人工决策：true = 批准执行，false = 拒绝（改成 false 即可观察「拒绝」分支） */
    private static final boolean HUMAN_APPROVES = false;

    // 敏感工具：删除文件（真实执行有副作用，故需执行前人工确认）
    public static class FileTools {
        @Tool(name = "delete_file", description = "Delete a file from the system")
        public String deleteFile(
                @ToolParam(name = "filename", description = "Name of the file to delete") String filename) {
            System.out.println("[TOOL] 正在删除文件: " + filename);
            return "文件 '" + filename + "' 已成功删除。";
        }
    }

    // 人工确认 Hook：推理后若命中敏感工具，则暂停 Agent，等待外部人工复核
    public static class ToolConfirmationHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PostReasoningEvent e) {
                Msg reasoning = e.getReasoningMessage();
                if (reasoning != null) {
                    boolean hasSensitive = reasoning.getContentBlocks(ToolUseBlock.class).stream()
                            .anyMatch(t -> SENSITIVE_TOOLS.contains(t.getName()));
                    if (hasSensitive) {
                        e.stopAgent(); // 暂停：工具不会执行，call() 返回 REASONING_STOP_REQUESTED
                    }
                }
            }
            return Mono.just(event);
        }
    }

    public static void main(String[] args) {
        // 1. 创建模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName(MODEL_NAME)
                .endpointType(EndpointType.MULTIMODAL)
                .build();

        // 2. 注册工具 + 挂载人工确认 Hook
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new FileTools());

        ReActAgent agent = ReActAgent.builder()
                .name("SafeAgent")
                .sysPrompt("你是一个助手。当用户要求删除文件时，使用 delete_file 工具，并根据工具结果用一句话回复。")
                .model(model)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .hook(new ToolConfirmationHook())
                .build();

        // 3. 用户请求删除文件
        Msg response = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent("帮我删除 report.pdf")
                .build()).block();

        // 4. 若被暂停（REASONING_STOP_REQUESTED），展示待执行工具，模拟人工决策后恢复
        if (response != null && response.getGenerateReason() == GenerateReason.REASONING_STOP_REQUESTED) {
            List<ToolUseBlock> pending = response.getContentBlocks(ToolUseBlock.class);
            System.out.println("====== ⚠️ Agent 已暂停，等待人工确认 ======");
            for (ToolUseBlock t : pending) {
                System.out.println("待执行工具: " + t.getName() + "，参数: " + t.getInput());
            }

            if (HUMAN_APPROVES) {
                // 用户批准，继续执行
                System.out.println("人工决策：批准 → 继续执行");
                response = agent.call().block(); // 无参恢复：真正执行挂起的工具
            } else {
                // 用户拒绝，返回取消信息
                System.out.println("人工决策：拒绝 → 取消操作");
                Msg cancelResult = Msg.builder()
                        .role(MsgRole.TOOL)
                        .content(pending.stream()
                                .map(t -> ToolResultBlock.of(t.getId(), t.getName(),
                                        TextBlock.builder().text("操作已取消").build()))
                                .toArray(ToolResultBlock[]::new))
                        .build();
                response = agent.call(cancelResult).block();
            }
        }

        // 5. 最终回答
        System.out.println("\n助手: " + (response != null ? response.getTextContent() : "(无响应)"));
    }
}
