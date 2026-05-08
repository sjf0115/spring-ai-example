package com.example.plan;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.SubTask;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/8 22:55
 */
public class PlanNNoteBookExample {

    public static class PlanTools {
        private static final Map<String, String> fileStorage = new HashMap<>();

        @Tool(name = "write_file", description = "Write content to a file")
        public Mono<String> writeFile(
                @ToolParam(name = "filename", description = "File name") String filename,
                @ToolParam(name = "content", description = "Content") String content) {
            System.out.println("\n📝 [write_file] " + filename + " (" + content.length() + " chars)");
            fileStorage.put(filename, content);
            return Mono.just("File saved: " + filename);
        }

        @Tool(name = "read_file", description = "Read content from a file")
        public Mono<String> readFile(@ToolParam(name = "filename", description = "File name") String filename) {
            System.out.println("\n📖 [read_file] " + filename);
            if (!fileStorage.containsKey(filename)) {
                return Mono.just("Error: File not found");
            }
            return Mono.just(fileStorage.get(filename));
        }

        @Tool(name = "calculate", description = "Basic math: +, -, *, /")
        public Mono<String> calculate(@ToolParam(name = "expression", description = "Math expression") String expression) {
            System.out.println("\n🔢 [calculate] " + expression);
            try {
                double result = evaluateExpression(expression);
                return Mono.just(expression + " = " + result);
            } catch (Exception e) {
                return Mono.just("Error: " + e.getMessage());
            }
        }

        private static double evaluateExpression(String expr) {
            expr = expr.replaceAll("\\s+", "");
            // Handle * and /
            while (expr.contains("*") || expr.contains("/")) {
                String[] parts = expr.split("(?=[*/])|(?<=[*/])");
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("*") && i > 0 && i < parts.length - 1) {
                        double result =
                                Double.parseDouble(parts[i - 1]) * Double.parseDouble(parts[i + 1]);
                        expr =
                                expr.replaceFirst(
                                        parts[i - 1] + "\\*" + parts[i + 1], String.valueOf(result));
                        break;
                    } else if (parts[i].equals("/") && i > 0 && i < parts.length - 1) {
                        double result =
                                Double.parseDouble(parts[i - 1]) / Double.parseDouble(parts[i + 1]);
                        expr =
                                expr.replaceFirst(
                                        parts[i - 1] + "/" + parts[i + 1], String.valueOf(result));
                        break;
                    }
                }
            }
            // Handle + and -
            String[] terms = expr.split("(?=[+\\-])|(?<=[+\\-])");
            double result = 0;
            String operator = "+";
            for (String term : terms) {
                if (term.equals("+") || term.equals("-")) {
                    operator = term;
                } else if (!term.isEmpty()) {
                    double value = Double.parseDouble(term);
                    result = operator.equals("+") ? result + value : result - value;
                }
            }
            return result;
        }
    }

    private static void printPlanState(PlanNotebook notebook, String event) {
        Plan currentPlan = notebook.getCurrentPlan();
        if (currentPlan == null) {
            System.out.println("\n📋 [" + event + "] No active plan");
            return;
        }

        System.out.println("\n" + "=".repeat(70));
        System.out.println("📋 PLAN STATE [" + event + "]");
        System.out.println("=".repeat(70));
        System.out.println("Plan: " + currentPlan.getName());
        System.out.println("State: " + currentPlan.getState());
        System.out.println("\nSubtasks:");

        for (int i = 0; i < currentPlan.getSubtasks().size(); i++) {
            SubTask subtask = currentPlan.getSubtasks().get(i);
            String icon =
                    switch (subtask.getState()) {
                        case TODO -> "⏸️";
                        case IN_PROGRESS -> "▶️";
                        case DONE -> "✅";
                        case ABANDONED -> "❌";
                    };
            System.out.printf(
                    "  %s [%d] %s - %s%n", icon, i, subtask.getName(), subtask.getState());
        }
        System.out.println("=".repeat(70) + "\n");
    }


    public static void main(String[] args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new PlanTools());

        PlanNotebook planNotebook = PlanNotebook.builder().build();

        // Create hook to visualize plan changes
        Hook planVisualizationHook =
                new Hook() {
                    @Override
                    public <T extends HookEvent> Mono<T> onEvent(T event) {
                        if (event instanceof PostActingEvent postActing) {
                            // Print plan state after each planning tool call
                            String toolName = postActing.getToolUse().getName();
                            printPlanState(planNotebook, "After " + toolName);
                        }
                        return Mono.just(event);
                    }
                };

        // 创建对话模型: 使用百练对话模型
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // Create agent with PlanNotebook and hook
        ReActAgent agent = ReActAgent.builder()
                        .name("PlanAgent")
                        .sysPrompt(
                                "You are a systematic assistant. For multi-step tasks:\n"
                                        + "1. Create a plan with create_plan tool\n"
                                        + "2. Execute subtasks one by one\n"
                                        + "3. Use finish_subtask after completing each\n"
                                        + "4. Call finish_plan when all done")
                        .model(chatModel)
                        .memory(new InMemoryMemory())
                        .toolkit(toolkit)
                        .maxIters(100)
                        .hooks(List.of(planVisualizationHook))
                        .planNotebook(planNotebook)
                        .build();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("TASK");
        System.out.println("=".repeat(70));
        String userInput = "Calculate the area of a rectangle (length=10, width=5), then save the result to"
                        + " 'result.txt' and verify by reading it back. This is a multi-step task -"
                        + " please organize with a plan.";
        System.out.println(userInput);
        System.out.println("=".repeat(70) + "\n");

        // 交互
        System.out.println("🚀 Starting execution...\n");
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(userInput).build())
                .build();
        Msg response = agent.call(userMsg).block();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("FINAL RESPONSE");
        System.out.println("=".repeat(70));
        System.out.println("\n助手: " + response.getTextContent());
        System.out.println("=".repeat(70) + "\n");

        // Show saved file
        /*if (fileStorage.containsKey("result.txt")) {
            System.out.println("📄 Saved File Content:");
            System.out.println("  " + fileStorage.get("result.txt") + "\n");
        }*/
    }
}
