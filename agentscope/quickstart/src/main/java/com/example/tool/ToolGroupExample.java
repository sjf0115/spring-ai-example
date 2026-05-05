package com.example.tool;

import io.agentscope.core.tool.Toolkit;

import java.util.List;

/**
 * 功能：工具组示例
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 15:55
 */
public class ToolGroupExample {
    public static void main(String[] args) {
        // 1. 创建工具组
        Toolkit toolkit = new Toolkit();
        toolkit.createToolGroup("basic", "基础工具", true);    // 默认激活
        toolkit.createToolGroup("admin", "管理工具", false);   // 默认停用
        toolkit.createToolGroup("finance", "财务工具", false);

        // 2. 注册工具到指定组
        toolkit.registration()
                .tool(new WeatherService())
                .tool(new CalculatorTool())
                .group("basic")
                .apply();

        /*toolkit.registration()
                .tool(new UserAdminTool())
                .tool(new DataExportTool())
                .group("admin")
                .apply();

        toolkit.registration()
                .tool(new InvoiceTool())
                .tool(new ReportTool())
                .group("finance")
                .apply();*/

        // 激活管理工具（用于管理员用户）
        toolkit.updateToolGroups(List.of("admin"), true);

        // 激活财务工具（用于财务场景）
        toolkit.updateToolGroups(List.of("finance"), true);

        // 停用基础工具（某些特殊场景不需要）
        toolkit.updateToolGroups(List.of("basic"), false);
    }
}
