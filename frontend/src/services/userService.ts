import request from './api';

export const userService = {
  getProfile: () => request.get('/profile'),
  updateProfile: (data: any) => request.put('/profile', data),
  changePassword: (data: any) => request.put('/profile/password', data),
  uploadAvatar: (formData: FormData) => request.post('/profile/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
};
