package com.mtmn.smartdoc.model.client;

import com.mtmn.smartdoc.model.dto.ChatRequest;
import com.mtmn.smartdoc.model.dto.ChatResponse;
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
     * 同步聊天 - 结构化请求
     *
     * @param request 聊天请求对象,包含消息列表、温度等参数
     * @return 聊天响应对象, 包含回复内容和元数据
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天 - 简单文本prompt
     *
     * <p>返回一个响应式流,实时推送生成的token</p>
     *
     * @param prompt 用户输入的提示词
     * @return 流式响应, 每个元素是一个token片段
     */
    Flux<String> streamChat(String prompt);

    /**
     * 流式聊天 - 结构化请求
     *
     * @param request 聊天请求对象
     * @return 流式响应
     */
    Flux<String> streamChat(ChatRequest request);

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