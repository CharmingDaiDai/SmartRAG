package com.mtmn.smartrag.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description TODO
 * @date 2025/6/1 13:24
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryRewriteResult {

    /**
     * 重写后的查询
     */
    private String rewrittenQuery;

    /**
     * 重写原因
     */
    private String reason;

    /**
     * 原始查询（内部使用）
     */
    private String originalQuery;

    /**
     * 是否重写成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 获取最终使用的查询
     */
    public String getFinalQuery() {
        return success && rewrittenQuery != null ? rewrittenQuery : originalQuery;
    }

    /**
     * 是否进行了重写
     */
    public boolean isRewritten() {
        return success && !Objects.equals(originalQuery, rewrittenQuery);
    }
}