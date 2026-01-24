import React, {useEffect, useState} from "react";
import {
    App,
    Avatar,
    Badge,
    Button,
    Card,
    Col,
    Divider,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Radio,
    Row,
    Select,
    Space,
    Spin,
    Table,
    Tag,
    Tooltip,
    Typography,
} from "antd";
import {
    BookOutlined,
    DeleteOutlined,
    ExclamationCircleOutlined,
    EyeOutlined,
    PlusOutlined,
    QuestionCircleOutlined,
    SearchOutlined,
    UploadOutlined,
} from "@ant-design/icons";
import {useAuth} from "../../context/AuthContext";
import api, {ragMethodAPI} from "../../services/api";
import RagMethodParams from "../../components/knowledge_base/RagMethodParams";
import {ragMethods} from "../../config/ragConfig";
import {useNavigate} from "react-router-dom";

const { Title, Paragraph, Text } = Typography;
const { Search } = Input;
const { Option } = Select;
const { confirm } = Modal;

const KnowledgeBaseManagement = () => {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [knowledgeBases, setKnowledgeBases] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [selectedRagMethod, setSelectedRagMethod] = useState(null);
  const [ragParams, setRagParams] = useState({});
  const [embeddingModels, setEmbeddingModels] = useState([]);
  const [loadingModels, setLoadingModels] = useState(false);
  const [viewMode, setViewMode] = useState("card"); // 'card' or 'table'
  const [form] = Form.useForm();
  // 获取app实例，用于Modal.confirm，解决antd v5的Hook调用问题
  const app = App.useApp();

  // 获取所有知识库
  const fetchKnowledgeBases = async () => {
    setLoading(true);
    try {
      const response = await api.get("/api/kb/list");
      // 直接使用response.data，不再检查success字段
      if (response && response.data) {
        console.log("获取到知识库列表:", response.data);
        setKnowledgeBases(response.data || []);
      } else {
        console.error("获取知识库列表失败:", response);
        message.error("获取知识库列表失败，返回数据格式不正确");
      }
    } catch (error) {
      console.error("获取知识库列表失败:", error);
      message.error("获取知识库列表失败，请稍后再试");
    } finally {
      setLoading(false);
    }
  };

  // 获取嵌入模型列表
  const fetchEmbeddingModels = async () => {
    setLoadingModels(true);
    try {
      const response = await ragMethodAPI.getEmbeddingModels();
      // 检查返回数据格式，适配后端API响应
      if (response && response.data) {
        setEmbeddingModels(response.data || []);
        // 如果有模型，默认选中第一个
        if (response.data && response.data.length > 0) {
          form.setFieldsValue({ embeddingModel: response.data[0].value });
        }
      } else {
        console.error("获取嵌入模型列表返回格式不正确:", response);
        message.error("获取嵌入模型列表失败，返回格式不正确");
      }
    } catch (error) {
      console.error("获取嵌入模型列表失败:", error);
      message.error("获取嵌入模型列表失败，请联系管理员");
    } finally {
      setLoadingModels(false);
    }
  };

  useEffect(() => {
    fetchKnowledgeBases();
    fetchEmbeddingModels();
  }, []);

  // 打开创建模态框时初始化RAG方法
  useEffect(() => {
    if (createModalVisible) {
      // 如果有方法，默认选择第一个
      if (ragMethods.length > 0) {
        const defaultMethod = ragMethods[0].id;
        setSelectedRagMethod(defaultMethod);
        form.setFieldsValue({ ragMethod: defaultMethod });

        // 主动初始化默认RAG方法的参数
        import("../../config/ragConfig").then(({ getMethodConfig }) => {
          try {
            const methodConfig = getMethodConfig(defaultMethod);
            if (methodConfig && methodConfig.indexParams) {
              // 只取索引参数，不包括搜索参数
              const defaultParams = { ...methodConfig.indexParams };
              console.log("默认RAG方法参数初始化:", defaultParams);
              setRagParams(defaultParams);
            }
          } catch (error) {
            console.error("初始化默认RAG参数失败:", error);
          }
        });
      }

      // 如果已经加载了嵌入模型，默认选择第一个
      if (embeddingModels.length > 0) {
        form.setFieldsValue({ embeddingModel: embeddingModels[0].value });
      }
    } else {
      // 重置表单和选择的RAG方法
      setSelectedRagMethod(null);
      setRagParams({});
    }
  }, [createModalVisible, form, embeddingModels]);

  // 处理搜索
  const handleSearch = (value) => {
    setSearchText(value);
  };

  // 过滤知识库
  const filteredKnowledgeBases = searchText
    ? knowledgeBases.filter((kb) =>
        kb.name.toLowerCase().includes(searchText.toLowerCase())
      )
    : knowledgeBases;

  // 创建知识库
  const handleCreateKnowledgeBase = async (values) => {
    try {
      // 确保indexParam不为空
      let indexParam = { ...ragParams };

      // 如果ragParams为空，尝试从配置中获取默认参数
      if (Object.keys(indexParam).length === 0 && values.ragMethod) {
        try {
          // 动态导入配置
          const { getMethodConfig } = await import("../../config/ragConfig");
          const methodConfig = getMethodConfig(values.ragMethod);
          if (methodConfig && methodConfig.indexParams) {
            indexParam = { ...methodConfig.indexParams };
            console.log("使用默认RAG参数:", indexParam);
          }
        } catch (error) {
          console.error("获取默认RAG参数失败:", error);
        }
      }

      console.log("RAG参数:", indexParam);
      console.log("表单值:", values);

      // 将RAG参数合并到提交的数据中
      const submitData = {
        name: values.name,
        description: values.description, // 添加描述字段
        ragMethod: values.ragMethod,
        embeddingModel: values.embeddingModel,
        indexParam: JSON.stringify(indexParam), // 将索引参数序列化为JSON字符串
      };

      console.log("提交数据:", submitData);

      const response = await api.post("/api/kb/create", submitData);

      if (response.data) {
        message.success("知识库创建成功");
        setCreateModalVisible(false);
        form.resetFields();
        fetchKnowledgeBases();
      } else {
        message.error("创建知识库失败，请稍后再试");
      }
    } catch (error) {
      console.error("创建知识库失败:", error);
      message.error("创建知识库失败，请稍后再试");
    }
  };

  // 处理RAG方法变更
  const handleRagMethodChange = (value) => {
    setSelectedRagMethod(value);

    // 当用户切换RAG方法时，重置参数，然后由RagMethodParams组件加载新的参数
    setRagParams({});

    // 主动加载选中方法的默认参数
    import("../../config/ragConfig").then(({ getMethodConfig }) => {
      try {
        const methodConfig = getMethodConfig(value);
        if (methodConfig && methodConfig.indexParams) {
          // 只取索引参数，不包括搜索参数
          const defaultParams = { ...methodConfig.indexParams };
          console.log("切换RAG方法，参数初始化:", defaultParams);
          setRagParams(defaultParams);
        }
      } catch (error) {
        console.error("切换RAG方法，初始化参数失败:", error);
      }
    });
  };

  // 处理RAG参数变更
  const handleRagParamsChange = (params) => {
    setRagParams(params);
  };

  // 删除知识库
  const handleDeleteKnowledgeBase = async (id) => {
    try {
      console.log("正在删除知识库ID:", id);
      // 修正API调用路径 - 更改为正确的后端API路径
      const response = await api.delete(`/api/kb/${id}`);

      // 检查响应格式并处理
      console.log("删除知识库响应:", response);

      if (response && response.data) {
        message.success("知识库删除成功");
        fetchKnowledgeBases();
      } else {
        message.error("删除知识库失败，请稍后再试");
      }
    } catch (error) {
      console.error("删除知识库失败:", error);
      message.error("删除知识库失败，请稍后再试");
    }
  };

  // 确认删除
  const showDeleteConfirm = (id, name) => {
    // 使用App.useApp()获取app实例，以兼容antd v5的Modal.confirm
    app.modal.confirm({
      title: "确认删除知识库",
      icon: <ExclamationCircleOutlined />,
      content: (
        <div>
          <p>
            您确定要删除知识库 <Text strong>{name}</Text> 吗？
          </p>
          <p>
            <Text type="danger">
              此操作不可恢复，知识库中的文档关联也将被删除。
            </Text>
          </p>
        </div>
      ),
      okText: "确认删除",
      okType: "danger",
      cancelText: "取消",
      onOk() {
        handleDeleteKnowledgeBase(id);
      },
    });
  };

  // 前往知识库详情页
  const goToKnowledgeBaseDetail = (id) => {
    navigate(`/knowledge_base/rag-x/${id}`);
  };

  // 前往文档管理页
  const goToDocumentManagement = (id, name) => {
    navigate(`/knowledge_base/docs/${id}`);
  };

  // 构建索引
  const handleBuildIndex = async (id, name) => {
    try {
      message.loading({
        content: `正在为知识库 ${name} 构建索引...`,
        key: "buildIndex",
        duration: 0,
      });

      const response = await api.post(`/api/kb/build-index/${id}`);

      if (response && response.data) {
        message.success({
          content: "索引构建成功",
          key: "buildIndex",
        });
        fetchKnowledgeBases();
      } else {
        message.error({
          content: "索引构建失败，请稍后再试",
          key: "buildIndex",
        });
      }
    } catch (error) {
      console.error("索引构建失败:", error);
      message.error({
        content: "索引构建失败，请稍后再试",
        key: "buildIndex",
      });
    }
  };

  // 获取RAG方法显示名称
  const getRagMethodDisplayName = (method, indexParam) => {
    let displayName = "默认方法";
    try {
      // 从 indexParam 判断 RAG 方法
      if (!method && indexParam) {
        const params =
          typeof indexParam === "string" ? JSON.parse(indexParam) : indexParam;

        if (
          params.hasOwnProperty("abstract") &&
          params["chunk-size"] === 2048
        ) {
          method = "hisem";
        } else if (
          params["chunk-size"] === 512 &&
          params["chunk-overlap"] === 100
        ) {
          method = "naive";
        } else if (params.hasOwnProperty("title-enhance")) {
          method = "hisemTree";
        }
      }

      // 根据 method 显示对应的名称
      switch (method) {
        case "naive":
          displayName = "RAG";
          break;
        case "hisem":
          displayName = "HiSem-RAG-Fast";
          break;
        case "hisemTree":
          displayName = "HiSem-RAG-Tree";
          break;
        default:
          // 如果有传入的值就显示，否则显示默认
          displayName = method || "默认方法";
      }
    } catch (e) {
      console.error("解析 RAG 方法失败:", e);
    }

    return displayName;
  };

  // 表格列定义
  const columns = [
    {
      title: "知识库名称",
      dataIndex: "name",
      key: "name",
      render: (text) => <Text strong>{text}</Text>,
      sorter: (a, b) => a.name.localeCompare(b.name),
    },
    {
      title: "RAG方法",
      dataIndex: "ragMethod",
      key: "ragMethod",
      render: (text, record) => {
        // 尝试从 indexParam 判断 RAG 方法
        let ragMethod = text;
        try {
          const indexParam = record.indexParam
            ? JSON.parse(record.indexParam)
            : {};

          // 根据 indexParam 特征判断 RAG 方法
          if (
            indexParam.hasOwnProperty("abstract") &&
            indexParam["chunk-size"] === 2048
          ) {
            ragMethod = "hisem";
          } else if (
            indexParam["chunk-size"] === 512 &&
            indexParam["chunk-overlap"] === 100
          ) {
            ragMethod = "naive";
          } else if (indexParam.hasOwnProperty("title-enhance")) {
            ragMethod = "hisemTree";
          }
        } catch (e) {
          console.error("解析 indexParam 失败:", e);
        }

        // 根据 ragMethod 显示对应的名称
        let displayName = "默认方法";
        switch (ragMethod) {
          case "naive":
            displayName = "RAG";
            break;
          case "hisem":
            displayName = "HiSem-RAG-Fast";
            break;
          case "hisemTree":
            displayName = "HiSem-RAG-Tree";
            break;
          default:
            // 如果有传入的值就显示，否则显示默认
            displayName = text || "默认方法";
        }

        return <Tag color="blue">{displayName}</Tag>;
      },
    },
    {
      title: "嵌入模型",
      dataIndex: "embeddingModel",
      key: "embeddingModel",
      render: (text) => {
        // 如果 embeddingModel 为 null，默认使用配置中第一个模型或系统默认
        const defaultModel =
          embeddingModels.length > 0
            ? embeddingModels[0].label
            : "text-embedding-ada-002";

        return <Tag color="green">{text || defaultModel}</Tag>;
      },
    },
    {
      title: "描述",
      dataIndex: "description",
      key: "description",
      ellipsis: true,
      render: (text) => text || "-",
    },
    {
      title: "创建时间",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (text) => new Date(text).toLocaleString(),
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
    },
    {
      title: "操作",
      key: "action",
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="文档管理">
            <Button
              type="primary"
              icon={<UploadOutlined />}
              size="small"
              onClick={() => goToDocumentManagement(record.id, record.name)}
            >
              文档
            </Button>
          </Tooltip>
          <Tooltip title="知识库查询">
            <Button
              icon={<EyeOutlined />}
              size="small"
              onClick={() => goToKnowledgeBaseDetail(record.id)}
            >
              查询
            </Button>
          </Tooltip>
          <Tooltip title="删除知识库">
            <Button
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => showDeleteConfirm(record.id, record.name)}
            >
              删除
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ];

  // 渲染知识库卡片
  const renderKnowledgeBaseCards = () => {
    if (loading) {
      return (
        <div style={{ textAlign: "center", padding: "50px 0" }}>
          <Spin size="large">
            <div style={{ padding: "50px", textAlign: "center" }}>
              <p>加载知识库中...</p>
            </div>
          </Spin>
        </div>
      );
    }

    if (filteredKnowledgeBases.length === 0) {
      return (
        <Empty
          description={
            searchText ? "没有找到匹配的知识库" : "您还没有创建任何知识库"
          }
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        >
          {!searchText && (
            <Button type="primary" onClick={() => setCreateModalVisible(true)}>
              创建知识库
            </Button>
          )}
        </Empty>
      );
    }

    return (
      <Row gutter={[24, 24]}>
        {filteredKnowledgeBases.map((kb) => {
          return (
            <Col xs={24} sm={12} lg={8} xl={6} key={kb.id}>
              <Badge.Ribbon
                text={getRagMethodDisplayName(kb.ragMethod, kb.indexParam)}
                color="blue"
              >
                <Card
                  hoverable
                  style={{
                    borderRadius: "12px",
                    overflow: "hidden",
                    height: "100%",
                    boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
                  }}
                  actions={[
                    <Tooltip title="文档管理">
                      <UploadOutlined
                        onClick={(e) => {
                          e.stopPropagation();
                          goToDocumentManagement(kb.id, kb.name);
                        }}
                      />
                    </Tooltip>,
                    <Tooltip title="知识库问答">
                      <SearchOutlined
                        onClick={(e) => {
                          e.stopPropagation();
                          goToKnowledgeBaseDetail(kb.id);
                        }}
                      />
                    </Tooltip>,
                    <Tooltip title="删除知识库">
                      <DeleteOutlined
                        onClick={(e) => {
                          e.stopPropagation();
                          showDeleteConfirm(kb.id, kb.name);
                        }}
                      />
                    </Tooltip>,
                  ]}
                  onClick={() => goToDocumentManagement(kb.id, kb.name)}
                >
                  <div style={{ padding: "16px 8px" }}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        marginBottom: "16px",
                      }}
                    >
                      <Avatar
                        icon={<BookOutlined />}
                        style={{ backgroundColor: "#1890ff", marginRight: 12 }}
                        size="large"
                      />
                      <div>
                        <Typography.Title level={5} style={{ margin: 0 }}>
                          {kb.name}
                        </Typography.Title>
                        <Typography.Text type="secondary">
                          创建于 {new Date(kb.createdAt).toLocaleDateString()}
                        </Typography.Text>
                      </div>
                    </div>

                    {/* 添加知识库描述显示 */}
                    {kb.description && (
                      <div style={{ marginBottom: "12px" }}>
                        <Typography.Paragraph
                          type="secondary"
                          ellipsis={{
                            rows: 2,
                            expandable: false,
                            tooltip: kb.description,
                          }}
                          style={{ fontSize: "13px", margin: 0 }}
                        >
                          {kb.description}
                        </Typography.Paragraph>
                      </div>
                    )}

                    <div style={{ marginBottom: "8px" }}>
                      <Tag color="green">
                        {kb.embeddingModel || "默认嵌入模型"}
                      </Tag>
                    </div>

                  </div>
                </Card>
              </Badge.Ribbon>
            </Col>
          );
        })}
      </Row>
    );
  };

  return (
    <div>
      <div
        style={{
          background: "linear-gradient(135deg, #1890ff 0%, #096dd9 100%)",
          borderRadius: "12px",
          padding: "24px",
          color: "white",
          boxShadow: "0 4px 12px rgba(24, 144, 255, 0.15)",
        }}
      >
        <Title level={2} style={{ color: "white", margin: 0 }}>
          知识库管理
        </Title>
        <Paragraph
          style={{
            color: "rgba(255, 255, 255, 0.85)",
            fontSize: "16px",
            marginBottom: 0,
          }}
        >
          创建和管理您的知识库，进行智能问答和文档检索
        </Paragraph>
      </div>

      <Row gutter={[24, 24]} style={{ marginTop: "24px" }}>
        <Col span={24}>
          <Card
            title={
              <div style={{ display: "flex", alignItems: "center" }}>
                <Avatar
                  icon={<BookOutlined />}
                  style={{ backgroundColor: "#1890ff", marginRight: 8 }}
                />
                <span>我的知识库</span>
              </div>
            }
            extra={
              <Space>
                <Radio.Group
                  value={viewMode}
                  onChange={(e) => setViewMode(e.target.value)}
                  buttonStyle="solid"
                  size="small"
                >
                  <Radio.Button value="card">卡片视图</Radio.Button>
                  <Radio.Button value="table">表格视图</Radio.Button>
                </Radio.Group>
                <Search
                  placeholder="搜索知识库"
                  allowClear
                  onSearch={handleSearch}
                  style={{ width: 200 }}
                />
                <Button
                  type="primary"
                  icon={<PlusOutlined />}
                  onClick={() => setCreateModalVisible(true)}
                >
                  新建知识库
                </Button>
              </Space>
            }
            style={{
              borderRadius: "12px",
              boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
            }}
          >
            {viewMode === "card" ? (
              renderKnowledgeBaseCards()
            ) : loading ? (
              <div style={{ textAlign: "center", padding: "50px 0" }}>
                <Spin size="large">
                  <div style={{ padding: "50px", textAlign: "center" }}>
                    <p>加载知识库中...</p>
                  </div>
                </Spin>
              </div>
            ) : filteredKnowledgeBases.length === 0 ? (
              <Empty
                description={
                  searchText ? "没有找到匹配的知识库" : "您还没有创建任何知识库"
                }
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                {!searchText && (
                  <Button
                    type="primary"
                    onClick={() => setCreateModalVisible(true)}
                  >
                    创建知识库
                  </Button>
                )}
              </Empty>
            ) : (
              <Table
                dataSource={filteredKnowledgeBases}
                columns={columns}
                rowKey="id"
                pagination={{ pageSize: 10 }}
                style={{ borderRadius: "8px", overflow: "hidden" }}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* 创建知识库模态框 */}
      <Modal
        title={
          <div style={{ display: "flex", alignItems: "center" }}>
            <BookOutlined style={{ color: "#1890ff", marginRight: 8 }} />
            <span>创建新的知识库</span>
          </div>
        }
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        footer={null}
        width={700}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateKnowledgeBase}
          initialValues={{
            ragMethod: ragMethods.length > 0 ? ragMethods[0].id : null,
            embeddingModel:
              embeddingModels.length > 0
                ? embeddingModels[0].value
                : "text-embedding-ada-002",
          }}
        >
          <Form.Item
            name="name"
            label="知识库名称"
            rules={[{ required: true, message: "请输入知识库名称" }]}
          >
            <Input placeholder="请输入知识库名称" />
          </Form.Item>

          {/* 添加知识库描述字段 */}
          <Form.Item
            name="description"
            label="知识库描述"
            rules={[{ required: false }]}
          >
            <Input.TextArea
              placeholder="请输入知识库描述（可选）"
              autoSize={{ minRows: 2, maxRows: 4 }}
              maxLength={200}
              showCount
            />
          </Form.Item>

          <Form.Item
            name="ragMethod"
            label={
              <span>
                RAG方法
                <Tooltip title="选择适合您的知识库的RAG方法，不同的方法适用于不同类型的问答场景">
                  <QuestionCircleOutlined style={{ marginLeft: 8 }} />
                </Tooltip>
              </span>
            }
            rules={[{ required: true, message: "请选择RAG方法" }]}
          >
            <Select
              placeholder="请选择RAG方法"
              onChange={handleRagMethodChange}
            >
              {ragMethods.map((method) => (
                <Option key={method.id} value={method.id}>
                  {method.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          {/* 动态RAG参数配置 */}
          {selectedRagMethod && (
            <>
              <Divider orientation="left">RAG方法参数配置</Divider>
              <div style={{ marginBottom: 16 }}>
                {/* 创建知识库时只显示索引参数 */}
                <RagMethodParams
                  methodId={selectedRagMethod}
                  onChange={handleRagParamsChange}
                  showSearchParams={false}
                  key={selectedRagMethod} // 添加key确保组件在方法变更时重新渲染
                />
              </div>
            </>
          )}

          <Form.Item
            name="embeddingModel"
            label={
              <span>
                嵌入模型
                <Tooltip title="选择用于文档向量化的嵌入模型，不同的模型有不同的语义理解能力">
                  <QuestionCircleOutlined style={{ marginLeft: 8 }} />
                </Tooltip>
              </span>
            }
            rules={[{ required: true, message: "请选择嵌入模型" }]}
          >
            <Select
              placeholder="请选择嵌入模型"
              loading={loadingModels}
              optionLabelProp="label"
            >
              {embeddingModels.map((model) => (
                <Option
                  key={model.value}
                  value={model.value}
                  label={model.label}
                >
                  <div>
                    <div>{model.label}</div>
                    {model.description && (
                      <div style={{ fontSize: "12px", color: "#00000073" }}>
                        {model.description}
                      </div>
                    )}
                  </div>
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Divider />

          <Form.Item style={{ marginBottom: 0, textAlign: "right" }}>
            <Space>
              <Button
                onClick={() => {
                  setCreateModalVisible(false);
                  form.resetFields();
                }}
              >
                取消
              </Button>
              <Button type="primary" htmlType="submit">
                创建
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

const KnowledgeBaseManagementWithApp = () => {
  return (
    <App>
      <KnowledgeBaseManagement />
    </App>
  );
};

export default KnowledgeBaseManagementWithApp;