package com.mtmn.smartrag.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GitHub用户信息响应
 *
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "GitHub用户信息响应")
public class GitHubUserInfoResponse {
    @Schema(description = "用户ID")
    private Long id;
    @Schema(description = "登录名")
    private String login;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "邮箱")
    private String email;

    @JsonProperty("avatar_url")
    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "位置")
    private String location;
    @Schema(description = "简介")
    private String bio;

    @JsonProperty("public_repos")
    @Schema(description = "公共仓库数")
    private Integer publicRepos;

    @JsonProperty("public_gists")
    @Schema(description = "公共Gist数")
    private Integer publicGists;

    @Schema(description = "粉丝数")
    private Integer followers;
    @Schema(description = "关注数")
    private Integer following;

    @JsonProperty("created_at")
    @Schema(description = "创建时间")
    private String createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "更新时间")
    private String updatedAt;
}