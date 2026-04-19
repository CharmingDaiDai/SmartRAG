package com.mtmn.smartrag.factory;

import com.mtmn.smartrag.enums.EnhancementType;
import com.mtmn.smartrag.rag.Enhancement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enhancement 策略工厂
 * 负责管理和获取不同类型的增强策略
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
public class EnhancementFactory {

    private final Map<EnhancementType, Enhancement> enhancementMap;

    /**
     * 通过 Spring 自动注入所有 Enhancement 实现
     */
    public EnhancementFactory(List<Enhancement> enhancements) {
        this.enhancementMap = enhancements.stream()
                .collect(Collectors.toMap(
                        Enhancement::getType,
                        Function.identity()
                ));

        log.info("EnhancementFactory initialized with {} enhancements: {}",
                enhancementMap.size(), enhancementMap.keySet());
    }

    /**
     * 根据类型获取增强策略
     *
     * @param type 增强类型
     * @return 对应的增强策略实现
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public Enhancement getEnhancement(EnhancementType type) {
        Enhancement enhancement = enhancementMap.get(type);
        if (enhancement == null) {
            throw new IllegalArgumentException("Unsupported enhancement type: " + type);
        }
        return enhancement;
    }

    /**
     * 检查是否支持某种增强类型
     *
     * @param type 增强类型
     * @return true 如果支持
     */
    public boolean isEnhancementAvailable(EnhancementType type) {
        return enhancementMap.containsKey(type);
    }

    /**
     * 获取所有可用的增强类型
     *
     * @return 增强类型列表
     */
    public List<EnhancementType> getAvailableEnhancements() {
        return List.copyOf(enhancementMap.keySet());
    }
}