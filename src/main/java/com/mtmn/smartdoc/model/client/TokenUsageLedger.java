package com.mtmn.smartdoc.model.client;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单次请求的 Token 用量账本
 *
 * <p>线程安全（{@link CopyOnWriteArrayList}），支持 SADP {@code executeDag}
 * 中 CompletableFuture 并发写入。</p>
 *
 * @author charmingdaidai
 */
public class TokenUsageLedger {

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    /**
     * 记录一次 LLM 调用的 token 用量和耗时
     *
     * @param label        用途标签，如 "意图路由"、"任务规划"、"生成[T1]"、"综合生成"
     * @param inputTokens  提示 token 数
     * @param outputTokens 完成 token 数
     * @param totalTokens  总 token 数
     * @param durationMs   本次调用耗时（毫秒）
     */
    public void record(String label, int inputTokens, int outputTokens, int totalTokens, long durationMs) {
        entries.add(new Entry(label, inputTokens, outputTokens, totalTokens, durationMs));
    }

    /**
     * 返回所有记录的快照（不可修改）
     */
    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    /**
     * 计算所有条目的汇总
     */
    public Entry getTotal() {
        int input    = entries.stream().mapToInt(Entry::getInputTokens).sum();
        int output   = entries.stream().mapToInt(Entry::getOutputTokens).sum();
        long dur     = entries.stream().mapToLong(Entry::getDurationMs).sum();
        return new Entry("合计", input, output, input + output, dur);
    }

    /**
     * 判断账本是否为空（无任何 LLM 调用记录）
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Data
    public static class Entry {
        private final String label;
        private final int inputTokens;
        private final int outputTokens;
        private final int totalTokens;
        private final long durationMs;
    }
}
