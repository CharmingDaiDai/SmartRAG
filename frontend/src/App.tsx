import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, theme } from 'antd';
import BasicLayout from './layouts/BasicLayout';
import DocumentsPage from './pages/documents';
import KnowledgeBasePage from './pages/KnowledgeBase';
import KnowledgeBaseDetail from './pages/KnowledgeBase/KnowledgeBaseDetail';
import ChatPage from './pages/Chat';
import TestChatPage from './pages/TestChat';
import UserProfile from './pages/User';
import Dashboard from './pages/Dashboard';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import GitHubCallback from './pages/auth/callback/GitHubCallback';
import ProtectedRoute from './components/ProtectedRoute';
import { useAppStore } from './store/useAppStore';

function App() {
  const { initUser, themeMode } = useAppStore();

  useEffect(() => {
    initUser();
  }, [initUser]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', themeMode);
  }, [themeMode]);

  return (
    <ConfigProvider
      theme={{
        algorithm: themeMode === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
      }}
    >
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/auth/callback/github" element={<GitHubCallback />} />

        <Route path="/" element={
          <ProtectedRoute>
            <BasicLayout />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="documents" element={<DocumentsPage />} />
          <Route path="kb" element={<KnowledgeBasePage />} />
          <Route path="kb/:id" element={<KnowledgeBaseDetail />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="test-chat" element={<TestChatPage />} />
          <Route path="profile" element={<UserProfile />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
}

export default App;
