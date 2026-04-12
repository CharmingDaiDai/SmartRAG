export interface DocumentPreviewMeta {
  documentId: number;
  kbId: number;
  filename: string;
  fileType?: string;
  fileSize?: number;
  indexStatus?: string;
  indexStrategyType?: string;
  previewType: 'RAW' | 'MARKDOWN' | 'TEXT' | 'UNSUPPORTED';
  supportsRawPreview: boolean;
  supportsTextPreview: boolean;
}

export interface DocumentPreviewTextPage {
  documentId: number;
  page: number;
  size: number;
  totalChars: number;
  totalSegments: number;
  hasMore: boolean;
  segments: string[];
}
