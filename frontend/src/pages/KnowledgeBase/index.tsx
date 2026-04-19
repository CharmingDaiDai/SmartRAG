import { Button, Tag, Space, Popconfirm, message, Modal, Form, Input, Select, Typography, Card, Row, Col, Spin, Slider, Switch, Tooltip, Dropdown, Empty, theme } from 'antd';
import { useState, useEffect, MouseEvent } from 'react';
import { PlusOutlined, MessageOutlined, FileTextOutlined, DeleteOutlined, QuestionCircleOutlined, SyncOutlined, EllipsisOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem } from '../../types';
import { StaggerContainer, StaggerItem, SlideInUp, HoverCard, ScaleIn } from '../../components/common/Motion';
import { getMethodConfig, normalizeStrategyType, RAG_METHODS } from '../../config/ragConfig';
import { useAppStore } from '../../store/useAppStore';
import { documentService } from '../../services/documentService';
import { formatRelativeDateTime } from '../../utils/formatters';

const INDEX_STRATEGY_TYPE_MAP: Record<string, string> = {
    [RAG_METHODS.NAIVE]: 'NAIVE_RAG',
    [RAG_METHODS.HISEM_FAST]: 'HISEM_RAG_FAST',
    [RAG_METHODS.HISEM]: 'HISEM_RAG',
};

const INDEX_STRATEGY_LABEL_MAP: Record<string, string> = {
    NAIVE_RAG: 'Naive RAG',
    HISEM_RAG_FAST: 'HiSem RAG Fast',
    HISEM_RAG: 'HiSem-SADP',
};

// RAG 策略的颜色点
const STRATEGY_COLORS: Record<string, string> = {
    NAIVE_RAG: '#86909c',
    HISEM_RAG_FAST: '#13c2c2',
    HISEM_RAG: '#722ed1',
};

const toCamelCase = (key: string) => key.replace(/_([a-z])/g, (_, char: string) => char.toUpperCase());

// RAG 方法选择卡片配置
const RAG_METHOD_CARDS = [
    {
        value: RAG_METHODS.NAIVE,
        label: 'Naive RAG',
        desc: '基础检索增强，按固定文本块切分和向量检索',
        scene: '适合：快速上手、小规模文档、低配置环境',
        color: '#86909c',
        bg: 'rgba(134,144,156,0.08)',
    },
    {
        value: RAG_METHODS.HISEM_FAST,
        label: 'HiSem RAG Fast',
        desc: '语义感知切分，保留上下文结构，速度优先',
        scene: '适合：中大规模文档、对速度有要求',
        color: '#13c2c2',
        bg: 'rgba(19,194,194,0.08)',
    },
    {
        value: RAG_METHODS.HISEM,
        label: 'HiSem-SADP',
        desc: '全语义增强，可选摘要压缩，检索精度最高',
        scene: '适合：高质量问答、允许较长建库时间',
        color: '#722ed1',
        bg: 'rgba(114,46,209,0.08)',
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
    <div className="knowledge-base-page" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
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
                <Row gutter={[24, 24]} style={{ margin: 0 }}>
                    {data.map((item) => {
                        const strategyType = normalizeStrategyType(item.indexStrategyType);
                        const strategyLabel = INDEX_STRATEGY_LABEL_MAP[strategyType] || 'Naive RAG';
                        const accentColor = STRATEGY_COLORS[strategyType] || '#86909c';

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
                                            role="link"
                                            tabIndex={deletingKbId === item.id ? -1 : 0}
                                            aria-label={`进入知识库 ${item.name}`}
                                            onKeyDown={(e) => {
                                                if (deletingKbId === item.id) return;
                                                if ((e.key === 'Enter' || e.key === ' ') && e.currentTarget === e.target) {
                                                    e.preventDefault();
                                                    handleNavigateDetail(item.id);
                                                }
                                            }}
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
                                                    <Typography.Text strong style={{ fontSize: 16, fontWeight: 600, color: '#1e293b', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
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
                                                        className="kb-card-menu-btn"
                                                        icon={<EllipsisOutlined />}
                                                        aria-label={`打开知识库 ${item.name} 的更多操作`}
                                                        size="small"
                                                        onClick={(e) => e.stopPropagation()}
                                                        style={{ color: token.colorTextTertiary, flexShrink: 0 }}
                                                    />
                                                </Dropdown>
                                            </div>

                                            {/* 标签区 */}
                                            <div className="kb-tag-row">
                                                <Tag
                                                    className="kb-tag-pill kb-tag-pill--strategy"
                                                    style={{
                                                        fontSize: 11,
                                                        color: '#ffffff',
                                                        background: accentColor,
                                                        borderColor: accentColor,
                                                    }}
                                                >
                                                    {strategyLabel}
                                                </Tag>
                                                {item.embeddingModelId && (
                                                    <Tooltip title={item.embeddingModelId}>
                                                        <Tag
                                                            className="kb-tag-pill kb-tag-pill--outline"
                                                            style={{
                                                                fontSize: 11,
                                                                fontFamily: "'JetBrains Mono', monospace",
                                                                maxWidth: 190,
                                                                display: 'inline-flex',
                                                                alignItems: 'center',
                                                                overflow: 'hidden',
                                                                textOverflow: 'ellipsis',
                                                                whiteSpace: 'nowrap',
                                                            }}
                                                        >
                                                            {item.embeddingModelId}
                                                        </Tag>
                                                    </Tooltip>
                                                )}
                                                <Tag className="kb-tag-pill kb-tag-pill--outline kb-tag-pill--neutral" style={{ fontSize: 11 }}>
                                                    {`${item.documentCount || 0} 篇`}
                                                </Tag>
                                            </div>

                                            {/* 描述 */}
                                            <Typography.Paragraph
                                                type="secondary"
                                                ellipsis={{ rows: 2 }}
                                                style={{
                                                    margin: 0,
                                                    fontSize: item.description ? 13 : 11,
                                                    fontWeight: 400,
                                                    color: item.description ? token.colorTextSecondary : '#94a3b8',
                                                    flex: 1,
                                                }}
                                            >
                                                {item.description || '暂无描述'}
                                            </Typography.Paragraph>

                                            {/* 底部信息 */}
                                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                                    更新于 {formatRelativeDateTime(item.updatedAt)}
                                                </Typography.Text>
                                                {/* 悬停 CTA */}
                                                <div className="kb-card-cta">
                                                    <Typography.Text className="kb-enter-link" style={{ fontSize: 12 }}>
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
