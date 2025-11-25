package com.mtmn.smartdoc.enums;

/**
 * 文档切分器类型枚举
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
public enum SplitterType {

    /**
     * 按段落切分
     * 使用 DocumentByParagraphSplitter
     */
    BY_PARAGRAPH("paragraph", "按段落切分"),

    /**
     * 按行切分
     * 使用 DocumentByLineSplitter
     */
    BY_LINE("line", "按行切分"),

    /**
     * 按句子切分
     * 使用 DocumentBySentenceSplitter
     */
    BY_SENTENCE("sentence", "按句子切分"),

    /**
     * 自定义切分（使用分隔符）
     * 使用简单的字符串分割
     */
    BY_SEPARATOR("separator", "自定义分隔符切分");

    private final String code;
    private final String description;

    SplitterType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     */
    public static SplitterType fromCode(String code) {
        for (SplitterType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown splitter type: " + code);
    }
}
