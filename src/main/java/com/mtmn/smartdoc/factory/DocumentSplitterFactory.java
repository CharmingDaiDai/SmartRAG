package com.mtmn.smartdoc.factory;

import com.mtmn.smartdoc.rag.config.NaiveRagIndexConfig;
import com.mtmn.smartdoc.enums.SplitterType;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切分器工厂
 * 根据配置创建对应的 LangChain4j 切分器
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Slf4j
public class DocumentSplitterFactory {

    /**
     * 根据配置创建切分器
     *
     * @param config NaiveRAG 配置
     * @return 文档切分器
     */
    public static DocumentSplitter createSplitter(NaiveRagIndexConfig config) {
        SplitterType splitterType = config.getSplitterType();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        log.debug("Creating splitter: type={}, chunkSize={}, overlap={}", splitterType, chunkSize, overlap);

        return switch (splitterType) {
            case BY_PARAGRAPH -> new DocumentByParagraphSplitter(
                    chunkSize,
                    overlap
            );

            case BY_LINE -> new DocumentByLineSplitter(
                    chunkSize,
                    overlap
            );

            case BY_SENTENCE -> new DocumentBySentenceSplitter(
                    chunkSize,
                    overlap
            );

            case BY_SEPARATOR -> new CustomSeparatorSplitter(
                    config.getSeparator(),
                    chunkSize,
                    overlap
            );
        };
    }

    /**
     * 自定义分隔符切分器
     * 用于 BY_SEPARATOR 模式
     */
    private static class CustomSeparatorSplitter implements DocumentSplitter {
        private final String separator;
        private final int maxChunkSize;
        private final int maxOverlapSize;

        public CustomSeparatorSplitter(String separator, int maxChunkSize, int maxOverlapSize) {
            this.separator = separator;
            this.maxChunkSize = maxChunkSize;
            this.maxOverlapSize = maxOverlapSize;
        }

        @Override
        public List<TextSegment> split(Document document) {
            String content = document.text();
            List<TextSegment> segments = new ArrayList<>();

            String[] parts = content.split(separator);
            StringBuilder currentChunk = new StringBuilder();
            int currentSize = 0;

            for (String part : parts) {
                if (currentSize + part.length() > maxChunkSize && !currentChunk.isEmpty()) {
                    // 保存当前 chunk
                    segments.add(TextSegment.from(currentChunk.toString()));

                    // 处理重叠
                    if (maxOverlapSize > 0) {
                        String overlapText = currentChunk.substring(
                                Math.max(0, currentChunk.length() - maxOverlapSize));
                        currentChunk = new StringBuilder(overlapText);
                        currentSize = overlapText.length();
                    } else {
                        currentChunk = new StringBuilder();
                        currentSize = 0;
                    }
                }

                currentChunk.append(part).append(separator);
                currentSize += part.length() + separator.length();
            }

            // 添加最后一个 chunk
            if (!currentChunk.isEmpty()) {
                segments.add(TextSegment.from(currentChunk.toString()));
            }

            return segments;
        }
    }
}