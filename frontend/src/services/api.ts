/**
 * 全局 Axios 服务实例配置
 * 
 * 功能逻辑：
 * 1. 统一管理 API 请求的基础配置（BaseURL: /api、超时时间等）
 * 2. 自动注入请求头认证 Token，维护用户会话状态
 * 3. 统一拦截网络异常和 HTTP 错误码，进行全局消息提示 (如使用 antd 的 message)
 * 4. 对于 401 错误，执行本地清理并跳转登录页
 * 
 * 影响范围：
 * 所有的通过此 `request` 实例发起的服务请求（如 authService、kbService、conversationService 等）。
 * 如果修改此类逻辑，可能会导致全局的请求验证或报错行为发生变化。
 */
import axios from 'axios';
import { message } from 'antd';

// 创建 axios 实例，全局生效
const request = axios.create({
  baseURL: '/api', // 代理会处理这个前缀，映射到后端开发服务器
  timeout: 30000, // 默认超时时间 30 秒，RAG 生成可能耗时较长
});

// 请求拦截器：在发送被 then 或 catch 处理前拦截
request.interceptors.request.use(
  (config) => {
    // 获取本地存储中的身份令牌
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`; // 统一附带 JWT Token
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器：在接收到服务器响应时统一处理错误码或异常结构
request.interceptors.response.use(
  (response) => {
    // 假设后端返回标准 REST 格式，此拦截器直接向业务层抛出真实的业务数据层级（response.data）
    return response.data;
  },
  (error) => {
    // 网络问题或服务器非 2xx 响应处理逻辑
    if (error.response) {
      const { status, data } = error.response;
      
      // 处理常见状态码
      if (status === 401) {
        message.error(data?.message || '登录已过期，请重新登录');
        localStorage.removeItem('token'); // 清理失效凭证
        // 自动跳转到登录页，如果当前已经在登录页则不跳
        if (window.location.pathname !== '/login') {
             window.location.href = '/login';
        }
      } else if (status === 403) {
        message.error(data?.message || '没有权限访问该资源');
      } else if (status === 404) {
        message.error(data?.message || '请求的资源不存在');
      } else if (status === 500) {
        message.error(data?.message || '服务器错误，请稍后重试');
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
