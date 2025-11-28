import React, { useEffect } from 'react';
import { Card, Form, Input, Button, Upload, Select, message, Avatar, Row, Col, Divider } from 'antd';
import { UserOutlined, UploadOutlined, SettingOutlined, IdcardOutlined } from '@ant-design/icons';
import { useAppStore } from '../../store/useAppStore';
import { userService } from '../../services/userService';

const UserProfile: React.FC = () => {
  const { userInfo, setUserInfo, llmModels, embeddingModels, rerankModels, fetchModelLists, localSettings, updateLocalSettings } = useAppStore();
  const [profileForm] = Form.useForm();
  const [settingsForm] = Form.useForm();

  useEffect(() => {
      const init = async () => {
          await fetchModelLists();
          
          // Validate settings against fetched lists
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
        
        // Update Profile Info
        const res: any = await userService.updateProfile(profileData);
        
        if (res.code === 200) {
            setUserInfo(res.data);
            message.success('个人信息更新成功');
        } else {
            message.error(res.message || '更新失败');
            return;
        }

        // Change Password if provided
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
    <div className="p-6 bg-gray-50 min-h-screen">
      <Row gutter={[24, 24]} justify="center">
        <Col xs={24} lg={10}>
            <Card 
                title={<><IdcardOutlined /> 个人资料</>} 
                bordered={false}
            >
                <div style={{ textAlign: 'center', marginBottom: 24 }}>
                    <Avatar size={100} src={userInfo?.avatarUrl || userInfo?.avatar} icon={<UserOutlined />} />
                    <div style={{ marginTop: 16 }}>
                        <Upload 
                            showUploadList={false}
                            customRequest={handleAvatarUpload}
                        >
                            <Button icon={<UploadOutlined />}>更换头像</Button>
                        </Upload>
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

                    <Divider orientation="left" plain>安全设置</Divider>

                    <Form.Item label="新密码" name="password">
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
                title={<><SettingOutlined /> 偏好设置</>} 
                bordered={false}
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
