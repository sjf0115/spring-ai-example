package com.example.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * 功能：第一个 ReActAgent 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/1 22:07
 */
public class HelloReActAgent {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserContext {
        private String userId;
    }

    // 工具类
    private static class SimpleTools {
        @Tool(name = "get_time", description = "获取当前时间")
        public String getTime(@ToolParam(name = "zone", description = "时区，例如：北京") String zone) {
            return java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        // 工具中自动注入
        @Tool(name = "query", description = "查询数据")
        public String query(
                @ToolParam(name = "sql") String sql,
                UserContext ctx  // 自动注入，无需 @ToolParam
        ) {
            return "用户 " + ctx.getUserId() + " 的查询结果";
        }
    }

    public static void main(String[] args) {
        // 准备工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SimpleTools());

        // 超时与重试
        ExecutionConfig modelConfig = ExecutionConfig.builder()
                .timeout(Duration.ofMinutes(20))
                .maxAttempts(3)
                .build();

        ExecutionConfig toolConfig = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(30))
                .maxAttempts(1)  // 工具通常不重试
                .build();

        // 工具执行上下文
        ToolExecutionContext context = ToolExecutionContext.builder()
                .register(new UserContext("user-123"))
                .build();

        // 自定义配置
        PlanNotebook planNotebook = PlanNotebook.builder()
                .maxSubtasks(15)
                .build();

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("HelloReActAgent") // 名称
                .sysPrompt("") // 系统提示词
                .description("第一个 ReActAgent")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName("qwen3-max-2026-01-23")
                        .build())
                .maxIters(10) // 最大迭代次数
                .toolkit(toolkit) // 工具
                .modelExecutionConfig(modelConfig)
                .toolExecutionConfig(toolConfig)
                .toolExecutionContext(context)
                .planNotebook(planNotebook)
                .build();

        // 调用智能体
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好，请介绍一下自己")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
