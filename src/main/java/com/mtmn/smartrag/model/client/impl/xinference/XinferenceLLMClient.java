package com.mtmn.smartrag.model.client.impl.xinference;

import com.mtmn.smartrag.model.client.impl.AbstractLLMClient;
import com.mtmn.smartrag.model.config.ModelProperties;
import dev.langchain4j.community.model.xinference.XinferenceChatModel;
import dev.langchain4j.community.model.xinference.XinferenceStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Xinference LLM客户端实现
 *
 * <p>使用 LangChain4j 专门的Xinference实现类，支持本地部署的各种开源模型</p>
 * <p>支持的模型系列:</p>
 * <ul>
 *   <li>Qwen2.5 系列 - 通义千问开源版</li>
 *   <li>LLaMA 系列 - Meta开源模型</li>
 *   <li>ChatGLM 系列 - 智谱开源模型</li>
 *   <li>其他开源LLM模型</li>
 * </ul>
 *
 * <p>特点:</p>
 * <ul>
 *   <li>支持本地部署，数据更安全</li>
 *   <li>无API调用费用</li>
 *   <li>可自定义模型配置</li>
 *   <li>支持GPU加速</li>
 * </ul>
 *
 * <p>使用前提:</p>
 * <ul>
 *   <li>需要先部署Xinference服务</li>
 *   <li>需要下载并加载对应的模型</li>
 *   <li>配置正确的base-url</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 2.2
 * @since 2025-01-17
 */
@Slf4j
public class XinferenceLLMClient extends AbstractLLMClient {

    public XinferenceLLMClient(
            String providerName,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(providerName, providerConfig, modelConfig);
    }

    @Override
    protected ChatModel buildChatModel() {
        log.info("构建Xinference同步聊天模型: baseUrl={}, model={}",
                providerConfig.getBaseUrl(), modelConfig.getName());

        XinferenceChatModel.XinferenceChatModelBuilder builder = XinferenceChatModel.builder()
                .apiKey(providerConfig.getApiKey())
                .baseUrl(providerConfig.getBaseUrl())
                .modelName(modelConfig.getName());

        // 设置超时（本地部署可能需要更长时间）
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        } else {
            // Xinference默认使用更长的超时时间
            builder.timeout(Duration.ofSeconds(120));
        }

        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }

        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxTokens(modelConfig.getMaxTokens());
        }

        // 设置温度
        if (modelConfig.getTemperature() != null) {
            builder.temperature(modelConfig.getTemperature());
        }

        return builder.build();
    }

    @Override
    protected StreamingChatModel buildStreamingModel() {
        log.info("构建Xinference流式聊天模型: baseUrl={}, model={}",
                providerConfig.getBaseUrl(), modelConfig.getName());

        XinferenceStreamingChatModel.XinferenceStreamingChatModelBuilder builder =
                XinferenceStreamingChatModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .baseUrl(providerConfig.getBaseUrl())
                        .modelName(modelConfig.getName());

        // 设置超时
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        } else {
            // Xinference默认使用更长的超时时间
            builder.timeout(Duration.ofSeconds(120));
        }

        // 设置最大token
        if (modelConfig.getMaxTokens() != null) {
            builder.maxTokens(modelConfig.getMaxTokens());
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
            return Duration.ofSeconds(120); // Xinference默认120s
        }

        try {
            String value = timeout.substring(0, timeout.length() - 1);
            String unit = timeout.substring(timeout.length() - 1);

            int num = Integer.parseInt(value);

            return switch (unit.toLowerCase()) {
                case "s" -> Duration.ofSeconds(num);
                case "m" -> Duration.ofMinutes(num);
                case "h" -> Duration.ofHours(num);
                default -> Duration.ofSeconds(120);
            };
        } catch (Exception e) {
            log.warn("无法解析超时时间: {}, 使用默认值120s", timeout);
            return Duration.ofSeconds(120);
        }
    }
}