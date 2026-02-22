package com.mtmn.smartdoc.rag.impl;

import com.mtmn.smartdoc.common.MyNode;
import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.dto.IndexUpdateItem;
import com.mtmn.smartdoc.model.dto.VectorItem;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Chunk;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.rag.AbstractIndexStrategy;
import com.mtmn.smartdoc.rag.config.HisemRagFastIndexConfig;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.repository.ChunkRepository;
import com.mtmn.smartdoc.service.IndexingProgressCallback;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.service.MinioService;
import com.mtmn.smartdoc.utils.MarkdownProcessor;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HisemRAG Fast 索引策略实现
 * 基于 Markdown 层级结构切分，支持标题增强
 *
 * @author charmingdaidai
 * @version 3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HisemRAGFastIndexStrategy extends AbstractIndexStrategy {

    private static final String MARKDOWN_MIME_TYPE = "text/markdown";
    private static final String MARKDOWN_X_MIME_TYPE = "text/x-markdown";

    private final ChunkRepository chunkRepository;
    private final MinioService minioService;
    private final MilvusService milvusService;
    private final ModelFactory modelFactory;

    @Override
    public IndexStrategyType getType() {
        return IndexStrategyType.HISEM_RAG_FAST;
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config) {
        buildIndex(kb, documentPo, config, IndexingProgressCallback.NOOP);
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config,
                           IndexingProgressCallback callback) {
        log.info("Building HisemRAG Fast index for document: id={}, filename={}",
                documentPo.getId(), documentPo.getFilename());
        Long docId = documentPo.getId();
        String docName = documentPo.getFilename();

        try {
            // 1. 校验文件类型必须是 Markdown
            validateMarkdownFile(documentPo);

            // 2. 读取文档内容
            callback.onStepChanged(docId, docName, IndexingStep.READING);
            String content = readDocumentContent(documentPo);

            // 3. 使用 Markdown 解析器处理内容
            callback.onStepChanged(docId, docName, IndexingStep.PARSING);
            List<TextSegment> segments = processContent(content, config, docName);

            // 4. 持久化 Chunks 到 MySQL
            callback.onStepChanged(docId, docName, IndexingStep.SAVING);
            chunkRepository.deleteByDocumentId(docId);

            Long kbId = kb.getId();
            List<Chunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                Chunk chunk = new Chunk();
                chunk.setKbId(kbId);
                chunk.setDocumentId(docId);
                chunk.setChunkIndex(i);
                chunk.setContent(segment.text());
                chunk.setIsModified(false);
                chunk.setStrategyType("HISEM_RAG_FAST");
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());
                chunkEntities.add(chunk);
            }
            List<Chunk> savedChunks = chunkRepository.saveAll(chunkEntities);

            // 5. 向量化并存储到 Milvus
            callback.onStepChanged(docId, docName, IndexingStep.EMBEDDING);
            vectorizeAndStore(savedChunks, kb, docId);

            log.info("HisemRAG Fast index built successfully: documentId={}, segments={}",
                    docId, segments.size());
        } catch (Exception e) {
            log.error("Failed to build HisemRAG Fast index: documentId={}", docId, e);
            throw new RuntimeException("Index building failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config) {
        rebuildIndexFromChunks(kb, document, config, IndexingProgressCallback.NOOP);
    }

    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                                       IndexingProgressCallback callback) {
        log.info("Rebuilding HisemRAG Fast index from chunks for document: id={}", document.getId());
        Long docId = document.getId();
        String docName = document.getFilename();

        try {
            // 1. 获取现有 Chunks
            List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(docId);
            if (chunks.isEmpty()) {
                log.warn("No chunks found for document: {}, falling back to full build.", docId);
                buildIndex(kb, document, config, callback);
                return;
            }

            // 2. 删除旧向量 + 重新向量化
            callback.onStepChanged(docId, docName, IndexingStep.EMBEDDING);
            deleteVectorsByDocumentIds(kb.getId(), List.of(docId));
            vectorizeAndStore(chunks, kb, docId);

            log.info("HisemRAG Fast index rebuilt from chunks successfully: documentId={}, chunks={}",
                    docId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to rebuild HisemRAG Fast index from chunks: documentId={}", docId, e);
            throw new RuntimeException("Index rebuild failed: " + e.getMessage(), e);
        }
    }

    /**
     * 校验文件必须是 Markdown 类型
     */
    private void validateMarkdownFile(DocumentPo document) {
        String filePath = document.getFilePath();
        String contentType = minioService.getFileMetadata(filePath).contentType();
        String filename = document.getFilename();

        boolean isMarkdown = MARKDOWN_MIME_TYPE.equalsIgnoreCase(contentType)
                || MARKDOWN_X_MIME_TYPE.equalsIgnoreCase(contentType)
                || (filename != null && filename.toLowerCase().endsWith(".md"));

        if (!isMarkdown) {
            throw new IllegalArgumentException(
                    String.format("HisemRAG Fast 策略仅支持 Markdown 文件，当前文件类型: %s", contentType));
        }
    }

    @Override
    protected String readDocumentContent(DocumentPo document) {
        String filePath = document.getFilePath();
        log.debug("Reading raw Markdown content from MinIO: {}", filePath);

        // 直接读取原始文本，保留 Markdown 格式（包括 # 标题符号）
        // 不使用 DocumentParseUtils，因为 MarkdownDocumentParser 会去掉 # 符号
        try (InputStream inputStream = minioService.getFileStream(filePath)) {
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read document content: documentId={}", document.getId(), e);
            throw new RuntimeException("Failed to read document content", e);
        }
    }

    /**
     * 使用 MarkdownProcessor 处理内容
     */
    protected List<TextSegment> processContent(String content, IndexStrategyConfig config, String documentTitle) {
        HisemRagFastIndexConfig hisemConfig = (HisemRagFastIndexConfig) config;

        Integer maxLength = hisemConfig.getMaxLength();
        boolean enableTitleEnhancement = Boolean.TRUE.equals(hisemConfig.getEnableTitleEnhancement());

        // 使用 MarkdownProcessor 解析 Markdown 内容
        // maxLevel 设为 null 表示不限制层级
        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownProcessor.parseMarkdownContent(content, documentTitle, null, maxLength);

        MyNode rootNode = result.getKey();
        Map<String, MyNode> nodesDict = result.getValue();

        // 标题增强：拼接多级标题路径（跳过根节点/文件名，从一级标题开始）
        if (enableTitleEnhancement) {
            for (String childId : rootNode.getChildren()) {
                if (nodesDict.containsKey(childId)) {
                    MarkdownProcessor.buildTitlePaths(nodesDict.get(childId), nodesDict, "");
                }
            }
            log.debug("Title enhancement applied for document: {}", documentTitle);
        }

        // 获取所有叶子节点并转换为 TextSegment
        Map<String, MyNode> leafNodes = MarkdownProcessor.findLeafNodes(nodesDict);
        List<TextSegment> segments = new ArrayList<>();

        int chunkIndex = 0;
        for (MyNode node : leafNodes.values()) {
            String nodeContent = node.getPageContent();
            if (nodeContent == null || nodeContent.trim().isEmpty()) {
                continue;
            }

            // 将拼接后的标题添加到内容前面，提高检索准确性
            String enhancedContent;
            if (enableTitleEnhancement && node.getTitle() != null && !node.getTitle().isEmpty()) {
                enhancedContent = node.getTitle() + "\n\n" + nodeContent;
            } else {
                enhancedContent = nodeContent;
            }

            // 构建 TextSegment，标题作为元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("chunk_index", chunkIndex);
            metadata.put("title", node.getTitle());
            metadata.put("level", node.getLevel());
            if (node.getBlockNumber() != null) {
                metadata.put("block_number", node.getBlockNumber());
            }

            TextSegment segment = TextSegment.from(enhancedContent, new dev.langchain4j.data.document.Metadata(metadata));
            segments.add(segment);
            chunkIndex++;
        }

        log.info("Markdown parsed into {} segments with title enhancement: {}",
                segments.size(), enableTitleEnhancement);

        return segments;
    }

    @Override
    protected List<TextSegment> processContent(String content, IndexStrategyConfig config) {
        // 默认使用空标题
        return processContent(content, config, "Untitled");
    }

    @Override
    protected void persist(KnowledgeBase kb, List<TextSegment> segments, Long documentId) {
        try {
            // 删除文档原有的 chunks
            chunkRepository.deleteByDocumentId(documentId);

            Long kbId = kb.getId();
            List<Chunk> chunkEntities = new ArrayList<>();

            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                Chunk chunk = new Chunk();
                chunk.setKbId(kbId);
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(i);
                chunk.setContent(segment.text());
                chunk.setIsModified(false);
                chunk.setStrategyType("HISEM_RAG_FAST");
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());
                chunkEntities.add(chunk);
            }

            List<Chunk> savedChunks = chunkRepository.saveAll(chunkEntities);
            vectorizeAndStore(savedChunks, kb, documentId);

            log.info("Persisted {} chunks for document: {}", savedChunks.size(), documentId);

        } catch (Exception e) {
            log.error("Failed to persist chunks: documentId={}", documentId, e);
            throw new RuntimeException("Failed to persist chunks: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(KnowledgeBase kb, List<Long> documentIds) {
        Long kbId = kb.getId();
        log.info("Deleting index for documents: documentIds={}, kbId={}", documentIds, kbId);

        try {
            // 1. 先删除向量
            deleteVectorsByDocumentIds(kbId, documentIds);

            // 2. 再删除 Chunks
            for (Long docId : documentIds) {
                chunkRepository.deleteByDocumentId(docId);
            }

            log.info("✅ Index deleted successfully for {} documents", documentIds.size());

        } catch (Exception e) {
            log.error("❌ Failed to delete index: documentIds={}, kbId={}", documentIds, kbId, e);
            throw new RuntimeException("Index deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(KnowledgeBase kb) {
        Long kbId = kb.getId();
        log.info("Deleting entire index for knowledge base: kbId={}", kbId);

        try {
            // 1. 删除向量库中的集合
            String collectionName = AppConstants.Milvus.CHUNKS_TEMPLATE.formatted(kbId);
            milvusService.dropCollection(collectionName);

            // 2. 删除数据库中的 Chunks
            chunkRepository.deleteByKbId(kbId);

            log.info("✅ Entire index deleted successfully for kbId={}", kbId);

        } catch (Exception e) {
            log.error("❌ Failed to delete entire index: kbId={}", kbId, e);
            throw new RuntimeException("Index deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * 向量化并存储到 Milvus
     */
    private void vectorizeAndStore(List<Chunk> chunks, KnowledgeBase kb, Long documentId) {
        log.info("Vectorizing {} chunks for document: {}", chunks.size(), documentId);

        Long kbId = kb.getId();

        try {
            String embeddingModelId = kb.getEmbeddingModelId();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(embeddingModelId);

            List<String> contents = chunks.stream()
                    .map(Chunk::getContent)
                    .collect(Collectors.toList());
            List<Embedding> embeddings = embeddingClient.embedBatch(contents);

            List<VectorItem> vectorItems = new ArrayList<>(chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                Chunk chunk = chunks.get(i);
                Embedding embedding = embeddings.get(i);

                VectorItem item = new VectorItem();
                item.setId(chunk.getId().toString());
                item.setEmbedding(embedding);
                item.setContent(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_id", chunk.getId());
                metadata.put("document_id", documentId);
                metadata.put("kb_id", kbId);
                metadata.put("chunk_index", chunk.getChunkIndex());
                item.setMetadata(metadata);

                vectorItems.add(item);
            }

            milvusService.store(kbId, vectorItems);

        } catch (Exception e) {
            log.error("Failed to vectorize chunks: documentId={}", documentId, e);
            throw new RuntimeException("Vectorization failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除向量数据
     */
    private void deleteVectorsByDocumentIds(Long kbId, List<Long> documentIds) {
        try {
            log.debug("Deleting vectors from collection: {}, documentIds: {}", kbId, documentIds);

            // 1. 查询关联的 Chunks
            List<Chunk> chunks = chunkRepository.findByDocumentIdIn(documentIds);
            if (chunks.isEmpty()) {
                return;
            }

            // 2. 构建删除请求
            List<IndexUpdateItem> items = chunks.stream()
                    .map(chunk -> {
                        IndexUpdateItem item = new IndexUpdateItem();
                        item.setId(chunk.getId().toString());
                        item.setUpdateType(IndexUpdateItem.UpdateType.DELETE);
                        return item;
                    })
                    .collect(Collectors.toList());

            // 3. 调用 MilvusService 删除向量
            milvusService.update(kbId, items);

        } catch (Exception e) {
            log.error("Failed to delete vectors: collection={}, documentIds={}", kbId, documentIds, e);
            throw new RuntimeException("Failed to delete vectors: " + e.getMessage(), e);
        }
    }
}