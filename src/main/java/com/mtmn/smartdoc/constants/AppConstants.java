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

        /**
         * 元数据字段：文档ID
         */
        public static final String METADATA_FIELD_DOC_ID = "document_id";
    }


    // ==================== 提示词模板 ====================

    /**
     * AI 提示词模板
     */
    public static final class PromptTemplates {

        /**
         * 意图识别提示词
         */
        public static final String PROMPT_INTENT_RECOGNITION = """
                # Role
                你是一个对话意图分类器。你的任务是判断用户的输入是否需要检索外部知识库来回答。
                
                # Input
                对话历史：
                <history>
                {history}
                </history>
                
                当前问题：
                <query>
                {query}
                </query>
                
                # Rules
                1. **需要检索 (SEARCH)**：当用户询问具体事实、知识概念、数据、流程，或者虽然是追问但需要补充新信息时。
                2. **无需检索 (CHAT)**：当用户进行闲聊（如打招呼、感谢）、确认信息、请求重写文案、或者完全基于上文进行简单的逻辑推理时。
                3. **格式**：必须且仅输出 JSON，不要包含任何其他解释。
                
                # Output Example
                {"action": "SEARCH", "reason": "用户询问具体技术概念"}
                {"action": "CHAT", "reason": "用户仅是在表示感谢"}
                
                # Result
                """;

        /**
         * 查询重写提示词
         */
        public static final String PROMPT_QUERY_REWRITE = """
                # Role
                你是一个搜索引擎优化专家。你的任务是将用户的口语化问题重写为高效的搜索引擎关键词。
                
                # Input
                对话历史：
                <history>
                {history}
                </history>
                
                当前问题：
                <query>
                {query}
                </query>
                
                # Rules
                1. **去噪**：去除“请问”、“是什么”、“有什么用”、“我想知道”等无实际语义的词。
                2. **指代消解**：如果当前问题包含“它”、“这个”、“他”等代词，请结合对话历史将其替换为具体的实体名称。
                3. **核心提取**：只保留对语义检索有用的核心实体、名词或动名短语。
                4. **单一输出**：直接输出重写后的查询字符串，不要包含任何解释或标点符号。
                
                # Examples
                User: "Redis 的持久化机制有哪些？" -> Output: "Redis 持久化机制 RDB AOF"
                User: "它有什么缺点？" (上文讨论的是 MySQL) -> Output: "MySQL 缺点"
                User: "帮我找一下关于劳动合同法的规定" -> Output: "劳动合同法 规定"
                
                # Result
                """;

        /**
         * 问题分解提示词
         */
        public static final String PROMPT_QUERY_DECOMPOSITION = """
                # Role
                你是一个搜索策略专家。你的任务是分析用户的复杂问题，提取出需要进行检索的**完整检索主题（Search Topics）**。
                
                # Input
                当前问题：
                <query>
                {query}
                </query>
                
                # Rules
                1. **保持语义完整 (聚合原则)**：对于单一主体的修饰、属性或动作（如“xxx的安装标准”、“xxx的原理”），**严禁拆分**。必须将其保留为一个完整的检索短语。
                   - 错误：["主变压器", "安装", "标准"]
                   - 正确：["主变压器安装标准"]
                2. **仅在多意图时拆分**：只有当问题包含多个**独立**的主体（如“A和B的区别”、“A的原理及B的应用”）时，才进行拆分。
                3. **去口语化**：提取的主题应去除“请问”、“是什么”等无意义词汇，直接保留核心名词短语。
                4. **格式**：必须且仅输出 JSON 字符串数组。
                
                # Examples
                Query: "主变压器安装的工艺标准是什么？"
                Output: ["主变压器安装工艺标准"]
                
                Query: "Spring Boot 和 Spring Cloud 有什么区别？"
                Output: ["Spring Boot", "Spring Cloud"]
                
                Query: "Redis 的 RDB 原理以及 AOF 的优缺点"
                Output: ["Redis RDB 原理", "Redis AOF 优缺点"]
                
                Query: "介绍一下 Kafka 的架构和高可用设计"
                Output: ["Kafka 架构", "Kafka 高可用设计"]
                
                # Result
                """;

        /**
         * HYDE 提示词
         */
        public static final String PROMPT_HYDE = """
                # Role
                你是一个专业的文档生成器。请根据用户的问题，生成一段**假想的**、**通过向量检索可能匹配到的**教科书片段或技术文档段落。
                
                # Input
                当前问题：
                <query>
                {query}
                </query>
                
                # Rules
                1. **内容相关**：生成的段落应包含回答该问题可能涉及的专业术语、关键词和上下文逻辑。
                2. **事实无关**：不需要保证内容的真实性或准确性，我们的目的是利用这段文本去匹配知识库中的真实文档。
                3. **风格模仿**：模仿专业文档、百科全书或技术手册的语气。
                4. **直接输出**：不要输出“以下是假想文档”等前缀，直接输出段落内容。
                
                # Result
                """;

        /**
         * RAG 回答生成提示词
         */
        public static final String RAG_ANSWER = """
                # Role
                你是一个专业的智能助手，专门负责根据参考文档回答用户问题。
                
                # Instructions
                请仔细阅读下方的【参考文档】，并据此回答【用户问题】。
                
                # Rules
                1. **仅依据文档**：完全依赖【参考文档】中的信息。严禁使用你训练数据中的外部知识。
                2. **引用溯源**：回答时必须在句末标注信息来源的文档 ID，格式为 [ID]。
                3. **诚实原则**：如果【参考文档】中没有包含回答问题所需的信息，请直接回答"根据现有文档无法回答该问题"，不要编造。
                4. **结构清晰**：回答要逻辑清晰，分点表述。
                5. **拒绝客套**：**严禁**输出任何开场白（如“根据提供的参考文档...”、“你好”、“以下是回答...”等）。**直接输出答案正文，不要有任何铺垫。**
                
                # Context (参考文档)
                <documents>
                {context}
                </documents>
                
                # Query (用户问题)
                <query>
                {query}
                </query>
                
                # Answer
                (切记：不要有任何前缀或开场白，直接开始回答)：
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