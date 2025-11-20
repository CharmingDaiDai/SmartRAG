-- =====================================================
-- SmartRAG V2.0 数据库结构创建脚本
-- =====================================================
-- 作者: charmingdaidai
-- 日期: 2025-11-20
-- 描述: 创建 SmartRAG V2.0 的完整数据库结构
-- MySQL 版本要求: >= 5.7 (支持 JSON 类型)
-- 字符集: utf8mb4
-- 数据库名: smart_rag
-- 
-- 使用说明:
-- 1. 先备份现有数据库
-- 2. 执行本脚本创建新数据库和表结构
-- 3. 手动迁移需要保留的数据
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `smart_rag`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `smart_rag`;

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. 用户相关表
-- =====================================================

-- 用户表
CREATE TABLE IF NOT EXISTS `users`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(255) NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255)          DEFAULT NULL COMMENT '密码(第三方登录用户可为空)',
    `email`       VARCHAR(255)          DEFAULT NULL COMMENT '邮箱',
    `avatar_path` VARCHAR(255)          DEFAULT NULL COMMENT '头像路径',
    `vip`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否VIP',
    `github_id`   VARCHAR(100)          DEFAULT NULL COMMENT 'GitHub ID',
    `created_at`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login`  DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `enabled`     TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_github_id` (`github_id`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';


-- VIP会员表
CREATE TABLE IF NOT EXISTS `vip_memberships`
(
    `id`                BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT      NOT NULL COMMENT '用户ID',
    `start_date`        DATETIME    NOT NULL COMMENT '开始日期',
    `expire_date`       DATETIME    NOT NULL COMMENT '过期日期',
    `membership_level`  VARCHAR(50) NOT NULL DEFAULT 'basic' COMMENT '会员等级: basic, premium, enterprise',
    `auto_renew`        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否自动续费',
    `payment_reference` VARCHAR(255)         DEFAULT NULL COMMENT '支付参考号',
    `status`            VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active, expired, cancelled',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `fk_vip_user` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_date` (`expire_date`),
    KEY `idx_level_status` (`membership_level`, `status`),
    CONSTRAINT `fk_vip_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='VIP会员表';


-- 用户活动记录表
CREATE TABLE IF NOT EXISTS `user_activities`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT      NOT NULL COMMENT '用户ID',
    `activity_type` VARCHAR(50) NOT NULL COMMENT '活动类型: summary, keywords, security, polish, upload, download',
    `document_id`   BIGINT               DEFAULT NULL COMMENT '相关文档ID',
    `document_name` VARCHAR(255)         DEFAULT NULL COMMENT '文档名称',
    `description`   VARCHAR(500)         DEFAULT NULL COMMENT '活动描述',
    `created_at`    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `fk_activity_user` (`user_id`),
    KEY `idx_activity_type` (`activity_type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_activity_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户活动记录表';


-- =====================================================
-- 2. 模型配置表
-- =====================================================

-- 用户自定义模型表
CREATE TABLE IF NOT EXISTS `models`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `model_type` VARCHAR(20)  NOT NULL COMMENT '模型类型: llm, embedding',
    `base_url`   VARCHAR(255) NOT NULL COMMENT 'API基础URL',
    `api_key`    VARCHAR(255) NOT NULL COMMENT 'API密钥',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `fk_model_user` (`user_id`),
    KEY `idx_model_type` (`model_type`),
    CONSTRAINT `fk_model_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户自定义AI模型配置表';


-- =====================================================
-- 3. 知识库相关表 (V2.0 核心)
-- =====================================================

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_bases`
(
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`               BIGINT       NOT NULL COMMENT '用户ID',
    `name`                  VARCHAR(255) NOT NULL COMMENT '知识库名称',
    `description`           VARCHAR(500)          DEFAULT NULL COMMENT '知识库描述',
    `index_strategy_type`   VARCHAR(50)  NOT NULL COMMENT '索引策略类型: NAIVE_RAG, HISEM_RAG',
    `index_strategy_config` JSON                  DEFAULT NULL COMMENT '策略配置参数(JSON格式)',
    `embedding_model_id`    VARCHAR(255) NOT NULL COMMENT 'Embedding模型ID',
    `status`                VARCHAR(50)  NOT NULL DEFAULT 'READY' COMMENT '状态: CREATING, READY, INDEXING, INDEXED, ERROR',
    `storage_metadata`      JSON                  DEFAULT NULL COMMENT '存储元信息(Milvus collection等)',
    `created_at`            DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `fk_kb_user` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_strategy_type` (`index_strategy_type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_kb_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='知识库表';


-- 文档表
CREATE TABLE IF NOT EXISTS `documents`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `kb_id`        BIGINT       NOT NULL COMMENT '知识库ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `filename`     VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_type`    VARCHAR(50)           DEFAULT NULL COMMENT '文件类型: pdf, docx, txt, md',
    `file_size`    BIGINT                DEFAULT NULL COMMENT '文件大小(字节)',
    `file_path`    VARCHAR(500) NOT NULL COMMENT '文件路径(MinIO)',
    `index_status` VARCHAR(50)  NOT NULL DEFAULT 'UPLOADED' COMMENT '索引状态: UPLOADED, PARSING, PARSED, INDEXING, INDEXED, ERROR',
    `metadata`     JSON                  DEFAULT NULL COMMENT '文档元数据(摘要、关键词等)',
    `upload_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `fk_doc_kb` (`kb_id`),
    KEY `fk_doc_user` (`user_id`),
    KEY `idx_index_status` (`index_status`),
    KEY `idx_upload_time` (`upload_time`),
    CONSTRAINT `fk_doc_kb` FOREIGN KEY (`kb_id`) REFERENCES `knowledge_bases` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_doc_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文档表';


-- 分块表 (NaiveRAG 使用)
CREATE TABLE IF NOT EXISTS `chunks`
(
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `kb_id`       BIGINT NOT NULL COMMENT '知识库ID',
    `document_id` BIGINT NOT NULL COMMENT '文档ID',
    `chunk_index` INT    NOT NULL COMMENT '分块序号(从0开始)',
    `content`     TEXT   NOT NULL COMMENT '分块内容',
    `is_modified` TINYINT(1)   DEFAULT 0 COMMENT '是否被用户修改',
    `vector_id`   VARCHAR(255) DEFAULT NULL COMMENT 'Milvus中的向量ID',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `fk_chunk_kb` (`kb_id`),
    KEY `fk_chunk_doc` (`document_id`),
    KEY `idx_vector_id` (`vector_id`),
    KEY `idx_chunk_index` (`document_id`, `chunk_index`),
    CONSTRAINT `fk_chunk_kb` FOREIGN KEY (`kb_id`) REFERENCES `knowledge_bases` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chunk_doc` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='NaiveRAG分块表';


-- 树节点表 (HisemRAG 使用)
CREATE TABLE IF NOT EXISTS `tree_nodes`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `kb_id`             BIGINT       NOT NULL COMMENT '知识库ID',
    `document_id`       BIGINT       NOT NULL COMMENT '文档ID',
    `node_id`           VARCHAR(255) NOT NULL COMMENT '节点唯一标识符',
    `parent_node_id`    VARCHAR(255) DEFAULT NULL COMMENT '父节点ID',
    `title_path`        JSON         NOT NULL COMMENT '多级标题路径: ["一级", "二级", "三级"]',
    `block_index`       INT          DEFAULT NULL COMMENT '文本块索引',
    `level`             INT          NOT NULL COMMENT '层级深度(1=根节点)',
    `node_type`         VARCHAR(50)  NOT NULL COMMENT '节点类型: ROOT, INTERNAL, LEAF',
    `key_knowledge`     TEXT         DEFAULT NULL COMMENT '关键知识点',
    `summary`           TEXT         DEFAULT NULL COMMENT '内容摘要',
    `content`           TEXT         DEFAULT NULL COMMENT '节点内容',
    `content_fragments` JSON         DEFAULT NULL COMMENT '内容片段(JSON数组)',
    `children_ids`      JSON         DEFAULT NULL COMMENT '子节点ID列表(JSON数组)',
    `vector_ids`        JSON         DEFAULT NULL COMMENT '向量ID列表(JSON数组)',
    `created_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_node` (`kb_id`, `node_id`),
    KEY `fk_tree_kb` (`kb_id`),
    KEY `fk_tree_doc` (`document_id`),
    KEY `idx_parent` (`parent_node_id`),
    KEY `idx_level` (`level`),
    KEY `idx_node_type` (`node_type`),
    CONSTRAINT `fk_tree_kb` FOREIGN KEY (`kb_id`) REFERENCES `knowledge_bases` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tree_doc` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='HisemRAG树节点表';


-- =====================================================
-- 4. 对话历史表
-- =====================================================

-- 对话表
CREATE TABLE IF NOT EXISTS `conversations`
(
    `id`               BIGINT NOT NULL AUTO_INCREMENT,
    `kb_id`            BIGINT NOT NULL COMMENT '知识库ID',
    `user_id`          BIGINT       DEFAULT NULL COMMENT '用户ID',
    `session_id`       VARCHAR(255) DEFAULT NULL COMMENT '会话ID',
    `query`            TEXT   NOT NULL COMMENT '用户查询',
    `response`         TEXT         DEFAULT NULL COMMENT '系统回答',
    `retrieval_config` JSON         DEFAULT NULL COMMENT '检索配置(JSON格式)',
    `llm_model_id`     VARCHAR(255) DEFAULT NULL COMMENT '使用的LLM模型ID',
    `rerank_model_id`  VARCHAR(255) DEFAULT NULL COMMENT '使用的Rerank模型ID',
    `retrieved_chunks` JSON         DEFAULT NULL COMMENT '检索到的chunk/node IDs',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `fk_conv_kb` (`kb_id`),
    KEY `fk_conv_user` (`user_id`),
    KEY `idx_session` (`session_id`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_conv_kb` FOREIGN KEY (`kb_id`) REFERENCES `knowledge_bases` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_conv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='对话历史表';


-- =====================================================
-- 5. 添加外键约束到 user_activities (依赖 documents)
-- =====================================================

ALTER TABLE `user_activities`
    ADD CONSTRAINT `fk_activity_doc`
        FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`) ON DELETE SET NULL;


-- =====================================================
-- 6. 创建视图(可选)
-- =====================================================

-- 知识库统计视图
CREATE OR REPLACE VIEW `v_kb_statistics` AS
SELECT kb.id                 AS kb_id,
       kb.name               AS kb_name,
       kb.user_id,
       kb.status,
       COUNT(DISTINCT d.id)  AS document_count,
       COUNT(DISTINCT c.id)  AS chunk_count,
       COUNT(DISTINCT tn.id) AS tree_node_count,
       SUM(d.file_size)      AS total_size,
       MAX(d.upload_time)    AS document_update
FROM knowledge_bases kb
         LEFT JOIN documents d ON kb.id = d.kb_id
         LEFT JOIN chunks c ON kb.id = c.kb_id
         LEFT JOIN tree_nodes tn ON kb.id = tn.kb_id
GROUP BY kb.id, kb.name, kb.user_id, kb.status;


-- =====================================================
-- 完成
-- =====================================================

SET FOREIGN_KEY_CHECKS = 1;

-- 输出提示信息
SELECT 'SmartRAG V2.0 数据库结构创建完成!' AS message;
SELECT '请检查表结构是否符合预期' AS message;