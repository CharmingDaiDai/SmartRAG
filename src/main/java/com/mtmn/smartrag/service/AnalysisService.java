package com.mtmn.smartrag.service;

import com.mtmn.smartrag.vo.KeywordsResult;
import com.mtmn.smartrag.vo.PolishResult;
import com.mtmn.smartrag.vo.SecurityResult;
import com.mtmn.smartrag.vo.SummaryResult;

/**
 * @author charmingdaidai
 */
public interface AnalysisService {

    /**
     * 生成文档摘要
     *
     * @param content 文档内容
     * @return 摘要结果
     */
    SummaryResult generateSummary(String content);

    /**
     * 基于已上传文档生成摘要并保存
     *
     * @param documentId 文档ID
     * @return 摘要结果
     */
    SummaryResult generateSummaryFromDocument(Long documentId);

    /**
     * 提取文档关键词
     *
     * @param content 文档内容
     * @return 关键词结果
     */
    KeywordsResult extractKeywords(String content);

    /**
     * 基于已上传文档提取关键词并保存
     *
     * @param documentId 文档ID
     * @return 关键词结果
     */
    KeywordsResult extractKeywordsFromDocument(Long documentId);

    /**
     * 文档润色
     *
     * @param content    文档内容
     * @param polishType 润色类型：formal(正式), concise(简洁), creative(创意)
     * @return 润色结果
     */
    PolishResult polishDocument(String content, String polishType);

    /**
     * 基于已上传文档进行润色并保存
     *
     * @param documentId 文档ID
     * @param polishType 润色类型：formal(正式), concise(简洁), creative(创意)
     * @return 润色结果
     */
    PolishResult polishDocumentFromDocument(Long documentId, String polishType);

    /**
     * 敏感信息检测
     *
     * @param content 文档内容
     * @return 敏感信息检测结果
     */
    SecurityResult detectSensitiveInfo(String content);

    /**
     * 基于已上传文档检测敏感信息并保存
     *
     * @param documentId 文档ID
     * @return 敏感信息检测结果
     */
    SecurityResult detectSensitiveInfoFromDocument(Long documentId);
}