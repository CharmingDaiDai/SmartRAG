package com.mtmn.smartdoc.repository;

import com.mtmn.smartdoc.enums.IndexingTaskStatus;
import com.mtmn.smartdoc.po.IndexingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 索引任务 Repository
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Repository
public interface IndexingTaskRepository extends JpaRepository<IndexingTask, Long> {

    /**
     * 查找指定知识库下进行中的任务
     */
    Optional<IndexingTask> findByKbIdAndStatusIn(Long kbId, List<IndexingTaskStatus> statuses);

    /**
     * 查找用户指定知识库下进行中的任务
     */
    Optional<IndexingTask> findByUserIdAndKbIdAndStatusIn(Long userId, Long kbId, List<IndexingTaskStatus> statuses);

    /**
     * 查找用户所有进行中的任务
     */
    List<IndexingTask> findByUserIdAndStatusIn(Long userId, List<IndexingTaskStatus> statuses);

    /**
     * 查找指定知识库的最新任务
     */
    Optional<IndexingTask> findFirstByKbIdOrderByCreatedAtDesc(Long kbId);

    /**
     * 删除知识库的所有任务
     */
    void deleteByKbId(Long kbId);
}
