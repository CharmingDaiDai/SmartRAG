package com.mtmn.smartdoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改密码请求DTO
 *
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {
    @Schema(description = "当前密码")
    private String currentPassword;
    @Schema(description = "新密码")
    private String newPassword;
    @Schema(description = "确认密码")
    private String confirmPassword;
}