package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.ToolChoice;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

import java.time.Duration;

/**
 * 功能：OpenAI 兼容平台 - 硅基流动
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/22 22:15
 */
public class SiliconFlowOpenAIChatModelExample {
    public static void main(String[] args) {
        // 1. 模型
        GenerateOptions options = GenerateOptions.builder()
                .temperature(0.7)           // // 随机性 (0.0-2.0)
                .topP(0.9)                  // 核采样(0.0-1.0)
                .topK(40)                   // Top-K 采样
                .maxTokens(2000)            // 最大输出 token 数
                .seed(42L)                  // 随机种子
                .toolChoice(new ToolChoice.Auto())  // 工具选择策略
                .build();

        OpenAIChatModel model = OpenAIChatModel.builder()
                .apiKey(System.getenv("SILICON_FlOW_API_KEY"))
                .modelName("Pro/deepseek-ai/DeepSeek-V3.2")
                .baseUrl("https://api.siliconflow.cn/v1")
                .generateOptions(options)
                .build();

        // 2. 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherTools());

        // 3. 构建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("OpenAIAgent")
                .sysPrompt("你是一个天气助手，可以查询城市天气信息。")
                .model(model)
                .toolkit(toolkit)
                .maxIters(10)
                .modelExecutionConfig(ExecutionConfig.builder()
                        .timeout(Duration.ofMinutes(2))
                        .maxAttempts(3)
                        .initialBackoff(Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(10))
                        .backoffMultiplier(2.0)
                        .build())
                .build();

        // 4. 调用
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("北京今天天气如何？")
                .build();
        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }

    // 定义工具
    public static class WeatherTools {
        @Tool(name = "get_weather", description = "获取指定城市的天气")
        public String getWeather(
                @ToolParam(name = "city", description = "城市名称") String city) {
            return String.format("%s：晴天，气温 25 ℃", city);
        }
    }
}
