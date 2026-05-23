package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：工具执行上下文（ToolExecutionContext）示例
 * 场景：工具方法在执行时常常需要"调用方身份"等运行时上下文（如 userId、tenantId、traceId、当前登录会话），
 *      这些信息既不应该暴露给 LLM，也不应该由 LLM 推理填充，而是由应用在 Agent 创建时绑定，
 *      框架会自动注入到工具方法的对应参数上。
 *
 * 与 presetParameters 的区别：
 *   - presetParameters：按"工具名 + 参数名"键值匹配注入，适合固定值（如 apiKey）
 *   - ToolExecutionContext：按"对象类型"匹配注入，适合复杂业务对象（如 UserContext、TenantContext）
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 16:30
 */
public class ToolExecutionContextExample {

    // 1. 定义上下文类：承载调用方身份等运行时信息
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserContext {
        private String userId;
    }

    // 2. 工具类：通过参数类型声明对上下文的依赖，框架按类型自动注入
    public static class OrderTools {
        @Tool(name = "query_orders", description = "查询当前用户的订单列表")
        public String queryOrders(
                @ToolParam(name = "status", description = "订单状态：待付款/待发货/已完成") String status,
                UserContext ctx) {   // 非 @ToolParam 标注的参数 → 框架按类型从 ToolExecutionContext 自动注入
            // ctx 已包含调用方身份，LLM 无需感知
            return String.format("用户 %s 的订单（状态=%s）：3 笔订单", ctx.getUserId(), status);
        }
    }

    public static void main(String[] args) {
        // 3. 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new OrderTools());

        // 4. 构建工具执行上下文，注册 UserContext 实例
        //   - register(...) 接受任意对象，按对象 Class 作为 key 存储
        //   - 工具方法中只要声明同类型参数，即可自动注入
        ToolExecutionContext context = ToolExecutionContext.builder()
                .register(new UserContext("张三"))
                .build();

        // 5. 创建 Agent，绑定 toolkit 与 toolExecutionContext
        ReActAgent agent = ReActAgent.builder()
                .name("订单助手")
                .sysPrompt("你是一个订单查询助手，当用户请求查询订单时，使用 query_orders 工具。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .build())
                .toolkit(toolkit)
                .toolExecutionContext(context) // 绑定上下文
                .build();

        // 6. 调用智能体：用户无需提供 userId，工具中可直接拿到
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("帮我查一下已支付的订单有哪些")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
