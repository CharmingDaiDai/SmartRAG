package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.enums.IndexingTaskStatus;
import com.mtmn.smartdoc.enums.IndexingTaskType;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.po.IndexingTask;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.repository.IndexingTaskRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.IndexingProgressCallback;
import com.mtmn.smartdoc.service.IndexingProgressEmitterManager;
import com.mtmn.smartdoc.service.IndexingService;
import com.mtmn.smartdoc.service.IndexingTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 索引任务服务实现
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingTaskServiceImpl implements IndexingTaskService {

    private final IndexingTaskRepository indexingTaskRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final IndexingService indexingService;
    private final IndexingProgressEmitterManager emitterManager;
    private final ObjectMapper objectMapper;

    /**
     * 进行中的任务状态列表
     */
    private static final List<IndexingTaskStatus> RUNNING_STATUSES = Arrays.asList(
            IndexingTaskStatus.PENDING,
            IndexingTaskStatus.RUNNING
    );

    @Override
    @Transactional
    public IndexingTask submitTask(Long userId, Long kbId, IndexingTaskType taskType, List<Long> documentIds) {
        log.info("Submitting indexing task: userId={}, kbId={}, type={}, docCount={}",
                userId, kbId, taskType, documentIds.size());

        // 检查是否有进行中的任务
        IndexingTask existingTask = getRunningTask(userId, kbId);
        if (existingTask != null) {
            log.info("Found existing running task: taskId={}", existingTask.getId());
            return existingTask;
        }

        // 创建新任务
        String documentIdsJson;
        try {
            documentIdsJson = objectMapper.writeValueAsString(documentIds);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize document IDs", e);
        }

        IndexingTask task = IndexingTask.builder()
                .userId(userId)
                .kbId(kbId)
                .taskType(taskType)
                .status(IndexingTaskStatus.PENDING)
                .totalDocs(documentIds.size())
                .completedDocs(0)
                .failedDocs(0)
                .documentIds(documentIdsJson)
                .createdAt(LocalDateTime.now())
                .build();

        task = indexingTaskRepository.save(task);
        log.info("Created new indexing task: taskId={}", task.getId());

        return task;
    }

    @Override
    public boolean hasRunningTask(Long userId, Long kbId) {
        return indexingTaskRepository.findByUserIdAndKbIdAndStatusIn(userId, kbId, RUNNING_STATUSES).isPresent();
    }

    @Override
    public IndexingTask getRunningTask(Long userId, Long kbId) {
        return indexingTaskRepository.findByUserIdAndKbIdAndStatusIn(userId, kbId, RUNNING_STATUSES).orElse(null);
    }

    @Override
    public IndexingTask getTask(Long taskId) {
        return indexingTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("IndexingTask", taskId));
    }

    @Override
    @Async("indexingExecutor")
    public void executeIndexingAsync(Long taskId) {
        log.info("Starting async indexing execution: taskId={}", taskId);

        IndexingTask task = indexingTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("Task not found: taskId={}", taskId);
            return;
        }

        // 更新任务状态为 RUNNING
        task.setStatus(IndexingTaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        indexingTaskRepository.save(task);

        Long userId = task.getUserId();
        Long kbId = task.getKbId();

        // 解析文档 ID 列表
        List<Long> documentIds;
        try {
            documentIds = objectMapper.readValue(task.getDocumentIds(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse document IDs: taskId={}", taskId, e);
            task.setStatus(IndexingTaskStatus.FAILED);
            task.setErrorMessage("Failed to parse document IDs: " + e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            indexingTaskRepository.save(task);
            return;
        }

        // 获取知识库和配置
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
        if (kb == null) {
            log.error("Knowledge base not found: kbId={}", kbId);
            task.setStatus(IndexingTaskStatus.FAILED);
            task.setErrorMessage("Knowledge base not found");
            task.setCompletedAt(LocalDateTime.now());
            indexingTaskRepository.save(task);
            return;
        }

        IndexStrategyConfig indexConfig;
        try {
            indexConfig = objectMapper.readValue(kb.getIndexStrategyConfig(), IndexStrategyConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse index strategy config: kbId={}", kbId, e);
            task.setStatus(IndexingTaskStatus.FAILED);
            task.setErrorMessage("Failed to parse index strategy config: " + e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            indexingTaskRepository.save(task);
            return;
        }

        int completed = 0;
        int failed = 0;

        // 创建进度回调
        IndexingProgressCallback callback = createProgressCallback(userId, kbId, task);

        // 逐个处理文档
        for (Long documentId : documentIds) {
            try {
                // 根据任务类型执行不同操作
                if (task.getTaskType() == IndexingTaskType.INDEX) {
                    indexingService.executeIndexing(documentId, kb, indexConfig, callback);
                } else {
                    indexingService.executeRebuildIndexFromChunks(documentId, kb, indexConfig, callback);
                }
                completed++;
            } catch (Exception e) {
                log.error("Failed to index document: documentId={}", documentId, e);
                failed++;
            }

            // 更新任务进度
            task.setCompletedDocs(completed);
            task.setFailedDocs(failed);
            indexingTaskRepository.save(task);

            // 发送进度事件
            emitterManager.sendProgress(userId, kbId, taskId,
                    task.getTotalDocs(), completed, failed);
        }

        // 任务完成
        task.setStatus(failed == documentIds.size() ? IndexingTaskStatus.FAILED : IndexingTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setCurrentDocId(null);
        task.setCurrentDocName(null);
        task.setCurrentStep(null);
        indexingTaskRepository.save(task);

        // 发送完成事件
        emitterManager.sendDone(userId, kbId, taskId, task.getTotalDocs(), completed, failed);

        log.info("Indexing task completed: taskId={}, completed={}, failed={}", taskId, completed, failed);
    }

    @Override
    public SseEmitter subscribe(Long userId, Long kbId) {
        return emitterManager.createEmitter(userId, kbId);
    }

    /**
     * 创建进度回调
     */
    private IndexingProgressCallback createProgressCallback(Long userId, Long kbId, IndexingTask task) {
        return new IndexingProgressCallback() {
            @Override
            public void onStepChanged(Long documentId, String documentName, IndexingStep step) {
                // 更新任务当前状态
                task.setCurrentDocId(documentId);
                task.setCurrentDocName(documentName);
                task.setCurrentStep(step);
                indexingTaskRepository.save(task);

                // 发送步骤事件
                emitterManager.sendStep(userId, kbId, documentId, documentName, step);
            }

            @Override
            public void onDocumentCompleted(Long documentId, String documentName) {
                log.debug("Document completed: documentId={}, name={}", documentId, documentName);
            }

            @Override
            public void onDocumentFailed(Long documentId, String documentName, String error) {
                // 发送错误事件
                emitterManager.sendError(userId, kbId, documentId, documentName, error);
            }
        };
    }
}
