import React, { useEffect, useState } from 'react';
import { Card, Form, Input, Button, Upload, Select, Avatar, Row, Col, Collapse, App, theme, Typography } from 'antd';
import { UserOutlined, CameraOutlined, SettingOutlined, IdcardOutlined, LockOutlined } from '@ant-design/icons';
import { useAppStore } from '../../store/useAppStore';
import { userService } from '../../services/userService';

const { Text } = Typography;

const UserProfile: React.FC = () => {
  const { message } = App.useApp();
  const { token } = theme.useToken();
  const { userInfo, setUserInfo, llmModels, embeddingModels, rerankModels, fetchModelLists, localSettings, updateLocalSettings } = useAppStore();
  const [profileForm] = Form.useForm();
  const [settingsForm] = Form.useForm();
  const [avatarHovered, setAvatarHovered] = useState(false);

  useEffect(() => {
      const init = async () => {
          await fetchModelLists();

          const { llmModels, embeddingModels, rerankModels } = useAppStore.getState();
          const currentSettings = settingsForm.getFieldsValue();

          if (llmModels.length === 0 || (currentSettings.defaultModel && !llmModels.includes(currentSettings.defaultModel))) {
              settingsForm.setFieldValue('defaultModel', undefined);
          }
          if (embeddingModels.length === 0 || (currentSettings.defaultEmbedding && !embeddingModels.includes(currentSettings.defaultEmbedding))) {
              settingsForm.setFieldValue('defaultEmbedding', undefined);
          }
          if (rerankModels.length === 0 || (currentSettings.defaultRerank && !rerankModels.includes(currentSettings.defaultRerank))) {
              settingsForm.setFieldValue('defaultRerank', undefined);
          }
      };
      init();

      const fetchProfile = async () => {
          try {
              const res: any = await userService.getProfile();
              if (res.code === 200) {
                  setUserInfo(res.data);
                  profileForm.setFieldsValue(res.data);
              }
          } catch (error) {
              // console.error(error);
          }
      };
      fetchProfile();
      settingsForm.setFieldsValue(localSettings);
  }, []);

  const handleUpdateProfile = async (values: any) => {
    try {
        const { password, confirmPassword, ...profileData } = values;

        const res: any = await userService.updateProfile(profileData);

        if (res.code === 200) {
            setUserInfo(res.data);
            message.success('个人信息更新成功');
        } else {
            message.error(res.message || '更新失败');
            return;
        }

        if (password) {
             const pwdRes: any = await userService.changePassword({ password });
            if (pwdRes.code === 200) {
                message.success('密码修改成功');
                profileForm.setFieldValue('password', '');
                profileForm.setFieldValue('confirmPassword', '');
            } else {
                message.error(pwdRes.message || '密码修改失败');
            }
        }
    } catch (error) {
        // message.error('操作失败');
    }
  };

  const handleUpdateSettings = (values: any) => {
      updateLocalSettings(values);
      message.success('偏好设置已保存');
  };

  const handleAvatarUpload = async (options: any) => {
      const { file, onSuccess, onError } = options;
      const formData = new FormData();
      formData.append('file', file);

      try {
          const res: any = await userService.uploadAvatar(formData);
          if (res.code === 200) {
              if (userInfo) {
                  setUserInfo({ ...userInfo, avatarUrl: res.data.avatarUrl });
              }
              onSuccess(res.data);
              message.success('头像上传成功');
          } else {
              onError(new Error(res.message));
              message.error('头像上传失败');
          }
      } catch (error) {
          onError(error);
          message.error('头像上传失败');
      }
  };

  return (
    <div style={{ height: '100%', overflowY: 'auto', padding: '0 8px 24px' }}>
      <Row gutter={[24, 24]} justify="center">
        <Col xs={24} lg={10}>
            <Card
                title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <IdcardOutlined style={{ color: token.colorPrimary }} />
                        个人资料
                    </span>
                }
            >
                {/* 头像区域（hover 显示相机遮罩） */}
                <div style={{ textAlign: 'center', marginBottom: 28 }}>
                    <Upload
                        showUploadList={false}
                        customRequest={handleAvatarUpload}
                    >
                        <div
                            style={{ position: 'relative', display: 'inline-block', cursor: 'pointer' }}
                            onMouseEnter={() => setAvatarHovered(true)}
                            onMouseLeave={() => setAvatarHovered(false)}
                        >
                            <Avatar
                                size={96}
                                src={userInfo?.avatarUrl || userInfo?.avatar}
                                icon={<UserOutlined />}
                                style={{ border: `2px solid ${token.colorBorderSecondary}` }}
                            />
                            {/* 相机遮罩 */}
                            <div style={{
                                position: 'absolute',
                                inset: 0,
                                borderRadius: '50%',
                                background: 'rgba(0,0,0,0.42)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                opacity: avatarHovered ? 1 : 0,
                                transition: 'opacity 0.2s ease',
                            }}>
                                <CameraOutlined style={{ fontSize: 22, color: '#fff' }} />
                            </div>
                        </div>
                    </Upload>
                    <div style={{ marginTop: 10 }}>
                        <Text style={{ fontWeight: 600, fontSize: 15 }}>{userInfo?.username || 'User'}</Text>
                        <br />
                        <Text type="secondary" style={{ fontSize: 13 }}>{userInfo?.email || ''}</Text>
                    </div>
                </div>

                <Form
                    form={profileForm}
                    layout="vertical"
                    onFinish={handleUpdateProfile}
                >
                    <Form.Item label="用户名" name="username" rules={[{ required: true }]}>
                        <Input />
                    </Form.Item>

                    <Form.Item label="邮箱" name="email" rules={[{ required: true, type: 'email' }]}>
                        <Input disabled />
                    </Form.Item>

                    {/* 密码修改用 Collapse 折叠（默认关闭） */}
                    <Collapse
                        ghost
                        style={{ marginBottom: 16, marginLeft: -8, marginRight: -8 }}
                        items={[
                            {
                                key: 'password',
                                label: (
                                    <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: token.colorTextSecondary }}>
                                        <LockOutlined />
                                        修改密码
                                    </span>
                                ),
                                children: (
                                    <div>
                                        <Form.Item label="新密码" name="password" style={{ marginBottom: 12 }}>
                                            <Input.Password placeholder="如果不修改请留空" />
                                        </Form.Item>

                                        <Form.Item
                                            label="确认新密码"
                                            name="confirmPassword"
                                            dependencies={['password']}
                                            rules={[
                                                ({ getFieldValue }) => ({
                                                    validator(_, value) {
                                                        if (!value || getFieldValue('password') === value) {
                                                            return Promise.resolve();
                                                        }
                                                        return Promise.reject(new Error('两次输入的密码不一致!'));
                                                    },
                                                }),
                                            ]}
                                        >
                                            <Input.Password placeholder="请再次输入新密码" />
                                        </Form.Item>
                                    </div>
                                ),
                            },
                        ]}
                    />

                    <Form.Item>
                        <Button type="primary" htmlType="submit" block>
                            保存个人信息
                        </Button>
                    </Form.Item>
                </Form>
            </Card>
        </Col>

        <Col xs={24} lg={10}>
            <Card
                title={
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <SettingOutlined style={{ color: token.colorPrimary }} />
                        偏好设置
                    </span>
                }
            >
                <Form
                    form={settingsForm}
                    layout="vertical"
                    onFinish={handleUpdateSettings}
                >
                    <Form.Item label="默认大模型" name="defaultModel">
                        <Select placeholder="选择默认大模型" allowClear>
                            {llmModels.map(model => (
                                <Select.Option key={model} value={model}>{model}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item label="默认文本嵌入模型" name="defaultEmbedding">
                        <Select placeholder="选择默认 Embedding 模型" allowClear>
                            {embeddingModels.map(model => (
                                <Select.Option key={model} value={model}>{model}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item label="默认重排序模型" name="defaultRerank">
                        <Select placeholder="选择默认 Rerank 模型" allowClear>
                            {rerankModels.map(model => (
                                <Select.Option key={model} value={model}>{model}</Select.Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" block>
                            保存偏好设置
                        </Button>
                    </Form.Item>
                </Form>
            </Card>
        </Col>
      </Row>
    </div>
  );
};

export default UserProfile;
