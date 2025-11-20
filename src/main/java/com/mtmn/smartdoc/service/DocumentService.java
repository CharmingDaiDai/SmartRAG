package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.dto.DocumentResponse;
import com.mtmn.smartdoc.po.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档服务接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface DocumentService {

    /**
     * 上传文档到知识库
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @param file   文件
     * @param title  文档标题 (可选)
     * @return 文档响应
     */
    DocumentResponse uploadDocument(Long kbId, Long userId, MultipartFile file, String title);

    /**
     * 批量上传文档
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @param files  文件数组
     * @param titles 标题数组 (可选)
     * @return 文档响应列表
     */
    List<DocumentResponse> uploadDocuments(Long kbId, Long userId, MultipartFile[] files, String[] titles);

    /**
     * 获取知识库的文档列表
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @return 文档列表
     */
    List<DocumentResponse> listDocuments(Long kbId, Long userId);

    /**
     * 获取文档详情
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 文档响应
     */
    DocumentResponse getDocument(Long documentId, Long userId);

    /**
     * 获取文档实体
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 文档实体
     */
    Document getDocumentEntity(Long documentId, Long userId);

    /**
     * 删除文档
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     */
    void deleteDocument(Long documentId, Long userId);

    /**
     * 批量删除文档
     *
     * @param documentIds 文档 ID 列表
     * @param userId      用户 ID
     */
    void deleteDocuments(List<Long> documentIds, Long userId);

    /**
     * 触发文档索引构建
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     */
    void triggerIndexing(Long documentId, Long userId);

    /**
     * 批量触发索引构建
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     */
    void triggerBatchIndexing(Long kbId, Long userId);
}