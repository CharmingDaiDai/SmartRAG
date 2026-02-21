package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.dto.AuthenticationRequest;
import com.mtmn.smartdoc.vo.AuthenticationResponse;
import com.mtmn.smartdoc.vo.RegisterRequest;


/**
 * 认证服务接口
 *
 * @author charmingdaidai
 */
public interface AuthenticationService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 认证响应，包含JWT令牌
     */
    public AuthenticationResponse register(RegisterRequest request);

    /**
     * 用户认证登录
     *
     * @param request 认证请求
     * @return 认证响应，包含JWT令牌
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request);

    /**
     * 创建GitHub OAuth授权URL
     *
     * @return GitHub授权URL
     */
    String createGithubAuthorizationUrl();

    /**
     * 使用GitHub OAuth进行认证
     *
     * @param code  GitHub OAuth返回的授权码
     * @param state 状态参数
     * @return 认证响应，包含JWT令牌
     */
    AuthenticationResponse authenticateWithGithub(String code, String state);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的认证响应，包含新的访问令牌和刷新令牌
     */
    public AuthenticationResponse refreshToken(String refreshToken);
}