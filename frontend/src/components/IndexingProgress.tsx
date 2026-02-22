import { Progress, Typography, Tag, Alert, theme } from 'antd';
import { useEffect, useState, useRef } from 'react';
import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, FileTextOutlined } from '@ant-design/icons';
import request from '../services/api';

const { Text } = Typography;

interface IndexingProgressProps {
  kbId: string;
  onComplete?: () => void;
}

interface ProgressState {
  taskId: number;
  status: string;
  total: number;
  completed: number;
  failed: number;
  percentage: number;
}

interface StepState {
  docId: number;
  docName: string;
  step: string;
  stepName: string;
}

const STEP_COLORS: Record<string, string> = {
  READING: 'geekblue',
  PARSING: 'blue',
  CHUNKING: 'cyan',
  TREE_BUILDING: 'lime',
  LLM_ENRICHING: 'gold',
  SAVING: 'volcano',
  EMBEDDING: 'purple',
  STORING: 'orange',
};

const POLL_INTERVAL_MS = 1500;

export default function IndexingProgress({ kbId, onComplete }: IndexingProgressProps) {
  const { token } = theme.useToken();
  const [progress, setProgress] = useState<ProgressState | null>(null);
  const [currentStep, setCurrentStep] = useState<StepState | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isDone, setIsDone] = useState(false);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const isDoneRef = useRef(false);

  useEffect(() => {
    isDoneRef.current = false;

    const fetchProgress = async () => {
      // 任务已结束后停止轮询（双重保护）
      if (isDoneRef.current) return;

      try {
        const res = await request.get(`/documents/index-progress?kbId=${kbId}`);
        // res 已经是 response.data（拦截器处理过）
        const task = (res as { data: Record<string, unknown> | null }).data;

        if (!task) return;

        setProgress({
          taskId: task.taskId as number,
          status: task.status as string,
          total: task.totalDocs as number,
          completed: task.completedDocs as number,
          failed: task.failedDocs as number,
          percentage: task.percentage as number,
        });

        if (task.currentDocId) {
          setCurrentStep({
            docId: task.currentDocId as number,
            docName: task.currentDocName as string,
            step: task.currentStep as string,
            stepName: task.currentStepName as string,
          });
        }

        if (task.errorMessage) {
          setErrorMessage(task.errorMessage as string);
        }

        const status = task.status as string;
        if (status === 'COMPLETED' || status === 'FAILED') {
          isDoneRef.current = true;
          if (pollingRef.current) clearInterval(pollingRef.current);
          setIsDone(true);
          setCurrentStep(null);
          setTimeout(() => onComplete?.(), 1500);
        }
      } catch {
        // 网络错误时继续轮询，不中断
      }
    };

    // 立即执行一次（刷新后立即恢复状态）
    fetchProgress();

    pollingRef.current = setInterval(fetchProgress, POLL_INTERVAL_MS);

    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, [kbId, onComplete]);

  const isSuccess = isDone && (!progress || progress.failed === 0);

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

      {/* 错误信息（任务级别，如配置错误等） */}
      {errorMessage && isDone && !isSuccess && (
        <Alert
          type="error"
          message={
            <Text ellipsis style={{ maxWidth: 500, fontSize: 12 }}>
              {errorMessage}
            </Text>
          }
          style={{ padding: '3px 10px', borderRadius: 6 }}
          showIcon
        />
      )}
    </div>
  );
}
