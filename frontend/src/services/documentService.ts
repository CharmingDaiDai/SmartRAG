import request from './api';

export const documentService = {
  list: (params: any) => request.get('/documents', { params }),
  listAll: (params: any) => request.get('/documents', { params }),
  get: (id: string) => request.get(`/documents/${id}`),
  upload: (data: FormData) => request.post('/documents/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  batchUpload: (data: FormData) => request.post('/documents/batch-upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  delete: (id: string) => request.delete(`/documents/${id}`),
  batchDelete: (ids: string[]) => request.delete('/documents/batch', { data: ids }),
  triggerIndex: (id: string) => request.post(`/documents/${id}/index`),
  triggerBatchIndex: (kbId: string) => request.post('/documents/batch-index', null, { params: { kbId } }),
};
