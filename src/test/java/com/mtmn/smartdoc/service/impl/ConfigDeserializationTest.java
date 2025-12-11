package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.rag.config.NaiveRagIndexConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 配置反序列化测试
 * 验证JSON反序列化是否能正确处理前端传来的配置
 *
 * @author charmingdaidai
 * @date 2025-11-21
 */
@DisplayName("配置反序列化测试")
class ConfigDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("反序列化NAIVE_RAG配置 - 不包含type字段")
    void deserializeNaiveRAGConfig_WithoutTypeField() throws Exception {
        // Given - 前端实际传来的JSON格式
        String configJson = "{\"chunkSize\": 1000, \"chunkOverlap\": 100, \"separator\": \"\\n\\n\"}";

        // When - 使用readerFor直接反序列化到具体类
        NaiveRagIndexConfig config = objectMapper.readerFor(NaiveRagIndexConfig.class)
                .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(configJson);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getChunkSize()).isEqualTo(1000);
        assertThat(config.getChunkOverlap()).isEqualTo(100);
        assertThat(config.getSeparator()).isEqualTo("\n\n");
    }

    @Test
    @DisplayName("反序列化NAIVE_RAG配置 - 部分字段缺失")
    void deserializeNaiveRAGConfig_PartialFields() throws Exception {
        // Given - 只包含部分字段
        String configJson = "{\"chunkSize\": 800}";

        // When
        NaiveRagIndexConfig config = objectMapper.readerFor(NaiveRagIndexConfig.class)
                .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(configJson);

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getChunkSize()).isEqualTo(800);
        assertThat(config.getChunkOverlap()).isNull(); // 未提供的字段为null
        assertThat(config.getSeparator()).isNull();
    }

    @Test
    @DisplayName("反序列化NAIVE_RAG配置 - 空JSON对象")
    void deserializeNaiveRAGConfig_EmptyJson() throws Exception {
        // Given
        String configJson = "{}";

        // When
        NaiveRagIndexConfig config = objectMapper.readerFor(NaiveRagIndexConfig.class)
                .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .readValue(configJson);

        // Then - 所有字段都应该是null,等待后续填充默认值
        assertThat(config).isNotNull();
        assertThat(config.getChunkSize()).isNull();
        assertThat(config.getChunkOverlap()).isNull();
        assertThat(config.getSeparator()).isNull();
    }

    @Test
    @DisplayName("验证序列化后的配置包含所有字段")
    void serializeNaiveRAGConfig_WithAllFields() throws Exception {
        // Given - 填充了所有默认值的配置
        NaiveRagIndexConfig config = new NaiveRagIndexConfig();
        config.setChunkSize(1000);
        config.setChunkOverlap(100);
        config.setSeparator("\n\n");

        // When
        String json = objectMapper.writeValueAsString(config);

        // Then - 序列化后应包含所有字段
        assertThat(json).contains("\"chunkSize\":1000");
        assertThat(json).contains("\"chunkOverlap\":100");
        assertThat(json).contains("\"separator\":\"\\n\\n\"");
    }
}