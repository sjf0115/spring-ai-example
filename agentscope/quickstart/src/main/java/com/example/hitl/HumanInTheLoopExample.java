package com.example.hitl;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：Human-in-the-Loop 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 22:59
 */
public class HumanInTheLoopExample {

    // 工具
    public static class SensitiveTools {
        // 删除文件
        @Tool(name = "delete_file", description = "Delete a file from the system")
        public String deleteFile(@ToolParam(name = "filename", description = "Name of the file to delete") String filename) {
            // Simulated deletion
            System.out.println("[TOOL] Deleting file: " + filename);
            return "File '" + filename + "' has been deleted successfully.";
        }

        // 发送邮件
        @Tool(name = "send_email", description = "Send an email to a recipient")
        public String sendEmail(
                @ToolParam(name = "to", description = "Recipient email address") String to,
                @ToolParam(name = "subject", description = "Email subject") String subject,
                @ToolParam(name = "body", description = "Email body content") String body) {
            // Simulated email sending
            System.out.println("[TOOL] Sending email to: " + to);
            System.out.println("[TOOL] Subject: " + subject);
            return "Email sent successfully to " + to;
        }

        // 联网查询
        @Tool(name = "search_web", description = "Search the web for information")
        public String searchWeb(@ToolParam(name = "query", description = "Search query") String query) {
            // Simulated web search
            System.out.println("[TOOL] Searching web for: " + query);
            return "Search results for '" + query + "': Found 10 relevant articles.";
        }
    }

    // 工具确认Hook
    private static class ToolConfirmationHook implements Hook {
        // 定义需要审批的敏感工具名单
        private static final List<String> SENSITIVE_TOOLS = List.of("delete_file", "send_email");

        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PostReasoningEvent postReasoning) {
                // 1. 推理后暂停
                Msg reasoningMsg = postReasoning.getReasoningMessage();
                if (reasoningMsg == null) {
                    return Mono.just(event);
                }
                // 调用的工具
                List<ToolUseBlock> toolCalls = reasoningMsg.getContentBlocks(ToolUseBlock.class);
                // 如果包含敏感工具，暂停等待确认
                boolean hasSensitive = toolCalls.stream().anyMatch(t -> SENSITIVE_TOOLS.contains(t.getName()));
                if (hasSensitive) {
                    // 暂停
                    postReasoning.stopAgent();
                }
            } else if (event instanceof PostActingEvent postActing) {
                // 2. 行动后暂停
            }
            return Mono.just(event);
        }
    }

    private static Msg createCancelledToolResults(Msg toolUseMsg, String agentName) {
        List<ToolUseBlock> toolCalls = toolUseMsg.getContentBlocks(ToolUseBlock.class);
        if (toolCalls.isEmpty()) {
            // Return empty tool message if no tool calls (should not happen in normal flow)
            return Msg.builder().name(agentName).role(MsgRole.TOOL).build();
        }

        // Create ToolResultBlock for each pending tool call
        List<ToolResultBlock> results = new ArrayList<>();
        for (ToolUseBlock tool : toolCalls) {
            TextBlock textBlock = TextBlock.builder().text("Operation cancelled by user. Please try a different approach.").build();
            ToolResultBlock toolResultBlock = ToolResultBlock.of(tool.getId(), tool.getName(), textBlock);
            results.add(toolResultBlock);
        }

        Msg msg = Msg.builder()
                .name(agentName)
                .role(MsgRole.TOOL)
                .content(results.toArray(new ToolResultBlock[0]))
                .build();
        return msg;
    }

    public static void main(String[] args) {
        // 工具集
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SensitiveTools());

        // 4. 创建对话模型: 使用百练对话模型
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 5. 创建智能体
        ReActAgent agent = ReActAgent.builder()
                .name("SafeAgent")
                .sysPrompt("You are a helpful assistant with access to file and email tools."
                        + " Always use the appropriate tool when asked to delete files"
                        + " or send emails.")
                .model(chatModel)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .hook(new ToolConfirmationHook())
                .build();

        // 6. 交互
        Scanner scanner = new Scanner(System.in);
        while (true) {
            // 用户输入
            System.out.print("\n用户: ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }
            if (input.isEmpty()) {
                continue;
            }

            // 用户消息
            Msg userMsg = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .textContent(input)
                    .build();

            // 调用 Agent
            Msg response = agent.call(userMsg).block();

            // 检查是否有挂起的工具等待确认
            while (response != null && response.hasContentBlocks(ToolUseBlock.class)) {
                // 展示待执行的工具
                System.out.println("==========================================");
                System.out.println("\n⚠️  Agent 暂停等待确认工具是否调用");
                List<ToolUseBlock> pending = response.getContentBlocks(ToolUseBlock.class);
                for (ToolUseBlock tool : pending) {
                    System.out.println("工具: " + tool.getName());
                    System.out.println("参数: " + tool.getInput());
                }
                System.out.println("==========================================");

                // 确认是否执行
                System.out.print("\n是否执行？(yes/no): ");
                String confirmation = scanner.nextLine().trim().toLowerCase();
                if (confirmation.equals("yes") || confirmation.equals("y")) {
                    // 1. 用户确认，继续执行
                    System.out.println("用户已确认, 继续执行...\n");
                    response = agent.call().block();
                } else if (confirmation.equals("no") || confirmation.equals("n")) {
                    // Provide a manual tool result for all pending tools
                    // 2. 用户拒绝，返回取消信息
                    System.out.println("用户拒绝.\n");
                    Msg cancelResult = createCancelledToolResults(response, agent.getName());
                    response = agent.call(cancelResult).block();
                } else {
                    System.out.println("无效输入，请输入 yes 或者 no");
                }
            }

            // 最终回答
            if (response != null) {
                System.out.println("\n助手: " + response.getTextContent());
            }
        }
    }
}
