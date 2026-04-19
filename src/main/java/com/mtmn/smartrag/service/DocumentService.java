package com.mtmn.smartrag.service;

import com.mtmn.smartrag.dto.DocumentResponse;
import com.mtmn.smartrag.dto.DocumentPreviewMetaResponse;
import com.mtmn.smartrag.dto.DocumentPreviewTextResponse;
import com.mtmn.smartrag.dto.IndexingTaskResponse;
import com.mtmn.smartrag.po.DocumentPo;
import jakarta.servlet.http.HttpServletResponse;
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
     * 分页获取所有文档列表
     *
     * @param userId 用户 ID
     * @param page   页码（从0开始）
     * @param size   每页大小
     * @return 文档分页结果
     */
    org.springframework.data.domain.Page<DocumentResponse> listAllDocuments(Long userId, int page, int size);

    /**
     * 分页获取知识库的文档列表
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @param page   页码（从0开始）
     * @param size   每页大小
     * @return 文档分页结果
     */
    org.springframework.data.domain.Page<DocumentResponse> listDocumentsByKb(Long kbId, Long userId, int page, int size);

    /**
     * 获取知识库的文档列表（不分页，内部使用）
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
    DocumentPo getDocumentEntity(Long documentId, Long userId);

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
     * 触发文档索引构建（异步）
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 任务响应
     */
    IndexingTaskResponse triggerIndexing(Long documentId, Long userId);

    /**
     * 批量触发索引构建（异步）
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @return 任务响应
     */
    IndexingTaskResponse triggerBatchIndexing(Long kbId, Long userId);

    /**
     * 重建文档索引（异步，基于现有 Chunk）
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 任务响应
     */
    IndexingTaskResponse rebuildIndex(Long documentId, Long userId);

    /**
     * 批量重建文档索引（异步）
     *
     * @param documentIds 文档 ID 列表
     * @param userId      用户 ID
     * @return 任务响应
     */
    IndexingTaskResponse batchRebuildIndex(List<Long> documentIds, Long userId);

    /**
     * 获取文档预览元信息
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 预览元信息
     */
    DocumentPreviewMetaResponse getPreviewMeta(Long documentId, Long userId);

    /**
     * 获取文档文本预览（分页）
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @param page       页码（0-based）
     * @param size       每页段数
     * @return 文本预览分页结果
     */
    DocumentPreviewTextResponse previewText(Long documentId, Long userId, int page, int size);

    /**
     * 原样预览文档流
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @param response   HTTP 响应
     */
    void previewRaw(Long documentId, Long userId, HttpServletResponse response);
}