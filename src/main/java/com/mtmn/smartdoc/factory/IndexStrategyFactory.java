package com.mtmn.smartdoc.factory;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.strategy.IndexStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 索引策略工厂
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
// 【修复】删除了 @RequiredArgsConstructor，因为下面已经手动定义了构造函数
public class IndexStrategyFactory {

    private final Map<IndexStrategyType, IndexStrategy> strategies;

    /**
     * 构造函数注入所有策略实现
     * Spring 会自动将所有实现了 IndexStrategy 接口的 Bean 注入到这个 list 中
     */
    public IndexStrategyFactory(List<IndexStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        IndexStrategy::getStrategyType,
                        Function.identity()
                ));

        log.info("Initialized IndexStrategyFactory with strategies: {}",
                strategies.keySet());
    }

    /**
     * 获取索引策略
     *
     * @param strategyType 策略类型
     * @return 索引策略实例
     */
    public IndexStrategy getStrategy(IndexStrategyType strategyType) {
        IndexStrategy strategy = strategies.get(strategyType);

        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No IndexStrategy found for type: " + strategyType);
        }

        return strategy;
    }

    /**
     * 检查策略是否可用
     *
     * @param strategyType 策略类型
     * @return 是否可用
     */
    public boolean isStrategyAvailable(IndexStrategyType strategyType) {
        return strategies.containsKey(strategyType);
    }

    /**
     * 获取所有可用的策略类型
     *
     * @return 策略类型列表
     */
    public List<IndexStrategyType> getAvailableStrategies() {
        return List.copyOf(strategies.keySet());
    }
}