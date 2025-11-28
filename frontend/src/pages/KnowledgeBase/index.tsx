import { Button, Tag, Space, Popconfirm, message, Modal, Form, Input, Select, Typography, List, Card } from 'antd';
import { useState, useEffect } from 'react';
import { PlusOutlined, DatabaseOutlined, MessageOutlined, FileTextOutlined, DeleteOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem } from '../../types';
import { FadeIn, StaggerContainer, StaggerItem } from '../../components/common/Motion';

export default function KnowledgeBasePage() {
  const navigate = useNavigate();
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<KnowledgeBaseItem[]>([]);
  const [embeddingModels, setEmbeddingModels] = useState<string[]>([]);

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
          // Construct payload based on backend requirements
          const payload = {
              name: values.name,
              description: values.description,
              embeddingModelId: values.embeddingModel,
              indexStrategyConfig: {
                  type: values.ragMethod === 'hisem' ? 'hisem' : 'naive',
                  // Add other default config if needed
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
    <div className="p-6 bg-gray-50 min-h-screen">
      <FadeIn>
        <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
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

      <StaggerContainer>
        <List
            grid={{ gutter: 16, column: 3 }}
            dataSource={data}
            loading={loading}
            renderItem={(item) => (
            <StaggerItem>
                <List.Item>
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
                                <Tag color="blue">{item.indexStrategyType === 'HISEM_RAG' ? 'HiSem RAG' : 'Naive RAG'}</Tag>
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
                </List.Item>
            </StaggerItem>
            )}
        />
      </StaggerContainer>

      <Modal
        title="新建知识库"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        onOk={() => form.submit()}
      >
          <Form form={form} layout="vertical" onFinish={handleCreate}>
              <Form.Item name="name" label="名称" rules={[{ required: true }]}>
                  <Input placeholder="请输入知识库名称" />
              </Form.Item>
              <Form.Item name="description" label="描述">
                  <Input.TextArea placeholder="请输入描述" />
              </Form.Item>
              <Form.Item name="ragMethod" label="RAG 方法" initialValue="naive">
                  <Select>
                      <Select.Option value="naive">Naive RAG</Select.Option>
                      <Select.Option value="hisem">HiSem RAG</Select.Option>
                  </Select>
              </Form.Item>
              <Form.Item name="embeddingModel" label="Embedding 模型" rules={[{ required: true }]}>
                  <Select placeholder="请选择 Embedding 模型">
                      {embeddingModels.map(model => (
                          <Select.Option key={model} value={model}>{model}</Select.Option>
                      ))}
                  </Select>
              </Form.Item>
          </Form>
      </Modal>
    </div>
  );
}
