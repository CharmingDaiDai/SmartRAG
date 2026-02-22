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
         * 意图识别提示词（RAGQueryProcessor 使用，%s 占位符格式）
         * 参数顺序：%1$s = 对话历史，%2$s = 当前用户问题
         */
        public static final String INTENT_CLASSIFIER = """
                你是RAG系统的意图识别模块。请分析用户问题是否需要进行知识库检索，并以JSON格式返回结果。

                **需要检索的情况：**
                - 询问具体的技术问题、产品信息、政策规定
                - 需要查找特定文档、数据、资料
                - 询问专业领域知识
                - 请求具体的操作步骤或解决方案

                **不需要检索的情况：**
                - 简单问候：你好、再见、谢谢
                - 日常闲聊：天气、心情、随便聊聊
                - 对前一个回答的追问、澄清、举例要求
                - 通用常识问题（如基础数学、常见概念）
                - 系统功能询问：怎么使用、帮助说明

                **对话历史：**
                %s

                **当前用户问题：**
                %s

                **分析要求：**
                1. 仔细考虑是否是对前一轮回答的追问
                2. 评估问题的复杂度和专业性
                3. 如果不确定，倾向于需要检索

                **严格按照以下JSON格式返回，不要包含任何其他内容：**
                {
                  "needRetrieval": true/false,
                  "reason": "简短说明判断原因",
                  "questionType": "问题类型分类"
                }
                """;

        /**
         * 查询分解提示词（QueryDecompose 使用，%s 占位符格式）
         * 参数顺序：%1$s = 用户问题
         */
        public static final String QUERY_DECOMPOSE_ADVANCED = """
                你是一个专业的查询规划助手，你的任务是将用户的复杂问题分解为一个包含多个步骤的执行计划。这个计划旨在先通过检索获取必要信息，然后综合这些信息来回答原始问题。

                计划中的每个步骤都应被归类为以下两种类型之一：
                - "检索": 用于直接从知识库中查找事实、定义或数据的子问题。
                - "回答": 用于综合所有"检索"步骤获得的信息，进行比较、总结或推断，最终形成对原始问题的回答。这通常是计划的最后一步。

                请严格按照以下 JSON 格式返回一个数组，其中每个对象代表一个步骤：
                [
                  {"type": "检索" 或 "回答", "query": "子问题内容"}
                ]

                示例:

                用户问题:
                微软和谷歌去年哪个公司的利润更高？

                执行计划 (JSON输出):
                [
                  {
                    "type": "检索",
                    "query": "微软去年赚了多少钱？"
                  },
                  {
                    "type": "检索",
                    "query": "谷歌去年赚了多少钱？"
                  },
                  {
                    "type": "回答",
                    "query": "微软和谷歌去年哪个公司的利润更高？"
                  }
                ]

                现在，请为以下问题制定执行计划：

                用户问题:
                %s

                执行计划 (JSON输出):
                """;

        /**
         * 叶子节点知识点与摘要提取提示词（HiSem 索引构建）
         */
        public static final String HISEM_LEAF_EXTRACTION = """
                # Role
                你是一个专业的知识提取专家。请从以下内容中提取核心知识点和简要摘要。
                
                # Input
                <content>
                {content}
                </content>
                
                # Rules
                1. **知识点**：提取 3-8 个最重要的知识点，每个知识点是一个简洁的短语或句子。
                2. **摘要**：用一句话概括本段内容的核心主题（不超过 50 字）。
                3. **格式**：必须仅输出 JSON，不要包含任何其他解释。
                
                # Output Format
                {"keyKnowledge": ["知识点1", "知识点2", ...], "summary": "一句话摘要"}
                
                # Result
                """;

        /**
         * 非叶子节点聚合摘要提示词（HiSem 索引构建）
         */
        public static final String HISEM_PARENT_AGGREGATION = """
                # Role
                你是一个专业的知识整合专家。请根据多个子章节的摘要和知识点，综合归纳出本章节的核心知识点和概括性摘要。
                
                # Input
                <children_summaries>
                {children_summaries}
                </children_summaries>
                
                # Rules
                1. **知识点**：综合提炼 3-8 个最重要的核心知识点，涵盖各子章节的主要内容。
                2. **摘要**：用一句话概括本章节的整体主题（不超过 60 字）。
                3. **格式**：必须仅输出 JSON，不要包含任何其他解释。
                
                # Output Format
                {"keyKnowledge": ["知识点1", "知识点2", ...], "summary": "一句话摘要"}
                
                # Result
                """;

        /**
         * SADP 问题复杂度判断提示词
         */
        public static final String SADP_COMPLEXITY_CHECK = """
                # Role
                你是一个查询分析专家。请判断以下问题是否需要多步推理或多跳检索才能回答。
                
                # Input
                <query>
                {query}
                </query>
                
                # Rules
                1. **复杂问题（true）**：需要分别检索多个独立主题，再综合推理；或需要前一个检索结果才能进行下一步检索；或问题明确包含"比较"、"关系"、"原因和影响"等多跳逻辑。
                2. **简单问题（false）**：问题只涉及单一主题，一次检索即可回答；或问题是单纯的概念解释、定义查询。
                3. **格式**：必须仅输出 JSON，不要包含任何其他解释。
                
                # Examples
                Query: "Redis 的持久化有哪些方式？" -> {"complex": false, "reason": "单一概念查询"}
                Query: "比较 MySQL 和 PostgreSQL 的锁机制，以及各自在高并发场景下的优缺点" -> {"complex": true, "reason": "需要分别检索两个系统并进行对比推理"}
                Query: "导致系统故障的原因是什么，以及如何从故障中恢复？" -> {"complex": true, "reason": "需要分别检索故障原因和恢复方案"}
                
                # Result
                """;

        /**
         * SADP DAG 任务分解提示词
         */
        public static final String SADP_DAG_DECOMPOSITION = """
                # Role
                你是一个任务规划专家。请将以下复杂问题拆解为若干个独立的子检索任务，每个子任务是一个具体的检索查询，并描述任务间的依赖关系。
                
                # Input
                <query>
                {query}
                </query>
                
                # Rules
                1. **子任务**：每个子任务描述一个独立的检索目标，应当简洁具体。
                2. **依赖关系**：如果某个任务需要前置任务的结果才能执行，则在 dependsOn 中列出前置任务 ID；若无依赖则为空数组。
                3. **数量**：子任务数量应在 2-5 个之间，不要过度拆分。
                4. **格式**：必须仅输出 JSON 数组，不要包含任何其他解释。
                
                # Output Format
                [
                  {"id": "t1", "description": "检索任务描述1", "dependsOn": []},
                  {"id": "t2", "description": "检索任务描述2", "dependsOn": []},
                  {"id": "t3", "description": "综合t1和t2的结果进行推理", "dependsOn": ["t1", "t2"]}
                ]
                
                # Result
                """;

        /**
         * SADP 子任务综合答案提示词
         */
        public static final String SADP_SUBTASK_ANSWER = """
                # Role
                你是一个专业的知识助手。请根据检索到的参考文档，回答以下子任务问题。
                
                # Input
                子任务：{subtask}
                
                前置任务结果：
                <prior_results>
                {prior_results}
                </prior_results>
                
                参考文档：
                <documents>
                {context}
                </documents>
                
                # Rules
                1. 基于参考文档和前置任务结果回答子任务问题。
                2. 如果文档中没有相关信息，明确说明"文档中未找到相关信息"。
                3. 回答应简洁，聚焦于子任务目标。
                4. 直接输出答案，不要有开场白。
                
                # Answer
                """;

        /**
         * SADP 最终综合答案提示词
         */
        public static final String SADP_FINAL_SYNTHESIS = """
                # Role
                你是一个专业的智能助手。请根据多个子任务的执行结果，综合回答用户的原始问题。
                
                # Input
                原始问题：
                <query>
                {query}
                </query>
                
                子任务执行结果：
                <subtask_results>
                {subtask_results}
                </subtask_results>
                
                # Rules
                1. **综合推理**：整合所有子任务的结果，给出完整、连贯的回答。
                2. **结构清晰**：回答应逻辑清晰，分点表述。
                3. **诚实原则**：如果子任务结果中有"未找到相关信息"，在回答中明确指出该部分无法回答。
                4. **拒绝客套**：直接输出答案正文，不要有任何前缀或开场白。
                
                # Answer
                (直接开始回答)：
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