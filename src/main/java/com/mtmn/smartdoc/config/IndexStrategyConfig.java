package com.mtmn.smartdoc.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import lombok.Data;

/**
 * 索引策略配置抽象基类
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NaiveRAGConfig.class, name = "NAIVE_RAG"),
        @JsonSubTypes.Type(value = HisemRAGConfig.class, name = "HISEM_RAG")
})
public abstract class IndexStrategyConfig {

    /**
     * 策略类型
     */
    private IndexStrategyType strategyType;

    /**
     * 验证配置是否有效
     * 子类必须实现此方法
     */
    public abstract void validate();
}