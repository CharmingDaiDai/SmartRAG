import axios from 'axios';
import {message} from 'antd';

// 优先使用环境变量，其次回退到默认值
const BASE_URL = process.env.REACT_APP_API_BASE || 'http://localhost:8080';
// const BASE_URL = 'http://3s62k66540.51vip.biz:59292';

// 创建一个axios实例
const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 是否正在刷新token
let isRefreshing = false;
// 等待token刷新的请求队列
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

// 请求拦截器 - 添加token到请求头
api.interceptors.request.use(
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

// 响应拦截器 - 处理统一返回格式和401错误(token过期)
api.interceptors.response.use(
  (response) => {
    // 统一处理响应数据，解构ApiResponse格式
    const apiResponse = response.data;
    if (apiResponse && apiResponse.code !== undefined) {
      // 这是统一响应格式
      if (apiResponse.code === 200) {
        // 成功响应，直接返回data
        return { ...response, data: apiResponse.data };
      } else {
        // 业务错误，显示错误消息并拒绝Promise
        message.error(apiResponse.message || '请求失败');
        return Promise.reject(new Error(apiResponse.message || '未知错误'));
      }
    }
    // 兼容旧格式的响应
    return response;
  },
  async (error) => {
    // 处理后端返回的错误
    const errorResponse = error.response;
    
    if (errorResponse && errorResponse.data) {
      // 如果是ApiResponse格式的错误
      const apiError = errorResponse.data;
      if (apiError.code && apiError.message) {
        message.error(apiError.message);
      } else if (apiError.message) {
        // 处理只有message没有code的情况
        message.error(apiError.message);
      } else if (typeof apiError === 'string') {
        // 处理纯字符串错误消息
        message.error(apiError);
      } else {
        // 处理其他情况
        message.error(`请求失败: ${errorResponse.status}`);
      }
    } else if (error.message) {
      // 其他错误消息
      message.error(error.message);
    } else {
      // 未知错误
      message.error('网络请求失败，请稍后再试');
    }

    const originalRequest = error.config;
    
    // 如果是401错误(未授权)且原请求未标记为已重试，尝试刷新token
    if (errorResponse && errorResponse.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 如果已经在刷新token，将请求添加到队列
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(token => {
            originalRequest.headers['Authorization'] = `Bearer ${token}`;
            return axios(originalRequest);
          })
          .catch(err => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      // 尝试使用refreshToken获取新的accessToken
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('无刷新令牌');
        }
        
        const response = await axios.post(`${BASE_URL}/api/auth/refresh-token`, { refreshToken });
        // 处理新的响应格式
        const responseData = response.data.data || response.data;
        const accessToken = responseData.accessToken;
        const newRefreshToken = responseData.refreshToken;
        
        localStorage.setItem('token', accessToken);
        localStorage.setItem('refreshToken', newRefreshToken);
        
        // 更新原始请求的Authorization头
        originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;
        
        // 处理队列中的请求
        processQueue(null, accessToken);
        
        // 重试原始请求
        return axios(originalRequest);
      } catch (err) {
        processQueue(err, null);
        // 刷新token失败，清除本地存储并重定向到登录页
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }
    
    return Promise.reject(error);
  }
);

// 认证相关API
export const authAPI = {
  login: (credentials) => api.post('/api/auth/login', credentials),
  register: (userData) => api.post('/api/auth/register', userData),
  refreshToken: (refreshData) => api.post('/api/auth/refresh-token', refreshData),
  // 添加GitHub登录API
  githubLogin: () => api.post('/api/auth/login/github'),
  // 使用授权码交换token - 使用POST请求方式，将code作为查询参数传递
  exchangeAuthCode: (code) => api.post(`/api/auth/exchange-token?code=${encodeURIComponent(code)}`),
};

// 仪表盘相关API
export const dashboardAPI = {
  // 获取仪表盘统计数据
  getStatistics: () => api.get('/api/dashboard/statistics'),
  // 获取最近活动
  getRecentActivities: (limit = 5) => api.get(`/api/dashboard/activities?limit=${limit}`),
};

// 文档相关API
export const documentAPI = {
  // 获取所有文档
  getAllDocuments: () => api.get('/api/documents'),
  // 获取单个文档
  getDocument: (id) => api.get(`/api/documents/${id}`),
  // 上传文档
  uploadDocument: (formData) => api.post('/api/documents/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }),
  // 删除文档
  deleteDocument: (id) => api.delete(`/api/documents/${id}`),
  
  // 文档分析相关API
  // 文档摘要
  getSummary: (content) => api.post('/api/analysis/summary', { content }),
  getSummaryFromDocument: (documentId) => api.post('/api/analysis/summary', { documentId }),
  // 提取关键词
  extractKeywords: (content) => api.post('/api/analysis/keywords', { content }),
  extractKeywordsFromDocument: (documentId) => api.post('/api/analysis/keywords', { documentId }),
  // 文档润色
  polishDocument: (content, polishType) => api.post('/api/analysis/polish', { content, polishType }),
  polishDocumentFromDocument: (documentId, polishType) => api.post('/api/analysis/polish', { documentId, polishType }),
  // 敏感信息检测
  detectSensitiveInfo: (content) => api.post('/api/analysis/security', { content }),
  detectSensitiveInfoFromDocument: (documentId) => api.post('/api/analysis/security', { documentId }),
  // 文档分类 - 此功能已移除
  // classifyDocument: (content) => api.post('/api/classification', { content }),
};

// 用户个人资料相关API
export const profileAPI = {
  // 获取个人资料
  getProfile: () => api.get('/api/profile'),
  // 更新个人资料
  updateProfile: (data) => api.put('/api/profile', data),
  // 上传头像
  uploadAvatar: (formData) => api.post('/api/profile/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }),
  // 修改密码
  changePassword: (data) => api.put('/api/profile/password', data),
};

// RAG方法相关API
export const ragMethodAPI = {
  // 获取所有可用的嵌入模型
  getEmbeddingModels: () => api.get('/api/kb/listEmbeddingModels'),
  // 获取RAG方法详情
  getMethodDetails: (methodId) => api.get(`/api/rag/methods/${methodId}`),
};

// 知识库相关API
export const knowledgeBaseAPI = {
  // 获取所有知识库列表
  getKnowledgeBases: () => api.get('/api/kb/list'),
  // 获取单个知识库详情
  getKnowledgeBase: (id) => api.get(`/api/kb/${id}`),
  // 创建新知识库
  createKnowledgeBase: (data) => api.post('/api/kb/create', data),
  // 删除知识库
  deleteKnowledgeBase: (id) => api.delete(`/api/kb/${id}`),
  // 构建知识库索引
  buildIndex: (id) => api.post(`/api/kb/index/${id}`),
  // 知识库问答
  queryKnowledgeBase: (data) => api.post('/api/kb/rag', data),
  // 流式知识库问答 (SSE)
  // 使用api.get方法会自动通过拦截器添加认证头
  streamQueryKnowledgeBase: (id, question, topk) => 
    api.get(`/api/kb/kbqa/${id}?question=${encodeURIComponent(question)}&topk=${topk || 5}`),
  // 获取知识库文档列表
  getDocuments: (id) => api.get(`/api/kb/listDocs/${id}`),
  // 上传文档到知识库
  uploadDocuments: (id, formData) => api.post(`/api/kb/addDocs/${id}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  }),
  // 删除知识库文档
  deleteDocuments: (kbId, docIds) => api.post(`/api/kb/deleteDocs/${kbId}`, docIds),
};

export default api;