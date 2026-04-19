package com.mtmn.smartrag.service.impl;

import com.mtmn.smartrag.common.CustomException;
import com.mtmn.smartrag.dto.ChangePasswordRequest;
import com.mtmn.smartrag.dto.UpdateProfileRequest;
import com.mtmn.smartrag.dto.UserProfileDto;
import com.mtmn.smartrag.po.User;
import com.mtmn.smartrag.repository.UserRepository;
import com.mtmn.smartrag.service.MinioService;
import com.mtmn.smartrag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户服务实现
 * 处理用户相关的业务逻辑，包括个人资料管理、头像上传、密码修改等
 *
 * @author charmingdaidai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;

    /**
     * 获取用户个人资料
     * <p>
     * 实现思路：
     * 1. 根据用户名从数据库查询用户实体
     * 2. 如果用户不存在则抛出404异常
     * 3. 调用convertToDTO方法将用户实体转换为DTO
     * 4. 包含头像URL等前端需要的完整信息
     *
     * @param username 用户名
     * @return 用户个人资料DTO，包含用户基本信息和头像URL
     * @throws CustomException 当用户不存在时抛出404异常
     */
    @Override
    public UserProfileDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(404, "用户不存在"));

        return convertToDTO(user);
    }

    /**
     * 更新用户个人资料
     * <p>
     * 实现思路：
     * 1. 根据用户名查询用户实体，验证用户存在性
     * 2. 检查邮箱变更情况，防止邮箱冲突
     * 3. 如果邮箱有变更，验证新邮箱是否已被其他用户使用
     * 4. 更新用户的邮箱等可修改字段
     * 5. 保存更新后的用户实体到数据库
     * 6. 转换为DTO返回给前端
     * 7. 使用事务确保数据一致性
     *
     * @param username 用户名
     * @param request  更新请求，包含要修改的用户信息
     * @return 更新后的用户个人资料DTO
     * @throws CustomException 当用户不存在或邮箱冲突时抛出异常
     */
    @Transactional
    @Override
    public UserProfileDto updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(404, "用户不存在"));

        // 如果要更改邮箱，检查邮箱是否已被其他用户使用
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(400, "该邮箱已被使用");
        }

        user.setEmail(request.getEmail());
        // user.setFullName(request.getFullName());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    /**
     * 上传用户头像
     * <p>
     * 实现思路：
     * 1. 验证上传文件的有效性，确保文件不为空
     * 2. 根据用户名查询用户实体，验证用户存在性
     * 3. 检查用户是否已有头像，如果有则先删除旧头像
     * 4. 处理旧头像删除失败的情况，记录警告但不中断流程
     * 5. 调用MinIO服务上传新头像文件
     * 6. 更新用户实体中的头像路径信息
     * 7. 保存更新后的用户实体到数据库
     * 8. 转换为DTO返回，包含新头像的URL
     * 9. 使用事务确保数据一致性，捕获异常并转换为业务异常
     *
     * @param username 用户名
     * @param file     上传的头像文件
     * @return 更新后的用户个人资料DTO，包含新头像URL
     * @throws CustomException 当文件为空、用户不存在或上传失败时抛出异常
     */
    @Transactional
    @Override
    public UserProfileDto uploadAvatar(String username, MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(400, "头像文件不能为空");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(404, "用户不存在"));

        try {
            // 如果用户已有头像，先删除旧头像
            if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
                try {
                    minioService.deleteFile(user.getAvatarPath());
                } catch (Exception e) {
                    log.warn("删除旧头像失败: {}", e.getMessage());
                    // 继续上传新头像，不中断流程
                }
            }

            // 上传新头像到 MinIO
            String filePath = minioService.uploadFile(file);

            // 更新用户头像路径
            user.setAvatarPath(filePath);
            User updatedUser = userRepository.save(user);

            return convertToDTO(updatedUser);
        } catch (Exception e) {
            log.error("上传头像失败: {}", e.getMessage(), e);
            throw new CustomException(500, "上传头像失败");
        }
    }

    /**
     * 修改用户密码
     * <p>
     * 实现思路：
     * 1. 验证新密码与确认密码的一致性
     * 2. 根据用户名查询用户实体，验证用户存在性
     * 3. 使用密码编码器验证当前密码的正确性
     * 4. 使用密码编码器对新密码进行加密
     * 5. 更新用户实体中的密码字段
     * 6. 保存更新后的用户实体到数据库
     * 7. 使用事务确保密码修改的原子性
     *
     * @param username 用户名
     * @param request  修改密码请求，包含当前密码、新密码和确认密码
     * @return true表示修改成功
     * @throws CustomException 当密码不一致、用户不存在或当前密码错误时抛出异常
     */
    @Transactional
    @Override
    public boolean changePassword(String username, ChangePasswordRequest request) {
        // 验证新密码与确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new CustomException(400, "新密码与确认密码不一致");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(404, "用户不存在"));

        // 验证当前密码是否正确
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(401, "当前密码不正确");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return true;
    }

    /**
     * 将用户实体转换为DTO
     * <p>
     * 实现思路：
     * 1. 使用Builder模式构建UserProfileDto对象
     * 2. 复制用户实体中的基本信息字段
     * 3. 检查用户是否有头像路径
     * 4. 如果有头像，调用MinIO服务生成可访问的头像URL
     * 5. 将头像URL设置到DTO中
     * 6. 过滤敏感信息，只包含前端需要的字段
     *
     * @param user 用户实体对象
     * @return 用户个人资料DTO，包含前端展示所需的用户信息
     */
    private UserProfileDto convertToDTO(User user) {
        UserProfileDto dto = UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                // .fullName(user.getFullName())
                .vip(user.isVip())
                .build();

        // 如果用户有头像，获取头像URL
        if (user.getAvatarPath() != null && !user.getAvatarPath().isEmpty()) {
            String avatarUrl = minioService.getPresignedUrl(user.getAvatarPath(), -1);
            dto.setAvatarUrl(avatarUrl);
        }

        return dto;
    }
}