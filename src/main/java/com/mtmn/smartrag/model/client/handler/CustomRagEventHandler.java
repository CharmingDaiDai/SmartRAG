package com.mtmn.smartrag.model.client.handler;

import com.mtmn.smartrag.model.client.SseEventBuilder;
import com.mtmn.smartrag.model.client.StreamEventHandler;
import com.mtmn.smartrag.model.dto.ReferenceDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

import java.util.List;

/**
 * 自定义 RAG 事件处理器（带检索结果）
 * 
 * <p>在流式输出前发送实际的检索结果，让前端可以展示引用的文档</p>
 * <p>使用方式：</p>
 * <pre>{@code
 * List<ReferenceDocument> retrievedDocs = Arrays.asList(
 *     ReferenceDocument.builder().title("文档1.pdf").score(0.95).content("内容...").build(),
 *     ReferenceDocument.builder().title("文档2.pdf").score(0.87).content("内容...").build()
 * );
 * StreamEventHandler handler = new CustomRagEventHandler(retrievedDocs);
 * Flux<ServerSentEvent<String>> response = llmClient.streamChat(prompt, handler);
 * }</pre>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-11-26
 */
@Slf4j
@RequiredArgsConstructor
public class CustomRagEventHandler implements StreamEventHandler {

    private final List<ReferenceDocument> retrievedDocs;
    private final boolean showDetailedSteps;

    public CustomRagEventHandler(List<ReferenceDocument> retrievedDocs) {
        this(retrievedDocs, true);
    }

    @Override
    public void onBeforeChat(FluxSink<ServerSentEvent<String>> sink, String prompt) {
        log.debug("开始 Custom RAG 流程，检索到 {} 个文档", retrievedDocs != null ? retrievedDocs.size() : 0);
        
        if (showDetailedSteps) {
            // 步骤1: 意图解析
            ServerSentEvent<String> event1 = SseEventBuilder.buildThoughtEvent("processing", "正在解析查询意图...", "search");
            if (event1 != null) sink.next(event1);
            sleep(200);
            
            // 步骤2: 向量检索
            ServerSentEvent<String> event2 = SseEventBuilder.buildThoughtEvent("processing", "正在检索向量数据库...", "search");
            if (event2 != null) sink.next(event2);
            sleep(300);
            
            // 步骤3: 检索完成，发送文档数量
            String message = String.format("检索完成，找到 %d 个相关文档", 
                    retrievedDocs != null ? retrievedDocs.size() : 0);
            ServerSentEvent<String> event3 = SseEventBuilder.buildThoughtEvent("success", message, "check");
            if (event3 != null) sink.next(event3);
        }
        
        // 发送检索到的参考文档（使用 ref 事件）
        if (retrievedDocs != null && !retrievedDocs.isEmpty()) {
            ServerSentEvent<String> refEvent = SseEventBuilder.buildRefEvent(retrievedDocs);
            if (refEvent != null) sink.next(refEvent);
        }
    }

    @Override
    public void onAfterChat(FluxSink<ServerSentEvent<String>> sink) {
        // 发送完成标识
        ServerSentEvent<String> doneEvent = SseEventBuilder.buildDoneEvent();
        if (doneEvent != null) sink.next(doneEvent);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
