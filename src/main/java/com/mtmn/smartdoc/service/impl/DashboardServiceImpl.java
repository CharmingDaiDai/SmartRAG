package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.dto.DashboardStatisticsDto;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.po.UserActivity;
import com.mtmn.smartdoc.repository.ConversationRepository;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.repository.UserActivityRepository;
import com.mtmn.smartdoc.repository.UserRepository;
import com.mtmn.smartdoc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现
 * 负责用户活动记录、统计数据计算和活动历史查询
 *
 * @author charmingdaidai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserActivityRepository userActivityRepository;
    private final ConversationRepository conversationRepository;
    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final UserRepository userRepository;

    @Override
    public void recordUserActivity(Authentication authentication, String activityType,
                                   Long documentId, String documentName, String description) {
        User user = getUserFromAuthentication(authentication);
        if (user == null) {
            log.warn("无法记录活动：用户未找到");
            return;
        }

        try {
            UserActivity activity = UserActivity.builder()
                    .userId(user.getId())
                    .activityType(activityType)
                    .documentId(documentId)
                    .documentName(documentName)
                    .description(description)
                    .build();

            userActivityRepository.save(activity);
            log.debug("用户活动已记录：用户={}, 类型={}", user.getUsername(), activityType);
        } catch (Exception e) {
            log.error("记录用户活动失败", e);
        }
    }

    @Override
    public DashboardStatisticsDto getUserStatistics(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<DashboardStatisticsDto.WordCloudItem> wordCloud = buildMockWordCloud();

        if (user == null) {
            return DashboardStatisticsDto.builder()
                    .knowledgeBases(0L)
                    .documents(0L)
                    .conversationStats(buildConversationStats(0L))
                    .wordCloud(wordCloud)
                    .build();
        }

        Long userId = user.getId();

        long kbCount = knowledgeBaseRepository.countByUserId(userId);
        long totalDocuments = documentRepository.countByUserId(userId);
        DashboardStatisticsDto.ConversationStats conversationStats = buildConversationStats(userId);

        return DashboardStatisticsDto.builder()
                .knowledgeBases(kbCount)
                .documents(totalDocuments)
                .conversationStats(conversationStats)
                .wordCloud(wordCloud)
                .build();
    }

//    @Override
//    public List<UserActivityDto> getUserRecentActivities(Authentication authentication, int limit) {
//        User user = getUserFromAuthentication(authentication);
//        if (user == null) {
//            return Collections.emptyList();
//        }
//
//        Pageable pageable = PageRequest.of(0, limit);
//        return userActivityRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
//                .stream()
//                .map(this::convertToDTO)
//                .collect(Collectors.toList());
//    }

    /**
     * 从认证信息中获取用户对象
     */
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    private DashboardStatisticsDto.ConversationStats buildConversationStats(Long userId) {
        LocalDate today = LocalDate.now();
        List<DashboardStatisticsDto.DailyConversationCount> daily = new ArrayList<>();

        if (userId == null || userId <= 0) {
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                daily.add(DashboardStatisticsDto.DailyConversationCount.builder()
                        .date(date.toString())
                        .count(0)
                        .build());
            }

            return DashboardStatisticsDto.ConversationStats.builder()
                    .total(0)
                    .last7Days(daily)
                    .build();
        }

        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Map<String, Object>> rows = conversationRepository.countDailyByUserIdBetween(userId, start, end);
        Map<String, Long> dailyMap = rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row.get("day")),
                row -> {
                    Object count = row.get("count");
                    return count instanceof Number ? ((Number) count).longValue() : 0L;
                }
        ));

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = dailyMap.getOrDefault(date.toString(), 0L);
            daily.add(DashboardStatisticsDto.DailyConversationCount.builder()
                    .date(date.toString())
                    .count(count)
                    .build());
        }

        long total = conversationRepository.countByUserId(userId);
        return DashboardStatisticsDto.ConversationStats.builder()
                .total(total)
                .last7Days(daily)
                .build();
    }

    private List<DashboardStatisticsDto.WordCloudItem> buildMockWordCloud() {
        List<DashboardStatisticsDto.WordCloudItem> items = new ArrayList<>();
        items.add(wordCloud("检索模块", "语义检索", 11.7392));
        items.add(wordCloud("检索模块", "召回率", 9.2372));
        items.add(wordCloud("检索模块", "精准匹配", 7.7543));
        items.add(wordCloud("嵌入模型", "Embedding", 11.3865));
        items.add(wordCloud("嵌入模型", "向量生成", 5.8354));
        items.add(wordCloud("向量数据库", "相似度计算", 6.3250));
        items.add(wordCloud("向量数据库", "索引构建", 9.2955));
        items.add(wordCloud("LLM推理层", "上下文拼接", 8.7077));
        items.add(wordCloud("LLM推理层", "Prompt", 11.7392));
        items.add(wordCloud("知识库管理", "分块Chunking", 34.4563));
        items.add(wordCloud("知识库管理", "文档解析", 21.6193));
        items.add(wordCloud("提示词工程", "明辨意图", 11.7034));
        items.add(wordCloud("提示词工程", "上下文窗口", 8.7350));
        items.add(wordCloud("重排序模块", "因果律匹配", 12.5143));
        items.add(wordCloud("数据预处理", "清洗去噪", 10.4471));
        items.add(wordCloud("RAG框架", "适者召回", 10.5684));
        items.add(wordCloud("问答场景", "自由问答", 11.8006));
        items.add(wordCloud("运维监控", "知识库更新", 11.7392));
        items.add(wordCloud("运维监控", "增量索引", 9.2372));
        items.add(wordCloud("评估指标", "准确率", 7.7543));
        items.add(wordCloud("评估指标", "响应速度", 5.8354));
        return items;
    }

    private DashboardStatisticsDto.WordCloudItem wordCloud(String name, String text, double value) {
        return DashboardStatisticsDto.WordCloudItem.builder()
                .name(name)
                .text(text)
                .value(value)
                .build();
    }

//    /**
//     * 将用户活动实体转换为DTO
//     */
//    private UserActivityDto convertToDTO(UserActivity activity) {
//        return UserActivityDto.builder()
//                .id(activity.getId())
//                .activityType(activity.getActivityType())
//                .documentId(activity.getDocumentId())
//                .documentName(activity.getDocumentName())
//                .description(activity.getDescription())
//                .createdAt(activity.getCreatedAt())
//                .build();
//    }
}