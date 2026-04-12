package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.ConversationSessionDetailResponse;
import com.mtmn.smartdoc.dto.ConversationSessionResponse;
import com.mtmn.smartdoc.exception.UnauthorizedAccessException;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话历史控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversation", description = "历史会话相关接口")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/sessions")
    @Operation(summary = "分页查询会话列表", description = "查询当前用户在知识库下的历史会话（按最近活跃时间倒序）")
    public ApiResponse<Page<ConversationSessionResponse>> listSessions(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedAccessException("用户未登录");
        }

        log.debug("查询会话列表: kbId={}, userId={}, page={}, size={}", kbId, user.getId(), page, size);
        Page<ConversationSessionResponse> sessions = conversationService.listSessions(kbId, user.getId(), page, size);
        return ApiResponse.success(sessions);
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "查询会话详情", description = "查询指定会话的完整问答轮次")
    public ApiResponse<ConversationSessionDetailResponse> getSessionDetail(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedAccessException("用户未登录");
        }

        log.debug("查询会话详情: kbId={}, userId={}, sessionId={}", kbId, user.getId(), sessionId);
        ConversationSessionDetailResponse detail = conversationService.getSessionDetail(kbId, user.getId(), sessionId);
        return ApiResponse.success(detail);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定会话下的全部轮次")
    public ApiResponse<Void> deleteSession(
            @Parameter(description = "知识库ID") @RequestParam Long kbId,
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            throw new UnauthorizedAccessException("用户未登录");
        }

        log.info("删除会话: kbId={}, userId={}, sessionId={}", kbId, user.getId(), sessionId);
        conversationService.deleteSession(kbId, user.getId(), sessionId);
        return ApiResponse.success("会话删除成功", null);
    }
}
