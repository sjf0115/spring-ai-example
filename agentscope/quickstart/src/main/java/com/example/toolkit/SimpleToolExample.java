package com.example.toolkit;

import com.example.bean.Constant;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

/**
 * 功能：工具调用入门示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/7 23:12
 */
public class SimpleToolExample {
    public static void main(String[] args) {
        // 1. 选择模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName(Constant.MODEL_NAME)
                .build();

        // 2. 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new WeatherService());

        // 3. 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("天气助手")
                .model(model)
                .toolkit(toolkit)
                .build();

        // 4. 调用智能体
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("今天北京的天气如何")
                .build();
        Msg response = agent.call(msg).block();
        if (response != null) {
            System.out.println(response.getTextContent());
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    // 工具 Tool
    public static class WeatherService {
        @Tool(description = "获取指定城市的天气")
        public String getWeather(@ToolParam(name = "city", description = "城市名称") String city) {
            return city + " 的天气：晴天，25°C";
        }
    }
}
