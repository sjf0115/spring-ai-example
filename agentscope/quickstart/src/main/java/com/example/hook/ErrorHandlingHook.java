package com.example.hook;

import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import reactor.core.publisher.Mono;

/**
 * 功能：ErrorHandlingHook
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/2 13:28
 */
public class ErrorHandlingHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof ErrorEvent errorEvent) {
            String agentName = errorEvent.getAgent().getName();
            Throwable error = errorEvent.getError();
            // 记录详细错误日志
            System.err.println("Agent [" + agentName + "] 执行异常:");
            error.printStackTrace();
            // 发送告警
            // alertService.sendAlert(agentName, error.getMessage());
        }
        return Mono.just(event);
    }
}
