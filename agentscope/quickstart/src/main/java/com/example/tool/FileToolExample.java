package com.example.tool;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.example.bean.Constant.MODEL_NAME;

/**
 * 功能：文件读写工具示例（使用内置文件工具）
 * 场景：让 LLM 通过 AgentScope 内置的文件工具完成本地文件的"读 / 写 / 插入 / 列目录"操作。
 *      内置工具内部已经实现了路径校验、负数行号、按行号读写、目录列举等能力，
 *      并通过 baseDir 把所有操作限制在工作目录内，避免 LLM 越权访问宿主文件系统。
 *
 * 内置工具集：
 *   io.agentscope.core.tool.file.ReadFileTool
 *     - view_text_file (file_path, ranges?)        读取文件内容（可指定行区间，支持负数）
 *     - list_directory (dir_path)                  列出目录下的文件与子目录
 *   io.agentscope.core.tool.file.WriteFileTool
 *     - write_text_file (file_path, content, ranges?)   创建/覆盖/区间替换文件内容
 *     - insert_text_file (file_path, content, line_number) 在指定行号插入新内容
 *
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/5/4 17:30
 */
public class FileToolExample {

    public static void main(String[] args) throws IOException {
        // 1. 准备沙箱工作目录（target/file-tool-workspace）
        //    内置工具会把所有 file_path 解析到 baseDir 内，越界会直接报错
        Path workspace = Paths.get("agentscope/quickstart/target/file-tool-workspace")
                .toAbsolutePath().normalize();
        Files.createDirectories(workspace);
        String baseDir = workspace.toString();

        // 2. 注册内置文件工具到 Toolkit
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ReadFileTool(baseDir));
        toolkit.registerTool(new WriteFileTool(baseDir));

        // 3. 创建 Agent，系统提示中说明可用的内置工具
        ReActAgent agent = ReActAgent.builder()
                .name("文件助手")
                .sysPrompt("你是一个文件操作助手，完成本地文件读写。请完成用户请求，并把最终结果用一句话告知用户。")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                        .modelName(MODEL_NAME)
                        .build())
                .toolkit(toolkit)
                .build();

        // 4. 调用智能体：完成"先写再插再读"的组合任务
        //    预计调用链：write_text_file → insert_text_file → view_text_file
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("把内容《今日学习：AgentScope 工具调用》写入 notes.txt，"
                        + "再在文件末尾插入一行《明日计划：练习 ReActAgent》，"
                        + "最后读取并把完整内容返回给我。")
                .build();

        Msg response = agent.call(msg).block();
        if (response != null) {
            System.out.println("助手: " + response.getTextContent());
        }
    }
}
