package com.example.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 功能：简单工具
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/3/7 23:24
 */
public class SimpleTools {
    // 获取指定时区的当前时间
    @Tool(name = "get_current_time", description = "Get the current time in a specific timezone")
    public String getCurrentTime(
            @ToolParam(name = "timezone", description = "Timezone name, e.g., 'Asia/Tokyo', 'America/New_York', 'Europe/London'") String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            LocalDateTime now = LocalDateTime.now(zoneId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return String.format("Current time in %s: %s", timezone, now.format(formatter));
        } catch (Exception e) {
            return "Error: Invalid timezone. Try 'Asia/Tokyo' or 'America/New_York'";
        }
    }

    /**
     * 计算简单四则运算
     * @param expression Math expression (e.g., "123 + 456", "10 * 20")
     * @return Calculation result
     */
    @Tool(name = "calculate", description = "Calculate simple math expressions")
    public String calculate(
            @ToolParam(name = "expression", description = "Math expression to evaluate, e.g., '123 + 456', '10 * 20'")
            String expression) {
        try {
            // Simple calculator supporting +, -, *, /
            expression = expression.replaceAll("\\s+", "");

            double result;
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                result = Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
            } else if (expression.contains("-")) {
                String[] parts = expression.split("-");
                result = Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
            } else if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                result = Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
            } else if (expression.contains("/")) {
                String[] parts = expression.split("/");
                result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            } else {
                return "Error: Unsupported operation. Use +, -, *, or /";
            }

            return String.format("%s = %.2f", expression, result);

        } catch (Exception e) {
            return "Error: Invalid expression. Example: '123 + 456'";
        }
    }
}
