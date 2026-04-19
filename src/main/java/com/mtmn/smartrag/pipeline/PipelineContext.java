package com.mtmn.smartrag.pipeline;

import com.mtmn.smartrag.vo.RetrievalResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管道上下文 - 在各个阶段间传递数据
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Data
public class PipelineContext {

    /**
     * 原始查询
     */
    private String originalQuery;

    /**
     * 扩展后的查询列表（查询扩展阶段填充）
     */
    private List<String> expandedQueries = new ArrayList<>();

    /**
     * 当前检索结果（每个阶段可以修改）
     */
    private List<RetrievalResult> results = new ArrayList<>();

    /**
     * 附加属性（用于阶段间传递自定义数据）
     */
    private Object metadata;

    /**
     * 构造器
     */
    public PipelineContext(String originalQuery) {
        this.originalQuery = originalQuery;
        this.expandedQueries.add(originalQuery); // 默认包含原始查询
    }
}
