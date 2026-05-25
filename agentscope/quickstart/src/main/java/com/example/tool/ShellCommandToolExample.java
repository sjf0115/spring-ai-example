package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.coding.ShellCommandTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：内置 Shell 命令工具示例
 * 场景：让 LLM 通过 AgentScope 内置的 ShellCommandTool 在受控的工作目录里执行 Shell 命令。
 *      工具内部已经实现了：
 *        - 命令白名单校验（仅允许预先放行的可执行文件）
 *        - 多命令拼接拦截（&、|、; 这些会被直接拒绝）
 *        - 非白名单命令的人工审批回调（approvalCallback）
 *        - 命令超时（默认 300s）和工作目录（baseDir）限制
 *      工具暴露给 LLM 的工具名是 execute_shell_command。
 *
 * 安全建议：
 *   - 生产环境禁止使用 new ShellCommandTool()（无白名单 + 无审批，等同任意命令执行）
 *   - 推荐：new ShellCommandTool(baseDir, allowedCommands, approvalCallback)
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 18:00
 */
public class ShellCommandToolExample {

    public static void main(String[] args) throws IOException {
        // 1. 准备沙箱工作目录，所有命令的相对路径都解析到这里
        Path workspace = Paths.get("agentscope/quickstart/target/shell-tool-workspace")
                .toAbsolutePath().normalize();
        Files.createDirectories(workspace);
        // 提前准备一个示例文件，供 LLM 读取和统计
        Files.writeString(workspace.resolve("hello.txt"),
                "Hello AgentScope\n你好，世界\n第三行示例\n");
        String baseDir = workspace.toString();

        // 2. 命令白名单：只放行明确安全的"只读"命令
        Set<String> allowedCommands = Set.of("ls", "pwd", "cat", "wc", "echo", "date");

        // 3. 审批回调：白名单之外的命令会走这里，让人来确认是否放行
        //    回调输入是完整命令字符串，返回 true 即放行，false 拒绝
        Scanner scanner = new Scanner(System.in);
        Function<String, Boolean> approvalCallback = command -> {
            System.out.println("[审批] 智能体想执行非白名单命令: " + command);
            System.out.print("[审批] 是否放行? (y/n): ");
            String input = scanner.hasNextLine() ? scanner.nextLine().trim() : "";
            return "y".equalsIgnoreCase(input) || "yes".equalsIgnoreCase(input);
        };

        // 4. 创建并注册内置 Shell 工具
        ShellCommandTool shellTool = new ShellCommandTool(baseDir, allowedCommands, approvalCallback);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(shellTool);

        // 5. 创建 Agent，系统提示中告知工具能力即可（具体白名单工具描述里会自动带上）
        ReActAgent agent = ReActAgent.builder()
                .name("小助手")
                .sysPrompt("你是一个操作小助手，请完成用户请求，并把最终结果用一句话告知用户。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .build())
                .toolkit(toolkit)
                .build();

        // 6. 调用智能体：组合任务（预计触发 ls / cat / wc 三次工具调用）
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("先列出当前目录下的文件，然后查看 hello.txt 的内容，最后告诉我它一共有多少行。最后删除该文件。")
                .build();

        Msg response = agent.call(msg).block();
        if (response != null) {
            System.out.println("助手: " + response.getTextContent());
        }
    }
}
