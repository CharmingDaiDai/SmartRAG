package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.config.ModelConfig;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 嵌入模型服务
 * 负责创建和管理不同的嵌入模型，提供文本向量化功能
 * 
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/4/18 10:00
 */
@Log4j2
@Service
// TODO 改回非静态的
public class EmbeddingService {

    private static final Map<String, EmbeddingModel> modelCache = new ConcurrentHashMap<>();

    @Autowired
    private ModelConfig modelConfigInstance;

    private static ModelConfig modelConfig;

    @PostConstruct
    public void init() {
        modelConfig = modelConfigInstance;
    }
    
    /**
     * 创建默认的嵌入模型
     * 
     * 实现思路：
     * 1. 检查ModelConfig是否已初始化
     * 2. 如果未初始化则抛出IllegalStateException异常
     * 3. 使用当前激活的嵌入模型配置创建模型实例
     * 4. 通过createEmbeddingModel(String)方法委托实际创建过程
     * 
     * @return 嵌入模型实例
     * @throws IllegalStateException 当ModelConfig未初始化时
     */
    public static EmbeddingModel createEmbeddingModel() {
        if (modelConfig == null) {
            log.error("ModelConfig 未初始化，无法创建嵌入模型");
            throw new IllegalStateException("ModelConfig 未初始化");
        }
        return createEmbeddingModel(modelConfig.getActiveEmbedding());
    }

    /**
     * 创建指定模型的嵌入模型
     * 
     * 实现思路：
     * 1. 检查ModelConfig是否已初始化，未初始化则抛出异常
     * 2. 确定目标模型ID：如果传入null则使用默认激活模型
     * 3. 使用computeIfAbsent实现模型缓存机制：
     *    - 如果缓存中存在则直接返回
     *    - 如果不存在则创建新实例并缓存
     * 4. 根据模型ID获取对应的配置信息
     * 5. 如果找不到配置则使用默认配置并记录警告
     * 6. 使用OpenAI客户端构建器创建嵌入模型：
     *    - 设置API密钥、基础URL和模型名称
     *    - 支持不同的嵌入模型提供商
     * 7. 将创建的模型实例存入缓存供后续使用
     * 
     * @param modelId 模型ID，null时使用默认模型
     * @return 嵌入模型实例
     * @throws IllegalStateException 当ModelConfig未初始化时
     */
    public static EmbeddingModel createEmbeddingModel(String modelId) {
        if (modelConfig == null) {
            log.error("ModelConfig 未初始化，无法创建嵌入模型");
            throw new IllegalStateException("ModelConfig 未初始化");
        }
        String targetModelId = modelId == null ? modelConfig.getActiveEmbedding() : modelId;
        return modelCache.computeIfAbsent(targetModelId, id -> {
            ModelConfig.ModelProperties config = modelConfig.getEmbeddingConfig(id);
            if (config == null) {
                log.warn("未找到模型配置：{}，将使用默认模型", id);
                config = modelConfig.getActiveEmbeddingConfig();
            }
            log.info("创建嵌入模型: {}", config.getModelName());
            return OpenAiEmbeddingModel.builder()
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .build();
        });
    }

    /**
     * 从文档内容创建向量存储（使用默认模型）
     * 
     * 实现思路：
     * 1. 使用默认嵌入模型处理文档内容
     * 2. 委托给重载方法createDocumentVectors(content, null)
     * 3. 简化API调用，提供便捷的默认模型接口
     * 
     * @param content 文档内容
     * @return 向量存储实例
     */
    public static EmbeddingStore<Embedding> createDocumentVectors(String content) {
        return createDocumentVectors(content, null);
    }

    /**
     * 从文档内容创建向量存储（使用指定模型）
     * 
     * 实现思路：
     * 1. 记录创建向量存储的操作，包含使用的模型信息
     * 2. 将文本内容转换为langchain4j的Document对象
     * 3. 创建指定的嵌入模型实例
     * 4. 初始化内存向量存储InMemoryEmbeddingStore
     * 5. 配置文档分割器DocumentByParagraphSplitter：
     *    - 设置最大块大小为2048字符
     *    - 设置重叠大小为100字符，保证上下文连续性
     * 6. 使用分割器将文档分割为多个文本段
     * 7. 使用嵌入模型对所有文本段进行批量向量化
     * 8. 将生成的向量添加到向量存储中
     * 9. 返回配置完成的向量存储实例
     * 
     * @param content 文档内容
     * @param modelId 模型ID，null时使用默认模型
     * @return 向量存储实例，包含文档的所有向量化片段
     */
    public static EmbeddingStore<Embedding> createDocumentVectors(String content, String modelId) {
        log.info("创建文档向量，使用模型：{}", modelId == null ? "默认" : modelId);
        Document document = Document.from(content);
        EmbeddingModel embeddingModel = createEmbeddingModel(modelId);
        EmbeddingStore<Embedding> embeddingStore = new InMemoryEmbeddingStore<>();

        DocumentSplitter splitter = new DocumentByParagraphSplitter(2048, 100);
        List<TextSegment> segments = splitter.split(document);
        List<Embedding> embeddings = embedInBatches(embeddingModel, segments, 10);
        embeddingStore.addAll(embeddings);

        return embeddingStore;
    }

    /**
     * 按批次对文本片段进行嵌入，解决在线模型单次最大批量限制问题
     *
     * @param embeddingModel 嵌入模型
     * @param segments       文本片段列表
     * @param batchSize      每批最大片段数
     * @return 所有片段对应的嵌入向量，顺序与输入segments一致
     */
    public static List<Embedding> embedInBatches(EmbeddingModel embeddingModel, List<TextSegment> segments, int batchSize) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        if (batchSize <= 0) {
            batchSize = 10;
        }
        List<Embedding> result = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(i, end);
            List<Embedding> batchEmbeddings = embeddingModel.embedAll(batch).content();
            result.addAll(batchEmbeddings);
        }
        return result;
    }

    /**
     * 将文本转换为嵌入向量（使用默认模型）
     * 
     * 实现思路：
     * 1. 使用默认激活的嵌入模型进行文本向量化
     * 2. 委托给重载方法embedText(text, null)处理
     * 3. 提供简化的API接口，方便快速向量化操作
     * 
     * @param text 待嵌入的文本
     * @return 嵌入向量
     */
    public static Embedding embedText(String text) {
        return embedText(text, null);
    }

    /**
     * 将文本转换为嵌入向量（使用指定模型）
     * 
     * 实现思路：
     * 1. 记录文本向量化操作，包含使用的模型信息
     * 2. 根据modelId创建对应的嵌入模型实例
     * 3. 调用模型的embed方法对文本进行向量化
     * 4. 提取并返回向量化结果的内容部分
     * 5. 支持不同类型的嵌入模型和文本长度
     * 
     * @param text 待嵌入的文本
     * @param modelId 模型ID，null时使用默认模型
     * @return 文本对应的嵌入向量
     */
    public static Embedding embedText(String text, String modelId) {
        log.info("生成文本嵌入向量，使用模型：{}", modelId == null ? "默认" : modelId);
        EmbeddingModel embeddingModel = createEmbeddingModel(modelId);
        return embeddingModel.embed(text).content();
    }

    /**
     * 切换默认嵌入模型
     * 
     * 实现思路：
     * 1. 调用ModelConfig的switchEmbeddingModel方法尝试切换
     * 2. 验证新的模型ID是否存在于配置中
     * 3. 如果切换成功，记录成功日志
     * 4. 如果切换失败，记录警告日志并说明原因
     * 5. 返回切换操作的结果状态
     * 6. 切换后的模型将作为新的默认模型使用
     * 
     * @param modelId 新的模型ID
     * @return 是否切换成功
     */
    public static boolean switchModel(String modelId) {
        boolean success = modelConfig.switchEmbeddingModel(modelId);
        if (success) {
            log.info("成功切换嵌入模型为: {}", modelId);
        } else {
            log.warn("切换嵌入模型失败: 未找到模型ID {}", modelId);
        }
        return success;
    }

    /**
     * 清除所有模型缓存
     * 
     * 实现思路：
     * 1. 清空ConcurrentHashMap中的所有模型实例
     * 2. 释放内存中缓存的嵌入模型资源
     * 3. 记录缓存清理操作的日志
     * 4. 下次访问时将重新创建模型实例
     * 5. 适用于模型配置更新或内存优化场景
     */
    public static void clearModelCache() {
        modelCache.clear();
        log.info("已清除模型缓存");
    }

    /**
     * 刷新指定模型的缓存
     * 
     * 实现思路：
     * 1. 从ConcurrentHashMap中移除指定modelId的模型实例
     * 2. 释放该模型占用的内存资源
     * 3. 记录模型缓存移除操作的日志
     * 4. 下次访问该模型时将重新创建实例
     * 5. 适用于单个模型配置更新的场景
     * 
     * @param modelId 要刷新缓存的模型ID
     */
    public static void refreshModelCache(String modelId) {
        modelCache.remove(modelId);
        log.info("已移除模型缓存: {}", modelId);
    }
}