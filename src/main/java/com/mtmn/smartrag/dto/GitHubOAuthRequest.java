package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * @author charmingdaidai
 * @version 1.0
 */
@Data
@Builder
@Schema(description = "GitHub 认证请求")
public class GitHubOAuthRequest {
    @Schema(description = "授权码")
    private String code;
    // 用于防止CSRF攻击的状态值
    @Schema(description = "状态值，用于防止CSRF攻击")
    private String state;
}