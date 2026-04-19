package com.mtmn.smartrag.model.client;

import com.mtmn.smartrag.model.dto.EmbeddingRequest;
import com.mtmn.smartrag.model.dto.EmbeddingResponse;
import dev.langchain4j.data.embedding.Embedding;

import java.util.List;

/**
 * Embedding客户端统一接口
 *
 * <p>提供文本向量化功能,支持单文本和批量文本的向量化</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
public interface EmbeddingClient {

    /**
     * 单文本向量化
     *
     * @param text 待向量化的文本
     * @return 向量表示, float列表
     */
    Embedding embed(String text);

    /**
     * 批量文本向量化
     *
     * <p>批量处理可以提高效率,减少网络开销</p>
     *
     * @param texts 待向量化的文本列表
     * @return 向量列表, 每个元素对应一个输入文本的向量
     */
    List<Embedding> embedBatch(List<String> texts);

    /**
     * 结构化请求向量化
     *
     * @param request 向量化请求对象
     * @return 向量化响应对象, 包含向量和元数据
     */
    EmbeddingResponse embed(EmbeddingRequest request);

    /**
     * 获取向量维度
     *
     * @return 向量维度大小
     */
    int getDimension();

    /**
     * 获取模型提供商类型
     *
     * @return 提供商类型, 如 "zhipuai", "qwen", "xinference", "openai"
     */
    String getProviderType();

    /**
     * 获取模型ID
     *
     * @return 模型ID, 如 "xinference-bge-m3"
     */
    String getModelId();

    /**
     * 获取模型名称
     *
     * @return 模型名称, 如 "bge-m3"
     */
    String getModelName();
}