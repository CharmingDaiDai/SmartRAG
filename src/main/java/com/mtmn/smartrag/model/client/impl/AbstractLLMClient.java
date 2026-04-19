package com.mtmn.smartrag.model.client.impl;

import com.mtmn.smartrag.model.client.LLMClient;
import com.mtmn.smartrag.model.client.SseEventBuilder;
import com.mtmn.smartrag.model.client.StreamEventHandler;
import com.mtmn.smartrag.model.client.TokenUsageLedger;
import com.mtmn.smartrag.model.config.ModelProperties;
import com.mtmn.smartrag.model.dto.ChatRequest;
import com.mtmn.smartrag.model.dto.ChatResponse;
import com.mtmn.smartrag.rag.support.RagStreamErrorMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM客户端抽象基类
 *
 * <p>提供通用的LLM调用实现，子类只需实现模型构建方法</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
@Slf4j
public abstract class AbstractLLMClient implements LLMClient {

    /** 速率限制最大重试次数 */
    private static final int RATE_LIMIT_MAX_RETRIES = 3;
    /** 速率限制初始退避时间（毫秒）*/
    private static final long RATE_LIMIT_INITIAL_BACKOFF_MS = 2000;

    protected final String providerName;
    protected final ModelProperties.ProviderConfig providerConfig;
    protected final ModelProperties.ModelConfig modelConfig;
    protected final ChatModel chatModel;
    protected final StreamingChatModel streamingModel;

    protected AbstractLLMClient(
            String providerName,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        this.providerName = providerName;
        this.providerConfig = providerConfig;
        this.modelConfig = modelConfig;

        log.info("初始化LLM客户端: provider={}, model={}",
                providerName, modelConfig.getName());

        this.chatModel = buildChatModel();
        this.streamingModel = buildStreamingModel();
    }

    /**
     * 构建同步聊天模型
     * 子类实现具体的模型构建逻辑
     */
    protected abstract ChatModel buildChatModel();

    /**
     * 构建流式聊天模型
     * 子类实现具体的模型构建逻辑
     */
    protected abstract StreamingChatModel buildStreamingModel();

    @Override
    public String chat(String prompt) {
        log.debug("同步聊天请求: prompt长度={}", prompt.length());

        for (int attempt = 0; attempt <= RATE_LIMIT_MAX_RETRIES; attempt++) {
            try {
                String response = chatModel.chat(prompt);
                log.debug("同步聊天响应: response长度={}", response.length());
                return response;
            } catch (Exception e) {
                if (isRateLimitError(e) && attempt < RATE_LIMIT_MAX_RETRIES) {
                    rateLimitBackoff(attempt + 1);
                    continue;
                }
                log.error("同步聊天失败: {}", e.getMessage(), e);
                throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("LLM调用失败: 超过最大重试次数");
    }

    @Override
    public String chat(String prompt, String label, TokenUsageLedger ledger) {
        log.debug("同步聊天请求（追踪）: label={}, prompt长度={}", label, prompt.length());

        for (int attempt = 0; attempt <= RATE_LIMIT_MAX_RETRIES; attempt++) {
            try {
                long startTime = System.currentTimeMillis();
                dev.langchain4j.model.chat.response.ChatResponse response =
                        chatModel.chat(List.of(UserMessage.from(prompt)));
                long durationMs = System.currentTimeMillis() - startTime;
                String text = response.aiMessage().text();
                if (ledger != null) {
                    int inputTokens, outputTokens, totalTokens;
                    if (response.tokenUsage() != null) {
                        inputTokens  = response.tokenUsage().inputTokenCount();
                        outputTokens = response.tokenUsage().outputTokenCount();
                        totalTokens  = response.tokenUsage().totalTokenCount();
                    } else {
                        inputTokens  = estimateTokens(prompt);
                        outputTokens = estimateTokens(text);
                        totalTokens  = inputTokens + outputTokens;
                        log.debug("Token 估算 [{}]: input≈{}, output≈{}", label, inputTokens, outputTokens);
                    }
                    ledger.record(label, inputTokens, outputTokens, totalTokens, durationMs);
                }
                return text;
            } catch (Exception e) {
                if (isRateLimitError(e) && attempt < RATE_LIMIT_MAX_RETRIES) {
                    rateLimitBackoff(attempt + 1);
                    continue;
                }
                log.error("同步聊天失败: {}", e.getMessage(), e);
                throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("LLM调用失败: 超过最大重试次数");
    }

    /**
     * 估算文本 token 数（当 LLM 未返回 tokenUsage 时的兜底）。
     * CJK 字符约 1 token/字；ASCII 连续序列约 1 token/4 字符（词级估算）。
     */
    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        int asciiSeq = 0;
        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                if (asciiSeq > 0) { count += Math.max(1, asciiSeq / 4); asciiSeq = 0; }
                count++;
            } else if (Character.isWhitespace(c)) {
                if (asciiSeq > 0) { count += Math.max(1, asciiSeq / 4); asciiSeq = 0; }
            } else {
                asciiSeq++;
            }
        }
        if (asciiSeq > 0) count += Math.max(1, asciiSeq / 4);
        return Math.max(1, count);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.debug("结构化聊天请求: messages数量={}", request.getMessages().size());

        List<ChatMessage> messages = convertMessages(request.getMessages());

        for (int attempt = 0; attempt <= RATE_LIMIT_MAX_RETRIES; attempt++) {
            try {
                // 调用模型,返回 LangChain4j 的 ChatResponse
                dev.langchain4j.model.chat.response.ChatResponse langchainResponse = chatModel.chat(messages);

                // 构建我们的 ChatResponse DTO
                return ChatResponse.builder()
                        .content(langchainResponse.aiMessage().text())
                        .model(modelConfig.getName())
                        .finishReason(langchainResponse.finishReason() != null ?
                                langchainResponse.finishReason().toString() : null)
                        .tokenUsage(langchainResponse.tokenUsage() != null ?
                                ChatResponse.TokenUsage.builder()
                                        .promptTokens(langchainResponse.tokenUsage().inputTokenCount())
                                        .completionTokens(langchainResponse.tokenUsage().outputTokenCount())
                                        .totalTokens(langchainResponse.tokenUsage().totalTokenCount())
                                        .build() : null)
                        .build();
            } catch (Exception e) {
                if (isRateLimitError(e) && attempt < RATE_LIMIT_MAX_RETRIES) {
                    rateLimitBackoff(attempt + 1);
                    continue;
                }
                log.error("结构化聊天失败: {}", e.getMessage(), e);
                throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
            }
        }
        throw new RuntimeException("LLM调用失败: 超过最大重试次数");
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChat(String prompt) {
        // 使用默认的 NoOpHandler（不发送任何额外事件，普通对话模式）
        return streamChat(prompt, new StreamEventHandler.NoOpHandler());
    }

    /**
     * 流式聊天 - 支持自定义事件处理器
     *
     * <p>通过传入不同的 StreamEventHandler 实现不同的流式输出策略：</p>
     * <ul>
     *   <li>NoOpHandler: 普通对话，直接输出 LLM 响应</li>
     *   <li>SimpleRagEventHandler: 简单 RAG，发送基础检索步骤</li>
     *   <li>AdvancedRagEventHandler: 高级 RAG，发送完整检索流程</li>
     *   <li>自定义 Handler: 根据业务需求自由扩展</li>
     * </ul>
     *
     * @param prompt       用户输入的提示词
     * @param eventHandler 事件处理器，用于在流式输出前后插入自定义事件
     * @return 流式响应
     */
    @Override
    public Flux<ServerSentEvent<String>> streamChat(String prompt, StreamEventHandler eventHandler) {
        log.debug("流式聊天请求: prompt长度={}, handler={}", prompt.length(), eventHandler.getClass().getSimpleName());

        return Flux.<ServerSentEvent<String>>create(sink -> {
                    try {
                        // 步骤1: 调用事件处理器的 onBeforeChat（RAG 检索步骤会在这里发送）
                        eventHandler.onBeforeChat(sink, prompt);

                        // 步骤2: 开始真正的流式聊天
                        streamingModel.chat(prompt, new StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(String partialResponse) {
                                // 过滤null值,防止sink.next(null)异常
                                if (partialResponse != null && !partialResponse.isEmpty()) {
                                    log.trace("LLM分片响应: {}", partialResponse);
                                    // 普通对话或 RAG 对话都使用 SSE 格式
                                    ServerSentEvent<String> event;
                                    if (eventHandler instanceof StreamEventHandler.NoOpHandler) {
                                        // 普通对话：只发送 delta，不带 event 类型（使用默认的 message）
                                        event = ServerSentEvent.<String>builder()
                                                .data(partialResponse)
                                                .build();
                                    } else {
                                        // RAG 对话：封装为 message 事件
                                        event = SseEventBuilder.buildMessageEvent(partialResponse);
                                    }
                                    if (event != null) {
                                        sink.next(event);
                                    }
                                }
                            }

                            @Override
                            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                                log.info("流式聊天完成");
                                // 步骤3: 调用事件处理器的 onAfterChat（发送完成标识等）
                                eventHandler.onAfterChat(sink);
                                sink.complete();
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("流式聊天失败: {}", error.getMessage(), error);
                                if (!sink.isCancelled()) {
                                    if (!(eventHandler instanceof StreamEventHandler.NoOpHandler)) {
                                        ServerSentEvent<String> errorEvent = SseEventBuilder.buildErrorEvent(error.getMessage());
                                        if (errorEvent != null) {
                                            sink.next(errorEvent);
                                        }
                                    }
                                    sink.error(new RuntimeException("LLM流式调用失败: " + error.getMessage(), error));
                                }
                            }
                        });
                    } catch (Exception e) {
                        log.error("流式聊天启动失败: {}", e.getMessage(), e);
                        if (!sink.isCancelled()) {
                            if (!(eventHandler instanceof StreamEventHandler.NoOpHandler)) {
                                ServerSentEvent<String> errorEvent = SseEventBuilder.buildErrorEvent(e.getMessage());
                                if (errorEvent != null) {
                                    sink.next(errorEvent);
                                }
                            }
                            sink.error(new RuntimeException("LLM流式调用启动失败: " + e.getMessage(), e));
                        }
                    }
                }, FluxSink.OverflowStrategy.BUFFER)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
        log.debug("结构化流式聊天请求: messages数量={}", request.getMessages().size());

        return Flux.create(sink -> {
            try {
                // 转换消息格式
                List<ChatMessage> messages = convertMessages(request.getMessages());

                streamingModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        // 过滤null值,防止sink.next(null)异常
                        if (partialResponse != null && !partialResponse.isEmpty()) {
                            log.trace("结构化LLM分片响应: {}", partialResponse);
                            // 结构化请求统一使用纯文本流
                            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                                    .data(partialResponse)
                                    .build();
                            sink.next(event);
                        }
                    }

                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        log.info("结构化流式聊天完成");
                        sink.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        log.error("结构化流式聊天失败: {}", error.getMessage(), error);
                        if (!sink.isCancelled()) {
                            sink.error(new RuntimeException("LLM流式调用失败: " + error.getMessage(), error));
                        }
                    }
                });
            } catch (Exception e) {
                log.error("结构化流式聊天启动失败: {}", e.getMessage(), e);
                if (!sink.isCancelled()) {
                    sink.error(new RuntimeException("LLM流式调用启动失败: " + e.getMessage(), e));
                }
            }
        });
    }

    @Override
    public String getModelId() {
        // 格式: provider@modelName
        return providerName + "@" + modelConfig.getName();
    }

    @Override
    public String getModelName() {
        return modelConfig.getName();
    }

    @Override
    public String getProviderType() {
        return providerName;
    }

    /**
     * 转换消息格式
     */
    private List<ChatMessage> convertMessages(List<ChatRequest.Message> messages) {
        List<ChatMessage> result = new ArrayList<>();

        for (ChatRequest.Message msg : messages) {
            ChatMessage chatMessage = switch (msg.getRole().toLowerCase()) {
                case "system" -> SystemMessage.from(msg.getContent());
                case "user" -> UserMessage.from(msg.getContent());
                case "assistant" -> AiMessage.from(msg.getContent());
                default -> throw new IllegalArgumentException("未知的消息角色: " + msg.getRole());
            };
            result.add(chatMessage);
        }

        return result;
    }

    @Override
    public void streamChatWithEmitter(String prompt, SseEmitter emitter) {
        streamChatWithEmitter(prompt, emitter, new StreamEventHandler.NoOpHandler());
    }

    @Override
    public void streamChatWithEmitter(String prompt, SseEmitter emitter, StreamEventHandler eventHandler) {
        log.debug("流式聊天请求(Emitter): prompt长度={}, handler={}", prompt.length(), eventHandler.getClass().getSimpleName());

        CompletableFuture.runAsync(() -> doStreamWithRetry(prompt, emitter, eventHandler, 0));
    }

    /**
     * 带重试的流式聊天内部实现
     * 当遇到 429 速率限制时，等待后自动重试
     */
    private void doStreamWithRetry(String prompt, SseEmitter emitter,
                                    StreamEventHandler eventHandler, int attempt) {
        try {
            // 仅首次调用时执行 onBeforeChat（重试时跳过，避免重复发送前置事件）
            if (attempt == 0) {
                eventHandler.onBeforeChat(emitter, prompt);
            }

            // 累计流式输出文本，用于 LLM 未返回 tokenUsage 时的兜底估算
            StringBuilder outputBuilder = new StringBuilder();
            final long streamStartTime = System.currentTimeMillis();

            streamingModel.chat(prompt, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    if (partialResponse != null && !partialResponse.isEmpty()) {
                        outputBuilder.append(partialResponse);
                        log.trace("Emitter LLM分片响应: {}", partialResponse);
                        eventHandler.onPartialResponse(emitter, partialResponse);
                        SseEventBuilder.sendMessageEvent(emitter, partialResponse);
                    }
                }

                @Override
                public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                    log.info("流式聊天完成");
                    long durationMs = System.currentTimeMillis() - streamStartTime;
                    String fullResponse = outputBuilder.toString();
                    eventHandler.onCompleteResponse(emitter, fullResponse);
                    // token 用量回调（实测值优先，估算值兜底）
                    int inputTokens, outputTokens, totalTokens;
                    if (response.tokenUsage() != null) {
                        inputTokens  = response.tokenUsage().inputTokenCount();
                        outputTokens = response.tokenUsage().outputTokenCount();
                        totalTokens  = response.tokenUsage().totalTokenCount();
                    } else {
                        inputTokens  = estimateTokens(prompt);
                        outputTokens = estimateTokens(fullResponse);
                        totalTokens  = inputTokens + outputTokens;
                        log.debug("流式 Token 估算: input≈{}, output≈{}", inputTokens, outputTokens);
                    }
                    eventHandler.onTokenUsage(emitter, inputTokens, outputTokens, totalTokens, durationMs);
                    eventHandler.onAfterChat(emitter);
                    SseEventBuilder.sendDoneEvent(emitter);
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    if (isRateLimitError(error) && attempt < RATE_LIMIT_MAX_RETRIES) {
                        log.warn("流式聊天遇到速率限制(attempt={}/{}), 将重试...",
                                attempt + 1, RATE_LIMIT_MAX_RETRIES);
                        rateLimitBackoff(attempt + 1);
                        doStreamWithRetry(prompt, emitter, eventHandler, attempt + 1);
                        return;
                    }
                    log.error("流式聊天失败: {}", error.getMessage(), error);
                    SseEventBuilder.sendErrorEvent(emitter, RagStreamErrorMapper.toUserMessage(error));
                    emitter.complete();
                }
            });
        } catch (Exception e) {
            if (isRateLimitError(e) && attempt < RATE_LIMIT_MAX_RETRIES) {
                rateLimitBackoff(attempt + 1);
                doStreamWithRetry(prompt, emitter, eventHandler, attempt + 1);
                return;
            }
            log.error("流式聊天启动失败: {}", e.getMessage(), e);
            SseEventBuilder.sendErrorEvent(emitter, RagStreamErrorMapper.toUserMessage(e));
            emitter.complete();
        }
    }

    // ===================== 速率限制辅助方法 =====================

    /**
     * 判断异常是否为速率限制（429）
     */
    private boolean isRateLimitError(Throwable e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("429")
                || msg.toLowerCase().contains("rate limit")
                || msg.contains("速率限制")
                || msg.contains("请求频率");
    }

    /**
     * 速率限制指数退避等待
     *
     * @param attempt 当前重试次数（从 1 开始）
     */
    private void rateLimitBackoff(int attempt) {
        long waitMs = RATE_LIMIT_INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 2s, 4s, 8s
        log.warn("速率限制退避: 第 {} 次重试，等待 {}ms...", attempt, waitMs);
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}