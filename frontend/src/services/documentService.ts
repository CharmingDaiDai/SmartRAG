import request from './api';

const multipartHeaders = { headers: { 'Content-Type': 'multipart/form-data' } };

/**
 * 索引任务响应类型
 */
export interface IndexingTaskResponse {
  id: number;
  kbId: number;
  taskType: 'INDEX' | 'REBUILD';
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  totalDocs: number;
  completedDocs: number;
  failedDocs: number;
  currentDocId?: number;
  currentDocName?: string;
  currentStep?: string;
  currentStepName?: string;
  percentage: number;
  errorMessage?: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  isNew: boolean;
}

export const documentService = {
  list: (params: any) => request.get('/documents', { params }),
  listAll: (params: any) => request.get('/documents', { params }),
  listByKb: (kbId: string, params: any) => request.get(`/documents/${kbId}`, { params }),
  get: (id: string) => request.get(`/documents/${id}`),
  upload: (kbId: string, file: File, title?: string) => {
    const formData = new FormData();
    formData.append('kbId', kbId);
    formData.append('file', file);
    if (title) formData.append('title', title);
    return request.post('/documents/upload', formData, multipartHeaders);
  },
  batchUpload: (kbId: string, files: File[], titles?: string[]) => {
    const formData = new FormData();
    formData.append('kbId', kbId);
    files.forEach((file) => formData.append('files', file));
    titles?.forEach((title) => formData.append('titles', title));
    return request.post('/documents/batch-upload', formData, multipartHeaders);
  },
  delete: (id: string) => request.delete(`/documents/${id}`),
  batchDelete: (ids: string[]) => request.delete('/documents/batch', { data: ids }),
  triggerIndex: (id: string) => request.post(`/documents/${id}/index`),
  triggerBatchIndex: (kbId: string) => request.post('/documents/batch-index', null, { params: { kbId } }),
  // 重建索引（基于现有 Chunk，不重新切分文档）
  rebuildIndex: (id: string) => request.post(`/documents/${id}/rebuild-index`),
  batchRebuildIndex: (ids: string[]) => request.post('/documents/batch-rebuild-index', ids),

  /**
   * 订阅索引进度（SSE）
   * @param kbId 知识库 ID
   * @returns EventSource 实例
   */
  subscribeIndexProgress: (kbId: string): EventSource => {
    const token = localStorage.getItem('token');
    return new EventSource(`/api/documents/index-progress/${kbId}?token=${token}`);
  },
};
