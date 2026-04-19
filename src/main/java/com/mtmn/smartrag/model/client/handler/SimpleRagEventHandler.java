package com.mtmn.smartrag.model.client.handler;

import com.mtmn.smartrag.model.client.SseEventBuilder;
import com.mtmn.smartrag.model.client.StreamEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * 简单 RAG 事件处理器
 * 
 * <p>发送基础的检索步骤：意图解析 → 向量检索 → 生成回答</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-11-26
 */
@Slf4j
public class SimpleRagEventHandler implements StreamEventHandler {

    @Override
    public void onBeforeChat(FluxSink<ServerSentEvent<String>> sink, String prompt) {
        log.debug("开始 Simple RAG 流程");
        
        // 步骤1: 意图解析
        ServerSentEvent<String> event1 = SseEventBuilder.buildThoughtEvent("processing", "正在解析用户意图...", "search");
        if (event1 != null) sink.next(event1);
        sleep(100);  // 100ms 延迟确保立即发送
        
        // 步骤2: 向量检索
        ServerSentEvent<String> event2 = SseEventBuilder.buildThoughtEvent("processing", "正在检索向量数据库...", "search");
        if (event2 != null) sink.next(event2);
        sleep(100);
        
        // 步骤3: 准备生成
        ServerSentEvent<String> event3 = SseEventBuilder.buildThoughtEvent("success", "检索完成，正在生成回答...", "check");
        if (event3 != null) sink.next(event3);
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
