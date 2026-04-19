package com.mtmn.smartrag.service.impl;

import com.mtmn.smartrag.common.CustomException;
import com.mtmn.smartrag.dto.AuthenticationRequest;
import com.mtmn.smartrag.po.User;
import com.mtmn.smartrag.repository.UserRepository;
import com.mtmn.smartrag.service.AuthenticationService;
import com.mtmn.smartrag.service.JwtService;
import com.mtmn.smartrag.service.MinioService;
import com.mtmn.smartrag.vo.AuthenticationResponse;
import com.mtmn.smartrag.vo.GitHubUserInfoResponse;
import com.mtmn.smartrag.vo.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 认证服务实现
 * 负责用户注册、登录、GitHub OAuth认证和令牌刷新等功能
 *
 * @author charmingdaidai
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;
    private final MinioService minioService;
    @Value("${spring.security.oauth2.client.github.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.github.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.github.redirectUri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.github.scope}")
    private String scope;

    /**
     * 用户注册
     * <p>
     * 实现思路：
     * 1. 根据注册请求构建新用户对象
     * 2. 使用BCrypt对用户密码进行加密存储
     * 3. 设置用户基本信息和默认状态（非VIP、已启用）
     * 4. 将用户信息保存到数据库
     * 5. 为新注册用户生成JWT访问令牌和刷新令牌
     * 6. 返回包含令牌的认证响应
     *
     * @param request 用户注册请求，包含用户名、邮箱、密码等信息
     * @return 包含JWT令牌的认证响应对象
     */
    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        // 创建用户对象
        var user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .vip(false)
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .build();

        // 保存用户
        userRepository.save(user);

        // 生成JWT令牌
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 用户登录认证
     * <p>
     * 实现思路：
     * 1. 使用Spring Security的AuthenticationManager验证用户凭证
     * 2. 验证用户名和密码的正确性
     * 3. 从数据库查询用户信息，检查用户是否存在
     * 4. 验证用户账户状态，确保用户未被禁用
     * 5. 更新用户的最后登录时间
     * 6. 为认证成功的用户生成JWT访问令牌和刷新令牌
     * 7. 返回包含令牌的认证响应
     *
     * @param request 用户登录请求，包含用户名和密码
     * @return 包含JWT令牌的认证响应对象
     * @throws CustomException 当用户不存在或被禁用时抛出
     */
    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 进行认证
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 获取用户
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException(404, "用户不存在"));

        if (!user.isEnabled()) {
            throw new CustomException(403, "用户已被禁用");
        }

        // 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // 生成JWT令牌
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 创建GitHub OAuth授权URL
     * <p>
     * 实现思路：
     * 1. 生成随机的state参数，用于防止CSRF攻击
     * 2. 使用UriComponentsBuilder构建标准的GitHub OAuth授权URL
     * 3. 设置必要的OAuth参数：client_id、redirect_uri、scope、state
     * 4. 返回完整的授权URL供前端重定向使用
     * 5. state参数应保存到session或缓存中以便后续验证
     *
     * @return GitHub OAuth授权URL字符串
     */
    @Override
    public String createGithubAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        // 保存state到session或缓存以便后续验证

        return UriComponentsBuilder.fromHttpUrl("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build().toUriString();
    }

    /**
     * GitHub OAuth认证处理
     * <p>
     * 实现思路：
     * 1. 使用授权码向GitHub交换访问令牌
     * 2. 使用访问令牌获取GitHub用户信息
     * 3. 下载用户头像并上传到MinIO存储服务
     * 4. 根据GitHub ID查找现有用户或创建新用户
     * 5. 更新用户信息和最后登录时间
     * 6. 为用户生成JWT访问令牌和刷新令牌
     * 7. 处理整个流程中的异常情况
     *
     * @param code  GitHub返回的授权码
     * @param state 防CSRF攻击的状态参数
     * @return 包含JWT令牌的认证响应对象
     * @throws RuntimeException 当GitHub认证流程失败时抛出
     */
    @Override
    public AuthenticationResponse authenticateWithGithub(String code, String state) {
        try {
            // 1. 用授权码换取访问令牌
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//
//            Map<String, String> requestBody = Map.of(
//                    "client_id", clientId,
//                    "client_secret", clientSecret,
//                    "code", code,
//                    "redirect_uri", redirectUri
//            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("code", code);
            requestBody.add("redirect_uri", redirectUri);

            ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                    "https://github.com/login/oauth/access_token",
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );

            if (tokenResponse.getBody() == null || !tokenResponse.getBody().containsKey("access_token")) {
                throw new RuntimeException("获取GitHub访问令牌失败");
            }

            String accessToken = (String) tokenResponse.getBody().get("access_token");

            // 2. 获取GitHub用户信息
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            userInfoHeaders.set("Accept", "application/json");

            ResponseEntity<GitHubUserInfoResponse> userInfoResponse = restTemplate.exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    new HttpEntity<>(userInfoHeaders),
                    GitHubUserInfoResponse.class
            );

            GitHubUserInfoResponse githubUser = userInfoResponse.getBody();

            if (githubUser == null) {
                throw new UsernameNotFoundException("GitHub 认证失败: 获取用户信息失败");
            }

            String gitId = githubUser.getId().toString();
            String avatarUrl = githubUser.getAvatarUrl();
            String minioAvatarPath = null;

            // 下载并上传GitHub头像到MinIO
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                try {
                    // 使用新的方法从URL直接下载并上传到MinIO
                    String fileName = "github_avatar_" + gitId + ".jpg";
                    minioAvatarPath = minioService.uploadFileFromUrl(avatarUrl, fileName);
                    log.info("GitHub头像成功上传到MinIO: {}", minioAvatarPath);
                } catch (Exception e) {
                    log.error("下载或上传GitHub用户头像失败: {}", e.getMessage(), e);
                    // 发生错误不中断主流程，继续执行
                }
            }

            // 3. 查找或创建用户
            String finalMinioAvatarPath = minioAvatarPath;
            User user = userRepository.findByGithubId(gitId)
                    .orElseGet(() -> {
                        // 创建新用户
                        User newUser = User.builder()
                                .githubId(gitId)
                                .username(githubUser.getLogin() + "_github") // 确保用户名不冲突
                                .email(githubUser.getEmail())
                                .avatarPath(finalMinioAvatarPath) // 使用MinIO中的头像路径
                                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // 随机密码
                                .vip(false)
                                .createdAt(LocalDateTime.now())
                                .enabled(true)
                                .build();

                        return userRepository.save(newUser);
                    });

            // 4. 更新用户信息（如果是已存在用户）
            if (user.getId() != null) {

                if (user.getAvatarPath() == null && minioAvatarPath != null) {
                    user.setAvatarPath(minioAvatarPath);
                }

                // 更新最后登录时间
                user.setLastLogin(LocalDateTime.now());

                user = userRepository.save(user);

            }

            // 5. 生成JWT令牌
            String jwtToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            return AuthenticationResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("GitHub 登录处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新JWT令牌
     * <p>
     * 实现思路：
     * 1. 从刷新令牌中提取用户名信息
     * 2. 验证刷新令牌的格式和有效性
     * 3. 根据用户名从数据库查询用户信息
     * 4. 验证刷新令牌是否为该用户签发且未过期
     * 5. 为用户生成新的访问令牌和刷新令牌
     * 6. 返回包含新令牌的认证响应
     *
     * @param refreshToken 客户端提供的刷新令牌
     * @return 包含新JWT令牌的认证响应对象
     * @throws CustomException 当刷新令牌无效或用户不存在时抛出
     */
    @Override
    public AuthenticationResponse refreshToken(String refreshToken) {
        // 验证刷新令牌的有效性
        String username = jwtService.extractUsername(refreshToken);
        if (username == null) {
            throw new CustomException(401, "无效的刷新令牌");
        }

        // 获取用户信息
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(404, "用户不存在"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new CustomException(401, "无效的刷新令牌");
        }

        // 生成新的访问令牌和刷新令牌
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // 返回新的认证响应
        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}