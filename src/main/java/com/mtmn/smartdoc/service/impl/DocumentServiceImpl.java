package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.dto.DocumentResponse;
import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.exception.UnauthorizedAccessException;
import com.mtmn.smartdoc.po.Document;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.service.DocumentService;
import com.mtmn.smartdoc.service.IndexingService;
import com.mtmn.smartdoc.service.KnowledgeBaseService;
import com.mtmn.smartdoc.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档服务实现
 *
 * @author charmingdaidai
 * @version 2.1
 * @date 2025-11-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MinioService minioService;
    private final IndexingService indexingService;
    private final ObjectMapper objectMapper;
    // 【新增】引入事务模板，用于精细控制事务范围，替代方法级别的 @Transactional
    private final TransactionTemplate transactionTemplate;

    @Override
    // 【修复】移除 @Transactional，防止 MinIO 网络 IO 阻塞数据库连接
    public DocumentResponse uploadDocument(Long kbId, Long userId, MultipartFile file, String title) {
        log.info("Uploading document to KB: {}, User: {}, File: {}", kbId, userId, file.getOriginalFilename());

        // 1. 权限校验（读操作，无事务）
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);

        // 2. 文件上传 MinIO（IO 操作，耗时，必须在事务外进行）
        // TODO【安全建议】此处建议校验 fileType 是否在白名单内，防止上传恶意脚本
        String filePath = minioService.uploadFile(file);

        // 3. 构建实体对象
        Document document = buildDocumentEntity(kbId, userId, file, title, filePath);

        try {
            // 4. 数据库操作（开启事务，速度快）
            Document savedDoc = transactionTemplate.execute(status -> documentRepository.save(document));

            log.info("Document uploaded and saved successfully: id={}", savedDoc.getId());
            return convertToResponse(savedDoc);

        } catch (Exception e) {
            // 5. 【一致性补偿】如果 DB 保存失败，需要删除刚才上传到 MinIO 的文件，防止产生垃圾文件
            log.error("Database save failed for file: {}. Rolling back MinIO upload.", FilenameUtils.getName(file.getOriginalFilename()), e);
            try {
                minioService.deleteFile(filePath);
            } catch (Exception deleteEx) {
                log.error("Failed to rollback MinIO file: {}", filePath, deleteEx);
            }
            // 重新抛出异常给上层处理
            throw e;
        }
    }

    @Override
    // 【修复】移除 @Transactional，避免批量上传时长时间占用连接
    public List<DocumentResponse> uploadDocuments(Long kbId, Long userId, MultipartFile[] files, String[] titles) {
        log.info("Batch uploading {} documents to knowledge base {}", files.length, kbId);

        // 1. 统一权限校验
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);

        List<Document> documentsToSave = new ArrayList<>();
        // 用于记录上传成功的路径，以便在 DB 失败时回滚
        List<String> uploadedFilePaths = new ArrayList<>();

        // 2. 循环执行 IO 操作（无事务）
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String title = (titles != null && i < titles.length) ? titles[i] : null;
            try {
                String filePath = minioService.uploadFile(file);
                // 记录路径
                uploadedFilePaths.add(filePath);

                documentsToSave.add(buildDocumentEntity(kbId, userId, file, title, filePath));
            } catch (Exception e) {
                log.error("Failed to upload file to MinIO: {}", FilenameUtils.getName(file.getOriginalFilename()), e);
                // 策略选择：这里选择跳过失败的文件，继续处理下一个。
                // 如果业务要求“全成功或全失败”，则应在这里抛出异常并在 catch 中清理 uploadedFilePaths
            }
        }

        if (documentsToSave.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 3. 批量数据库插入（一次性事务）
            // 【性能优化】使用 saveAll 替代循环 save，减少数据库交互次数
            List<Document> savedDocs = transactionTemplate
                    .execute(status -> documentRepository.saveAll(documentsToSave));

            if (savedDocs != null) {
                return savedDocs.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            // 4. 【一致性补偿】DB 批量保存失败，删除所有本次已上传的文件
            log.error("Batch database save failed. Rolling back {} files.", uploadedFilePaths.size(), e);
            for (String path : uploadedFilePaths) {
                try {
                    minioService.deleteFile(path);
                } catch (Exception ignored) {
                }
            }
            throw e;
        }

        return List.of();
    }

    /**
     * 辅助方法：构建文档实体
     */
    private Document buildDocumentEntity(Long kbId, Long userId, MultipartFile file, String title, String filePath) {
        String filename = file.getOriginalFilename();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalFilename", filename);
        metadata.put("uploadTime", LocalDateTime.now().toString());

        String metadataJson = "{}";
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Metadata serialization failed", e);
        }

        return Document.builder()
                .kbId(kbId)
                .userId(userId)
                .filename(title != null ? title : filename)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .indexStatus(DocumentIndexStatus.UPLOADED)
                .metadata(metadataJson)
                .uploadTime(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(Long kbId, Long userId) {
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);
        return documentRepository.findByKbId(kbId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId, Long userId) {
        Document document = getDocumentEntity(documentId, userId);
        return convertToResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Document getDocumentEntity(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        // 权限校验优化：只要校验 User 对不对即可，KB 的权限隐含在 Document 的归属中
        if (!document.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(userId, "Document", documentId);
        }
        return document;
    }

    @Override
    // 【调整】删除操作可以保留事务，但建议先删 DB 后删文件
    public void deleteDocument(Long documentId, Long userId) {
        log.info("Deleting document: {}", documentId);

        // 1. 查询并验证权限
        Document document = getDocumentEntity(documentId, userId);
        String filePath = document.getFilePath();

        // 2. 数据库删除（事务内）
        // 关联数据的删除（如 Index、Chunks）应通过数据库级联删除或在 Service 内部逻辑处理
        transactionTemplate.execute(status -> {
            documentRepository.delete(document);
            return null;
        });

        // 3. MinIO 删除（事务提交后执行）
        // TODO: 改为异步删除
        // 即使 MinIO 删除失败，DB 记录已删，不会影响业务逻辑，只会产生孤儿文件（可通过定时任务清理）
        try {
            minioService.deleteFile(filePath);
        } catch (Exception e) {
            log.error("Document record deleted but failed to delete file from MinIO: {}", filePath, e);
        }

        // TODO: 异步触发向量库删除
    }

    @Override
    public void deleteDocuments(List<Long> documentIds, Long userId) {
        log.info("Batch deleting {} documents", documentIds.size());

        // 1. 批量查询
        List<Document> documents = documentRepository.findAllById(documentIds);
        if (documents.isEmpty()) {
            return;
        }

        // 2. 批量验证权限
        for (Document document : documents) {
            if (!document.getUserId().equals(userId)) {
                throw new UnauthorizedAccessException(userId, "Document", document.getId());
            }
        }

        // 3. 收集文件路径用于后续删除
        List<String> filePaths = documents.stream()
                .map(Document::getFilePath)
                .toList();

        // 4. 批量数据库删除（一次性事务）
        // 【性能优化】使用 deleteAllInBatch 生成单条 DELETE ... WHERE id IN (...) 语句
        transactionTemplate.execute(status -> {
            documentRepository.deleteAllInBatch(documents);
            return null;
        });

        // 5. 循环删除 MinIO 文件（非事务）
        // TODO: 改为异步删除
        for (String path : filePaths) {
            try {
                minioService.deleteFile(path);
            } catch (Exception e) {
                log.error("Failed to delete file from MinIO: {}", path, e);
            }
        }

        // TODO: 异步触发向量库删除
    }

    @Override
    public void triggerIndexing(Long documentId, Long userId) {
        // 复用 getDocumentEntity 进行权限校验
        Document document = getDocumentEntity(documentId, userId);
        indexingService.submitIndexingTask(document.getId(), document.getKbId());
    }

    @Override
    public void triggerBatchIndexing(Long kbId, Long userId) {
        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);

        List<Document> documents = documentRepository.findByKbIdAndIndexStatus(
                kbId, DocumentIndexStatus.UPLOADED);

        if (!documents.isEmpty()) {
            log.info("Submitting batch indexing task for {} documents", documents.size());
            indexingService.submitBatchIndexingTask(kbId,
                    documents.stream().map(Document::getId).collect(Collectors.toList()));
        }
    }

    private DocumentResponse convertToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .kbId(document.getKbId())
                .filename(document.getFilename())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .fileType(document.getFileType())
                .indexStatus(document.getIndexStatus())
                .metadata(document.getMetadata())
                .uploadTime(document.getUploadTime())
                .build();
    }
}