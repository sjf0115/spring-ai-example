package com.example.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:26
 */
public class ToolApprovalHook implements Hook {
    private static final Set<String> SENSITIVE_TOOLS = Set.of(
            "transfer_money", "delete_user", "update_database"
    );

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent e) {
            String toolName = e.getToolUse().getName();

            if (SENSITIVE_TOOLS.contains(toolName)) {
                Map<String, Object> input = e.getToolUse().getInput();
                System.out.println("⚠️ 敏感操作待审批！");
                System.out.println("工具: " + toolName);
                System.out.println("参数: " + input);
                System.out.print("是否执行？(y/n): ");

                Scanner scanner = new Scanner(System.in);
                String decision = scanner.nextLine().trim().toLowerCase();

                /*if (!"y".equals(decision)) {
                    // 拒绝执行：将工具结果修改为空，Agent 会收到"操作被拒绝"的反馈
                    e.setToolResult(ToolResultBlock.builder()
                            .name(toolName)
                            .output(List.of(TextBlock.builder()
                                    .text("操作被管理员拒绝")
                                    .build()))
                            .build());
                }*/
            }
        }
        return Mono.just(event);
    }
}
