import { Button, Space, Popconfirm, Upload, Modal, Tag, Table, Input, Select, Form, Alert, Typography, Tooltip, App, Empty, theme } from 'antd';
import { useState, useEffect, useRef, useCallback } from 'react';
import { PlusOutlined, FilePdfOutlined, FileWordOutlined, FileTextOutlined, SearchOutlined, FileExcelOutlined, FilePptOutlined, FileMarkdownOutlined, FileImageOutlined, FileZipOutlined, CloseOutlined, InboxOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { useSearchParams } from 'react-router-dom';
import { documentService } from '../../services/documentService';
import { kbService } from '../../services/kbService';
import { useAppStore } from '../../store/useAppStore';
import { DocumentItem, KnowledgeBaseItem } from '../../types';
import type { ColumnsType } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import { FadeIn, SlideInUp, ScaleIn } from '../../components/common/Motion';
import DocumentViewer from '../../components/DocumentViewer';
import { formatRelativeDateTime } from '../../utils/formatters';

const DEFAULT_PAGE = 1;
const DEFAULT_PAGE_SIZE = 10;

const parsePositiveIntParam = (value: string | null, fallback: number) => {
    if (!value) return fallback;

    const parsed = Number(value);
    if (!Number.isInteger(parsed) || parsed <= 0) {
        return fallback;
    }

    return parsed;
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

export default function DocumentsPage() {
  const { message } = App.useApp();
  const { token } = theme.useToken();
    const [searchParams, setSearchParams] = useSearchParams();
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<DocumentItem[]>([]);
  const [total, setTotal] = useState(0);
    const [currentPage, setCurrentPage] = useState(() => parsePositiveIntParam(searchParams.get('page'), DEFAULT_PAGE));
    const [pageSize, setPageSize] = useState(() => parsePositiveIntParam(searchParams.get('size'), DEFAULT_PAGE_SIZE));
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
    const [searchText, setSearchText] = useState(() => searchParams.get('q') || '');
  const [kbs, setKbs] = useState<KnowledgeBaseItem[]>([]);
    const [filterKbId, setFilterKbId] = useState<string | undefined>(() => searchParams.get('kbId') || undefined);
  const { currentKbId, setCurrentKbId } = useAppStore();
    const [uploadForm] = Form.useForm();
        const [fileList, setFileList] = useState<UploadFile[]>([]);
        const [uploading, setUploading] = useState(false);
        const [deletingDocIds, setDeletingDocIds] = useState<Record<string, boolean>>({});
        const [batchDeleteLoading, setBatchDeleteLoading] = useState(false);
        const [viewerOpen, setViewerOpen] = useState(false);
        const [viewingDocument, setViewingDocument] = useState<DocumentItem | null>(null);

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

  const fetchKbs = async () => {
      try {
          const res: any = await kbService.list({});
          if (res.code === 200) {
              setKbs(res.data);
              if (!currentKbId && res.data.length > 0) {
                  setCurrentKbId(res.data[0].id);
              }
          }
      } catch (error) {
          console.error(error);
      }
  };

  const fetchData = async (page = currentPage, size = pageSize) => {
      setLoading(true);
      try {
          const params: any = {
              page: page - 1, // Backend uses 0-based index
              size: size,
          };

          let res: any;
          if (filterKbId) {
              // 使用 listByKb 按知识库筛选
              res = await documentService.listByKb(String(filterKbId), params);
          } else {
              // 获取所有文档
              res = await documentService.listAll(params);
          }

          if (res.code === 200) {
              // Assuming the response structure for pagination is data: { content: [], totalElements: 0 }
              // Adjust based on actual backend response structure if different
              if (res.data && Array.isArray(res.data.content)) {
                  setData(res.data.content);
                  setTotal(res.data.totalElements);
              } else if (Array.isArray(res.data)) {
                  // Fallback for non-paginated response or different structure
                  setData(res.data);
                  setTotal(res.data.length);
              }
          } else {
              message.error(res.message || '获取文档列表失败');
          }
      } catch (error) {
          message.error('获取文档列表失败，请稍后重试');
      } finally {
          setLoading(false);
      }
  };

  useEffect(() => {
      fetchKbs();
  }, []);

  useEffect(() => {
      const nextPage = parsePositiveIntParam(searchParams.get('page'), DEFAULT_PAGE);
      const nextPageSize = parsePositiveIntParam(searchParams.get('size'), DEFAULT_PAGE_SIZE);
      const nextSearchText = searchParams.get('q') || '';
      const nextKbId = searchParams.get('kbId') || undefined;

      setCurrentPage(prev => (prev === nextPage ? prev : nextPage));
      setPageSize(prev => (prev === nextPageSize ? prev : nextPageSize));
      setSearchText(prev => (prev === nextSearchText ? prev : nextSearchText));
      setFilterKbId(prev => (prev === nextKbId ? prev : nextKbId));
  }, [searchParams]);

  useEffect(() => {
      const nextParams = new URLSearchParams();

      if (searchText) {
          nextParams.set('q', searchText);
      }
      if (filterKbId) {
          nextParams.set('kbId', String(filterKbId));
      }
      if (currentPage !== DEFAULT_PAGE) {
          nextParams.set('page', String(currentPage));
      }
      if (pageSize !== DEFAULT_PAGE_SIZE) {
          nextParams.set('size', String(pageSize));
      }

      if (nextParams.toString() !== searchParams.toString()) {
          setSearchParams(nextParams, { replace: true });
      }
  }, [searchText, filterKbId, currentPage, pageSize, searchParams, setSearchParams]);

  useEffect(() => {
      fetchData(currentPage, pageSize);
  }, [filterKbId, currentPage, pageSize]);

  const handleDelete = async (id: string) => {
    setDeletingDocIds(prev => ({ ...prev, [id]: true }));
    try {
      const res: any = await documentService.delete(id);
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
          delete next[id];
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

  const handleBatchUpload = async () => {
      const kbId = uploadForm.getFieldValue('kbId');
      if (!kbId) {
          message.error('请先选择知识库');
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
          const res: any = await documentService.batchUpload(kbId, files, titles);
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
      dataIndex: 'filename', // Changed from title to filename based on OpenAPI
      ellipsis: true,
            render: (text, entity) => {
                const fileName = text || entity.filename || entity.fileName || '未命名文档';
                return (
                    <Space style={{ width: '100%', minWidth: 0 }}>
                            {getFileIcon(fileName, token.colorPrimary)}
                            <Typography.Text
                            ellipsis={{ tooltip: fileName }}
                            style={{ maxWidth: 320, minWidth: 0, fontSize: 16, fontWeight: 600, color: '#1e293b' }}
                            >
                                    {fileName}
                            </Typography.Text>
                    </Space>
                );
            },
    },
    {
      title: '所属知识库',
      dataIndex: 'kbId',
      width: 150,
      render: (kbId) => {
          const kb = kbs.find(k => k.id === kbId);
          if (!kb) return kbId;
          return (
              <Tooltip title={kb.name}>
                  <Tag
                      className="docs-kb-tag"
                      style={{
                          maxWidth: 140,
                          display: 'inline-block',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          margin: 0,
                      }}
                  >
                      {kb.name}
                  </Tag>
              </Tooltip>
          );
      },
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      width: 100,
            render: (size) => (
                    <Typography.Text style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
                            {(size / 1024 / 1024).toFixed(2)} MB
                    </Typography.Text>
            ),
    },
    {
      title: '状态',
      dataIndex: 'indexStatus', // Changed from status to indexStatus
      width: 120,
      render: (status) => {
          const statusMap: any = {
              UPLOADED: { text: '已上传', color: '#a8a29e' },
              READING: { text: '读取中', color: token.colorPrimary },
              PARSING: { text: '解析中', color: '#3b82f6' },
              CHUNKING: { text: '切分中', color: '#06b6d4' },
              TREE_BUILDING: { text: '构建语义树', color: '#84cc16' },
              LLM_ENRICHING: { text: '语义增强中', color: '#eab308' },
              SAVING: { text: '保存中', color: '#f97316' },
              EMBEDDING: { text: '向量化中', color: '#8b5cf6' },
              STORING: { text: '存储向量', color: '#f97316' },
              INDEXED: { text: '已索引', color: '#00b42a' },
              ERROR: { text: '错误', color: '#ef4444' },
          };
          const s = statusMap[status] || { text: status, color: '#a8a29e' };
          if (status === 'INDEXED') {
              return (
                  <Tag className="docs-status-pill docs-status-pill--success" style={{ margin: 0 }}>
                      {s.text}
                  </Tag>
              );
          }
          return (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
                  <span style={{ width: 6, height: 6, borderRadius: '50%', background: s.color, display: 'inline-block', flexShrink: 0 }} />
                  <span style={{ color: s.color, fontWeight: 500 }}>{s.text}</span>
              </span>
          );
      }
    },
    {
      title: '上传时间',
      dataIndex: 'uploadTime', // Changed from createdAt
      width: 180,
      sorter: (a: any, b: any) => new Date(a.uploadTime).getTime() - new Date(b.uploadTime).getTime(),
            render: (val: string) => (
                    <Typography.Text style={{ fontSize: 12, fontWeight: 400, color: '#64748b' }}>
                    {formatRelativeDateTime(val)}
                    </Typography.Text>
            ),
    },
    {
      title: '操作',
      key: 'option',
      width: 120,
      render: (_, record) => (
        <Space>
            <Tooltip title="查看">
                <Button
                    type="text"
                    className="docs-action-btn docs-action-btn--view"
                    aria-label="查看文档"
                    icon={<EyeOutlined />}
                    onClick={() => {
                        setViewingDocument(record);
                        setViewerOpen(true);
                    }}
                />
            </Tooltip>
            <Popconfirm
                title="确定删除吗?"
                onConfirm={() => handleDelete(record.id)}
            >
                <Tooltip title="删除">
                    <Button
                        type="text"
                        danger
                        className="docs-action-btn docs-action-btn--delete"
                        aria-label="删除文档"
                        icon={<DeleteOutlined />}
                        loading={!!deletingDocIds[record.id]}
                    />
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
        <FadeIn className="documents-page" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* 工具栏 — 固定在顶部，不随表格滚动 */}
        <SlideInUp style={{ flexShrink: 0 }}>
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
                <Space>
                    <Input
                        placeholder="搜索文档"
                        prefix={<SearchOutlined />}
                        allowClear
                        aria-label="搜索文档"
                        value={searchText}
                        onChange={e => {
                            setSearchText(e.target.value);
                            setCurrentPage(DEFAULT_PAGE);
                        }}
                        style={{ width: 200 }}
                    />
                    <Select
                        style={{ width: 200 }}
                        placeholder="筛选知识库"
                        aria-label="筛选知识库"
                        allowClear
                        value={filterKbId}
                        onChange={(val) => {
                            setFilterKbId(val);
                            setCurrentPage(DEFAULT_PAGE);
                        }}
                    >
                        {kbs.map(kb => (
                            <Select.Option key={kb.id} value={kb.id}>{kb.name}</Select.Option>
                        ))}
                    </Select>
                </Space>
                <Space>
                    {selectedRowKeys.length > 0 && (
                        <Popconfirm title="确定删除选中的文档吗?" onConfirm={handleBatchDelete}>
                            <Button className="documents-toolbar-btn" danger loading={batchDeleteLoading}>批量删除 ({selectedRowKeys.length})</Button>
                        </Popconfirm>
                    )}
                    <Button
                        className="documents-toolbar-btn"
                        icon={<PlusOutlined />}
                        onClick={() => {
                            uploadForm.setFieldsValue({ kbId: currentKbId });
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

        {/* 表格区域 — 占满剩余空间，内部滚动 */}
        <SlideInUp transition={{ type: "spring", stiffness: 300, damping: 30, delay: 0.1 }} style={{ flex: 1, minHeight: 0 }}>
            <div ref={tableWrapperRef} className="documents-table-shell" style={{ height: '100%' }}>
            <Table
                className="documents-table"
                columns={columns}
                dataSource={filteredData}
                rowKey="id"
                loading={loading}
                rowSelection={{
                    selectedRowKeys,
                    onChange: (keys) => setSelectedRowKeys(keys),
                }}
                scroll={{ x: 1000, y: tableScrollY }}
                locale={{
                    emptyText: (
                        <Empty
                            image={Empty.PRESENTED_IMAGE_SIMPLE}
                            description={searchText ? '没有匹配的文档，请调整关键词' : '暂无文档，先上传一批文件吧'}
                        />
                    ),
                }}
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
                <Form.Item name="kbId" label="选择知识库" rules={[{ required: true }]}>
                    <Select placeholder="请选择知识库">
                        {kbs.map(kb => (
                            <Select.Option key={kb.id} value={kb.id}>{kb.name}</Select.Option>
                        ))}
                    </Select>
                </Form.Item>
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
                                    <Button type="text" aria-label={`移除文件 ${file.name}`} icon={<CloseOutlined />} onClick={() => setFileList(prev => prev.filter(item => item.uid !== file.uid))} />
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
        <DocumentViewer
            open={viewerOpen}
            onClose={() => {
                setViewerOpen(false);
                setViewingDocument(null);
            }}
            document={viewingDocument}
        />
    </FadeIn>
  );
}
