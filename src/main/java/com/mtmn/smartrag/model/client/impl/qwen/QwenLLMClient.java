package com.mtmn.smartrag.model.client.impl.qwen;

import com.mtmn.smartrag.model.client.impl.AbstractLLMClient;
import com.mtmn.smartrag.model.config.ModelProperties;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 通义千问 LLM客户端实现
 *
 * <p>使用 LangChain4j 专门的通义千问实现类，支持Qwen系列模型</p>
 * <p>支持的模型系列:</p>
 * <ul>
 *   <li>Qwen-Turbo - 快速响应，高性价比</li>
 *   <li>Qwen-Plus - 能力增强，平衡性价比和效果</li>
 *   <li>Qwen-Max - 最强能力，适合复杂任务</li>
 * </ul>
 *
 * <p>特点:</p>
 * <ul>
 *   <li>阿里云官方大模型</li>
 *   <li>支持长文本处理</li>
 *   <li>中英文双语能力强</li>
 *   <li>响应速度快</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 2.2
 * @since 2025-01-17
 */
@Slf4j
public class QwenLLMClient extends AbstractLLMClient {

    public QwenLLMClient(
            String providerName,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(providerName, providerConfig, modelConfig);
    }

    @Override
    protected ChatModel buildChatModel() {
        log.info("构建通义千问同步聊天模型: baseUrl={}, model={}",
                providerConfig.getBaseUrl(), modelConfig.getName());

        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .apiKey(providerConfig.getApiKey())
                .modelName(modelConfig.getName());

        // Qwen 的 baseUrl 不需要设置，默认就是 DashScope 的地址
        // 如果需要设置自定义地址，可以使用 baseUrl() 方法
        if (providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().isEmpty()) {
            builder.baseUrl(providerConfig.getBaseUrl());
        }

        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxTokens(modelConfig.getMaxTokens());
        }

        // 设置温度
        if (modelConfig.getTemperature() != null) {
            // QwenChatModel 的 temperature 参数类型是 Float
            builder.temperature(modelConfig.getTemperature().floatValue());
        }

        return builder.build();
    }

    @Override
    protected StreamingChatModel buildStreamingModel() {
        log.info("构建通义千问流式聊天模型: baseUrl={}, model={}",
                providerConfig.getBaseUrl(), modelConfig.getName());

        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder =
                QwenStreamingChatModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .modelName(modelConfig.getName());

        // Qwen 的 baseUrl 不需要设置，默认就是 DashScope 的地址
        if (providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().isEmpty()) {
            builder.baseUrl(providerConfig.getBaseUrl());
        }

        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxTokens(modelConfig.getMaxTokens());
        }

        // 设置温度
        if (modelConfig.getTemperature() != null) {
            builder.temperature(modelConfig.getTemperature().floatValue());
        }

        return builder.build();
    }
}