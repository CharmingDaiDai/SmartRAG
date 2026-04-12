package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.dto.ConversationMessageResponse;
import com.mtmn.smartdoc.dto.ConversationSessionDetailResponse;
import com.mtmn.smartdoc.dto.ConversationSessionResponse;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.po.Conversation;
import com.mtmn.smartdoc.repository.ConversationRepository;
import com.mtmn.smartdoc.service.ConversationService;
import com.mtmn.smartdoc.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 对话历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationSessionResponse> listSessions(Long kbId, Long userId, int page, int size) {
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Conversation> latestPage = conversationRepository.findLatestSessionsByKbIdAndUserId(kbId, userId, pageable);

        List<ConversationSessionResponse> sessions = latestPage.getContent().stream()
                .map(conversation -> ConversationSessionResponse.builder()
                        .sessionId(conversation.getSessionId())
                        .title(extractTitle(conversation.getQuery()))
                        .lastQuestion(conversation.getQuery())
                        .lastResponse(conversation.getResponse())
                        .messageCount(conversationRepository.countByKbIdAndUserIdAndSessionId(
                                kbId, userId, conversation.getSessionId()))
                        .lastActiveAt(conversation.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(sessions, pageable, latestPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationSessionDetailResponse getSessionDetail(Long kbId, Long userId, String sessionId) {
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);

        List<Conversation> records = conversationRepository
                .findByKbIdAndUserIdAndSessionIdOrderByCreatedAtAsc(kbId, userId, sessionId);

        if (records.isEmpty()) {
            throw new ResourceNotFoundException("Conversation session not found");
        }

        List<ConversationMessageResponse> messages = records.stream()
                .map(record -> ConversationMessageResponse.builder()
                        .id(record.getId())
                        .query(record.getQuery())
                        .response(record.getResponse())
                        .createdAt(record.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ConversationSessionDetailResponse.builder()
                .kbId(kbId)
                .sessionId(sessionId)
                .messageCount(records.size())
                .lastActiveAt(records.get(records.size() - 1).getCreatedAt())
                .messages(messages)
                .build();
    }

    @Override
    @Transactional
    public void deleteSession(Long kbId, Long userId, String sessionId) {
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);

        long deleted = conversationRepository.deleteByKbIdAndUserIdAndSessionId(kbId, userId, sessionId);
        if (deleted <= 0) {
            throw new ResourceNotFoundException("Conversation session not found");
        }

        log.info("删除会话成功: kbId={}, userId={}, sessionId={}, deleted={}", kbId, userId, sessionId, deleted);
    }

    @Override
    @Transactional
    public Conversation saveConversation(Conversation conversation) {
        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public String buildHistoryText(Long kbId, Long userId, String sessionId, Integer historyWindow) {
        if (sessionId == null || sessionId.isBlank()) {
            return "";
        }

        List<Conversation> records = conversationRepository
                .findByKbIdAndUserIdAndSessionIdOrderByCreatedAtAsc(kbId, userId, sessionId);

        if (records.isEmpty()) {
            return "";
        }

        int window = historyWindow == null || historyWindow <= 0 ? 8 : historyWindow;
        int start = Math.max(records.size() - window, 0);
        List<Conversation> selected = records.subList(start, records.size());

        StringBuilder historyBuilder = new StringBuilder();
        for (Conversation item : selected) {
            if (item.getQuery() != null && !item.getQuery().isBlank()) {
                historyBuilder.append("用户: ").append(item.getQuery().trim()).append("\n");
            }
            if (item.getResponse() != null && !item.getResponse().isBlank()) {
                historyBuilder.append("助手: ").append(item.getResponse().trim()).append("\n");
            }
        }

        return historyBuilder.toString().trim();
    }

    @Override
    public String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return sessionId;
    }

    private String extractTitle(String query) {
        if (query == null || query.isBlank()) {
            return "新对话";
        }

        String normalized = query.replace("\n", " ").trim();
        int maxLen = 24;
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }
}
