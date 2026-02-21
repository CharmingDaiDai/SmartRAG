package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.dto.CreateKnowledgeBaseRequest;
import com.mtmn.smartdoc.dto.KnowledgeBaseResponse;
import com.mtmn.smartdoc.po.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     *
     * @param request 创建请求
     * @param userId  用户 ID
     * @return 知识库响应
     */
    KnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request, Long userId);

    /**
     * 查询用户的知识库列表
     *
     * @param userId 用户 ID
     * @return 知识库列表
     */
    List<KnowledgeBaseResponse> listKnowledgeBases(Long userId);

    /**
     * 获取知识库详情
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @return 知识库响应
     */
    KnowledgeBaseResponse getKnowledgeBase(Long kbId, Long userId);

    /**
     * 获取知识库实体
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @return 知识库实体
     */
    KnowledgeBase getKnowledgeBaseEntity(Long kbId, Long userId);

    /**
     * 删除知识库
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     */
    void deleteKnowledgeBase(Long kbId, Long userId);
}