package com.example.utils;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 功能：AgentUtil
 * 作者：@SmartSi
 * 博客：https://smartsi.blog.csdn.net/
 * 公众号：大数据生态
 * 日期：2026/4/19 18:32
 */
public class AgentUtil {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void startChat(Agent agent) throws IOException {
        System.out.println("=== Chat Started ===");
        System.out.println("Type 'exit' to quit\n");

        while (true) {
            System.out.print("You> ");
            String input = reader.readLine();

            if (input == null || "exit".equalsIgnoreCase(input.trim())) {
                System.out.println("Goodbye!");
                break;
            }

            if (input.trim().isEmpty()) {
                continue;
            }

            try {
                Msg userMsg = Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(input).build())
                        .build();

                System.out.print("Agent> ");

                try {
                    // Try to use stream() first for real-time output
                    AtomicBoolean hasPrintedThinkingHeader = new AtomicBoolean(false);
                    AtomicBoolean hasPrintedTextHeader = new AtomicBoolean(false);
                    AtomicBoolean hasPrintedTextSeparator = new AtomicBoolean(false);
                    AtomicReference<String> lastThinkingContent = new AtomicReference<>("");
                    AtomicReference<String> lastTextContent = new AtomicReference<>("");

                    StreamOptions streamOptions = StreamOptions.builder()
                                    .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                                    .incremental(true)
                                    .includeReasoningResult(false)
                                    .build();

                    agent.stream(userMsg, streamOptions)
                            .doOnNext(
                                    event -> {
                                        Msg msg = event.getMessage();
                                        for (ContentBlock block : msg.getContent()) {
                                            if (block instanceof ThinkingBlock) {
                                                printStreamContent(
                                                        ((ThinkingBlock) block).getThinking(),
                                                        lastThinkingContent,
                                                        hasPrintedThinkingHeader,
                                                        "> Thinking: ",
                                                        null);
                                            } else if (block instanceof TextBlock) {
                                                printStreamContent(
                                                        ((TextBlock) block).getText(),
                                                        lastTextContent,
                                                        hasPrintedTextHeader,
                                                        "Text: ",
                                                        () -> {
                                                            if (hasPrintedThinkingHeader.get()
                                                                    && !hasPrintedTextSeparator
                                                                    .get()) {
                                                                System.out.print("\n\n");
                                                                hasPrintedTextSeparator.set(true);
                                                            }
                                                        });
                                            }
                                        }
                                    })
                            .blockLast();
                } catch (Exception e) {
                    if (e instanceof UnsupportedOperationException) {
                        System.err.println("\n[Info] Streaming not supported by this agent. Falling back to"
                                        + " call().");
                    } else {
                        System.err.println("\n[Warning] Exception during streaming: " + e.getMessage());
                        e.printStackTrace();
                        System.err.println("[Info] Falling back to call().");
                    }

                    Msg response = agent.call(userMsg).block();
                    if (response != null) {
                        String thinking = response.getContent().stream()
                                        .filter(block -> block instanceof ThinkingBlock)
                                        .map(block -> ((ThinkingBlock) block).getThinking())
                                        .collect(Collectors.joining("\n"));

                        String text = response.getContent().stream()
                                        .filter(block -> block instanceof TextBlock)
                                        .map(block -> ((TextBlock) block).getText())
                                        .collect(Collectors.joining("\n"));

                        boolean hasContent = false;
                        if (!thinking.isEmpty()) {
                            System.out.print("> Thinking: " + thinking);
                            hasContent = true;
                        }
                        if (!text.isEmpty()) {
                            if (hasContent) {
                                System.out.print("\n\n");
                            }
                            System.out.print("Text: " + text);
                            hasContent = true;
                        }
                        if (!hasContent) {
                            System.out.print("[No response]");
                        }
                    }
                }

                System.out.println("\n");

            } catch (Exception e) {
                System.err.println("\nError: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void printStreamContent(String content, AtomicReference<String> lastContentRef,
            AtomicBoolean hasPrintedHeaderRef, String header, Runnable prePrintAction) {
        String lastContent = lastContentRef.get();
        String toPrint;

        if (content.startsWith(lastContent)) {
            toPrint = content.substring(lastContent.length());
            lastContentRef.set(content);
        } else {
            toPrint = content;
            lastContentRef.set(lastContent + content);
        }

        if (!toPrint.isEmpty()) {
            if (prePrintAction != null) {
                prePrintAction.run();
            }

            if (!hasPrintedHeaderRef.get()) {
                System.out.print(header);
                hasPrintedHeaderRef.set(true);
            }
            System.out.print(toPrint);
            System.out.flush();
        }
    }
}
