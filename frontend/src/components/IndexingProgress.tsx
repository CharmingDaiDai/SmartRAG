import { Progress, Card, Typography, Space, Tag, Alert } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, FileTextOutlined } from '@ant-design/icons';

const { Text, Title } = Typography;

interface IndexingProgressProps {
  kbId: string;
  taskId?: number;
  onComplete?: () => void;
  onClose?: () => void;
}

interface ProgressData {
  taskId: number;
  total: number;
  completed: number;
  failed: number;
  percentage: number;
}

interface StepData {
  docId: number;
  docName: string;
  step: string;
  stepName: string;
}

interface ErrorData {
  docId: number;
  docName: string;
  error: string;
}

interface DoneData {
  taskId: number;
  total: number;
  completed: number;
  failed: number;
}

const STEP_COLORS: Record<string, string> = {
  PARSING: 'blue',
  CHUNKING: 'cyan',
  EMBEDDING: 'purple',
  STORING: 'orange',
};

export default function IndexingProgress({ kbId, onComplete, onClose }: IndexingProgressProps) {
  const [progress, setProgress] = useState<ProgressData | null>(null);
  const [currentStep, setCurrentStep] = useState<StepData | null>(null);
  const [errors, setErrors] = useState<ErrorData[]>([]);
  const [isDone, setIsDone] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    // 创建 SSE 连接
    const token = localStorage.getItem('token');
    const eventSource = new EventSource(
      `/api/documents/index-progress/${kbId}?token=${token}`
    );
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      setIsConnected(true);
    };

    eventSource.onerror = () => {
      setIsConnected(false);
    };

    // 监听进度事件
    eventSource.addEventListener('progress', (event) => {
      const data: ProgressData = JSON.parse(event.data);
      setProgress(data);
    });

    // 监听步骤事件
    eventSource.addEventListener('step', (event) => {
      const data: StepData = JSON.parse(event.data);
      setCurrentStep(data);
    });

    // 监听错误事件
    eventSource.addEventListener('error', (event) => {
      const data: ErrorData = JSON.parse(event.data);
      setErrors(prev => [...prev, data]);
    });

    // 监听完成事件
    eventSource.addEventListener('done', (event) => {
      const data: DoneData = JSON.parse(event.data);
      setProgress({
        taskId: data.taskId,
        total: data.total,
        completed: data.completed,
        failed: data.failed,
        percentage: 100,
      });
      setIsDone(true);
      setCurrentStep(null);
      eventSource.close();

      // 延迟调用完成回调
      setTimeout(() => {
        onComplete?.();
      }, 1500);
    });

    return () => {
      eventSource.close();
    };
  }, [kbId, onComplete]);

  const getStatusIcon = () => {
    if (isDone) {
      return progress?.failed === 0
        ? <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 24 }} />
        : <CloseCircleOutlined style={{ color: '#ff4d4f', fontSize: 24 }} />;
    }
    return <LoadingOutlined style={{ color: '#1677ff', fontSize: 24 }} spin />;
  };

  const getStatusText = () => {
    if (isDone) {
      if (progress?.failed === 0) {
        return '索引构建完成';
      }
      return `索引构建完成，${progress?.failed} 个文档失败`;
    }
    return '索引构建中...';
  };

  const progressStatus = isDone
    ? (progress?.failed === 0 ? 'success' : 'exception')
    : 'active';

  return (
    <Card
      size="small"
      style={{
        marginBottom: 16,
        borderColor: isDone ? (progress?.failed === 0 ? '#b7eb8f' : '#ffccc7') : '#91caff',
        backgroundColor: isDone ? (progress?.failed === 0 ? '#f6ffed' : '#fff2f0') : '#e6f4ff'
      }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Space>
          {getStatusIcon()}
          <Title level={5} style={{ margin: 0 }}>{getStatusText()}</Title>
          {!isConnected && !isDone && (
            <Tag color="warning">连接中...</Tag>
          )}
        </Space>

        {progress && (
          <>
            <Progress
              percent={progress.percentage}
              status={progressStatus}
              size="small"
            />
            <Space>
              <Text type="secondary">
                已完成: {progress.completed}/{progress.total}
              </Text>
              {progress.failed > 0 && (
                <Text type="danger">
                  失败: {progress.failed}
                </Text>
              )}
            </Space>
          </>
        )}

        {currentStep && (
          <Space>
            <FileTextOutlined />
            <Text ellipsis style={{ maxWidth: 200 }}>{currentStep.docName}</Text>
            <Tag color={STEP_COLORS[currentStep.step] || 'default'}>
              {currentStep.stepName}
            </Tag>
          </Space>
        )}

        {errors.length > 0 && (
          <div style={{ maxHeight: 100, overflowY: 'auto' }}>
            {errors.slice(-3).map((err, index) => (
              <Alert
                key={index}
                type="error"
                size="small"
                message={
                  <Text ellipsis style={{ maxWidth: 300 }}>
                    {err.docName}: {err.error}
                  </Text>
                }
                style={{ marginTop: 4, padding: '4px 8px' }}
                showIcon
              />
            ))}
          </div>
        )}
      </Space>
    </Card>
  );
}
