package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.CreateKnowledgeBaseRequest;
import com.mtmn.smartdoc.dto.KnowledgeBaseResponse;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.service.KnowledgeBaseService;
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
 * @date 2025-11-19
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> createKnowledgeBase(
            @RequestBody CreateKnowledgeBaseRequest request,
            @AuthenticationPrincipal User user) {

        log.info("Creating knowledge base: {}, user: {}", request.getName(), user.getId());

        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(
                request, user.getId());

        return ApiResponse.success(response);
    }

    /**
     * 获取用户的知识库列表
     */
    @GetMapping
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
    public ApiResponse<KnowledgeBaseResponse> getKnowledgeBase(
            @PathVariable Long kbId,
            @AuthenticationPrincipal User user) {

        log.debug("Getting knowledge base: {}, user: {}", kbId, user.getId());

        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBase(kbId, user.getId());

        return ApiResponse.success(response);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{kbId}")
    public ApiResponse<Void> deleteKnowledgeBase(
            @PathVariable Long kbId,
            @AuthenticationPrincipal User user) {

        log.info("Deleting knowledge base: {}, user: {}", kbId, user.getId());

        knowledgeBaseService.deleteKnowledgeBase(kbId, user.getId());

        return ApiResponse.success(null);
    }
}