package com.mtmn.smartrag.pipeline;

import com.mtmn.smartrag.vo.RetrievalResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索管道 - 使用责任链模式组装多个处理阶段
 *
 * <p>设计模式：
 * <ul>
 *   <li>Builder Pattern：fluent API 构建管道</li>
 *   <li>Chain of Responsibility：顺序执行多个阶段</li>
 *   <li>Strategy Pattern：每个阶段可插拔替换</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Slf4j
public class RetrievalPipeline {

    private final List<PipelineStage> stages;

    /**
     * 私有构造器（通过 Builder 创建）
     */
    private RetrievalPipeline(List<PipelineStage> stages) {
        this.stages = stages;
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 执行管道
     *
     * @param query 用户查询
     * @return 最终的检索结果
     */
    public List<RetrievalResult> execute(String query) {
        PipelineContext context = new PipelineContext(query);

        log.info("Pipeline execution started: query={}, stages={}", query, stages.size());

        // 顺序执行每个阶段
        for (PipelineStage stage : stages) {
            long startTime = System.currentTimeMillis();

            List<RetrievalResult> stageResults = stage.process(context);
            context.setResults(stageResults);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Stage completed: stage={}, resultCount={}, duration={}ms",
                    stage.getStageName(), stageResults.size(), duration);
        }

        log.info("Pipeline execution finished: finalResultCount={}", context.getResults().size());
        return context.getResults();
    }

    /**
     * Builder 类 - 构建检索管道
     */
    public static class Builder {
        private final List<PipelineStage> stages = new ArrayList<>();

        /**
         * 添加一个处理阶段
         *
         * @param stage 管道阶段
         * @return Builder 实例（支持链式调用）
         */
        public Builder addStage(PipelineStage stage) {
            this.stages.add(stage);
            return this;
        }

        /**
         * 构建管道
         *
         * @return 检索管道实例
         */
        public RetrievalPipeline build() {
            if (stages.isEmpty()) {
                throw new IllegalStateException("Pipeline must have at least one stage");
            }
            return new RetrievalPipeline(new ArrayList<>(stages));
        }
    }
}
