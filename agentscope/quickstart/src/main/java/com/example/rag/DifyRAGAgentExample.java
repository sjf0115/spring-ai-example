package com.example.rag;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.integration.dify.*;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.tool.Toolkit;

import java.time.Duration;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 10:49
 */
public class DifyRAGAgentExample {
    public static void main(String[] args) {
        // 1. Dify RAG 配置
        DifyRAGConfig config = DifyRAGConfig.builder()
                // === 连接配置（必填）===
//                .apiKey(System.getenv("DIFY_RAG_API_KEY"))  // Dataset API Key
                .datasetId("1948bb98-46c6-4464-846d-549af8d02d87")  // 数据集 ID（UUID 格式）

                // === 端点配置（可选）===
//                .apiBaseUrl("https://api.dify.ai/v1")       // Dify Cloud（默认）
                 .apiBaseUrl("http://localhost/v1")  // 自托管实例

                // === 检索配置（可选）===
                .retrievalMode(RetrievalMode.HYBRID_SEARCH) // 检索模式，默认 HYBRID_SEARCH
                // 可选模式：KEYWORD（关键词）、SEMANTIC_SEARCH（语义）、HYBRID_SEARCH（混合）、FULLTEXT（全文）
                .topK(10)                                   // 检索返回数量，范围 1-100，默认 10
                .scoreThreshold(0.5)                        // 相似度阈值，范围 0.0-1.0，默认 0.0
                .weights(0.6)                               // 混合搜索语义权重，范围 0.0-1.0

                // === 重排序配置（可选）===
                .enableRerank(true)                         // 启用重排序，默认 false
                .rerankConfig(RerankConfig.builder()
                        .providerName("BAAI")                 // Rerank 模型提供商
                        .modelName("bge-reranker-v2-m3")       // Rerank 模型名称
                        .build())

                // === 元数据过滤（可选）===
                .metadataFilter(MetadataFilter.builder()
                        .logicalOperator("AND")                 // 逻辑运算符：AND 或 OR
                        .addCondition(MetadataFilterCondition.builder()
                                .name("category")                   // 元数据字段名
                                .comparisonOperator("=")            // 比较运算符
                                .value("documentation")             // 过滤值
                                .build())
                        .build())

                // === HTTP 配置（可选）===
                .connectTimeout(Duration.ofSeconds(30))     // 连接超时，默认 30s
                .readTimeout(Duration.ofSeconds(60))        // 读取超时，默认 60s
                .maxRetries(3)                              // 最大重试次数，默认 3
                .addCustomHeader("X-Custom-Header", "value") // 自定义请求头

                .build();

        // 2. 创建 Dify 知识库
        DifyKnowledge knowledge = DifyKnowledge.builder()
                .config(config)
                .build();

        // 3. 创建对话模型: 使用百练对话模型
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 4. 创建 RAG Agent（Generic 模式）
        ReActAgent agent = ReActAgent.builder()
                .name("DocAssistant")
                .sysPrompt("""
                        你是企业文档助手，基于知识库中的参考资料回答用户问题。
                        如果知识库中没有相关信息，请明确告知用户。
                        回答时引用参考资料的来源。
                        """)
                .model(chatModel)
                .toolkit(new Toolkit())
                .knowledge(knowledge)
                .ragMode(RAGMode.AGENTIC)
                .retrieveConfig(
                        RetrieveConfig.builder()
                                .limit(5)
                                .scoreThreshold(0.4)
                                .build())
                .memory(new InMemoryMemory())
                .build();

        // 5. 交互
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("我们公司 2025 年的营收目标是什么？")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
