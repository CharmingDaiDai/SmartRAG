//package com.mtmn.smartrag.controller;
//
//import com.mtmn.smartrag.common.ApiResponse;
//import com.mtmn.smartrag.po.Document;
//import com.mtmn.smartrag.po.User;
//import com.mtmn.smartrag.repository.DocumentRepository;
//import com.mtmn.smartrag.service.AnalysisService;
//import com.mtmn.smartrag.service.UserActivityService;
//import com.mtmn.smartrag.vo.KeywordsResult;
//import com.mtmn.smartrag.vo.PolishResult;
//import com.mtmn.smartrag.vo.SecurityResult;
//import com.mtmn.smartrag.vo.SummaryResult;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.Map;
//import java.util.Optional;
//
/// **
// * @author charmingdaidai
// */
//@RestController
//@RequestMapping("/api/analysis")
//@RequiredArgsConstructor
//@Slf4j
//public class AnalysisController {
//
//    private final AnalysisService analysisService;
//    private final UserActivityService userActivityService;
//    private final DocumentRepository documentRepository;
//
//    /**
//     * 生成文档摘要
//     *
//     * @param requestBody 包含文档内容或文档ID的请求体
//     * @param user 当前登录用户
//     * @return 摘要结果
//     */
//    @PostMapping("/summary")
//    public ApiResponse<SummaryResult> generateSummary(
//            @RequestBody Map<String, Object> requestBody,
//            @AuthenticationPrincipal User user) {
//
//        if (user == null) {
//            return ApiResponse.unauthorized("用户未登录");
//        }
//
//        if (requestBody.containsKey("documentId")) {
//            // 基于已上传文档生成摘要
//            Long documentId = Long.valueOf(requestBody.get("documentId").toString());
//            SummaryResult summary = analysisService.generateSummaryFromDocument(documentId);
//
//            // 获取文档信息并记录用户活动
//            Optional<Document> documentOpt = documentRepository.findById(documentId);
//            if (documentOpt.isPresent()) {
//                Document document = documentOpt.get();
//                userActivityService.recordSummaryAnalysis(user.getId(), documentId, document.getTitle());
//                log.info("用户 {} 生成了文档 {} 的摘要", user.getUsername(), document.getTitle());
//            }
//
//            return ApiResponse.success(summary);
//        } else {
//            // 基于提供的文本内容生成摘要
//            String content = (String) requestBody.get("content");
//            SummaryResult summary = analysisService.generateSummary(content);
//
//            // 记录用户活动 - 没有关联文档
//            userActivityService.recordSummaryAnalysis(user.getId(), null, "手动输入文本");
//            log.info("用户 {} 生成了手动输入文本的摘要", user.getUsername());
//
//            return ApiResponse.success(summary);
//        }
//    }
//
//    /**
//     * 提取文档关键词
//     *
//     * @param requestBody 包含文档内容或文档ID的请求体
//     * @param user 当前登录用户
//     * @return 关键词结果
//     */
//    @PostMapping("/keywords")
//    public ApiResponse<KeywordsResult> extractKeywords(
//            @RequestBody Map<String, Object> requestBody,
//            @AuthenticationPrincipal User user) {
//
//        if (user == null) {
//            return ApiResponse.unauthorized("用户未登录");
//        }
//
//        if (requestBody.containsKey("documentId")) {
//            // 基于已上传文档提取关键词
//            Long documentId = Long.valueOf(requestBody.get("documentId").toString());
//            KeywordsResult keywords = analysisService.extractKeywordsFromDocument(documentId);
//
//            // 获取文档信息并记录用户活动
//            Optional<Document> documentOpt = documentRepository.findById(documentId);
//            if (documentOpt.isPresent()) {
//                Document document = documentOpt.get();
//                userActivityService.recordKeywordsAnalysis(user.getId(), documentId, document.getTitle());
//                log.info("用户 {} 提取了文档 {} 的关键词", user.getUsername(), document.getTitle());
//            }
//
//            return ApiResponse.success(keywords);
//        } else {
//            // 基于提供的文本内容提取关键词
//            String content = (String) requestBody.get("content");
//            KeywordsResult keywords = analysisService.extractKeywords(content);
//
//            // 记录用户活动 - 没有关联文档
//            userActivityService.recordKeywordsAnalysis(user.getId(), null, "手动输入文本");
//            log.info("用户 {} 提取了手动输入文本的关键词", user.getUsername());
//
//            return ApiResponse.success(keywords);
//        }
//    }
//
//    /**
//     * 文档润色
//     *
//     * @param requestBody 包含文档内容或文档ID，以及润色类型的请求体
//     * @param user 当前登录用户
//     * @return 润色结果
//     */
//    @PostMapping("/polish")
//    public ApiResponse<PolishResult> polishDocument(
//            @RequestBody Map<String, Object> requestBody,
//            @AuthenticationPrincipal User user) {
//
//        if (user == null) {
//            return ApiResponse.unauthorized("用户未登录");
//        }
//
//        String polishType = (String) requestBody.getOrDefault("polishType", "formal");
//
//        if (requestBody.containsKey("documentId")) {
//            // 基于已上传文档进行润色
//            Long documentId = Long.valueOf(requestBody.get("documentId").toString());
//            PolishResult polished = analysisService.polishDocumentFromDocument(documentId, polishType);
//
//            // 获取文档信息并记录用户活动
//            Optional<Document> documentOpt = documentRepository.findById(documentId);
//            if (documentOpt.isPresent()) {
//                Document document = documentOpt.get();
//                userActivityService.recordPolishAnalysis(user.getId(), documentId, document.getTitle());
//                log.info("用户 {} 润色了文档 {}", user.getUsername(), document.getTitle());
//            }
//
//            return ApiResponse.success(polished);
//        } else {
//            // 基于提供的文本内容进行润色
//            String content = (String) requestBody.get("content");
//            PolishResult polished = analysisService.polishDocument(content, polishType);
//
//            // 记录用户活动 - 没有关联文档
//            userActivityService.recordPolishAnalysis(user.getId(), null, "手动输入文本");
//            log.info("用户 {} 润色了手动输入的文本", user.getUsername());
//
//            return ApiResponse.success(polished);
//        }
//    }
//
//    /**
//     * 敏感信息检测
//     *
//     * @param requestBody 包含文档内容或文档ID的请求体
//     * @param user 当前登录用户
//     * @return 敏感信息检测结果
//     */
//    @PostMapping("/security")
//    public ApiResponse<SecurityResult> detectSensitiveInfo(
//            @RequestBody Map<String, Object> requestBody,
//            @AuthenticationPrincipal User user) {
//
//        if (user == null) {
//            return ApiResponse.unauthorized("用户未登录");
//        }
//
//        if (requestBody.containsKey("documentId")) {
//            // 基于已上传文档检测敏感信息
//            Long documentId = Long.valueOf(requestBody.get("documentId").toString());
//            SecurityResult security = analysisService.detectSensitiveInfoFromDocument(documentId);
//
//            // 获取文档信息并记录用户活动
//            Optional<Document> documentOpt = documentRepository.findById(documentId);
//            if (documentOpt.isPresent()) {
//                Document document = documentOpt.get();
//                userActivityService.recordSecurityAnalysis(user.getId(), documentId, document.getTitle());
//                log.info("用户 {} 对文档 {} 进行了敏感信息检测", user.getUsername(), document.getTitle());
//            }
//
//            return ApiResponse.success(security);
//        } else {
//            // 基于提供的文本内容检测敏感信息
//            String content = (String) requestBody.get("content");
//            SecurityResult security = analysisService.detectSensitiveInfo(content);
//
//            // 记录用户活动 - 没有关联文档
//            userActivityService.recordSecurityAnalysis(user.getId(), null, "手动输入文本");
//            log.info("用户 {} 对手动输入文本进行了敏感信息检测", user.getUsername());
//
//            return ApiResponse.success(security);
//        }
//    }
//}