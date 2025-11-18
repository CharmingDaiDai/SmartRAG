package com.mtmn.smartdoc.service.model.client.impl.openai;

import com.mtmn.smartdoc.service.model.client.impl.AbstractLLMClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialChatModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialStreamingChatModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * OpenAI 官方 LLM客户端实现
 * 
 * <p>使用 OpenAI 官方 SDK (openai-java) 实现，提供最原生的 OpenAI API 支持</p>
 * <p>支持的服务:</p>
 * <ul>
 *   <li>OpenAI 官方 API - 直接访问 OpenAI 服务</li>
 *   <li>Azure OpenAI - 通过 Azure 部署的 OpenAI 服务</li>
 *   <li>GitHub Models - GitHub 提供的模型服务</li>
 * </ul>
 * 
 * <p>与 OpenAI 兼容格式的区别:</p>
 * <ul>
 *   <li>使用官方 SDK，功能更完整</li>
 *   <li>支持 Azure OpenAI 特定配置</li>
 *   <li>内置重试机制</li>
 *   <li>更好的类型安全</li>
 * </ul>
 * 
 * <p>注意: 如果你使用的是第三方 OpenAI 兼容 API，请使用 OpenAILLMClient</p>
 * 
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-18
 */
@Slf4j
public class OpenAIOfficialLLMClient extends AbstractLLMClient {
    
    public OpenAIOfficialLLMClient(
            String providerName,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(providerName, providerConfig, modelConfig);
    }
    
    @Override
    protected ChatModel buildChatModel() {
        log.info("构建OpenAI官方同步聊天模型: baseUrl={}, model={}", 
                providerConfig.getBaseUrl(), modelConfig.getName());
        
        OpenAiOfficialChatModel.Builder builder = OpenAiOfficialChatModel.builder()
                .apiKey(providerConfig.getApiKey())
                .modelName(modelConfig.getName());
        
        // 设置 baseUrl（如果有）
        if (providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().isEmpty()) {
            builder.baseUrl(providerConfig.getBaseUrl());
        }
        
        // 设置超时
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        }
        
        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }
        
        // 设置最大token (注意官方SDK使用 maxCompletionTokens)
        if (modelConfig.getMaxTokens() != null) {
            builder.maxCompletionTokens(modelConfig.getMaxTokens());
        }
        
        // 设置温度
        if (modelConfig.getTemperature() != null) {
            builder.temperature(modelConfig.getTemperature());
        }
        
        return builder.build();
    }
    
    @Override
    protected StreamingChatModel buildStreamingModel() {
        log.info("构建OpenAI官方流式聊天模型: baseUrl={}, model={}", 
                providerConfig.getBaseUrl(), modelConfig.getName());
        
        OpenAiOfficialStreamingChatModel.Builder builder = 
                OpenAiOfficialStreamingChatModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .modelName(modelConfig.getName());
        
        // 设置 baseUrl（如果有）
        if (providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().isEmpty()) {
            builder.baseUrl(providerConfig.getBaseUrl());
        }
        
        // 设置超时
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        }
        
        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }
        
        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxCompletionTokens(modelConfig.getMaxTokens());
        }
        
        // 设置温度
        if (modelConfig.getTemperature() != null) {
            builder.temperature(modelConfig.getTemperature());
        }
        
        return builder.build();
    }
    
    @Override
    public String getProviderType() {
        return providerName;
    }
    
    /**
     * 解析超时时间字符串
     * 支持格式: 60s, 2m, 1h
     */
    private Duration parseDuration(String timeout) {
        if (timeout == null || timeout.isEmpty()) {
            return Duration.ofSeconds(60);
        }
        
        try {
            String value = timeout.substring(0, timeout.length() - 1);
            String unit = timeout.substring(timeout.length() - 1);
            
            int num = Integer.parseInt(value);
            
            return switch (unit.toLowerCase()) {
                case "s" -> Duration.ofSeconds(num);
                case "m" -> Duration.ofMinutes(num);
                case "h" -> Duration.ofHours(num);
                default -> Duration.ofSeconds(60);
            };
        } catch (Exception e) {
            log.warn("无法解析超时时间: {}, 使用默认值60s", timeout);
            return Duration.ofSeconds(60);
        }
    }
}
