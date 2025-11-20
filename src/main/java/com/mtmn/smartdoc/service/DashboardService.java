package com.mtmn.smartdoc.service;


import com.mtmn.smartdoc.dto.DashboardStatisticsDto;
import com.mtmn.smartdoc.dto.UserActivityDto;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * 仪表盘服务接口
 * 负责用户活动记录、统计数据计算和活动历史查询
 *
 * @author charmingdaidai
 */
public interface DashboardService {

    /**
     * 记录用户活动到数据库
     *
     * @param authentication 用户认证信息
     * @param activityType   活动类型（如SUMMARY、KEYWORDS等）
     * @param documentId     关联的文档ID
     * @param documentName   文档名称
     * @param description    活动描述信息
     */
    void recordUserActivity(Authentication authentication, String activityType,
                            Long documentId, String documentName, String description);

    /**
     * 获取用户的仪表盘统计数据
     *
     * @param authentication 用户认证信息
     * @return 仪表盘统计数据DTO
     */
    DashboardStatisticsDto getUserStatistics(Authentication authentication);

//    /**
//     * 获取用户的最近活动
//     *
//     * @param authentication 认证信息
//     * @param limit          限制返回的活动数量
//     * @return 用户活动DTO列表
//     */
//    List<UserActivityDto> getUserRecentActivities(Authentication authentication, int limit);
}