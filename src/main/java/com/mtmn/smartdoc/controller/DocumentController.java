package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.DocumentResponse;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.service.DocumentService;
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
 * @date 2025-11-19
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ApiResponse<DocumentResponse> uploadDocument(
            @RequestParam Long kbId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String title,
            @AuthenticationPrincipal User user) {

        log.info("Uploading document to knowledge base {}: {}", kbId, file.getOriginalFilename());

        DocumentResponse response = documentService.uploadDocument(kbId, user.getId(), file, title);

        return ApiResponse.success(response);
    }

    /**
     * 批量上传文档
     */
    @PostMapping("/batch-upload")
    public ApiResponse<List<DocumentResponse>> batchUploadDocuments(
            @RequestParam Long kbId,
            @RequestParam MultipartFile[] files,
            @RequestParam(required = false) String[] titles,
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
    public ApiResponse<List<DocumentResponse>> listDocuments(
            @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {

        log.debug("Listing documents for knowledge base {}", kbId);

        List<DocumentResponse> responses = documentService.listDocuments(kbId, user.getId());

        return ApiResponse.success(responses);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    public ApiResponse<DocumentResponse> getDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.debug("Getting document: {}", documentId);

        DocumentResponse response = documentService.getDocument(documentId, user.getId());

        return ApiResponse.success(response);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Deleting document: {}", documentId);

        documentService.deleteDocument(documentId, user.getId());

        return ApiResponse.success(null);
    }

    /**
     * 批量删除文档
     */
    @DeleteMapping("/batch")
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
    public ApiResponse<Void> triggerIndexing(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Triggering indexing for document: {}", documentId);

        documentService.triggerIndexing(documentId, user.getId());

        return ApiResponse.success(null);
    }

    /**
     * 触发批量索引
     */
    @PostMapping("/batch-index")
    public ApiResponse<Void> triggerBatchIndexing(
            @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {

        log.info("Triggering batch indexing for knowledge base: {}", kbId);

        documentService.triggerBatchIndexing(kbId, user.getId());

        return ApiResponse.success(null);
    }
}