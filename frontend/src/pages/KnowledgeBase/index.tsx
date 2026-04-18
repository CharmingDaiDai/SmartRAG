import { Button, Tag, Space, Popconfirm, message, Modal, Form, Input, Select, Typography, Card, Row, Col, Spin, Slider, Switch, Tooltip, Dropdown, Empty, theme } from 'antd';
import { useState, useEffect, MouseEvent } from 'react';
import { PlusOutlined, MessageOutlined, FileTextOutlined, DeleteOutlined, QuestionCircleOutlined, SyncOutlined, EllipsisOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem } from '../../types';
import { StaggerContainer, StaggerItem, SlideInUp, HoverCard, ScaleIn } from '../../components/common/Motion';
import { getMethodConfig, RAG_METHODS } from '../../config/ragConfig';
import { useAppStore } from '../../store/useAppStore';
import { documentService } from '../../services/documentService';

const INDEX_STRATEGY_TYPE_MAP: Record<string, string> = {
    [RAG_METHODS.NAIVE]: 'NAIVE_RAG',
    [RAG_METHODS.HISEM_FAST]: 'HISEM_RAG_FAST',
    [RAG_METHODS.HISEM]: 'HISEM_RAG',
};

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

const INDEX_STRATEGY_LABEL_MAP: Record<string, string> = {
    NAIVE_RAG: 'Naive RAG',
    HISEM_RAG_FAST: 'HiSem RAG Fast',
    HISEM_RAG: 'HiSem-SADP',
};

// RAG 策略的颜色点
const STRATEGY_COLORS: Record<string, string> = {
    NAIVE_RAG: '#6366f1',
    HISEM_RAG_FAST: '#0ea5e9',
    HISEM_RAG: '#8b5cf6',
};

// RAG 策略的 Tag 颜色
const STRATEGY_TAG_COLORS: Record<string, string> = {
    NAIVE_RAG: 'geekblue',
    HISEM_RAG_FAST: 'cyan',
    HISEM_RAG: 'purple',
};

const toCamelCase = (key: string) => key.replace(/_([a-z])/g, (_, char: string) => char.toUpperCase());

// RAG 方法选择卡片配置
const RAG_METHOD_CARDS = [
    {
        value: RAG_METHODS.NAIVE,
        label: 'Naive RAG',
        desc: '基础检索增强，按固定文本块切分和向量检索',
        scene: '适合：快速上手、小规模文档、低配置环境',
        color: '#6366f1',
        bg: 'rgba(99,102,241,0.06)',
    },
    {
        value: RAG_METHODS.HISEM_FAST,
        label: 'HiSem RAG Fast',
        desc: '语义感知切分，保留上下文结构，速度优先',
        scene: '适合：中大规模文档、对速度有要求',
        color: '#0ea5e9',
        bg: 'rgba(14,165,233,0.06)',
    },
    {
        value: RAG_METHODS.HISEM,
        label: 'HiSem-SADP',
        desc: '全语义增强，可选摘要压缩，检索精度最高',
        scene: '适合：高质量问答、允许较长建库时间',
        color: '#8b5cf6',
        bg: 'rgba(139,92,246,0.06)',
    },
];

export default function KnowledgeBasePage() {
  const navigate = useNavigate();
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<KnowledgeBaseItem[]>([]);
  const [embeddingModels, setEmbeddingModels] = useState<string[]>([]);
  const [llmModels, setLlmModels] = useState<string[]>([]);
  const [indexingKbId, setIndexingKbId] = useState<string | null>(null);
  const [deletingKbId, setDeletingKbId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const ragMethod = Form.useWatch('ragMethod', form);
  const splitterType = Form.useWatch('splitter_type', form);
  const chunkSize = Form.useWatch('chunk_size', form);
  const enableSemanticCompression = Form.useWatch('enableSemanticCompression', form);
  const { localSettings } = useAppStore();
  const { token } = theme.useToken();

  useEffect(() => {
      if (chunkSize) {
          const maxOverlap = Math.floor(chunkSize * 0.2);
          const currentOverlap = form.getFieldValue('chunk_overlap');
          if (currentOverlap > maxOverlap) {
              form.setFieldValue('chunk_overlap', maxOverlap);
          }
      }
  }, [chunkSize, form]);

  const fetchData = async () => {
      setLoading(true);
      try {
          const res: any = await kbService.list({});
          if (res.code === 200) {
             setData(res.data);
          } else {
             message.error(res.message || '获取知识库列表失败');
          }
      } catch (error) {
          // ignore
      } finally {
          setLoading(false);
      }
  };

  const fetchModels = async () => {
      try {
          const [embeddingRes, llmRes]: any[] = await Promise.all([
              modelService.getEmbeddings(),
              modelService.getLLMs(),
          ]);
          if (embeddingRes.code === 200) setEmbeddingModels(embeddingRes.data);
          if (llmRes.code === 200) setLlmModels(llmRes.data);
      } catch (error) {
          console.error('Failed to fetch models');
      }
  };

  useEffect(() => {
      fetchData();
      fetchModels();
  }, []);

  const handleDelete = async (id: string) => {
    setDeletingKbId(id);
    try {
      const res: any = await kbService.delete(id);
      if (res.code === 200) {
          message.success('删除成功');
          fetchData();
      } else {
          message.error(res.message || '删除失败');
      }
    } catch (error) {
      // ignore
    } finally {
      setDeletingKbId(null);
    }
  };

  const handleCreate = async (values: any) => {
      setCreating(true);
      try {
          const { name, description, ragMethod: method, embedding_model, ...rest } = values;
          const strategyType = INDEX_STRATEGY_TYPE_MAP[method] || method;

          const normalizedIndexConfig = Object.entries(rest).reduce<Record<string, any>>((acc, [key, value]) => {
              acc[toCamelCase(key)] = value;
              return acc;
          }, {});

          const payload = {
              name,
              description,
              embeddingModelId: embedding_model,
              indexStrategyConfig: {
                  type: strategyType,
                  strategyType,
                  ...normalizedIndexConfig
              }
          };

          const res: any = await kbService.create(payload);
          if (res.code === 200) {
              message.success('创建成功');
              setCreateModalOpen(false);
              form.resetFields();
              fetchData();
          } else {
              message.error(res.message || '创建失败');
          }
      } catch (error) {
          // ignore
      } finally {
          setCreating(false);
      }
  };

  const handleNavigateDetail = (kbId: string) => {
      navigate(`/kb/${kbId}`);
  };

  const handleTriggerBatchIndex = async (kbId: string, e?: MouseEvent) => {
      e?.stopPropagation();
      setIndexingKbId(kbId);
      try {
          const res: any = await documentService.triggerBatchIndex(kbId);
          if (res.code === 200) {
              message.success('已触发知识库索引构建');
          } else {
              message.error(res.message || '触发索引失败');
          }
      } catch (error) {
          message.error('触发索引失败');
      } finally {
          setIndexingKbId(null);
      }
  };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <SlideInUp>
        <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
            <div>
                <Typography.Title level={4} style={{ margin: 0 }}>知识库管理</Typography.Title>
                <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                    统一管理知识空间、索引策略与问答入口
                </Typography.Text>
            </div>
            <Button
                icon={<PlusOutlined />}
                aria-label="新建知识库"
                onClick={() => setCreateModalOpen(true)}
                type="primary"
            >
                新建知识库
            </Button>
        </div>
      </SlideInUp>

      <StaggerContainer style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingBottom: 24 }}>
        <Spin spinning={loading}>
            {data.length === 0 && !loading ? (
                <Empty
                    className="ui-empty-panel"
                    image={Empty.PRESENTED_IMAGE_SIMPLE}
                    description={
                        <div style={{ textAlign: 'center' }}>
                            <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
                                还没有知识库，创建第一个吧
                            </Typography.Text>
                            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
                                新建知识库
                            </Button>
                        </div>
                    }
                    style={{ marginTop: 80 }}
                />
            ) : (
                <Row gutter={[16, 16]} style={{ margin: 0 }}>
                    {data.map((item) => {
                        const strategyType = item.indexStrategyType || 'NAIVE_RAG';
                        const accentColor = STRATEGY_COLORS[strategyType] || '#6366f1';
                        const tagColor = STRATEGY_TAG_COLORS[strategyType] || 'geekblue';

                        const cardMenuItems = [
                            {
                                key: 'chat',
                                icon: <MessageOutlined />,
                                label: '开始对话',
                                onClick: ({ domEvent }: any) => { domEvent.stopPropagation(); navigate(`/chat?kbId=${item.id}`); },
                            },
                            {
                                key: 'docs',
                                icon: <FileTextOutlined />,
                                label: '文档管理',
                                onClick: ({ domEvent }: any) => { domEvent.stopPropagation(); navigate(`/kb/${item.id}`); },
                            },
                            {
                                key: 'index',
                                icon: <SyncOutlined spin={indexingKbId === item.id} />,
                                label: '构建索引',
                                onClick: ({ domEvent }: any) => handleTriggerBatchIndex(item.id, domEvent),
                            },
                            { type: 'divider' as const },
                            {
                                key: 'delete',
                                icon: <DeleteOutlined />,
                                label: (
                                    <Popconfirm
                                        title="确定删除这个知识库吗？"
                                        onConfirm={(e) => { e?.stopPropagation(); handleDelete(item.id); }}
                                        onCancel={(e) => e?.stopPropagation()}
                                        okText="删除"
                                        okButtonProps={{ danger: true }}
                                    >
                                        <span onClick={(e) => e.stopPropagation()}>删除知识库</span>
                                    </Popconfirm>
                                ),
                                danger: true,
                            },
                        ];

                        return (
                            <Col xs={24} md={12} xl={8} key={item.id}>
                                <StaggerItem>
                                    <HoverCard style={{ height: '100%' }}>
                                        <Card
                                            hoverable
                                            className="kb-card kb-grid-card"
                                            onClick={() => deletingKbId !== item.id && handleNavigateDetail(item.id)}
                                            style={{
                                                height: '100%',
                                                cursor: deletingKbId === item.id ? 'not-allowed' : 'pointer',
                                                position: 'relative',
                                                overflow: 'hidden',
                                                opacity: deletingKbId === item.id ? 0.5 : 1,
                                                transition: 'opacity 0.2s ease',
                                                pointerEvents: deletingKbId === item.id ? 'none' : undefined,
                                            }}
                                            styles={{ body: { padding: 20, minHeight: 180, display: 'flex', flexDirection: 'column', gap: 12 } }}
                                        >
                                            {/* 删除中遮罩 */}
                                            {deletingKbId === item.id && (
                                                <div style={{
                                                    position: 'absolute',
                                                    inset: 0,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    zIndex: 10,
                                                    pointerEvents: 'all',
                                                }}>
                                                    <Spin tip="删除中..." />
                                                </div>
                                            )}
                                            {/* 顶部彩色强调条 */}
                                            <div style={{
                                                position: 'absolute',
                                                top: 0,
                                                left: 0,
                                                right: 0,
                                                height: 3,
                                                background: accentColor,
                                                opacity: 0.8,
                                            }} />

                                            {/* 卡片头部：标题 + 操作菜单 */}
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginTop: 4 }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flex: 1, minWidth: 0 }}>
                                                    <div style={{
                                                        width: 8,
                                                        height: 8,
                                                        borderRadius: '50%',
                                                        background: accentColor,
                                                        flexShrink: 0,
                                                    }} />
                                                    <Typography.Text strong style={{ fontSize: 15, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                        {item.name}
                                                    </Typography.Text>
                                                </div>
                                                <Dropdown
                                                    menu={{ items: cardMenuItems }}
                                                    trigger={['click']}
                                                    placement="bottomRight"
                                                >
                                                    <Button
                                                        type="text"
                                                        icon={<EllipsisOutlined />}
                                                        aria-label={`打开知识库 ${item.name} 的更多操作`}
                                                        size="small"
                                                        onClick={(e) => e.stopPropagation()}
                                                        style={{ color: token.colorTextTertiary, flexShrink: 0 }}
                                                    />
                                                </Dropdown>
                                            </div>

                                            {/* 标签区 */}
                                            <Space size={4} wrap>
                                                <Tag color={tagColor} style={{ fontSize: 11 }}>
                                                    {INDEX_STRATEGY_LABEL_MAP[strategyType] || strategyType}
                                                </Tag>
                                                {item.embeddingModelId && (
                                                    <Tag style={{ fontSize: 11, fontFamily: "'JetBrains Mono', monospace" }}>
                                                        {item.embeddingModelId}
                                                    </Tag>
                                                )}
                                                <Tag style={{ fontSize: 11 }}>{`${item.documentCount || 0} 篇`}</Tag>
                                            </Space>

                                            {/* 描述 */}
                                            <Typography.Paragraph
                                                type="secondary"
                                                ellipsis={{ rows: 2 }}
                                                style={{ margin: 0, fontSize: 13, flex: 1 }}
                                            >
                                                {item.description || '暂无描述'}
                                            </Typography.Paragraph>

                                            {/* 底部信息 */}
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                                    更新于 {formatDateTime(item.updatedAt)}
                                                </Typography.Text>
                                                {/* 悬停 CTA */}
                                                <div className="kb-card-cta">
                                                    <Typography.Text style={{ fontSize: 12, color: accentColor }}>
                                                        进入知识库 →
                                                    </Typography.Text>
                                                </div>
                                            </div>
                                        </Card>
                                    </HoverCard>
                                </StaggerItem>
                            </Col>
                        );
                    })}
                </Row>
            )}
        </Spin>
      </StaggerContainer>

      {/* 新建知识库 Modal */}
      <Modal
        title="新建知识库"
        open={createModalOpen}
        onCancel={() => { if (!creating) { setCreateModalOpen(false); form.resetFields(); } }}
        onOk={() => form.submit()}
        confirmLoading={creating}
        width={620}
        modalRender={(modal) => <ScaleIn>{modal}</ScaleIn>}
      >
          <Form form={form} layout="vertical" onFinish={handleCreate} initialValues={{ ragMethod: RAG_METHODS.NAIVE }}>
              <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入知识库名称' }]}>
                  <Input placeholder="请输入知识库名称" />
              </Form.Item>
              <Form.Item name="description" label="描述">
                  <Input.TextArea placeholder="请输入描述（可选）" rows={2} />
              </Form.Item>

              {/* RAG 方法选择卡片 */}
              <Form.Item name="ragMethod" label="RAG 策略" rules={[{ required: true }]}>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
                      {RAG_METHOD_CARDS.map((card) => (
                          <div
                              key={card.value}
                              onClick={() => form.setFieldValue('ragMethod', card.value)}
                              style={{
                                  padding: '12px',
                                  borderRadius: 10,
                                  border: `2px solid ${ragMethod === card.value ? card.color : token.colorBorder}`,
                                  background: ragMethod === card.value ? card.bg : token.colorBgContainer,
                                  cursor: 'pointer',
                                  transition: 'background-color 0.18s ease, border-color 0.18s ease',
                              }}
                          >
                              <div style={{
                                  width: 8,
                                  height: 8,
                                  borderRadius: '50%',
                                  background: card.color,
                                  marginBottom: 6,
                              }} />
                              <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 4 }}>{card.label}</div>
                              <div style={{ fontSize: 11, color: token.colorTextSecondary, lineHeight: 1.5, marginBottom: 4 }}>{card.desc}</div>
                              <div style={{ fontSize: 11, color: token.colorTextTertiary, lineHeight: 1.4 }}>{card.scene}</div>
                          </div>
                      ))}
                  </div>
              </Form.Item>

              {/* 动态配置项 */}
              {ragMethod && getMethodConfig(ragMethod).indexConfig.map((item: any) => {
                  if (item.dependency) {
                      const depFieldMap: Record<string, any> = {
                          'splitter_type': splitterType,
                          'enableSemanticCompression': enableSemanticCompression,
                      };
                      const depValue = depFieldMap[item.dependency.field] ?? form.getFieldValue(item.dependency.field);
                      if (depValue !== item.dependency.value) return null;
                  }

                  let inputNode = <Input />;
                  let initialValue = item.defaultValue;

                  if (item.type === 'select') {
                      inputNode = (
                          <Select>
                              {item.options?.map((opt: any) => (
                                  <Select.Option key={opt.value} value={opt.value}>{opt.label}</Select.Option>
                              ))}
                          </Select>
                      );
                  } else if (item.type === 'model_select') {
                      const modelList = item.modelType === 'llm' ? llmModels : embeddingModels;
                      if (item.modelType === 'embedding' && localSettings?.defaultEmbedding) {
                          initialValue = localSettings.defaultEmbedding;
                      }
                      if (item.modelType === 'llm' && localSettings?.defaultModel) {
                          initialValue = localSettings.defaultModel;
                      }
                      inputNode = (
                          <Select placeholder={`请选择 ${item.label}`}>
                              {modelList.map(model => (
                                  <Select.Option key={model} value={model}>{model}</Select.Option>
                              ))}
                          </Select>
                      );
                  } else if (item.type === 'slider') {
                      const max = item.dynamicMaxRatio ? Math.floor((chunkSize || 512) * item.dynamicMaxRatio) : item.max;
                      const marks = item.marks || { [item.min]: item.min, [max]: max };
                      inputNode = <Slider min={item.min} max={max} step={item.step} marks={marks} />;
                  } else if (item.type === 'switch') {
                      inputNode = <Switch />;
                  } else if (item.type === 'input') {
                      inputNode = <Input />;
                  }

                  const labelNode = item.tooltip ? (
                      <Space>
                          {item.label}
                          <Tooltip title={item.tooltip}>
                              <QuestionCircleOutlined style={{ color: token.colorTextTertiary }} />
                          </Tooltip>
                      </Space>
                  ) : item.label;

                  return (
                    <Form.Item
                        key={item.key}
                        name={item.key}
                        label={labelNode}
                        rules={item.type === 'switch' ? undefined : [{ required: true, message: `请输入${item.label}` }]}
                        initialValue={initialValue}
                        valuePropName={item.type === 'switch' ? 'checked' : 'value'}
                    >
                        {inputNode}
                    </Form.Item>
                  );
              })}
          </Form>
      </Modal>
    </div>
  );
}
