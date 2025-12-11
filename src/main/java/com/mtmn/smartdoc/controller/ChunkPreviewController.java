//package com.mtmn.smartdoc.controller;
//
//import com.mtmn.smartdoc.common.ApiResponse;
//import com.mtmn.smartdoc.dto.ChunkPreviewResponse;
//import com.mtmn.smartdoc.dto.TreeNodePreviewResponse;
//import com.mtmn.smartdoc.po.User;
//import com.mtmn.smartdoc.service.ChunkPreviewService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * Chunk 和 TreeNode 预览控制器
// *
// * @author charmingdaidai
// * @version 2.0
// * @date 2025-11-19
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/preview")
//@RequiredArgsConstructor
//public class ChunkPreviewController {
//
//    private final ChunkPreviewService chunkPreviewService;
//
//    /**
//     * 获取文档的 Chunk 列表
//     */
//    @GetMapping("/chunks/document/{documentId}")
//    public ApiResponse<List<ChunkPreviewResponse>> listChunksByDocument(
//            @PathVariable Long documentId,
//            @AuthenticationPrincipal User user) {
//
//        log.debug("Listing chunks for document: {}", documentId);
//
//        List<ChunkPreviewResponse> responses = chunkPreviewService.listChunks(documentId, user.getId());
//
//        return ApiResponse.success(responses);
//    }
//
//    /**
//     * 获取知识库的 Chunk 列表 (分页)
//     */
//    @GetMapping("/chunks/kb/{kbId}")
//    public ApiResponse<List<ChunkPreviewResponse>> listChunksByKb(
//            @PathVariable Long kbId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @AuthenticationPrincipal User user) {
//
//        log.debug("Listing chunks for KB: {}, page: {}, size: {}", kbId, page, size);
//
//        List<ChunkPreviewResponse> responses = chunkPreviewService.listChunksByKb(
//                kbId, user.getId(), page, size);
//
//        return ApiResponse.success(responses);
//    }
//
//    /**
//     * 获取单个 Chunk 详情
//     */
//    @GetMapping("/chunks/{chunkId}")
//    public ApiResponse<ChunkPreviewResponse> getChunk(
//            @PathVariable Long chunkId,
//            @AuthenticationPrincipal User user) {
//
//        log.debug("Getting chunk: {}", chunkId);
//
//        ChunkPreviewResponse response = chunkPreviewService.getChunk(chunkId, user.getId());
//
//        return ApiResponse.success(response);
//    }
//
//    /**
//     * 更新 Chunk 内容
//     */
//    @PutMapping("/chunks/{chunkId}")
//    public ApiResponse<Void> updateChunk(
//            @PathVariable Long chunkId,
//            @RequestBody String newContent,
//            @AuthenticationPrincipal User user) {
//
//        log.info("Updating chunk: {}", chunkId);
//
//        chunkPreviewService.updateChunk(chunkId, user.getId(), newContent);
//
//        return ApiResponse.success(null);
//    }
//
//    /**
//     * 获取文档的树形结构
//     */
//    @GetMapping("/tree/document/{documentId}")
//    public ApiResponse<List<TreeNodePreviewResponse>> getTreeStructureByDocument(
//            @PathVariable Long documentId,
//            @AuthenticationPrincipal User user) {
//
//        log.debug("Getting tree structure for document: {}", documentId);
//
//        List<TreeNodePreviewResponse> responses = chunkPreviewService.getTreeStructure(
//                documentId, user.getId());
//
//        return ApiResponse.success(responses);
//    }
//
//    /**
//     * 获取知识库的树形结构
//     */
//    @GetMapping("/tree/kb/{kbId}")
//    public ApiResponse<List<TreeNodePreviewResponse>> getTreeStructureByKb(
//            @PathVariable Long kbId,
//            @AuthenticationPrincipal User user) {
//
//        log.debug("Getting tree structure for KB: {}", kbId);
//
//        List<TreeNodePreviewResponse> responses = chunkPreviewService.getTreeStructureByKb(
//                kbId, user.getId());
//
//        return ApiResponse.success(responses);
//    }
//
//    /**
//     * 获取单个 TreeNode 详情
//     */
//    @GetMapping("/tree/{nodeId}")
//    public ApiResponse<TreeNodePreviewResponse> getTreeNode(
//            @PathVariable Long nodeId,
//            @AuthenticationPrincipal User user) {
//
//        log.debug("Getting tree node: {}", nodeId);
//
//        TreeNodePreviewResponse response = chunkPreviewService.getTreeNode(nodeId, user.getId());
//
//        return ApiResponse.success(response);
//    }
//
//    /**
//     * 更新 TreeNode 内容
//     */
//    @PutMapping("/tree/{nodeId}")
//    public ApiResponse<Void> updateTreeNode(
//            @PathVariable Long nodeId,
//            @RequestBody String newContent,
//            @AuthenticationPrincipal User user) {
//
//        log.info("Updating tree node: {}", nodeId);
//
//        chunkPreviewService.updateTreeNode(nodeId, user.getId(), newContent);
//
//        return ApiResponse.success(null);
//    }
//}