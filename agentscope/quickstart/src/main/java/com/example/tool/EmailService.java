package com.example.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;

/**
 * 功能：邮件服务-工具
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 16:00
 */
@Slf4j
public class EmailService {
    @Tool(description = "发送邮件")
    public String send(
            @ToolParam(name = "to", description = "收件人邮箱") String to,
            @ToolParam(name = "subject", description = "邮件主题") String subject,
            @ToolParam(name = "content", description = "邮件正文") String content,
            @ToolParam(name = "apiKey", description = "邮件服务 API Key") String apiKey) {
        // 使用 apiKey 调用邮件服务
        log.info("收件人邮箱: {}, 邮件主题：{}, API key: {}", to, subject, content, apiKey);
        return "邮件已发送至 " + to;
    }
}
