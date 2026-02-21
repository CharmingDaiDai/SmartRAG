import request from './api';

export const modelService = {
  getLLMs: () => request.get('/models/llms'),
  getEmbeddings: () => request.get('/models/embeddings'),
  getReranks: () => request.get('/models/reranks'),
};
