import axios from 'axios';
import { message } from 'antd';

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api', // 代理会处理这个前缀
  timeout: 30000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 如果后端返回的结构是 { code: 200, data: ..., message: ... }
    // 我们可以在这里统一处理，或者直接返回 response.data
    return response.data;
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      
      if (status === 401) {
        message.error('登录已过期，请重新登录');
        localStorage.removeItem('token');
        // 可以选择跳转到登录页，或者由组件处理
        if (window.location.pathname !== '/login') {
             window.location.href = '/login';
        }
      } else if (status === 403) {
        message.error('没有权限访问该资源');
      } else if (status === 404) {
        message.error('请求的资源不存在');
      } else if (status === 500) {
        message.error('服务器错误，请稍后重试');
      } else {
        message.error(data?.message || '请求失败');
      }
    } else {
      message.error('网络连接失败');
    }
    return Promise.reject(error);
  }
);

export default request;
