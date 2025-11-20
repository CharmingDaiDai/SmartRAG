package com.mtmn.smartdoc.config;

import com.mtmn.smartdoc.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author charmingdaidai
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 判断是否为流式响应请求
        boolean isStreamRequest = request.getRequestURI().contains("/api/kb/chat/") &&
                ("text/event-stream".equals(request.getHeader("Accept")) ||
                        request.getRequestURI().endsWith(".stream"));

        // 如果没有Authorization头或不是Bearer token，则直接放行
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 解析JWT
        jwt = authHeader.substring(7);

        try {
            // 尝试提取用户名
            username = jwtService.extractUsername(jwt);

            // 如果获取到用户名且尚未认证
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 检查token是否有效
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 对于流式请求，记录额外日志
                    if (isStreamRequest) {
                        log.debug("流式请求认证成功: {}", request.getRequestURI());
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
            // 流式请求中的令牌过期处理
            if (isStreamRequest && !response.isCommitted()) {
                handleStreamingError(response, "Token expired");
                return; // 不继续处理过期的流式请求
            }
        } catch (MalformedJwtException | SignatureException e) {
            log.warn("无效的JWT令牌: {}", e.getMessage());
            // 流式请求中的无效令牌处理
            if (isStreamRequest && !response.isCommitted()) {
                handleStreamingError(response, "Invalid token");
                return; // 不继续处理无效令牌的流式请求
            }
        } catch (JwtException e) {
            log.warn("JWT解析异常: {}", e.getMessage());
            // 流式请求中的JWT异常处理
            if (isStreamRequest && !response.isCommitted()) {
                handleStreamingError(response, "Token error");
                return; // 不继续处理令牌有问题的流式请求
            }
        } catch (Exception e) {
            log.error("处理JWT时发生未预期的异常: {}", e.getMessage());
            // 流式请求中的其他异常处理
            if (isStreamRequest && !response.isCommitted()) {
                handleStreamingError(response, "Authentication error");
                return; // 不继续处理发生异常的流式请求
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 处理流式请求的认证错误
     *
     * @param response     HttpServletResponse
     * @param errorMessage 错误消息
     * @throws IOException 如果写入响应时发生错误
     */
    private void handleStreamingError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("event: error\ndata: " + errorMessage + "\n\n");
        response.flushBuffer();
    }
}