package com.mtmn.smartdoc.constants;

/**
 * 应用常量类
 * 
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-25
 */
public final class AppConstants {

    private AppConstants() {
        // 私有构造函数，防止实例化
        throw new AssertionError("Cannot instantiate constants class");
    }

    // ==================== 系统常量 ====================

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final int MAX_PAGE_SIZE = 100;


    // ==================== 业务常量 ====================

    /**
     * 默认向量维度
     */
    public static final int DEFAULT_EMBEDDING_DIMENSION = 1024;

    /**
     * 默认检索数量
     */
    public static final int DEFAULT_TOP_K = 5;

    /**
     * 默认相似度阈值
     */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;

    /**
     * 批量操作最大大小
     */
    public static final int MAX_BATCH_SIZE = 1000;


    // ==================== 文件相关常量 ====================

    /**
     * 支持的文档类型
     */
    public static final class FileTypes {
        public static final String PDF = "application/pdf";
        public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        public static final String DOC = "application/msword";
        public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        public static final String XLS = "application/vnd.ms-excel";
        public static final String PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        public static final String PPT = "application/vnd.ms-powerpoint";
        public static final String TXT = "text/plain";
        public static final String MD = "text/markdown";
    }

    /**
     * 文件大小限制
     */
    public static final class FileSizeLimit {
        /**
         * 单个文件最大大小（100MB）
         */
        public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

        /**
         * 批量上传总大小限制（500MB）
         */
        public static final long MAX_BATCH_SIZE = 500 * 1024 * 1024;
    }


    // ==================== Milvus 集合名称 ====================

    /**
     * Milvus 集合名称模板
     */
    public static final class Milvus {
        /**
         * Chunk 集合名称模板：kb_{kbId}_chunks
         */
        public static final String CHUNKS_TEMPLATE = "smart_rag_%d_chunks";

        /**
         * TreeNode 集合名称模板：kb_{kbId}_nodes
         */
        public static final String NODES_TEMPLATE = "smart_rag_%d_nodes";

        public static final Integer INSERT_BATCH_SIZE = 1000;
    }


    // ==================== 提示词模板 ====================

    /**
     * AI 提示词模板
     */
    public static final class PromptTemplates {

        /**
         * 查询扩展提示词
         */
        public static final String QUERY_EXPANSION = """
                请根据用户的原始查询，生成 3 个相关的扩展查询。
                这些扩展查询应该：
                1. 使用不同的表达方式
                2. 包含相关的同义词或近义词
                3. 从不同角度理解原始问题
                
                原始查询：{query}
                
                请以 JSON 数组格式返回扩展查询，例如：
                ["扩展查询1", "扩展查询2", "扩展查询3"]
                """;

        /**
         * 文档摘要提示词
         */
        public static final String DOCUMENT_SUMMARY = """
                请为以下文档内容生成一个简洁的摘要（不超过 200 字）：
                
                {content}
                
                摘要应该：
                1. 准确概括文档的主要内容
                2. 保留关键信息和核心观点
                3. 使用简洁清晰的语言
                """;

        /**
         * RAG 回答生成提示词
         */
        public static final String RAG_ANSWER = """
                你是一个专业的智能助手。请根据以下检索到的相关文档，回答用户的问题。
                
                用户问题：{query}
                
                相关文档：
                {context}
                
                要求：
                1. 基于提供的文档内容回答，不要编造信息
                2. 如果文档中没有相关信息，请明确说明
                3. 回答要准确、简洁、易懂
                4. 必要时可以引用文档中的原文
                """;

        /**
         * 关键词提取提示词
         */
        public static final String KEYWORD_EXTRACTION = """
                请从以下文本中提取 5-10 个最重要的关键词：
                
                {text}
                
                要求：
                1. 关键词应能代表文本的核心主题
                2. 优先选择专业术语和核心概念
                3. 以 JSON 数组格式返回，例如：["关键词1", "关键词2"]
                """;
    }


    // ==================== 缓存相关常量 ====================

    /**
     * 缓存配置
     */
    public static final class Cache {
        /**
         * 缓存名称
         */
        public static final String EMBEDDING_CACHE = "embedding_cache";
        public static final String MODEL_CONFIG_CACHE = "model_config_cache";
        public static final String KNOWLEDGE_BASE_CACHE = "kb_cache";

        /**
         * 缓存过期时间（秒）
         */
        public static final long EMBEDDING_EXPIRE_SECONDS = 3600;  // 1小时
        public static final long MODEL_CONFIG_EXPIRE_SECONDS = 1800;  // 30分钟
        public static final long KB_EXPIRE_SECONDS = 600;  // 10分钟
    }


    // ==================== 重试配置 ====================

    /**
     * 重试策略配置
     */
    public static final class Retry {
        /**
         * 最大重试次数
         */
        public static final int MAX_ATTEMPTS = 3;

        /**
         * 重试延迟（毫秒）
         */
        public static final long RETRY_DELAY_MS = 1000;

        /**
         * Milvus 连接重试次数
         */
        public static final int MILVUS_MAX_ATTEMPTS = 3;
    }


    // ==================== 错误消息 ====================

    /**
     * 错误消息模板
     */
    public static final class ErrorMessages {
        public static final String FILE_TOO_LARGE = "文件大小超过限制：%d MB";
        public static final String UNSUPPORTED_FILE_TYPE = "不支持的文件类型：%s";
        public static final String MILVUS_CONNECTION_FAILED = "Milvus 连接失败，请检查服务状态";
        public static final String DOCUMENT_NOT_FOUND = "文档不存在：%d";
        public static final String KNOWLEDGE_BASE_NOT_FOUND = "知识库不存在：%d";
        public static final String INVALID_CONFIGURATION = "配置无效：%s";
    }
}