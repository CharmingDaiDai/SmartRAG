import React, {useState} from "react";
import {Alert, Button, Card, Checkbox, Col, Divider, Form, Input, message, Row, Typography,} from "antd";
import {
    GithubOutlined,
    LockOutlined,
    MailOutlined,
    QqOutlined,
    SafetyOutlined,
    UserOutlined,
    WechatOutlined,
} from "@ant-design/icons";
import {Link, useNavigate} from "react-router-dom";
import {useAuth} from "../../context/AuthContext";
import "../../styles/auth.css";

const { Title, Text, Paragraph } = Typography;

const Register = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const { register } = useAuth();
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    setError("");
    try {
      // 确保传递正确的注册数据给register函数
      const userData = {
        username: values.username,
        email: values.email,
        password: values.password,
        // 移除确认密码字段，后端API不需要
        // confirmPassword: values.confirmPassword
      };
      
      const success = await register(userData);
      if (success) {
        message.success('注册成功！');
        navigate("/dashboard");
      } else {
        setError("注册失败，请稍后重试");
      }
    } catch (err) {
      console.error('注册错误:', err);
      setError("注册时发生错误，请稍后再试");
    } finally {
      setLoading(false);
    }
  };

  const handleSocialLogin = (platform) => {
    message.info(`正在使用${platform}登录，此功能正在开发中...`);
    // 实际调用第三方登录API的代码会在这里
  };

  return (
    <div className="register-container">
      <Row className="register-row">
        <Col xs={24} md={12} className="register-left">
          <div className="register-left-content">
            <div className="brand-logo">
              {/* Use the image from the public folder */}
              <img
                src="/logo192.png"
                alt="SmartRAG Logo"
                className="brand-logo-img"
              />
              <Title level={3} className="brand-logo-title">
                SmartRAG
              </Title>
            </div>
            <Title level={2} className="welcome-title">
              加入我们
            </Title>
            <Paragraph className="welcome-subtitle">
              注册账户以体验智能RAG系统的强大功能
            </Paragraph>
            <img
              src="/register-illustration.svg"
              alt="Register"
              className="register-illustration"
              onError={(e) => {
                e.target.onerror = null;
                e.target.style.display = "none";
              }}
            />
          </div>
        </Col>

        <Col xs={24} md={12} className="register-right">
          <Card bordered={false} className="register-card">
            <div className="register-header">
              <Title level={3}>创建账户</Title>
              <Text type="secondary">填写以下信息完成注册</Text>
            </div>

            {error && (
              <Alert
                message={error}
                type="error"
                showIcon
                style={{ marginBottom: 24 }}
              />
            )}

            <Form
              name="register"
              onFinish={onFinish}
              size="large"
              layout="vertical"
            >
              <Form.Item
                name="username"
                rules={[{ required: true, message: "请输入用户名!" }]}
              >
                <Input prefix={<UserOutlined />} placeholder="用户名" />
              </Form.Item>

              <Form.Item
                name="email"
                rules={[
                  { required: true, message: "请输入邮箱!" },
                  { type: "email", message: "请输入有效的邮箱地址!" },
                ]}
              >
                <Input prefix={<MailOutlined />} placeholder="邮箱地址" />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: "请输入密码!" },
                  { min: 6, message: "密码长度至少为6位!" },
                ]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="密码" />
              </Form.Item>

              <Form.Item
                name="confirmPassword"
                dependencies={["password"]}
                rules={[
                  { required: true, message: "请确认您的密码!" },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue("password") === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error("两次输入的密码不一致!"));
                    },
                  }),
                ]}
              >
                <Input.Password
                  prefix={<SafetyOutlined />}
                  placeholder="确认密码"
                />
              </Form.Item>

              <Form.Item
                name="agreement"
                valuePropName="checked"
                rules={[
                  {
                    validator: (_, value) =>
                      value
                        ? Promise.resolve()
                        : Promise.reject(
                            new Error("请阅读并同意用户协议和隐私政策")
                          ),
                  },
                ]}
              >
                <Checkbox>
                  我已阅读并同意
                  <a href="#" className="agreement-link">
                    {" "}
                    用户协议{" "}
                  </a>
                  和
                  <a href="#" className="agreement-link">
                    {" "}
                    隐私政策
                  </a>
                </Checkbox>
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  className="register-button"
                  loading={loading}
                >
                  注册账户
                </Button>
              </Form.Item>

              <Divider className="register-divider">
                <span className="divider-text">或使用社交账号注册</span>
              </Divider>

              <div className="social-login">
                <Button
                  type="default"
                  icon={<GithubOutlined />}
                  size="large"
                  className="social-button github"
                  onClick={() => handleSocialLogin("GitHub")}
                >
                  GitHub
                </Button>
                <Button
                  type="default"
                  icon={<WechatOutlined />}
                  size="large"
                  className="social-button wechat"
                  onClick={() => handleSocialLogin("微信")}
                >
                  微信
                </Button>
                <Button
                  type="default"
                  icon={<QqOutlined />}
                  size="large"
                  className="social-button qq"
                  onClick={() => handleSocialLogin("QQ")}
                >
                  QQ
                </Button>
              </div>

              <div className="login-prompt">
                <Text type="secondary">已有账户? </Text>
                <Link to="/login" className="login-link">
                  立即登录
                </Link>
              </div>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Register;