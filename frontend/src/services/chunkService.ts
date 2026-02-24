import request from './api';
import { ChunkVO, TreeNodeVO } from '../types';

export const chunkService = {
  /**
   * 获取文档的 Chunk 列表（NAIVE_RAG / HISEM_RAG_FAST），按 chunkIndex 升序
   */
  listChunks: (documentId: string | number, kbId: string | number) =>
    request.get<any, { data: ChunkVO[] }>('/chunks', { params: { documentId, kbId } }),

  /**
   * 更新 Chunk 内容，同步到向量库
   */
  updateChunk: (id: number, kbId: string | number, content: string) =>
    request.put<any, { data: ChunkVO }>(`/chunks/${id}`, { content }, { params: { kbId } }),

  /**
   * 获取文档的 TreeNode 树形结构（HISEM_RAG）
   */
  listTreeNodes: (documentId: string | number, kbId: string | number) =>
    request.get<any, { data: TreeNodeVO[] }>('/tree-nodes/tree', { params: { documentId, kbId } }),

  /**
   * 更新 TreeNode 内容（仅叶子节点），同步到向量库
   */
  updateTreeNode: (id: number, kbId: string | number, content: string) =>
    request.put<any, { data: null }>(`/tree-nodes/${id}`, { content }, { params: { kbId } }),
};
