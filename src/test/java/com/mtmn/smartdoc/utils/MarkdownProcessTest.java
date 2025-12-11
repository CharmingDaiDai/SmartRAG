package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.MyNode;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description Markdown处理器测试类
 * @date 2025/5/23 10:50
 */
public class MarkdownProcessTest {

    public String mdPath = "/Users/charmingdaidai/Documents/code/smartDoc/src/main/resources/doc_data/Redis.md";
    public String outputPath = "/Users/charmingdaidai/Documents/code/smartDoc/src/main/resources/processed_docs/sample_processed.json";
    public String mdFolder = "/Users/charmingdaidai/Documents/code/smartDoc/src/main/resources/doc_data";
    public String outputFolder = "/Users/charmingdaidai/Documents/code/smartDoc/src/main/resources/processed_docs";

    /**
     * 打印文档节点结构
     *
     * @param rootNode  根节点
     * @param nodesDict 节点字典
     * @param indent    缩进级别
     */
    private void printNodeStructure(MyNode rootNode, Map<String, MyNode> nodesDict, int indent) {
        // 创建缩进
        StringBuilder indentStr = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentStr.append("  ");
        }

        // 打印当前节点
        System.out.println(indentStr + "- " + rootNode.getTitle() +
                (rootNode.getBlockNumber() != null && !rootNode.getBlockNumber().isEmpty() ?
                        " [块 " + rootNode.getBlockNumber() + "]" : "") +
                " (级别: " + rootNode.getLevel() + ")");

        // 如果有内容，打印内容摘要
        if (rootNode.getPageContent() != null && !rootNode.getPageContent().isEmpty()) {
            String contentPreview = rootNode.getPageContent().length() > 50 ?
                    rootNode.getPageContent().substring(0, 50) + "..." : rootNode.getPageContent();
            System.out.println(indentStr + "  内容: " + contentPreview);
        }

        // 递归打印子节点
        for (String childId : rootNode.getChildren()) {
            printNodeStructure(nodesDict.get(childId), nodesDict, indent + 1);
        }
    }

    @Test
    public void testProcessSingleFile() {
        try {
            System.out.println("测试1：处理单个文件");
            String filePath = mdPath;
            String outputFile = outputPath;

            Map.Entry<MyNode, Map<String, MyNode>> result =
                    MarkdownProcessor.processMarkdownFile(
                            filePath,
                            outputFile,
                            3,   // 最大标题层级
                            2048,  // 最大块大小
                            true,  // 进行多级标题拼接
                            false   // 生成知识点和摘要
                    );

            if (result != null) {
                MyNode rootNode = result.getKey();
                Map<String, MyNode> nodesDict = result.getValue();
                System.out.println("解析完成，共 " + nodesDict.size() + " 个节点");
                System.out.println("节点结构：");
                printNodeStructure(rootNode, nodesDict, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessWithInputStream() {
        try {
            System.out.println("\n测试2：使用输入流处理Markdown");
            String filePath = mdPath;

            try (InputStream inputStream = new FileInputStream(filePath)) {
                Map.Entry<MyNode, Map<String, MyNode>> streamResult =
                        MarkdownProcessor.processMarkdownFile(
                                inputStream,
                                "示例文档",
                                "stream_processed.json",
                                3,
                                2048,
                                true,
                                false
                        );

                if (streamResult != null) {
                    MyNode rootNode = streamResult.getKey();
                    Map<String, MyNode> nodesDict = streamResult.getValue();
                    System.out.println("从流解析完成，共 " + nodesDict.size() + " 个节点");
                    System.out.println("节点结构：");
                    printNodeStructure(rootNode, nodesDict, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testProcessFolder() {
        try {
            System.out.println("\n测试3：处理整个目录");
            String inputDir = mdFolder;
            String outputDir = outputFolder;
            String sampleFile = "doc_data/Redis.md"; // 用于演示结构打印的样本文件

            // 处理整个文件夹
            MarkdownProcessor.processMarkdownFolder(
                    inputDir,
                    outputDir,
                    3,     // 最大标题层级
                    2048,  // 最大块大小
                    true,  // 进行多级标题拼接
                    false, // 不生成知识点和摘要
                    4      // 使用4个线程
            );

            // 为了演示打印结构，单独处理一个文件并打印
            Map.Entry<MyNode, Map<String, MyNode>> result =
                    MarkdownProcessor.processMarkdownFile(
                            sampleFile,
                            null,  // 不输出到文件
                            3,
                            2048,
                            true,
                            false
                    );

            if (result != null) {
                MyNode rootNode = result.getKey();
                Map<String, MyNode> nodesDict = result.getValue();
                System.out.println("样本文件解析完成，共 " + nodesDict.size() + " 个节点");
                System.out.println("样本文件节点结构：");
                printNodeStructure(rootNode, nodesDict, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}