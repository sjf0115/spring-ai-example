package com.example.pipeline;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Optional;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/10 16:46
 */
public class ParallelAgentExample {
    private static final String RESEARCH_REPORT_KEY = "research_report";

    public static void main(String[] args) throws GraphRunnerException {
        // 模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 1. techResearcher
        ReActAgent.Builder techBuilder = ReActAgent.builder()
                .name("tech_researcher")
                .model(model)
                .description("Researches from technology perspective")
                .sysPrompt("你是一名技术分析师。从技术角度对给定的主题进行研究。提供一个简洁的 2 至 3 段的分析，涵盖：关键技术、趋势和创新。仅关注技术方面。")
                .memory(new InMemoryMemory());

        AgentScopeAgent techResearcher = AgentScopeAgent.fromBuilder(techBuilder)
                .name("tech_researcher")
                .description("Researches from technology perspective")
                .instruction("Research the following topic: {input}.")
                .includeContents(false)
                .outputKey("tech_analysis")
                .build();

        // 2. financeResearcher
        ReActAgent.Builder financeBuilder =
                ReActAgent.builder()
                        .name("finance_researcher")
                        .model(model)
                        .description("Researches from finance perspective")
                        .sysPrompt("你是一名金融分析师。从金融和商业的角度对给定的主题进行研究。提供一个简洁的 2 至 3 段的分析，涵盖：市场规模、投资趋势、商业模式。只关注金融和商业方面的内容。")
                        .memory(new InMemoryMemory());
        AgentScopeAgent financeResearcher =
                AgentScopeAgent.fromBuilder(financeBuilder)
                        .name("finance_researcher")
                        .description("Researches from finance perspective")
                        .instruction("Research the following topic: {input}.")
                        .includeContents(false)
                        .outputKey("finance_analysis")
                        .build();

        // 3. marketResearcher
        ReActAgent.Builder marketBuilder =
                ReActAgent.builder()
                        .name("market_researcher")
                        .model(model)
                        .description("Researches from market perspective")
                        .sysPrompt("你是一名市场分析师。从行业和市场角度对给定的主题进行研究。提供一份简洁的 2 至 3 段的分析报告，涵盖：竞争格局、增长动力、挑战。仅关注市场和行业方面的内容。")
                        .memory(new InMemoryMemory());
        AgentScopeAgent marketResearcher =
                AgentScopeAgent.fromBuilder(marketBuilder)
                        .name("market_researcher")
                        .description("Researches from market perspective")
                        .instruction("Research the following topic: {input}.")
                        .outputKey("market_analysis")
                        .build();

        // 并行Pipeline

        ParallelAgent pipeline = ParallelAgent.builder()
                .name("parallel_research_agent")
                .description("Multi-topic research: analyzes a topic from tech, finance, and market angles in parallel")
                .subAgents(List.of(techResearcher, financeResearcher, marketResearcher))
                .mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
                .mergeOutputKey(RESEARCH_REPORT_KEY)
                .maxConcurrency(3)
                .build();

        System.out.println("Pipeline 生成3个Agent");
        System.out.println("  [1] 技术调研、金融/商业调研、市场/行业调研");

        Optional<OverAllState> resultOpt = pipeline.invoke("企业软件中的 AI Agent");
        if (resultOpt.isEmpty()) {
            System.out.println("[无结果]");
        } else {
            OverAllState state = resultOpt.get();
            Optional<Object> resultOptional = state.value(RESEARCH_REPORT_KEY);
            if (resultOptional.isEmpty()) {
                System.out.println("[无结果]");
            } else {
                Object v = resultOptional.get();
                if (v instanceof Message message) {
                    System.out.println(message.getText());
                }
            }
        }
    }
}
