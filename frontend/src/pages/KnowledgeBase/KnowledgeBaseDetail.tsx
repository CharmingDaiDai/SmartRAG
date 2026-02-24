import { Button, Space, Popconfirm, Upload, Modal, Table, Input, Form, Alert, Typography, Tooltip, App, Breadcrumb, Statistic, theme } from 'antd';
import { useState, useEffect, useRef, useCallback } from 'react';
import { PlusOutlined, FilePdfOutlined, FileWordOutlined, FileTextOutlined, SearchOutlined, FileExcelOutlined, FilePptOutlined, FileMarkdownOutlined, FileImageOutlined, FileZipOutlined, CloseOutlined, InboxOutlined, SyncOutlined, EyeOutlined, DeleteOutlined, ReloadOutlined, BuildOutlined, HomeOutlined, FileSearchOutlined, RobotOutlined, DatabaseOutlined } from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import { documentService } from '../../services/documentService';
import { kbService } from '../../services/kbService';
import { DocumentItem, KnowledgeBaseItem } from '../../types';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { FadeIn, SlideInUp, ScaleIn } from '../../components/common/Motion';
import IndexingProgress from '../../components/IndexingProgress';
import ChunkDrawer from '../../components/ChunkDrawer';

// Augment component to use theme token
const formatDateTime = (val: string | undefined | null): string => {
    if (!val) return '—';
    const d = new Date(val);
    if (isNaN(d.getTime())) return val;
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
    const isToday = d.toDateString() === now.toDateString();
    const yesterday = new Date(now); yesterday.setDate(now.getDate() - 1);
    const isYesterday = d.toDateString() === yesterday.toDateString();
    if (isToday) return `今天 ${time}`;
    if (isYesterday) return `昨天 ${time}`;
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`;
};

// Augment component to use theme token
const KbInfoCards = ({ kbInfo }: { kbInfo: KnowledgeBaseItem | null }) => {
    const { token } = theme.useToken();
    const STRATEGY_LABELS: Record<string, { label: string; color: string }> = {
        NAIVE_RAG: { label: 'Naive RAG', color: '#6366f1' },
        HISEM_RAG: { label: 'HiSem-SADP', color: '#8b5cf6' },
        HISEM_RAG_FAST: { label: 'HiSem Fast', color: '#06b6d4' },
    };
    const strategy = kbInfo?.indexStrategyType || 'NAIVE_RAG';
    const strategyInfo = STRATEGY_LABELS[strategy] || { label: strategy, color: '#6366f1' };

    const stats = [
        {
            key: 'docs',
            icon: <FileSearchOutlined />,
            color: token.colorPrimary,
            title: '文档数量',
            value: kbInfo?.documentCount ?? 0,
        },
        {
            key: 'indexed',
            icon: <DatabaseOutlined />,
            color: '#10b981',
            title: '已索引',
            value: kbInfo?.indexedDocumentCount ?? kbInfo?.documentCount ?? 0,
        },
        {
            key: 'strategy',
            icon: <RobotOutlined />,
            color: strategyInfo.color,
            title: 'RAG 策略',
            value: strategyInfo.label,
            isText: true,
        },
        {
            key: 'model',
            icon: <RobotOutlined />,
            color: '#f59e0b',
            title: 'Embedding 模型',
            value: kbInfo?.embeddingModelId || '-',
            isText: true,
        },
    ];

    return (
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
            {stats.map(stat => (
                <div
                    key={stat.key}
                    style={{
                        padding: '6px 12px',
                        borderRadius: 10,
                        border: `1px solid ${token.colorBorderSecondary}`,
                        background: token.colorBgContainer,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        whiteSpace: 'nowrap',
                    }}
                >
                    <div style={{
                        width: 28,
                        height: 28,
                        borderRadius: 7,
                        background: `${stat.color}18`,
                        border: `1px solid ${stat.color}30`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: stat.color,
                        fontSize: 13,
                        flexShrink: 0,
                    }}>
                        {stat.icon}
                    </div>
                    <div>
                        <div style={{ fontSize: 10, color: token.colorTextTertiary, lineHeight: 1.2 }}>{stat.title}</div>
                        {stat.isText ? (
                            <div style={{ fontSize: 12, fontWeight: 600, color: token.colorText, lineHeight: 1.4 }}>{stat.value}</div>
                        ) : (
                            <Statistic
                                value={stat.value as number}
                                valueStyle={{ fontSize: 16, fontWeight: 700, color: token.colorText, fontFamily: "'JetBrains Mono', monospace", lineHeight: 1.3 }}
                                style={{ lineHeight: 1 }}
                            />
                        )}
                    </div>
                </div>
            ))}
        </div>
    );
};

const getFileIcon = (fileName: string, primaryColor = '#6366f1') => {
    const ext = fileName?.split('.').pop()?.toLowerCase();
    const style = { fontSize: '20px' };
    if (ext === 'pdf') return <FilePdfOutlined style={{ ...style, color: '#ef4444' }} />;
    if (ext === 'doc' || ext === 'docx') return <FileWordOutlined style={{ ...style, color: primaryColor }} />;
    if (ext === 'xls' || ext === 'xlsx') return <FileExcelOutlined style={{ ...style, color: '#10b981' }} />;
    if (ext === 'ppt' || ext === 'pptx') return <FilePptOutlined style={{ ...style, color: '#f59e0b' }} />;
    if (ext === 'md' || ext === 'markdown') return <FileMarkdownOutlined style={{ ...style, color: '#8b5cf6' }} />;
    if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext || '')) return <FileImageOutlined style={{ ...style, color: '#06b6d4' }} />;
    if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext || '')) return <FileZipOutlined style={{ ...style, color: '#f59e0b' }} />;
    return <FileTextOutlined style={{ ...style, color: '#a8a29e' }} />;
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
  const { message } = App.useApp();
  const { token } = theme.useToken();
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
    const [deletingDocIds, setDeletingDocIds] = useState<Record<string, boolean>>({});
    const [batchDeleteLoading, setBatchDeleteLoading] = useState(false);
    // 索引任务状态
    const [showIndexingProgress, setShowIndexingProgress] = useState(false);
    // 切分块查看
    const [chunkDrawerOpen, setChunkDrawerOpen] = useState(false);
    const [selectedDocument, setSelectedDocument] = useState<DocumentItem | null>(null);

  // 动态计算表格滚动高度
  const tableWrapperRef = useRef<HTMLDivElement>(null);
  const [tableScrollY, setTableScrollY] = useState<number>(400);

  const calcTableHeight = useCallback(() => {
      if (!tableWrapperRef.current) return;
      // 表格容器的总高度减去表头(~55px)和分页栏(~56px)
      const h = tableWrapperRef.current.clientHeight - 55 - 56;
      setTableScrollY(Math.max(h, 200));
  }, []);

  useEffect(() => {
      calcTableHeight();
      const obs = new ResizeObserver(calcTableHeight);
      if (tableWrapperRef.current) obs.observe(tableWrapperRef.current);
      return () => obs.disconnect();
  }, [calcTableHeight]);

  // 索引进度条显示/隐藏会改变可用空间，触发重新计算
  useEffect(() => {
      calcTableHeight();
  }, [showIndexingProgress, calcTableHeight]);

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
    setDeletingDocIds(prev => ({ ...prev, [docId]: true }));
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
    } finally {
      setDeletingDocIds(prev => {
          const next = { ...prev };
          delete next[docId];
          return next;
      });
    }
  };

  const handleBatchDelete = async () => {
      setBatchDeleteLoading(true);
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
      } finally {
          setBatchDeleteLoading(false);
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
      fetchData();
      fetchKbInfo();
  };

  // 重建单个文档索引
  const handleRebuildDocIndex = async (docId: string) => {
      setRebuildingDocIds(prev => ({ ...prev, [docId]: true }));
      try {
          const res: any = await documentService.rebuildIndex(docId);
          if (res.code === 200) {
              if (res.data) {
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
            {getFileIcon(text || entity.filename || entity.fileName, token.colorPrimary)}
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
              UPLOADED: { text: '已上传', color: '#a8a29e', dot: '#a8a29e' },
              READING: { text: '读取中', color: token.colorPrimary, dot: token.colorPrimary },
              PARSING: { text: '解析中', color: '#3b82f6', dot: '#3b82f6' },
              CHUNKING: { text: '切分中', color: '#06b6d4', dot: '#06b6d4' },
              TREE_BUILDING: { text: '构建语义树', color: '#84cc16', dot: '#84cc16' },
              LLM_ENRICHING: { text: '语义增强中', color: '#eab308', dot: '#eab308' },
              SAVING: { text: '保存中', color: '#f97316', dot: '#f97316' },
              EMBEDDING: { text: '向量化中', color: '#8b5cf6', dot: '#8b5cf6' },
              STORING: { text: '存储向量', color: '#f97316', dot: '#f97316' },
              INDEXED: { text: '已索引', color: '#10b981', dot: '#10b981' },
              ERROR: { text: '错误', color: '#ef4444', dot: '#ef4444' },
          };
          const s = statusMap[status] || { text: status, color: '#a8a29e', dot: '#a8a29e' };
          return (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
                  <span style={{ width: 6, height: 6, borderRadius: '50%', background: s.dot, display: 'inline-block', flexShrink: 0 }} />
                  <span style={{ color: s.color, fontWeight: 500 }}>{s.text}</span>
              </span>
          );
      }
    },
    {
      title: '上传时间',
      dataIndex: 'uploadTime',
      width: 180,
      sorter: (a: any, b: any) => new Date(a.uploadTime).getTime() - new Date(b.uploadTime).getTime(),
      render: (val: string) => formatDateTime(val),
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
                    onClick={() => {
                        setSelectedDocument(record);
                        setChunkDrawerOpen(true);
                    }}
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
                    <Button type="text" danger icon={<DeleteOutlined />} loading={!!deletingDocIds[record.id]} />
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
    <FadeIn style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {/* 面包屑 + KB 信息区 — 一行显示：左侧名称，右侧卡片 */}
      <SlideInUp style={{ flexShrink: 0 }}>
      <div style={{ marginBottom: 12, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 24 }}>
        <div style={{ flexShrink: 0 }}>
          <Breadcrumb
            style={{ marginBottom: 4 }}
            items={[
              {
                href: '/kb',
                title: <><HomeOutlined style={{ marginRight: 4 }} />知识库列表</>,
              },
              {
                title: kbInfo?.name || '知识库详情',
              },
            ]}
          />
          <Typography.Title level={4} style={{ margin: 0 }}>
            {kbInfo?.name || '知识库详情'}
          </Typography.Title>
          {kbInfo?.description && (
            <Typography.Text type="secondary" style={{ fontSize: 13 }}>
              {kbInfo.description}
            </Typography.Text>
          )}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <KbInfoCards kbInfo={kbInfo} />
        </div>
      </div>
      </SlideInUp>

      {/* 工具栏 — 固定在顶部 */}
      <SlideInUp transition={{ type: "spring", stiffness: 300, damping: 30, delay: 0.05 }} style={{ flexShrink: 0 }}>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between' }}>
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
                        <Button danger loading={batchDeleteLoading}>批量删除 ({selectedRowKeys.length})</Button>
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
          onComplete={handleIndexingComplete}
        />
      )}

      {/* 表格区域 — 占满剩余空间，内部滚动 */}
      <SlideInUp transition={{ type: "spring", stiffness: 300, damping: 30, delay: 0.1 }} style={{ flex: 1, minHeight: 0 }}>
      <div ref={tableWrapperRef} style={{ height: '100%' }}>
      <Table
        columns={columns}
        dataSource={filteredData}
        rowKey="id"
        loading={loading}
        rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
        }}
        scroll={{ y: tableScrollY }}
        pagination={{
            current: currentPage,
            pageSize: pageSize,
            total: total,
            showSizeChanger: true,
            align: 'end',
            onChange: (page, size) => {
                setCurrentPage(page);
                setPageSize(size);
            }
        }}
      />
      </div>
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
                  <div style={{ background: `${token.colorPrimary}0A`, border: `1px solid ${token.colorPrimary}26`, borderRadius: 12, padding: 12, marginBottom: 16 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                          <Typography.Text strong>已选择 {fileList.length} 个文件</Typography.Text>
                          <Typography.Text type="secondary">共 {formatFileSize(totalSelectedSize)}</Typography.Text>
                      </div>
                      <div style={{ maxHeight: 180, overflowY: 'auto' }}>
                          {fileList.map(file => (
                              <div key={file.uid} style={{ display: 'flex', alignItems: 'center', padding: '6px 0', borderBottom: '1px solid #f0f0f0' }}>
                                  <FileTextOutlined style={{ color: token.colorPrimary, marginRight: 8 }} />
                                  <div style={{ flex: 1 }}>
                                      <div style={{ fontSize: 14 }}>{file.name}</div>
                                      <div style={{ fontSize: 12, color: '#a8a29e' }}>{formatFileSize(file.size || file.originFileObj?.size || 0)}</div>
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
      <ChunkDrawer
        open={chunkDrawerOpen}
        onClose={() => { setChunkDrawerOpen(false); setSelectedDocument(null); }}
        document={selectedDocument}
        kbId={id || ''}
        indexStrategyType={kbInfo?.indexStrategyType || 'NAIVE_RAG'}
      />
    </FadeIn>
  );
}
