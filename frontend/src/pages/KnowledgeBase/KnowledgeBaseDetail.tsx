import { Button, Space, Popconfirm, Upload, Modal, Tag, Table, Input, Form, Card, Descriptions, Alert, Typography, Tooltip, App } from 'antd';
import { useState, useEffect } from 'react';
import { PlusOutlined, FilePdfOutlined, FileWordOutlined, FileTextOutlined, SearchOutlined, ArrowLeftOutlined, FileExcelOutlined, FilePptOutlined, FileMarkdownOutlined, FileImageOutlined, FileZipOutlined, CloseOutlined, InboxOutlined, SyncOutlined, EyeOutlined, DeleteOutlined, ReloadOutlined, BuildOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { documentService, IndexingTaskResponse } from '../../services/documentService';
import { kbService } from '../../services/kbService';
import { DocumentItem, KnowledgeBaseItem } from '../../types';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { FadeIn, SlideInUp, ScaleIn } from '../../components/common/Motion';
import IndexingProgress from '../../components/IndexingProgress';

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

const STRATEGY_LABELS: Record<string, string> = {
    NAIVE_RAG: 'Naive RAG',
    HISEM_RAG: 'HiSem RAG',
    HISEM_RAG_FAST: 'HiSem RAG Fast',
};

const toNativeFile = (file: UploadFile): File | null => {
    if (file.originFileObj instanceof File) return file.originFileObj;
    const possibleFile = file as unknown as File;
    return possibleFile && typeof possibleFile.size === 'number' ? possibleFile : null;
};

const formatFileSize = (size: number) => {
    if (!size) return '0 MB';
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(2)} MB`;
};

export default function KnowledgeBaseDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { message } = App.useApp();
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
    const [fileList, setFileList] = useState<UploadFile[]>([]);
    const [uploading, setUploading] = useState(false);
    const [batchIndexLoading, setBatchIndexLoading] = useState(false);
    const [rebuildingDocIds, setRebuildingDocIds] = useState<Record<string, boolean>>({});
    const [batchRebuildLoading, setBatchRebuildLoading] = useState(false);
    // 索引任务状态
    const [indexingTask, setIndexingTask] = useState<IndexingTaskResponse | null>(null);
    const [showIndexingProgress, setShowIndexingProgress] = useState(false);

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

  const totalSelectedSize = fileList.reduce((sum, file) => sum + (file.size || file.originFileObj?.size || 0), 0);

  const handleTriggerKbIndex = async () => {
      if (!id) return;
      setBatchIndexLoading(true);
      try {
          const res: any = await documentService.triggerBatchIndex(id);
          if (res.code === 200) {
              if (res.data) {
                  // 有任务返回，显示进度组件
                  setIndexingTask(res.data);
                  setShowIndexingProgress(true);
                  message.success(res.data.isNew ? '索引任务已提交' : '正在继续之前的索引任务');
              } else {
                  message.info('没有待索引的文档');
              }
          } else {
              message.error(res.message || '触发索引失败');
          }
      } catch (error) {
          message.error('触发索引失败');
      } finally {
          setBatchIndexLoading(false);
      }
  };

  // 索引完成回调
  const handleIndexingComplete = () => {
      setShowIndexingProgress(false);
      setIndexingTask(null);
      fetchData();
      fetchKbInfo();
  };

  // 关闭进度组件
  const handleIndexingClose = () => {
      setShowIndexingProgress(false);
      setIndexingTask(null);
  };

  // 重建单个文档索引
  const handleRebuildDocIndex = async (docId: string) => {
      setRebuildingDocIds(prev => ({ ...prev, [docId]: true }));
      try {
          const res: any = await documentService.rebuildIndex(docId);
          if (res.code === 200) {
              if (res.data) {
                  setIndexingTask(res.data);
                  setShowIndexingProgress(true);
                  message.success(res.data.isNew ? '重建索引任务已提交' : '正在继续之前的任务');
              } else {
                  message.info('没有需要重建的内容');
              }
          } else {
              message.error(res.message || '重建索引失败');
          }
      } catch (error) {
          message.error('重建索引失败');
      } finally {
          setRebuildingDocIds(prev => {
              const next = { ...prev };
              delete next[docId];
              return next;
          });
      }
  };

  // 批量重建索引
  const handleBatchRebuildIndex = async () => {
      if (selectedRowKeys.length === 0) {
          message.warning('请先选择要重建索引的文档');
          return;
      }
      setBatchRebuildLoading(true);
      try {
          const res: any = await documentService.batchRebuildIndex(selectedRowKeys as string[]);
          if (res.code === 200) {
              if (res.data) {
                  setIndexingTask(res.data);
                  setShowIndexingProgress(true);
                  message.success(res.data.isNew ? `已提交 ${selectedRowKeys.length} 个文档的重建索引` : '正在继续之前的任务');
                  setSelectedRowKeys([]);
              } else {
                  message.info('没有文档需要重建索引');
              }
          } else {
              message.error(res.message || '批量重建索引失败');
          }
      } catch (error) {
          message.error('批量重建索引失败');
      } finally {
          setBatchRebuildLoading(false);
      }
  };

  const handleBatchUpload = async () => {
      if (!id) {
          message.error('知识库ID不存在');
          return;
      }
      if (fileList.length === 0) {
          message.warning('请先选择需要上传的文件');
          return;
      }
      const files = fileList
          .map(toNativeFile)
          .filter((file): file is File => !!file);
      if (files.length === 0) {
          message.error('文件无效，请重新选择');
          return;
      }
      setUploading(true);
      try {
          const titles = fileList.map(file => file.name);
          const res: any = await documentService.batchUpload(id, files, titles);
          if (res.code === 200) {
              message.success('文件上传成功');
              setFileList([]);
              setIsUploadModalOpen(false);
              uploadForm.resetFields();
              fetchData();
          } else {
              message.error(res.message || '上传失败');
          }
      } catch (error) {
          message.error('上传失败，请稍后再试');
      } finally {
          setUploading(false);
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
      width: 180,
      render: (_, record) => (
        <Space>
            <Tooltip title="查看">
                <Button
                    type="text"
                    icon={<EyeOutlined />}
                    onClick={() => message.info('查看详情: ' + (record.filename || record.fileName))}
                />
            </Tooltip>
            <Tooltip title="重建索引">
                <Button
                    type="text"
                    icon={<BuildOutlined />}
                    loading={!!rebuildingDocIds[record.id]}
                    onClick={() => handleRebuildDocIndex(record.id)}
                />
            </Tooltip>
            <Popconfirm
                title="确定删除吗?"
                onConfirm={() => handleDelete(record.id)}
            >
                <Tooltip title="删除">
                    <Button type="text" danger icon={<DeleteOutlined />} />
                </Tooltip>
            </Popconfirm>
        </Space>
      ),
    },
  ];

  const filteredData = data.filter(item => 
      (item.filename || item.fileName || '').toLowerCase().includes(searchText.toLowerCase())
  );

  return (
    <FadeIn style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div className="p-6 bg-gray-50" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <SlideInUp>
      <div style={{ marginBottom: 24, flexShrink: 0 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/kb')} style={{ marginBottom: 16 }}>
            返回知识库列表
        </Button>
        <Card>
            <Descriptions title={kbInfo?.name || '知识库详情'} column={2}>
                <Descriptions.Item label="描述">{kbInfo?.description || '暂无描述'}</Descriptions.Item>
                <Descriptions.Item label="Embedding 模型">{kbInfo?.embeddingModelId}</Descriptions.Item>
                <Descriptions.Item label="RAG 方法">
                    <Tag color="blue">{STRATEGY_LABELS[kbInfo?.indexStrategyType || 'NAIVE_RAG']}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="文档数量">{kbInfo?.documentCount || 0}</Descriptions.Item>
            </Descriptions>
        </Card>
      </div>
      </SlideInUp>

      <SlideInUp transition={{ type: "spring", stiffness: 300, damping: 30, delay: 0.05 }}>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', flexShrink: 0 }}>
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
            <Button icon={<ReloadOutlined />} onClick={() => fetchData()}>
                刷新
            </Button>
            <Button
                icon={<SyncOutlined />}
                loading={batchIndexLoading}
                disabled={showIndexingProgress}
                onClick={handleTriggerKbIndex}
            >
                构建知识库索引
            </Button>
            {selectedRowKeys.length > 0 && (
                <>
                    <Button
                        icon={<BuildOutlined />}
                        loading={batchRebuildLoading}
                        disabled={showIndexingProgress}
                        onClick={handleBatchRebuildIndex}
                    >
                        批量重建索引 ({selectedRowKeys.length})
                    </Button>
                    <Popconfirm title="确定删除选中的文档吗?" onConfirm={handleBatchDelete}>
                        <Button danger>批量删除 ({selectedRowKeys.length})</Button>
                    </Popconfirm>
                </>
            )}
            <Button
                icon={<PlusOutlined />}
                onClick={() => {
                    setFileList([]);
                    setIsUploadModalOpen(true);
                }}
                type="primary"
            >
                上传文档
            </Button>
          </Space>
      </div>
      </SlideInUp>

      {/* 索引进度显示 */}
      {showIndexingProgress && id && (
        <IndexingProgress
          kbId={id}
          taskId={indexingTask?.id}
          onComplete={handleIndexingComplete}
          onClose={handleIndexingClose}
        />
      )}

      <SlideInUp transition={{ type: "spring", stiffness: 300, damping: 30, delay: 0.1 }} style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
        }}
        scroll={{ y: 'calc(100vh - 350px)' }}
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
      </SlideInUp>

      <Modal
        title="上传文档"
        open={isUploadModalOpen}
        onCancel={() => {
            setIsUploadModalOpen(false);
            setFileList([]);
        }}
        footer={null}
        modalRender={(modal) => <ScaleIn>{modal}</ScaleIn>}
      >
          <Form form={uploadForm} layout="vertical">
              <Alert
                  type="info"
                  showIcon
                  style={{ marginBottom: 16, borderRadius: 8 }}
                  title="支持批量拖拽上传"
                  description="单个文件最大 200MB，上传前可删除或替换文件"
              />
              <Form.Item label="选择文件">
                <Upload.Dragger
                    multiple
                    showUploadList={false}
                    fileList={fileList}
                    beforeUpload={(file) => {
                        setFileList(prev => {
                            const exists = prev.some(item => item.uid === file.uid);
                            if (exists) {
                                message.warning(`${file.name} 已在列表中`);
                                return prev;
                            }
                            return [...prev, file];
                        });
                        return false;
                    }}
                >
                    <p className="ant-upload-drag-icon">
                        <InboxOutlined />
                    </p>
                    <Typography.Title level={5} style={{ marginBottom: 8 }}>拖拽文件或点击上传</Typography.Title>
                    <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
                        支持 PDF、Word、PPT、Markdown 等常见格式
                    </Typography.Paragraph>
                </Upload.Dragger>
              </Form.Item>
              {fileList.length > 0 && (
                  <div style={{ background: '#fafbff', border: '1px solid #e5e7ff', borderRadius: 12, padding: 12, marginBottom: 16 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                          <Typography.Text strong>已选择 {fileList.length} 个文件</Typography.Text>
                          <Typography.Text type="secondary">共 {formatFileSize(totalSelectedSize)}</Typography.Text>
                      </div>
                      <div style={{ maxHeight: 180, overflowY: 'auto' }}>
                          {fileList.map(file => (
                              <div key={file.uid} style={{ display: 'flex', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid #f0f0f0' }}>
                                  <FileTextOutlined style={{ color: '#1677ff', marginRight: 8 }} />
                                  <div style={{ flex: 1 }}>
                                      <div style={{ fontSize: 14 }}>{file.name}</div>
                                      <div style={{ fontSize: 12, color: '#999' }}>{formatFileSize(file.size || file.originFileObj?.size || 0)}</div>
                                  </div>
                                  <Button type="text" icon={<CloseOutlined />} onClick={() => setFileList(prev => prev.filter(item => item.uid !== file.uid))} />
                              </div>
                          ))}
                      </div>
                      <div style={{ textAlign: 'right', marginTop: 8 }}>
                          <Button type="link" onClick={() => setFileList([])}>清空列表</Button>
                      </div>
                  </div>
              )}
              <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                  <Button onClick={() => setIsUploadModalOpen(false)}>取消</Button>
                  <Button type="primary" disabled={fileList.length === 0} loading={uploading} onClick={handleBatchUpload}>开始上传</Button>
              </Space>
          </Form>
      </Modal>
      </div>
    </FadeIn>
  );
}
