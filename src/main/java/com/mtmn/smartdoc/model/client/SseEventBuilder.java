package com.mtmn.smartdoc.model.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * SSE 事件构建器
 *
 * <p>使用 Spring 的 ServerSentEvent 构建标准的 SSE 事件</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-11-26
 */
@Slf4j
public class SseEventBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 构建思考事件
     * event: thought
     * data: {"status": "processing|success", "content": "...", "icon": "..."}
     */
    public static ServerSentEvent<String> buildThoughtEvent(String status, String content, String icon) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("status", status);
            data.put("content", content);
            data.put("icon", icon);

            String json = OBJECT_MAPPER.writeValueAsString(data);
            return ServerSentEvent.<String>builder()
                    .event("thought")
                    .data(json)
                    .build();
        } catch (Exception e) {
            log.error("构建思考事件失败", e);
            return null;
        }
    }

    /**
     * 构建消息增量事件
     * event: message
     * data: {"delta": "..."}
     */
    public static ServerSentEvent<String> buildMessageEvent(String delta) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("delta", delta);

            String json = OBJECT_MAPPER.writeValueAsString(data);
            return ServerSentEvent.<String>builder()
                    .event("message")
                    .data(json)
                    .build();
        } catch (Exception e) {
            log.error("构建消息事件失败", e);
            return null;
        }
    }

    /**
     * 构建完成事件
     * event: done
     * data: [DONE]
     */
    public static ServerSentEvent<String> buildDoneEvent() {
        return ServerSentEvent.<String>builder()
                .event("done")
                .data("[DONE]")
                .build();
    }

    /**
     * 构建参考文档事件
     * event: ref
     * data: [{"title": "文档A.pdf", "score": 0.9, "content": "文档内容..."}]
     *
     * @param documents 文档列表，每个文档包含 title、score、content 字段
     */
    public static ServerSentEvent<String> buildRefEvent(java.util.List<?> documents) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(documents);
            return ServerSentEvent.<String>builder()
                    .event("ref")
                    .data(json)
                    .build();
        } catch (Exception e) {
            log.error("构建参考文档事件失败", e);
            return null;
        }
    }

    /**
     * 构建错误事件
     * event: error
     * data: {"error": "..."}
     */
    public static ServerSentEvent<String> buildErrorEvent(String errorMessage) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("error", errorMessage);

            String json = OBJECT_MAPPER.writeValueAsString(data);
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data(json)
                    .build();
        } catch (Exception e) {
            log.error("构建错误事件失败", e);
            return ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\":\"构建错误消息失败\"}")
                    .build();
        }
    }

    /**
     * 构建自定义事件
     * event: {eventType}
     * data: {jsonData}
     */
    public static ServerSentEvent<String> buildCustomEvent(String eventType, Object data) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(data);
            return ServerSentEvent.<String>builder()
                    .event(eventType)
                    .data(json)
                    .build();
        } catch (Exception e) {
            log.error("构建自定义事件失败: eventType={}", eventType, e);
            return null;
        }
    }

    /**
     * 发送思考事件 (SseEmitter)
     */
    public static void sendThoughtEvent(SseEmitter emitter, String status, String content, String icon) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("status", status);
            data.put("content", content);
            data.put("icon", icon);

            String json = OBJECT_MAPPER.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                    .name("thought")
                    .data(json)
            );
        } catch (Exception e) {
            log.error("发送思考事件失败", e);
        }
    }

    /**
     * 发送消息事件 (SseEmitter)
     */
    public static void sendMessageEvent(SseEmitter emitter, String delta) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("delta", delta);

            String json = OBJECT_MAPPER.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(json)
            );
        } catch (Exception e) {
            log.error("发送消息事件失败", e);
        }
    }

    /**
     * 发送参考文档事件 (SseEmitter)
     */
    public static void sendRefEvent(SseEmitter emitter, java.util.List<?> documents) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(documents);
            emitter.send(SseEmitter.event()
                    .name("ref")
                    .data(json)
            );
        } catch (Exception e) {
            log.error("发送参考文档事件失败", e);
        }
    }

    /**
     * 发送完成事件 (SseEmitter)
     */
    public static void sendDoneEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("done")
                    .data("[DONE]")
            );
        } catch (Exception e) {
            log.error("发送完成事件失败", e);
        }
    }
}