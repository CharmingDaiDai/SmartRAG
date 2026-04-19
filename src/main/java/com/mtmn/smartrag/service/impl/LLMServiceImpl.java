package com.mtmn.smartrag.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartrag.model.client.LLMClient;
import com.mtmn.smartrag.model.factory.ModelFactory;
import com.mtmn.smartrag.service.LLMService;
import com.mtmn.smartrag.vo.SecurityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大语言模型服务实现
 * 负责创建和管理不同的聊天模型，提供文本生成功能
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025/11/20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LLMClient createLLMClient() {
        return modelFactory.createDefaultLLMClient();
    }

    @Override
    public LLMClient createLLMClient(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return createLLMClient();
        }
        return modelFactory.createLLMClient(modelId);
    }

    @Override
    public String generateSummary(String text) {
        return generateSummary(text, null);
    }

    public String generateSummary(String text, String modelId) {
        log.info("生成摘要，使用模型：{}", modelId == null ? "默认" : modelId);

        LLMClient client = modelId == null ? createLLMClient() : createLLMClient(modelId);

        String prompt = String.format(
                "请为以下文本生成一个简洁的摘要，不超过200字：\n\n%s",
                text
        );

        return client.chat(prompt);
    }

    @Override
    public List<String> extractKeywords(String text) {
        return extractKeywords(text, null);
    }

    public List<String> extractKeywords(String text, String modelId) {
        log.info("提取关键词，使用模型：{}", modelId == null ? "默认" : modelId);

        LLMClient client = modelId == null ? createLLMClient() : createLLMClient(modelId);

        String prompt = String.format(
                "请从以下文本中提取5-10个关键词，以JSON格式返回。格式：{\"keywords\": [\"关键词1\", \"关键词2\", ...]}\\n\\n%s",
                text
        );

        String response = client.chat(prompt);
        return parseKeywordsFromJson(response);
    }

    @Override
    public String polishDocument(String text, String polishType) {
        return polishDocument(text, polishType, null);
    }

    public String polishDocument(String text, String polishType, String modelId) {
        log.info("文档润色，类型：{}，使用模型：{}", polishType, modelId == null ? "默认" : modelId);

        LLMClient client = modelId == null ? createLLMClient() : createLLMClient(modelId);

        String prompt = switch (polishType.toLowerCase()) {
            case "grammar" -> String.format("请修正以下文本中的语法错误，保持原意不变：\n\n%s", text);
            case "professional" -> String.format("请将以下文本改写得更加专业和正式：\n\n%s", text);
            case "concise" -> String.format("请将以下文本改写得更加简洁明了：\n\n%s", text);
            default -> String.format("请优化以下文本，使其更加流畅易读：\n\n%s", text);
        };

        return client.chat(prompt);
    }

    @Override
    public SecurityResult detectSensitiveInfo(String text) {
        return detectSensitiveInfo(text, null);
    }

    public SecurityResult detectSensitiveInfo(String text, String modelId) {
        log.info("检测敏感信息，使用模型：{}", modelId == null ? "默认" : modelId);

        LLMClient client = modelId == null ? createLLMClient() : createLLMClient(modelId);

        String prompt = String.format(
                "请检测以下文本中的敏感信息（包括个人信息、机密信息等），以JSON格式返回。" +
                        "格式：{\"sensitiveInfo\": [{\"type\": \"类型\", \"content\": \"内容\", \"position\": \"位置\"}]}\\n\\n%s",
                text
        );

        String response = client.chat(prompt);
        List<SecurityResult.SensitiveInfo> sensitiveInfoList = parseSensitiveInfoFromJson(response);

        // 构建SecurityResult
        return SecurityResult.builder()
                .sensitiveInfoList(sensitiveInfoList)
                .totalCount(sensitiveInfoList.size())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 清理JSON字符串
     */
    private String cleanJsonString(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = input.trim()
                .replaceAll("^```json\\s*", "")
                .replaceAll("```$", "")
                .trim();

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1);
        }

        log.warn("无法提取有效的JSON结构，返回空对象");
        return "{}";
    }

    /**
     * 从JSON字符串解析关键词列表
     */
    private List<String> parseKeywordsFromJson(String jsonString) {
        try {
            String cleanedJson = cleanJsonString(jsonString);
            JsonNode rootNode = objectMapper.readTree(cleanedJson);
            JsonNode keywordsNode = rootNode.get("keywords");

            if (keywordsNode != null && keywordsNode.isArray()) {
                List<String> keywords = new ArrayList<>();
                keywordsNode.forEach(node -> {
                    String keyword = node.asText();
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        keywords.add(keyword.trim());
                    }
                });
                return keywords;
            }
        } catch (Exception e) {
            log.error("解析关键词JSON失败: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    /**
     * 从JSON字符串解析敏感信息列表
     */
    private List<SecurityResult.SensitiveInfo> parseSensitiveInfoFromJson(String jsonString) {
        try {
            String cleanedJson = cleanJsonString(jsonString);
            JsonNode rootNode = objectMapper.readTree(cleanedJson);
            JsonNode sensitiveInfoNode = rootNode.get("sensitiveInfo");

            if (sensitiveInfoNode != null && sensitiveInfoNode.isArray()) {
                List<SecurityResult.SensitiveInfo> result = new ArrayList<>();
                sensitiveInfoNode.forEach(node -> {
                    SecurityResult.SensitiveInfo.SensitiveInfoBuilder builder = SecurityResult.SensitiveInfo.builder()
                            .type(node.has("type") ? node.get("type").asText() : "未知")
                            .content(node.has("content") ? node.get("content").asText() : "")
                            .risk(node.has("risk") ? node.get("risk").asText() : "中");

                    // 处理position字段
                    if (node.has("position")) {
                        JsonNode posNode = node.get("position");
                        if (posNode.isObject() && posNode.has("start") && posNode.has("end")) {
                            builder.position(SecurityResult.Position.builder()
                                    .start(posNode.get("start").asInt())
                                    .end(posNode.get("end").asInt())
                                    .build());
                        }
                    }

                    result.add(builder.build());
                });
                return result;
            }
        } catch (Exception e) {
            log.error("解析敏感信息JSON失败: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}