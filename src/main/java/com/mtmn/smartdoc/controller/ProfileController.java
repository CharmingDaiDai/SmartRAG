package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.dto.ChangePasswordRequest;
import com.mtmn.smartdoc.dto.UpdateProfileRequest;
import com.mtmn.smartdoc.dto.UserProfileDto;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户个人资料控制器
 *
 * @author charmingdaidai
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "个人资料", description = "用户个人资料管理接口")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "获取个人资料", description = "获取当前登录用户的个人资料")
    public ApiResponse<UserProfileDto> getUserProfile(@AuthenticationPrincipal User user) {
        log.info("获取用户个人资料: {}", user.getUsername());
        UserProfileDto profile = userService.getUserProfile(user.getUsername());
        return ApiResponse.success(profile);
    }

    @PutMapping
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的个人资料")
    public ApiResponse<UserProfileDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequest request) {

        log.info("更新用户个人资料: {}", user.getUsername());
        try {
            UserProfileDto updatedProfile = userService.updateProfile(user.getUsername(), request);
            return ApiResponse.success("个人资料更新成功", updatedProfile);
        } catch (Exception e) {
            log.error("更新个人资料失败: {}", e.getMessage(), e);
            return ApiResponse.error("更新个人资料失败: " + e.getMessage());
        }
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像", description = "上传或更新当前登录用户的头像")
    public ApiResponse<UserProfileDto> uploadAvatar(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {

        log.info("上传用户头像: {}", user.getUsername());
        try {
            UserProfileDto updatedProfile = userService.uploadAvatar(user.getUsername(), file);
            return ApiResponse.success("头像上传成功", updatedProfile);
        } catch (Exception e) {
            log.error("上传头像失败: {}", e.getMessage(), e);
            return ApiResponse.error("上传头像失败: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody ChangePasswordRequest request) {

        log.info("修改用户密码: {}", user.getUsername());
        try {
            boolean result = userService.changePassword(user.getUsername(), request);
            return ApiResponse.success("密码修改成功", null);
        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage(), e);
            return ApiResponse.error("修改密码失败: " + e.getMessage());
        }
    }
}