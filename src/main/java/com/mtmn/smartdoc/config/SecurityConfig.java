package com.mtmn.smartdoc.config;

import com.mtmn.smartdoc.repository.UserRepository;
import com.mtmn.smartdoc.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @author charmingdaidai
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 不需要认证的路径
                        .requestMatchers("/api/auth/**").permitAll()
                        // Knife4j 和 Swagger 文档路径（完整配置）
                        .requestMatchers("/doc.html", "/doc.html/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/v3/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 测试接口（临时开放，生产环境应移除）
//                        .requestMatchers("/api/kb/chat/**").permitAll()
                        .requestMatchers("/api/chat/**").permitAll()
                        // 要求认证的路径
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/documents/**").authenticated()
                        // VIP功能
                        .requestMatchers("/api/vip/**").hasAuthority("ROLE_VIP")
                        // 其他请求都需要认证
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // 禁用响应提交后的异常处理，防止流式响应中断
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint((request, response, authException) -> {
                            // 针对流式响应的特殊处理
                            boolean isStreamRequest = request.getRequestURI().contains("/api/kb/chat/") &&
                                    (MediaType.TEXT_EVENT_STREAM_VALUE.equals(request.getHeader("Accept")) ||
                                            request.getRequestURI().endsWith(".stream"));

                            if (response.isCommitted()) {
                                // 响应已经提交，不要尝试写入错误信息
                                return;
                            } else if (isStreamRequest && !response.isCommitted()) {
                                // 流式请求但响应尚未提交，发送401状态但不终止连接
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.getWriter().write("event: error\ndata: Unauthorized\n\n");
                                response.flushBuffer();
                            } else {
                                // 普通请求，发送标准401响应
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));  // 允许所有来源
        configuration.setAllowedMethods(List.of("*"));  // 允许所有HTTP方法
        configuration.setAllowedHeaders(List.of("*"));  // 允许所有请求头
        configuration.setExposedHeaders(List.of("*"));  // 暴露所有响应头
        configuration.setAllowCredentials(false);      // 不允许发送凭证（与允许所有来源冲突）
        configuration.setMaxAge(3600L);                // 预检请求的有效期，单位为秒

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService());
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}