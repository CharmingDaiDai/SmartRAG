import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, Button, Divider, Drawer, Empty, List, Space, Spin, Tag, Typography } from 'antd';
import { FileImageOutlined, FilePdfOutlined, FileTextOutlined, ReloadOutlined } from '@ant-design/icons';
import { CodeHighlighter, Mermaid } from '@ant-design/x';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import Latex from '@ant-design/x-markdown/plugins/Latex';
import { documentService } from '../services/documentService';
import { DocumentItem } from '../types';
import { DocumentPreviewMeta, DocumentPreviewTextPage } from '../types/preview';

interface DocumentViewerProps {
  open: boolean;
  onClose: () => void;
  document: DocumentItem | null;
}

const PAGE_SIZE = 4;

interface RawImageSize {
  width: number;
  height: number;
}

const Code: React.FC<ComponentProps> = (props) => {
  const { className, children } = props;
  const lang = className?.match(/language-(\w+)/)?.[1] || '';
  if (typeof children !== 'string') return null;
  if (lang === 'mermaid') {
    return <Mermaid>{children}</Mermaid>;
  }
  return <CodeHighlighter lang={lang}>{children}</CodeHighlighter>;
};

const MD_CONFIG = {
  extensions: Latex({
    katexOptions: {
      output: 'html' as const,
      throwOnError: false,
    },
  }),
};

const MD_COMPONENTS = { code: Code };

const DocumentViewer: React.FC<DocumentViewerProps> = ({ open, onClose, document }) => {
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [errorText, setErrorText] = useState('');
  const [meta, setMeta] = useState<DocumentPreviewMeta | null>(null);
  const [textSegments, setTextSegments] = useState<string[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [rawUrl, setRawUrl] = useState<string | null>(null);
  const [rawImageSize, setRawImageSize] = useState<RawImageSize | null>(null);

  const requestIdRef = useRef(0);
  const rawUrlRef = useRef<string | null>(null);

  const clearRawUrl = useCallback(() => {
    if (rawUrlRef.current) {
      URL.revokeObjectURL(rawUrlRef.current);
      rawUrlRef.current = null;
    }
    setRawUrl(null);
  }, []);

  const replaceRawUrl = useCallback((nextUrl: string | null) => {
    if (rawUrlRef.current) {
      URL.revokeObjectURL(rawUrlRef.current);
    }
    rawUrlRef.current = nextUrl;
    setRawUrl(nextUrl);
  }, []);

  const readImageNaturalSize = useCallback(async (blob: Blob): Promise<RawImageSize | null> => {
    if (!blob.type.toLowerCase().startsWith('image/')) {
      return null;
    }

    if (typeof createImageBitmap === 'function') {
      try {
        const bitmap = await createImageBitmap(blob);
        const size = { width: bitmap.width, height: bitmap.height };
        bitmap.close();
        return size;
      } catch {
        // Fall through to Image loading for broader browser compatibility.
      }
    }

    return new Promise<RawImageSize | null>((resolve) => {
      const tmpUrl = URL.createObjectURL(blob);
      const img = new Image();

      img.onload = () => {
        const width = img.naturalWidth;
        const height = img.naturalHeight;
        URL.revokeObjectURL(tmpUrl);
        resolve(width > 0 && height > 0 ? { width, height } : null);
      };

      img.onerror = () => {
        URL.revokeObjectURL(tmpUrl);
        resolve(null);
      };

      img.src = tmpUrl;
    });
  }, []);

  const resetState = useCallback(() => {
    setErrorText('');
    setMeta(null);
    setTextSegments([]);
    setCurrentPage(0);
    setHasMore(false);
    setRawImageSize(null);
    clearRawUrl();
  }, [clearRawUrl]);

  const loadTextPage = useCallback(async (docId: string, requestId: number, page: number, append: boolean) => {
    const res: any = await documentService.previewText(docId, page, PAGE_SIZE);
    if (requestId !== requestIdRef.current) {
      return;
    }

    if (res.code !== 200 || !res.data) {
      throw new Error(res.message || '加载文本预览失败');
    }

    const pageData = res.data as DocumentPreviewTextPage;
    setCurrentPage(pageData.page);
    setHasMore(pageData.hasMore);
    setTextSegments(prev => append ? [...prev, ...pageData.segments] : pageData.segments);
  }, []);

  const loadPreview = useCallback(async (docId: string) => {
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setErrorText('');
    setMeta(null);
    setTextSegments([]);
    setCurrentPage(0);
    setHasMore(false);
    setRawImageSize(null);
    clearRawUrl();

    try {
      const metaRes: any = await documentService.getPreviewMeta(docId);
      if (requestId !== requestIdRef.current) {
        return;
      }

      if (metaRes.code !== 200 || !metaRes.data) {
        setErrorText(metaRes.message || '加载预览信息失败');
        return;
      }

      const metaData = metaRes.data as DocumentPreviewMeta;
      setMeta(metaData);

      if (metaData.supportsRawPreview) {
        const blob: Blob = await documentService.previewRawBlob(docId);
        if (requestId !== requestIdRef.current) {
          return;
        }

        if (blob.type.toLowerCase().startsWith('image/')) {
          const imageSize = await readImageNaturalSize(blob);
          if (requestId !== requestIdRef.current) {
            return;
          }
          setRawImageSize(imageSize);
        }

        const blobUrl = URL.createObjectURL(blob);
        replaceRawUrl(blobUrl);
        return;
      }

      if (metaData.supportsTextPreview) {
        await loadTextPage(docId, requestId, 0, false);
        return;
      }

      setErrorText('当前文件类型暂不支持预览');
    } catch (e: any) {
      if (requestId === requestIdRef.current) {
        setErrorText(e?.message || '文档预览加载失败');
      }
    } finally {
      if (requestId === requestIdRef.current) {
        setLoading(false);
      }
    }
  }, [clearRawUrl, loadTextPage, readImageNaturalSize, replaceRawUrl]);

  useEffect(() => {
    if (!open || !document) {
      requestIdRef.current += 1;
      resetState();
      return;
    }

    void loadPreview(document.id);

    return () => {
      requestIdRef.current += 1;
    };
  }, [document, loadPreview, open, resetState]);

  useEffect(() => {
    return () => {
      requestIdRef.current += 1;
      clearRawUrl();
    };
  }, [clearRawUrl]);

  const handleLoadMore = useCallback(async () => {
    if (!document || !hasMore || loadingMore || !meta?.supportsTextPreview) {
      return;
    }

    const requestId = requestIdRef.current;
    setLoadingMore(true);
    try {
      await loadTextPage(document.id, requestId, currentPage + 1, true);
    } catch (e: any) {
      setErrorText(e?.message || '加载更多内容失败');
    } finally {
      setLoadingMore(false);
    }
  }, [currentPage, document, hasMore, loadTextPage, loadingMore, meta?.supportsTextPreview]);

  const previewTypeLabel = useMemo(() => {
    if (!meta) return '预览';
    if (meta.previewType === 'RAW') return '原样预览';
    if (meta.previewType === 'MARKDOWN') return 'Markdown 预览';
    if (meta.previewType === 'TEXT') return '文本预览';
    return '不支持预览';
  }, [meta]);

  const mergedText = useMemo(() => textSegments.join('\n\n'), [textSegments]);
  const isRawImage = useMemo(() => {
    if (!meta?.fileType) return false;
    return meta.fileType.toLowerCase().startsWith('image/');
  }, [meta?.fileType]);

  const renderBody = () => {
    if (loading) {
      return (
        <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 96 }}>
          <Spin size="large" />
        </div>
      );
    }

    if (errorText) {
      return <Alert type="error" showIcon message={errorText} />;
    }

    if (!meta) {
      return <Empty description="暂无可预览内容" />;
    }

    if (meta.supportsRawPreview) {
      if (!rawUrl) {
        return <Spin />;
      }

      if (isRawImage) {
        return (
          <div style={{ textAlign: 'center', minHeight: 240, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <img
              src={rawUrl}
              alt={document?.filename || 'document-image'}
              width={rawImageSize?.width}
              height={rawImageSize?.height}
              decoding="async"
              style={{ maxWidth: '100%', maxHeight: '72vh', width: 'auto', height: 'auto', objectFit: 'contain' }}
            />
          </div>
        );
      }

      return (
        <iframe
          title="document-raw-preview"
          src={rawUrl}
          style={{ width: '100%', height: '72vh', border: '1px solid #f0f0f0', borderRadius: 8 }}
        />
      );
    }

    if (meta.supportsTextPreview) {
      return (
        <div>
          {textSegments.length === 0 ? (
            <Empty description="暂无文本内容" />
          ) : meta.previewType === 'MARKDOWN' ? (
            <div style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: '12px 16px' }}>
              <XMarkdown config={MD_CONFIG} components={MD_COMPONENTS}>
                {mergedText}
              </XMarkdown>
            </div>
          ) : (
            <List
              dataSource={textSegments}
              split
              renderItem={(segment, idx) => (
                <List.Item key={`${idx}-${segment.length}`}>
                  <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap', width: '100%' }}>
                    {segment}
                  </Typography.Paragraph>
                </List.Item>
              )}
            />
          )}

          {hasMore && (
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Button loading={loadingMore} onClick={handleLoadMore}>
                加载更多
              </Button>
            </div>
          )}
        </div>
      );
    }

    return <Empty description="当前文件类型暂不支持预览" />;
  };

  return (
    <Drawer
      title={
        <Space wrap>
          <Typography.Text strong>{document?.filename || '文档预览'}</Typography.Text>
          <Tag color="blue">{previewTypeLabel}</Tag>
          {meta?.fileType && <Tag>{meta.fileType}</Tag>}
        </Space>
      }
      placement="right"
      width={920}
      open={open}
      onClose={onClose}
      destroyOnClose
      styles={{ body: { padding: 16 } }}
      extra={
        document ? (
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              void loadPreview(document.id);
            }}
          >
            刷新
          </Button>
        ) : undefined
      }
    >
      <Space size={8} style={{ marginBottom: 8 }}>
        {meta?.supportsRawPreview && <Tag icon={<FilePdfOutlined />}>原样流式</Tag>}
        {meta?.supportsTextPreview && <Tag icon={<FileTextOutlined />}>文本分页</Tag>}
        {isRawImage && <Tag icon={<FileImageOutlined />}>图片模式</Tag>}
      </Space>
      <Divider style={{ margin: '8px 0 16px' }} />
      {renderBody()}
    </Drawer>
  );
};

export default DocumentViewer;
