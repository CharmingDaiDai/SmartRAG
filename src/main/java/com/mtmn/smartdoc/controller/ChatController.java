package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.model.client.StreamEventHandler;
import com.mtmn.smartdoc.model.client.handler.AdvancedRagEventHandler;
import com.mtmn.smartdoc.model.client.handler.FullRagEventHandlerExample;
import com.mtmn.smartdoc.model.client.handler.SimpleRagEventHandler;
import com.mtmn.smartdoc.model.dto.ReferenceDocument;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author charmingdaidai
 * @version 2.0
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "对话相关 API")
public class ChatController {

    private final ModelFactory modelFactory;

    @Operation(summary = "普通对话测试", description = "返回 SSE 格式流（纯文本）")
    @PostMapping(value = "/test/normal", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> testNormalChat(@AuthenticationPrincipal User user) {
        log.info("收到普通对话测试请求");
        // 使用默认的 NoOpHandler，直接返回文本流
        return modelFactory.createDefaultLLMClient()
                .streamChat("什么是 RAG");
    }

    @Operation(summary = "简单 RAG 对话测试", description = "返回 SSE 格式流（包含基础检索步骤）")
    @PostMapping(value = "/test/simple-rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> testSimpleRagChat(@AuthenticationPrincipal User user) {
        log.info("收到简单 RAG 对话测试请求");
        // 使用 SimpleRagEventHandler，发送基础检索步骤
        return modelFactory.createDefaultLLMClient()
                .streamChat("什么是 RAG", new SimpleRagEventHandler());
    }

    @Operation(summary = "高级 RAG 对话测试", description = "返回 SSE 格式流（包含完整检索流程）")
    @PostMapping(value = "/test/advanced-rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> testAdvancedRagChat(@AuthenticationPrincipal User user) {
        log.info("收到高级 RAG 对话测试请求");
        // 使用 AdvancedRagEventHandler，发送完整检索流程
        return modelFactory.createDefaultLLMClient()
                .streamChat("什么是 RAG", new AdvancedRagEventHandler());
    }

    @Operation(summary = "完整 RAG 演示", description = "演示完整的 RAG 流程（带参考文档）")
    @PostMapping(value = "/test/full-rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> testFullRagChat(@AuthenticationPrincipal User user, HttpServletResponse response) {

        response.setHeader("X-Accel-Buffering", "no"); // Nginx
        response.setHeader("Content-Encoding", "none"); // 禁用 Gzip
        response.setHeader("Cache-Control", "no-cache");

        List<ReferenceDocument> docs = Arrays.asList(
                ReferenceDocument.builder()
                        .title("国家电网有限公司输变电工程标准工艺（变电工程电气分册）2022版.md")
                        .score(0.95)
                        .content("1.1 总则\n本标准工艺适用于110kV及以上电压等级变电工程...")
                        .documentId(1001L)
                        .chunkId("chunk_001")
                        .build(),

                ReferenceDocument.builder()
                        .title("设计模式.md")
                        .score(0.87)
                        .content("策略模式（Strategy Pattern）：定义一系列算法，将每一个算法封装起来...")
                        .documentId(1002L)
                        .chunkId("chunk_005")
                        .build(),

                ReferenceDocument.builder()
                        .title("MySQL.md")
                        .score(0.82)
                        .content("事务的ACID特性：原子性（Atomicity）、一致性（Consistency）...")
                        .documentId(1003L)
                        .chunkId("chunk_012")
                        .build()
        );

        StreamEventHandler handler = new FullRagEventHandlerExample(docs);

        return modelFactory.createDefaultLLMClient()
                .streamChat("什么是 RAG", handler);
    }

    @Operation(summary = "完整 RAG 演示 (SseEmitter)", description = "演示完整的 RAG 流程（带参考文档），使用 SseEmitter 解决缓冲问题")
    @PostMapping(value = "/test/full-rag-emitter")
    public SseEmitter testFullRagChatWithEmitter(@AuthenticationPrincipal User user, HttpServletResponse response) {
        log.info("收到完整 RAG 对话测试请求 (SseEmitter)");

        // 设置响应头禁用缓冲
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Content-Encoding", "none");
        response.setHeader("Cache-Control", "no-cache");

        // 创建 SseEmitter，设置超时时间 (例如 5 分钟)
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        List<ReferenceDocument> docs = Arrays.asList(
                ReferenceDocument.builder()
                        .title("国家电网有限公司输变电工程标准工艺（变电工程电气分册）2022版.md")
                        .score(0.95)
                        .content("1.1 总则\n本标准工艺适用于110kV及以上电压等级变电工程...")
                        .documentId(1001L)
                        .chunkId("chunk_001")
                        .build(),

                ReferenceDocument.builder()
                        .title("设计模式.md")
                        .score(0.87)
                        .content("策略模式（Strategy Pattern）：定义一系列算法，将每一个算法封装起来...")
                        .documentId(1002L)
                        .chunkId("chunk_005")
                        .build(),

                ReferenceDocument.builder()
                        .title("MySQL.md")
                        .score(0.82)
                        .content("事务的ACID特性：原子性（Atomicity）、一致性（Consistency）...")
                        .documentId(1003L)
                        .chunkId("chunk_012")
                        .build()
        );

        StreamEventHandler handler = new FullRagEventHandlerExample(docs);

        modelFactory.createDefaultLLMClient()
                .streamChatWithEmitter("什么是 RAG", emitter, handler);

        return emitter;
    }
}