package com.mtmn.smartrag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话详情响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSessionDetailResponse {

    private Long kbId;

    private String sessionId;

    private long messageCount;

    private LocalDateTime lastActiveAt;

    private List<ConversationMessageResponse> messages;
}
