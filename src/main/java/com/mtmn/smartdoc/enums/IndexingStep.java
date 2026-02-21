package com.mtmn.smartdoc.enums;

/**
 * 索引构建步骤
 *
 * @author charmingdaidai
 * @version 2.0
 */
public enum IndexingStep {
    /**
     * 解析文档
     */
    PARSING("解析中"),

    /**
     * 切分文档
     */
    CHUNKING("切分中"),

    /**
     * 向量化
     */
    EMBEDDING("向量化中"),

    /**
     * 存储到向量库
     */
    STORING("存储中");

    private final String displayName;

    IndexingStep(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
