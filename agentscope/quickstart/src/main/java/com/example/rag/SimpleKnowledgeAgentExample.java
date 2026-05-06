package com.example.rag;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.embedding.EmbeddingModel;
import io.agentscope.core.embedding.dashscope.DashScopeTextEmbedding;
import io.agentscope.core.embedding.openai.OpenAITextEmbedding;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.rag.Knowledge;
import io.agentscope.core.rag.RAGMode;
import io.agentscope.core.rag.knowledge.SimpleKnowledge;
import io.agentscope.core.rag.model.Document;
import io.agentscope.core.rag.model.RetrieveConfig;
import io.agentscope.core.rag.reader.ReaderInput;
import io.agentscope.core.rag.reader.SplitStrategy;
import io.agentscope.core.rag.reader.TextReader;
import io.agentscope.core.rag.store.InMemoryStore;
import io.agentscope.core.tool.Toolkit;

import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：本地知识库 Agent 示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/5 22:20
 */
public class SimpleKnowledgeAgentExample {
    public static void main(String[] args) {
        // 1. 创建 Embedding 模型: 使用硅基流动向量化模型
        EmbeddingModel embeddingModel = OpenAITextEmbedding.builder()
                .baseUrl("https://api.siliconflow.cn/v1")           // 硅基流动端点
                .apiKey(System.getenv("SILICON_FlOW_API_KEY"))       // API Key
                .modelName("BAAI/bge-m3")                           // 模型名称
                .dimensions(1024)
                .build();

        // 2. 向量化存储
        InMemoryStore vectorStore = InMemoryStore.builder()
                .dimensions(1024)
                .build();

        // 3. 创建本地知识库
        // 3.1 创建知识库
        Knowledge knowledge = SimpleKnowledge.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(vectorStore)
                .build();

        // 3.2 读取并切分文档
        TextReader reader = new TextReader(512, SplitStrategy.PARAGRAPH, 50);
        List<Document> docs = reader.read(
                        ReaderInput.fromString("""
                        AgentScope Java 是一个面向智能体的编程框架，用于构建 LLM 驱动的应用程序。
                        它提供了创建智能体所需的一切：ReAct 推理、工具调用、内存管理、多智能体协作等。
                        AgentScope 采用 ReAct（推理-行动）范式，使智能体能够自主规划和执行复杂任务。
                        """))
                .block();

        // 3.3 添加文档到知识库
        knowledge.addDocuments(docs).block();

        // 4. 创建对话模型: 使用百练对话模型
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 5. 创建智能体
        ReActAgent agent = ReActAgent.builder()
                        .name("RAGAssistant")
                        .sysPrompt(
                                "You are a helpful assistant with access to a knowledge retrieval"
                                        + " tool. When you need information from the knowledge base,"
                                        + " use the retrieve_knowledge tool. Always explain what you're"
                                        + " doing.")
                        .model(chatModel)
                        .memory(new InMemoryMemory())
                        .toolkit(new Toolkit())
                        .knowledge(knowledge) // 本地知识库
                        .ragMode(RAGMode.AGENTIC)
                        .retrieveConfig(
                                RetrieveConfig.builder()
                                        .limit(3)
                                        .scoreThreshold(0.3)
                                        .build()
                                )
                        .build();

        // 6. 交互
        Msg response = agent.call(
                Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("AgentScope Java 的核心范式是什么")
                        .build()
        ).block();

        System.out.println(response.getTextContent());
    }
}
