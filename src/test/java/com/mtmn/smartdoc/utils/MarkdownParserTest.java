package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.MyNode;

import java.util.Map;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description TODO
 * @date 2025/5/22 10:40
 */
public class MarkdownParserTest {
    public static void main(String[] args) {
        String markdown = """
                # 1. 引言
                这是引言部分的内容。
                
                ## 1.1 背景
                这是背景部分的内容。
                
                # 2. 主要内容
                这是主要内容部分。
                
                ## 2.1 小节一
                小节一的内容。
                
                ### 2.1.1 更深一层
                更深一层的内容。
                
                ## 2.2 小节二
                小节二的内容。
                """;

        // 解析Markdown文档
        Map.Entry<MyNode, Map<String, MyNode>> result =
                MarkdownParser.buildDocumentStructure(markdown, "示例文档", null);

        MyNode rootNode = result.getKey();
        Map<String, MyNode> nodesDict = result.getValue();

        // 打印文档结构
        MarkdownParser.printDocumentStructure(rootNode, nodesDict, 0);
    }
}