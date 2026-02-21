import request from './api';

export const dashboardService = {
  getStatistics: () => request.get('/dashboard/statistics'),
};
