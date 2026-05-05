package com.example.rag;

import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
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
 * 功能：SimpleKnowledge 快速入门
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/5 22:20
 */
public class SimpleKnowledgeQuickStart {
    public static void main(String[] args) {
        // 1. 创建 Embedding 模型
        EmbeddingModel embeddingModel = DashScopeTextEmbedding.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
//                .modelName("text-embedding-v3")
                .modelName("tongyi-embedding-vision-plus-2026-03-06")
                .dimensions(1024)
                .build();

        // 2. 创建本地知识库
        Knowledge knowledge = SimpleKnowledge.builder()
                .embeddingModel(embeddingModel)
                // 向量存储: InMemoryStore 内存存储
                .embeddingStore(InMemoryStore.builder().dimensions(1024).build())
                .build();

        // 3. 读取并切分文档并添加文档到知识库
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
        List<Document> docs = reader.read(
                        ReaderInput.fromString("""
                        AgentScope Java 是一个面向智能体的编程框架，用于构建 LLM 驱动的应用程序。
                        它提供了创建智能体所需的一切：ReAct 推理、工具调用、内存管理、多智能体协作等。
                        AgentScope 采用 ReAct（推理-行动）范式，使智能体能够自主规划和执行复杂任务。
                        """))
                .block();
        knowledge.addDocuments(docs).block();

        // 4. 检索
        List<Document> results = knowledge.retrieve(
                "AgentScope 的核心范式是什么？",
                RetrieveConfig.builder()
                        .limit(3)           // 返回最多 3 条结果
                        .scoreThreshold(0.5) // 相似度阈值
                        .build()
        ).block();

        // 5. 输出结果
        for (Document doc : results) {
            System.out.println("相似度: " + doc.getScore());
            System.out.println("内容: " + doc.getMetadata().getContent());
        }
    }
}
