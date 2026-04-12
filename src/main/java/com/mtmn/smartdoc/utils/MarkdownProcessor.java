package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.MyNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description Markdown文档处理工具类，提供文档解析、分块、知识点提取等功能
 * @date 2025/5/23 10:36
 */
@Slf4j
public class MarkdownProcessor {
    // 全局参数配置
    // 最大解析的标题层级
    private static final int DEFAULT_MAX_TITLE_LEVEL = 3;
    // 最大块大小(字符数)
    private static final int DEFAULT_MAX_BLOCK_SIZE = 2048;

    /**
     * 将长文本内容分割成不超过最大大小的小块，保持句子完整性
     *
     * @param content 原始文本内容
     * @param maxSize 每个块的最大大小(字符数)
     * @return 分割后的内容块列表
     */
    public static List<String> splitContentIntoChunks(String content, int maxSize) {
        if (content == null || content.isEmpty() || content.length() <= maxSize) {
            return content == null || content.isEmpty() ?
                    Collections.emptyList() : Collections.singletonList(content);
        }

        // 定义句子结束的标点符号
        char[] sentenceEndings = {'.', '!', '?', '。', '！', '？', ';', '；'};
        String paragraphBreak = "\n\n";

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        StringBuilder currentSentence = new StringBuilder();

        // 按字符遍历内容
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            currentSentence.append(ch);

            // 检查是否句子结束 检查是否是段落结束
            boolean isSentenceEnd = i + 1 < content.length() &&
                    content.substring(i, i + 2).equals(paragraphBreak);

            // 检查是否是句子结束标点
            for (char ending : sentenceEndings) {
                if (ch == ending) {
                    isSentenceEnd = true;
                    break;
                }
            }

            // 如果句子结束，检查加入该句子后是否超过大小限制
            if (isSentenceEnd) {
                if (currentChunk.length() + currentSentence.length() <= maxSize) {
                    currentChunk.append(currentSentence);
                } else {
                    // 如果加上这个句子会超过大小限制，先保存当前块，再开始新块
                    if (!currentChunk.isEmpty()) {
                        chunks.add(currentChunk.toString());
                    }
                    currentChunk = new StringBuilder(currentSentence);
                }
                currentSentence = new StringBuilder();
            }
        }

        // 处理最后一个句子和块
        if (!currentSentence.isEmpty()) {
            if (currentChunk.length() + currentSentence.length() <= maxSize) {
                currentChunk.append(currentSentence);
            } else {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder(currentSentence);
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * 解析Markdown文件，构建文档的层级结构，并处理内容分块
     *
     * @param markdownText  Markdown文本内容
     * @param documentTitle 文档标题
     * @param maxLevel      最大解析的标题层级，超过此层级的内容作为一个整体
     * @param maxBlockSize  最大块大小(字符数)
     * @return 包含根节点和所有节点字典的Map.Entry
     */
    public static Map.Entry<MyNode, Map<String, MyNode>> parseMarkdownContent(
            String markdownText, String documentTitle, Integer maxLevel, Integer maxBlockSize) {
        try {
            // 解析文档结构，限制最大层级
            Map.Entry<MyNode, Map<String, MyNode>> result =
                    MarkdownParser.parseMarkdownStructure(markdownText, documentTitle, maxLevel);

            MyNode rootNode = result.getKey();
            Map<String, MyNode> nodesDict = result.getValue();

            // 处理内容超过最大块大小的节点
            List<MyNode> nodesToSplit = new ArrayList<>();
            for (MyNode node : nodesDict.values()) {
                if (node.getPageContent() != null &&
                        node.getPageContent().length() > maxBlockSize) {
                    nodesToSplit.add(node);
                }
            }

            // 对每个需要分割的节点进行处理
            for (MyNode node : nodesToSplit) {
                List<String> contentChunks = splitContentIntoChunks(node.getPageContent(), maxBlockSize);

                // 只有在确实需要分割时才处理（至少有2个块）
                if (contentChunks.size() > 1) {
                    // 清空原节点内容，它将作为父节点
                    String originalContent = node.getPageContent();
                    node.setPageContent("");

                    // 为每个块创建子节点
                    for (int i = 0; i < contentChunks.size(); i++) {
                        String blockNumber = String.format("%d/%d", i + 1, contentChunks.size());
                        MyNode childNode = new MyNode(
                                contentChunks.get(i),
                                node.getLevel(),
                                String.format("%s (%s)", node.getTitle(), blockNumber),
                                blockNumber
                        );
                        childNode.setParentId(node.getId());
                        childNode.getMetadata().put("chunk_child", true);
                        childNode.getMetadata().put("original_level", node.getLevel());

                        // 将子节点加入节点字典
                        nodesDict.put(childNode.getId(), childNode);

                        // 在父节点中添加子节点ID
                        node.addChild(childNode.getId());
                    }

                    // 在父节点元数据中标记它已被分块
                    node.getMetadata().put("chunked", true);
                    node.getMetadata().put("original_length", originalContent.length());
                    node.getMetadata().put("chunks_count", contentChunks.size());
                }
            }

            return new AbstractMap.SimpleEntry<>(rootNode, nodesDict);
        } catch (Exception e) {
            log.error("解析文档结构时出错: {}", e.getMessage());

            // 创建一个基本的根节点作为后备
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

    /**
     * 为每个节点构建完整的标题路径
     *
     * @param rootNode    当前处理的节点
     * @param nodesDict   所有节点的字典
     * @param currentPath 当前路径（上级标题路径）
     */
    public static void buildTitlePaths(MyNode rootNode, Map<String, MyNode> nodesDict, String currentPath) {
        // 构建当前节点的完整路径
        String path = currentPath.isEmpty() ?
                rootNode.getTitle() :
                String.format("%s -> %s", currentPath, rootNode.getTitle());

        // 设置节点的完整标题路径
        // 保留根节点原始标题
        if (!currentPath.isEmpty()) {
            rootNode.setTitle(path);
        }

        // 对所有子节点递归处理
        for (String childId : rootNode.getChildren()) {
            if (nodesDict.containsKey(childId)) {
                buildTitlePaths(nodesDict.get(childId), nodesDict, rootNode.getTitle());
            }
        }
    }

    /**
     * 为每个节点生成知识点摘要（占位实现）
     *
     * @param nodesDict 所有节点的字典
     */
    public static void generateKnowledgeSummaries(Map<String, MyNode> nodesDict) {
        log.info("开始生成知识点摘要...");

        for (MyNode node : nodesDict.values()) {
            // 跳过空内容节点
            if (node.getPageContent() == null || node.getPageContent().trim().isEmpty()) {
                continue;
            }

            // 占位实现 - 实际应用中需要替换为调用大语言模型的代码
            String summary = "这里是节点 [" + node.getTitle() + "] 的知识点摘要";

            // 设置知识摘要到节点
            node.getMetadata().put("knowledge_summary", summary);
        }

        log.info("知识点摘要生成完成");
    }

    /**
     * 将子节点的知识点和摘要整合到父节点（占位实现）
     *
     * @param rootNode  根节点
     * @param nodesDict 所有节点的字典
     */
    public static void propagateKnowledgeToParents(MyNode rootNode, Map<String, MyNode> nodesDict) {
        log.info("开始知识点向上传递...");

        // 构建一个映射，从每个节点ID到其父节点ID
        Map<String, String> parentMap = new HashMap<>();
        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            MyNode node = entry.getValue();
            if (node.getParentId() != null) {
                parentMap.put(node.getId(), node.getParentId());
            }
        }

        // 构建一个按层级的节点列表，从叶子节点到根节点排序
        Map<Integer, List<String>> levelToNodes = new HashMap<>();
        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            String nodeId = entry.getKey();
            MyNode node = entry.getValue();

            if (!levelToNodes.containsKey(node.getLevel())) {
                levelToNodes.put(node.getLevel(), new ArrayList<>());
            }
            levelToNodes.get(node.getLevel()).add(nodeId);
        }

        // 从最深层级开始向上处理
        List<Integer> sortedLevels = new ArrayList<>(levelToNodes.keySet());
        sortedLevels.sort(Collections.reverseOrder());

        for (int level : sortedLevels) {
            if (level == 0) {
                continue;
            }

            for (String nodeId : levelToNodes.get(level)) {
                MyNode node = nodesDict.get(nodeId);

                // 如果节点有父节点，将其知识点传递给父节点
                if (node.getParentId() != null && nodesDict.containsKey(node.getParentId())) {
                    MyNode parentNode = nodesDict.get(node.getParentId());

                    // 如果子节点没有知识摘要，跳过
                    if (!node.getMetadata().containsKey("knowledge_summary")) {
                        continue;
                    }

                    // 将子节点的知识汇总到父节点
                    if (!parentNode.getMetadata().containsKey("child_knowledge")) {
                        parentNode.getMetadata().put("child_knowledge", new ArrayList<>());
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> childKnowledge =
                            (List<Map<String, String>>) parentNode.getMetadata().get("child_knowledge");

                    Map<String, String> knowledgeItem = new HashMap<>();
                    knowledgeItem.put("title", node.getTitle());
                    knowledgeItem.put("summary", (String) node.getMetadata().get("knowledge_summary"));

                    childKnowledge.add(knowledgeItem);
                }
            }
        }

        // 整合父节点的子节点知识
        // 移除最后一个（最深）层级
        sortedLevels.remove(sortedLevels.size() - 1);

        for (int level : sortedLevels) {
            if (!levelToNodes.containsKey(level)) {
                continue;
            }

            for (String nodeId : levelToNodes.get(level)) {
                MyNode node = nodesDict.get(nodeId);

                // 如果节点有子节点知识摘要
                if (node.getMetadata().containsKey("child_knowledge")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> childKnowledge =
                            (List<Map<String, String>>) node.getMetadata().get("child_knowledge");

                    if (childKnowledge == null || childKnowledge.isEmpty()) {
                        continue;
                    }

                    // 占位实现 - 实际应用中需要替换为调用大语言模型的代码
                    String integratedKnowledge = "这里是父节点 [" + node.getTitle() + "] 整合的知识点摘要";

                    // 如果节点已有知识摘要，则进行融合而不是替换
                    if (node.getMetadata().containsKey("knowledge_summary")) {
                        String existingSummary = (String) node.getMetadata().get("knowledge_summary");
                        node.getMetadata().put("knowledge_summary",
                                existingSummary + "\n\n【子节点综合】\n" + integratedKnowledge);
                    } else {
                        node.getMetadata().put("knowledge_summary", integratedKnowledge);
                    }
                }
            }
        }

        log.info("知识点向上传递完成");
    }

    /**
     * 将节点保存到本地文件
     *
     * @param nodesDict  所有节点的字典
     * @param outputFile 输出文件路径
     * @throws IOException 如果保存失败
     */
    public static void saveNodesToFile(Map<String, MyNode> nodesDict, String outputFile) throws IOException {
        // 确保输出目录存在
        File outputDir = new File(outputFile).getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 将节点转换为字典
        Map<String, Object> nodesData = new HashMap<>();
        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            nodesData.put(entry.getKey(), nodeToMap(entry.getValue()));
        }

        // 保存为JSON文件
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            // 简单的JSON序列化实现 - 实际应用应使用Jackson或Gson等库
            writer.write("{\n");
            int nodeCount = nodesData.size();
            int index = 0;

            for (Map.Entry<String, Object> entry : nodesData.entrySet()) {
                writer.write("  \"" + entry.getKey() + "\": " + mapToJson(entry.getValue()));
                if (index < nodeCount - 1) {
                    writer.write(",");
                }
                writer.write("\n");
                index++;
            }

            writer.write("}");
        }
    }

    /**
     * 将节点转换为Map（用于JSON序列化）
     */
    private static Map<String, Object> nodeToMap(MyNode node) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", node.getId());
        map.put("pageContent", node.getPageContent());
        map.put("level", node.getLevel());
        map.put("title", node.getTitle());
        map.put("parentId", node.getParentId());
        map.put("children", node.getChildren());
        map.put("blockNumber", node.getBlockNumber());
        map.put("metadata", node.getMetadata());
        return map;
    }

    /**
     * 简单的Map转JSON字符串（实际应用应使用成熟的JSON库）
     */
    private static String mapToJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            StringBuilder sb = new StringBuilder();
            sb.append("{");

            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append(mapToJson(entry.getValue()));
            }

            sb.append("}");
            return sb.toString();
        } else if (obj instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            boolean first = true;
            for (Object item : (Collection<?>) obj) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(mapToJson(item));
            }

            sb.append("]");
            return sb.toString();
        } else if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else {
            return "\"" + escapeJson(obj.toString()) + "\"";
        }
    }

    /**
     * 转义JSON字符串
     */
    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch);
            }
        }

        return sb.toString();
    }

    /**
     * 处理单个Markdown文件
     *
     * @param filePath          Markdown文件路径
     * @param outputFile        输出文件路径
     * @param maxLevel          最大解析的标题层级
     * @param maxBlockSize      最大块大小(字符数)
     * @param joinTitles        是否进行多级标题拼接
     * @param generateSummaries 是否生成知识点和摘要
     * @return 解析结果，包含根节点和所有节点字典
     */
    public static Map.Entry<MyNode, Map<String, MyNode>> processMarkdownFile(
            String filePath,
            String outputFile,
            int maxLevel,
            int maxBlockSize,
            boolean joinTitles,
            boolean generateSummaries) {

        try {
            log.info("开始处理文件: {}", filePath);

            // 1. 从文件读取Markdown内容
            String markdownText = Files.readString(Paths.get(filePath));

            // 获取文件名（不含后缀）
            String fileName = new File(filePath).getName().replaceFirst("\\.md$", "");

            // 2. 解析Markdown文件，指定最大层级和块大小
            Map.Entry<MyNode, Map<String, MyNode>> result =
                    parseMarkdownContent(markdownText, fileName, maxLevel, maxBlockSize);

            MyNode rootNode = result.getKey();
            Map<String, MyNode> nodesDict = result.getValue();

            log.info("文件解析完成，{} 共 {} 个节点", fileName, nodesDict.size());

            // 3. 多级标题拼接
            if (joinTitles) {
                buildTitlePaths(rootNode, nodesDict, "");
                log.info("多级标题拼接完成");
            }

            // 4. 知识点和摘要生成与汇总
            if (generateSummaries) {
                generateKnowledgeSummaries(nodesDict);
                log.info("{} 知识点摘要生成完成", fileName);

                propagateKnowledgeToParents(rootNode, nodesDict);
                log.info("{} 知识点向上传递完成", fileName);
            }

            // 5. 将节点持久化保存
            if (outputFile != null) {
                saveNodesToFile(nodesDict, outputFile);
                log.info("{} 处理完成，结果保存到: {}", fileName, outputFile);
            }

            return result;
        } catch (IOException e) {
            log.error("处理文件 {} 时出错: {}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     * 处理单个Markdown文件（重载方法，使用默认配置）
     *
     * @param filePath   Markdown文件路径
     * @param outputFile 输出文件路径
     * @return 解析结果，包含根节点和所有节点字典
     */
    public static Map.Entry<MyNode, Map<String, MyNode>> processMarkdownFile(
            String filePath, String outputFile) {
        return processMarkdownFile(
                filePath, outputFile,
                DEFAULT_MAX_TITLE_LEVEL, DEFAULT_MAX_BLOCK_SIZE,
                true, false
        );
    }

    /**
     * 处理单个Markdown文件（重载方法，使用输入流）
     *
     * @param inputStream       Markdown内容输入流
     * @param documentTitle     文档标题
     * @param outputFile        输出文件路径
     * @param maxLevel          最大解析的标题层级
     * @param maxBlockSize      最大块大小(字符数)
     * @param joinTitles        是否进行多级标题拼接
     * @param generateSummaries 是否生成知识点和摘要
     * @return 解析结果，包含根节点和所有节点字典
     */
    public static Map.Entry<MyNode, Map<String, MyNode>> processMarkdownFile(
            InputStream inputStream,
            String documentTitle,
            String outputFile,
            int maxLevel,
            int maxBlockSize,
            boolean joinTitles,
            boolean generateSummaries) {

        try {
            log.info("开始处理文档: {}", documentTitle);

            // 1. 从输入流读取Markdown内容
            String markdownText = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // 2. 解析Markdown文档，指定最大层级和块大小
            Map.Entry<MyNode, Map<String, MyNode>> result =
                    parseMarkdownContent(markdownText, documentTitle, maxLevel, maxBlockSize);

            MyNode rootNode = result.getKey();
            Map<String, MyNode> nodesDict = result.getValue();

            log.info("文档解析完成，{} 共 {} 个节点", documentTitle, nodesDict.size());

            // 3. 多级标题拼接
            if (joinTitles) {
                buildTitlePaths(rootNode, nodesDict, "");
                log.info("多级标题拼接完成");
            }

            // 4. 知识点和摘要生成与汇总
            if (generateSummaries) {
                generateKnowledgeSummaries(nodesDict);
                log.info("{} 知识点摘要生成完成", documentTitle);

                propagateKnowledgeToParents(rootNode, nodesDict);
                log.info("{} 知识点向上传递完成", documentTitle);
            }

            // 5. 将节点持久化保存
            if (outputFile != null) {
                saveNodesToFile(nodesDict, outputFile);
                log.info("{} 处理完成，结果保存到: {}", documentTitle, outputFile);
            }

            return result;
        } catch (IOException e) {
            log.error("处理文档 {} 时出错: {}", documentTitle, e.getMessage());
            return null;
        }
    }

    /**
     * 处理目录中的所有Markdown文件
     *
     * @param inputDir          输入目录
     * @param outputDir         输出目录
     * @param maxLevel          最大解析的标题层级
     * @param maxBlockSize      最大块大小(字符数)
     * @param joinTitles        是否进行多级标题拼接
     * @param generateSummaries 是否生成知识点和摘要
     * @param numThreads        线程数量
     */
    public static void processMarkdownFolder(
            String inputDir,
            String outputDir,
            int maxLevel,
            int maxBlockSize,
            boolean joinTitles,
            boolean generateSummaries,
            int numThreads) {

        try {
            // 确保输出目录存在
            Files.createDirectories(Paths.get(outputDir));

            // 获取输入目录中的所有md文件
            List<Path> mdFiles;
            try (Stream<Path> walk = Files.walk(Paths.get(inputDir))) {
                mdFiles = walk
                        .filter(p -> !Files.isDirectory(p))
                        .filter(p -> p.toString().toLowerCase().endsWith(".md"))
                        .toList();
            }

            if (mdFiles.isEmpty()) {
                log.info("在目录 {} 中未找到Markdown文件", inputDir);
                return;
            }

            int totalFiles = mdFiles.size();
            log.info("找到 {} 个Markdown文件待处理", totalFiles);
            log.info("最大标题层级: {}, 最大块大小: {} 字符", maxLevel, maxBlockSize);

            // 创建线程池
            ExecutorService executor = Executors.newFixedThreadPool(
                    Math.min(numThreads, totalFiles));

            // 提交任务到线程池
            List<Future<?>> futures = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(totalFiles);

            for (Path file : mdFiles) {
                String outputFile = Paths.get(outputDir,
                                file.getFileName().toString().replaceFirst("\\.md$", "_processed.json"))
                        .toString();

                futures.add(executor.submit(() -> {
                    try {
                        processMarkdownFile(
                                file.toString(),
                                outputFile,
                                maxLevel,
                                maxBlockSize,
                                joinTitles,
                                generateSummaries);
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            // 等待所有任务完成或显示进度
            new Thread(() -> {
                try {
                    int completed = 0;
                    while (completed < totalFiles) {
                        Thread.sleep(1000); // 每秒更新一次
                        int newCompleted = (int) (totalFiles - latch.getCount());
                        if (newCompleted != completed) {
                            completed = newCompleted;
                            log.info("处理进度: {}/{} ({}%)",
                                    completed, totalFiles, String.format("%.1f", 100.0 * completed / totalFiles));
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // 等待所有任务完成
            latch.await();

            // 关闭线程池
            executor.shutdown();
            log.info("所有文件处理完成！处理结果保存在: {}", outputDir);

        } catch (IOException | InterruptedException e) {
            log.error("处理目录时出错: {}", e.getMessage());
        }
    }

    /**
     * 处理目录中的所有Markdown文件（重载方法，使用默认配置）
     *
     * @param inputDir  输入目录
     * @param outputDir 输出目录
     */
    public static void processMarkdownFolder(String inputDir, String outputDir) {
        processMarkdownFolder(
                inputDir,
                outputDir,
                DEFAULT_MAX_TITLE_LEVEL,
                DEFAULT_MAX_BLOCK_SIZE,
                true,
                false,
                Runtime.getRuntime().availableProcessors()
        );
    }

    /**
     * 获取所有叶子节点（没有子节点的节点）
     *
     * @param nodesDict 所有节点的字典
     * @return 包含所有叶子节点的映射
     */
    public static Map<String, MyNode> findLeafNodes(Map<String, MyNode> nodesDict) {
        Map<String, MyNode> leafNodes = new LinkedHashMap<>();

        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            String nodeId = entry.getKey();
            MyNode node = entry.getValue();

            // 检查节点是否有子节点
            if (node.getChildren() == null || node.getChildren().isEmpty()) {
                leafNodes.put(nodeId, node);
            }
        }

        return leafNodes;
    }
}