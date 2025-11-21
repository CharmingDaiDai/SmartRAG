package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.DocumentResponse;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

        return ApiResponse.success(response);
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

        return ApiResponse.success(responses);
    }

    /**
     * 获取知识库的文档列表
     */
    @GetMapping
    @Operation(summary = "获取文档列表", description = "获取指定知识库的所有文档列表")
    public ApiResponse<List<DocumentResponse>> listDocuments(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {

        log.debug("Listing documents for knowledge base {}", kbId);

        List<DocumentResponse> responses = documentService.listDocuments(kbId, user.getId());

        return ApiResponse.success(responses);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "获取文档详情", description = "根据文档ID获取详细信息")
    public ApiResponse<DocumentResponse> getDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.debug("Getting document: {}", documentId);

        DocumentResponse response = documentService.getDocument(documentId, user.getId());

        return ApiResponse.success(response);
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

        return ApiResponse.success(null);
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

        return ApiResponse.success(null);
    }

    /**
     * 触发文档索引
     */
    @PostMapping("/{documentId}/index")
    @Operation(summary = "触发文档索引", description = "触发指定文档的索引操作")
    public ApiResponse<Void> triggerIndexing(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Triggering indexing for document: {}", documentId);

        documentService.triggerIndexing(documentId, user.getId());

        return ApiResponse.success(null);
    }

    /**
     * 触发批量索引
     */
    @PostMapping("/batch-index")
    @Operation(summary = "触发批量索引", description = "触发指定知识库的批量索引操作")
    public ApiResponse<Void> triggerBatchIndexing(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {

        log.info("Triggering batch indexing for knowledge base: {}", kbId);

        documentService.triggerBatchIndexing(kbId, user.getId());

        return ApiResponse.success(null);
    }
}