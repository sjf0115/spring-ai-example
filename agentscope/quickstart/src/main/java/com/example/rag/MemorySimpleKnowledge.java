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
 * 功能：本地知识库
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/8 23:53
 */
public class MemorySimpleKnowledge {
    public static void main(String[] args) {
        // 1. 创建知识库
        EmbeddingModel embeddingModel = DashScopeTextEmbedding.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen3-vl-embedding")
                .dimensions(1024)
                .build();

        Knowledge knowledge = SimpleKnowledge.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(InMemoryStore.builder().dimensions(1024).build())
                .build();

        // 2. 添加文档
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
        List<Document> docs = reader.read(ReaderInput.fromString("Dify 作为一个开源的 LLM 应用开发平台，通过其强大的知识库功能，完美地搭建了私有数据与大模型之间的桥梁。")).block();
        knowledge.addDocuments(docs).block();

        // 3. 检索
        List<Document> results = knowledge.retrieve("Dify 是什么",
                RetrieveConfig.builder().limit(3).scoreThreshold(0.5).build()).block();
    }
}
