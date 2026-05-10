package com.example.output;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.StructuredOutputReminder;

import java.util.List;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：结构化输出示例 - TOOL_CHOICE 模式
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/10 13:07
 */
public class StructuredOutputToolChoiceExample {
    public static void main(String[] args) {
        // 模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // API 密钥
                .modelName(MODEL_NAME) // 模型名称
                .build();

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("智能助手")
                .sysPrompt("你是一名智能分析助手。对用户请求进行分析，并给出条理清晰的回复。")
                .model(model)
                .structuredOutputReminder(StructuredOutputReminder.TOOL_CHOICE)
                .build();

        // 调用智能体
        String query = "我正在寻找一台笔记本电脑。我需要至少 16GB 的内存，最好是有苹果品牌的产品，而且我的预算在 8000 人民币左右。它还要便于携带以便于旅行使用。";
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("从这个查询中提取出产品需求: " + query)
                .build();
        Msg response = agent.call(userMsg, ProductInfo.class).block();
        // 结构化输出
        ProductInfo result = response.getStructuredData(ProductInfo.class);
        System.out.println("产品需求:");
        System.out.println("  产品类型: " + result.productType);
        System.out.println("  品牌: " + result.brand);
        System.out.println("  最小内存: " + result.minRam + " GB");
        System.out.println("  最大预算: " + result.maxBudget + " 元");
        System.out.println("  特征: " + result.features);
    }

    // 产品信息
    public static class ProductInfo {
        public String productType;
        public String brand;
        public Integer minRam;
        public Double maxBudget;
        public List<String> features;
    }
}
