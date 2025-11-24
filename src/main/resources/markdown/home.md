# 智能文档系统 API 文档

欢迎使用智能文档系统 API。

## 系统简介
本系统基于 LangChain4j 构建，提供文档管理、知识库检索、智能对话等功能。

## 快速开始
1. 获取 Token: 调用 `/api/auth/login` 接口获取 JWT Token
2. 认证: 点击右上角 "Authorize" 按钮，输入 Token
3. 调用接口: 选择相应接口进行测试

## 常见问题
- **401 Unauthorized**: Token 过期或无效，请重新登录
- **403 Forbidden**: 权限不足，请联系管理员
