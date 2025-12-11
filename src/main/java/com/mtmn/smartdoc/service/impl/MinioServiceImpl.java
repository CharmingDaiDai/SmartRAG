package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.common.CustomException;
import com.mtmn.smartdoc.service.MinioService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author charmingdaidai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.files}")
    private String bucketName;

    // 【优化1】在 Bean 初始化时检查一次 Bucket 即可，不需要每次上传都检查，提高性能
    @PostConstruct
    public void init() {
        // 尝试初始化 Bucket，带重试机制
        int maxRetries = 3;
        int retryDelayMs = 2000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("MinIO Bucket created: {}", bucketName);
                } else {
                    log.info("MinIO Bucket already exists: {}", bucketName);
                }
                return; // 初始化成功，直接返回
            } catch (Exception e) {
                log.warn("Failed to initialize MinIO bucket (Attempt {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("MinIO bucket initialization failed after {} attempts. File upload features may be unavailable.", maxRetries);
        // 不抛出异常，允许应用继续启动，但在使用时可能会报错
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, file.getOriginalFilename());
    }

    @Override
    public String uploadFile(MultipartFile file, String assignedFileName) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new CustomException(400, "上传文件不能为空");
        }

        try {
            // 2. 确定文件名
            // 优先使用传入的 assignedFileName，如果为空则使用原始文件名
            String finalFileName = StringUtils.isNotBlank(assignedFileName)
                    ? assignedFileName
                    : file.getOriginalFilename();

            // 3. 安全清理：只保留文件名，去除路径（防止 ../../etc/passwd 这种目录遍历攻击）
            // 这一步非常关键，无论 filename 是前端传的还是业务传的，都要清洗
            String safeFileName = FilenameUtils.getName(finalFileName);

            // 4. 生成存储路径
            String objectName = generateObjectPath(safeFileName);

            // 5. 执行上传
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            log.info("Upload success: {} (Original: {})", objectName, safeFileName);
            return objectName;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new CustomException(500, "上传文件失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadFile(InputStream stream, String filename, String contentType) {
        try {
            String objectName = generateObjectPath(filename);

            // TODO 生产环境建议明确流大小，或者使用 putObject 的 partSize 参数
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            // 10MB part size
                            .stream(stream, -1, 10485760)
                            .contentType(contentType)
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            throw new CustomException(500, "流上传失败");
        }
    }

    @Override
    public String uploadFileFromUrl(String urlStr) {
        return uploadFileFromUrl(urlStr, null);
    }

    @Override
    public String uploadFileFromUrl(String urlStr, String assignedFileName) {
        if (StringUtils.isBlank(urlStr)) {
            throw new CustomException(400, "URL不能为空");
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");

            // 添加 User-Agent 防止某些服务器拒绝 Java 客户端请求
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new CustomException(500, "远程文件下载失败，状态码: " + responseCode);
            }

            // 1. 确定文件名
            String finalFileName = assignedFileName;
            if (StringUtils.isBlank(finalFileName)) {
                // 如果没传文件名，尝试从 URL 路径获取
                finalFileName = FilenameUtils.getName(url.getPath());
                // 如果 URL 没后缀 (如 http://example.com/image)，生成 UUID
                if (StringUtils.isBlank(finalFileName)) {
                    finalFileName = UUID.randomUUID().toString();
                }
            }

            // 2. 获取 Content-Type
            String contentType = connection.getContentType();
            if (StringUtils.isBlank(contentType)) {
                // 如果网络流没返回类型，尝试根据文件名推断
                contentType = "application/octet-stream";
                // 可选：这里可以使用 Files.probeContentType 或 Tika 根据 finalFileName 猜测类型
            }

            // 3. 直接流式上传
            try (InputStream inputStream = connection.getInputStream()) {
                // 调用底层的流上传方法
                return uploadFile(inputStream, finalFileName, contentType);
            }

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Download from URL failed: {} filename: {}", urlStr, assignedFileName, e);
            throw new CustomException(500, "远程文件抓取失败: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void deleteFile(String filePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Delete failed: {}", filePath, e);
            throw new CustomException(500, "删除文件失败");
        }
    }

    @Override
    public InputStream getFileStream(String filePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Get file stream failed: {}", filePath, e);
            throw new CustomException(500, "获取文件流失败");
        }
    }

    @Override
    // 【新增】MinIO原生支持直接下载到本地文件，效率更高，不需要手动 copy stream
    public void downloadFileToLocal(String filePath, String localPath) {
        try {
            minioClient.downloadObject(
                    DownloadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .filename(localPath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Download to local failed: {}", filePath, e);
            throw new CustomException(500, "下载文件到本地失败");
        }
    }

    @Override
    // 【新增】Web开发中最常用的方法，直接写入 Response
    public void downloadToResponse(String filePath, HttpServletResponse response) {
        try (InputStream inputStream = getFileStream(filePath);
             OutputStream outputStream = response.getOutputStream()) {

            // 获取文件元数据，设置 Content-Type
            StatObjectResponse stat = getFileMetadata(filePath);
            response.setContentType(stat.contentType());

            // 设置下载文件名 (解决中文乱码)
            String fileName = FilenameUtils.getName(filePath);
            // 如果路径中有UUID前缀，可以去掉前缀只保留原始文件名（取决于你的业务需求）
            String originalName = fileName.contains("-") ? fileName.substring(fileName.indexOf("-") + 1) : fileName;

            String encodedFilename = URLEncoder.encode(originalName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
            response.setHeader("Content-Length", String.valueOf(stat.size()));

            // 流拷贝
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("Download to response failed", e);
            throw new CustomException(500, "文件下载失败");
        }
    }

    @Override
    public String getPresignedUrl(String filePath, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .method(Method.GET)
                            .expiry(expirySeconds > 0 ? expirySeconds : 7 * 24 * 3600, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new CustomException(500, "获取签名URL失败");
        }
    }

    @Override
    // 【新增】检查文件是否存在
    public boolean fileExists(String filePath) {
        try {
            getFileMetadata(filePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    // 【新增】获取元数据
    public StatObjectResponse getFileMetadata(String filePath) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .build()
            );
        } catch (Exception e) {
            throw new CustomException(404, "文件不存在或无法获取: " + filePath);
        }
    }

    // 工具方法：生成路径
    private String generateObjectPath(String originalFilename) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s-%s", datePath, uuid, originalFilename);
    }
}