package com.mtmn.smartrag.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 大模型 JSON 输出解析工具类
 * <p>
 * 特点：
 * 1. 自动去除 Markdown 代码块包裹
 * 2. 自动截取首个 { 或 [ 开始的内容
 * 3. 极其宽容的解析策略（允许注释、单引号、非标转义等）
 * 4. 完善的异常兜底
 * </p>
 *
 * @author charmingdaidai
 */
@Slf4j
public class LlmJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    static {
        // --- 核心配置：让 Jackson 变得极其宽容 ---

        // 允许标准 JSON 不支持的特性
        // 允许注释
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        // 允许注释 #
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
        // 允许单引号
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 允许键名不带引号
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 允许换行符等控制字符
        OBJECT_MAPPER.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

        // 反序列化配置
        // 忽略未知字段
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许单个值转数组
        OBJECT_MAPPER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    /**
     * 解析为 Map (对应 Python 字典)
     * 兜底：返回空 Map
     */
    public static Map<String, Object> parseMap(String llmOutput) {
        try {
            String json = extractJson(llmOutput, true);
            if (!StringUtils.hasText(json)) {
                return Collections.emptyMap();
            }

            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("LLM输出解析Map失败，源数据: [{}], 错误: {}", limitLog(llmOutput), e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 解析为 List (对应 Python 数组)
     * 兜底：返回空 List
     */
    public static <T> List<T> parseList(String llmOutput, Class<T> clazz) {
        try {
            String json = extractJson(llmOutput, false);
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }

            JavaType type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(json, type);
        } catch (Exception e) {
            log.warn("LLM输出解析List失败，源数据: [{}], 错误: {}", limitLog(llmOutput), e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析为指定对象 (Java Bean)
     * 兜底：返回 null
     */
    public static <T> T parseObject(String llmOutput, Class<T> clazz) {
        try {
            // 假设对象以 { 开头
            String json = extractJson(llmOutput, true);
            if (!StringUtils.hasText(json)) {
                return null;
            }

            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("LLM输出解析Object失败，源数据: [{}], 错误: {}", limitLog(llmOutput), e.getMessage());
            return null;
        }
    }

    /**
     * 尝试提取纯文本字符串 (去除引号等)
     * 如果本身不是 JSON 字符串，则返回原清洗后的文本
     */
    public static String parseString(String llmOutput) {
        if (!StringUtils.hasText(llmOutput)) {
            return "";
        }
        try {
            // 先尝试当做 JSON String 解析 ("abc" -> abc)
            JsonNode node = OBJECT_MAPPER.readTree(llmOutput);
            if (node.isTextual()) {
                return node.asText();
            }
        } catch (Exception ignored) {
            // 不是标准 JSON String，直接返回清理后的文本
        }
        // 如果失败，移除 Markdown 后返回
        return cleanMarkdown(llmOutput).trim();
    }

    // ================= 私有核心逻辑 =================

    /**
     * 核心：从混乱的文本中提取 JSON 核心部分
     *
     * @param content      大模型原始内容
     * @param expectObject true=期望是对象{}, false=期望是数组[]
     */
    private static String extractJson(String content, boolean expectObject) {
        if (content == null) {
            return null;
        }

        // 1. 尝试移除 Markdown 代码块 (```json ... ```)
        String clean = cleanMarkdown(content);

        // 2. 寻找边界
        int startWrapper = expectObject ? content.indexOf("{") : content.indexOf("[");
        // 如果找不到期望的开始，尝试找另一个（容错）
        if (startWrapper == -1) {
            startWrapper = expectObject ? content.indexOf("[") : content.indexOf("{");
        }

        if (startWrapper == -1) {
            // 实在找不到括号，可能直接返回了 null 字符串或者空
            return null;
        }

        // 寻找结束边界
        int endWrapper = content.lastIndexOf(expectObject ? "}" : "]");
        if (endWrapper == -1) {
            endWrapper = expectObject ? content.lastIndexOf("]") : content.lastIndexOf("}");
        }

        // 3. 截取有效部分
        if (endWrapper != -1 && endWrapper > startWrapper) {
            return content.substring(startWrapper, endWrapper + 1);
        }

        // 如果没找到标准闭环，尝试返回清洗后的全文碰运气
        return clean;
    }

    /**
     * 移除 Markdown 标记
     */
    private static String cleanMarkdown(String content) {
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return content;
    }

    /**
     * 日志截断，防止日志过大
     */
    private static String limitLog(String log) {
        if (log == null) {
            return "";
        }
        return log.length() > 200 ? log.substring(0, 200) + "..." : log;
    }
}