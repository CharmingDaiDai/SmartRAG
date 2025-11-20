-- 创建用户活动记录表
CREATE TABLE IF NOT EXISTS user_activities
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    activity_type VARCHAR(50) NOT NULL COMMENT '活动类型，如：summary, keywords, security, polish, upload, download',
    document_id   BIGINT COMMENT '相关文档ID',
    document_name VARCHAR(255) COMMENT '文档名称',
    description   VARCHAR(500) COMMENT '活动描述',
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户活动记录表';

-- 添加索引用于快速查询
CREATE INDEX idx_user_activities_user_id ON user_activities (user_id);
CREATE INDEX idx_user_activities_document_id ON user_activities (document_id);
CREATE INDEX idx_user_activities_created_at ON user_activities (created_at);