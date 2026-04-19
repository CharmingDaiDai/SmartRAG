package com.mtmn.smartrag.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartrag.rag.IndexStrategy;
import com.mtmn.smartrag.rag.config.IndexStrategyConfig;
import com.mtmn.smartrag.dto.CreateKnowledgeBaseRequest;
import com.mtmn.smartrag.dto.KnowledgeBaseResponse;
import com.mtmn.smartrag.enums.DocumentIndexStatus;
import com.mtmn.smartrag.exception.ConfigValidationException;
import com.mtmn.smartrag.exception.ResourceNotFoundException;
import com.mtmn.smartrag.exception.UnauthorizedAccessException;
import com.mtmn.smartrag.model.factory.ModelFactory;
import com.mtmn.smartrag.po.DocumentPo;
import com.mtmn.smartrag.po.KnowledgeBase;
import com.mtmn.smartrag.rag.factory.RAGStrategyFactory;
import com.mtmn.smartrag.repository.DocumentRepository;
import com.mtmn.smartrag.repository.IndexingTaskRepository;
import com.mtmn.smartrag.repository.KnowledgeBaseRepository;
import com.mtmn.smartrag.service.KnowledgeBaseService;
import com.mtmn.smartrag.service.MinioService;
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
    private final RAGStrategyFactory ragStrategyFactory;
    private final MinioService minioService;
    private final IndexingTaskRepository indexingTaskRepository;

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

        // 2. 获取该知识库下的所有文档（用于删除 MinIO 文件）
        List<DocumentPo> documents = documentRepository.findByKbId(kbId);

        // 3. 删除索引（向量和Chunks）
        // 调用策略模式删除整个知识库的索引数据
        IndexStrategy indexStrategy = ragStrategyFactory.getIndexStrategy(kb.getIndexStrategyType());
        indexStrategy.deleteIndex(kb);

        // 4. 删除 MinIO 文件
        for (DocumentPo doc : documents) {
            try {
                minioService.deleteFile(doc.getFilePath());
            } catch (Exception e) {
                log.warn("Failed to delete file from MinIO: {}, error: {}", doc.getFilePath(), e.getMessage());
                // 继续删除其他文件，不中断流程
            }
        }

        // 5. 删除数据库中的文档记录
        if (!documents.isEmpty()) {
            log.info("Deleting {} documents associated with KB: {}", documents.size(), kbId);
            documentRepository.deleteByKbId(kbId);
        }

        // 6. 删除知识库关联的索引任务记录（避免历史孤儿任务）
        indexingTaskRepository.deleteByKbId(kbId);

        // 7. 删除知识库记录
        knowledgeBaseRepository.delete(kb);

        log.info("Knowledge base deleted successfully: id={}, cleaned {} documents", kbId, documents.size());
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