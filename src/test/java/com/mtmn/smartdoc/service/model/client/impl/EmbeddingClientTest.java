package com.mtmn.smartdoc.service.model.client.impl;

import com.mtmn.smartdoc.service.model.client.EmbeddingClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import com.mtmn.smartdoc.service.model.dto.EmbeddingRequest;
import com.mtmn.smartdoc.service.model.dto.EmbeddingResponse;
import com.mtmn.smartdoc.service.model.factory.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Embedding客户端测试类
 * 
 * <p>测试各个提供商的Embedding客户端实现</p>
 * <p>测试内容包括:</p>
 * <ul>
 *   <li>单文本向量化</li>
 *   <li>批量文本向量化</li>
 *   <li>结构化请求向量化</li>
 *   <li>批量大小配置</li>
 *   <li>向量维度验证</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class EmbeddingClientTest {
    
    @Autowired
    private ModelFactory modelFactory;
    
    @Autowired
    private ModelProperties modelProperties;
    
    private EmbeddingClient embeddingClient;
    
    @BeforeEach
    public void setUp() {
        // 使用默认的Embedding客户端
        String activeEmbedding = modelProperties.getActive().getEmbedding();
        log.info("使用默认Embedding模型: {}", activeEmbedding);
        embeddingClient = modelFactory.createEmbeddingClient(activeEmbedding);
    }
    
    @Test
    public void testSingleTextEmbedding() {
        log.info("=== 测试单文本向量化 ===");
        
        String text = "人工智能是计算机科学的一个分支";
        
        List<Float> embedding = embeddingClient.embed(text);
        
        assertNotNull(embedding, "向量不应为null");
        assertFalse(embedding.isEmpty(), "向量不应为空");
        assertEquals(embeddingClient.getDimension(), embedding.size(), 
                "向量维度应与配置一致");
        
        log.info("单文本向量化成功: text长度={}, 向量维度={}", 
                text.length(), embedding.size());
        log.info("向量前5个值: {}", embedding.subList(0, Math.min(5, embedding.size())));
    }
    
    @Test
    public void testBatchEmbedding() {
        log.info("=== 测试批量文本向量化 ===");
        
        List<String> texts = Arrays.asList(
                "深度学习是机器学习的一个子领域",
                "自然语言处理是人工智能的重要应用",
                "计算机视觉让机器能够理解图像",
                "强化学习用于训练智能体做出决策"
        );
        
        List<List<Float>> embeddings = embeddingClient.embedBatch(texts);
        
        assertNotNull(embeddings, "批量向量不应为null");
        assertEquals(texts.size(), embeddings.size(), 
                "向量数量应与文本数量一致");
        
        for (int i = 0; i < embeddings.size(); i++) {
            List<Float> embedding = embeddings.get(i);
            assertNotNull(embedding, String.format("第%d个向量不应为null", i));
            assertEquals(embeddingClient.getDimension(), embedding.size(), 
                    String.format("第%d个向量维度应与配置一致", i));
        }
        
        log.info("批量向量化成功: 文本数量={}, 向量维度={}", 
                texts.size(), embeddings.get(0).size());
    }
    
    @Test
    public void testLargeBatchEmbedding() {
        log.info("=== 测试大批量文本向量化 ===");
        
        // 生成20个文本，测试批量处理
        List<String> texts = Arrays.asList(
                "文本1：机器学习基础",
                "文本2：深度神经网络",
                "文本3：卷积神经网络",
                "文本4：循环神经网络",
                "文本5：生成对抗网络",
                "文本6：注意力机制",
                "文本7：Transformer架构",
                "文本8：BERT模型",
                "文本9：GPT模型",
                "文本10：大语言模型",
                "文本11：提示工程",
                "文本12：微调技术",
                "文本13：迁移学习",
                "文本14：强化学习",
                "文本15：Q学习算法",
                "文本16：策略梯度",
                "文本17：Actor-Critic",
                "文本18：多智能体系统",
                "文本19：联邦学习",
                "文本20：边缘智能"
        );
        
        long startTime = System.currentTimeMillis();
        List<List<Float>> embeddings = embeddingClient.embedBatch(texts);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(embeddings, "批量向量不应为null");
        assertEquals(texts.size(), embeddings.size(), 
                "向量数量应与文本数量一致");
        
        log.info("大批量向量化成功: 文本数量={}, 耗时={}ms, 平均每个={}ms", 
                texts.size(), endTime - startTime, 
                (endTime - startTime) / (double) texts.size());
    }
    
    @Test
    public void testStructuredEmbedding() {
        log.info("=== 测试结构化请求向量化 ===");
        
        List<String> texts = Arrays.asList(
                "人工智能改变世界",
                "机器学习驱动创新"
        );
        
        EmbeddingRequest request = EmbeddingRequest.builder()
                .texts(texts)
                .build();
        
        EmbeddingResponse response = embeddingClient.embed(request);
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getEmbeddings(), "向量列表不应为null");
        assertEquals(texts.size(), response.getEmbeddings().size(), 
                "向量数量应与文本数量一致");
        assertEquals(embeddingClient.getModelName(), response.getModel(), 
                "模型名称应一致");
        assertEquals(embeddingClient.getDimension(), response.getDimension(), 
                "向量维度应一致");
        
        log.info("结构化请求成功: model={}, dimension={}, count={}", 
                response.getModel(), response.getDimension(), 
                response.getEmbeddings().size());
    }
    
    @Test
    public void testClientMetadata() {
        log.info("=== 测试客户端元数据 ===");
        
        String modelId = embeddingClient.getModelId();
        String modelName = embeddingClient.getModelName();
        String providerType = embeddingClient.getProviderType();
        int dimension = embeddingClient.getDimension();
        
        assertNotNull(modelId, "模型ID不应为null");
        assertNotNull(modelName, "模型名称不应为null");
        assertNotNull(providerType, "提供商类型不应为null");
        assertTrue(dimension > 0, "向量维度应大于0");
        
        log.info("客户端元数据:");
        log.info("  Model ID: {}", modelId);
        log.info("  Model Name: {}", modelName);
        log.info("  Provider Type: {}", providerType);
        log.info("  Dimension: {}", dimension);
        
        // 验证modelId格式: instanceId@modelName
        assertTrue(modelId.contains("@"), "modelId应包含@符号");
        String[] parts = modelId.split("@");
        assertEquals(2, parts.length, "modelId应为 instanceId@modelName 格式");
        assertEquals(modelName, parts[1], "modelId的modelName部分应与getModelName一致");
    }
    
    @Test
    public void testEmptyTextHandling() {
        log.info("=== 测试空文本处理 ===");
        
        List<List<Float>> embeddings = embeddingClient.embedBatch(Arrays.asList());
        
        assertNotNull(embeddings, "空列表应返回空结果而非null");
        assertTrue(embeddings.isEmpty(), "空列表应返回空结果");
        
        log.info("空文本处理正确");
    }
    
    @Test
    public void testMultipleProviders() {
        log.info("=== 测试多个提供商 ===");
        
        List<String> availableModels = modelFactory.getAvailableEmbeddingModels();
        log.info("可用Embedding模型数量: {}", availableModels.size());
        
        String testText = "测试文本：这是一个向量化测试";
        
        for (String modelId : availableModels) {
            try {
                EmbeddingClient client = modelFactory.createEmbeddingClient(modelId);
                
                List<Float> embedding = client.embed(testText);
                
                assertNotNull(embedding, 
                        String.format("模型 %s 的向量不应为null", modelId));
                assertEquals(client.getDimension(), embedding.size(), 
                        String.format("模型 %s 的向量维度应一致", modelId));
                
                log.info("模型 {} 测试成功: dimension={}", modelId, embedding.size());
                
            } catch (Exception e) {
                // 某些模型可能未配置或不可用，记录但不失败
                log.warn("模型 {} 测试失败: {}", modelId, e.getMessage());
            }
        }
    }
    
    @Test
    public void testCacheEffectiveness() {
        log.info("=== 测试缓存效果 ===");
        
        String modelId = embeddingClient.getModelId();
        
        // 第一次创建
        long start1 = System.currentTimeMillis();
        EmbeddingClient client1 = modelFactory.createEmbeddingClient(modelId);
        long time1 = System.currentTimeMillis() - start1;
        
        // 第二次创建（应该从缓存获取）
        long start2 = System.currentTimeMillis();
        EmbeddingClient client2 = modelFactory.createEmbeddingClient(modelId);
        long time2 = System.currentTimeMillis() - start2;
        
        // 应该是同一个实例
        assertSame(client1, client2, "应该从缓存返回同一个实例");
        
        log.info("首次创建耗时: {}ms", time1);
        log.info("缓存获取耗时: {}ms", time2);
        log.info("缓存加速比: {}x", time1 / (double) Math.max(time2, 1));
        
        // 清除缓存
        modelFactory.clearCache(modelId);
        
        // 再次创建（应该重新创建）
        EmbeddingClient client3 = modelFactory.createEmbeddingClient(modelId);
        
        log.info("缓存清除后重新创建成功");
    }
}
