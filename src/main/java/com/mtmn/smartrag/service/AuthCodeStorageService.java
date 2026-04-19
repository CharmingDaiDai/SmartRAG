package com.mtmn.smartrag.service;

import com.mtmn.smartrag.vo.AuthenticationResponse;

/**
 * 授权码存储服务接口
 * 用于临时存储一次性授权码和对应的认证信息
 * <p>
 * 注意：这是一个简化实现，实际生产环境应使用Redis等分布式缓存
 *
 * @author charmingdaidai
 */
public interface AuthCodeStorageService {

    /**
     * 存储授权码及对应的认证响应
     *
     * @param code     一次性授权码
     * @param response 认证响应对象
     */
    void storeAuthCode(String code, AuthenticationResponse response);

    /**
     * 获取并删除授权码对应的认证响应
     *
     * @param code 一次性授权码
     * @return 认证响应对象（如果授权码有效），否则返回null
     */
    AuthenticationResponse getAndRemoveAuthResponse(String code);
}