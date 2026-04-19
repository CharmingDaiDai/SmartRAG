package com.mtmn.smartrag.repository;

import com.mtmn.smartrag.po.UserActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author charmingdaidai
 */
@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    /**
     * 查找特定用户的所有活动记录
     */
    List<UserActivity> findByUserId(Long userId);

    /**
     * 分页查询用户活动记录，按创建时间降序排序
     * 同时支持获取用户最近活动
     */
    Page<UserActivity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 按时间范围查询用户活动
     */
    List<UserActivity> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 按活动类型查询用户活动
     */
    List<UserActivity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(Long userId, String activityType);

    /**
     * 查询与特定文档相关的活动记录
     */
    List<UserActivity> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * 统计用户的分析活动数量（summary, keywords, security, polish）
     */
    @Query("SELECT COUNT(a) FROM UserActivity a WHERE a.userId = :userId AND a.activityType IN ('SUMMARY', 'KEYWORDS', 'SECURITY', 'POLISH')")
    long countAnalysisActivitiesByUserId(@Param("userId") Long userId);

    /**
     * 按用户和活动类型统计数量
     */
    long countByUserIdAndActivityType(Long userId, String activityType);
}