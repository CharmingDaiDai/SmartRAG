package com.mtmn.smartdoc.model.client;

import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.FluxSink;

/**
 * 流式事件处理器接口
 * 
 * <p>用于在流式聊天过程中插入自定义的事件（如 RAG 检索步骤、思考过程等）</p>
 * <p>设计思想：将流式输出的控制权交给调用方，支持不同的 RAG 策略</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-11-26
 */
public interface StreamEventHandler {

    /**
     * 在开始 LLM 调用之前触发 (Flux 方式)
     * 
     * <p>典型用途：</p>
     * <ul>
     *   <li>RAG 场景：发送检索步骤（解析意图、检索向量库、重排序等）</li>
     *   <li>Agent 场景：发送工具调用步骤</li>
     *   <li>普通对话：不发送任何内容（默认实现）</li>
     * </ul>
     *
     * @param sink Flux 的发射器，可以通过它发送自定义事件
     * @param prompt 用户输入的原始提示词
     */
    void onBeforeChat(FluxSink<ServerSentEvent<String>> sink, String prompt);

    /**
     * 在 LLM 调用完成后触发 (Flux 方式)
     * 
     * <p>典型用途：</p>
     * <ul>
     *   <li>发送完成标识</li>
     *   <li>发送引用的文档列表</li>
     *   <li>发送统计信息（token 使用量、耗时等）</li>
     * </ul>
     *
     * @param sink Flux 的发射器
     */
    void onAfterChat(FluxSink<ServerSentEvent<String>> sink);

    /**
     * 在开始 LLM 调用之前触发 (SseEmitter 方式)
     *
     * @param emitter SseEmitter 发射器
     * @param prompt 用户输入的原始提示词
     */
    default void onBeforeChat(SseEmitter emitter, String prompt) {
        // 默认不做任何处理
    }

    /**
     * 在 LLM 调用完成后触发 (SseEmitter 方式)
     *
     * @param emitter SseEmitter 发射器
     */
    default void onAfterChat(SseEmitter emitter) {
        // 默认不做任何处理
    }

    /**
     * 默认实现：不做任何处理（用于普通对话）
     */
    class NoOpHandler implements StreamEventHandler {
        @Override
        public void onBeforeChat(FluxSink<ServerSentEvent<String>> sink, String prompt) {
            // 不发送任何事件
        }

        @Override
        public void onAfterChat(FluxSink<ServerSentEvent<String>> sink) {
            // 不发送任何事件
        }
    }
}
