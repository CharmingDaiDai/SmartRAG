
export const RAG_METHODS = {
  NAIVE: 'naive',
  HISEM_FAST: 'hisem-fast',
  HISEM: 'hisem',
};

export const SPLITTER_TYPES = [
  { label: '按段落切分', value: 'BY_PARAGRAPH' },
  { label: '按行切分', value: 'BY_LINE' },
  { label: '按句子切分', value: 'BY_SENTENCE' },
  { label: '自定义分隔符', value: 'BY_SEPARATOR' },
];

export const getMethodConfig = (method: string) => {
  const commonIndexConfig = [
    {
      key: 'splitter_type',
      label: '分块方式',
      type: 'select',
      options: SPLITTER_TYPES,
      defaultValue: 'BY_PARAGRAPH',
    },
    {
      key: 'separator',
      label: '分隔符',
      type: 'input',
      defaultValue: '\\n\\n',
      dependency: { field: 'splitter_type', value: 'BY_SEPARATOR' },
    },
    {
      key: 'chunk_size',
      label: '块大小',
      type: 'slider',
      min: 1,
      max: 8192,
      step: 1,
      defaultValue: 512,
    },
    {
      key: 'chunk_overlap',
      label: '重叠大小',
      type: 'slider',
      min: 0,
      // max will be calculated dynamically: chunk_size * 0.2
      dynamicMaxRatio: 0.2, 
      step: 1,
      defaultValue: 50,
    },
    {
      key: 'embedding_model',
      label: 'Embedding 模型',
      type: 'model_select', // Special type to fetch from API
      modelType: 'embedding',
      defaultValue: '',
    }
  ];

  const commonSearchConfig = [
    {
      key: 'top_k',
      label: 'Top K',
      type: 'slider',
      min: 1,
      max: 20,
      step: 1,
      defaultValue: 5,
    },
    {
      key: 'score_threshold',
      label: '相似度阈值',
      type: 'slider',
      min: 0,
      max: 1,
      step: 0.01,
      defaultValue: 0.5,
    },
    {
      key: 'enable_rerank',
      label: '开启重排序',
      type: 'switch',
      defaultValue: false,
    },
    {
      key: 'rerank_model',
      label: 'Rerank 模型',
      type: 'model_select',
      modelType: 'rerank',
      defaultValue: '',
      dependency: { field: 'enable_rerank', value: true },
    },
    {
      key: 'llm_model',
      label: 'LLM 模型',
      type: 'model_select',
      modelType: 'llm',
      defaultValue: '',
    },
    {
      key: 'enable_query_rewrite',
      label: '查询重写',
      type: 'switch',
      defaultValue: false,
    },
    {
      key: 'enable_query_decomposition',
      label: '查询分解',
      type: 'switch',
      defaultValue: false,
    },
    {
      key: 'enable_hyde',
      label: 'Hyde 模式',
      type: 'switch',
      defaultValue: false,
    },
  ];

  const configs: Record<string, any> = {
    [RAG_METHODS.NAIVE]: {
      name: 'Naive RAG',
      description: '基础检索增强生成',
      indexConfig: commonIndexConfig,
      searchConfig: commonSearchConfig,
    },
    [RAG_METHODS.HISEM_FAST]: {
      name: 'HiSem RAG Fast',
      description: '层级语义检索 快速版',
      indexConfig: commonIndexConfig, // Assuming similar index config for now
      searchConfig: commonSearchConfig,
    },
    [RAG_METHODS.HISEM]: {
      name: 'Graph RAG',
      description: '层级语义检索',
      indexConfig: [
         // Placeholder for Graph RAG specific index config if needed
         ...commonIndexConfig
      ],
      searchConfig: [
        {
            key: 'depth', 
            label: '搜索深度', 
            type: 'slider', 
            defaultValue: 2, 
            min: 1, 
            max: 5 
        },
        ...commonSearchConfig
      ],
    }
  };

  return configs[method] || configs[RAG_METHODS.NAIVE];
};
