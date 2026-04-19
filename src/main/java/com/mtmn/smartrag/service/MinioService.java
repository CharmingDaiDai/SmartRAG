package com.mtmn.smartrag.service;

import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface MinioService {

    /**
     * 上传文件 (使用原始文件名)
     *
     * @param file 文件对象
     * @return MinIO中的文件路径
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传文件 (指定自定义文件名)
     *
     * @param file     文件对象
     * @param fileName 自定义文件名 (例如: "avatar.png")。如果传 null，则回退使用原始文件名
     * @return MinIO中的文件路径
     */
    String uploadFile(MultipartFile file, String fileName);

    /**
     * 上传文件 (通用流)
     */
    String uploadFile(InputStream stream, String filename, String contentType);

    /**
     * 从 URL 上传
     */
    String uploadFileFromUrl(String url);

    /**
     * 从 URL 上传 (指定文件名)
     *
     * @param url      远程文件地址
     * @param fileName 指定保存的文件名 (如: "test.png")，传 null 则自动从 URL 解析
     */
    String uploadFileFromUrl(String url, String fileName);

    /**
     * 删除文件
     */
    void deleteFile(String filePath);

    /**
     * 获取文件流 (调用者需手动关闭流)
     */
    InputStream getFileStream(String filePath);

    /**
     * 下载文件到本地
     */
    void downloadFileToLocal(String filePath, String localPath);

    /**
     * 下载文件并写入 HTTP 响应 (Web下载专用)
     */
    void downloadToResponse(String filePath, HttpServletResponse response);

    /**
     * 获取预览/下载链接
     *
     * @param expiry 过期时间(秒)，默认 7 天
     */
    String getPresignedUrl(String filePath, int expiry);

    /**
     * 检查文件是否存在
     */
    boolean fileExists(String filePath);

    /**
     * 获取文件元数据
     */
    StatObjectResponse getFileMetadata(String filePath);
}