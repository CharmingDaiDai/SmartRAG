package com.mtmn.smartdoc.service.model.factory;

import com.mtmn.smartdoc.service.model.client.EmbeddingClient;
import com.mtmn.smartdoc.service.model.client.LLMClient;
import com.mtmn.smartdoc.service.model.client.RerankClient;

import java.util.List;

/**
 * 模型工厂接口
 * 
 * <p>统一创建各类模型客户端的工厂接口</p>
 * 
 * <p>职责:</p>
 * <ul>
 *   <li>根据modelId创建对应的客户端实例</li>
 *   <li>管理客户端缓存</li>
 *   <li>处理客户端生命周期</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
public interface ModelFactory {
    
    /**
     * 创建LLM客户端
     * 
     * @param modelId 模型ID,如 zhipuai-glm4
     * @return LLM客户端实例
     * @throws com.mtmn.smartdoc.service.model.exception.ModelException 当模型不存在或配置错误时
     */
    LLMClient createLLMClient(String modelId);
    
    /**
     * 创建Embedding客户端
     * 
     * @param modelId 模型ID,如 xinference-bge-m3
     * @return Embedding客户端实例
     * @throws com.mtmn.smartdoc.service.model.exception.ModelException 当模型不存在或配置错误时
     */
    EmbeddingClient createEmbeddingClient(String modelId);
    
    /**
     * 创建Rerank客户端(预留)
     * 
     * @param modelId 模型ID
     * @return Rerank客户端实例
     * @throws com.mtmn.smartdoc.service.model.exception.ModelException 当模型不存在或配置错误时
     */
    RerankClient createRerankClient(String modelId);
    
    /**
     * 创建默认LLM客户端(使用配置的active模型)
     * 
     * @return LLM客户端实例
     */
    LLMClient createDefaultLLMClient();
    
    /**
     * 创建默认Embedding客户端
     * 
     * @return Embedding客户端实例
     */
    EmbeddingClient createDefaultEmbeddingClient();
    
    /**
     * 创建默认Rerank客户端(预留)
     * 
     * @return Rerank客户端实例
     */
    RerankClient createDefaultRerankClient();
    
    /**
     * 获取所有可用的LLM模型ID列表
     * 
     * @return LLM模型ID列表
     */
    List<String> getAvailableLLMModels();
    
    /**
     * 获取所有可用的Embedding模型ID列表
     * 
     * @return Embedding模型ID列表
     */
    List<String> getAvailableEmbeddingModels();
    
    /**
     * 获取所有可用的Rerank模型ID列表(预留)
     * 
     * @return Rerank模型ID列表
     */
    List<String> getAvailableRerankModels();
    
    /**
     * 清除指定模型的缓存
     * 
     * @param modelId 模型ID
     */
    void clearCache(String modelId);
    
    /**
     * 清除所有缓存
     */
    void clearAllCache();
    
    /**
     * 检查模型是否可用
     * 
     * @param modelId 模型ID
     * @return 是否可用
     */
    boolean isModelAvailable(String modelId);
}
