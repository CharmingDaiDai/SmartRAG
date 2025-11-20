package com.mtmn.smartdoc.factory;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.strategy.RetrievalStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RetrievalStrategy 策略工厂
 * 负责管理和获取不同类型的检索策略
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
public class RetrievalStrategyFactory {

    private final Map<IndexStrategyType, RetrievalStrategy> strategyMap;

    /**
     * 通过 Spring 自动注入所有 RetrievalStrategy 实现
     */
    public RetrievalStrategyFactory(List<RetrievalStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        RetrievalStrategy::getStrategyType,
                        Function.identity()
                ));

        log.info("RetrievalStrategyFactory initialized with {} strategies: {}",
                strategyMap.size(), strategyMap.keySet());
    }

    /**
     * 根据索引策略类型获取检索策略
     *
     * @param type 索引策略类型
     * @return 对应的检索策略实现
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public RetrievalStrategy getStrategy(IndexStrategyType type) {
        RetrievalStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported retrieval strategy type: " + type);
        }
        return strategy;
    }

    /**
     * 检查是否支持某种检索策略
     *
     * @param type 索引策略类型
     * @return true 如果支持
     */
    public boolean isStrategyAvailable(IndexStrategyType type) {
        return strategyMap.containsKey(type);
    }

    /**
     * 获取所有可用的策略类型
     *
     * @return 策略类型列表
     */
    public List<IndexStrategyType> getAvailableStrategies() {
        return List.copyOf(strategyMap.keySet());
    }
}