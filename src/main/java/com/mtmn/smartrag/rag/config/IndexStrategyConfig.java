package com.mtmn.smartrag.rag.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mtmn.smartrag.enums.IndexStrategyType;

/**
 * 索引策略配置抽象基类
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NaiveRagIndexConfig.class, name = "NAIVE_RAG"),
        @JsonSubTypes.Type(value = HisemRagIndexConfig.class, name = "HISEM_RAG"),
        @JsonSubTypes.Type(value = HisemRagFastIndexConfig.class, name = "HISEM_RAG_FAST")
})
public abstract class IndexStrategyConfig {

    /**
     * 获取策略类型
     * 由子类实现返回对应的枚举值
     *
     * @return 策略类型枚举
     */
    public abstract IndexStrategyType getStrategyType();

    /**
     * 验证配置是否有效
     * 子类必须实现此方法
     */
    public abstract void validate();
}