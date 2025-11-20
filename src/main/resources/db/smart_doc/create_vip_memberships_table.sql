-- 创建VIP会员表
CREATE TABLE IF NOT EXISTS `vip_memberships`
(
    `id`
    bigint
(
    20
) NOT NULL AUTO_INCREMENT,
    `user_id` bigint
(
    20
) NOT NULL,
    `start_date` datetime NOT NULL,
    `expire_date` datetime NOT NULL,
    `membership_level` varchar
(
    50
) NOT NULL DEFAULT 'basic' COMMENT '会员等级，如：basic, premium, enterprise',
    `auto_renew` bit
(
    1
) NOT NULL DEFAULT b'0' COMMENT '是否自动续费',
    `payment_reference` varchar
(
    255
) DEFAULT NULL COMMENT '支付参考号',
    `status` varchar
(
    20
) NOT NULL DEFAULT 'active' COMMENT '状态：active, expired, cancelled',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT NULL,
    PRIMARY KEY
(
    `id`
),
    KEY `FK_vip_memberships_user`
(
    `user_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_general_ci COMMENT='VIP会员表';

-- 添加索引用于快速查询
CREATE INDEX idx_vip_memberships_status ON vip_memberships (status);
CREATE INDEX idx_vip_memberships_expire_date ON vip_memberships (expire_date);
CREATE INDEX idx_vip_memberships_level_status ON vip_memberships (membership_level, status);