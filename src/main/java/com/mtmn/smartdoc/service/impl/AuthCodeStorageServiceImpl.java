package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.service.AuthCodeStorageService;
import com.mtmn.smartdoc.vo.AuthenticationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 授权码存储服务实现
 * 用于临时存储一次性授权码和对应的认证信息
 * <p>
 * 注意：这是一个简化实现，实际生产环境应使用Redis等分布式缓存
 *
 * @author charmingdaidai
 */
@Service
@Slf4j
public class AuthCodeStorageServiceImpl implements AuthCodeStorageService {

    // 授权码过期时间（秒）
    private static final long CODE_EXPIRATION_SECONDS = 120; // 2分钟
    // 存储格式：授权码 -> (认证响应, 过期时间)
    private final Map<String, AuthCodeEntry> codeStore = new ConcurrentHashMap<>();

    @Override
    public void storeAuthCode(String code, AuthenticationResponse response) {
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(CODE_EXPIRATION_SECONDS);
        codeStore.put(code, new AuthCodeEntry(response, expiryTime));
        log.debug("存储授权码: {}, 过期时间: {}", code, expiryTime);

        // 定期清理过期的授权码（简化实现）
        cleanupExpiredCodes();
    }

    @Override
    public AuthenticationResponse getAndRemoveAuthResponse(String code) {
        if (code == null || !codeStore.containsKey(code)) {
            log.warn("无效的授权码: {}", code);
            return null;
        }

        AuthCodeEntry entry = codeStore.get(code);

        // 检查是否过期
        if (entry.isExpired()) {
            codeStore.remove(code);
            log.warn("授权码已过期: {}", code);
            return null;
        }

        // 获取并移除（一次性使用）
        codeStore.remove(code);
        log.debug("成功使用并移除授权码: {}", code);
        return entry.getResponse();
    }

    /**
     * 清理过期的授权码
     */
    private void cleanupExpiredCodes() {
        codeStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 授权码条目，包含认证响应和过期时间
     */
    private static class AuthCodeEntry {
        private final AuthenticationResponse response;
        private final LocalDateTime expiryTime;

        public AuthCodeEntry(AuthenticationResponse response, LocalDateTime expiryTime) {
            this.response = response;
            this.expiryTime = expiryTime;
        }

        public AuthenticationResponse getResponse() {
            return response;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}