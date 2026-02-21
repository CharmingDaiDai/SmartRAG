CREATE TABLE IF NOT EXISTS `documents`
(
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT,
    `title`             varchar(255) NOT NULL,
    `file_name`         varchar(255) NOT NULL,
    `file_type`         varchar(255) NOT NULL,
    `file_size`         bigint(20)   NOT NULL,
    `file_path`         varchar(255) NOT NULL,
    `summary`           TEXT          DEFAULT NULL,
    `keywords`          varchar(2000) DEFAULT NULL,
    `sensitive_info`    varchar(2000) DEFAULT NULL,
    `categories`        varchar(500)  DEFAULT NULL,
    `indexed`           TINYINT(1)    DEFAULT NULL,
    `user_id`           bigint(20)    DEFAULT NULL,
    `knowledge_base_id` bigint(20)    DEFAULT NULL COMMENT '所属知识库ID',
    `created_at`        datetime      DEFAULT NULL,
    `updated_at`        datetime      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FK_documents_user` (`user_id`),
    KEY `idx_knowledge_base_id` (`knowledge_base_id`),
    CONSTRAINT `FK_documents_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;