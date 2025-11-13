import React, {useEffect, useState} from "react";
import {
    Avatar,
    Button,
    Card,
    Col,
    Empty,
    List,
    message,
    Progress,
    Row,
    Space,
    Spin,
    Statistic,
    Tag,
    Typography,
} from "antd";
import {
    AppstoreOutlined,
    BarChartOutlined,
    BookOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CrownOutlined,
    EditOutlined,
    EyeOutlined,
    FileProtectOutlined,
    FileSearchOutlined,
    FileTextOutlined,
    HighlightOutlined,
    LoadingOutlined,
    NotificationOutlined,
    RiseOutlined,
    SafetyOutlined,
} from "@ant-design/icons";
import {useAuth} from "../../context/AuthContext";
import {useNavigate} from "react-router-dom";
import {dashboardAPI} from "../../services/api";

const { Title, Paragraph, Text } = Typography;

// 渐变色定义 - 让组件更有视觉层次感
const gradients = {
  blue: "linear-gradient(135deg, #1890ff 0%, #36cfc9 100%)",
  green: "linear-gradient(135deg, #52c41a 0%, #73d13d 100%)",
  orange: "linear-gradient(135deg, #faad14 0%, #ffc53d 100%)",
  purple: "linear-gradient(135deg, #722ed1 0%, #9254de 100%)",
  magenta: "linear-gradient(135deg, #eb2f96 0%, #f759ab 100%)",
  cyan: "linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%)",
};

// 统计卡片样式 - 美化统计数据展示
const StatisticCard = ({ title, value, icon, color, suffix, description }) => (
  <Card
    hoverable
    style={{
      background: gradients[color],
      color: "#fff",
      borderRadius: "12px",
      overflow: "hidden",
      boxShadow: "0 4px 12px rgba(0, 0, 0, 0.1)",
      height: "100%",
      transition: "all 0.3s ease",
    }}
    styles={{
      body: {
        padding: "20px",
        height: "100%",
      },
    }}
  >
    <div style={{ display: "flex", alignItems: "center", marginBottom: 8 }}>
      <Avatar
        size="large"
        style={{ backgroundColor: "rgba(255, 255, 255, 0.2)", marginRight: 8 }}
        icon={icon}
      />
      <Text strong style={{ color: "#fff", fontSize: "16px" }}>
        {title}
      </Text>
    </div>
    <Statistic
      value={value}
      suffix={suffix}
      valueStyle={{ color: "#fff", fontSize: "28px", fontWeight: "bold" }}
    />
    {description && (
      <Paragraph
        style={{
          color: "rgba(255, 255, 255, 0.85)",
          margin: "8px 0 0",
          fontSize: "13px",
        }}
      >
        {description}
      </Paragraph>
    )}
  </Card>
);

// 活动项目样式 - 美化活动列表
const ActivityItem = ({ activity }) => {
  const iconStyle = { fontSize: "18px" };
  const getActivityIcon = () => {
    switch (activity.type) {
      case "SUMMARY":
        return <EyeOutlined style={{ ...iconStyle, color: "#1890ff" }} />;
      case "KEYWORDS":
        return <HighlightOutlined style={{ ...iconStyle, color: "#52c41a" }} />;
      case "SECURITY":
        return <SafetyOutlined style={{ ...iconStyle, color: "#faad14" }} />;
      case "POLISH":
        return <EditOutlined style={{ ...iconStyle, color: "#722ed1" }} />;
      case "UPLOAD":
        return (
          <FileProtectOutlined style={{ ...iconStyle, color: "#1890ff" }} />
        );
      case "DOWNLOAD":
        return <FileTextOutlined style={{ ...iconStyle, color: "#1890ff" }} />;
      default:
        return <FileTextOutlined style={{ ...iconStyle, color: "#1890ff" }} />;
    }
  };

  return (
    <List.Item
      style={{
        padding: "12px 16px",
        borderRadius: "8px",
        marginBottom: "8px",
        transition: "all 0.2s ease",
        background: "white",
        border: "1px solid #f0f0f0",
        "&:hover": {
          boxShadow: "0 2px 10px rgba(0,0,0,0.05)",
          background: "#fafafa",
        },
      }}
    >
      <List.Item.Meta
        avatar={
          <Avatar
            style={{
              backgroundColor:
                activity.color === "blue"
                  ? "#e6f7ff"
                  : activity.color === "green"
                  ? "#f6ffed"
                  : activity.color === "orange"
                  ? "#fff7e6"
                  : activity.color === "purple"
                  ? "#f9f0ff"
                  : activity.color === "cyan"
                  ? "#e6fffb"
                  : "#e6f7ff",
              color:
                activity.color === "blue"
                  ? "#1890ff"
                  : activity.color === "green"
                  ? "#52c41a"
                  : activity.color === "orange"
                  ? "#faad14"
                  : activity.color === "purple"
                  ? "#722ed1"
                  : activity.color === "cyan"
                  ? "#13c2c2"
                  : "#1890ff",
            }}
            icon={getActivityIcon()}
          />
        }
        title={
          <div
            style={{
              fontWeight: 500,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <span style={{ cursor: "pointer" }}>{activity.documentName}</span>
            <Tag
              color={activity.color}
              style={{ marginLeft: 8, borderRadius: "4px" }}
            >
              {activity.tag}
            </Tag>
          </div>
        }
        description={
          <Space>
            <span style={{ fontSize: "12px", color: "#8c8c8c" }}>
              <ClockCircleOutlined /> {activity.timestamp}
            </span>
            <span style={{ fontSize: "12px", color: "#8c8c8c" }}>
              操作: {activity.operationType || "查看"}
            </span>
          </Space>
        }
      />
    </List.Item>
  );
};

const Dashboard = () => {
  const { currentUser } = useAuth();
  const navigate = useNavigate();
  const [statistics, setStatistics] = useState({
    documents: 0,
    analysis: 0,
    keywords: 0,
    security: 0,
    summary: 0,
    polish: 0,
  });
  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // 获取仪表盘数据
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        setError(null);

        // 并行请求获取仪表盘数据
        const [statisticsResponse, activitiesResponse] = await Promise.all([
          dashboardAPI.getStatistics(),
          dashboardAPI.getRecentActivities(5),
        ]);

        // 确保设置默认值为0，以防后端未返回这些字段
        const statsData = {
          ...statisticsResponse.data,
          summary: statisticsResponse.data.summary || 0,
          polish: statisticsResponse.data.polish || 0,
        };
        setStatistics(statsData);

        // 处理活动数据，添加图标和标签
        const processedActivities = activitiesResponse.data.map((activity) => {
          // 基于活动类型添加图标和标签颜色
          let icon = <FileTextOutlined />;
          let tag = "文档";
          let color = "default";
          let operationType = "查看";

          switch (activity.type) {
            case "SUMMARY":
              icon = <EyeOutlined />;
              tag = "摘要";
              color = "blue";
              operationType = "摘要生成";
              break;
            case "KEYWORDS":
              icon = <HighlightOutlined />;
              tag = "关键词";
              color = "green";
              operationType = "关键词提取";
              break;
            case "SECURITY":
              icon = <SafetyOutlined />;
              tag = "敏感信息";
              color = "orange";
              operationType = "安全检查";
              break;
            case "POLISH":
              icon = <HighlightOutlined />;
              tag = "润色";
              color = "purple";
              operationType = "文档润色";
              break;
            case "UPLOAD":
              icon = <FileTextOutlined />;
              tag = "上传";
              color = "cyan";
              operationType = "文档上传";
              break;
            case "DOWNLOAD":
              icon = <FileTextOutlined />;
              tag = "下载";
              color = "geekblue";
              operationType = "文档下载";
              break;
            default:
              break;
          }

          return {
            ...activity,
            icon,
            tag,
            color,
            operationType,
          };
        });

        setActivities(processedActivities);
      } catch (err) {
        console.error("获取仪表盘数据失败:", err);
        setError("获取仪表盘数据失败，请稍后再试");
        message.error("获取仪表盘数据失败，请稍后再试");
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  // 加载指示器配置
  const antIcon = <LoadingOutlined style={{ fontSize: 24 }} spin />;

  if (loading) {
    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100%",
          padding: "50px 0",
          flexDirection: "column",
        }}
      >
        <Spin indicator={antIcon} size="large" />
        <Paragraph style={{ marginTop: 16, color: "#8c8c8c" }}>
          正在加载您的智能文档仪表盘...
        </Paragraph>
      </div>
    );
  }

  if (error) {
    return (
      <div
        style={{
          textAlign: "center",
          padding: "50px 0",
          background: "#f9f9f9",
          borderRadius: "8px",
        }}
      >
        <Empty description={error} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        <Button
          type="primary"
          onClick={() => window.location.reload()}
          style={{ marginTop: 16 }}
        >
          重试
        </Button>
      </div>
    );
  }

  // 计算使用进度 (模拟数据)
  const usageProgress =
    Math.min(Math.round((statistics.documents / 100) * 100), 100) || 25;

  return (
    <div>
      <div
        style={{
          marginBottom: 24,
          background: "linear-gradient(135deg, #1890ff 0%, #096dd9 100%)",
          borderRadius: "12px",
          padding: "24px",
          color: "white",
          boxShadow: "0 4px 12px rgba(24, 144, 255, 0.15)",
        }}
      >
        <Title level={2} style={{ color: "white", margin: 0 }}>
          欢迎回来, {currentUser?.username}!
        </Title>
        <Paragraph
          style={{
            color: "rgba(255, 255, 255, 0.85)",
            fontSize: "16px",
            marginBottom: 0,
          }}
        >
          智能RAG系统帮助您高效管理和分析各类文档
        </Paragraph>
      </div>

      <Row gutter={[24, 24]}>
        <Col xs={24} sm={12} md={6}>
          <StatisticCard
            title="文档总数"
            value={statistics.documents}
            icon={<FileTextOutlined />}
            color="blue"
            suffix="份"
            description="您的所有已上传文档"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatisticCard
            title="分析次数"
            value={statistics.analysis}
            icon={<EyeOutlined />}
            color="green"
            suffix="次"
            description="文档总计分析次数"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatisticCard
            title="提取关键词"
            value={statistics.keywords}
            icon={<HighlightOutlined />}
            color="orange"
            suffix="次"
            description="关键词提取使用次数"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatisticCard
            title="安全检测"
            value={statistics.security}
            icon={<SafetyOutlined />}
            color="purple"
            suffix="次"
            description="敏感信息检测次数"
          />
        </Col>
      </Row>

      <Row gutter={[24, 24]} style={{ marginTop: 24 }}>
        <Col xs={24} sm={12} md={6}>
          <StatisticCard
            title="生成摘要"
            value={statistics.summary}
            icon={<FileSearchOutlined />}
            color="cyan"
            suffix="次"
            description="文档摘要生成次数"
          />
        </Col>
        <Col xs={24} sm={12} md={6}>
          <StatisticCard
            title="内容润色"
            value={statistics.polish}
            icon={<EditOutlined />}
            color="magenta"
            suffix="次"
            description="文档内容润色次数"
          />
        </Col>
      </Row>

      <Row gutter={[24, 24]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={16}>
          <Card
            title={
              <div style={{ display: "flex", alignItems: "center" }}>
                <ClockCircleOutlined
                  style={{ marginRight: 8, color: "#1890ff" }}
                />
                <span>最近活动</span>
              </div>
            }
            extra={
              <Button type="link" icon={<RiseOutlined />}>
                查看全部
              </Button>
            }
            style={{
              borderRadius: "12px",
              boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
            }}
            styles={{
              body: {
                padding: "8px 16px",
              },
            }}
          >
            <List
              itemLayout="horizontal"
              dataSource={activities}
              locale={{ emptyText: <Empty description="暂无活动记录" /> }}
              renderItem={(item) => <ActivityItem activity={item} />}
            />
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card
            title={
              <div style={{ display: "flex", alignItems: "center" }}>
                <BookOutlined style={{ marginRight: 8, color: "#1890ff" }} />
                <span>常用功能</span>
              </div>
            }
            extra={
              <Button type="link" icon={<AppstoreOutlined />}>
                全部功能
              </Button>
            }
            style={{
              borderRadius: "12px",
              boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
            }}
          >
            <List
              size="large"
              dataSource={[
                {
                  title: "文档摘要",
                  link: "/analysis/summary",
                  icon: <EyeOutlined style={{ color: "#1890ff" }} />,
                },
                {
                  title: "关键词提取",
                  link: "/analysis/keywords",
                  icon: <HighlightOutlined style={{ color: "#52c41a" }} />,
                },
                {
                  title: "敏感信息检测",
                  link: "/analysis/security",
                  icon: <SafetyOutlined style={{ color: "#faad14" }} />,
                },
                {
                  title: "文档润色",
                  link: "/analysis/polish",
                  icon: <EditOutlined style={{ color: "#722ed1" }} />,
                },
              ]}
              renderItem={(item) => (
                <List.Item
                  style={{
                    cursor: "pointer",
                    padding: "12px 16px",
                    borderRadius: "8px",
                    transition: "all 0.3s ease",
                    marginBottom: "8px",
                    "&:hover": {
                      background: "#f5f5f5",
                    },
                  }}
                  onClick={() => navigate(item.link)}
                >
                  <Space>
                    <Avatar
                      size="small"
                      style={{ backgroundColor: "rgba(24, 144, 255, 0.1)" }}
                      icon={item.icon}
                    />
                    <span>{item.title}</span>
                  </Space>
                  <div>
                    <Button type="link" size="small" style={{ padding: 0 }}>
                      使用
                    </Button>
                  </div>
                </List.Item>
              )}
            />
          </Card>

          <Card
            title={
              <div style={{ display: "flex", alignItems: "center" }}>
                <NotificationOutlined
                  style={{ marginRight: 8, color: "#1890ff" }}
                />
                <span>系统公告</span>
              </div>
            }
            style={{
              marginTop: 24,
              borderRadius: "12px",
              boxShadow: "0 4px 12px rgba(0, 0, 0, 0.05)",
            }}
          >
            <div
              style={{
                padding: "12px",
                background: "#f6ffed",
                borderRadius: "8px",
                border: "1px solid #b7eb8f",
                marginBottom: "16px",
              }}
            >
              <div style={{ display: "flex", alignItems: "flex-start" }}>
                <CheckCircleOutlined
                  style={{
                    color: "#52c41a",
                    marginRight: "8px",
                    marginTop: "4px",
                  }}
                />
                <div>
                  <Text strong>新功能发布</Text>
                  <Paragraph style={{ margin: "4px 0 0" }}>
                    现已支持多种格式文档分析，包括PDF、Word、Excel等。
                  </Paragraph>
                </div>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;