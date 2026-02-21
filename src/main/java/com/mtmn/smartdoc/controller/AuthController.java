package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.AuthenticationRequest;
import com.mtmn.smartdoc.dto.TokenRefreshRequest;
import com.mtmn.smartdoc.service.AuthCodeStorageService;
import com.mtmn.smartdoc.service.AuthenticationService;
import com.mtmn.smartdoc.vo.AuthenticationResponse;
import com.mtmn.smartdoc.vo.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

/**
 * @author charmingdaidai
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "用户认证和注册接口")
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthCodeStorageService authCodeStorage;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public ApiResponse<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        log.info("收到用户注册请求: {}", request.getUsername());
        try {
            AuthenticationResponse response = authenticationService.register(request);
            log.info("用户注册成功: {}", request.getUsername());
            return ApiResponse.success("用户注册成功", response);
        } catch (Exception e) {
            log.error("用户注册失败: {} - 错误信息: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录")
    public ApiResponse<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest request
    ) {
        log.info("收到用户登录请求: {}", request.getUsername());
        try {
            AuthenticationResponse response = authenticationService.authenticate(request);
            log.info("用户登录成功: {}", request.getUsername());
            return ApiResponse.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录失败: {} - 错误信息: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/login/github")
    @Operation(summary = "GitHub登录", description = "生成GitHub授权URL")
    public ApiResponse<String> githubLogin() {
        log.info("收到GitHub登录请求");
        String authorizationUrl = authenticationService.createGithubAuthorizationUrl();
        return ApiResponse.success("已生成GitHub授权URL", authorizationUrl);
    }

    @GetMapping("/callback/github")
    @Operation(summary = "GitHub回调", description = "处理GitHub OAuth回调")
    public void githubCallback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response
    ) {
        log.info("收到GitHub回调请求");
        try {
            // 获取GitHub用户信息和生成JWT token
            AuthenticationResponse authResponse = authenticationService.authenticateWithGithub(code, state);
            log.info("GitHub登录成功");

            // 生成一次性授权码（使用UUID）
            String oneTimeCode = UUID.randomUUID().toString();

            // 将授权码与token存储在Redis或内存缓存中，设置较短的过期时间（例如2分钟）
            // 这里简化处理，使用内存Map存储，实际生产环境应使用Redis等缓存服务
            authCodeStorage.storeAuthCode(oneTimeCode, authResponse);

            // 重定向到前端应用，只传递一次性授权码
            String frontendRedirectUrl = "http://localhost:3000/auth/callback/github?code=" + oneTimeCode;
            response.sendRedirect(frontendRedirectUrl);
        } catch (Exception e) {
            log.error("GitHub登录失败 - 错误信息: {}", e.getMessage());
            try {
                // 登录失败时重定向到登录页面并带上错误信息
                response.sendRedirect("http://localhost:3000/login?error=github-auth-failed");
            } catch (IOException ex) {
                log.error("重定向失败", ex);
            }
        }
    }

    @PostMapping("/login/wx")
    @Operation(summary = "微信登录", description = "生成微信授权URL")
    public ApiResponse<String> wxLogin() {
        log.info("收到微信登录请求");

        return ApiResponse.error(404, "敬请期待");
    }

    @PostMapping("/login/qq")
    @Operation(summary = "qq登录", description = "生成qq授权URL")
    public ApiResponse<String> qqLogin() {
        log.info("收到qq登录请求");

        return ApiResponse.error(404, "敬请期待");
    }

    @PostMapping("/exchange-token")
    @Operation(summary = "交换授权码获取Token", description = "使用一次性授权码获取JWT token")
    public ApiResponse<AuthenticationResponse> exchangeToken(@RequestParam("code") String code) {
        log.info("收到授权码交换Token请求");

        // 验证并获取授权码对应的认证信息
        AuthenticationResponse authResponse = authCodeStorage.getAndRemoveAuthResponse(code);

        if (authResponse == null) {
            log.warn("无效或已过期的授权码: {}", code);
            return ApiResponse.error(401, "无效或已过期的授权码");
        }

        log.info("授权码交换Token成功");
        return ApiResponse.success("Token获取成功", authResponse);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ApiResponse<AuthenticationResponse> refreshToken(
            @RequestBody TokenRefreshRequest request
    ) {
        log.info("收到令牌刷新请求");
        try {
            AuthenticationResponse response = authenticationService.refreshToken(request.getRefreshToken());
            log.info("令牌刷新成功");
            return ApiResponse.success("令牌刷新成功", response);
        } catch (Exception e) {
            log.error("令牌刷新失败 - 错误信息: {}", e.getMessage());
            throw e;
        }
    }
}