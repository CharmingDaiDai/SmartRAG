package com.mtmn.smartrag.service.impl;

import com.mtmn.smartrag.po.UserActivity;
import com.mtmn.smartrag.repository.UserActivityRepository;
import com.mtmn.smartrag.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户活动服务实现类
 *
 * @author charmingdaidai
 */
// TODO 待完善
@Service
@RequiredArgsConstructor
@Slf4j
public class UserActivityServiceImpl implements UserActivityService {

    private final UserActivityRepository userActivityRepository;

    /**
     * 记录用户活动
     * <p>
     * 实现思路：
     * 1. 接收用户活动的详细信息参数
     * 2. 使用Builder模式构建UserActivity实体对象
     * 3. 设置当前时间作为活动创建时间
     * 4. 将活动记录保存到数据库
     * 5. 记录操作日志便于问题追踪
     * 6. 返回保存后的活动实体对象
     *
     * @param userId       用户ID
     * @param activityType 活动类型（如SUMMARY、KEYWORDS等）
     * @param documentId   关联的文档ID
     * @param documentName 文档名称
     * @param description  活动描述
     * @return 保存后的用户活动实体对象
     */
    @Override
    public UserActivity recordActivity(Long userId, String activityType, Long documentId,
                                       String documentName, String description) {
        log.info("记录用户活动: userId={}, activityType={}, documentId={}", userId, activityType, documentId);

        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .activityType(activityType)
                .documentId(documentId)
                .documentName(documentName)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        return userActivityRepository.save(activity);
    }

    /**
     * 记录摘要分析活动
     * <p>
     * 实现思路：
     * 1. 调用通用的recordActivity方法
     * 2. 设置活动类型为SUMMARY
     * 3. 使用标准化的描述信息
     * 4. 简化摘要分析活动的记录流程
     *
     * @param userId       用户ID
     * @param documentId   文档ID
     * @param documentName 文档名称
     * @return 保存后的用户活动实体对象
     */
    @Override
    public UserActivity recordSummaryAnalysis(Long userId, Long documentId, String documentName) {
        return recordActivity(
                userId,
                UserActivity.ActivityType.SUMMARY.name(),
                documentId,
                documentName,
                "生成文档摘要"
        );
    }

    /**
     * 记录关键词分析活动
     * <p>
     * 实现思路：
     * 1. 调用通用的recordActivity方法
     * 2. 设置活动类型为KEYWORDS
     * 3. 使用标准化的描述信息
     * 4. 简化关键词分析活动的记录流程
     *
     * @param userId       用户ID
     * @param documentId   文档ID
     * @param documentName 文档名称
     * @return 保存后的用户活动实体对象
     */
    @Override
    public UserActivity recordKeywordsAnalysis(Long userId, Long documentId, String documentName) {
        return recordActivity(
                userId,
                UserActivity.ActivityType.KEYWORDS.name(),
                documentId,
                documentName,
                "提取文档关键词"
        );
    }

    /**
     * 记录安全检查活动
     * <p>
     * 实现思路：
     * 1. 调用通用的recordActivity方法
     * 2. 设置活动类型为SECURITY
     * 3. 使用标准化的描述信息
     * 4. 简化安全检查活动的记录流程
     *
     * @param userId       用户ID
     * @param documentId   文档ID
     * @param documentName 文档名称
     * @return 保存后的用户活动实体对象
     */
    @Override
    public UserActivity recordSecurityAnalysis(Long userId, Long documentId, String documentName) {
        return recordActivity(
                userId,
                UserActivity.ActivityType.SECURITY.name(),
                documentId,
                documentName,
                "进行文档安全检查"
        );
    }

    /**
     * 记录文档润色活动
     * <p>
     * 实现思路：
     * 1. 调用通用的recordActivity方法
     * 2. 设置活动类型为POLISH
     * 3. 使用标准化的描述信息
     * 4. 简化文档润色活动的记录流程
     *
     * @param userId       用户ID
     * @param documentId   文档ID
     * @param documentName 文档名称
     * @return 保存后的用户活动实体对象
     */
    @Override
    public UserActivity recordPolishAnalysis(Long userId, Long documentId, String documentName) {
        return recordActivity(
                userId,
                UserActivity.ActivityType.POLISH.name(),
                documentId,
                documentName,
                "进行文档润色优化"
        );
    }

    /**
     * 获取用户的所有活动记录
     * <p>
     * 实现思路：
     * 1. 根据用户ID查询该用户的所有活动记录
     * 2. 直接调用Repository的查询方法
     * 3. 返回完整的活动记录列表
     *
     * @param userId 用户ID
     * @return 用户的所有活动记录列表
     */
    @Override
    public List<UserActivity> getUserActivities(Long userId) {
        return userActivityRepository.findByUserId(userId);
    }

    /**
     * 分页获取用户活动记录
     * <p>
     * 实现思路：
     * 1. 根据用户ID查询该用户的活动记录
     * 2. 使用Pageable参数进行分页处理
     * 3. 按创建时间倒序排列，最新的活动在前
     * 4. 返回分页后的活动记录结果
     *
     * @param userId   用户ID
     * @param pageable 分页参数，包含页码、页面大小等信息
     * @return 分页后的用户活动记录
     */
    @Override
    public Page<UserActivity> getUserActivitiesPaged(Long userId, Pageable pageable) {
        return userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 获取文档相关的活动记录
     * <p>
     * 实现思路：
     * 1. 根据文档ID查询与该文档相关的所有活动记录
     * 2. 按创建时间倒序排列，最新的活动在前
     * 3. 返回该文档的完整活动历史记录
     * 4. 用于追踪文档的操作历史和使用情况
     *
     * @param documentId 文档ID
     * @return 与文档相关的活动记录列表，按时间倒序排列
     */
    @Override
    public List<UserActivity> getDocumentActivities(Long documentId) {
        return userActivityRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }
}