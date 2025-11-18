package com.mtmn.smartdoc.service.model.client.impl;

import com.mtmn.smartdoc.service.model.client.LLMClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import com.mtmn.smartdoc.service.model.dto.ChatRequest;
import com.mtmn.smartdoc.service.model.dto.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

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
        
        try {
            String response = chatModel.chat(prompt);
            log.debug("同步聊天响应: response长度={}", response.length());
            return response;
        } catch (Exception e) {
            log.error("同步聊天失败: {}", e.getMessage(), e);
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.debug("结构化聊天请求: messages数量={}", request.getMessages().size());
        
        try {
            // 转换消息格式
            List<ChatMessage> messages = convertMessages(request.getMessages());
            
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
            log.error("结构化聊天失败: {}", e.getMessage(), e);
            throw new RuntimeException("LLM调用失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Flux<String> streamChat(String prompt) {
        log.debug("流式聊天请求: prompt长度={}", prompt.length());
        
        return Flux.create(sink -> {
            try {
                streamingModel.chat(prompt, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        sink.next(partialResponse);
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        log.debug("流式聊天完成");
                        sink.complete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        log.error("流式聊天失败: {}", error.getMessage(), error);
                        sink.error(new RuntimeException("LLM流式调用失败: " + error.getMessage(), error));
                    }
                });
            } catch (Exception e) {
                log.error("流式聊天启动失败: {}", e.getMessage(), e);
                sink.error(new RuntimeException("LLM流式调用启动失败: " + e.getMessage(), e));
            }
        });
    }
    
    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.debug("结构化流式聊天请求: messages数量={}", request.getMessages().size());
        
        return Flux.create(sink -> {
            try {
                // 转换消息格式
                List<ChatMessage> messages = convertMessages(request.getMessages());
                
                streamingModel.chat(messages, new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partialResponse) {
                        sink.next(partialResponse);
                    }
                    
                    @Override
                    public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse response) {
                        log.debug("结构化流式聊天完成");
                        sink.complete();
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        log.error("结构化流式聊天失败: {}", error.getMessage(), error);
                        sink.error(new RuntimeException("LLM流式调用失败: " + error.getMessage(), error));
                    }
                });
            } catch (Exception e) {
                log.error("结构化流式聊天启动失败: {}", e.getMessage(), e);
                sink.error(new RuntimeException("LLM流式调用启动失败: " + e.getMessage(), e));
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
}