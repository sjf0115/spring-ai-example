package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;

import java.util.Map;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：注册工具时预设敏感/固定参数
 * 场景：工具方法中某些参数（如 apiKey、商户号、租户 ID、账号密码等）不应暴露给 LLM，
 *      也不应由 LLM 推理填充，而是由应用在注册工具时预设。
 *      调用时框架会自动将这些参数注入工具方法， LLM 看不到也无需传递。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 16:01
 */
public class PresetParametersExample {
    public static void main(String[] args) throws Exception {
        // 1. 创建工具集
        Toolkit toolkit = new Toolkit();

        // 2. 注册工具并预设敏感参数
        // presetParameters 结构: Map<工具名, Map<参数名, 参数值>>
        //   - 外层 key  ：@Tool 方法名（这里 EmailService.send，默认以方法名作为工具名）
        //   - 内层 key  ：@ToolParam 参数名（apiKey）
        //   - 内层 value：注入的实际值，来源于环境变量、配置中心、密钥管理服务等
        //
        // 效果：
        //   - LLM 看到的 send 工具 Schema 中不再包含 apiKey 参数
        //   - 调用时框架会自动从 presetParameters 中读取并注入
        //   - 避免敏感信息泄露到上下文/对话记录
        toolkit.registration()
                .tool(new EmailService())
                .presetParameters(
                        Map.of(
                                "send",
                                Map.of("apiKey", System.getenv("EMAIL_API_KEY"))
                        )
                )
                .apply();

        System.out.println("已注册工具：send（apiKey 已预设，对 LLM 不可见）");

        // 3. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("邮件小助手")
                .sysPrompt("你是一个邮件助手，当用户请求发送邮件时，使用 send 工具。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .build())
                .toolkit(toolkit) // 绑定工具集
                .build();

        // 4. 调用智能体：用户请求发邮件，无需提供 apiKey
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("请发一封邮件给 zhangsan@example.com，"
                        + "主题是《周会通知》，内容是《本周五下午3点在会议室A召开周会》。")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
