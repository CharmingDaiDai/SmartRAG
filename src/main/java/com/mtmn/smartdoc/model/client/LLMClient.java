package com.mtmn.smartdoc.model.client;

import com.mtmn.smartdoc.model.dto.ChatRequest;
import com.mtmn.smartdoc.model.dto.ChatResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * LLM客户端统一接口
 *
 * <p>设计原则:</p>
 * <ul>
 *   <li>统一不同提供商的调用方式</li>
 *   <li>支持同步和异步(流式)两种调用模式</li>
 *   <li>屏蔽底层实现细节</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
public interface LLMClient {

    /**
     * 同步聊天 - 简单文本prompt
     *
     * @param prompt 用户输入的提示词
     * @return AI生成的回复文本
     */
    String chat(String prompt);

    /**
     * 同步聊天（含 token 追踪）
     *
     * <p>若 LLM 提供商返回 tokenUsage，使用实测值；否则退而求其次进行字符级估算。</p>
     *
     * @param prompt  提示词
     * @param label   用途标签，如 "意图路由"、"任务规划"、"生成[T1]"
     * @param ledger  Token 账本，null 表示不追踪
     * @return AI 回复文本
     */
    default String chat(String prompt, String label, TokenUsageLedger ledger) {
        return chat(prompt);  // 默认降级：不追踪 token
    }

    /**
     * 同步聊天 - 结构化请求
     *
     * @param request 聊天请求对象,包含消息列表、温度等参数
     * @return 聊天响应对象, 包含回复内容和元数据
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天 - 简单文本prompt (Flux 方式)
     *
     * <p>返回一个响应式流,实时推送生成的token</p>
     *
     * @param prompt 用户输入的提示词
     * @return 流式响应, 每个元素是一个 ServerSentEvent
     */
    Flux<ServerSentEvent<String>> streamChat(String prompt);

    /**
     * 流式聊天 - 支持自定义事件处理器 (Flux 方式)
     *
     * <p>通过传入不同的 StreamEventHandler 实现不同的流式输出策略：</p>
     * <ul>
     *   <li>NoOpHandler: 普通对话，直接输出 LLM 响应</li>
     *   <li>SimpleRagEventHandler: 简单 RAG，发送基础检索步骤</li>
     *   <li>AdvancedRagEventHandler: 高级 RAG，发送完整检索流程</li>
     *   <li>自定义 Handler: 根据业务需求自由扩展</li>
     * </ul>
     *
     * @param prompt 用户输入的提示词
     * @param eventHandler 事件处理器，用于在流式输出前后插入自定义事件
     * @return 流式响应, 每个元素是一个 ServerSentEvent
     */
    Flux<ServerSentEvent<String>> streamChat(String prompt, StreamEventHandler eventHandler);

    /**
     * 流式聊天 - 简单文本prompt (SseEmitter 方式)
     *
     * <p>使用 Spring MVC SseEmitter 推送数据，解决 Nginx 缓冲问题</p>
     *
     * @param prompt 用户输入的提示词
     * @param emitter SseEmitter 发射器
     */
    void streamChatWithEmitter(String prompt, SseEmitter emitter);

    /**
     * 流式聊天 - 支持自定义事件处理器 (SseEmitter 方式)
     *
     * @param prompt 用户输入的提示词
     * @param emitter SseEmitter 发射器
     * @param eventHandler 事件处理器
     */
    void streamChatWithEmitter(String prompt, SseEmitter emitter, StreamEventHandler eventHandler);

    /**
     * 流式聊天 - 结构化请求
     *
     * @param request 聊天请求对象
     * @return 流式响应, 每个元素是一个 ServerSentEvent
     */
    Flux<ServerSentEvent<String>> streamChat(ChatRequest request);

    /**
     * 获取模型提供商类型
     *
     * @return 提供商类型, 如 "zhipuai", "qwen", "xinference", "openai"
     */
    String getProviderType();

    /**
     * 获取模型ID
     *
     * @return 模型ID, 如 "zhipuai-glm4"
     */
    String getModelId();

    /**
     * 获取模型名称
     *
     * @return 模型名称, 如 "glm-4-flash"
     */
    String getModelName();
}