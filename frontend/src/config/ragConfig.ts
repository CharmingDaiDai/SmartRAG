export const RAG_METHODS = {
  NAIVE: "naive",
  HISEM_FAST: "hisem-fast",
  HISEM: "hisem",
};

export const RAG_METHOD_OPTIONS = [
  { value: RAG_METHODS.NAIVE, label: "Naive RAG" },
  { value: RAG_METHODS.HISEM_FAST, label: "HiSem RAG Fast" },
  { value: RAG_METHODS.HISEM, label: "HiSem-SADP" },
];

export const RAG_STRATEGIES = {
  NAIVE_RAG: "NAIVE_RAG",
  HISEM_RAG: "HISEM_RAG",
  HISEM_RAG_FAST: "HISEM_RAG_FAST",
} as const;

export type RagStrategyType = (typeof RAG_STRATEGIES)[keyof typeof RAG_STRATEGIES];

const STRATEGY_ALIAS_MAP: Record<string, RagStrategyType> = {
  NAIVE: RAG_STRATEGIES.NAIVE_RAG,
  NAIVE_RAG: RAG_STRATEGIES.NAIVE_RAG,
  HISEM: RAG_STRATEGIES.HISEM_RAG,
  HISEM_RAG: RAG_STRATEGIES.HISEM_RAG,
  HISEM_SADP: RAG_STRATEGIES.HISEM_RAG,
  HISEM_RAG_SADP: RAG_STRATEGIES.HISEM_RAG,
  HISEM_FAST: RAG_STRATEGIES.HISEM_RAG_FAST,
  HISEM_RAG_FAST: RAG_STRATEGIES.HISEM_RAG_FAST,
  HISEMFAST: RAG_STRATEGIES.HISEM_RAG_FAST,
};

export const normalizeStrategyType = (rawType?: string | null): RagStrategyType => {
  const base = (rawType ?? '').trim();
  if (!base) {
    return RAG_STRATEGIES.NAIVE_RAG;
  }

  const normalized = base.toUpperCase().replace(/[\s-]+/g, '_');
  if (normalized in STRATEGY_ALIAS_MAP) {
    return STRATEGY_ALIAS_MAP[normalized];
  }

  // 兜底：兼容历史数据或后端变体命名
  if (normalized.includes('NAIVE')) {
    return RAG_STRATEGIES.NAIVE_RAG;
  }
  if (normalized.includes('FAST')) {
    return RAG_STRATEGIES.HISEM_RAG_FAST;
  }
  if (normalized.includes('HISEM') || normalized.includes('SADP')) {
    return RAG_STRATEGIES.HISEM_RAG;
  }

  return RAG_STRATEGIES.NAIVE_RAG;
};

// 策略类型到方法的映射
export const STRATEGY_TO_METHOD: Record<string, string> = {
  [RAG_STRATEGIES.NAIVE_RAG]: RAG_METHODS.NAIVE,
  [RAG_STRATEGIES.HISEM_RAG]: RAG_METHODS.HISEM,
  [RAG_STRATEGIES.HISEM_RAG_FAST]: RAG_METHODS.HISEM_FAST,
};

export const SPLITTER_TYPES = [
  { label: "按段落切分", value: "BY_PARAGRAPH" },
  { label: "按行切分", value: "BY_LINE" },
  { label: "按句子切分", value: "BY_SENTENCE" },
  { label: "自定义分隔符", value: "BY_SEPARATOR" },
];

export const getMethodConfig = (method: string) => {
  // Naive RAG 索引配置：块大小、重叠大小、分块方式、分隔符、Embedding 模型
  const naiveIndexConfig = [
    // 拖动类型
    {
      key: "chunk_size",
      label: "块大小",
      type: "slider",
      min: 1,
      max: 8192,
      step: 1,
      defaultValue: 512,
      marks: {
        512: "512",
        1024: "1024",
        2048: "2048",
        4096: "4096",
        8192: "8192",
      },
    },
    {
      key: "chunk_overlap",
      label: "重叠大小",
      type: "slider",
      min: 0,
      dynamicMaxRatio: 0.2,
      step: 1,
      defaultValue: 50,
    },
    // 选择类型
    {
      key: "splitter_type",
      label: "分块方式",
      type: "select",
      options: SPLITTER_TYPES,
      defaultValue: "BY_PARAGRAPH",
    },
    {
      key: "separator",
      label: "分隔符",
      type: "input",
      defaultValue: "\\n\\n",
      dependency: { field: "splitter_type", value: "BY_SEPARATOR" },
    },
    {
      key: "embedding_model",
      label: "Embedding 模型",
      type: "model_select",
      modelType: "embedding",
      defaultValue: "",
    },
  ];

  // HiSem RAG Fast 索引配置：块大小、标题语义增强、Embedding 模型
  const hisemFastIndexConfig = [
    // 拖动类型
    {
      key: "chunk_size",
      label: "块大小",
      type: "slider",
      min: 1,
      max: 8192,
      step: 1,
      defaultValue: 512,
      marks: {
        512: "512",
        1024: "1024",
        2048: "2048",
        4096: "4096",
        8192: "8192",
      },
    },
    // 开关类型
    {
      key: "enableTitleEnhancement",
      label: "标题语义增强",
      type: "switch",
      defaultValue: true,
    },
    // 选择类型
    {
      key: "embedding_model",
      label: "Embedding 模型",
      type: "model_select",
      modelType: "embedding",
      defaultValue: "",
    },
  ];

  // HiSem-SADP 索引配置：块大小、标题语义增强、语义压缩、Embedding 模型、LLM 模型
  const hisemIndexConfig = [
    // 拖动类型
    {
      key: "chunk_size",
      label: "块大小",
      type: "slider",
      min: 1,
      max: 8192,
      step: 1,
      defaultValue: 512,
      marks: {
        512: "512",
        1024: "1024",
        2048: "2048",
        4096: "4096",
        8192: "8192",
      },
    },
    // 开关类型
    {
      key: "enableTitleEnhancement",
      label: "标题语义增强",
      type: "switch",
      defaultValue: true,
    },
    {
      key: "enableSemanticCompression",
      label: "语义压缩",
      type: "switch",
      defaultValue: true,
      tooltip: "用大模型提取摘要",
    },
    // 选择类型
    {
      key: "embedding_model",
      label: "Embedding 模型",
      type: "model_select",
      modelType: "embedding",
      defaultValue: "",
    },
    {
      key: "llm_model_id",
      label: "LLM 模型",
      type: "model_select",
      modelType: "llm",
      defaultValue: "",
      dependency: { field: "enableSemanticCompression", value: true },
      tooltip: "用于生成摘要和知识点",
    },
  ];

  const commonSearchConfig = [
    // 开关类型
    {
      key: "enable_rerank",
      label: "开启重排序",
      type: "switch",
      defaultValue: false,
    },
    {
      key: "enableIntentRecognition",
      label: "意图识别",
      type: "switch",
      defaultValue: false,
    },
    {
      key: "enableQueryRewrite",
      label: "查询重写",
      type: "switch",
      defaultValue: false,
    },
    {
      key: "enableQueryDecomposition",
      label: "查询分解",
      type: "switch",
      defaultValue: false,
    },
    {
      key: "enableHyde",
      label: "Hyde 模式",
      type: "switch",
      defaultValue: false,
    },
    {
      key: "historyWindow",
      label: "历史对话轮数",
      type: "slider",
      min: 1,
      max: 20,
      step: 1,
      defaultValue: 8,
      section: "advanced",
      description: "控制后端从当前会话中读取的最近历史轮数",
    },
    // 选择类型
    {
      key: "rerankModelId",
      label: "Rerank 模型",
      type: "model_select",
      modelType: "rerank",
      defaultValue: "",
      dependency: { field: "enable_rerank", value: true },
    },
    {
      key: "llmModelId",
      label: "LLM 模型",
      type: "model_select",
      modelType: "llm",
      defaultValue: "",
    },
  ];

  const configs: Record<string, any> = {
    [RAG_METHODS.NAIVE]: {
      name: "Naive RAG",
      description: "基础检索增强生成",
      indexConfig: naiveIndexConfig,
      searchConfig: [
        {
          key: "topK",
          label: "Top K",
          type: "slider",
          min: 1,
          max: 20,
          step: 1,
          defaultValue: 5,
        },
        {
          key: "threshold",
          label: "相似度阈值",
          type: "slider",
          min: 0,
          max: 1,
          step: 0.01,
          defaultValue: 0,
        },
        ...commonSearchConfig,
      ],
    },
    [RAG_METHODS.HISEM_FAST]: {
      name: "HiSem RAG Fast",
      description: "层级语义检索 快速版",
      indexConfig: hisemFastIndexConfig,
      searchConfig: [
        {
          key: "maxTopK",
          label: "最大结果数",
          type: "slider",
          min: 1,
          max: 20,
          step: 1,
          defaultValue: 10,
        },
        ...commonSearchConfig,
      ],
    },
    [RAG_METHODS.HISEM]: {
      name: "HiSem-SADP",
      description: "层级语义检索",
      indexConfig: hisemIndexConfig,
      searchConfig: [
        {
          key: "maxTopK",
          label: "最大结果数",
          type: "slider",
          min: 1,
          max: 20,
          step: 1,
          defaultValue: 10,
        },
        ...[
          // 开关类型
          {
            key: "enable_rerank",
            label: "开启重排序",
            type: "switch",
            defaultValue: false,
          },
          {
            key: "enableIntentRecognition",
            label: "意图识别",
            type: "switch",
            defaultValue: false,
          },
          // 选择类型
          {
            key: "rerankModelId",
            label: "Rerank 模型",
            type: "model_select",
            modelType: "rerank",
            defaultValue: "",
            dependency: { field: "enable_rerank", value: true },
          },
          {
            key: "llmModelId",
            label: "LLM 模型",
            type: "model_select",
            modelType: "llm",
            defaultValue: "",
          },
        ],
      ],
    },
  };

  // 支持 RAG_STRATEGIES 和 RAG_METHODS 两种格式
  const normalizedMethod = STRATEGY_TO_METHOD[method] || method;

  return configs[normalizedMethod] || configs[RAG_METHODS.NAIVE];
};
