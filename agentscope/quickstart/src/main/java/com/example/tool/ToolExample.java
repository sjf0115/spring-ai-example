package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.tool.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：工具完整示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/23 14:27
 */
public class ToolExample {
    // 1. 定义上下文类：承载调用方身份等运行时信息
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserContext {
        private String userId;
    }

    // 工具类
    public static class OrderTools {
        @Tool(name = "query_orders", description = "查询当前用户的订单列表")
        public String queryOrders(
                @ToolParam(name = "status", description = "订单状态：待付款/待发货/已完成") String status,
                UserContext ctx) {
            return String.format("用户 %s 的订单（状态=%s）有 3 笔：%s", ctx.getUserId(), status, "2笔金额 18元, 一笔金额 201元");
        }
    }

    public static class Calculator {
        @Tool(name = "calculate_price_total", description = "计算订单总金额")
        public double calculateTotal(
                @ToolParam(name = "price", description = "订单单笔金额") double price,
                @ToolParam(name = "num", description = "订单数") int num) {
            return price * num;
        }
    }

    public static void main(String[] args) {
        // 1. 创建工具集
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder()
                .parallel(true)
                .executionConfig(ExecutionConfig.builder()
                        .timeout(Duration.ofSeconds(30))
                        .build())
                .build());

        // 2. 创建工具组并注册工具
        toolkit.createToolGroup("business", "业务工具", true);
        toolkit.registration()
                .tool(new OrderTools())
                .tool(new Calculator())
                .group("business")
                .apply();

        // 3. 构建上下文
        ToolExecutionContext context = ToolExecutionContext.builder()
                .register(new UserContext("张三"))
                .build();

        // 4. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("BusinessAgent")
                .sysPrompt("你是智能业务助手，可以帮助用户查询订单和计算金额。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .build())
                .toolkit(toolkit)
                .toolExecutionContext(context)
                .maxIters(10)
                .build();

        // 5. 调用
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("帮我查一下所有订单，并帮我算一下订单总金额")
                .build();
        Msg response = agent.call(msg).block();

        if (response != null) {
            System.out.println("助手: " + response.getTextContent());
        }
    }
}
