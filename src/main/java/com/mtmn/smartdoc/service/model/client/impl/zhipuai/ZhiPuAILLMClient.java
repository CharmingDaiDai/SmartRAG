package com.mtmn.smartdoc.service.model.client.impl.zhipuai;

import com.mtmn.smartdoc.service.model.client.impl.AbstractLLMClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 智谱AI LLM客户端实现
 * 
 * <p>使用 LangChain4j 专门的智谱AI实现类，支持GLM系列模型</p>
 * <p>支持的模型系列:</p>
 * <ul>
 *   <li>GLM-4 Flash - 快速响应模型</li>
 *   <li>GLM-4.5 Flash - 增强版快速响应模型</li>
 *   <li>GLM-4.6 Flash - 最新版快速响应模型</li>
 * </ul>
 * 
 * <p>特点:</p>
 * <ul>
 *   <li>支持长文本处理(最高128K tokens)</li>
 *   <li>中文理解能力强</li>
 *   <li>响应速度快</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 2.2
 * @since 2025-01-17
 */
@Slf4j
public class ZhiPuAILLMClient extends AbstractLLMClient {
    
    public ZhiPuAILLMClient(
            String providerName,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(providerName, providerConfig, modelConfig);
    }
    
    @Override
    protected ChatModel buildChatModel() {
        log.info("构建智谱AI同步聊天模型: baseUrl={}, model={}", 
                providerConfig.getBaseUrl(), modelConfig.getName());
        
        ZhipuAiChatModel.ZhipuAiChatModelBuilder builder = ZhipuAiChatModel.builder()
                .apiKey(providerConfig.getApiKey())
                .baseUrl(providerConfig.getBaseUrl())
                .model(modelConfig.getName())
                .logRequests(true)
                .logResponses(false);
        
        // 设置超时
        if (providerConfig.getTimeout() != null) {
            Duration timeout = parseDuration(providerConfig.getTimeout());
            builder.connectTimeout(timeout)
                   .readTimeout(timeout);
        }
        
        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }
        
        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxToken(modelConfig.getMaxTokens());
        }
        
        // 设置温度
        if (modelConfig.getTemperature() != null) {
            builder.temperature(modelConfig.getTemperature());
        }
        
        return builder.build();
    }
    
    @Override
    protected StreamingChatModel buildStreamingModel() {
        log.info("构建智谱AI流式聊天模型: baseUrl={}, model={}", 
                providerConfig.getBaseUrl(), modelConfig.getName());
        
        ZhipuAiStreamingChatModel.ZhipuAiStreamingChatModelBuilder builder = 
                ZhipuAiStreamingChatModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .baseUrl(providerConfig.getBaseUrl())
                        .model(modelConfig.getName())
                        .logRequests(true)
                        .logResponses(false);
        
        // 设置超时
        if (providerConfig.getTimeout() != null) {
            Duration timeout = parseDuration(providerConfig.getTimeout());
            builder.connectTimeout(timeout)
                   .readTimeout(timeout);
        }
        
        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxToken(modelConfig.getMaxTokens());
        }
        
        // 设置温度
        if (modelConfig.getTemperature() != null) {
            builder.temperature(modelConfig.getTemperature());
        }
        
        return builder.build();
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
