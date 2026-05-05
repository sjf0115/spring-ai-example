package com.example.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 功能：计算器工具
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 15:56
 */
public class CalculatorTool {
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
