package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.MyNode;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.markdown.MarkdownDocumentParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 测试比较 LangChain4j MarkdownDocumentParser 和自定义 MarkdownParser 的解析结果
 *
 * @author charmingdaidai
 */
public class MarkdownDocumentParserComparisonTest {

    private static final String LOCAL_MD_FILE = "src/main/resources/doc_data/Redis.md";

    /**
     * 测试 LangChain4j MarkdownDocumentParser 解析本地 Markdown 文件
     */
    @Test
    public void testLangChain4jMarkdownParser_LocalFile() throws Exception {
        System.out.println("========== LangChain4j MarkdownDocumentParser 解析本地文件 ==========");
        System.out.println("文件路径: " + LOCAL_MD_FILE);
        System.out.println();

        MarkdownDocumentParser parser = new MarkdownDocumentParser();

        try (InputStream inputStream = new FileInputStream(LOCAL_MD_FILE)) {
            Document document = parser.parse(inputStream);
            String text = document.text();

            System.out.println("【解析后的文本内容】（前 2000 字符）:");
            System.out.println("----------------------------------------");
            System.out.println(text.length() > 2000 ? text.substring(0, 2000) + "..." : text);
            System.out.println("----------------------------------------");
            System.out.println("总字符数: " + text.length());
            System.out.println();

            // 检查是否保留了 # 标题结构
            boolean hasHashTitles = text.contains("# ");
            System.out.println("是否保留 # 标题结构: " + hasHashTitles);

            // 检查原文件中的标题
            String originalContent = Files.readString(Paths.get(LOCAL_MD_FILE));
            System.out.println("\n【原始文件中的标题】:");
            originalContent.lines()
                    .filter(line -> line.startsWith("#"))
                    .limit(10)
                    .forEach(line -> System.out.println("  " + line));
        }
    }

    /**
     * 测试 LangChain4j TextDocumentParser 解析本地 Markdown 文件（作为对比）
     */
    @Test
    public void testLangChain4jTextParser_LocalFile() throws Exception {
        System.out.println("========== LangChain4j TextDocumentParser 解析本地文件 ==========");
        System.out.println("文件路径: " + LOCAL_MD_FILE);
        System.out.println();

        TextDocumentParser parser = new TextDocumentParser();

        try (InputStream inputStream = new FileInputStream(LOCAL_MD_FILE)) {
            Document document = parser.parse(inputStream);
            String text = document.text();

            System.out.println("【解析后的文本内容】（前 2000 字符）:");
            System.out.println("----------------------------------------");
            System.out.println(text.length() > 2000 ? text.substring(0, 2000) + "..." : text);
            System.out.println("----------------------------------------");
            System.out.println("总字符数: " + text.length());
            System.out.println();

            // 检查是否保留了 # 标题结构
            boolean hasHashTitles = text.contains("# ");
            System.out.println("是否保留 # 标题结构: " + hasHashTitles);
        }
    }

    /**
     * 测试自定义 MarkdownParser 解析本地 Markdown 文件
     */
    @Test
    public void testCustomMarkdownParser_LocalFile() throws Exception {
        System.out.println("========== 自定义 MarkdownParser 解析本地文件 ==========");
        System.out.println("文件路径: " + LOCAL_MD_FILE);
        System.out.println();

        String markdownContent = Files.readString(Paths.get(LOCAL_MD_FILE));

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.buildDocumentStructure(markdownContent, "Redis", null);

        MyNode rootNode = result.getKey();
        Map<String, MyNode> nodesDict = result.getValue();

        System.out.println("【解析后的树结构】:");
        System.out.println("----------------------------------------");
        MarkdownParser.printDocumentStructure(rootNode, nodesDict, 0);
        System.out.println("----------------------------------------");
        System.out.println("总节点数: " + nodesDict.size());
    }

    /**
     * 测试 LangChain4j MarkdownDocumentParser 解析内存中的 Markdown 流
     */
    @Test
    public void testLangChain4jMarkdownParser_InputStream() {
        System.out.println("========== LangChain4j MarkdownDocumentParser 解析内存流 ==========");

        String markdownContent = """
                # 第一章 引言
                这是引言部分的内容。

                ## 1.1 背景介绍
                背景介绍的详细内容。

                ### 1.1.1 历史沿革
                历史沿革的内容。

                ## 1.2 研究目的
                研究目的的内容。

                # 第二章 方法论
                方法论的内容。

                ## 2.1 实验设计
                实验设计的详细说明。
                """;

        System.out.println("【原始 Markdown 内容】:");
        System.out.println("----------------------------------------");
        System.out.println(markdownContent);
        System.out.println("----------------------------------------");

        MarkdownDocumentParser parser = new MarkdownDocumentParser();
        InputStream inputStream = new ByteArrayInputStream(markdownContent.getBytes(StandardCharsets.UTF_8));
        Document document = parser.parse(inputStream);
        String text = document.text();

        System.out.println("\n【MarkdownDocumentParser 解析后】:");
        System.out.println("----------------------------------------");
        System.out.println(text);
        System.out.println("----------------------------------------");

        // 检查标题是否保留
        boolean hasLevel1 = text.contains("# 第一章");
        boolean hasLevel2 = text.contains("## 1.1");
        boolean hasLevel3 = text.contains("### 1.1.1");

        System.out.println("\n【标题保留情况】:");
        System.out.println("  一级标题 (# 第一章): " + hasLevel1);
        System.out.println("  二级标题 (## 1.1): " + hasLevel2);
        System.out.println("  三级标题 (### 1.1.1): " + hasLevel3);
    }

    /**
     * 测试自定义 MarkdownParser 解析内存中的 Markdown
     */
    @Test
    public void testCustomMarkdownParser_InMemory() {
        System.out.println("========== 自定义 MarkdownParser 解析内存流 ==========");

        String markdownContent = """
                # 第一章 引言
                这是引言部分的内容。

                ## 1.1 背景介绍
                背景介绍的详细内容。

                ### 1.1.1 历史沿革
                历史沿革的内容。

                ## 1.2 研究目的
                研究目的的内容。

                # 第二章 方法论
                方法论的内容。

                ## 2.1 实验设计
                实验设计的详细说明。
                """;

        System.out.println("【原始 Markdown 内容】:");
        System.out.println("----------------------------------------");
        System.out.println(markdownContent);
        System.out.println("----------------------------------------");

        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.buildDocumentStructure(markdownContent, "测试文档", null);

        MyNode rootNode = result.getKey();
        Map<String, MyNode> nodesDict = result.getValue();

        System.out.println("\n【自定义 MarkdownParser 解析后的树结构】:");
        System.out.println("----------------------------------------");
        MarkdownParser.printDocumentStructure(rootNode, nodesDict, 0);
        System.out.println("----------------------------------------");
        System.out.println("总节点数: " + nodesDict.size());

        // 打印每个节点的详细信息
        System.out.println("\n【各节点详情】:");
        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            MyNode node = entry.getValue();
            System.out.println("  节点: " + node.getTitle());
            System.out.println("    层级: " + node.getLevel());
            System.out.println("    内容长度: " + (node.getPageContent() != null ? node.getPageContent().length() : 0));
            System.out.println("    子节点数: " + node.getChildren().size());
            System.out.println();
        }
    }

    /**
     * 对比测试：同时使用两种解析器解析同一内容
     */
    @Test
    public void testCompareParserResults() {
        System.out.println("========== 解析器对比测试 ==========");

        String markdownContent = """
                # Redis 缓存

                ## 缓存穿透
                缓存穿透是指查询一个一定不存在的数据。

                ### 解决方案
                1. 布隆过滤器
                2. 缓存空值

                ## 缓存击穿
                缓存击穿是指一个热点 key 在某个时间点过期。

                ### 解决方案
                使用互斥锁或逻辑过期。

                ## 缓存雪崩
                大量 key 同时过期导致的问题。
                """;

        // 1. LangChain4j MarkdownDocumentParser
        System.out.println("【1. LangChain4j MarkdownDocumentParser】");
        MarkdownDocumentParser mdParser = new MarkdownDocumentParser();
        InputStream is1 = new ByteArrayInputStream(markdownContent.getBytes(StandardCharsets.UTF_8));
        Document doc1 = mdParser.parse(is1);
        System.out.println("解析结果:");
        System.out.println(doc1.text());
        System.out.println();

        // 2. LangChain4j TextDocumentParser
        System.out.println("【2. LangChain4j TextDocumentParser】");
        TextDocumentParser textParser = new TextDocumentParser();
        InputStream is2 = new ByteArrayInputStream(markdownContent.getBytes(StandardCharsets.UTF_8));
        Document doc2 = textParser.parse(is2);
        System.out.println("解析结果:");
        System.out.println(doc2.text());
        System.out.println();

        // 3. 自定义 MarkdownParser
        System.out.println("【3. 自定义 MarkdownParser】");
        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.buildDocumentStructure(markdownContent, "Redis 缓存", 2);
        MyNode rootNode = result.getKey();
        Map<String, MyNode> nodesDict = result.getValue();

        System.out.println("解析结果（树结构）:");
        MarkdownParser.printDocumentStructure(rootNode, nodesDict, 0);
        System.out.println();

        // 4. 获取叶子节点内容
        System.out.println("【4. 自定义 MarkdownParser 叶子节点】");
        Map<String, MyNode> leafNodes = MarkdownProcessor.findLeafNodes(nodesDict);
        for (MyNode leaf : leafNodes.values()) {
            System.out.println("标题: " + leaf.getTitle());
            System.out.println("内容: " + leaf.getPageContent());
            System.out.println("---");
        }
    }

    /**
     * 测试 DocumentParseUtils 对 Markdown 的处理
     */
    @Test
    public void testDocumentParseUtils_Markdown() throws Exception {
        System.out.println("========== DocumentParseUtils 解析 Markdown ==========");

        String markdownContent = """
                # 标题一
                内容一

                ## 标题二
                内容二

                ### 标题三
                内容三
                """;

        InputStream inputStream = new ByteArrayInputStream(markdownContent.getBytes(StandardCharsets.UTF_8));
        String result = DocumentParseUtils.parse(inputStream, "test.md", "text/markdown");

        System.out.println("【原始内容】:");
        System.out.println(markdownContent);
        System.out.println();
        System.out.println("【DocumentParseUtils.parse() 结果】:");
        System.out.println(result);
        System.out.println();

        // 检查是否保留了标题结构
        boolean hasHashTitles = result.contains("# ");
        System.out.println("是否保留 # 标题结构: " + hasHashTitles);
    }
}
