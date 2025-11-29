import { Button, Space, Popconfirm, message, Upload, Modal, Tag, Table, Input, Form, Card, Descriptions } from 'antd';
import { useState, useEffect } from 'react';
import { PlusOutlined, UploadOutlined, FilePdfOutlined, FileWordOutlined, FileTextOutlined, SearchOutlined, ArrowLeftOutlined, FileExcelOutlined, FilePptOutlined, FileMarkdownOutlined, FileImageOutlined, FileZipOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { documentService } from '../../services/documentService';
import { kbService } from '../../services/kbService';
import { DocumentItem, KnowledgeBaseItem } from '../../types';
import type { ColumnsType } from 'antd/es/table';

const getFileIcon = (fileName: string) => {
    const ext = fileName?.split('.').pop()?.toLowerCase();
    const style = { fontSize: '20px' };
    if (ext === 'pdf') return <FilePdfOutlined style={{ ...style, color: '#ff4d4f' }} />;
    if (ext === 'doc' || ext === 'docx') return <FileWordOutlined style={{ ...style, color: '#1677ff' }} />;
    if (ext === 'xls' || ext === 'xlsx') return <FileExcelOutlined style={{ ...style, color: '#52c41a' }} />;
    if (ext === 'ppt' || ext === 'pptx') return <FilePptOutlined style={{ ...style, color: '#fa8c16' }} />;
    if (ext === 'md' || ext === 'markdown') return <FileMarkdownOutlined style={{ ...style, color: '#722ed1' }} />;
    if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext || '')) return <FileImageOutlined style={{ ...style, color: '#13c2c2' }} />;
    if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext || '')) return <FileZipOutlined style={{ ...style, color: '#faad14' }} />;
    return <FileTextOutlined style={{ ...style, color: '#8c8c8c' }} />;
};

export default function KnowledgeBaseDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [kbInfo, setKbInfo] = useState<KnowledgeBaseItem | null>(null);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<DocumentItem[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [searchText, setSearchText] = useState('');
  const [uploadForm] = Form.useForm();

  const fetchKbInfo = async () => {
      if (!id) return;
      try {
          const res: any = await kbService.get(id);
          if (res.code === 200) {
              setKbInfo(res.data);
          }
      } catch (error) {
          message.error('获取知识库详情失败');
      }
  };

  const fetchData = async (page = currentPage, size = pageSize) => {
      if (!id) return;
      setLoading(true);
      try {
          const params: any = {
              page: page - 1,
              size: size,
          };
          
          const res: any = await documentService.listByKb(id, params);
          if (res.code === 200) {
              if (res.data && Array.isArray(res.data.content)) {
                  setData(res.data.content);
                  setTotal(res.data.totalElements);
              } else if (Array.isArray(res.data)) {
                  setData(res.data);
                  setTotal(res.data.length);
              }
          } else {
              message.error(res.message || '获取文档列表失败');
          }
      } catch (error) {
          // message.error('获取文档列表失败');
      } finally {
          setLoading(false);
      }
  };

  useEffect(() => {
      fetchKbInfo();
      fetchData(currentPage, pageSize);
  }, [id, currentPage, pageSize]);

  const handleDelete = async (docId: string) => {
    try {
      const res: any = await documentService.delete(docId);
      if (res.code === 200) {
          message.success('删除成功');
          fetchData();
      } else {
          message.error(res.message || '删除失败');
      }
    } catch (error) {
      // message.error('删除失败');
    }
  };

  const handleBatchDelete = async () => {
      try {
          const res: any = await documentService.batchDelete(selectedRowKeys as string[]);
          if (res.code === 200) {
              message.success('批量删除成功');
              setSelectedRowKeys([]);
              fetchData();
          } else {
              message.error(res.message || '批量删除失败');
          }
      } catch (error) {
          // message.error('批量删除失败');
      }
  };

  const handleUpload = async (options: any) => {
      const { file, onSuccess, onError } = options;
      
      if (!id) {
          message.error('知识库ID不存在');
          onError(new Error('No KB ID'));
          return;
      }

      const formData = new FormData();
      formData.append('file', file);
      formData.append('kbId', id);

      try {
          const res: any = await documentService.upload(formData);
          if (res.code === 200) {
              onSuccess(res.data);
              message.success(`${file.name} 上传成功`);
              fetchData();
          } else {
              onError(new Error(res.message));
              message.error(`${file.name} 上传失败: ${res.message}`);
          }
      } catch (error) {
          onError(error);
          message.error(`${file.name} 上传失败`);
      }
  };

  const columns: ColumnsType<DocumentItem> = [
    {
      title: '文档名称',
      dataIndex: 'filename',
      ellipsis: true,
      render: (text, entity) => (
        <Space>
            {getFileIcon(text || entity.filename || entity.fileName)}
            {text || entity.filename || entity.fileName}
        </Space>
      ),
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      width: 100,
      render: (size) => (size / 1024 / 1024).toFixed(2) + ' MB',
    },
    {
      title: '状态',
      dataIndex: 'indexStatus',
      width: 120,
      render: (status) => {
          const statusMap: any = {
              UPLOADED: { text: '已上传', color: 'default' },
              CHUNKING: { text: '切分中', color: 'processing' },
              CHUNKED: { text: '已切分', color: 'warning' },
              INDEXING: { text: '索引中', color: 'processing' },
              INDEXED: { text: '已索引', color: 'success' },
              ERROR: { text: '错误', color: 'error' },
          };
          const s = statusMap[status] || { text: status, color: 'default' };
          return <Tag color={s.color}>{s.text}</Tag>;
      }
    },
    {
      title: '上传时间',
      dataIndex: 'uploadTime',
      width: 180,
      sorter: (a: any, b: any) => new Date(a.uploadTime).getTime() - new Date(b.uploadTime).getTime(),
    },
    {
      title: '操作',
      key: 'option',
      width: 120,
      render: (_, record) => (
        <Space>
            <a onClick={() => message.info('查看详情: ' + (record.filename || record.fileName))}>查看</a>
            <Popconfirm
                title="确定删除吗?"
                onConfirm={() => handleDelete(record.id)}
            >
                <a className="text-red-500">删除</a>
            </Popconfirm>
        </Space>
      ),
    },
  ];

  const filteredData = data.filter(item => 
      (item.filename || item.fileName || '').toLowerCase().includes(searchText.toLowerCase())
  );

  return (
    <div className="p-6 bg-gray-50 min-h-screen">
      <div style={{ marginBottom: 24 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/kb')} style={{ marginBottom: 16 }}>
            返回知识库列表
        </Button>
        <Card>
            <Descriptions title={kbInfo?.name || '知识库详情'} column={2}>
                <Descriptions.Item label="描述">{kbInfo?.description || '暂无描述'}</Descriptions.Item>
                <Descriptions.Item label="Embedding 模型">{kbInfo?.embeddingModelId}</Descriptions.Item>
                <Descriptions.Item label="RAG 方法">
                    <Tag color="blue">{kbInfo?.indexStrategyType === 'HISEM_RAG' ? 'HiSem RAG' : 'Naive RAG'}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="文档数量">{kbInfo?.documentCount || 0}</Descriptions.Item>
            </Descriptions>
        </Card>
      </div>

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
          <Space>
            <Input 
                placeholder="搜索文档" 
                prefix={<SearchOutlined />} 
                value={searchText}
                onChange={e => setSearchText(e.target.value)}
                style={{ width: 200 }}
            />
          </Space>
          <Space>
            {selectedRowKeys.length > 0 && (
                <Popconfirm title="确定删除选中的文档吗?" onConfirm={handleBatchDelete}>
                    <Button danger>批量删除 ({selectedRowKeys.length})</Button>
                </Popconfirm>
            )}
            <Button
                icon={<PlusOutlined />}
                onClick={() => setIsUploadModalOpen(true)}
                type="primary"
            >
                上传文档
            </Button>
          </Space>
      </div>

      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
        }}
        pagination={{ 
            current: currentPage,
            pageSize: pageSize,
            total: total,
            showSizeChanger: true,
            onChange: (page, size) => {
                setCurrentPage(page);
                setPageSize(size);
            }
        }}
      />

      <Modal
        title="上传文档"
        open={isUploadModalOpen}
        onCancel={() => setIsUploadModalOpen(false)}
        footer={null}
      >
          <Form form={uploadForm} layout="vertical">
              <Form.Item label="文件">
                <Upload.Dragger
                    multiple
                    name="file"
                    customRequest={handleUpload}
                    showUploadList={true}
                >
                    <p className="ant-upload-drag-icon">
                        <UploadOutlined />
                    </p>
                    <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
                    <p className="ant-upload-hint">
                        支持单次或批量上传。
                    </p>
                </Upload.Dragger>
              </Form.Item>
          </Form>
      </Modal>
    </div>
  );
}
