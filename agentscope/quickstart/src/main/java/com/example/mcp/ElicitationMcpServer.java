package com.example.mcp;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

/**
 * 功能：演示 Elicitation 的最小 stdio MCP Server（配合 McpElicitationExample 一起使用）。
 * <p>
 * 关键点：
 * <ul>
 *   <li>注册一个 <b>book_meeting</b> 工具，参数只有 topic（会议主题）。</li>
 *   <li>工具执行时通过 <code>exchange.createElicitation(...)</code> <b>反向</b>向客户端发起一次 elicitation
 *       请求 —— 询问用户邮箱（schema 形式声明），服务端拿到 ACCEPT/DECLINE/CANCEL 后再决定最终结果。</li>
 *   <li>使用 stdin/stdout 作为传输（StdioServerTransportProvider），由父进程通过 -cp 拉起本 main 即可。</li>
 * </ul>
 * <p>
 * 注意：本类被独立 JVM 子进程启动，stdout 是 MCP 协议通道，<b>禁止打印任何业务日志到 System.out</b>，
 *      所有诊断信息只能写 System.err（父进程的子进程 stderr）。
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 */
public class ElicitationMcpServer {

    public static void main(String[] args) throws Exception {
        // 1. 声明工具元数据（用 JsonSchema record 构造 input schema）
        Map<String, Object> properties = Map.of(
                "topic", Map.of(
                        "type", "string",
                        "description", "会议主题"
                )
        );
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object", properties, List.of("topic"), false, null, null);

        McpSchema.Tool bookMeetingTool = McpSchema.Tool.builder()
                .name("book_meeting")
                .description("预订一场会议。执行过程中服务端会通过 elicitation 反向向客户端索取邮箱，然后据此发出确认。")
                .inputSchema(inputSchema)
                .build();

        // 2. 工具实现：参数 -> 反向 elicitation 取邮箱 -> 返回结果
        McpServerFeatures.AsyncToolSpecification spec = new McpServerFeatures.AsyncToolSpecification(
                bookMeetingTool,
                (exchange, params) -> {
                    String topic = String.valueOf(params.getOrDefault("topic", "未命名会议"));

                    // 期望客户端返回 email 字段（标准 elicitation schema）
                    Map<String, Object> requestedSchema = Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "email", Map.of(
                                            "type", "string",
                                            "description", "请输入您的邮箱用于接收会议确认"
                                    )
                            ),
                            "required", List.of("email")
                    );

                    McpSchema.ElicitRequest elicitReq = new McpSchema.ElicitRequest(
                            "需要您的邮箱来发送《" + topic + "》的会议确认信，请补充。",
                            requestedSchema);

                    // 关键：server 反向调用 client，等用户回填
                    return exchange.createElicitation(elicitReq).map(result -> {
                        McpSchema.ElicitResult.Action action = result.action();
                        String text;
                        boolean isError = false;
                        if (action == McpSchema.ElicitResult.Action.ACCEPT) {
                            Object email = result.content() == null ? null : result.content().get("email");
                            text = "会议《" + topic + "》已预订成功，确认信将发往：" + email;
                        } else if (action == McpSchema.ElicitResult.Action.DECLINE) {
                            text = "用户拒绝提供邮箱，会议《" + topic + "》已登记但未发送确认信。";
                        } else {
                            text = "用户取消了会议《" + topic + "》的预订。";
                            isError = true;
                        }
                        return new McpSchema.CallToolResult(
                                List.of(new McpSchema.TextContent(text)), isError);
                    });
                });

        // 3. 启动 stdio MCP server
        McpServer.async(new StdioServerTransportProvider(McpJsonMapper.createDefault()))
                .serverInfo("elicit-demo-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(spec)
                .build();

        System.err.println("[ElicitationMcpServer] started, waiting for client over stdio ...");

        // 4. 阻塞主线程；stdio transport 由内部 IO 线程驱动
        Thread.currentThread().join();
    }
}
