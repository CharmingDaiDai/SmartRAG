package com.mtmn.smartrag.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 配置属性类
 *
 * <p>从 application.yml 中加载 rag 前缀的配置</p>
 *
 * @author charmingdaidai
 * @version 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Data
public class RagProperties {

    /**
     * 默认使用的 RAG 方法
     */
    private String defaultMethod = "naive";

    /**
     * Naive RAG 配置
     */
    private NaiveConfig naive = new NaiveConfig();

    /**
     * HiSem RAG Fast 配置
     */
    private HisemConfig hisem = new HisemConfig();

    /**
     * HiSem SADP RAG 配置
     */
    private HisemSadpConfig hisemSadp = new HisemSadpConfig();

    // ==================== Naive RAG ====================

    @Data
    public static class NaiveConfig {
        private String name = "RAG";
        private String description = "普通的 RAG 方法";
        private NaiveIndexConfig index = new NaiveIndexConfig();
        private NaiveSearchConfig search = new NaiveSearchConfig();
    }

    @Data
    public static class NaiveIndexConfig {
        private int chunkSize = 512;
        private int chunkOverlap = 100;
    }

    @Data
    public static class NaiveSearchConfig {
        private int topK = 5;
    }

    // ==================== HiSem RAG Fast ====================

    @Data
    public static class HisemConfig {
        private String name = "HiSem-RAG-Fast";
        private String description = "层级语义驱动的 RAG 方法（不构建树）";
        private HisemIndexConfig index = new HisemIndexConfig();
        private HisemSearchConfig search = new HisemSearchConfig();
        private ThresholdConfig threshold = new ThresholdConfig();
    }

    @Data
    public static class HisemIndexConfig {
        private int chunkSize = 2048;
        private boolean titleEnhance = true;
        private boolean anAbstract = false;
    }

    @Data
    public static class HisemSearchConfig {
        /**
         * 最大检索结果数（传递给大模型的上下文片段数量上限）
         */
        private int maxRes = 10;
    }

    // ==================== HiSem SADP RAG ====================

    @Data
    public static class HisemSadpConfig {
        private String name = "HiSem-SADP-RAG";
        private String description = "完整的 HiSem-SADP-RAG 方法";
        private HisemSadpIndexConfig index = new HisemSadpIndexConfig();
        private HisemSadpSearchConfig search = new HisemSadpSearchConfig();
        private ThresholdConfig threshold = new ThresholdConfig();
    }

    @Data
    public static class HisemSadpIndexConfig {
        private int chunkSize = 2048;
        private boolean titleEnhance = true;
    }

    @Data
    public static class HisemSadpSearchConfig {
        /**
         * 最大检索结果数（传递给大模型的上下文片段数量上限）
         */
        private int maxRes = 10;
    }

    // ==================== 共用阈值配置 ====================

    /**
     * 自适应阈值参数配置
     *
     * <p>动态阈值公式：θ = β·sMax - (1 - γ·CV)·(sMax - μ)</p>
     * <p>其中 CV = σ/μ 为变异系数</p>
     */
    @Data
    public static class ThresholdConfig {
        /**
         * β 参数：控制阈值与最高分的距离（0-1）
         */
        private double beta = 0.9;

        /**
         * γ 参数：控制分布离散程度的影响权重（0-1）
         */
        private double gamma = 0.8;

        /**
         * 最小阈值下界
         */
        private double thetaMin = 0.3;

        /**
         * 每层检索最少保留的候选数量
         */
        private int kMin = 1;

        /**
         * 每层检索最多保留的候选数量（同时作为 Milvus search 的 topK）
         */
        private int kMax = 20;
    }
}
