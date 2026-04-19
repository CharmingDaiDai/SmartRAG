package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author charmingdaidai
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "令牌刷新请求")
public class TokenRefreshRequest {
    @Schema(description = "刷新令牌")
    private String refreshToken;
}