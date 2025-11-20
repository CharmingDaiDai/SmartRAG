package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.dto.DashboardStatisticsDto;
import com.mtmn.smartdoc.dto.UserActivityDto;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.po.UserActivity;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.UserActivityRepository;
import com.mtmn.smartdoc.repository.UserRepository;
import com.mtmn.smartdoc.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
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
    private final DocumentRepository documentRepository;
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
        if (user == null) {
            return DashboardStatisticsDto.builder()
                    .documents(0L)
                    .keywords(0L)
                    .security(0L)
                    .summary(0L)
                    .polish(0L)
                    .build();
        }

        Long userId = user.getId();

        // 统计文档数量 - TODO: 改为统计知识库中的文档

        long totalDocuments = documentRepository.countByUserId(userId);

        // 统计各类分析活动
        long keywordCount = userActivityRepository.countByUserIdAndActivityType(userId, "KEYWORDS");
        long securityCount = userActivityRepository.countByUserIdAndActivityType(userId, "SECURITY");
        long summaryCount = userActivityRepository.countByUserIdAndActivityType(userId, "SUMMARY");
        long polishCount = userActivityRepository.countByUserIdAndActivityType(userId, "POLISH");

        return DashboardStatisticsDto.builder()
                .documents(totalDocuments)
                .keywords(keywordCount)
                .security(securityCount)
                .summary(summaryCount)
                .polish(polishCount)
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