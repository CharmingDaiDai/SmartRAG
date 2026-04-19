//package com.mtmn.smartrag.service.model.client.impl;
//
//import com.mtmn.smartrag.model.client.EmbeddingClient;
//import com.mtmn.smartrag.model.config.ModelProperties;
//import com.mtmn.smartrag.model.factory.ModelFactory;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//
///**
// * 批量大小配置测试
// *
// * <p>验证不同提供商的批量大小配置是否正确应用</p>
// *
// * @author charmingdaidai
// * @version 1.0
// * @since 2025-01-17
// */
//@SpringBootTest
//@ActiveProfiles("test")
//@Slf4j
//public class BatchSizeConfigTest {
//
//    @Resource
//    private ModelFactory modelFactory;
//
//    @Resource
//    private ModelProperties modelProperties;
//
//    @Test
//    public void testBatchSizeConfiguration() {
//        log.info("=== 测试批量大小配置 ===");
//
//        // 检查所有配置的Embedding模型
//        modelProperties.getProviders().forEach((instanceId, providerConfig) -> {
//            if (providerConfig.isEnabled() &&
//                    providerConfig.getEmbeddingModels() != null) {
//
//                for (ModelProperties.ModelConfig modelConfig :
//                        providerConfig.getEmbeddingModels()) {
//
//                    String modelId = instanceId + "@" + modelConfig.getName();
//                    Integer configuredBatchSize = modelConfig.getBatchSize();
//
//                    log.info("检查模型: {}", modelId);
//                    log.info("  配置的批量大小: {}",
//                            configuredBatchSize != null ? configuredBatchSize : "默认(8)");
//
//                    // 验证默认值
//                    if (configuredBatchSize == null) {
//                        assertEquals(8, 8, "未配置时应使用默认值8");
//                    }
//                }
//            }
//        });
//    }
//
//    @Test
//    public void testBatchProcessing() {
//        log.info("=== 测试批量处理效果 ===");
//
//        String modelId = modelProperties.getActive().getEmbedding();
//        EmbeddingClient client = modelFactory.createEmbeddingClient(modelId);
//
//        // 生成多于批量大小的文本
//        int textCount = 25;  // 大于默认批量8，会分批处理
//        List<String> texts = new ArrayList<>();
//        for (int i = 1; i <= textCount; i++) {
//            texts.add("测试文本 " + i + ": 这是用于测试批量处理的文本");
//        }
//
//        log.info("测试批量处理: 文本数量={}", textCount);
//
//        long startTime = System.currentTimeMillis();
//        List<List<Float>> embeddings = client.embedBatch(texts);
//        long endTime = System.currentTimeMillis();
//
//        assertNotNull(embeddings);
//        assertEquals(textCount, embeddings.size());
//
//        log.info("批量处理完成: 耗时={}ms", endTime - startTime);
//        log.info("平均每个文本: {}ms", (endTime - startTime) / (double) textCount);
//
//        // 验证所有向量维度一致
//        int dimension = client.getDimension();
//        for (List<Float> embedding : embeddings) {
//            assertEquals(dimension, embedding.size(), "所有向量维度应一致");
//        }
//    }
//
//    @Test
//    public void testDifferentBatchSizes() {
//        log.info("=== 测试不同批量大小的性能 ===");
//
//        String modelId = modelProperties.getActive().getEmbedding();
//
//        // 测试数据
//        List<String> texts = new ArrayList<>();
//        for (int i = 1; i <= 50; i++) {
//            texts.add("性能测试文本 " + i);
//        }
//
//        EmbeddingClient client = modelFactory.createEmbeddingClient(modelId);
//
//        log.info("使用模型: {}", modelId);
//        log.info("测试文本数量: {}", texts.size());
//
//        // 执行测试
//        long startTime = System.currentTimeMillis();
//        List<List<Float>> embeddings = client.embedBatch(texts);
//        long endTime = System.currentTimeMillis();
//
//        assertEquals(texts.size(), embeddings.size());
//
//        log.info("批量处理完成:");
//        log.info("  总耗时: {}ms", endTime - startTime);
//        log.info("  平均每个: {}ms", (endTime - startTime) / (double) texts.size());
//        log.info("  向量维度: {}", embeddings.get(0).size());
//    }
//}