import { useCallback, useEffect, useRef, useState, useMemo } from "react";
import {
  Avatar,
  Button,
  Card,
  Col,
  InputNumber,
  message,
  Row,
  Select,
  Slider,
  Space,
  Spin,
  Switch,
  Tag,
  theme,
  Typography,
} from "antd";
import {
  BookOutlined,
  CopyOutlined,
  DatabaseOutlined,
  DislikeOutlined,
  LikeOutlined,
  QuestionCircleOutlined,
  ReloadOutlined,
  RobotOutlined,
  SettingOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { Bubble, ThoughtChain, Sender, XProvider } from "@ant-design/x";
import { useXChat } from "@ant-design/x-sdk";
import { XMarkdown } from "@ant-design/x-markdown";
import { useNavigate, useParams } from "react-router-dom";
import { kbAPI } from "../../services/api";
import { getMethodConfig } from "../../config/ragConfig";
import "../../styles/components/ragChat.css";
import { useAppStore } from "../../store/useAppStore";
import { SmartDocChatProvider } from "../../utils/SmartRagChatProvider";

const { Title, Paragraph } = Typography;
const { Option } = Select;

// 消息内容渲染组件
const CustomBubbleMessageRender = ({ content, sources = [] }: { content: string, sources?: any[] }) => {
  // 确保 content 是字符串
  const textContent = typeof content === 'string' ? content : '';

  if (!textContent && (!sources || sources.length === 0)) {
    return (
      <Typography>
        <div style={{ color: "#999", fontStyle: "italic" }}>（空消息）</div>
      </Typography>
    );
  }

  return (
    <>
      <XMarkdown>{textContent}</XMarkdown>
      {sources && sources.length > 0 && (
        <div
          style={{
            marginTop: "12px",
            paddingTop: "12px",
            borderTop: "1px solid #e8f4f8",
            backgroundColor: "#f8fcff",
            padding: "12px",
            borderRadius: "6px",
            border: "1px solid #e1f0ff",
          }}
        >
          <div
            style={{
              fontSize: "13px",
              color: "#1890ff",
              marginBottom: "8px",
              fontWeight: "500",
              display: "flex",
              alignItems: "center",
              gap: "4px",
            }}
          >
            <BookOutlined style={{ fontSize: "12px" }} />
            参考文档（{sources.length}个）:
          </div>

          <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
            {sources.slice(0, 3).map((source, index) => (
              <Tag
                key={index}
                color="blue"
                style={{
                  fontSize: "12px",
                  margin: 0,
                  borderRadius: "4px",
                  cursor: "pointer",
                }}
                title="点击查看详情"
              >
                {source.title ||
                  source.fileName ||
                  source.name ||
                  `文档${index + 1}`}
              </Tag>
            ))}
          </div>
        </div>
      )}
    </>
  );
};

// RAG参数配置组件
const RagParamsConfig = ({ ragMethodDetails, ragParams, onParamsChange }: any) => {
  if (!ragMethodDetails) {
    return (
      <div style={{ textAlign: "center", padding: "20px", color: "#999" }}>
        <QuestionCircleOutlined
          style={{ fontSize: "24px", marginBottom: "8px" }}
        />
        <div style={{ fontSize: "14px" }}>请先选择知识库以配置RAG参数</div>
      </div>
    );
  }

  const handleParamChange = (paramKey: string, value: any) => {
    onParamsChange({ [paramKey]: value });
  };

  const renderParamControl = (paramKey: string, paramConfig: any) => {
    const currentValue = ragParams[paramKey] ?? paramConfig.default;

    if (paramConfig.type === "integer" || paramConfig.type === "number") {
      return (
        <div key={paramKey} style={{ marginBottom: "16px" }}>
          <div
            style={{
              marginBottom: "8px",
              fontSize: "13px",
              fontWeight: "bold",
            }}
          >
            {paramConfig.label || paramKey}
          </div>
          <Slider
            min={paramConfig.min || 1}
            max={paramConfig.max || 100}
            value={currentValue}
            onChange={(value) => handleParamChange(paramKey, value)}
            marks={{
              [paramConfig.min || 1]: paramConfig.min || 1,
              [paramConfig.max || 100]: paramConfig.max || 100,
            }}
            tooltip={{ formatter: (value) => `${value}` }}
          />
          <div
            style={{
              textAlign: "center",
              fontSize: "12px",
              color: "#666",
              marginTop: "4px",
            }}
          >
            当前值: {currentValue}
          </div>
        </div>
      );
    } else if (paramConfig.type === "boolean") {
      return (
        <div key={paramKey} style={{ marginBottom: "16px" }}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <span style={{ fontSize: "13px", fontWeight: "bold" }}>
              {paramConfig.label || paramKey}
            </span>
            <Switch
              checked={currentValue}
              onChange={(checked) => handleParamChange(paramKey, checked)}
              size="small"
            />
          </div>
          {paramConfig.description && (
            <div style={{ fontSize: "11px", color: "#666", marginTop: "4px" }}>
              {paramConfig.description}
            </div>
          )}
        </div>
      );
    } else if (paramConfig.type === "select") {
      return (
        <div key={paramKey} style={{ marginBottom: "16px" }}>
          <div
            style={{
              marginBottom: "8px",
              fontSize: "13px",
              fontWeight: "bold",
            }}
          >
            {paramConfig.label || paramKey}
          </div>
          <Select
            size="small"
            style={{ width: "100%" }}
            value={currentValue}
            onChange={(value) => handleParamChange(paramKey, value)}
          >
            {paramConfig.options?.map((option: any) => (
              <Select.Option key={option.value} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>
        </div>
      );
    } else {
      return (
        <div key={paramKey} style={{ marginBottom: "16px" }}>
          <div
            style={{
              marginBottom: "8px",
              fontSize: "13px",
              fontWeight: "bold",
            }}
          >
            {paramConfig.label || paramKey}
          </div>
          <InputNumber
            size="small"
            style={{ width: "100%" }}
            value={currentValue}
            onChange={(value) => handleParamChange(paramKey, value)}
          />
        </div>
      );
    }
  };

  return (
    <div>
      <div style={{ marginBottom: "16px", textAlign: "center" }}>
        <Tag color="blue" style={{ fontSize: "12px" }}>
          {ragMethodDetails.name}
        </Tag>
      </div>
      <div
        style={{
          fontSize: "12px",
          color: "#666",
          marginBottom: "16px",
          textAlign: "center",
        }}
      >
        {ragMethodDetails.description}
      </div>

      {ragMethodDetails.searchParams &&
      Object.keys(ragMethodDetails.searchParams).length > 0 ? (
        Object.entries(ragMethodDetails.searchParams).map(([key, config]) =>
          renderParamControl(key, config)
        )
      ) : (
        <div style={{ textAlign: "center", color: "#999", fontSize: "12px" }}>
          暂无可配置参数
        </div>
      )}
    </div>
  );
};

// 主组件
const RAGChatX = () => {
  const { id: knowledgeBaseId } = useParams();
  const navigate = useNavigate();
  const [messageApi, contextHolder] = message.useMessage();
  const { setCurrentKbId } = useAppStore();
  const { token } = theme.useToken();

  // 核心状态管理
  const [knowledgeBase, setKnowledgeBase] = useState<any>(null);
  const [knowledgeBases, setKnowledgeBases] = useState<any[]>([]);
  const [listLoading, setListLoading] = useState(true);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [kbSelectLoading, setKbSelectLoading] = useState(false);
  const [input, setInput] = useState("");

  // RAG 方法配置相关状态
  const [loadingMethodDetails, setLoadingMethodDetails] = useState(false);
  const [ragMethodDetails, setRagMethodDetails] = useState<any>(null);
  const [ragParams, setRagParams] = useState({});
  const [ragMethodType, setRagMethodType] = useState(null);

  // 自动滚动 ref
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 使用 useXChat
  const provider = useMemo(() => new SmartDocChatProvider(), []);
  const { messages, onRequest, isRequesting, abort } = useXChat({
    provider,
  });

  // 消息发送处理函数
  const handleSendMessage = () => {
    if (!input.trim()) return;

    if (!knowledgeBase) {
      messageApi.warning("请先选择一个知识库");
      return;
    }

    onRequest({
      messages: [{ role: 'user', content: input.trim(), kbId: knowledgeBase.id, ragMethod: ragMethodType || knowledgeBase.ragMethod, ragParams } as any],
    });
    setInput("");
  };

  // RAG参数变更处理
  const handleRagParamsChange = (newParams: any) => {
    setRagParams((prevParams) => ({ ...prevParams, ...newParams }));
  };

  // 自动滚动到最新消息
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages]);

  // 获取知识库详情
  const fetchKnowledgeBaseDetails = useCallback(
    async (id: string) => {
      if (!id) {
        setKnowledgeBase(null);
        setCurrentKbId(null);
        setRagParams({});
        setRagMethodDetails(null);
        setRagMethodType(null);
        return;
      }
      setDetailsLoading(true);
      try {
        const response = await kbAPI.get(id) as any;
        
        if (
          (response.data && response.data.success) ||
          (response.data && response.data.code === 200) ||
          (response.data && !response.data.code) // 直接返回对象的情况
        ) {
          const kbData = response.data.data || response.data;
          setKnowledgeBase(kbData);
          setCurrentKbId(kbData.id);

          if (kbData.ragMethod) {
            setLoadingMethodDetails(true);
            try {
              const methodConfig = getMethodConfig(kbData.ragMethod);

              if (methodConfig) {
                setRagMethodDetails(methodConfig);
                setRagMethodType(kbData.ragMethod);

                const initialSearchParams: any = {};
                if (methodConfig.searchParams) {
                  Object.keys(methodConfig.searchParams).forEach((paramKey) => {
                    const paramConfig = methodConfig.searchParams[paramKey];
                    initialSearchParams[paramKey] =
                      paramConfig.default !== undefined
                        ? paramConfig.default
                        : paramConfig;
                  });
                }
                setRagParams(initialSearchParams);
              } else {
                messageApi.warning("未找到RAG方法配置信息，将使用默认参数");
                setRagMethodDetails(null);
                setRagParams({});
              }
            } catch (error) {
              console.error("获取RAG方法详情失败:", error);
              setRagMethodDetails(null);
              setRagParams({});
            } finally {
              setLoadingMethodDetails(false);
            }
          } else {
            setRagMethodDetails(null);
            setRagParams({});
          }
        } else {
          messageApi.error(response.data?.message || "获取知识库详情失败");
          setKnowledgeBase(null);
          setCurrentKbId(null);
        }
      } catch (error) {
        console.error("获取知识库详情失败:", error);
        messageApi.error("获取知识库详情失败，请稍后再试");
        setKnowledgeBase(null);
        setCurrentKbId(null);
      } finally {
        setDetailsLoading(false);
      }
    },
    [messageApi, setCurrentKbId]
  );

  // 知识库选择变更处理
  const handleKnowledgeBaseChange = (id: string) => {
    if (id) {
      navigate(`/knowledge_base/rag-x/${id}`);
    } else {
      navigate("/knowledge_base/rag-x");
      setKnowledgeBase(null);
      setCurrentKbId(null);
      setRagParams({});
      setRagMethodDetails(null);
      setRagMethodType(null);
    }
  };

  // 获取知识库列表
  const fetchKnowledgeBases = useCallback(async () => {
    setListLoading(true);
    setKbSelectLoading(true);
    try {
      const response = await kbAPI.list({}) as any;
      if (response.data && Array.isArray(response.data)) {
        const kbs = response.data;
        kbs.sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setKnowledgeBases(kbs);
      } else if (response.data && response.data.code === 200) {
        const kbs = response.data.data || [];
        kbs.sort((a: any, b: any) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        setKnowledgeBases(kbs);
      } else {
        messageApi.error(response.data?.message || "获取知识库列表失败");
        setKnowledgeBases([]);
      }
    } catch (error) {
      console.error("获取知识库列表失败:", error);
      messageApi.error("获取知识库列表失败，请稍后再试");
      setKnowledgeBases([]);
    } finally {
      setListLoading(false);
      setKbSelectLoading(false);
    }
  }, [messageApi]);

  useEffect(() => {
    fetchKnowledgeBases();
  }, [fetchKnowledgeBases]);

  useEffect(() => {
    fetchKnowledgeBaseDetails(knowledgeBaseId as string);
  }, [knowledgeBaseId, fetchKnowledgeBaseDetails]);

  if (listLoading) {
    return (
      <div style={{ textAlign: "center", padding: "100px 0" }}>
        <Spin size="large" tip="加载知识库列表中..." />
      </div>
    );
  }

  return (
    <XProvider>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          height: "calc(100vh - 112px)",
        }}
      >
        {contextHolder}

        <div
          style={{
            background: "#52c41a",
            borderRadius: "6px",
            padding: "16px 24px",
            color: "white",
            marginBottom: "24px",
          }}
        >
          <Title level={4} style={{ color: "white", margin: 0 }}>
            知识库问答 (流式渲染)
          </Title>
          <Paragraph
            style={{
              color: "rgba(255, 255, 255, 0.85)",
              fontSize: "14px",
              marginBottom: 0,
            }}
          >
            基于RAG技术的智能问答系统，使用Ant Design X原生流式响应
          </Paragraph>
        </div>

        <Row gutter={24} style={{ flex: 1, overflow: "hidden" }}>
          <Col
            span={18}
            style={{ display: "flex", flexDirection: "column", height: "100%" }}
          >
            <Card
              title={
                <div style={{ display: "flex", alignItems: "center" }}>
                  <Avatar
                    icon={<BookOutlined />}
                    style={{ backgroundColor: "#52c41a", marginRight: 8 }}
                  />
                  <span>智能问答对话</span>
                </div>
              }
              style={{
                borderRadius: "8px",
                boxShadow: "0 2px 8px rgba(0, 0, 0, 0.05)",
                flex: 1,
                display: "flex",
                flexDirection: "column",
                overflow: "auto",
              }}
              styles={{
                body: {
                  flex: 1,
                  overflow: "hidden",
                  display: "flex",
                  flexDirection: "column",
                  padding: "16px",
                },
              }}
            >
              <div
                style={{
                  flex: 1,
                  overflowY: "auto",
                  overflowX: "hidden",
                  padding: "16px 0",
                  marginBottom: "16px",
                  display: "flex",
                  flexDirection: "column",
                }}
              >
                {messages.length === 0 ? (
                  <div
                    style={{
                      textAlign: "center",
                      padding: "100px 20px",
                      color: "#999",
                    }}
                  >
                    <RobotOutlined
                      style={{ fontSize: "48px", marginBottom: "16px" }}
                    />
                    <div>开始与 AI 助手对话吧！</div>
                  </div>
                ) : (
                  <Bubble.List
                    items={messages.map((msg) => {
                      const isUser = msg.message.role === 'user';
                      // @ts-ignore
                      const thoughts = msg.message.thoughts;
                      
                      return {
                        key: msg.id,
                        role: isUser ? 'user' : 'assistant',
                        content: msg.message.content,
                        loading: msg.status === 'loading',
                        placement: isUser ? 'end' : 'start',
                        avatar: isUser 
                          ? <Avatar icon={<UserOutlined />} style={{ backgroundColor: "#1890ff" }} />
                          : <Avatar icon={<RobotOutlined />} style={{ backgroundColor: "#52c41a" }} />,
                        messageRender: isUser 
                          ? undefined 
                          : (content: string) => <CustomBubbleMessageRender content={content} />,
                        header: (!isUser && thoughts && thoughts.length > 0) ? (
                              <div style={{ marginBottom: 8, marginLeft: 40 }}>
                                <ThoughtChain
                                  items={thoughts.map((t: any) => ({
                                    title: t.title,
                                    status: t.status,
                                    icon: t.icon,
                                    description: t.duration ? `${t.duration}ms` : undefined,
                                    content: t.content
                                  }))}
                                />
                              </div>
                            ) : undefined,
                        footer: !isUser ? (
                          <Space size={token.paddingXXS}>
                            <Button
                              color="default"
                              variant="text"
                              size="small"
                              icon={<ReloadOutlined />}
                            />
                            <Button
                              color="default"
                              variant="text"
                              size="small"
                              icon={<CopyOutlined />}
                            />
                            <Button
                              type="text"
                              size="small"
                              icon={<LikeOutlined />}
                            />
                            <Button
                              type="text"
                              size="small"
                              icon={<DislikeOutlined />}
                            />
                          </Space>
                        ) : undefined
                      };
                    })}
                  />
                )}
                <div ref={messagesEndRef} />
              </div>

              <div
                style={{
                  borderTop: "1px solid #f0f0f0",
                  paddingTop: "16px",
                  display: "flex",
                  gap: "12px",
                  alignItems: "flex-end",
                }}
              >
                <Sender
                  value={input}
                  onChange={(v) => setInput(v)}
                  onSubmit={() => {
                    handleSendMessage();
                  }}
                  loading={isRequesting}
                  onCancel={abort}
                  placeholder="输入您的问题..."
                  style={{ flex: 1 }}
                />
              </div>
            </Card>
          </Col>

          <Col span={6}>
            <Card
              title={
                <div style={{ display: "flex", alignItems: "center" }}>
                  <DatabaseOutlined
                    style={{ marginRight: 8, color: "#1890ff" }}
                  />
                  <span>知识库</span>
                </div>
              }
              size="small"
              style={{ marginBottom: 16 }}
            >
              <Select
                showSearch
                style={{ width: "100%" }}
                placeholder="选择知识库"
                loading={kbSelectLoading}
                value={knowledgeBase?.id}
                onChange={handleKnowledgeBaseChange}
                optionFilterProp="children"
                filterOption={(input, option) =>
                  (option?.children as unknown as string).toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {knowledgeBases.map((kb) => (
                  <Option key={kb.id} value={kb.id}>
                    {kb.name}
                  </Option>
                ))}
              </Select>

              {detailsLoading ? (
                <div style={{ textAlign: "center", padding: "12px" }}>
                  <Spin size="small" />
                </div>
              ) : knowledgeBase ? (
                <div style={{ marginTop: "12px", textAlign: "center" }}>
                  <Tag color="green" style={{ fontSize: "12px" }}>
                    已选择: {knowledgeBase.name}
                  </Tag>
                </div>
              ) : (
                <div
                  style={{
                    textAlign: "center",
                    padding: "12px",
                    color: "#999",
                    fontSize: "12px",
                  }}
                >
                  请选择知识库
                </div>
              )}
            </Card>

            <Card
              title={
                <div style={{ display: "flex", alignItems: "center" }}>
                  <SettingOutlined style={{ marginRight: 8, color: "#52c41a" }} />
                  <span>RAG配置</span>
                </div>
              }
              size="small"
            >
              {loadingMethodDetails ? (
                <div style={{ textAlign: "center", padding: "20px" }}>
                  <Spin size="small" />
                  <div
                    style={{ marginTop: "8px", color: "#666", fontSize: "12px" }}
                  >
                    加载参数...
                  </div>
                </div>
              ) : (
                <RagParamsConfig
                  ragMethodDetails={ragMethodDetails}
                  ragParams={ragParams}
                  onParamsChange={handleRagParamsChange}
                />
              )}
            </Card>
          </Col>
        </Row>
      </div>
    </XProvider>
  );
};

export default RAGChatX;
