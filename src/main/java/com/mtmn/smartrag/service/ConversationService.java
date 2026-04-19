package com.mtmn.smartrag.service;

import com.mtmn.smartrag.dto.ConversationSessionDetailResponse;
import com.mtmn.smartrag.dto.ConversationSessionResponse;
import com.mtmn.smartrag.po.Conversation;
import org.springframework.data.domain.Page;

/**
 * 对话历史服务
 */
public interface ConversationService {

    Page<ConversationSessionResponse> listSessions(Long kbId, Long userId, int page, int size);

    ConversationSessionDetailResponse getSessionDetail(Long kbId, Long userId, String sessionId);

    void deleteSession(Long kbId, Long userId, String sessionId);

    Conversation saveConversation(Conversation conversation);

    String buildHistoryText(Long kbId, Long userId, String sessionId, Integer historyWindow);

    String resolveSessionId(String sessionId);
}
