package com.mtmn.smartdoc.service.model.example;

import com.mtmn.smartdoc.service.model.client.EmbeddingClient;
import com.mtmn.smartdoc.service.model.dto.EmbeddingRequest;
import com.mtmn.smartdoc.service.model.dto.EmbeddingResponse;
import com.mtmn.smartdoc.service.model.factory.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Embedding模型使用示例
 * 
 * <p>演示如何使用新的Embedding客户端架构</p>
 * <p>支持的功能:</p>
 * <ul>
 *   <li>单文本向量化</li>
 *   <li>批量文本向量化</li>
 *   <li>自定义批量大小</li>
 *   <li>多实例配置</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Component
@Slf4j
public class EmbeddingUsageExample {
    
    private final ModelFactory modelFactory;
    
    public EmbeddingUsageExample(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }
    
    /**
     * 示例1: 使用默认Embedding模型
     */
    public void example1_UseDefaultModel() {
        log.info("=== 示例1: 使用默认Embedding模型 ===");
        
        // 创建默认Embedding客户端
        EmbeddingClient client = modelFactory.createDefaultEmbeddingClient();
        
        // 单文本向量化
        String text = "人工智能正在改变世界";
        List<Float> embedding = client.embed(text);
        
        log.info("文本: {}", text);
        log.info("向量维度: {}", embedding.size());
        log.info("向量前5个值: {}", embedding.subList(0, 5));
    }
    
    /**
     * 示例2: 批量文本向量化
     */
    public void example2_BatchEmbedding() {
        log.info("=== 示例2: 批量文本向量化 ===");
        
        EmbeddingClient client = modelFactory.createDefaultEmbeddingClient();
        
        // 准备多个文本
        List<String> texts = Arrays.asList(
                "深度学习是机器学习的子领域",
                "自然语言处理让机器理解人类语言",
                "计算机视觉使机器能够看懂图像"
        );
        
        // 批量向量化
        List<List<Float>> embeddings = client.embedBatch(texts);
        
        log.info("输入文本数: {}", texts.size());
        log.info("输出向量数: {}", embeddings.size());
        log.info("每个向量维度: {}", embeddings.get(0).size());
    }
    
    /**
     * 示例3: 使用结构化请求
     */
    public void example3_StructuredRequest() {
        log.info("=== 示例3: 使用结构化请求 ===");
        
        EmbeddingClient client = modelFactory.createDefaultEmbeddingClient();
        
        // 构建请求
        EmbeddingRequest request = EmbeddingRequest.builder()
                .texts(Arrays.asList(
                        "机器学习算法",
                        "神经网络结构"
                ))
                .build();
        
        // 执行请求
        EmbeddingResponse response = client.embed(request);
        
        log.info("模型名称: {}", response.getModel());
        log.info("向量维度: {}", response.getDimension());
        log.info("向量数量: {}", response.getEmbeddings().size());
    }
    
    /**
     * 示例4: 使用指定的模型实例
     */
    public void example4_UseSpecificModel() {
        log.info("=== 示例4: 使用指定的模型实例 ===");
        
        // 使用 Xinference GPU 实例的 bge-m3 模型
        String modelId = "xinference-gpu@bge-m3";
        EmbeddingClient client = modelFactory.createEmbeddingClient(modelId);
        
        String text = "使用指定的Embedding模型";
        List<Float> embedding = client.embed(text);
        
        log.info("模型ID: {}", client.getModelId());
        log.info("模型名称: {}", client.getModelName());
        log.info("提供商类型: {}", client.getProviderType());
        log.info("向量维度: {}", client.getDimension());
    }
    
    /**
     * 示例5: 多实例场景 - 根据场景选择不同实例
     */
    public void example5_MultiInstance() {
        log.info("=== 示例5: 多实例场景 ===");
        
        // 场景1: 使用GPU服务器（高性能）
        EmbeddingClient gpuClient = modelFactory.createEmbeddingClient("xinference-gpu@bge-m3");
        log.info("GPU实例: {}", gpuClient.getModelId());
        
        // 场景2: 使用通义千问（云服务）
        EmbeddingClient qwenClient = modelFactory.createEmbeddingClient("qwen@text-embedding-v4");
        log.info("通义千问实例: {}", qwenClient.getModelId());
        
        // 场景3: 使用OpenAI官方（最高质量）
        try {
            EmbeddingClient openaiClient = modelFactory.createEmbeddingClient("openai-official@text-embedding-3-small");
            log.info("OpenAI实例: {}", openaiClient.getModelId());
        } catch (Exception e) {
            log.warn("OpenAI实例未配置或不可用");
        }
    }
    
    /**
     * 示例6: 查看所有可用的Embedding模型
     */
    public void example6_ListAvailableModels() {
        log.info("=== 示例6: 查看所有可用的Embedding模型 ===");
        
        List<String> models = modelFactory.getAvailableEmbeddingModels();
        
        log.info("可用Embedding模型数量: {}", models.size());
        for (String modelId : models) {
            log.info("  - {}", modelId);
        }
    }
    
    /**
     * 示例7: 大批量处理
     */
    public void example7_LargeBatchProcessing() {
        log.info("=== 示例7: 大批量处理 ===");
        
        EmbeddingClient client = modelFactory.createDefaultEmbeddingClient();
        
        // 生成大量文本（超过批量大小）
        List<String> largeTextList = Arrays.asList(
                "文本1", "文本2", "文本3", "文本4", "文本5",
                "文本6", "文本7", "文本8", "文本9", "文本10",
                "文本11", "文本12", "文本13", "文本14", "文本15",
                "文本16", "文本17", "文本18", "文本19", "文本20"
        );
        
        long startTime = System.currentTimeMillis();
        List<List<Float>> embeddings = client.embedBatch(largeTextList);
        long endTime = System.currentTimeMillis();
        
        log.info("处理文本数: {}", largeTextList.size());
        log.info("总耗时: {}ms", endTime - startTime);
        log.info("平均每个: {}ms", (endTime - startTime) / (double) largeTextList.size());
        log.info("向量维度: {}", embeddings.get(0).size());
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        try {
            example1_UseDefaultModel();
            example2_BatchEmbedding();
            example3_StructuredRequest();
            example4_UseSpecificModel();
            example5_MultiInstance();
            example6_ListAvailableModels();
            example7_LargeBatchProcessing();
            
            log.info("=== 所有示例运行完成 ===");
        } catch (Exception e) {
            log.error("运行示例时出错: {}", e.getMessage(), e);
        }
    }
}
