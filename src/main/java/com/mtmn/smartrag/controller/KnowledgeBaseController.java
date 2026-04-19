package com.mtmn.smartrag.controller;

import com.mtmn.smartrag.common.ApiResponse;
import com.mtmn.smartrag.dto.CreateKnowledgeBaseRequest;
import com.mtmn.smartrag.dto.KnowledgeBaseResponse;
import com.mtmn.smartrag.po.User;
import com.mtmn.smartrag.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库控制器
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base Management", description = "知识库管理相关接口")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping
    @Operation(summary = "创建知识库", description = "根据请求创建新的知识库")
    public ApiResponse<KnowledgeBaseResponse> createKnowledgeBase(
            @RequestBody CreateKnowledgeBaseRequest request,
            @AuthenticationPrincipal User user) {

        log.info("Creating knowledge base: {}, user: {}", request.getName(), user.getId());

        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(
                request, user.getId());

        return ApiResponse.success("知识库创建成功", response);
    }

    /**
     * 获取用户的知识库列表
     */
    @GetMapping
    @Operation(summary = "获取知识库列表", description = "获取当前用户的所有知识库列表")
    public ApiResponse<List<KnowledgeBaseResponse>> listKnowledgeBases(
            @AuthenticationPrincipal User user) {

        log.debug("Listing knowledge bases for user: {}", user.getId());

        List<KnowledgeBaseResponse> responses = knowledgeBaseService.listKnowledgeBases(user.getId());

        return ApiResponse.success(responses);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{kbId}")
    @Operation(summary = "获取知识库详情", description = "根据知识库ID获取详细信息")
    public ApiResponse<KnowledgeBaseResponse> getKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long kbId,
            @AuthenticationPrincipal User user) {

        log.debug("Getting knowledge base: {}, user: {}", kbId, user.getId());

        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBase(kbId, user.getId());

        return ApiResponse.success(response);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{kbId}")
    @Operation(summary = "删除知识库", description = "根据知识库ID删除指定的知识库")
    public ApiResponse<Void> deleteKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long kbId,
            @AuthenticationPrincipal User user) {

        log.info("Deleting knowledge base: {}, user: {}", kbId, user.getId());

        knowledgeBaseService.deleteKnowledgeBase(kbId, user.getId());

        return ApiResponse.success("知识库删除成功", null);
    }
}