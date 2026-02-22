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
import com.mtmn.smartdoc.service.IndexingService;
import com.mtmn.smartdoc.service.IndexingTaskService;
import com.mtmn.smartdoc.vo.IndexingTaskProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 索引任务服务实现
 *
 * @author charmingdaidai
 * @version 3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingTaskServiceImpl implements IndexingTaskService {

    private final IndexingTaskRepository indexingTaskRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final IndexingService indexingService;
    private final ObjectMapper objectMapper;

    private static final List<IndexingTaskStatus> RUNNING_STATUSES = Arrays.asList(
            IndexingTaskStatus.PENDING,
            IndexingTaskStatus.RUNNING
    );

    /**
     * 应用启动时清理孤儿任务（服务重启前残留的 PENDING/RUNNING 任务）
     * 孤儿任务会阻止新任务提交，必须在启动时重置为 FAILED
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOrphanTasks() {
        List<IndexingTask> orphans = indexingTaskRepository.findByStatusIn(RUNNING_STATUSES);
        if (!orphans.isEmpty()) {
            for (IndexingTask task : orphans) {
                task.setStatus(IndexingTaskStatus.FAILED);
                task.setErrorMessage("服务重启导致任务中断，请重新构建索引");
                task.setCompletedAt(LocalDateTime.now());
            }
            indexingTaskRepository.saveAll(orphans);
            log.warn("Cleaned up {} orphan indexing tasks on startup", orphans.size());
        }
    }

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

        // 创建进度回调（将当前步骤持久化到 indexing_tasks 表，供轮询接口读取）
        IndexingProgressCallback callback = createProgressCallback(task);

        // 逐个处理文档
        for (Long documentId : documentIds) {
            try {
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

            // 每处理完一个文档，更新整体进度到数据库（轮询端读取）
            try {
                task.setCompletedDocs(completed);
                task.setFailedDocs(failed);
                indexingTaskRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to save task progress: taskId={}", taskId, e);
            }
        }

        // 任务完成
        task.setStatus(failed == documentIds.size() ? IndexingTaskStatus.FAILED : IndexingTaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setCurrentDocId(null);
        task.setCurrentDocName(null);
        task.setCurrentStep(null);
        try {
            indexingTaskRepository.save(task);
        } catch (Exception e) {
            log.error("Failed to save task completion: taskId={}", taskId, e);
        }

        log.info("Indexing task completed: taskId={}, completed={}, failed={}", taskId, completed, failed);
    }

    /**
     * 查询最近一次任务进度（供前端轮询）
     * 优先返回进行中的任务，无进行中任务则返回最近一次任务（让前端看到最终状态）
     */
    @Override
    public IndexingTaskProgressVO getLatestTaskProgress(Long userId, Long kbId) {
        // 先查进行中的任务
        IndexingTask task = indexingTaskRepository
                .findByUserIdAndKbIdAndStatusIn(userId, kbId, RUNNING_STATUSES)
                .orElse(null);

        // 无进行中任务则取最近一条（任意状态，让前端展示最终状态）
        if (task == null) {
            task = indexingTaskRepository
                    .findFirstByKbIdOrderByCreatedAtDesc(kbId)
                    .orElse(null);
        }

        return task != null ? IndexingTaskProgressVO.from(task) : null;
    }

    /**
     * 创建进度回调：将当前步骤和文档信息持久化到 indexing_tasks 表
     * 前端通过轮询接口读取这些信息，无需 SSE
     */
    private IndexingProgressCallback createProgressCallback(IndexingTask task) {
        return new IndexingProgressCallback() {
            @Override
            public void onStepChanged(Long documentId, String documentName, IndexingStep step) {
                task.setCurrentDocId(documentId);
                task.setCurrentDocName(documentName);
                task.setCurrentStep(step);
                indexingTaskRepository.save(task);
            }

            @Override
            public void onDocumentCompleted(Long documentId, String documentName) {
                log.debug("Document indexing completed: documentId={}, name={}", documentId, documentName);
            }

            @Override
            public void onDocumentFailed(Long documentId, String documentName, String error) {
                log.warn("Document indexing failed: documentId={}, name={}, error={}", documentId, documentName, error);
            }
        };
    }
}
