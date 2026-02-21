import request from './api';

export const authService = {
  login: (data: any) => request.post('/auth/login', data),
  register: (data: any) => request.post('/auth/register', data),
  refreshToken: (refreshToken: string) => request.post('/auth/refresh-token', { refreshToken }),
  githubLogin: () => request.post('/auth/login/github'),
  exchangeToken: (code: string, state: string) => request.post('/auth/exchange-token', null, { params: { code, state } }),
};
