package com.example.model.embedding;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.InMemoryStore;

import java.util.List;

/**
 * 功能：硅基流动 × AgentScope 构建本地知识库
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/6 10:44
 */
public class SiliconFlowEmbeddingExample {
    public static void main(String[] args) {
        // 1. 创建硅基流动 Embedding 模型
        // 使用 BAAI/bge-m3：支持 8192 tokens，多语言效果优秀
        EmbeddingModel embeddingModel = OpenAITextEmbedding.builder()
                .baseUrl("https://api.siliconflow.cn/v1")           // 硅基流动端点
                .apiKey(System.getenv("SILICONFLOW_API_KEY"))       // API Key
                .modelName("BAAI/bge-m3")                           // 模型名称
                .build();

        // 2. 创建本地知识库
        // bge-m3 输出维度为 1024，InMemoryStore 需要匹配
        Knowledge knowledge = SimpleKnowledge.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(InMemoryStore.builder().dimensions(1024).build())
                .build();

        // 3. 读取文档并添加到知识库
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
        List<Document> docs = reader.read(
                        ReaderInput.fromString("""
                硅基流动（SiliconFlow）是一家专注于开源大模型推理加速的云服务平台。
                平台提供百余种开源模型的 API 服务，覆盖文本、图像、语音、视频等多种模态。
                硅基流动的核心优势在于高性能推理引擎，能够将开源模型的推理速度提升数倍。
                同时，平台完全兼容 OpenAI API 格式，开发者可以零成本迁移现有应用。
                """))
                .block();
        knowledge.addDocuments(docs).block();

        // 4. 检索测试
        List<Document> results = knowledge.retrieve(
                "SiliconFlow 支持哪些模态？",
                RetrieveConfig.builder()
                        .limit(3)
                        .scoreThreshold(0.5)
                        .build()
        ).block();

        // 5. 输出结果
        System.out.println("===== 检索结果 =====");
        for (Document doc : results) {
            System.out.println("相似度: " + doc.getScore());
            System.out.println("内容: " + doc.getMetadata().getContent());
            System.out.println("---");
        }
    }
}
