package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.dto.HisemRagChatRequest;
import com.mtmn.smartdoc.dto.NaiveRagChatRequest;
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

    SseEmitter naiveRagChat(NaiveRagChatRequest request);

    SseEmitter hisemRagFastChat(HisemRagChatRequest request);

    SseEmitter hisemRagChat(HisemRagChatRequest request);
}