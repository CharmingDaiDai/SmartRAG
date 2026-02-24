export interface DocumentItem {
  id: string;
  filename: string;
  fileSize: number;
  indexStatus: string;
  uploadTime: string;
  kbId: string;
  // Deprecated or optional fields
  title?: string;
  fileName?: string; // Keep for compatibility if needed, but prefer filename
  fileType?: string;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
  knowledgeBaseId?: string;
}

export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  indexStrategyType?: string;
  indexStrategyConfig?: string;
  embeddingModelId?: string;
  documentCount?: number;
  indexedDocumentCount?: number;
  createdAt?: string;
  updatedAt?: string;
  // Deprecated fields kept for compatibility if needed, or remove if sure
  docCount?: number; 
  ragMethod?: string;
  embeddingModel?: string;
}

// 兼容旧代码的别名
export type KnowledgeBaseItem = KnowledgeBase;

export interface User {
  id: string;
  username: string;
  avatar?: string;
  avatarUrl?: string;
  email?: string;
  role: 'admin' | 'user' | 'vip';
}

export interface ThoughtItem {
  title: string;
  status: 'pending' | 'processing' | 'success' | 'error';
  icon?: React.ReactNode;
  duration?: number;
  content?: string; // 详细思考内容
}

export interface RetrievalTreeNode {
  nodeId: string;
  titlePath: string;
  score: number;
  passedThreshold: boolean;
  inResults: boolean;
  children: RetrievalTreeNode[];
}

export interface ReferenceItem {
    id?: string | number;
    title: string;
    score: number;
    content?: string;
    url?: string;
}

export interface TokenUsageEntry {
  label: string;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  durationMs: number;
}

export interface TokenUsageReport {
  entries: TokenUsageEntry[];
  total: TokenUsageEntry;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'ai';
  content: string;
  thoughts?: ThoughtItem[];
  references?: ReferenceItem[]; // 引用文档
  retrievalTree?: RetrievalTreeNode[]; // 检索路径树（层级可视化）
  tokenUsage?: TokenUsageReport; // Token 用量明细
  createTime: number;
  // 兼容字段
  status?: 'loading' | 'success' | 'error' | 'updating';
}

export interface ChunkVO {
  id: number;
  kbId: number;
  documentId: number;
  chunkIndex: number;
  content: string;
  keyKnowledge?: string;
  summary?: string;
  strategyType: string;
}

export interface TreeNodeVO {
  id: number;
  nodeId: string;
  parentNodeId?: string;
  titlePath: string;
  level: number;
  nodeType: string; // 'ROOT' | 'INTERNAL' | 'LEAF'
  content: string;
  summary?: string;
  children: TreeNodeVO[];
}

