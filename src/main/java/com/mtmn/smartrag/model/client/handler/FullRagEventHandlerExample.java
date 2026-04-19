package com.mtmn.smartrag.model.client.handler;

import com.mtmn.smartrag.model.client.SseEventBuilder;
import com.mtmn.smartrag.model.client.StreamEventHandler;
import com.mtmn.smartrag.model.dto.ReferenceDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.FluxSink;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 完整的 RAG 事件处理器示例
 *
 * <p>演示完整的 RAG 流程：查询扩展 → 检索 → 重排序 → 发送文档 → 生成回答</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-11-26
 */
@Slf4j
public class FullRagEventHandlerExample implements StreamEventHandler {

    private final List<ReferenceDocument> documents;

    public FullRagEventHandlerExample(List<ReferenceDocument> documents) {
        this.documents = documents;
    }

    @Override
    public void onBeforeChat(FluxSink<ServerSentEvent<String>> sink, String prompt) {
        log.debug("开始完整 RAG 流程");
        long time = 500;
        // 步骤1: 查询扩展
        ServerSentEvent<String> event1 = SseEventBuilder.buildThoughtEvent("processing", "正在扩展查询语句...", "edit");
        if (event1 != null) {
            sink.next(event1);
        }
        sleep(time);

        // 步骤2: 意图识别
        ServerSentEvent<String> event2 = SseEventBuilder.buildThoughtEvent("processing", "正在分析查询意图...", "search");
        if (event2 != null) {
            sink.next(event2);
        }
        sleep(time);

        // 步骤3: 向量检索
        ServerSentEvent<String> event3 = SseEventBuilder.buildThoughtEvent("processing", "正在执行向量检索...", "search");
        if (event3 != null) {
            sink.next(event3);
        }
        sleep(time);

        // 步骤4: 关键词检索
        ServerSentEvent<String> event4 = SseEventBuilder.buildThoughtEvent("processing", "正在执行关键词检索...", "search");
        if (event4 != null) {
            sink.next(event4);
        }
        sleep(time);

        // 步骤5: 混合检索结果
        ServerSentEvent<String> event5 = SseEventBuilder.buildThoughtEvent("processing", "正在融合检索结果...", "merge");
        if (event5 != null) {
            sink.next(event5);
        }
        sleep(time);

        // 步骤6: 重排序
        ServerSentEvent<String> event6 = SseEventBuilder.buildThoughtEvent("processing", "正在对结果重排序...", "sort");
        if (event6 != null) {
            sink.next(event6);
        }
        sleep(time);

        // 步骤7: 检索完成
        String message = String.format("检索完成，找到 %d 个高相关度文档", documents.size());
        ServerSentEvent<String> event7 = SseEventBuilder.buildThoughtEvent("success", message, "check");
        if (event7 != null) {
            sink.next(event7);
        }
        sleep(time);

        // 步骤8: 发送参考文档
        if (!documents.isEmpty()) {
            ServerSentEvent<String> refEvent = SseEventBuilder.buildRefEvent(documents);
            if (refEvent != null) {
                sink.next(refEvent);
            }
        }
        sleep(time);
    }

    @Override
    public void onAfterChat(FluxSink<ServerSentEvent<String>> sink) {
        // 发送完成标识
        ServerSentEvent<String> doneEvent = SseEventBuilder.buildDoneEvent();
        sink.next(doneEvent);
    }

    @Override
    public void onBeforeChat(SseEmitter emitter, String prompt) {
        log.debug("开始完整 RAG 流程 (SseEmitter)");
        long time = 1;

        // 步骤1: 查询扩展
        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在扩展查询语句...", "edit");
        sleep(time);

        // 步骤2: 意图识别
        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
        sleep(time);

        // 步骤3: 向量检索
        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在执行向量检索...", "search");
        sleep(time);

        // 步骤4: 关键词检索
        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在执行关键词检索...", "search");
        sleep(time);

        // 步骤5: 混合检索结果
        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在融合检索结果...", "merge");
        sleep(time);

        // 步骤6: 重排序
        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在对结果重排序...", "sort");
        sleep(time);

        // 步骤7: 检索完成
        String message = String.format("检索完成，找到 %d 个高相关度文档", documents.size());
        SseEventBuilder.sendThoughtEvent(emitter, "success", message, "check");
        sleep(time);

        // 步骤8: 发送参考文档
        if (!documents.isEmpty()) {
            SseEventBuilder.sendRefEvent(emitter, documents);
        }
        sleep(time);
    }

    @Override
    public void onAfterChat(SseEmitter emitter) {
        // AbstractLLMClient 会自动发送 done 事件，这里不需要重复发送
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 示例用法
     */
    public static void main(String[] args) {
        // 模拟检索到的文档
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

        // 创建 Handler
        StreamEventHandler handler = new FullRagEventHandlerExample(docs);

        // 使用示例（在实际代码中）
        // Flux<String> response = llmClient.streamChat("什么是策略模式", handler);

        log.info("完整 RAG 事件处理器示例创建成功，包含 {} 个参考文档", docs.size());
    }
}