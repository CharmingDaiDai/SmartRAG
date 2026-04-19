package com.mtmn.smartrag.service;

import com.mtmn.smartrag.model.client.LLMClient;
import com.mtmn.smartrag.vo.SecurityResult;

import java.util.List;

/**
 * 大语言模型服务接口
 * 负责创建和管理不同的聊天模型，提供文本生成功能
 *
 * @author charmingdaidai
 */
public interface LLMService {

    /**
     * 创建聊天语言模型 - 使用当前激活的模型配置
     *
     * @return 聊天模型实例
     */
    LLMClient createLLMClient();

    /**
     * 创建聊天语言模型 - 根据指定的modelId
     *
     * @param modelId 模型ID，如果为null则使用当前激活的模型
     * @return 对应的聊天语言模型实例
     */
    LLMClient createLLMClient(String modelId);

    /**
     * 生成文本摘要
     *
     * @param content 文档内容
     * @return 摘要文本
     */
    String generateSummary(String content);

    /**
     * 提取关键词
     *
     * @param content 文档内容
     * @return 关键词列表
     */
    List<String> extractKeywords(String content);

    /**
     * 文档润色
     *
     * @param content    文档内容
     * @param polishType 润色类型
     * @return 润色后的文本
     */
    String polishDocument(String content, String polishType);

    /**
     * 检测敏感信息
     *
     * @param content 文档内容
     * @return 敏感信息检测结果
     */
    SecurityResult detectSensitiveInfo(String content);
}