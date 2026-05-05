package com.example.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 功能：天气工具
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/4/19 18:17
 */
public class WeatherService {
    @Tool(name = "get_weather", description = "获取指定城市的实时天气信息，包括天气状况、气温、风力。当用户询问天气、出行建议、穿衣指南时使用。")
    public String getWeather(@ToolParam(name = "city", description = "城市名称") String city) {
        return city + " 的天气：晴天，25°C";
    }
}
