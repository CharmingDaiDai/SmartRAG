import React, {useEffect, useState} from "react";
import {
    Avatar,
    Button,
    Card,
    Col,
    Drawer,
    Empty,
    Form,
    Input,
    message,
    Modal,
    Progress,
    Row,
    Space,
    Spin,
    Statistic,
    Table,
    Tag,
    Tooltip,
    Typography,
    Upload,
} from "antd";
import {
    CloudUploadOutlined,
    DeleteOutlined,
    ExclamationCircleOutlined,
    EyeOutlined,
    FileExcelOutlined,
    FileMarkdownOutlined,
    FilePdfOutlined,
    FilePptOutlined,
    FileTextOutlined,
    FileUnknownOutlined,
    FileWordOutlined,
    SearchOutlined,
    UploadOutlined,
} from "@ant-design/icons";
import api from "../../services/api";
import DocumentViewer from "../../components/viewer/DocumentViewer";

const { Title, Paragraph, Text } = Typography;
const { confirm } = Modal;
const { Search } = Input;

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleString("zh-CN");
};

// 获取文件名（不含扩展名）
const getFileNameWithoutExtension = (fileName) => {
  if (!fileName) return "";
  return fileName.replace(/\.[^/.]+$/, "");
};

// 根据文件扩展名获取对应的图标
const getFileIcon = (fileName) => {
  if (!fileName) return <FileUnknownOutlined />;

  const extension = fileName.split(".").pop()?.toLowerCase();

  switch (extension) {
    case "pdf":
      return <FilePdfOutlined style={{ color: "#ff4d4f" }} />;
    case "doc":
    case "docx":
      return <FileWordOutlined style={{ color: "#1890ff" }} />;
    case "xls":
    case "xlsx":
      return <FileExcelOutlined style={{ color: "#52c41a" }} />;
    case "ppt":
    case "pptx":
      return <FilePptOutlined style={{ color: "#faad14" }} />;
    case "md":
    case "markdown":
      return <FileMarkdownOutlined style={{ color: "#722ed1" }} />;
    case "txt":
      return <FileTextOutlined style={{ color: "#1890ff" }} />;
    default:
      return <FileUnknownOutlined style={{ color: "#8c8c8c" }} />;
  }
};

// 获取文件类型的标签颜色
const getFileTypeTagColor = (fileName) => {
  if (!fileName) return "default";

  const extension = fileName.split(".").pop()?.toLowerCase();

  switch (extension) {
    case "pdf":
      return "red";
    case "doc":
    case "docx":
      return "blue";
    case "xls":
    case "xlsx":
      return "green";
    case "ppt":
    case "pptx":
      return "orange";
    case "md":
    case "markdown":
      return "purple";
    case "txt":
      return "cyan";
    default:
      return "default";
  }
};

const DocumentManagement = () => {
  const [documents, setDocuments] = useState([]);
  const [filteredDocuments, setFilteredDocuments] = useState([]);
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadModalVisible, setUploadModalVisible] = useState(false);
  const [detailsDrawerVisible, setDetailsDrawerVisible] = useState(false);
  const [fileList, setFileList] = useState([]);
  const [searchText, setSearchText] = useState("");
  const [form] = Form.useForm();
  const [uploadProgress, setUploadProgress] = useState(0);
  const [totalStorageUsed, setTotalStorageUsed] = useState(0);
  const [storageLimit] = useState(1073741824); // 1GB 假设的存储限制

  // 获取文档列表
  const fetchDocuments = async () => {
    try {
      setLoading(true);
      const response = await api.get("/api/documents");
      setDocuments(response.data);
      setFilteredDocuments(response.data);

      // 计算已用存储空间
      const totalSize = response.data.reduce(
        (sum, doc) => sum + (doc.fileSize || 0),
        0
      );
      setTotalStorageUsed(totalSize);
    } catch (error) {
      console.error("获取文档失败:", error);
      message.error("获取文档列表失败");
    } finally {
      setLoading(false);
    }
  };

  // 初始化加载
  useEffect(() => {
    fetchDocuments();
  }, []);

  // 搜索过滤
  useEffect(() => {
    if (!searchText) {
      setFilteredDocuments(documents);
      return;
    }

    const filtered = documents.filter(
      (doc) =>
        doc.title?.toLowerCase().includes(searchText.toLowerCase()) ||
        doc.fileName?.toLowerCase().includes(searchText.toLowerCase())
    );
    setFilteredDocuments(filtered);
  }, [searchText, documents]);

  // 处理文件上传前的检查
  const beforeUpload = (file) => {
    // 这里可以添加文件类型、大小等检查
    return false; // 阻止自动上传，由我们手动上传
  };

  // 处理文件列表变化
  const handleFileChange = ({ fileList }) => {
    setFileList(fileList);

    // 当文件列表变化时，更新表单中对应的标题默认值
    const defaultTitles = fileList.map((file) =>
      getFileNameWithoutExtension(file.name)
    );
    form.setFieldsValue({ titles: defaultTitles });
  };

  // 模拟上传进度
  const simulateProgress = () => {
    let percent = 0;
    const interval = setInterval(() => {
      percent += Math.floor(Math.random() * 10) + 5;
      if (percent >= 100) {
        clearInterval(interval);
        percent = 100;
      }
      setUploadProgress(percent);
    }, 300);
    return () => clearInterval(interval);
  };

  // 处理文件上传
  const handleUpload = async () => {
    try {
      if (fileList.length === 0) {
        message.warning("请选择要上传的文件");
        return;
      }

      const values = await form.validateFields();

      setUploading(true);
      setUploadProgress(0);

      // 开始模拟进度条
      const stopProgress = simulateProgress();

      if (fileList.length === 1) {
        // 单文件上传
        const formData = new FormData();
        formData.append("file", fileList[0].originFileObj);
        formData.append("title", values.titles[0] || fileList[0].name);

        await api.post("/api/documents/upload", formData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        });
      } else {
        // 批量上传
        const formData = new FormData();
        fileList.forEach((file, index) => {
          formData.append("files", file.originFileObj);
          formData.append("titles", values.titles[index] || file.name);
        });

        await api.post("/api/documents/upload-batch", formData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        });
      }

      // 停止进度条模拟
      stopProgress();
      setUploadProgress(100);

      setTimeout(() => {
        message.success("文档上传成功");
        setUploadModalVisible(false);
        setFileList([]);
        form.resetFields();
        fetchDocuments();
      }, 500);
    } catch (error) {
      console.error("上传文档失败:", error);
      message.error("文档上传失败");
    } finally {
      setUploading(false);
    }
  };

  // 确认删除文档
  const showDeleteConfirm = () => {
    if (selectedRowKeys.length === 0) {
      message.warning("请先选择要删除的文档");
      return;
    }

    confirm({
      title: `确定要删除选中的 ${selectedRowKeys.length} 个文档吗？`,
      icon: <ExclamationCircleOutlined style={{ color: "#ff4d4f" }} />,
      content: "此操作不可逆，删除后将无法恢复",
      okText: "确定删除",
      okType: "danger",
      cancelText: "取消",
      okButtonProps: {
        danger: true,
      },
      onOk: async () => {
        try {
          if (selectedRowKeys.length === 1) {
            await api.delete(`/api/documents/${selectedRowKeys[0]}`);
          } else {
            // 修复批量删除的请求格式
            await api.delete("/api/documents/batch", {
              data: selectedRowKeys, // 确保正确传递数据
            });
          }
          message.success("文档删除成功");
          setSelectedRowKeys([]);
          fetchDocuments();
        } catch (error) {
          console.error("删除文档失败:", error);
          message.error(
            `删除文档失败: ${error.response?.data?.message || error.message}`
          );
        }
      },
    });
  };

  // 查看文档详情
  const handleViewDetails = async (documentId) => {
    try {
      const response = await api.get(`/api/documents/${documentId}`);
      setSelectedDocument(response.data);
      setDetailsDrawerVisible(true);
    } catch (error) {
      console.error("获取文档详情失败:", error);
      message.error("获取文档详情失败");
    }
  };

  // 处理搜索
  const handleSearch = (value) => {
    setSearchText(value);
  };

  // 渲染搜索结果数量提示
  const renderSearchResultCount = () => {
    if (!searchText) return null;
    return (
      <Text
        type="secondary"
        style={{ marginBottom: 16, display: "inline-block" }}
      >
        搜索 "{searchText}" 找到 {filteredDocuments.length} 个结果
      </Text>
    );
  };

  // 表格列配置
  const columns = [
    {
      title: "文档",
      key: "document",
      render: (_, record) => (
        <div style={{ display: "flex", alignItems: "center" }}>
          <Avatar
            icon={getFileIcon(record.fileName)}
            size={40}
            shape="square"
            style={{
              backgroundColor: "rgba(0,0,0,0.04)",
              marginRight: 12,
              padding: 5,
            }}
          />
          <div>
            <div style={{ fontWeight: 500 }}>{record.title}</div>
            <Text type="secondary" style={{ fontSize: "12px" }}>
              {record.fileName}
            </Text>
          </div>
        </div>
      ),
      sorter: (a, b) => a.title.localeCompare(b.title),
    },
    {
      title: "类型",
      dataIndex: "fileName",
      key: "fileType",
      render: (fileName) => {
        const extension = fileName.split(".").pop()?.toUpperCase();
        return (
          <Tag
            color={getFileTypeTagColor(fileName)}
            style={{ fontWeight: 500 }}
          >
            {extension || "未知"}
          </Tag>
        );
      },
      responsive: ["md"],
      width: 100,
    },
    {
      title: "大小",
      dataIndex: "fileSize",
      key: "fileSize",
      render: (fileSize) => formatFileSize(fileSize),
      sorter: (a, b) => a.fileSize - b.fileSize,
      responsive: ["md"],
      width: 120,
    },
    {
      title: "上传时间",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (createdAt) => formatDate(createdAt),
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt),
      responsive: ["lg"],
      width: 180,
    },
    {
      title: "操作",
      key: "action",
      width: 160,
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看文档">
            <Button
              type="primary"
              icon={<EyeOutlined />}
              size="small"
              onClick={() => handleViewDetails(record.id)}
            >
              查看
            </Button>
          </Tooltip>
          <Tooltip title="删除文档">
            <Button
              danger
              icon={<DeleteOutlined />}
              size="small"
              onClick={() => {
                setSelectedRowKeys([record.id]);
                showDeleteConfirm();
              }}
            >
              删除
            </Button>
          </Tooltip>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div
        style={{
          marginBottom: 24,
          background: "linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%)",
          borderRadius: "12px",
          padding: "24px",
          color: "white",
          boxShadow: "0 4px 12px rgba(19, 194, 194, 0.15)",
        }}
      >
        <Title level={2} style={{ color: "white", margin: 0 }}>
          文档管理
        </Title>
        <Paragraph
          style={{
            color: "rgba(255, 255, 255, 0.85)",
            fontSize: "16px",
            marginBottom: 0,
          }}
        >
          在这里管理您的所有文档，上传新文档或查看已有文档。
        </Paragraph>
      </div>

      <Row gutter={[24, 24]}>
        <Col span={24}>
          <Card
            style={{
              borderRadius: "12px",
              boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
            }}
          >
            <Row gutter={[16, 16]}>
              <Col xs={24} md={8}>
                <Card
                  bordered={false}
                  style={{
                    background:
                      "linear-gradient(135deg, #52c41a 0%, #73d13d 100%)",
                    color: "white",
                    borderRadius: "8px",
                    height: "100%",
                  }}
                >
                  <Statistic
                    title={<span style={{ color: "white" }}>文档总数</span>}
                    value={documents.length}
                    valueStyle={{ color: "white", fontSize: "28px" }}
                    suffix={<Text style={{ color: "white" }}>个</Text>}
                    prefix={<FileTextOutlined style={{ marginRight: 8 }} />}
                  />
                </Card>
              </Col>
              <Col xs={24} md={8}>
                <Card
                  bordered={false}
                  style={{
                    background:
                      "linear-gradient(135deg, #1890ff 0%, #096dd9 100%)",
                    color: "white",
                    borderRadius: "8px",
                    height: "100%",
                  }}
                >
                  <Statistic
                    title={<span style={{ color: "white" }}>存储空间</span>}
                    value={formatFileSize(totalStorageUsed)}
                    valueStyle={{ color: "white", fontSize: "28px" }}
                    prefix={<CloudUploadOutlined style={{ marginRight: 8 }} />}
                  />
                </Card>
              </Col>
              {/* <Col xs={24} md={8}>
                <Card
                  bordered={false}
                  style={{
                    borderRadius: "8px",
                    height: "100%",
                    backgroundColor: "#f9f9f9",
                  }}
                >
                  <div>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        marginBottom: 8,
                      }}
                    >
                      <Text strong>存储使用情况</Text>
                      <Text type="secondary">
                        {Math.round((totalStorageUsed / storageLimit) * 100)}%
                      </Text>
                    </div>
                    <Progress
                      percent={Math.round(
                        (totalStorageUsed / storageLimit) * 100
                      )}
                      status={
                        totalStorageUsed > storageLimit * 0.8
                          ? "exception"
                          : "normal"
                      }
                      strokeColor={{
                        "0%": "#108ee9",
                        "100%":
                          totalStorageUsed > storageLimit * 0.8
                            ? "#ff4d4f"
                            : "#87d068",
                      }}
                    />
                    <Text type="secondary" style={{ fontSize: "12px" }}>
                      已使用 {formatFileSize(totalStorageUsed)} /{" "}
                      {formatFileSize(storageLimit)}
                    </Text>
                  </div>
                </Card>
              </Col> */}
            </Row>
          </Card>
        </Col>

        <Col span={24}>
          <Card
            style={{
              borderRadius: "12px",
              boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
            }}
          >
            {/* 操作区域 */}
            <div
              style={{
                marginBottom: 16,
                display: "flex",
                justifyContent: "space-between",
                flexWrap: "wrap",
                gap: "12px",
              }}
            >
              <div>
                <Button
                  type="primary"
                  icon={<UploadOutlined />}
                  onClick={() => setUploadModalVisible(true)}
                  style={{
                    borderRadius: "6px",
                    boxShadow: "0 2px 6px rgba(24, 144, 255, 0.15)",
                    marginRight: 12,
                  }}
                >
                  上传文档
                </Button>

                {selectedRowKeys.length > 0 && (
                  <Button
                    danger
                    icon={<DeleteOutlined />}
                    onClick={showDeleteConfirm}
                    style={{ borderRadius: "6px" }}
                  >
                    删除所选 ({selectedRowKeys.length})
                  </Button>
                )}
              </div>

              <Search
                placeholder="搜索文档..."
                allowClear
                onSearch={handleSearch}
                onChange={(e) => handleSearch(e.target.value)}
                style={{ width: 250 }}
                enterButton={<SearchOutlined />}
              />
            </div>

            {renderSearchResultCount()}

            {/* 文档列表 */}
            {loading ? (
              <div
                style={{
                  textAlign: "center",
                  padding: "50px",
                  background: "#f9f9f9",
                  borderRadius: "8px",
                }}
              >
                <Spin size="large" />
                <div style={{ marginTop: 16, color: "#8c8c8c" }}>
                  加载中，请稍候...
                </div>
              </div>
            ) : filteredDocuments.length === 0 ? (
              <Empty
                description={
                  searchText ? "没有找到匹配的文档" : "暂无文档，请上传文档"
                }
                style={{
                  padding: "40px",
                  background: "#f9f9f9",
                  borderRadius: "8px",
                }}
              />
            ) : (
              <Table
                rowSelection={{
                  selectedRowKeys,
                  onChange: setSelectedRowKeys,
                  selections: [
                    Table.SELECTION_ALL,
                    Table.SELECTION_INVERT,
                    {
                      key: "none",
                      text: "取消选择",
                      onSelect: () => setSelectedRowKeys([]),
                    },
                  ],
                }}
                columns={columns}
                dataSource={filteredDocuments.map((doc) => ({
                  ...doc,
                  key: doc.id,
                }))}
                rowKey="id"
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                  showTotal: (total) => `共 ${total} 个文档`,
                  pageSizeOptions: ["10", "20", "50"],
                }}
                style={{
                  borderRadius: "8px",
                  overflow: "hidden",
                }}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* 上传对话框 */}
      <Modal
        title={
          <div style={{ display: "flex", alignItems: "center" }}>
            <CloudUploadOutlined
              style={{ fontSize: "20px", color: "#1890ff", marginRight: 8 }}
            />
            <span>上传文档</span>
          </div>
        }
        open={uploadModalVisible}
        onCancel={() => {
          setUploadModalVisible(false);
          setFileList([]);
          form.resetFields();
          setUploadProgress(0);
        }}
        footer={[
          <Button key="back" onClick={() => setUploadModalVisible(false)}>
            取消
          </Button>,
          <Button
            key="submit"
            type="primary"
            loading={uploading}
            onClick={handleUpload}
            disabled={fileList.length === 0}
          >
            上传
          </Button>,
        ]}
        width={700}
        styles={{
          body: {
            padding: "24px",
          },
        }}
      >
        <div
          style={{
            background: "#f6f8fa",
            padding: "20px",
            borderRadius: "8px",
            marginBottom: "20px",
            textAlign: "center",
            border: "1px dashed #d9d9d9",
          }}
        >
          <Upload
            multiple
            beforeUpload={beforeUpload}
            onChange={handleFileChange}
            fileList={fileList}
            accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.ppt,.pptx"
            maxCount={5}
            listType="picture"
            style={{ width: "100%" }}
          >
            <Button
              icon={<UploadOutlined />}
              type="primary"
              style={{
                height: "60px",
                width: "180px",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                margin: "0 auto",
                fontSize: "16px",
              }}
            >
              选择文件
            </Button>
            <div style={{ marginTop: "12px", color: "#8c8c8c" }}>
              支持 PDF、Word、Excel、TXT、Markdown、PPT 等格式
            </div>
          </Upload>
        </div>

        {uploading && (
          <div style={{ marginBottom: "24px" }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 8,
              }}
            >
              <span>上传进度</span>
              <span>{uploadProgress}%</span>
            </div>
            <Progress percent={uploadProgress} status="active" />
          </div>
        )}

        {fileList.length > 0 && (
          <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
            <Form.List name="titles">
              {(fields) => (
                <>
                  {fileList.map((file, index) => (
                    <Form.Item
                      key={index}
                      label={
                        <div style={{ display: "flex", alignItems: "center" }}>
                          <Avatar
                            icon={getFileIcon(file.name)}
                            size="small"
                            style={{
                              marginRight: 8,
                              backgroundColor: "rgba(0,0,0,0.04)",
                            }}
                          />
                          <span>文档标题 ({file.name})</span>
                        </div>
                      }
                      name={index}
                      rules={[{ required: true, message: "请输入文档标题" }]}
                    >
                      <Input
                        placeholder="请输入文档标题"
                        allowClear
                        style={{ borderRadius: "6px" }}
                      />
                    </Form.Item>
                  ))}
                </>
              )}
            </Form.List>
          </Form>
        )}
      </Modal>

      {/* 文档详情抽屉 */}
      <Drawer
        title={
          selectedDocument && (
            <div style={{ display: "flex", alignItems: "center" }}>
              <Avatar
                icon={getFileIcon(selectedDocument.fileName)}
                size="small"
                style={{ marginRight: 8, backgroundColor: "rgba(0,0,0,0.04)" }}
              />
              <span>{selectedDocument.title}</span>
              <Tag
                color={getFileTypeTagColor(selectedDocument.fileName)}
                style={{ marginLeft: 8 }}
              >
                {selectedDocument.fileName?.split(".").pop()?.toUpperCase() ||
                  "未知"}
              </Tag>
            </div>
          )
        }
        width={800}
        placement="right"
        onClose={() => setDetailsDrawerVisible(false)}
        open={detailsDrawerVisible}
        styles={{
          body: {
            padding: "0",
          },
        }}
        extra={
          <Button type="primary" onClick={() => setDetailsDrawerVisible(false)}>
            关闭
          </Button>
        }
      >
        {selectedDocument && (
          <DocumentViewer
            document={selectedDocument}
            onClose={() => setDetailsDrawerVisible(false)}
          />
        )}
      </Drawer>
    </div>
  );
};

export default DocumentManagement;