package com.mtmn.smartdoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.enums.IndexingStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 索引进度 SSE 连接管理器
 * 管理用户的 SSE 连接，按 kbId 区分
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@Component
public class IndexingProgressEmitterManager {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * SSE 连接存储：key = "userId:kbId"
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 连接超时时间：30 分钟（索引可能耗时较长）
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 创建并注册 SSE 连接
     */
    public SseEmitter createEmitter(Long userId, Long kbId) {
        String key = buildKey(userId, kbId);

        // 如果已有连接，先关闭
        SseEmitter oldEmitter = emitters.get(key);
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception ignored) {
            }
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed: {}", key);
            emitters.remove(key);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timeout: {}", key);
            emitters.remove(key);
        });

        emitter.onError(e -> {
            log.debug("SSE connection error: {}, error: {}", key, e.getMessage());
            emitters.remove(key);
        });

        emitters.put(key, emitter);
        log.info("SSE connection created: {}", key);

        return emitter;
    }

    /**
     * 发送进度事件
     * event: progress
     * data: {"taskId": 1, "total": 10, "completed": 3, "failed": 0, "percentage": 30}
     */
    public void sendProgress(Long userId, Long kbId, Long taskId, int total, int completed, int failed) {
        SseEmitter emitter = emitters.get(buildKey(userId, kbId));
        if (emitter == null) {
            return;
        }

        try {
            int percentage = total > 0 ? (completed * 100 / total) : 0;
            Map<String, Object> data = Map.of(
                    "taskId", taskId,
                    "total", total,
                    "completed", completed,
                    "failed", failed,
                    "percentage", percentage
            );

            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(OBJECT_MAPPER.writeValueAsString(data)));
        } catch (IOException e) {
            log.error("Failed to send progress event", e);
            removeEmitter(userId, kbId);
        }
    }

    /**
     * 发送步骤事件
     * event: step
     * data: {"docId": 123, "docName": "文档A.pdf", "step": "EMBEDDING", "stepName": "向量化中"}
     */
    public void sendStep(Long userId, Long kbId, Long docId, String docName, IndexingStep step) {
        SseEmitter emitter = emitters.get(buildKey(userId, kbId));
        if (emitter == null) {
            return;
        }

        try {
            Map<String, Object> data = Map.of(
                    "docId", docId,
                    "docName", docName,
                    "step", step.name(),
                    "stepName", step.getDisplayName()
            );

            emitter.send(SseEmitter.event()
                    .name("step")
                    .data(OBJECT_MAPPER.writeValueAsString(data)));
        } catch (IOException e) {
            log.error("Failed to send step event", e);
            removeEmitter(userId, kbId);
        }
    }

    /**
     * 发送错误事件
     * event: error
     * data: {"docId": 123, "docName": "文档A.pdf", "error": "文件解析失败"}
     */
    public void sendError(Long userId, Long kbId, Long docId, String docName, String errorMessage) {
        SseEmitter emitter = emitters.get(buildKey(userId, kbId));
        if (emitter == null) {
            return;
        }

        try {
            Map<String, Object> data = Map.of(
                    "docId", docId,
                    "docName", docName,
                    "error", errorMessage
            );

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(OBJECT_MAPPER.writeValueAsString(data)));
        } catch (IOException e) {
            log.error("Failed to send error event", e);
            removeEmitter(userId, kbId);
        }
    }

    /**
     * 发送完成事件
     * event: done
     * data: {"taskId": 1, "total": 10, "completed": 9, "failed": 1}
     */
    public void sendDone(Long userId, Long kbId, Long taskId, int total, int completed, int failed) {
        SseEmitter emitter = emitters.get(buildKey(userId, kbId));
        if (emitter == null) {
            return;
        }

        try {
            Map<String, Object> data = Map.of(
                    "taskId", taskId,
                    "total", total,
                    "completed", completed,
                    "failed", failed
            );

            emitter.send(SseEmitter.event()
                    .name("done")
                    .data(OBJECT_MAPPER.writeValueAsString(data)));

            // 完成后关闭连接
            emitter.complete();
        } catch (IOException e) {
            log.error("Failed to send done event", e);
        } finally {
            removeEmitter(userId, kbId);
        }
    }

    /**
     * 移除 SSE 连接
     */
    public void removeEmitter(Long userId, Long kbId) {
        String key = buildKey(userId, kbId);
        SseEmitter emitter = emitters.remove(key);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 检查是否有活跃连接
     */
    public boolean hasActiveEmitter(Long userId, Long kbId) {
        return emitters.containsKey(buildKey(userId, kbId));
    }

    private String buildKey(Long userId, Long kbId) {
        return userId + ":" + kbId;
    }
}
