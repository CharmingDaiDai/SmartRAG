/**
 * RAG方法的默认配置
 * 这些配置在用户未调整参数时使用
 */
export const ragMethods = [
  {
    id: "naive",
    name: "RAG",
    description: "普通的RAG方法，适用于一般场景的知识问答",
    indexParams: {
      "chunk-size": 512,
      "chunk-overlap": 100,
    },
    searchParams: {
      "top-k": {
        default: 5,
        type: "integer",
        min: 1,
        max: 15,
        label: "检索数量",
      },
      "intent-recognition": {
        default: false,
        type: "boolean",
        label: "意图识别",
        description: "判断用户查询的意图类型，决定是否需要检索（开启后会增加响应时间）",
      },
      "query-rewriting": {
        default: false,
        type: "boolean",
        label: "查询重写",
        description: "智能重写用户查询以提高检索效果（开启后会增加响应时间）",
      },
      // "query-decomposition": {
      //   default: false,
      //   type: "boolean",
      //   label: "查询分解",
      //   description: "将复杂查询分解为多个简单查询（开启后会增加响应时间）",
      // },
    },
  },
  {
    id: "hisem",
    name: "HiSem-RAG-Fast",
    description: "层级语义驱动的RAG方法（不构建树），适合处理多文档问答",
    indexParams: {
      "chunk-size": 2048,
      "title-enhance": true,
      // abstract: false,
    },
    searchParams: {
      "max-res": {
        default: 10,
        type: "integer",
        min: 1,
        max: 15,
        label: "最大结果数",
      },
      "intent-recognition": {
        default: false,
        type: "boolean",
        label: "意图识别",
        description: "判断用户查询的意图类型，决定是否需要检索（开启后会增加响应时间）",
      },
      "query-rewriting": {
        default: false,
        type: "boolean",
        label: "查询重写",
        description: "智能重写用户查询以提高检索效果（开启后会增加响应时间）",
      },
      // "query-decomposition": {
      //   default: false,
      //   type: "boolean",
      //   label: "查询分解",
      //   description: "将复杂查询分解为多个简单查询（开启后会增加响应时间）",
      // },
    },
  },
  // {
  //   id: "hisem-tree",
  //   name: "HiSem-RAG-Tree",
  //   description: "层级语义驱动的RAG方法（构建树），适合处理结构化文档问答",
  //   indexParams: {
  //     "chunk-size": 2048,
  //     "title-enhance": true,
  //     abstract: true,
  //   },
  //   searchParams: {
  //     "max-res": {
  //       default: 10,
  //       type: "integer",
  //       min: 1,
  //       max: 15,
  //       label: "最大结果数",
  //     },
  //     "intent-recognition": {
  //       default: false,
  //       type: "boolean",
  //       label: "意图识别",
  //       description: "判断用户查询的意图类型，决定是否需要检索（开启后会增加响应时间）",
  //     },
  //     "query-rewriting": {
  //       default: false,
  //       type: "boolean",
  //       label: "查询重写",
  //       description: "智能重写用户查询以提高检索效果（开启后会增加响应时间）",
  //     },
  //     "query-decomposition": {
  //       default: false,
  //       type: "boolean",
  //       label: "查询分解",
  //       description: "将复杂查询分解为多个简单查询（开启后会增加响应时间）",
  //     },
  //   },
  // },
];

/**
 * 参数配置的限制
 * 包含最小值、最大值、步长等
 */
export const paramConstraints = {
  "chunk-size": {
    min: 128,
    max: 4096,
    step: 32,
    type: "number",
    unit: "字符",
    description: "文档切分的块大小，较大的值有助于保留上下文，较小的值更精确",
    displayName: "块大小",
    example: "例如：512字符适合一般文档，2048字符适合保留更多上下文",
  },
  "chunk-overlap": {
    min: 0,
    max: 512,
    step: 10,
    type: "number",
    unit: "字符",
    description: "相邻块之间重叠的内容数量，用于保持上下文连贯性",
    displayName: "块重叠",
    example: "推荐值为块大小的10%-20%",
  },
  "title-enhance": {
    type: "boolean",
    description: "使用文档标题增强检索质量，对结构化文档效果更好",
    displayName: "标题增强",
    example: "适用于有明确标题层次的文档，如论文、报告等",
  },
  // abstract: {
  //   type: "boolean",
  //   description: "是否提取摘要，用于概括文档内容",
  //   displayName: "摘要提取",
  //   example: "开启此功能可以提升对文档主题的理解",
  // },
  "intent-recognition": {
    default: false,
    type: "boolean",
    label: "意图识别",
    description: "判断用户查询的意图类型，决定是否需要检索",
  },
  "query-rewriting": {
    type: "boolean",
    description: "查询重写",
    displayName: "查询重写",
    example: "",
  },
  "query-decomposition": {
    type: "boolean",
    description: "查询分解",
    displayName: "查询分解",
    example: "",
  },
  "top-k": {
    min: 1,
    max: 15,
    step: 1,
    type: "slider", // 改为滑动条类型
    description: "检索时返回的最相似文档数量",
    displayName: "检索数量",
    example: "数值越大，结果越全面，但可能引入更多噪音",
    marks: { 1: "1", 5: "5", 10: "10", 15: "15" }, // 添加标记点
  },
  "max-res": {
    min: 1,
    max: 15,
    step: 1,
    type: "slider", // 改为滑动条类型
    description: "生成回答时使用的最大文档数",
    displayName: "最大结果数",
    example: "数值越大，使用的上下文越多，但响应可能变慢",
    marks: { 1: "1", 5: "5", 10: "10", 15: "15" }, // 添加标记点
  },
};

/**
 * 获取指定方法的配置信息
 * @param {string} methodId - 方法ID
 * @returns {Object|null} 方法配置或null
 */
export const getMethodConfig = (methodId) => {
  return ragMethods.find((method) => method.id === methodId) || null;
};

export default {
  ragMethods,
  paramConstraints,
  getMethodConfig,
};
