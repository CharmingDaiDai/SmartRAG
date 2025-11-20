-- 创建用户表
CREATE TABLE IF NOT EXISTS `users`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT,
    `username` varchar
(
    255
) NOT NULL,
    `password` varchar
(
    255
) DEFAULT NULL COMMENT '密码可为空，表示第三方登录用户',
    `email` varchar
(
    255
),
    `avatar_path` varchar
(
    255
) DEFAULT NULL,
    `vip` bit
(
    1
) NOT NULL DEFAULT b'0',
    `github_id` varchar
(
    100
) DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    `last_login` datetime DEFAULT NULL,
    `enabled` bit
(
    1
) NOT NULL DEFAULT b'1',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `UK_users_username`
(
    `username`
),
    UNIQUE KEY `UK_users_email`
(
    `email`
),
    UNIQUE KEY `UK_users_github_id`
(
    `github_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_general_ci;

-- 添加索引用于快速查询
CREATE INDEX idx_users_created_at ON users (created_at);
CREATE INDEX idx_users_enabled ON users (enabled);