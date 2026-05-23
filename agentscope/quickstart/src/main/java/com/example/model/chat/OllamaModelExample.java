package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.ollama.OllamaOptions;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

import java.time.Duration;

/**
 * 功能：与 Ollama 自托管模型集成示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/7 23:14
 */
public class OllamaModelExample {
    public static void main(String[] args) {
        // 1. 模型参数
        OllamaOptions options = OllamaOptions.builder()
                .numCtx(4096)           // 上下文窗口大小
                .temperature(0.7)       // 生成随机性
                .topK(40)               // Top-K 采样
                .topP(0.9)              // 核采样
                .repeatPenalty(1.1)     // 重复惩罚
                .build();

        // 2. Ollama 千问模型
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("qwen2.5:7b")
                .baseUrl("http://localhost:11434")  // 默认值
                .defaultOptions(options)
                .build();

        // 3. 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTools());

        // 4. 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("你是一个本地运行的天气助手，可以查询城市天气。请用中文回答。")
                .model(model)
                .toolkit(toolkit) // 绑定工具
                .maxIters(10)
                .modelExecutionConfig(ExecutionConfig.builder()
                        .timeout(Duration.ofMinutes(5))    // 本地推理较慢，超时设长
                        .maxAttempts(2)
                        .initialBackoff(Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .backoffMultiplier(2.0)
                        .build())
                .build();

        // 5. 调用智能体
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("北京今天天气怎么样？适合跑步吗？")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }

    // 定义工具
    public static class WeatherTools {
        @Tool(name = "get_weather", description = "获取指定城市的天气信息")
        public String getWeather(
                @ToolParam(name = "city", description = "城市名称") String city) {
            return String.format("%s：多云，气温 22 ℃，湿度 65%%", city);
        }
    }
}
