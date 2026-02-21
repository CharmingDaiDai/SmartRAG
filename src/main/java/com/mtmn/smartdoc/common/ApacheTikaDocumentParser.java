package com.mtmn.smartdoc.common;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 使用Apache Tika实现的文档解析器
 * @date 2025/5/9 15:02
 */

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrBlank;

/**
 * 使用Apache Tika库将文件解析为{@link Document}对象，自动检测文件格式。
 * 该解析器支持多种文件格式，包括PDF、DOC、PPT、XLS等。
 * 有关支持的格式的详细信息，
 * 请参阅<a href="https://tika.apache.org/2.9.1/formats.html">Apache Tika文档</a>。
 */
public class ApacheTikaDocumentParser implements DocumentParser {

    public static final Supplier<Parser> DEFAULT_PARSER_SUPPLIER = AutoDetectParser::new;
    public static final Supplier<Metadata> DEFAULT_METADATA_SUPPLIER = Metadata::new;
    public static final Supplier<ParseContext> DEFAULT_PARSE_CONTEXT_SUPPLIER = ParseContext::new;
    private static final int NO_WRITE_LIMIT = -1;
    public static final Supplier<ContentHandler> DEFAULT_CONTENT_HANDLER_SUPPLIER =
            () -> new BodyContentHandler(NO_WRITE_LIMIT);

    private final Supplier<Parser> parserSupplier;
    private final Supplier<ContentHandler> contentHandlerSupplier;
    private final Supplier<Metadata> metadataSupplier;
    private final Supplier<ParseContext> parseContextSupplier;

    private final boolean includeMetadata;

    /**
     * 使用默认Tika组件创建{@code ApacheTikaDocumentParser}实例。
     * 它使用{@link AutoDetectParser}、无写入限制的{@link BodyContentHandler}、
     * 空的{@link Metadata}和空的{@link ParseContext}。
     * 注意：默认情况下，不会向解析的文档中添加元数据。
     */
    public ApacheTikaDocumentParser() {
        this(false);
    }

    /**
     * 使用默认Tika组件创建{@code ApacheTikaDocumentParser}实例。
     * 它使用{@link AutoDetectParser}、无写入限制的{@link BodyContentHandler}、
     * 空的{@link Metadata}和空的{@link ParseContext}。
     *
     * @param includeMetadata 是否在解析的文档中包含元数据
     */
    public ApacheTikaDocumentParser(boolean includeMetadata) {
        this(null, null, null, null, includeMetadata);
    }

    /**
     * 使用提供的Tika组件创建{@code ApacheTikaDocumentParser}实例。
     * 如果某些组件未提供({@code null})，将使用默认值。
     *
     * @param parser         要使用的Tika解析器。默认值：{@link AutoDetectParser}
     * @param contentHandler Tika内容处理器。默认值：无写入限制的{@link BodyContentHandler}
     * @param metadata       Tika元数据。默认值：空的{@link Metadata}
     * @param parseContext   Tika解析上下文。默认值：空的{@link ParseContext}
     * @deprecated 如果您打算将此解析器用于多个文件，请使用带有Tika组件供应商的构造函数。
     */
    @Deprecated(forRemoval = true)
    public ApacheTikaDocumentParser(
            Parser parser, ContentHandler contentHandler, Metadata metadata, ParseContext parseContext) {
        this(
                () -> getOrDefault(parser, DEFAULT_PARSER_SUPPLIER),
                () -> getOrDefault(contentHandler, DEFAULT_CONTENT_HANDLER_SUPPLIER),
                () -> getOrDefault(metadata, DEFAULT_METADATA_SUPPLIER),
                () -> getOrDefault(parseContext, DEFAULT_PARSE_CONTEXT_SUPPLIER),
                false);
    }

    /**
     * 使用提供的Tika组件供应商创建{@code ApacheTikaDocumentParser}实例。
     * 如果某些供应商未提供({@code null})，将使用默认值。
     *
     * @param parserSupplier         Tika解析器的供应商。默认值：{@link AutoDetectParser}
     * @param contentHandlerSupplier Tika内容处理器的供应商。默认值：无写入限制的{@link BodyContentHandler}
     * @param metadataSupplier       Tika元数据的供应商。默认值：空的{@link Metadata}
     * @param parseContextSupplier   Tika解析上下文的供应商。默认值：空的{@link ParseContext}
     * @deprecated 如果您打算将此解析器用于多个文件，请使用带有Tika组件供应商的构造函数，
     * 并指定是否包含元数据。
     */
    @Deprecated(forRemoval = true)
    public ApacheTikaDocumentParser(
            Supplier<Parser> parserSupplier,
            Supplier<ContentHandler> contentHandlerSupplier,
            Supplier<Metadata> metadataSupplier,
            Supplier<ParseContext> parseContextSupplier) {
        this(parserSupplier, contentHandlerSupplier, metadataSupplier, parseContextSupplier, false);
    }

    /**
     * 使用提供的Tika组件供应商创建{@code ApacheTikaDocumentParser}实例。
     * 如果某些供应商未提供({@code null})，将使用默认值。
     *
     * @param parserSupplier         Tika解析器的供应商。默认值：{@link AutoDetectParser}
     * @param contentHandlerSupplier Tika内容处理器的供应商。默认值：无写入限制的{@link BodyContentHandler}
     * @param metadataSupplier       Tika元数据的供应商。默认值：空的{@link Metadata}
     * @param parseContextSupplier   Tika解析上下文的供应商。默认值：空的{@link ParseContext}
     * @param includeMetadata        是否在解析的文档中包含元数据
     */
    public ApacheTikaDocumentParser(
            Supplier<Parser> parserSupplier,
            Supplier<ContentHandler> contentHandlerSupplier,
            Supplier<Metadata> metadataSupplier,
            Supplier<ParseContext> parseContextSupplier,
            boolean includeMetadata) {
        this.parserSupplier = getOrDefault(parserSupplier, () -> DEFAULT_PARSER_SUPPLIER);
        this.contentHandlerSupplier = getOrDefault(contentHandlerSupplier, () -> DEFAULT_CONTENT_HANDLER_SUPPLIER);
        this.metadataSupplier = getOrDefault(metadataSupplier, () -> DEFAULT_METADATA_SUPPLIER);
        this.parseContextSupplier = getOrDefault(parseContextSupplier, () -> DEFAULT_PARSE_CONTEXT_SUPPLIER);
        this.includeMetadata = includeMetadata;
    }

    @Override
    public Document parse(InputStream inputStream) {
        try {
            Parser parser = parserSupplier.get();
            ContentHandler contentHandler = contentHandlerSupplier.get();
            Metadata metadata = metadataSupplier.get();
            ParseContext parseContext = parseContextSupplier.get();

            parser.parse(inputStream, contentHandler, metadata, parseContext);
            String text = contentHandler.toString();

            if (isNullOrBlank(text)) {
                throw new BlankDocumentException();
            }

            return includeMetadata ? Document.from(text, convert(metadata)) : Document.from(text);
        } catch (BlankDocumentException e) {
            throw e;
        } catch (ZeroByteFileException e) {
            throw new BlankDocumentException();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将Tika的{@link Metadata}对象转换为{@link dev.langchain4j.data.document.Metadata}对象。
     *
     * @param tikaMetadata 来自Tika库的包含元数据信息的{@code Metadata}对象
     * @return 以langchain4j格式表示的{@link dev.langchain4j.data.document.Metadata}对象。
     */
    private dev.langchain4j.data.document.Metadata convert(Metadata tikaMetadata) {

        final Map<String, String> tikaMetaData = new HashMap<>();

        for (String name : tikaMetadata.names()) {
            tikaMetaData.put(name, String.join(";", tikaMetadata.getValues(name)));
        }

        return new dev.langchain4j.data.document.Metadata(tikaMetaData);
    }
}