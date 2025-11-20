//package com.mtmn.smartdoc.service.impl;
//
//import com.mtmn.smartdoc.common.ApacheTikaDocumentParser;
//import com.mtmn.smartdoc.service.FileService;
//import com.mtmn.smartdoc.service.MinioService;
//import dev.langchain4j.data.document.Document;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.io.InputStream;
//
/// **
// * 文件内容读取服务实现类
// *
// * @author charmingdaidai
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class FileServiceImpl implements FileService {
//
//    private final MinioService minioService;
//
//    /**
//     * 读取文件内容并转换为文本
//     *
//     * 实现思路：
//     * 1. 使用Apache Tika文档解析器，支持多种文件格式
//     * 2. 通过MinIO服务获取文件的输入流
//     * 3. 验证文件类型参数的有效性
//     * 4. 使用Apache Tika解析文件内容，自动处理不同格式
//     * 5. 提取文档中的纯文本内容
//     * 6. 使用try-with-resources确保输入流正确关闭
//     * 7. 捕获并处理IO异常，返回错误信息而不是抛出异常
//     *
//     * @param filePath 文件在存储服务中的路径
//     * @param fileType 文件类型（如pdf、docx、txt等）
//     * @return 文件的文本内容，如果解析失败则返回错误信息
//     */
//    @Override
//    public String readFileContent(String filePath, String fileType) {
//        ApacheTikaDocumentParser documentParser = new ApacheTikaDocumentParser();
//
//        try (InputStream inputStream = minioService.getFileContent(filePath)) {
//            if (fileType == null) {
//                log.warn("文件类型未指定，无法解析文件内容：{}", filePath);
//                return "";
//            }
//
//            Document document = documentParser.parse(inputStream);
//            return document.text();
//
//        } catch (IOException e) {
//            log.error("读取文件内容出错：{}", e.getMessage(), e);
//            return "读取文件内容出错：" + e.getMessage();
//        }
//    }
//}