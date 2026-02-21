package com.mtmn.smartdoc.dto;

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
@Schema(description = "用户认证请求")
public class AuthenticationRequest {
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
}