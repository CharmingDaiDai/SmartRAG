package com.mtmn.smartrag.model.client.handler;

import com.mtmn.smartrag.model.client.SseEventBuilder;
import com.mtmn.smartrag.model.client.StreamEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.FluxSink;

/**
 * 高级 RAG 事件处理器
 * 
 * <p>发送完整的检索步骤：查询扩展 → 多路检索 → 重排序 → 生成回答</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-11-26
 */
@Slf4j
public class AdvancedRagEventHandler implements StreamEventHandler {

    @Override
    public void onBeforeChat(FluxSink<ServerSentEvent<String>> sink, String prompt) {
        log.debug("开始 Advanced RAG 流程");
        
        // 步骤1: 查询扩展
        ServerSentEvent<String> event1 = SseEventBuilder.buildThoughtEvent("processing", "正在扩展查询语句...", "edit");
        if (event1 != null) sink.next(event1);
        sleep(100);
        
        // 步骤2: 意图识别
        ServerSentEvent<String> event2 = SseEventBuilder.buildThoughtEvent("processing", "正在分析查询意图...", "search");
        if (event2 != null) sink.next(event2);
        sleep(100);
        
        // 步骤3: 多路检索
        ServerSentEvent<String> event3 = SseEventBuilder.buildThoughtEvent("processing", "正在执行多路检索（向量+关键词）...", "search");
        if (event3 != null) sink.next(event3);
        sleep(100);
        
        // 步骤4: 重排序
        ServerSentEvent<String> event4 = SseEventBuilder.buildThoughtEvent("processing", "正在对检索结果重排序...", "sort");
        if (event4 != null) sink.next(event4);
        sleep(100);
        
        // 步骤5: 准备生成
        ServerSentEvent<String> event5 = SseEventBuilder.buildThoughtEvent("success", "检索完成，正在生成回答...", "check");
        if (event5 != null) sink.next(event5);
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
