package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.enums.IndexingStep;

/**
 * 索引进度回调接口
 *
 * @author charmingdaidai
 * @version 2.0
 */
public interface IndexingProgressCallback {

    /**
     * 当前步骤变更
     *
     * @param documentId   文档 ID
     * @param documentName 文档名称
     * @param step         当前步骤
     */
    void onStepChanged(Long documentId, String documentName, IndexingStep step);

    /**
     * 单个文档完成
     *
     * @param documentId   文档 ID
     * @param documentName 文档名称
     */
    void onDocumentCompleted(Long documentId, String documentName);

    /**
     * 单个文档失败
     *
     * @param documentId   文档 ID
     * @param documentName 文档名称
     * @param error        错误信息
     */
    void onDocumentFailed(Long documentId, String documentName, String error);

    /**
     * 空实现，用于不需要回调的场景
     */
    IndexingProgressCallback NOOP = new IndexingProgressCallback() {
        @Override
        public void onStepChanged(Long documentId, String documentName, IndexingStep step) {
        }

        @Override
        public void onDocumentCompleted(Long documentId, String documentName) {
        }

        @Override
        public void onDocumentFailed(Long documentId, String documentName, String error) {
        }
    };
}
