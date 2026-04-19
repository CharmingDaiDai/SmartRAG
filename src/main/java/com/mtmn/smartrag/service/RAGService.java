package com.mtmn.smartrag.service;

import com.mtmn.smartrag.dto.HisemRagChatRequest;
import com.mtmn.smartrag.dto.NaiveRagChatRequest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * RAG 检索服务接口
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
public interface RAGService {

    SseEmitter naiveRagChat(Long userId, NaiveRagChatRequest request);

    SseEmitter hisemRagFastChat(Long userId, HisemRagChatRequest request);

    SseEmitter hisemRagChat(Long userId, HisemRagChatRequest request);
}