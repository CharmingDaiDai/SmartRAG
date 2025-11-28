import request from './api';

export const kbService = {
  list: (params?: any) => request.get('/knowledge-bases', { params }),
  create: (data: any) => request.post('/knowledge-bases', data),
  get: (id: string) => request.get(`/knowledge-bases/${id}`),
  delete: (id: string) => request.delete(`/knowledge-bases/${id}`),
};
