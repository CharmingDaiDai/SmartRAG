package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.DocumentPreviewMetaResponse;
import com.mtmn.smartdoc.dto.DocumentPreviewTextResponse;
import com.mtmn.smartdoc.dto.DocumentResponse;
import com.mtmn.smartdoc.dto.IndexingTaskResponse;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.service.DocumentService;
import com.mtmn.smartdoc.service.IndexingTaskService;
import com.mtmn.smartdoc.vo.IndexingTaskProgressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档控制器
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document Management", description = "文档管理相关接口")
public class DocumentController {

    private final DocumentService documentService;
    private final IndexingTaskService indexingTaskService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传单个文档到指定知识库")
    public ApiResponse<DocumentResponse> uploadDocument(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @Parameter(description = "上传的文件") @RequestParam MultipartFile file,
            @Parameter(description = "文档标题（可选）") @RequestParam(required = false) String title,
            @AuthenticationPrincipal User user) {

        log.info("Uploading document to knowledge base {}: {}", kbId, file.getOriginalFilename());

        DocumentResponse response = documentService.uploadDocument(kbId, user.getId(), file, title);

        return ApiResponse.success("文档上传成功", response);
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/batch-upload")
    @Operation(summary = "批量上传文档", description = "批量上传多个文档到指定知识库")
    public ApiResponse<List<DocumentResponse>> batchUploadDocuments(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @Parameter(description = "上传的文件数组") @RequestParam MultipartFile[] files,
            @Parameter(description = "文档标题数组（可选）") @RequestParam(required = false) String[] titles,
            @AuthenticationPrincipal User user) {

        log.info("Batch uploading {} documents to knowledge base {}", files.length, kbId);

        List<DocumentResponse> responses = documentService.uploadDocuments(
                kbId, user.getId(), files, titles);

        return ApiResponse.success("文档批量上传成功", responses);
    }

    /**
     * 分页获取所有文档列表
     */
    @GetMapping
    @Operation(summary = "分页获取所有文档", description = "分页查询当前用户的所有文档")
    public ApiResponse<org.springframework.data.domain.Page<DocumentResponse>> listAllDocuments(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {

        log.debug("Listing all documents for user {}, page={}, size={}", user.getId(), page, size);

        org.springframework.data.domain.Page<DocumentResponse> responses = documentService.listAllDocuments(user.getId(), page, size);

        return ApiResponse.success(responses);
    }

    /**
     * 分页获取知识库的文档列表
     */
    @GetMapping("/{kbId}")
    @Operation(summary = "分页获取知识库文档", description = "分页查询指定知识库的文档列表")
    public ApiResponse<org.springframework.data.domain.Page<DocumentResponse>> listDocumentsByKb(
            @Parameter(description = "知识库ID") @PathVariable Long kbId,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {

        log.debug("Listing documents for knowledge base {}, page={}, size={}", kbId, page, size);

        org.springframework.data.domain.Page<DocumentResponse> responses = documentService.listDocumentsByKb(kbId, user.getId(), page, size);

        return ApiResponse.success(responses);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/detail/{documentId}")
    @Operation(summary = "获取文档详情", description = "根据文档ID获取详细信息")
    public ApiResponse<DocumentResponse> getDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.debug("Getting document: {}", documentId);

        DocumentResponse response = documentService.getDocument(documentId, user.getId());

        return ApiResponse.success(response);
    }

    /**
     * 获取文档预览元信息
     */
    @GetMapping("/{documentId}/preview/meta")
    @Operation(summary = "获取文档预览元信息", description = "返回文档预览类型与能力信息")
    public ApiResponse<DocumentPreviewMetaResponse> getPreviewMeta(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.debug("Getting preview meta for document: {}", documentId);
        DocumentPreviewMetaResponse response = documentService.getPreviewMeta(documentId, user.getId());
        return ApiResponse.success(response);
    }

    /**
     * 获取文档文本预览（分页）
     */
    @GetMapping("/{documentId}/preview/text")
    @Operation(summary = "获取文档文本预览", description = "分页返回文档文本片段")
    public ApiResponse<DocumentPreviewTextResponse> previewText(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Parameter(description = "页码（0-based）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页段数") @RequestParam(defaultValue = "4") int size,
            @AuthenticationPrincipal User user) {

        log.debug("Preview text for document: {}, page={}, size={}", documentId, page, size);
        DocumentPreviewTextResponse response = documentService.previewText(documentId, user.getId(), page, size);
        return ApiResponse.success(response);
    }

    /**
     * 原样预览文档（流式）
     */
    @GetMapping("/{documentId}/preview/raw")
    @Operation(summary = "原样预览文档", description = "以流式方式返回原始文件内容（inline）")
    public void previewRaw(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user,
            HttpServletResponse response) {

        log.debug("Preview raw file for document: {}", documentId);
        documentService.previewRaw(documentId, user.getId(), response);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "删除文档", description = "根据文档ID删除指定的文档")
    public ApiResponse<Void> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Deleting document: {}", documentId);

        documentService.deleteDocument(documentId, user.getId());

        return ApiResponse.success("文档删除成功", null);
    }

    /**
     * 批量删除文档
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除文档", description = "批量删除多个文档")
    public ApiResponse<Void> batchDeleteDocuments(
            @RequestBody List<Long> documentIds,
            @AuthenticationPrincipal User user) {

        log.info("Batch deleting {} documents", documentIds.size());

        documentService.deleteDocuments(documentIds, user.getId());

        return ApiResponse.success("文档批量删除成功", null);
    }

    /**
     * 查询索引进度（轮询接口）
     */
    @GetMapping("/index-progress")
    @Operation(summary = "查询索引进度", description = "轮询获取指定知识库的最新索引任务进度，前端按 1-2s 间隔调用")
    public ApiResponse<IndexingTaskProgressVO> getIndexProgress(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {

        IndexingTaskProgressVO progress = indexingTaskService.getLatestTaskProgress(user.getId(), kbId);
        return ApiResponse.success(progress);
    }

    /**
     * 触发文档索引（异步）
     */
    @PostMapping("/{documentId}/index")
    @Operation(summary = "触发文档索引", description = "异步触发指定文档的索引操作，返回任务信息")
    public ApiResponse<IndexingTaskResponse> triggerIndexing(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Triggering indexing for document: {}", documentId);

        IndexingTaskResponse response = documentService.triggerIndexing(documentId, user.getId());

        return ApiResponse.success("索引任务已提交", response);
    }

    /**
     * 触发批量索引（异步）
     */
    @PostMapping("/batch-index")
    @Operation(summary = "触发批量索引", description = "异步触发指定知识库的批量索引操作，返回任务信息")
    public ApiResponse<IndexingTaskResponse> triggerBatchIndexing(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {

        log.info("Triggering batch indexing for knowledge base: {}", kbId);

        IndexingTaskResponse response = documentService.triggerBatchIndexing(kbId, user.getId());

        if (response == null) {
            return ApiResponse.success("没有待索引的文档", null);
        }

        return ApiResponse.success("索引任务已提交", response);
    }

    /**
     * 重建文档索引（异步，基于现有 Chunk）
     */
    @PostMapping("/{documentId}/rebuild-index")
    @Operation(summary = "重建文档索引", description = "异步重建索引，基于数据库中现有的 Chunk，不重新切分文档")
    public ApiResponse<IndexingTaskResponse> rebuildIndex(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Rebuilding index for document: {}", documentId);

        IndexingTaskResponse response = documentService.rebuildIndex(documentId, user.getId());

        return ApiResponse.success("重建索引任务已提交", response);
    }

    /**
     * 批量重建文档索引（异步）
     */
    @PostMapping("/batch-rebuild-index")
    @Operation(summary = "批量重建文档索引", description = "异步批量重建多个文档的索引")
    public ApiResponse<IndexingTaskResponse> batchRebuildIndex(
            @RequestBody List<Long> documentIds,
            @AuthenticationPrincipal User user) {

        log.info("Batch rebuilding index for {} documents", documentIds.size());

        IndexingTaskResponse response = documentService.batchRebuildIndex(documentIds, user.getId());

        if (response == null) {
            return ApiResponse.success("没有文档需要重建索引", null);
        }

        return ApiResponse.success("重建索引任务已提交", response);
    }
}