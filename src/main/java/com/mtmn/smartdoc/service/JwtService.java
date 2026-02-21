package com.mtmn.smartdoc.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

/**
 * JWT令牌服务接口
 * 负责JWT令牌的生成、解析和验证
 *
 * @author charmingdaidai
 */
public interface JwtService {

    /**
     * 从JWT令牌中提取用户名
     *
     * @param token JWT令牌字符串
     * @return 令牌中存储的用户名
     */
    String extractUsername(String token);

    /**
     * 为用户生成JWT访问令牌
     *
     * @param userDetails Spring Security用户详情对象
     * @return 包含用户信息的JWT访问令牌
     */
    String generateToken(UserDetails userDetails);

    /**
     * 生成包含额外声明的JWT令牌
     *
     * @param extraClaims 额外的声明信息Map
     * @param userDetails Spring Security用户详情对象
     * @return 包含额外声明的JWT令牌
     */
    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    /**
     * 生成JWT刷新令牌
     *
     * @param userDetails Spring Security用户详情对象
     * @return JWT刷新令牌
     */
    String generateRefreshToken(UserDetails userDetails);

    /**
     * 验证JWT令牌是否有效
     *
     * @param token       JWT令牌字符串
     * @param userDetails 用户详情对象
     * @return true表示令牌有效, false表示无效
     */
    boolean isTokenValid(String token, UserDetails userDetails);
}