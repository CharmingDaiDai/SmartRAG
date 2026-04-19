package com.mtmn.smartrag.controller;

import com.mtmn.smartrag.common.ApiResponse;
import com.mtmn.smartrag.po.User;
import com.mtmn.smartrag.service.ChunkManageService;
import com.mtmn.smartrag.vo.ChunkVO;
import com.mtmn.smartrag.vo.TreeNodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 切分块查看与编辑控制器
 *
 * @author charmingdaidai
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Chunk Manage", description = "切分块查看与编辑接口")
public class ChunkManageController {

    private final ChunkManageService chunkManageService;

    /**
     * 获取文档的 Chunk 列表（NAIVE_RAG / HISEM_RAG_FAST）
     * GET /api/chunks?documentId=&kbId=
     */
    @GetMapping("/chunks")
    @Operation(summary = "获取文档 Chunk 列表", description = "按 chunkIndex 升序返回文档的所有切分块")
    public ApiResponse<List<ChunkVO>> listChunks(
            @RequestParam Long documentId,
            @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {
        log.debug("listChunks: documentId={}, kbId={}, userId={}", documentId, kbId, user.getId());
        return ApiResponse.success(chunkManageService.listChunks(documentId, kbId));
    }

    /**
     * 更新 Chunk 内容
     * PUT /api/chunks/{id}?kbId=
     */
    @PutMapping("/chunks/{id}")
    @Operation(summary = "更新 Chunk 内容", description = "更新切分块内容并同步到向量库")
    public ApiResponse<ChunkVO> updateChunk(
            @PathVariable Long id,
            @RequestParam Long kbId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        log.info("updateChunk: chunkId={}, kbId={}, userId={}", id, kbId, user.getId());
        String newContent = body.get("content");
        if (newContent == null || newContent.isBlank()) {
            return ApiResponse.badRequest("内容不能为空");
        }
        return ApiResponse.success(chunkManageService.updateChunk(id, newContent, kbId));
    }

    /**
     * 获取文档的 TreeNode 树形结构（HISEM_RAG）
     * GET /api/tree-nodes/tree?documentId=&kbId=
     */
    @GetMapping("/tree-nodes/tree")
    @Operation(summary = "获取文档 TreeNode 树形结构", description = "返回 HiSem-SADP 文档的树形节点结构")
    public ApiResponse<List<TreeNodeVO>> listTreeNodes(
            @RequestParam Long documentId,
            @RequestParam Long kbId,
            @AuthenticationPrincipal User user) {
        log.debug("listTreeNodes: documentId={}, kbId={}, userId={}", documentId, kbId, user.getId());
        return ApiResponse.success(chunkManageService.listTreeNodesAsTree(documentId, kbId));
    }

    /**
     * 更新 TreeNode 内容（仅叶子节点）
     * PUT /api/tree-nodes/{id}?kbId=
     */
    @PutMapping("/tree-nodes/{id}")
    @Operation(summary = "更新 TreeNode 内容", description = "更新树节点内容并同步到向量库（仅叶子节点）")
    public ApiResponse<Void> updateTreeNode(
            @PathVariable Long id,
            @RequestParam Long kbId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        log.info("updateTreeNode: nodeDbId={}, kbId={}, userId={}", id, kbId, user.getId());
        String newContent = body.get("content");
        if (newContent == null || newContent.isBlank()) {
            return ApiResponse.badRequest("内容不能为空");
        }
        chunkManageService.updateTreeNode(id, newContent, kbId);
        return ApiResponse.success(null);
    }
}
