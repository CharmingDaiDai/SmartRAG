package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.dto.CreateKnowledgeBaseRequest;
import com.mtmn.smartdoc.dto.KnowledgeBaseResponse;
import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.exception.ConfigValidationException;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.exception.UnauthorizedAccessException;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request, Long userId) {
        log.info("Creating knowledge base for user {}: {}", userId, request.getName());

        // 1. 验证 Embedding 模型是否存在
        if (!modelFactory.isEmbeddingModelAvailable(request.getEmbeddingModelId())) {
            throw new ConfigValidationException("Embedding model not found: " + request.getEmbeddingModelId());
        }

        // 2. 获取配置对象(Jackson已自动反序列化并填充默认值)
        IndexStrategyConfig config = request.getIndexStrategyConfig();
        if (config == null) {
            throw new ConfigValidationException("Index strategy configuration is required");
        }

        // 3. 验证配置
        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            throw new ConfigValidationException("Invalid strategy configuration: " + e.getMessage(), e);
        }

        // 4. 序列化配置为JSON字符串存储到数据库
        String configJson;
        try {
            configJson = objectMapper.writeValueAsString(config);
            log.debug("Serialized config: {}", configJson);
        } catch (Exception e) {
            log.error("Failed to serialize strategy config", e);
            throw new ConfigValidationException("Failed to serialize strategy configuration", e);
        }

        // 5. 创建知识库实体
        KnowledgeBase kb = KnowledgeBase.builder()
                .name(request.getName())
                .description(request.getDescription())
                .indexStrategyType(config.getStrategyType())
                .indexStrategyConfig(configJson)
                .embeddingModelId(request.getEmbeddingModelId())
                .userId(userId)
                .build();

        // 6. 保存到数据库
        kb = knowledgeBaseRepository.save(kb);

        log.info("Knowledge base created successfully: id={}", kb.getId());
        return convertToResponse(kb);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> listKnowledgeBases(Long userId) {
        log.debug("Listing knowledge bases for user {}", userId);

        // 1. 查询知识库列表
        List<KnowledgeBase> kbs = knowledgeBaseRepository.findByUserId(userId);

        if (kbs.isEmpty()) {
            return List.of();
        }

        // 2. 【性能优化】提取所有知识库 ID
        List<Long> kbIds = kbs.stream()
                .map(KnowledgeBase::getId)
                .collect(Collectors.toList());

        // 3. 【批量查询】一次性获取所有知识库的文档统计
        // 从 2N 次查询优化为 2 次查询
        Map<Long, Long> totalCountMap = convertToCountMap(
                documentRepository.countByKbIdIn(kbIds));
        Map<Long, Long> indexedCountMap = convertToCountMap(
                documentRepository.countByKbIdInAndIndexStatus(kbIds, DocumentIndexStatus.INDEXED));

        // 4. 批量构建响应对象
        return kbs.stream()
                .map(kb -> convertToResponse(kb, totalCountMap, indexedCountMap))
                .collect(Collectors.toList());
    }

    /**
     * 辅助方法：将查询结果转换为 Map<kbId, count>
     */
    private Map<Long, Long> convertToCountMap(List<Map<String, Object>> queryResults) {
        return queryResults.stream()
                .collect(Collectors.toMap(
                        map -> ((Number) map.get("kbId")).longValue(),
                        map -> ((Number) map.get("count")).longValue()));
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseResponse getKnowledgeBase(Long kbId, Long userId) {
        log.debug("Getting knowledge base: id={}, userId={}", kbId, userId);

        KnowledgeBase kb = getKnowledgeBaseEntity(kbId, userId);
        return convertToResponse(kb);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getKnowledgeBaseEntity(Long kbId, Long userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));

        // 验证用户权限
        if (!kb.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(userId, "KnowledgeBase", kbId);
        }

        return kb;
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long kbId, Long userId) {
        log.info("Deleting knowledge base: id={}, userId={}", kbId, userId);

        // 1. 验证权限
        KnowledgeBase kb = getKnowledgeBaseEntity(kbId, userId);

        // 2. 【级联删除】删除关联数据
        // 注意：由于在 SQL 中定义了外键级联删除（ON DELETE CASCADE），
        // 理论上删除知识库会自动删除关联的 documents, chunks, tree_nodes, conversations
        // 但这里显式处理是为了：
        // a) 控制删除顺序，避免外键约束冲突
        // b) 记录日志便于追踪
        // c) 触发额外的清理逻辑（如删除 MinIO 文件、向量库索引）

        long documentCount = documentRepository.countByKbId(kbId);
        if (documentCount > 0) {
            log.info("Deleting {} documents associated with KB: {}", documentCount, kbId);
            // 使用批量删除，比循环调用 delete() 效率高
            documentRepository.deleteByKbId(kbId);
        }

        // TODO: 删除 Chunks（如果使用 NaiveRAG）
        // chunkRepository.deleteByKbId(kbId);

        // TODO: 删除 TreeNodes（如果使用 HisemRAG）
        // treeNodeRepository.deleteByKbId(kbId);

        // TODO: 删除 Conversations
        // conversationRepository.deleteByKbId(kbId);

        // TODO: 异步删除向量存储中的索引
        // vectorStoreService.deleteCollection(kbId);

        // TODO: 异步删除 MinIO 中的文件
        // minioService.deleteFolder("kb-" + kbId);

        // 3. 删除知识库
        knowledgeBaseRepository.delete(kb);

        log.info("Knowledge base deleted successfully: id={}, cleaned {} documents", kbId, documentCount);
    }

    /**
     * 转换为响应 DTO（单个知识库查询使用）
     */
    private KnowledgeBaseResponse convertToResponse(KnowledgeBase kb) {
        // 统计文档数量（单个查询场景）
        long totalDocs = documentRepository.countByKbId(kb.getId());
        long indexedDocs = documentRepository.countByKbIdAndIndexStatus(
                kb.getId(), DocumentIndexStatus.INDEXED);

        return buildResponse(kb, totalDocs, indexedDocs);
    }

    /**
     * 转换为响应 DTO（批量查询使用，避免 N+1）
     *
     * @param kb              知识库实体
     * @param totalCountMap   文档总数 Map
     * @param indexedCountMap 已索引文档数 Map
     */
    private KnowledgeBaseResponse convertToResponse(
            KnowledgeBase kb,
            Map<Long, Long> totalCountMap,
            Map<Long, Long> indexedCountMap) {

        long totalDocs = totalCountMap.getOrDefault(kb.getId(), 0L);
        long indexedDocs = indexedCountMap.getOrDefault(kb.getId(), 0L);

        return buildResponse(kb, totalDocs, indexedDocs);
    }

    /**
     * 构建响应对象（公共逻辑）
     */
    private KnowledgeBaseResponse buildResponse(KnowledgeBase kb, long totalDocs, long indexedDocs) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .indexStrategyType(kb.getIndexStrategyType())
                .indexStrategyConfig(kb.getIndexStrategyConfig())
                .embeddingModelId(kb.getEmbeddingModelId())
                .userId(kb.getUserId())
                .documentCount(totalDocs)
                .indexedDocumentCount(indexedDocs)
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}