package com.example.pipeline;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.pipeline.SequentialPipeline;
import io.agentscope.core.tool.Toolkit;

import java.time.Duration;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：顺序管道示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/10 15:56
 */
public class SequentialAgentExample {

    private static final String SAMPLE_ARTICLE =
            "Artificial Intelligence has revolutionized the technology industry in recent years."
                    + " Machine learning algorithms now power everything from recommendation systems to"
                    + " autonomous vehicles. While AI brings tremendous opportunities for innovation"
                    + " and efficiency, it also raises important questions about ethics, privacy, and"
                    + " job displacement. As we move forward, finding the right balance between"
                    + " technological advancement and societal well-being will be crucial for"
                    + " sustainable development.";

    public static void main(String[] args) {
        // 模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 1. 翻译Agent
        ReActAgent translator = ReActAgent.builder()
                .name("翻译助手")
                .sysPrompt("你是一名专业的翻译人员。将给定的英语文本准确翻译成中文。保留原意和语气。只输出翻译后的文本，无需解释。")
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .build();

        // 2. 摘要Agent
        ReActAgent summarizer = ReActAgent.builder()
                .name("内容摘要助手")
                .sysPrompt("你是一名专业的内容摘要生成者。请根据给定的文本生成 2-3 句简洁的摘要。要涵盖主要观点和关键信息。摘要应使用与输入相同的语言。仅输出摘要，不添加其他评论。")
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .build();

        // 3. 情感分析专家Agent
        ReActAgent sentimentAnalyzer = ReActAgent.builder()
                .name("情感分析专家")
                .sysPrompt("""
                            您是情感分析专家。分析给定文本的情感基调。将情感分类为：积极、消极、中性或混合。用 1-2 句话解释您的分类理由。格式如下：
                                    情感：[分类]
                                    理由：[解释]
                                    总结：[重复输入文本]
                        """)
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .build();

        // 构建顺序管道
        SequentialPipeline pipeline =
                SequentialPipeline.builder()
                        .addAgent(translator)
                        .addAgent(summarizer)
                        .addAgent(sentimentAnalyzer)
                        .build();
        System.out.println("Pipeline 生成3个Agent");
        System.out.println("  [1] 英文翻译 → [2] 生成摘要 → [3] 情感分析");

        // 执行顺序管道
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(SAMPLE_ARTICLE).build())
                .build();
        Msg response = pipeline.execute(msg).block(Duration.ofMinutes(3));

        // 最终结果
        System.out.println("Pipeline 执行结果：");
        if (response != null) {
            System.out.println(response.getTextContent());
        } else {
            System.out.println("[无结果]");
        }
    }
}
