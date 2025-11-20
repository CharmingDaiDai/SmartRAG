package com.mtmn.smartdoc.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * 自适应阈值计算工具
 * 基于统计指标和安全机制计算最优阈值
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/5/27 09:42
 */
@Slf4j
public final class ThresholdCalculator {

    // 私有构造函数，防止实例化
    private ThresholdCalculator() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 计算自适应阈值
     *
     * @param similarities 相似度列表
     * @param level        层级
     * @param kMax         最大保留节点数
     * @param beta         权重参数 beta
     * @param gamma        权重参数 gamma
     * @param kMin         最小保留节点数
     * @return 计算得到的阈值
     */
    public static double calculateAdaptiveThreshold(List<Double> similarities, int level, int kMax,
                                                    double beta, double gamma, int kMin) {
        if (similarities == null || similarities.isEmpty()) {
            return 0.0;
        }

        log.debug("层级 {} 的相似度数组: {}", level, similarities);

        // 一次性计算统计量
        Statistics stats = calculateStatistics(similarities);

        // 计算原始阈值
        double rawThreshold = calculateRawThreshold(stats, beta, gamma);

        // 应用安全机制
        List<Double> sortedSimilarities = getSortedSimilarities(similarities);
        double finalThreshold = applySafetyConstraints(rawThreshold, sortedSimilarities, kMax, kMin);

        // 输出调试信息
        logCalculationDetails(level, stats, rawThreshold, finalThreshold, sortedSimilarities, similarities.size());

        return finalThreshold;
    }

    /**
     * 计算统计量
     */
    private static Statistics calculateStatistics(List<Double> similarities) {
        DoubleSummaryStatistics summary = similarities.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        double mean = summary.getAverage();
        double variance = similarities.stream()
                .mapToDouble(s -> Math.pow(s - mean, 2))
                .average()
                .orElse(0.0);

        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = mean != 0 ? standardDeviation / mean : 0.0;
        double normalizedRange = summary.getMax() != 0 ?
                (summary.getMax() - summary.getMin()) / summary.getMax() : 0.0;

        return new Statistics(
                summary.getMax(),
                summary.getMin(),
                mean,
                standardDeviation,
                coefficientOfVariation,
                normalizedRange
        );
    }

    /**
     * 计算原始阈值
     */
    private static double calculateRawThreshold(Statistics stats, double beta, double gamma) {
        return beta * stats.max() - (1 - gamma * stats.coefficientOfVariation()) * (stats.max() - stats.mean());
    }

    /**
     * 获取降序排序的相似度列表
     */
    private static List<Double> getSortedSimilarities(List<Double> similarities) {
        return similarities.stream()
                .sorted((a, b) -> Double.compare(b, a))  // 降序排序
                .toList();
    }

    /**
     * 应用安全约束机制
     */
    private static double applySafetyConstraints(double initialThreshold, List<Double> sortedSimilarities, int kMax, int kMin) {
        // 计算当前阈值下的节点数量
        long countAboveThreshold = sortedSimilarities.stream()
                .mapToLong(s -> s >= initialThreshold ? 1 : 0)
                .sum();

        // 确保至少保留 kMin 个节点
        if (countAboveThreshold < kMin && !sortedSimilarities.isEmpty()) {
            int index = Math.min(kMin - 1, sortedSimilarities.size() - 1);
            return sortedSimilarities.get(index);
        }

        // 确保至多保留 kMax 个节点
        countAboveThreshold = sortedSimilarities.stream()
                .mapToLong(s -> s >= initialThreshold ? 1 : 0)
                .sum();

        if (countAboveThreshold > kMax && kMax > 0) {
            return sortedSimilarities.get(kMax - 1);
        }

        return initialThreshold;
    }

    /**
     * 输出计算详情日志
     */
    private static void logCalculationDetails(int level, Statistics stats, double rawThreshold,
                                              double finalThreshold, List<Double> sortedSimilarities, int totalCount) {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("层级 {} 的统计指标:", level);
        log.debug("  最大值: {}, 最小值: {}, 平均值: {}",
                String.format("%.4f", stats.max()),
                String.format("%.4f", stats.min()),
                String.format("%.4f", stats.mean()));
        log.debug("  标准差: {}, 变异系数: {}, 归一化极差: {}",
                String.format("%.4f", stats.standardDeviation()),
                String.format("%.4f", stats.coefficientOfVariation()),
                String.format("%.4f", stats.normalizedRange()));
        log.debug("  原始计算的阈值: {}", String.format("%.4f", rawThreshold));
        log.debug("  应用安全机制后的最终阈值: {}", String.format("%.4f", finalThreshold));

        // 计算通过阈值的节点数量
        long passedCount = sortedSimilarities.stream()
                .mapToLong(s -> s >= finalThreshold ? 1 : 0)
                .sum();
        log.debug("  通过阈值的节点数量: {}/{}", passedCount, totalCount);
    }

    /**
     * 统计量记录类
     */
    private record Statistics(
            double max,
            double min,
            double mean,
            double standardDeviation,
            double coefficientOfVariation,
            double normalizedRange
    ) {
    }
}