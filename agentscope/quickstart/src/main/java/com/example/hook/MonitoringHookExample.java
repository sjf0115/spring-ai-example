package com.example.hook;

import com.example.tool.WeatherService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.Map;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：MonitoringHook 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/8 12:18
 */
public class MonitoringHookExample {
    // 监控 Hook
    public static class MonitoringHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PreCallEvent preCall) {
                // 1. Agent 调用前
                System.out.println("[HOOK] PreCallEvent - Agent call started: " + preCall.getAgent().getName());
            } else if (event instanceof PreReasoningEvent preReasoning) {
                // 2. LLM 推理前
                System.out.println("[HOOK] PreReasoningEvent - Agent reasoning started: " + preReasoning.getAgent().getName());
            } else if (event instanceof ReasoningChunkEvent reasoningChunk) {
                // 3. LL 推理流式期间
                Msg chunk = reasoningChunk.getIncrementalChunk();
                String text = chunk.getTextContent();
                if (text != null && !text.isEmpty()) {
                    System.out.println("[HOOK] ReasoningChunkEvent - Agent reasoning chunk: " + text);
                }
            } else if (event instanceof PostReasoningEvent postReasoning) {
                // 4. LLM 推理完成后
                System.out.println("[HOOK] PostReasoningEvent - Agent reasoning finished: " + postReasoning.getAgent().getName());
            } else if (event instanceof PreActingEvent preActing) {
                // 5. 工具执行前
                String toolName = preActing.getToolUse().getName();
                Map<String, Object> input = preActing.getToolUse().getInput();
                System.out.println("[HOOK] PreActingEvent - Tool acting started: " + toolName + ", Input: " + input);
            } else if (event instanceof ActingChunkEvent actingChunk) {
                // 6. 工具流式期间
                ToolResultBlock chunk = actingChunk.getChunk();
                String toolName = actingChunk.getToolUse().getName();
                String output = chunk.getOutput().isEmpty() ? "" : chunk.getOutput().get(0).toString();
                System.out.println("[HOOK] ActingChunkEvent - Tool acting chunk: " + toolName + ", Progress: " + output);
            } else if (event instanceof PostActingEvent postActing) {
                // 7. 工具执行完成后
                ToolResultBlock result = postActing.getToolResult();
                String toolName = postActing.getToolUse().getName();
                String output = result.getOutput().isEmpty() ? "" : result.getOutput().get(0).toString();
                System.out.println("[HOOK] PostActingEvent - Tool acting finished: " + toolName + ", Result: " + output);
            } else if (event instanceof PostCallEvent postCall) {
                // 8. PostCallEvent
                System.out.println("[HOOK] PostCallEvent - Agent call finished: " + postCall.getAgent().getName());
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

        // 2. 创建 Agent
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherService());

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .model(model)
                .toolkit(toolkit) // 注册天气查询工具
                .hook(new MonitoringHook()) // 注册 Hook
                .build();

        // 3. 调用
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("北京明天天气如何")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
