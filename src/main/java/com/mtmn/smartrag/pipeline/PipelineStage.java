package com.mtmn.smartrag.pipeline;

import com.mtmn.smartrag.vo.RetrievalResult;

import java.util.List;

/**
 * 管道阶段接口 - 定义检索管道中的单个处理阶段
 *
 * <p>每个阶段负责一个特定的处理任务：
 * <ul>
 *   <li>查询扩展：生成多个查询变体</li>
 *   <li>检索：调用检索策略获取候选结果</li>
 *   <li>重排序：使用重排序模型优化结果顺序</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
public interface PipelineStage {

    /**
     * 执行当前阶段的处理逻辑
     *
     * @param context 管道上下文（包含查询、中间结果等）
     * @return 处理后的检索结果
     */
    List<RetrievalResult> process(PipelineContext context);

    /**
     * 获取阶段名称（用于日志和调试）
     *
     * @return 阶段名称
     */
    String getStageName();
}
