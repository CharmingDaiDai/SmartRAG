package com.mtmn.smartrag.service;

/**
 * 文件内容读取服务，负责从不同类型的文件中提取文本内容
 *
 * @author charmingdaidai
 */
public interface FileService {

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return 文件内容
     */
    String readFileContent(String filePath, String fileType);
}