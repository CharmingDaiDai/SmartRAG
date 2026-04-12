import request from './api';

export const conversationService = {
  listSessions: (kbId: string, params?: { page?: number; size?: number }) =>
    request.get('/conversations/sessions', {
      params: {
        kbId,
        page: params?.page ?? 0,
        size: params?.size ?? 20,
      },
    }),

  getSessionDetail: (kbId: string, sessionId: string) =>
    request.get(`/conversations/sessions/${sessionId}`, {
      params: { kbId },
    }),

  deleteSession: (kbId: string, sessionId: string) =>
    request.delete(`/conversations/sessions/${sessionId}`, {
      params: { kbId },
    }),
};
