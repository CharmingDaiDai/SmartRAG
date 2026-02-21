package com.mtmn.smartdoc.utils;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.parser.markdown.MarkdownDocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;

import java.io.InputStream;

/**
 * @author charmingdaidai
 * @version 1.0
 * 文档解析工具类
 * 基于 LangChain4j 的多种 Parser 实现
 */
@Slf4j
public class DocumentParseUtils {


    // Tika 探测器实例
    private static final Tika TIKA_DETECTOR = new Tika();

    // 预加载解析器实例
    private static final DocumentParser PDF_PARSER = new ApachePdfBoxDocumentParser();
    private static final DocumentParser OFFICE_PARSER = new ApachePoiDocumentParser();
    private static final DocumentParser MARKDOWN_PARSER = new MarkdownDocumentParser();
    private static final DocumentParser TEXT_PARSER = new TextDocumentParser();
    private static final DocumentParser TIKA_PARSER = new ApacheTikaDocumentParser();

    /*
      解析文档
     */
    /**
     * 解析文档
     *
     * @param mediaTypeFromStorage 存储服务(MinIO)提供的类型，可为 null
     */
    public static String parse(InputStream inputStream, String originalFileName, String mediaTypeFromStorage) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try (TikaInputStream tis = TikaInputStream.get(inputStream)) {
            String mimeType;

            // 策略 1: 如果 MinIO 提供了有效的类型，直接信赖 MinIO
            if (mediaTypeFromStorage != null && !mediaTypeFromStorage.equals("application/octet-stream")) {
                mimeType = mediaTypeFromStorage;
            }
            // 策略 2: 使用 Tika 探测流 + 文件名
            else {
                mimeType = TIKA_DETECTOR.detect(tis, originalFileName);

                // 策略 3 (修复你的报错):
                // 如果 Tika 探测结果是 octet-stream (未知流)，但文件名明明是 .pdf
                // 则强制通过文件名再判断一次
                if ("application/octet-stream".equals(mimeType) && originalFileName != null) {
                    // 使用 Tika 单纯基于文件名的探测能力
                    String extensionType = new Tika().detect(originalFileName);
                    log.warn("Stream detection failed (octet-stream). Fallback to filename detection: {} -> {}", originalFileName, extensionType);

                    if (extensionType != null && !extensionType.equals("application/octet-stream")) {
                        mimeType = extensionType;
                    }
                }
            }

            log.info("Final used MIME type: {} for file: {}", mimeType, originalFileName);

            // 路由到解析器
            DocumentParser parser = selectParserByMimeType(mimeType);

            // 执行解析
            return parser.parse(tis).text();

        } catch (Exception e) {
            log.error("Failed to parse document: {}", originalFileName, e);
            throw new RuntimeException("Document parsing failed", e);
        }
    }

    // 保留旧的重载方法
    public static String parse(InputStream inputStream, String originalFileName) {
        return parse(inputStream, originalFileName, null);
    }

    /**
     * 根据 MIME Type 选择解析器
     */
    private static DocumentParser selectParserByMimeType(String mimeType) {
        if (mimeType == null) {
            return TIKA_PARSER;
        }

        // 处理带参数的 mimeType，如 "text/plain; charset=UTF-8" -> "text/plain"
        String baseMimeType = mimeType.split(";")[0].trim().toLowerCase();

        return switch (baseMimeType) {
            // PDF
            case "application/pdf" -> PDF_PARSER;

            // Microsoft Word
            // doc
            // docx
            // Microsoft Excel
            // xls
            // xlsx
            // Microsoft PowerPoint
            // ppt
            // pptx
            case "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                 "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.ms-powerpoint",
                 "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> OFFICE_PARSER;

            // Markdown
            case "text/markdown", "text/x-markdown" -> MARKDOWN_PARSER;

            // 纯文本类
            case "text/plain", "application/json", "text/xml", "application/xml", "text/yaml", "application/yaml",
                 "text/x-java-source" -> TEXT_PARSER;

            // 其他所有格式 (图片、HTML、未知二进制等) 走 Tika 兜底
            default -> {
                log.debug("No specific parser for MIME type [{}], falling back to Tika.", mimeType);
                yield TIKA_PARSER;
            }
        };
    }
}