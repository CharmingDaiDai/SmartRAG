package com.mtmn.smartrag.utils;

import com.mtmn.smartrag.common.MyNode;
import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown解析器，根据标题级别构建文档树状结构
 *
 * @author charmingdaidai
 */
public class MarkdownParser {

    /**
     * 解析Markdown文本，构建层级结构
     *
     * @param markdownText  Markdown格式的文本内容
     * @param documentTitle 文档标题，作为根节点
     * @param maxLevel      最大解析的标题层级，超过此层级的内容作为上级标题内容处理，设置为null则不限制
     * @return 返回根节点和所有节点的字典映射 (rootNode, {节点ID: 节点对象})
     */
    public static Map.Entry<MyNode, Map<String, MyNode>> parseMarkdownStructure(
            String markdownText, String documentTitle, Integer maxLevel) {

        // 创建根节点
        MyNode rootNode = new MyNode("", 0, documentTitle);

        // 存储所有节点的字典，键为节点ID
        Map<String, MyNode> nodesDict = new LinkedHashMap<>();
        nodesDict.put(rootNode.getId(), rootNode);

        // 按行分割Markdown文本
        String[] lines = markdownText.split("\\n", -1);

        // 标记是否在 fenced code block 内
        boolean inFencedCodeBlock = false;
        char fencedMarkerChar = '\0';
        int fencedMarkerLength = 0;

        // 阶段1: 收集所有标题和内容，但不建立层级关系
        List<HeaderInfo> allHeaders = new ArrayList<>();
        List<String> currentContent = new ArrayList<>();
        HeaderInfo currentHeader = null;

        // 先处理根节点的内容
        List<String> rootContent = new ArrayList<>();

        for (String line : lines) {
            FenceMarker fenceMarker = parseFenceMarker(line);

            // fenced code block 边界判定
            if (inFencedCodeBlock) {
                if (fenceMarker != null
                        && fenceMarker.getMarkerChar() == fencedMarkerChar
                        && fenceMarker.getMarkerLength() >= fencedMarkerLength
                        && fenceMarker.getTrailing().trim().isEmpty()) {
                    inFencedCodeBlock = false;
                    fencedMarkerChar = '\0';
                    fencedMarkerLength = 0;
                }

                appendLineToCurrentContent(currentHeader, currentContent, rootContent, line);
                continue;
            }

            if (fenceMarker != null) {
                inFencedCodeBlock = true;
                fencedMarkerChar = fenceMarker.getMarkerChar();
                fencedMarkerLength = fenceMarker.getMarkerLength();

                appendLineToCurrentContent(currentHeader, currentContent, rootContent, line);
                continue;
            }

            // 缩进代码块（4空格/Tab）
            if (line.startsWith("    ") || line.startsWith("\t")) {
                appendLineToCurrentContent(currentHeader, currentContent, rootContent, line);
                continue;
            }

            // 处理 ATX 标题（支持最多 3 个前导空格）
            HeaderInfo parsedHeader = parseAtxHeader(line);

            if (parsedHeader != null) {
                int level = parsedHeader.getLevel();

                // 检查是否超过最大层级
                if (maxLevel != null && level > maxLevel) {
                    appendLineToCurrentContent(currentHeader, currentContent, rootContent, line);
                    continue;
                }

                // 如果遇到新标题，先保存之前的内容
                if (currentHeader != null) {
                    String contentText = String.join("\n", currentContent).trim();
                    currentHeader.setContent(contentText);
                    currentHeader.setOriginalLevel(currentHeader.getLevel());
                    allHeaders.add(currentHeader);
                    currentContent = new ArrayList<>();
                }

                currentHeader = parsedHeader;
            } else {
                appendLineToCurrentContent(currentHeader, currentContent, rootContent, line);
            }
        }

        // 处理最后一个标题的内容
        if (currentHeader != null) {
            String contentText = String.join("\n", currentContent).trim();
            currentHeader.setContent(contentText);
            currentHeader.setOriginalLevel(currentHeader.getLevel());
            allHeaders.add(currentHeader);
        }

        // 设置根节点内容
        rootNode.setPageContent(String.join("\n", rootContent).trim());

        // 阶段2: 创建所有节点对象并建立父子关系（按顺序 + 层级栈）
        Deque<MyNode> parentStack = new ArrayDeque<>();

        for (HeaderInfo header : allHeaders) {
            int level = header.getLevel();
            String title = header.getTitle();
            String blockNumber = header.getBlockNumber();
            String content = header.getContent();
            int originalLevel = header.getOriginalLevel();

            // 创建新节点
            MyNode newNode = new MyNode(content, level, title, blockNumber);

            // 将原始级别保存到元数据中
            newNode.getMetadata().put("original_level", originalLevel);

            nodesDict.put(newNode.getId(), newNode);

            // 栈顶始终维护最近的更高层标题
            while (!parentStack.isEmpty() && parentStack.peek().getLevel() >= level) {
                parentStack.pop();
            }

            MyNode parent = parentStack.isEmpty() ? rootNode : parentStack.peek();
            newNode.setParentId(parent.getId());
            parent.addChild(newNode.getId());

            parentStack.push(newNode);
        }

        return new AbstractMap.SimpleEntry<>(rootNode, nodesDict);
    }

    /**
     * 构建文档的层级结构
     *
     * @param markdownText  Markdown文本内容
     * @param documentTitle 文档标题
     * @param maxLevel      最大解析的标题层级，超过此层级的内容作为上级标题内容处理，设置为null则不限制
     * @return 根节点和所有节点的字典
     */
    public static Map.Entry<MyNode, Map<String, MyNode>> buildDocumentStructure(
            String markdownText, String documentTitle, Integer maxLevel) {

        try {
            return parseMarkdownStructure(markdownText, documentTitle, maxLevel);
        } catch (Exception e) {
            System.err.println("解析文档结构时出错: " + e.getMessage());
            // 创建一个基本的根节点作为后备
            // 将全部内容放入根节点
            MyNode rootNode = new MyNode(
                    markdownText,
                    0,
                    documentTitle
            );
            Map<String, MyNode> nodesDict = new LinkedHashMap<>();
            nodesDict.put(rootNode.getId(), rootNode);
            return new AbstractMap.SimpleEntry<>(rootNode, nodesDict);
        }
    }

    private static void appendLineToCurrentContent(HeaderInfo currentHeader,
                                                   List<String> currentContent,
                                                   List<String> rootContent,
                                                   String line) {
        if (currentHeader != null) {
            currentContent.add(line);
        } else {
            rootContent.add(line);
        }
    }

    /**
     * 解析 fenced code block 标记。
     *
     * 规则：最多 3 个前导空格，随后是 >=3 个连续的 ` 或 ~。
     */
    private static FenceMarker parseFenceMarker(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        int leadingSpaces = countLeadingSpaces(line);
        if (leadingSpaces > 3 || leadingSpaces >= line.length()) {
            return null;
        }

        char markerChar = line.charAt(leadingSpaces);
        if (markerChar != '`' && markerChar != '~') {
            return null;
        }

        int i = leadingSpaces;
        while (i < line.length() && line.charAt(i) == markerChar) {
            i++;
        }

        int markerLength = i - leadingSpaces;
        if (markerLength < 3) {
            return null;
        }

        String trailing = line.substring(i);
        return new FenceMarker(markerChar, markerLength, trailing);
    }

    /**
     * 仅解析 ATX 标题（# 风格），不解析 Setext 标题。
     */
    private static HeaderInfo parseAtxHeader(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        int leadingSpaces = countLeadingSpaces(line);
        if (leadingSpaces > 3 || leadingSpaces >= line.length()) {
            return null;
        }

        int i = leadingSpaces;
        while (i < line.length() && line.charAt(i) == '#') {
            i++;
        }

        int level = i - leadingSpaces;
        if (level <= 0) {
            return null;
        }

        // CommonMark: 超过 6 个 # 不作为标题
        if (level > 6) {
            return null;
        }

        // # 后必须是空白或行尾
        if (i < line.length() && !Character.isWhitespace(line.charAt(i))) {
            return null;
        }

        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }

        String title = i < line.length() ? line.substring(i).trim() : "";
        // 移除可选的尾部 #（ATX closing sequence）
        title = title.replaceFirst("\\s+#+\\s*$", "").trim();

        return new HeaderInfo(level, title, "");
    }

    private static int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    /**
     * 打印文档结构，确保同级标题缩进一致且按正确数值顺序排列
     *
     * @param rootNode   根节点
     * @param nodesDict  所有节点的字典
     * @param baseIndent 基础缩进级别
     */
    public static void printDocumentStructure(MyNode rootNode, Map<String, MyNode> nodesDict, int baseIndent) {
        // 根据节点级别确定实际缩进
        int actualIndent = baseIndent + rootNode.getLevel();
        StringBuilder prefix = new StringBuilder();
        prefix.append("  ".repeat(Math.max(0, actualIndent)));

        // 获取标题原始级别（如果有）
        int originalLevel = rootNode.getMetadata().containsKey("original_level") ?
                (int) rootNode.getMetadata().get("original_level") : rootNode.getLevel();

        // 显示块编号（如果有）
        String blockInfo = rootNode.getBlockNumber() != null && !rootNode.getBlockNumber().isEmpty() ?
                " [块 " + rootNode.getBlockNumber() + "]" : "";

        System.out.println(prefix + "- " + rootNode.getTitle() + blockInfo +
                " (级别: " + rootNode.getLevel() + ", 原始级别: " + originalLevel + ")");

        // 显示内容摘要（最多100个字符）
        String contentPreview = rootNode.getPageContent().length() > 100 ?
                rootNode.getPageContent().substring(0, 100) + "..." : rootNode.getPageContent();
        if (!contentPreview.isEmpty()) {
            System.out.println(prefix + "  内容: " + contentPreview);
        }

        // 获取所有子节点
        List<MyNode> childNodes = new ArrayList<>();
        for (String childId : rootNode.getChildren()) {
            childNodes.add(nodesDict.get(childId));
        }

        // 对子节点进行排序
        childNodes.sort((node1, node2) -> {
            // 先按级别排序
            int levelCompare = Integer.compare(node1.getLevel(), node2.getLevel());
            if (levelCompare != 0) {
                return levelCompare;
            }

            // 尝试从标题中提取数字前缀
            String title1 = node1.getTitle();
            String title2 = node2.getTitle();

            return naturalCompare(title1, title2);
        });

        // 递归打印子节点
        for (MyNode childNode : childNodes) {
            printDocumentStructure(childNode, nodesDict, baseIndent);
        }
    }

    /**
     * 自然排序比较两个字符串
     */
    private static int naturalCompare(String s1, String s2) {
        Pattern numberPattern = Pattern.compile("^(\\d+(?:\\.\\d+)*)");
        Matcher m1 = numberPattern.matcher(s1);
        Matcher m2 = numberPattern.matcher(s2);

        boolean hasNumber1 = m1.find();
        boolean hasNumber2 = m2.find();

        // 如果两者都有数字前缀
        if (hasNumber1 && hasNumber2) {
            return compareNumbers(m1.group(1), m2.group(1));
        }

        // 如果只有一个有数字前缀，有数字的优先
        if (hasNumber1) {
            return -1;
        }
        if (hasNumber2) {
            return 1;
        }

        // 都没有数字前缀，按字母顺序
        return s1.compareTo(s2);
    }

    /**
     * 比较两个数字字符串
     */
    private static int compareNumbers(String num1, String num2) {
        // 将数字字符串拆分为数字部分
        String[] parts1 = num1.split("\\.");
        String[] parts2 = num2.split("\\.");

        // 逐部分比较
        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            try {
                int n1 = Integer.parseInt(parts1[i]);
                int n2 = Integer.parseInt(parts2[i]);
                int comp = Integer.compare(n1, n2);
                if (comp != 0) {
                    return comp;
                }
            } catch (NumberFormatException e) {
                // 如果解析失败，则按字符串比较
                int comp = parts1[i].compareTo(parts2[i]);
                if (comp != 0) {
                    return comp;
                }
            }
        }

        // 如果公共部分相同，则比较长度
        return Integer.compare(parts1.length, parts2.length);
    }

    /**
     * 辅助类用于存储标题信息
     */
    @Data
    private static class HeaderInfo {
        private int level;
        private String title;
        private String blockNumber;
        private String content;
        private int originalLevel;

        public HeaderInfo(int level, String title, String blockNumber) {
            this.level = level;
            this.title = title;
            this.blockNumber = blockNumber;
        }
    }

    @Data
    private static class FenceMarker {
        private final char markerChar;
        private final int markerLength;
        private final String trailing;
    }
}
