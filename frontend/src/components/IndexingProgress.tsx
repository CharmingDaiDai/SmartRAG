import { Progress, Typography, Tag, Alert, theme } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, FileTextOutlined } from '@ant-design/icons';

const { Text } = Typography;

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

export default function IndexingProgress({ kbId, onComplete }: IndexingProgressProps) {
  const { token } = theme.useToken();
  const [progress, setProgress] = useState<ProgressData | null>(null);
  const [currentStep, setCurrentStep] = useState<StepData | null>(null);
  const [errors, setErrors] = useState<ErrorData[]>([]);
  const [isDone, setIsDone] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    const authToken = localStorage.getItem('token');
    const eventSource = new EventSource(
      `/api/documents/index-progress/${kbId}?token=${authToken}`
    );
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      setIsConnected(true);
    };

    eventSource.onerror = () => {
      setIsConnected(false);
    };

    eventSource.addEventListener('progress', (event) => {
      const data: ProgressData = JSON.parse((event as MessageEvent).data);
      setProgress(data);
    });

    eventSource.addEventListener('step', (event) => {
      const data: StepData = JSON.parse((event as MessageEvent).data);
      setCurrentStep(data);
    });

    eventSource.addEventListener('error', (event) => {
      const data: ErrorData = JSON.parse((event as MessageEvent).data);
      setErrors(prev => [...prev, data]);
    });

    eventSource.addEventListener('done', (event) => {
      const data: DoneData = JSON.parse((event as MessageEvent).data);
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

      setTimeout(() => {
        onComplete?.();
      }, 1500);
    });

    return () => {
      eventSource.close();
    };
  }, [kbId, onComplete]);

  const isSuccess = isDone && (!progress || progress.failed === 0);

  // Banner 背景和边框颜色
  const bannerBg = isDone
    ? (isSuccess ? 'rgba(16, 185, 129, 0.06)' : 'rgba(239, 68, 68, 0.06)')
    : 'rgba(99, 102, 241, 0.06)';
  const bannerBorder = isDone
    ? (isSuccess ? 'rgba(16, 185, 129, 0.25)' : 'rgba(239, 68, 68, 0.25)')
    : 'rgba(99, 102, 241, 0.20)';

  const statusIcon = isDone
    ? (isSuccess
        ? <CheckCircleOutlined style={{ color: '#10b981', fontSize: 16 }} />
        : <CloseCircleOutlined style={{ color: '#ef4444', fontSize: 16 }} />)
    : <LoadingOutlined style={{ color: token.colorPrimary, fontSize: 16 }} spin />;

  const statusText = isDone
    ? (isSuccess ? '索引构建完成' : `构建完成，${progress?.failed} 个文档失败`)
    : '索引构建中...';

  return (
    <div style={{
      marginBottom: 12,
      padding: '10px 14px',
      borderRadius: 8,
      border: `1px solid ${bannerBorder}`,
      background: bannerBg,
      display: 'flex',
      flexDirection: 'column',
      gap: 8,
    }}>
      {/* 主行：图标 + 状态 + 进度条 + 统计 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {statusIcon}

        <Text style={{ fontSize: 13, fontWeight: 500, flexShrink: 0 }}>{statusText}</Text>

        {!isConnected && !isDone && (
          <Tag color="warning" style={{ flexShrink: 0 }}>连接中...</Tag>
        )}

        {progress && (
          <>
            <div style={{ flex: 1, minWidth: 60 }}>
              <Progress
                percent={progress.percentage}
                status={isDone ? (isSuccess ? 'success' : 'exception') : 'active'}
                size="small"
                showInfo={false}
                strokeColor={isDone ? undefined : token.colorPrimary}
              />
            </div>
            <Text type="secondary" style={{ fontSize: 12, flexShrink: 0 }}>
              {progress.completed}/{progress.total}
              {progress.failed > 0 && (
                <Text type="danger" style={{ marginLeft: 6, fontSize: 12 }}>
                  · {progress.failed} 失败
                </Text>
              )}
            </Text>
          </>
        )}

        {currentStep && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
            <FileTextOutlined style={{ fontSize: 12, color: token.colorTextTertiary }} />
            <Text ellipsis style={{ maxWidth: 120, fontSize: 12, color: token.colorTextSecondary }}>
              {currentStep.docName}
            </Text>
            <Tag color={STEP_COLORS[currentStep.step] || 'default'} style={{ margin: 0, fontSize: 11 }}>
              {currentStep.stepName}
            </Tag>
          </div>
        )}
      </div>

      {/* 错误行（只在有错误时显示） */}
      {errors.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {errors.slice(-2).map((err, index) => (
            <Alert
              key={index}
              type="error"
              message={
                <Text ellipsis style={{ maxWidth: 400, fontSize: 12 }}>
                  {err.docName}: {err.error}
                </Text>
              }
              style={{ padding: '3px 10px', borderRadius: 6 }}
              showIcon
            />
          ))}
        </div>
      )}
    </div>
  );
}
