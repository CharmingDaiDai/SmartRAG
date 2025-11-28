export const getMethodConfig = (method: string) => {
  const configs: Record<string, any> = {
    naive: {
      name: 'Naive RAG',
      description: '基础检索增强生成',
      searchParams: {
        topK: { label: 'Top K', type: 'number', default: 5, min: 1, max: 20 },
        threshold: { label: '相似度阈值', type: 'number', default: 0.7, min: 0, max: 1, step: 0.1 },
      }
    },
    hisem: {
      name: 'HiSem RAG',
      description: '分层语义检索',
      searchParams: {
        topK: { label: 'Top K', type: 'number', default: 5, min: 1, max: 20 },
        threshold: { label: '相似度阈值', type: 'number', default: 0.7, min: 0, max: 1, step: 0.1 },
        rerank: { label: '重排序', type: 'boolean', default: false },
      }
    },
    graph: {
      name: 'Graph RAG',
      description: '图谱增强检索',
      searchParams: {
        depth: { label: '搜索深度', type: 'number', default: 2, min: 1, max: 5 },
      }
    }
  };
  return configs[method] || configs.naive;
};
