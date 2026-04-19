package com.mtmn.smartrag.rag.factory;

import com.mtmn.smartrag.enums.IndexStrategyType;
import com.mtmn.smartrag.rag.IndexStrategy;
import com.mtmn.smartrag.rag.RetrievalStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RAG 策略工厂
 * <p>
 * 根据知识库的索引策略类型，获取对应的索引策略和检索策略
 * 使用 Factory Pattern + Map 实现 O(1) 查找，避免 if-else
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RAGStrategyFactory {

    private final List<IndexStrategy> indexStrategies;
    private final List<RetrievalStrategy> retrievalStrategies;

    private Map<IndexStrategyType, IndexStrategy> indexStrategyMap;
    private Map<IndexStrategyType, RetrievalStrategy> retrievalStrategyMap;

    @PostConstruct
    public void init() {
        // 将 List 转为 Map，方便 O(1) 查找
        indexStrategyMap = indexStrategies.stream()
                .collect(Collectors.toMap(
                        IndexStrategy::getType,
                        Function.identity()
                ));

        retrievalStrategyMap = retrievalStrategies.stream()
                .collect(Collectors.toMap(
                        RetrievalStrategy::getType,
                        Function.identity()
                ));

        log.info("RAGStrategyFactory initialized: {} index strategies, {} retrieval strategies",
                indexStrategyMap.keySet(), retrievalStrategyMap.keySet());
    }

    /**
     * 获取索引策略
     *
     * @param type 策略类型
     * @return 索引策略实例
     */
    public IndexStrategy getIndexStrategy(IndexStrategyType type) {
        IndexStrategy strategy = indexStrategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No IndexStrategy found for type: " + type +
                            ". Available: " + indexStrategyMap.keySet());
        }
        return strategy;
    }

    /**
     * 获取检索策略
     *
     * @param type 策略类型
     * @return 检索策略实例
     */
    public RetrievalStrategy getRetrievalStrategy(IndexStrategyType type) {
        RetrievalStrategy strategy = retrievalStrategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No RetrievalStrategy found for type: " + type +
                            ". Available: " + retrievalStrategyMap.keySet());
        }
        return strategy;
    }

    /**
     * 检查策略类型是否支持
     */
    public boolean isStrategySupported(IndexStrategyType type) {
        return indexStrategyMap.containsKey(type) && retrievalStrategyMap.containsKey(type);
    }

    /**
     * 获取所有支持的策略类型
     */
    public List<IndexStrategyType> getSupportedStrategyTypes() {
        return List.copyOf(indexStrategyMap.keySet());
    }
}