package com.mtmn.smartrag.rag.config;

import com.mtmn.smartrag.enums.EnhancementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.mtmn.smartrag.enums.IndexStrategyType;

/**
 * 检索策略配置抽象基类
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-12-01
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NaiveRagIndexConfig.class, name = "NAIVE_RAG"),
        @JsonSubTypes.Type(value = HisemRagIndexConfig.class, name = "HISEM_RAG")
})
public abstract class RetrievalStrategyConfig {

    /**
     * 获取策略类型
     * 由子类实现返回对应的枚举值
     *
     * @return 策略类型枚举
     */
    public abstract IndexStrategyType getStrategyType();

}