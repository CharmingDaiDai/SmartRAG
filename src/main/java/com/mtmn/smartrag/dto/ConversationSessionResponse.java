package com.mtmn.smartrag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话列表项响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSessionResponse {

    private String sessionId;

    private String title;

    private String lastQuestion;

    private String lastResponse;

    private long messageCount;

    private LocalDateTime lastActiveAt;
}
