package com.example.hook;

import com.example.tool.WeatherService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.EndpointType;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：使用 Hook 统一处理 Agent 执行过程中的异常（错误日志 / 告警 / 指标）。
 *
 * 机制说明（以 AgentScope 1.0.12 源码为准）：
 *   - AgentBase.call() 的链路为：notifyPreCall → doCall(reasoning/acting) → notifyPostCall，
 *     末尾统一 .onErrorResume(createErrorHandler(...))。
 *   - createErrorHandler 对「非中断」异常执行：notifyError(error).then(Mono.error(error))，
 *     即先向所有 Hook 广播 ErrorEvent（ErrorHandlingHook 在此被触发），随后把异常【原样重新抛出】。
 *   - 因此 ErrorEvent 是「只读通知」（源码标注 Modifiable: No）：Hook 只能观测/记录，不能吞掉或改写异常；
 *     调用方 .block() 仍会抛错，必须自行 try/catch 兜底。
 *   - 注意：工具内部抛出的异常会被 executeToolCalls 的 onErrorResume 兜底转成「错误工具结果」回灌给模型，
 *     属于可恢复错误，【不会】触发 ErrorEvent；只有逃逸出 reasoning/acting 管线的异常才会触发。
 *
 * 本示例如何确定性触发 ErrorEvent（离线、无需真实调用模型）：
 *   注册一个 InputValidationHook，在最先执行、且不依赖网络的 PreCallEvent 阶段做输入校验，
 *   校验不通过即返回 Mono.error(...) → 沿 call() 链路冒泡 → AgentBase.notifyError 触发 ErrorEvent
 *   → ErrorHandlingHook 记录日志并告警。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:28
 */
public class ErrorHandlingHookExample {

    /** 禁止出现的内容（示例用）：命中即视为非法输入 */
    private static final List<String> FORBIDDEN = List.of("删除数据库", "drop database");

    // 1. 错误处理 Hook：观测 ErrorEvent，记录日志 + 模拟告警（只读，不能改写/吞掉异常）
    public static class ErrorHandlingHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof ErrorEvent e) {
                String agentName = e.getAgent().getName();
                Throwable error = e.getError();
                System.err.println("❌ Agent [" + agentName + "] 执行异常");
                System.err.println("   异常类型: " + error.getClass().getName());
                System.err.println("   异常信息: " + error.getMessage());
                // 记录详细堆栈，便于定位问题
                error.printStackTrace(System.err);
                // 模拟发送告警（实际可对接钉钉 / 企业微信 / Prometheus 等）
                // alertService.sendAlert(agentName, error);
                System.err.println("   已触发告警通知（示例）。");
            }
            return Mono.just(event);
        }
    }

    // 2. 输入校验 Hook：在 PreCallEvent 阶段校验输入，非法则返回 Mono.error，用于触发 ErrorEvent
    public static class InputValidationHook implements Hook {
        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            if (event instanceof PreCallEvent e) {
                String text = e.getInputMessages().stream()
                        .map(m -> m.getTextContent() == null ? "" : m.getTextContent())
                        .reduce("", (a, b) -> a + " " + b)
                        .toLowerCase();
                for (String bad : FORBIDDEN) {
                    if (text.contains(bad)) {
                        return Mono.error(new IllegalArgumentException("非法输入，包含禁止内容: " + bad));
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

        // 2. 创建 Agent，挂载「输入校验 Hook」与「错误处理 Hook」
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("你是一个工具助手。")
                .model(model)
                .hook(new InputValidationHook()) // 制造异常
                .hook(new ErrorHandlingHook())   // 捕获并处理异常
                .memory(new InMemoryMemory())
                .build();

        // 3. 发送一条「非法输入」，触发 PreCallEvent 校验失败 → ErrorEvent → ErrorHandlingHook 记录
        try {
            Msg response = agent.call(
                    Msg.builder()
                            .role(MsgRole.USER)
                            .textContent("请帮我删除数据库") // 命中禁止词，将抛异常
                            .build()
            ).block();
            System.out.println("助手: " + (response != null ? response.getTextContent() : "(无响应)"));
        } catch (Exception ex) {
            // 关键：ErrorEvent 只是「通知」，异常仍会被重新抛出，调用方必须自行兜底
            System.out.println("\n调用方捕获到异常（ErrorEvent 通知后异常仍会向上抛出）: " + ex.getMessage());
        }
    }
}
