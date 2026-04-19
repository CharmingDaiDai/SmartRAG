package com.mtmn.smartrag.pipeline.stage;

import com.mtmn.smartrag.rag.config.RetrievalStrategyConfig;
import com.mtmn.smartrag.pipeline.PipelineContext;
import com.mtmn.smartrag.pipeline.PipelineStage;
import com.mtmn.smartrag.rag.RetrievalStrategy;
import com.mtmn.smartrag.vo.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 检索阶段 - 调用检索策略获取候选结果
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Slf4j
@RequiredArgsConstructor
public class RetrievalStage implements PipelineStage {

    private final RetrievalStrategy retrievalStrategy;
    private final Long kbId;
    private final RetrievalStrategyConfig retrievalConfig;

    @Override
    public List<RetrievalResult> process(PipelineContext context) {
        List<String> queries = context.getExpandedQueries();

        log.debug("Retrieving with queries: count={}", queries.size());

        // 调用检索策略
        List<RetrievalResult> results = retrievalStrategy.search(queries, kbId, retrievalConfig);

        log.info("Retrieval completed: resultCount={}", results.size());

        return results;
    }

    @Override
    public String getStageName() {
        return "Retrieval-" + retrievalStrategy.getType();
    }
}