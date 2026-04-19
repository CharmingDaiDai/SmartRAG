package com.mtmn.smartrag.controller;

import com.mtmn.smartrag.common.ApiResponse;
import com.mtmn.smartrag.model.factory.ModelFactory;
import com.mtmn.smartrag.po.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型控制器
 *
 * @author charmingdaidai
 * @version 1.0
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/models")
@Tag(name = "Model Management", description = "模型管理相关接口")
public class ModelController {

    private final ModelFactory modelFactory;

    @GetMapping("/llms")
    @Operation(summary = "获取可用LLM模型列表", description = "返回系统中可用的LLM模型名称列表")
    public ApiResponse<List<String>> getAvailableLLMModels(@AuthenticationPrincipal User user) {
        log.info("User {} is requesting available LLM models", user.getId());

        List<String> availableLLMModels = modelFactory.getAvailableLLMModels();

        return ApiResponse.success(availableLLMModels);
    }

    @GetMapping("/embeddings")
    @Operation(summary = "获取可用嵌入模型列表", description = "返回系统中可用的嵌入模型名称列表")
    public ApiResponse<List<String>> getAvailableEmbeddingModels(@AuthenticationPrincipal User user) {
        log.info("User {} is requesting available embedding models", user.getId());

        List<String> availableEmbeddingModels = modelFactory.getAvailableEmbeddingModels();

        return ApiResponse.success(availableEmbeddingModels);
    }

    @GetMapping("/reranks")
    @Operation(summary = "获取可用重排序模型列表", description = "返回系统中可用的重排序模型名称列表")
    public ApiResponse<List<String>> getAvailableRerankModels(@AuthenticationPrincipal User user) {
        log.info("User {} is requesting available rerank models", user.getId());

        List<String> availableRerankModels = modelFactory.getAvailableRerankModels();

        return ApiResponse.success(availableRerankModels);
    }

}