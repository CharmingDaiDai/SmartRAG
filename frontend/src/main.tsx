import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'
import '@ant-design/x-markdown/themes/light.css';
import 'katex/dist/katex.min.css';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';

ReactDOM.createRoot(document.getElementById('root')!).render(
    <ConfigProvider locale={zhCN}>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </ConfigProvider>,
)
