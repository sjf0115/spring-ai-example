package com.example.model.chat;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.ollama.OllamaOptions;
import io.agentscope.core.model.ollama.ThinkOption;

/**
 * 功能：OllamaOptions 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/23 09:09
 */
public class OllamaOptionsExample {
    public static void main(String[] args) {
        // 1. 模型参数
        // 1.1 Ollama 参数配置
        OllamaOptions options = OllamaOptions.builder()
                // 基础生成参数
                .temperature(0.7)       // 生成随机性 (0.0-2.0)
                .topK(40)               // Top-K 采样
                .topP(0.9)              // 核采样
                .minP(0.05)             // 最小概率阈值
                .numCtx(4096)           // 上下文窗口大小
                .repeatPenalty(1.1)     // 重复惩罚
                .frequencyPenalty(0.5)  // 频率惩罚
                .presencePenalty(0.5)   // 存在惩罚
                .seed(42)               // 随机种子
                .numPredict(-1)         // 生成的最大 token 数 -1 表示无限
                // 模型加载参数
                .numBatch(512)          // 批处理大小
                .numGPU(0)             // GPU 层数：-1 表示全部卸载到 GPU、0 表示纯 CPU
                .numThread(8)           // CPU 线程数
                .lowVRAM(false)         // 低显存模式
                .useMMap(true)          // 内存映射加载
                .useMLock(false)        // 锁定内存防交换
                .f16KV(true)            // 16 位 KV 缓存（省显存）
                .mainGPU(0)             // 主 GPU 索引
                // 思考模式
                .thinkOption(ThinkOption.ThinkBoolean.ENABLED)
                .build();

        // 1.2 标准参数配置
        /*GenerateOptions options = GenerateOptions.builder()
                .temperature(0.7)           // 映射到 Ollama 的 temperature
                .topP(0.9)                  // 映射到 Ollama 的 top_p
                .topK(40)                   // 映射到 Ollama 的 top_k
                .maxTokens(2000)            // 映射到 Ollama 的 num_predict
                .seed(42L)                  // 映射到 Ollama 的 seed
                .frequencyPenalty(0.5)      // 映射到 Ollama 的 frequency_penalty
                .presencePenalty(0.5)       // 映射到 Ollama 的 presence_penalty
                .additionalBodyParam(OllamaOptions.ParamKey.NUM_CTX.getKey(), 4096)      // 上下文窗口大小
                .additionalBodyParam(OllamaOptions.ParamKey.NUM_GPU.getKey(), -1)        // 将所有层卸载到 GPU
                .additionalBodyParam(OllamaOptions.ParamKey.REPEAT_PENALTY.getKey(), 1.1) // 重复惩罚
                .additionalBodyParam(OllamaOptions.ParamKey.MAIN_GPU.getKey(), 0)        // 主 GPU 索引
                .additionalBodyParam(OllamaOptions.ParamKey.LOW_VRAM.getKey(), false)    // 低显存模式
                .additionalBodyParam(OllamaOptions.ParamKey.F16_KV.getKey(), true)       // 16位 KV 缓存
                .additionalBodyParam(OllamaOptions.ParamKey.NUM_THREAD.getKey(), 8)      // CPU 线程数
                .build();*/

        // 2. 模型
        OllamaChatModel model = OllamaChatModel.builder()
                .modelName("qwen3:8b")
                .baseUrl("http://localhost:11434")  // 默认值，可省略
                .defaultOptions(options)
                //.defaultOptions(OllamaOptions.fromGenerateOptions(options))  // 内部转换为 OllamaOptions
                .build();

        // 3. 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("LocalAssistant")
                .model(model)
                .build();

        // 4. 调用
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好，请用三句话介绍一下 AgentScope Java")
                .build();
        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
