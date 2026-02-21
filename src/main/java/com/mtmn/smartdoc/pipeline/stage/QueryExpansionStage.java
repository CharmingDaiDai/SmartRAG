package com.mtmn.smartdoc.pipeline.stage;

import com.mtmn.smartdoc.pipeline.PipelineContext;
import com.mtmn.smartdoc.pipeline.PipelineStage;
import com.mtmn.smartdoc.vo.RetrievalResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询扩展阶段 - 生成多个查询变体提升召回率
 *
 * <p>扩展策略：
 * <ul>
 *   <li>同义词替换</li>
 *   <li>查询改写（基于 LLM）</li>
 *   <li>多语言翻译</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Slf4j
public class QueryExpansionStage implements PipelineStage {

    @Override
    public List<RetrievalResult> process(PipelineContext context) {
        String originalQuery = context.getOriginalQuery();

        log.debug("Expanding query: {}", originalQuery);

        // TODO: 实现查询扩展逻辑（调用 LLM 或使用规则引擎）
        // 示例：生成 2 个查询变体
        List<String> expandedQueries = new ArrayList<>();
        expandedQueries.add(originalQuery); // 原始查询
        expandedQueries.add(reformulateQuery(originalQuery)); // 改写查询

        context.setExpandedQueries(expandedQueries);

        log.info("Query expansion completed: originalQuery={}, expandedCount={}",
                originalQuery, expandedQueries.size());

        // 查询扩展阶段不产生结果，返回空列表
        return context.getResults();
    }

    @Override
    public String getStageName() {
        return "QueryExpansion";
    }

    /**
     * 查询改写（简单示例）
     */
    private String reformulateQuery(String query) {
        // TODO: 使用 LLM 进行查询改写
        // 示例：添加疑问词
        if (!query.contains("如何") && !query.contains("什么")) {
            return "如何" + query;
        }
        return query;
    }
}
