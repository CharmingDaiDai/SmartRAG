package com.mtmn.smartrag.service;

import com.mtmn.smartrag.dto.ChangePasswordRequest;
import com.mtmn.smartrag.dto.UpdateProfileRequest;
import com.mtmn.smartrag.dto.UserProfileDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务接口
 * 处理用户相关的业务逻辑，包括个人资料管理、头像上传、密码修改等
 *
 * @author charmingdaidai
 */
public interface UserService {

    /**
     * 获取用户个人资料
     *
     * @param username 用户名
     * @return 用户个人资料DTO
     */
    UserProfileDto getUserProfile(String username);

    /**
     * 更新用户个人资料
     *
     * @param username 用户名
     * @param request  更新请求
     * @return 更新后的用户个人资料DTO
     */
    UserProfileDto updateProfile(String username, UpdateProfileRequest request);

    /**
     * 上传用户头像
     *
     * @param username 用户名
     * @param file     上传的头像文件
     * @return 更新后的用户个人资料DTO
     */
    UserProfileDto uploadAvatar(String username, MultipartFile file);

    /**
     * 修改用户密码
     *
     * @param username 用户名
     * @param request  修改密码请求
     * @return true表示修改成功
     */
    boolean changePassword(String username, ChangePasswordRequest request);
}