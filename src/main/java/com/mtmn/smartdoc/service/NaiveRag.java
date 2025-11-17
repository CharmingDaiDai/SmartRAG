package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.common.ApacheTikaDocumentParser;
import com.mtmn.smartdoc.common.CustomException;
import com.mtmn.smartdoc.po.DocumentPO;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.utils.SseUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.mtmn.smartdoc.service.impl.KnowledgeBaseServiceImpl.getCurrentUserId;
import static com.mtmn.smartdoc.service.impl.KnowledgeBaseServiceImpl.getStoreKnowledgeBaseName;

/**
 * 朴素RAG实现
 * 基于简单的文档分块和向量检索的RAG方法
 * 
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/5/8 09:19
 */
@Component("naiveRag")
@Log4j2
@RequiredArgsConstructor
public class NaiveRag implements BaseRag {

    private final MinioService minioService;
    private final SseUtil sseUtil;
    private final MilvusService milvusService;

    /**
     * 获取RAG方法名称
     * 
     * 实现思路：
     * 1. 返回该RAG实现的标识名称
     * 2. 用于RAG策略工厂的策略选择
     * 3. 与配置文件中的方法名保持一致
     * 
     * @return RAG方法名称："naive"
     */
    @Override
    public String getMethodName() {
        return "naive";
    }

    /**
     * 构建朴素RAG索引
     * 
     * 实现思路：
     * 1. 从参数中获取分块配置（块大小、重叠大小）和嵌入模型名称
     * 2. 验证必要参数的有效性，特别是嵌入模型名称
     * 3. 创建对应的嵌入模型实例
     * 4. 获取当前用户ID并构建知识库的集合名称
     * 5. 创建Milvus嵌入存储实例
     * 6. 遍历文档列表，使用Apache Tika解析每个文档内容
     * 7. 使用文档分割器将长文档分割成较小的文本段
     * 8. 为每个文本段生成嵌入向量
     * 9. 将文本段和对应的嵌入向量存储到Milvus中
     * 10. 记录每个文档的处理结果并返回成功状态列表
     * 
     * @param kbName 知识库名称
     * @param documentPoList 要建立索引的文档列表
     * @param params 索引构建参数，包含分块配置和嵌入模型等
     * @return 每个文档的索引构建结果列表，true表示成功，false表示失败
     * @throws CustomException 当嵌入模型为空时抛出
     * @throws BadCredentialsException 当用户未登录时抛出
     */
    @Override
    public List<Boolean> buildIndex(String kbName, List<DocumentPO> documentPoList, Map<String, Object> params) {
        // 获取配置参数
        Integer chunkSize = (Integer) params.getOrDefault("chunk-size", 512);
        Integer chunkOverlap = (Integer) params.getOrDefault("chunk-overlap", 50);
        String embeddingModelName = (String) params.get("embeddingModelName");
        if (embeddingModelName == null) {
            throw new CustomException("索引构建失败: embedding 模型为空");
        }

        log.info("使用嵌入模型：{} 创建索引", embeddingModelName);

        // 创建Embedding模型
        EmbeddingModel embeddingModel = EmbeddingService.createEmbeddingModel(embeddingModelName);

        List<Boolean> success = new ArrayList<>();

        log.info("使用朴素RAG配置，块大小：{}，重叠大小：{}", chunkSize, chunkOverlap);

        Long userId = getCurrentUserId();
        if (null == userId) {
            throw new BadCredentialsException("请登录");
        }

        String collectionName = getStoreKnowledgeBaseName(kbName);

        MilvusEmbeddingStore embeddingStore = milvusService.getEmbeddingStore(collectionName, embeddingModel.dimension());

        List<Document> documents = new ArrayList<>();

        ApacheTikaDocumentParser documentParser = new ApacheTikaDocumentParser();

        for (DocumentPO documentPo : documentPoList) {
            String filePath = documentPo.getFilePath();

            try (InputStream inputStream = minioService.getFileContent(filePath)) {
                // 使用Apache Tika解析器解析文档
                Document document = documentParser.parse(inputStream);
                documents.add(document);

                log.debug("成功从URL加载文档, 文档路径: {}", filePath);
            } catch (Exception e) {
                log.error("解析文档失败: {}, 错误: {}", filePath, e.getMessage(), e);
            }
        }

        for (Document document : documents) {
            log.debug("处理文档，元数据：{}", document.metadata());

            if (document.text() != null && !document.text().isEmpty()) {
                log.debug("文档内容预览：{}", document.text().substring(0, Math.min(200, document.text().length())) + "...");

                // 使用配置的chunkSize和chunkOverlap进行文档切分
                DocumentSplitter splitter = DocumentSplitters.recursive(chunkSize, chunkOverlap);
                List<TextSegment> segments = splitter.split(document);

                log.info("文档已切分为{}个片段", segments.size());

                // 将文档片段转换为向量并存入向量库
                if (segments.isEmpty()) {
                    log.warn("文档内容为空，跳过处理");
                    continue;
                }

                List<Embedding> embeddings = EmbeddingService.embedInBatches(embeddingModel, segments, 10);

                embeddingStore.addAll(embeddings, segments);

                success.add(true);

                continue;
            }

            success.add(false);
        }
        return success;
    }

    /**
     * 删除所有索引数据
     * 
     * 实现思路：
     * 1. 删除Milvus集合中的所有向量数据
     * 2. 清理相关的元数据信息
     * 3. 释放存储空间
     * 
     * @return 删除操作是否成功
     */
    @Override
    public Boolean deleteIndex() {
        return null;
    }

    /**
     * 根据文档ID列表删除指定索引
     * 
     * 实现思路：
     * 1. 验证文档ID列表的有效性
     * 2. 根据文档ID查找对应的向量记录
     * 3. 从Milvus集合中删除指定的向量数据
     * 4. 更新索引状态和统计信息
     * 
     * @param docIds 要删除的文档ID列表
     * @return 删除操作是否成功
     */
    @Override
    public Boolean deleteIndex(List<String> docIds) {
        return null;
    }

    /**
     * 朴素RAG问答服务
     * 
     * 实现思路：
     * 1. 从参数中获取TopK值，默认为10个最相关文档
     * 2. 使用指定的嵌入模型对用户问题进行向量化
     * 3. 在Milvus向量数据库中进行相似度检索
     * 4. 构建内容检索器，设置最大结果数量
     * 5. 检索与问题最相关的文档片段
     * 6. 如果没有找到相关内容，返回提示信息
     * 7. 构建包含检索指导的提示模板，提高回答质量
     * 8. 将检索到的文档片段格式化为上下文
     * 9. 使用SSE流式输出处理聊天响应
     * 10. 异常处理：捕获并返回友好的错误信息
     * 
     * @param knowledgeBase 知识库对象，包含配置信息
     * @param question 用户提出的问题
     * @param params 查询参数，包含topk等配置
     * @return 流式响应，实时返回生成的答案
     */
    @Override
    public Flux<String> chat(KnowledgeBase knowledgeBase, String question, Map<String, Object> params) {
        Integer topk = (Integer) params.getOrDefault("topk", 10);

        String kbName = knowledgeBase.getName();

        String embeddingModelName = knowledgeBase.getEmbeddingModel();

        try {
            // 创建Embedding模型
            EmbeddingModel embeddingModel = EmbeddingService.createEmbeddingModel(embeddingModelName);

            String collectionName = getStoreKnowledgeBaseName(kbName);

            MilvusEmbeddingStore embeddingStore = milvusService.getEmbeddingStore(collectionName, embeddingModel.dimension());

            ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    .maxResults(topk)
                    .build();

            List<Content> contents = new ArrayList<>(contentRetriever.retrieve(new Query(question)));

            if (contents.isEmpty()) {
                log.warn("知识库中没有找到与您问题相关的信息。");
                return sseUtil.sendFluxMessage("知识库中没有找到与您问题相关的信息。");
            }

            String promptTemplate = """
                    作为一个精确的RAG系统助手，请严格按照以下指南回答用户问题：
                    1. 仔细分析问题，识别关键词和核心概念。
                    2. 从提供的上下文中精确定位相关信息，优先使用完全匹配的内容。
                    3. 构建回答时，确保包含所有必要的关键词，提高关键词评分(scoreikw)。
                    4. 保持回答与原文的语义相似度，以提高向量相似度评分(scoreies)。
                    5. 对于表格查询或需要多段落/多文档综合的问题，给予特别关注并提供更全面的回答。
                    6. 如果上下文信息不足，可以进行合理推理，但要明确指出推理部分。
                    7. 回答应准确、完整，直接解答问题，避免不必要的解释。
                    8. 不要输出“检索到的文本块”、“根据”，“信息”等前缀修饰句，直接输出答案即可
                    9. 不要使用"根据提供的信息"、"支撑信息显示"等前缀，直接给出答案。
                    问题: %s
                    参考上下文：
                    ···
                    %s
                    ···
                    请提供准确且相关的回答：""";

            // 准备检索到的文档列表和提示词片段
            List<String> docContents = new ArrayList<>();
            StringBuilder contextBuilder = new StringBuilder();

            // 使用IntStream处理文档片段
            IntStream.range(0, contents.size()).forEach(i -> {
                String segmentText = contents.get(i).textSegment().text();
                contextBuilder.append(String.format("【片段%d】\n%s\n\n", i + 1, segmentText));
//                docContents.add(String.format("出处 [%d] %s\n\n", i + 1, segmentText));
                docContents.add(segmentText);
            });

            String prompt = String.format(promptTemplate, contextBuilder.toString(), question);

            return sseUtil.handleStreamingChatResponse(prompt, docContents);
        } catch (Exception e) {
            log.error("RAG问答处理失败", e);
            // 生成错误对象的新格式响应
            String errorMessage = "抱歉，处理您的问题时遇到了错误：" + e.getMessage();
            String escapedError = errorMessage.replace("\"", "\\\"").replace("\n", "\\n");
            return sseUtil.sendFluxMessage("escapedError");
        }
    }

    /**
     * 检查是否支持指定的RAG方法
     * 
     * 实现思路：
     * 1. 验证传入的RAG方法名称
     * 2. 检查当前实现是否支持该方法
     * 3. 返回支持状态布尔值
     * 
     * @param ragMethod RAG方法名称
     * @return 是否支持该RAG方法
     */
    @Override
    public boolean supports(String ragMethod) {
        return true;
    }
}