//package com.mtmn.smartdoc.service.impl;
//
//import com.mtmn.smartdoc.po.Document;
//import com.mtmn.smartdoc.repository.DocumentRepository;
//import com.mtmn.smartdoc.service.AnalysisService;
//import com.mtmn.smartdoc.service.FileService;
//import com.mtmn.smartdoc.service.LLMService;
//import com.mtmn.smartdoc.vo.KeywordsResult;
//import com.mtmn.smartdoc.vo.PolishResult;
//import com.mtmn.smartdoc.vo.SecurityResult;
//import com.mtmn.smartdoc.vo.SummaryResult;
//import jakarta.annotation.Resource;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.stream.Collectors;
//
/// **
// * @author charmingdaidai
// */
//@Service
//@Slf4j
//public class AnalysisServiceImpl implements AnalysisService {
//
//    /**
//     * 内容块的最大字符数
//     */
//    private static final int MAX_BLOCK_SIZE = 4000;
//
//    @Resource
//    private DocumentRepository documentRepository;
//
//    @Resource
//    private LLMService llmService;
//
//    @Resource
//    private FileService fileService;
//
//    /**
//     * 将长文本内容分割成不超过最大大小的小块，保持句子完整性
//     *
//     * 实现思路：
//     * 1. 检查内容长度，如果不超过最大限制则直接返回
//     * 2. 定义句子结束的标点符号集合
//     * 3. 逐字符遍历内容，构建完整句子
//     * 4. 在句子边界处检查是否超过大小限制
//     * 5. 确保每个块都不超过最大大小且保持句子完整性
//     *
//     * @param content  原始文本内容
//     * @param maxSize  每个块的最大大小(字符数)
//     * @return 分割后的内容块列表
//     */
//    private List<String> splitContentIntoChunks(String content, int maxSize) {
//        if (content == null || content.isEmpty() || content.length() <= maxSize) {
//            List<String> result = new ArrayList<>();
//            if (content != null && !content.isEmpty()) {
//                result.add(content);
//            }
//            return result;
//        }
//
//        // 定义句子结束的标点符号
//        List<Character> sentenceEndings = Arrays.asList('.', '!', '?', '。', '！', '？', ';', '；');
//
//        List<String> chunks = new ArrayList<>();
//        StringBuilder currentChunk = new StringBuilder();
//        StringBuilder currentSentence = new StringBuilder();
//
//        // 按字符遍历内容
//        for (int i = 0; i < content.length(); i++) {
//            char c = content.charAt(i);
//            currentSentence.append(c);
//
//            // 检查是否句子结束  检查是否为段落结束（两个连续地换行）
//            boolean isSentenceEnd = c == '\n' && i + 1 < content.length() && content.charAt(i + 1) == '\n';
//
//            // 检查是否为句子结束标点符号
//            if (!isSentenceEnd && sentenceEndings.contains(c)) {
//                isSentenceEnd = true;
//            }
//
//            // 如果句子结束，检查加入该句子后是否超过大小限制
//            if (isSentenceEnd) {
//                if (currentChunk.length() + currentSentence.length() <= maxSize) {
//                    currentChunk.append(currentSentence);
//                    currentSentence.setLength(0);
//                } else {
//                    // 如果加上这个句子会超过大小限制，先保存当前块，再开始新块
//                    if (!currentChunk.isEmpty()) {
//                        chunks.add(currentChunk.toString());
//                    }
//                    currentChunk = new StringBuilder(currentSentence);
//                    currentSentence.setLength(0);
//                }
//            }
//        }
//
//        // 处理最后一个句子和块
//        if (!currentSentence.isEmpty()) {
//            if (currentChunk.length() + currentSentence.length() <= maxSize) {
//                currentChunk.append(currentSentence);
//            } else {
//                chunks.add(currentChunk.toString());
//                currentChunk = new StringBuilder(currentSentence);
//            }
//        }
//
//        if (!currentChunk.isEmpty()) {
//            chunks.add(currentChunk.toString());
//        }
//
//        return chunks;
//    }
//
//    /**
//     * 重载方法，使用默认最大块大小
//     *
//     * 实现思路：
//     * 1. 调用带参数的splitContentIntoChunks方法
//     * 2. 使用类常量MAX_BLOCK_SIZE作为默认最大块大小
//     * 3. 简化调用接口，提供便捷方法
//     *
//     * @param content 原始文本内容
//     * @return 分割后的内容块列表
//     */
//    private List<String> splitContentIntoChunks(String content) {
//        return splitContentIntoChunks(content, MAX_BLOCK_SIZE);
//    }
//
//    /**
//     * 生成文本摘要
//     *
//     * 实现思路：
//     * 1. 计算原始内容长度用于统计
//     * 2. 将超长内容分割成多个块，避免单次处理过长文本
//     * 3. 使用线程池并行处理多个文本块，提高处理效率
//     * 4. 为每个文本块提交摘要生成任务到线程池
//     * 5. 收集所有任务的执行结果，合并成最终摘要
//     * 6. 正确关闭线程池，避免资源泄露
//     * 7. 构建包含统计信息的摘要结果对象
//     *
//     * @param content 需要生成摘要的文本内容
//     * @return 包含摘要内容和统计信息的结果对象
//     */
//    @Override
//    public SummaryResult generateSummary(String content) {
//        // 计算原始内容长度
//        int originalLength = content != null ? content.length() : 0;
//
//        // 如果长度太长超过MAX_BLOCK_SIZE，则进行拆分
//        List<String> chunks = splitContentIntoChunks(content);
//
//        // 创建一个线程池，数量为5
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        List<Future<String>> futures = new ArrayList<>();
//
//        // 提交摘要生成任务
//        for (String chunk : chunks) {
//            Callable<String> task = () -> llmService.generateSummary(chunk);
//            futures.add(executorService.submit(task));
//        }
//
//        // 收集摘要结果
//        StringBuilder summaryBuilder = new StringBuilder();
//        for (Future<String> future : futures) {
//            try {
//                summaryBuilder.append(future.get()).append(" ");
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("Error generating summary for a chunk", e);
//                // 恢复机制：继续处理下一个chunk
//                continue;
//            }
//        }
//
//        // 关闭线程池
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//        }
//
//        String summary = summaryBuilder.toString().trim();
//
//        return SummaryResult.builder()
//                .summary(summary)
//                .timestamp(System.currentTimeMillis())
//                .originalLength(originalLength)
//                .summaryLength(summary.length())
//                .build();
//    }
//
//    /**
//     * 从数据库文档生成摘要并保存
//     *
//     * 实现思路：
//     * 1. 根据文档ID从数据库查询文档实体
//     * 2. 如果文档不存在则抛出EntityNotFoundException异常
//     * 3. 调用readDocumentContent方法读取文档的实际内容
//     * 4. 调用generateSummary方法生成摘要
//     * 5. 将生成的摘要保存回文档实体的summary字段
//     * 6. 使用事务确保数据一致性
//     * 7. 记录操作日志便于追踪
//     *
//     * @param documentId 文档ID
//     * @return 包含摘要内容和统计信息的结果对象
//     * @throws EntityNotFoundException 当文档不存在时抛出
//     */
//    @Override
//    @Transactional
//    public SummaryResult generateSummaryFromDocument(Long documentId) {
//        // 查找文档
//        Document document = documentRepository.findById(documentId)
//                .orElseThrow(() -> new EntityNotFoundException("文档不存在，ID: " + documentId));
//
//        // 读取文档内容
//        String content = readDocumentContent(document);
//
//        // 生成摘要
//        SummaryResult result = generateSummary(content);
//
//        // 将摘要保存到文档实体
//        document.setSummary(result.getSummary());
//        documentRepository.save(document);
//        log.info("已将摘要保存到文档，ID: {}, 摘要长度: {}", documentId, result.getSummaryLength());
//
//        return result;
//    }
//
//    /**
//     * 提取文本关键词
//     *
//     * 实现思路：
//     * 1. 计算原始内容长度用于统计
//     * 2. 将超长内容分割成多个块，避免单次处理过长文本
//     * 3. 使用线程池并行处理多个文本块，提高处理效率
//     * 4. 为每个文本块提交关键词提取任务到线程池
//     * 5. 使用Set去重，避免重复关键词
//     * 6. 收集所有任务的执行结果，合并成最终关键词列表
//     * 7. 提供容错机制，如果没有提取到关键词则使用默认值
//     * 8. 正确关闭线程池，避免资源泄露
//     *
//     * @param content 需要提取关键词的文本内容
//     * @return 包含关键词列表和统计信息的结果对象
//     */
//    @Override
//    public KeywordsResult extractKeywords(String content) {
//        // 计算原始内容长度
//        int originalLength = content != null ? content.length() : 0;
//
//        // 如果长度太长超过MAX_BLOCK_SIZE，则进行拆分
//        List<String> chunks = splitContentIntoChunks(content);
//
//        // 创建一个线程池，数量为5
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        List<Future<List<String>>> futures = new ArrayList<>();
//
//        // 提交关键词提取任务
//        for (String chunk : chunks) {
//            Callable<List<String>> task = () -> {
//                return llmService.extractKeywords(chunk);
//            };
//            futures.add(executorService.submit(task));
//        }
//
//        // 收集所有关键词结果并去重
//        Set<String> uniqueKeywords = new HashSet<>();
//        for (Future<List<String>> future : futures) {
//            try {
//                List<String> keywords = future.get();
//                if (keywords != null) {
//                    uniqueKeywords.addAll(keywords);
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("Error extracting keywords for a chunk", e);
//                // 恢复机制：继续处理下一个chunk
//                continue;
//            }
//        }
//
//        // 关闭线程池
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//                log.warn("ExecutorService did not terminate in the specified time for keyword extraction");
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            log.error("ExecutorService termination interrupted for keyword extraction", e);
//            Thread.currentThread().interrupt();
//        }
//
//        // 将Set转换为List返回
//        List<String> keywordsList = new ArrayList<>(uniqueKeywords);
//
//        // 如果没有提取到关键词，使用模拟数据
//        if (keywordsList.isEmpty()) {
//            log.warn("No keywords extracted, using mock data");
//            keywordsList = List.of(
//                    "无提取到的关键词"
//            );
//        }
//
//        return KeywordsResult.builder()
//                .keywords(keywordsList)
//                .timestamp(System.currentTimeMillis())
//                .originalLength(originalLength)
//                .build();
//    }
//
//    /**
//     * 从数据库文档提取关键词并保存
//     *
//     * 实现思路：
//     * 1. 根据文档ID从数据库查询文档实体
//     * 2. 如果文档不存在则抛出EntityNotFoundException异常
//     * 3. 调用readDocumentContent方法读取文档的实际内容
//     * 4. 调用extractKeywords方法提取关键词
//     * 5. 将关键词列表用逗号连接成字符串，保存到文档实体的keywords字段
//     * 6. 使用事务确保数据一致性
//     * 7. 记录操作日志便于追踪
//     *
//     * @param documentId 文档ID
//     * @return 包含关键词列表和统计信息的结果对象
//     * @throws EntityNotFoundException 当文档不存在时抛出
//     */
//    @Override
//    @Transactional
//    public KeywordsResult extractKeywordsFromDocument(Long documentId) {
//        // 查找文档
//        Document document = documentRepository.findById(documentId)
//                .orElseThrow(() -> new EntityNotFoundException("文档不存在，ID: " + documentId));
//
//        // 读取文档内容
//        String content = readDocumentContent(document);
//
//        // 提取关键词
//        KeywordsResult result = extractKeywords(content);
//
//        // 将关键词保存到文档实体
//        document.setKeywords(String.join(",", result.getKeywords()));
//        documentRepository.save(document);
//        log.info("已将关键词保存到文档，ID: {}, 关键词数量: {}", documentId, result.getKeywords().size());
//
//        return result;
//    }
//
//    /**
//     * 润色文档内容
//     *
//     * 实现思路：
//     * 1. 计算原始内容长度用于统计
//     * 2. 验证润色类型参数，如果为空则使用默认的"formal"类型
//     * 3. 将超长内容分割成多个块，避免单次处理过长文本
//     * 4. 使用线程池并行处理多个文本块，提高处理效率
//     * 5. 为每个文本块提交润色任务到线程池
//     * 6. 收集所有任务的执行结果，用换行符连接成最终润色内容
//     * 7. 提供容错机制，如果润色失败则使用默认提示
//     * 8. 正确关闭线程池，避免资源泄露
//     *
//     * @param content 需要润色的文本内容
//     * @param polishType 润色类型（如formal、casual等）
//     * @return 包含润色后内容和统计信息的结果对象
//     */
//    @Override
//    public PolishResult polishDocument(String content, String polishType) {
//        // 计算原始内容长度
//        int originalLength = content != null ? content.length() : 0;
//
//        // 默认使用formal润色类型
//        if (polishType == null || polishType.isEmpty()) {
//            polishType = "formal";
//        }
//
//        final String finalPolishType = polishType;
//
//        // 如果长度太长超过MAX_BLOCK_SIZE，则进行拆分
//        List<String> chunks = splitContentIntoChunks(content);
//
//        // 创建一个线程池，数量为5
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        List<Future<String>> futures = new ArrayList<>();
//
//        // 提交润色任务
//        for (String chunk : chunks) {
//            Callable<String> task = () -> {
//                // 实际项目中，这里应该调用文本润色算法或外部API
//                return llmService.polishDocument(chunk, finalPolishType);
//            };
//            futures.add(executorService.submit(task));
//        }
//
//        // 收集润色结果
//        StringBuilder polishedBuilder = new StringBuilder();
//        for (Future<String> future : futures) {
//            try {
//                String result = future.get();
//                if (result != null) {
//                    polishedBuilder.append(result).append("\n\n");
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("Error polishing document chunk with type {}: {}", finalPolishType, e.getMessage(), e);
//                // 恢复机制：继续处理下一个chunk
//                continue;
//            }
//        }
//
//        // 关闭线程池
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//                log.warn("ExecutorService did not terminate in the specified time for document polishing");
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            log.error("ExecutorService termination interrupted for document polishing", e);
//            Thread.currentThread().interrupt();
//        }
//
//        String polishedContent = polishedBuilder.toString().trim();
//
//        // 如果润色结果为空，根据润色类型返回默认内容
//        if (polishedContent.isEmpty()) {
//            log.warn("Polished content is empty, using default content for type: {}", finalPolishType);
//            polishedContent = "润色失败，返回内容为空";
//        }
//
//        return PolishResult.builder()
//                .polishedContent(polishedContent)
//                .polishType(finalPolishType)
//                .timestamp(System.currentTimeMillis())
//                .originalLength(originalLength)
//                .polishedLength(polishedContent.length())
//                .build();
//    }
//
//    /**
//     * 从数据库文档润色内容并保存信息
//     *
//     * 实现思路：
//     * 1. 根据文档ID从数据库查询文档实体
//     * 2. 如果文档不存在则抛出EntityNotFoundException异常
//     * 3. 调用readDocumentContent方法读取文档的实际内容
//     * 4. 调用polishDocument方法进行文档润色
//     * 5. 将润色信息（类型和时间）追加到文档的summary字段
//     * 6. 使用事务确保数据一致性
//     * 7. 记录操作日志便于追踪
//     *
//     * @param documentId 文档ID
//     * @param polishType 润色类型
//     * @return 包含润色后内容和统计信息的结果对象
//     * @throws EntityNotFoundException 当文档不存在时抛出
//     */
//    @Override
//    @Transactional
//    public PolishResult polishDocumentFromDocument(Long documentId, String polishType) {
//        // 查找文档
//        Document document = documentRepository.findById(documentId)
//                .orElseThrow(() -> new EntityNotFoundException("文档不存在，ID: " + documentId));
//
//        // 读取文档内容
//        String content = readDocumentContent(document);
//
//        // 润色文档
//        PolishResult result = polishDocument(content, polishType);
//
//        // 将润色结果保存到文档实体
//        // 注意：这里假设我们将润色后的内容作为新文档的内容来处理
//        // 如果Document实体没有专门用于保存润色结果的字段，可以考虑添加一个
//        document.setSummary(document.getSummary() + "\n\n【润色结果】\n" +
//                "润色类型：" + result.getPolishType() + "\n" +
//                "润色时间：" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(result.getTimestamp())));
//
//        documentRepository.save(document);
//        log.info("已将润色信息保存到文档，ID: {}, 润色类型: {}, 润色后长度: {}",
//                documentId, result.getPolishType(), result.getPolishedLength());
//
//        return result;
//    }
//
//    /**
//     * 检测文本中的敏感信息
//     *
//     * 实现思路：
//     * 1. 计算原始内容长度用于统计
//     * 2. 验证输入内容，如果为空则返回空结果
//     * 3. 判断内容长度，如果超过阈值则调用分块检测方法
//     * 4. 对于正常长度的内容直接调用LLMService进行检测
//     * 5. 处理检测过程中的异常，确保服务稳定性
//     * 6. 构建包含敏感信息列表和统计信息的结果对象
//     *
//     * @param content 需要检测敏感信息的文本内容
//     * @return 包含敏感信息列表和统计信息的结果对象
//     */
//    @Override
//    public SecurityResult detectSensitiveInfo(String content) {
//        // 计算原始内容长度
//        int originalLength = content != null ? content.length() : 0;
//
//        if (content == null || content.isEmpty()) {
//            log.warn("检测敏感信息时收到空内容");
//            return SecurityResult.builder()
//                    .sensitiveInfoList(new ArrayList<>())
//                    .timestamp(System.currentTimeMillis())
//                    .originalLength(0)
//                    .totalCount(0)
//                    .build();
//        }
//
//        // 如果内容太长，分块处理
//        if (originalLength > MAX_BLOCK_SIZE) {
//            log.info("内容过长，分块检测敏感信息，总长度: {}", originalLength);
//            return detectSensitiveInfoInChunks(content);
//        }
//
//        // 调用LLMService进行敏感信息检测
//        List<SecurityResult.SensitiveInfo> sensitiveInfoList;
//        try {
//            sensitiveInfoList = llmService.detectSensitiveInfo(content);
//            log.info("敏感信息检测完成，找到{}条敏感信息", sensitiveInfoList.size());
//        } catch (Exception e) {
//            log.error("检测敏感信息时发生错误: {}", e.getMessage(), e);
//            sensitiveInfoList = new ArrayList<>();
//        }
//
//        return SecurityResult.builder()
//                .sensitiveInfoList(sensitiveInfoList)
//                .timestamp(System.currentTimeMillis())
//                .originalLength(originalLength)
//                .totalCount(sensitiveInfoList.size())
//                .build();
//    }
//
//    /**
//     * 对长内容分块检测敏感信息
//     *
//     * 实现思路：
//     * 1. 将长内容分割成多个块，便于并行处理
//     * 2. 使用线程池并行处理多个文本块，提高检测效率
//     * 3. 为每个文本块计算在原文中的偏移量，确保位置信息准确
//     * 4. 提交敏感信息检测任务到线程池
//     * 5. 调整每个块检测结果中的位置信息，加上相应的偏移量
//     * 6. 收集所有块的检测结果，合并成最终结果
//     * 7. 正确关闭线程池，避免资源泄露
//     *
//     * @param content 需要检测的长内容
//     * @return 合并后的敏感信息检测结果
//     */
//    private SecurityResult detectSensitiveInfoInChunks(String content) {
//        // 分块处理
//        List<String> chunks = splitContentIntoChunks(content);
//        log.info("长内容被分为{}个块进行敏感信息检测", chunks.size());
//
//        // 创建一个线程池
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        List<Future<List<SecurityResult.SensitiveInfo>>> futures = new ArrayList<>();
//
//        // 提交敏感信息检测任务
//        for (int i = 0; i < chunks.size(); i++) {
//            final String chunk = chunks.get(i);
//            final int chunkOffset = i > 0
//                    ? chunks.subList(0, i).stream().mapToInt(String::length).sum()
//                    : 0;
//
//            Callable<List<SecurityResult.SensitiveInfo>> task = () -> {
//                List<SecurityResult.SensitiveInfo> chunkResults = llmService.detectSensitiveInfo(chunk);
//
//                // 调整位置信息，加上之前块的长度偏移
//                if (chunkOffset > 0) {
//                    chunkResults = chunkResults.stream()
//                            .map(info -> {
//                                SecurityResult.Position adjustedPos = new SecurityResult.Position(
//                                        info.getPosition().getStart() + chunkOffset,
//                                        info.getPosition().getEnd() + chunkOffset
//                                );
//                                return SecurityResult.SensitiveInfo.builder()
//                                        .type(info.getType())
//                                        .content(info.getContent())
//                                        .risk(info.getRisk())
//                                        .position(adjustedPos)
//                                        .build();
//                            })
//                            .collect(Collectors.toList());
//                }
//                return chunkResults;
//            };
//
//            futures.add(executorService.submit(task));
//        }
//
//        // 收集所有块的检测结果
//        List<SecurityResult.SensitiveInfo> allSensitiveInfo = new ArrayList<>();
//        for (Future<List<SecurityResult.SensitiveInfo>> future : futures) {
//            try {
//                List<SecurityResult.SensitiveInfo> result = future.get();
//                if (result != null) {
//                    allSensitiveInfo.addAll(result);
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("敏感信息检测任务执行出错: {}", e.getMessage(), e);
//                // 继续处理其他结果
//            }
//        }
//
//        // 关闭线程池
//        executorService.shutdown();
//        try {
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
//
//        return SecurityResult.builder()
//                .sensitiveInfoList(allSensitiveInfo)
//                .timestamp(System.currentTimeMillis())
//                .originalLength(content.length())
//                .totalCount(allSensitiveInfo.size())
//                .build();
//    }
//
//    /**
//     * 从数据库文档检测敏感信息并保存结果
//     *
//     * 实现思路：
//     * 1. 根据文档ID从数据库查询文档实体
//     * 2. 如果文档不存在则抛出EntityNotFoundException异常
//     * 3. 调用readDocumentContent方法读取文档的实际内容
//     * 4. 调用detectSensitiveInfo方法检测敏感信息
//     * 5. 使用Jackson将检测结果序列化为JSON字符串
//     * 6. 将JSON结果保存到文档实体的sensitiveInfo字段
//     * 7. 使用事务确保数据一致性
//     * 8. 处理JSON序列化异常，记录错误日志
//     *
//     * @param documentId 文档ID
//     * @return 包含敏感信息列表和统计信息的结果对象
//     * @throws EntityNotFoundException 当文档不存在时抛出
//     */
//    @Override
//    @Transactional
//    public SecurityResult detectSensitiveInfoFromDocument(Long documentId) {
//        // 查找文档
//        Document document = documentRepository.findById(documentId)
//                .orElseThrow(() -> new EntityNotFoundException("文档不存在，ID: " + documentId));
//
//        // 读取文档内容
//        String content = readDocumentContent(document);
//
//        // 检测敏感信息
//        SecurityResult result = detectSensitiveInfo(content);
//
//        // 将敏感信息检测结果保存到文档实体
//        // 将检测到的敏感信息转换为JSON格式存储
//        try {
//            // 使用jackson或gson等库将对象转换为json
//            String jsonResult = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
//            document.setSensitiveInfo(jsonResult);
//            documentRepository.save(document);
//            log.info("已将敏感信息检测结果保存到文档，ID: {}, 敏感信息数量: {}", documentId, result.getTotalCount());
//        } catch (Exception e) {
//            log.error("将敏感信息检测结果转换为JSON时出错: {}", e.getMessage(), e);
//        }
//
//        return result;
//    }
//
//    /**
//     * 读取文档内容
//     *
//     * 实现思路：
//     * 1. 验证文档对象和文档路径的有效性
//     * 2. 获取文档的文件类型和存储路径信息
//     * 3. 调用FileService的readFileContent方法读取文件内容
//     * 4. FileService会根据文件类型自动选择合适的读取方法
//     * 5. 记录读取操作的成功日志，包含关键统计信息
//     * 6. 捕获并处理读取过程中的异常，返回错误提示
//     *
//     * @param document 文档实体对象，包含文件路径和类型信息
//     * @return 文档的文本内容，如果读取失败则返回错误信息
//     */
//    private String readDocumentContent(Document document) {
//        if (document == null || document.getFilePath() == null) {
//            log.warn("文档或文档路径为空，无法读取内容");
//            return "";
//        }
//
//        try {
//            String fileType = document.getFileType();
//            String filePath = document.getFilePath();
//
//            // 使用FileService读取文件内容，会根据文件类型自动选择合适的读取方法
//            String content = fileService.readFileContent(filePath, fileType);
//
//            log.info("成功读取文档内容，文档ID：{}，文件路径：{}，内容长度：{}",
//                    document.getId(), filePath, content.length());
//
//            return content;
//        } catch (Exception e) {
//            log.error("读取文档内容出错，文档ID：{}，错误：{}",
//                    document.getId(), e.getMessage(), e);
//            return "读取文档内容出错：" + e.getMessage();
//        }
//    }
//}