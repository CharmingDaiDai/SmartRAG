-- Create models table for user-defined AI models
CREATE TABLE IF NOT EXISTS models
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    user_id
    BIGINT
    NOT
    NULL,
    model_type
    VARCHAR
(
    20
) NOT NULL COMMENT 'Model type: llm or embedding',
    base_url VARCHAR
(
    255
) NOT NULL COMMENT 'API base URL for the model',
    api_key VARCHAR
(
    255
) NOT NULL COMMENT 'API key for authentication',
    model_name VARCHAR
(
    100
) NOT NULL COMMENT 'Name of the model',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY
(
    user_id
) REFERENCES users
(
    id
)
                                                   ON DELETE CASCADE,
    INDEX idx_user_id
(
    user_id
),
    INDEX idx_model_type
(
    model_type
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE =utf8mb4_unicode_ci COMMENT='User-defined AI models configuration';