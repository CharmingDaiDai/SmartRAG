import { Button, Tag, Space, Popconfirm, message, Modal, Form, Input, Select, Typography, Card, Row, Col, Spin, Slider, InputNumber, Switch, Tooltip } from 'antd';
import { useState, useEffect } from 'react';
import { PlusOutlined, DatabaseOutlined, MessageOutlined, FileTextOutlined, DeleteOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem } from '../../types';
import { FadeIn, StaggerContainer, StaggerItem } from '../../components/common/Motion';
import { getMethodConfig, RAG_METHODS } from '../../config/ragConfig';
import { useAppStore } from '../../store/useAppStore';

const INDEX_STRATEGY_TYPE_MAP: Record<string, string> = {
    [RAG_METHODS.NAIVE]: 'NAIVE_RAG',
    [RAG_METHODS.HISEM_FAST]: 'HISEM_RAG_FAST',
    [RAG_METHODS.HISEM]: 'HISEM_RAG',
};

const INDEX_STRATEGY_LABEL_MAP: Record<string, string> = {
    NAIVE_RAG: 'Naive RAG',
    HISEM_RAG_FAST: 'HiSem RAG Fast',
    HISEM_RAG: 'Graph RAG',
};

const toCamelCase = (key: string) => key.replace(/_([a-z])/g, (_, char: string) => char.toUpperCase());

export default function KnowledgeBasePage() {
  const navigate = useNavigate();
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<KnowledgeBaseItem[]>([]);
  const [embeddingModels, setEmbeddingModels] = useState<string[]>([]);
  const ragMethod = Form.useWatch('ragMethod', form);
  const splitterType = Form.useWatch('splitter_type', form);
  const chunkSize = Form.useWatch('chunk_size', form);
  const { localSettings } = useAppStore();

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
          // message.error('获取知识库列表失败');
      } finally {
          setLoading(false);
      }
  };

  const fetchModels = async () => {
      try {
          const res: any = await modelService.getEmbeddings();
          if (res.code === 200) {
              setEmbeddingModels(res.data);
          }
      } catch (error) {
          console.error('Failed to fetch models');
      }
  };

  useEffect(() => {
      fetchData();
      fetchModels();
  }, []);

  const handleDelete = async (id: string) => {
    try {
      const res: any = await kbService.delete(id);
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

  const handleCreate = async (values: any) => {
      try {
          const { name, description, ragMethod, embedding_model, ...rest } = values;
          const strategyType = INDEX_STRATEGY_TYPE_MAP[ragMethod] || ragMethod;
          
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
          // message.error('创建失败');
      }
  };

  return (
    <div className="p-6 bg-gray-50" style={{ height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <FadeIn>
        <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexShrink: 0 }}>
            <Typography.Title level={4} style={{ margin: 0 }}>知识库列表</Typography.Title>
            <Button
                icon={<PlusOutlined />}
                onClick={() => setCreateModalOpen(true)}
                type="primary"
            >
                新建知识库
            </Button>
        </div>
      </FadeIn>

      <StaggerContainer style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingBottom: 24 }}>
        <Spin spinning={loading}>
            <Row gutter={[16, 16]} style={{ margin: 0 }}>
                {data.map((item) => (
                    <Col span={8} key={item.id}>
                        <StaggerItem>
                            <Card 
                                title={
                                    <Space>
                                        <DatabaseOutlined className="text-blue-500" style={{ fontSize: 20 }} />
                                        <span style={{ fontSize: 16, fontWeight: 'bold' }}>{item.name}</span>
                                    </Space>
                                }
                                actions={[
                                    <Button type="link" key="chat" icon={<MessageOutlined />} onClick={() => navigate(`/chat?kbId=${item.id}`)}>
                                        对话
                                    </Button>,
                                    <Button type="link" key="docs" icon={<FileTextOutlined />} onClick={() => navigate(`/kb/${item.id}`)}>
                                        文档
                                    </Button>,
                                    <Popconfirm
                                        key="delete"
                                        title="确定删除吗?"
                                        onConfirm={() => handleDelete(item.id)}
                                    >
                                        <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
                                    </Popconfirm>,
                                ]}
                            >
                                <div style={{ marginBottom: 16 }}>
                                    <Space size={4}>
                                        <Tag color="blue">{INDEX_STRATEGY_LABEL_MAP[item.indexStrategyType || 'NAIVE_RAG'] || item.indexStrategyType}</Tag>
                                        <Tag color="green">{item.embeddingModelId}</Tag>
                                    </Space>
                                </div>
                                <div className="h-10 overflow-hidden text-gray-500" style={{ marginBottom: 16 }}>
                                    {item.description || '暂无描述'}
                                </div>
                                <div className="flex items-center text-gray-500">
                                    <FileTextOutlined className="mr-2" />
                                    <span>{item.documentCount || 0} 文档</span>
                                </div>
                            </Card>
                        </StaggerItem>
                    </Col>
                ))}
            </Row>
        </Spin>
      </StaggerContainer>

      <Modal
        title="新建知识库"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => form.submit()}
        width={600}
      >
          <Form form={form} layout="vertical" onFinish={handleCreate} initialValues={{ ragMethod: RAG_METHODS.NAIVE }}>
              <Form.Item name="name" label="名称" rules={[{ required: true }]}>
                  <Input placeholder="请输入知识库名称" />
              </Form.Item>
              <Form.Item name="description" label="描述">
                  <Input.TextArea placeholder="请输入描述" />
              </Form.Item>
              <Form.Item name="ragMethod" label="RAG 方法">
                  <Select>
                      <Select.Option value={RAG_METHODS.NAIVE}>Naive RAG</Select.Option>
                      <Select.Option value={RAG_METHODS.HISEM_FAST}>HiSem RAG Fast</Select.Option>
                      <Select.Option value={RAG_METHODS.HISEM}>Graph RAG</Select.Option>
                  </Select>
              </Form.Item>
              
              {ragMethod && getMethodConfig(ragMethod).indexConfig.map((item: any) => {
                  if (item.dependency) {
                      const depValue = item.dependency.field === 'splitter_type' ? splitterType : form.getFieldValue(item.dependency.field);
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
                      if (item.modelType === 'embedding' && localSettings?.defaultEmbedding) {
                          initialValue = localSettings.defaultEmbedding;
                      }
                      inputNode = (
                          <Select placeholder={`请选择 ${item.label}`}>
                              {embeddingModels.map(model => (
                                  <Select.Option key={model} value={model}>{model}</Select.Option>
                              ))}
                          </Select>
                      );
                  } else if (item.type === 'slider') {
                      const max = item.dynamicMaxRatio ? Math.floor((chunkSize || 512) * item.dynamicMaxRatio) : item.max;
                      inputNode = <Slider min={item.min} max={max} step={item.step} marks={{ [item.min]: item.min, [max]: max }} />;
                  } else if (item.type === 'input') {
                      inputNode = <Input />;
                  }

                  return (
                    <Form.Item 
                        key={item.key} 
                        name={item.key} 
                        label={item.label}
                        rules={[{ required: true, message: `请输入${item.label}` }]}
                        initialValue={initialValue}
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
