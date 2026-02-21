package com.mtmn.smartdoc.pipeline.stage;

import com.mtmn.smartdoc.pipeline.PipelineContext;
import com.mtmn.smartdoc.pipeline.PipelineStage;
import com.mtmn.smartdoc.vo.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 重排序阶段 - 使用重排序模型优化结果顺序
 *
 * <p>重排序模型通常使用 Cross-Encoder 架构：
 * <ul>
 *   <li>输入：(query, document) 对</li>
 *   <li>输出：相关性分数</li>
 *   <li>优点：比双塔模型（Bi-Encoder）精度更高</li>
 *   <li>缺点：计算开销较大，只能用于重排序，不能用于召回</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Slf4j
@RequiredArgsConstructor
public class RerankingStage implements PipelineStage {

    private final String rerankModelId;

    @Override
    public List<RetrievalResult> process(PipelineContext context) {
        List<RetrievalResult> results = context.getResults();
        String query = context.getOriginalQuery();

        if (results.isEmpty()) {
            log.warn("No results to rerank");
            return results;
        }

        log.debug("Reranking results: count={}, model={}", results.size(), rerankModelId);

        // TODO: 调用重排序模型（例如 bge-reranker-large）
        // 示例：简单按分数降序排序
        results.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

        log.info("Reranking completed: model={}", rerankModelId);

        return results;
    }

    @Override
    public String getStageName() {
        return "Reranking-" + rerankModelId;
    }
}
