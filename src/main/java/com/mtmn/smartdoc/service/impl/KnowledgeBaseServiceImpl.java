package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.common.IntentResult;
import com.mtmn.smartdoc.common.QueryDecomposeResult;
import com.mtmn.smartdoc.config.ModelConfig;
import com.mtmn.smartdoc.config.RagStrategyFactory;
import com.mtmn.smartdoc.dto.CreateKbRequest;
import com.mtmn.smartdoc.dto.KnowledgeBaseDTO;
import com.mtmn.smartdoc.po.DocumentPO;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.po.User;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.*;
import com.mtmn.smartdoc.utils.IntentClassifier;
import com.mtmn.smartdoc.utils.QueryDecompose;
import com.mtmn.smartdoc.utils.QueryRewrite;
import com.mtmn.smartdoc.utils.SseUtil;
import com.mtmn.smartdoc.vo.DocumentVO;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 知识库接口实现类
 * @date 2025/5/4 14:49
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {


    @Value("${milvus.uri}")
    private String milvusUri;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final ModelConfig modelConfig;
    private final ObjectMapper objectMapper;
    private final DocumentService documentService;
    //    private final MilvusService milvusService;
    private final MinioService minioService;
    private final SseUtil sseUtil;
    private final LLMService llmService;
    private final IntentClassifier intentClassifier;
    private final QueryRewrite queryRewrite;
    private final QueryDecompose queryDecompose;
    private final RagStrategyFactory ragStrategyFactory;
    private final HiSemRag hiSemRag;
    private final NaiveRag naiveRag;

    /**
     * 获取用户的知识库列表
     * 
     * 实现思路：
     * 1. 根据用户查询其所有知识库，按创建时间倒序排列
     * 2. 将知识库实体转换为DTO对象，避免暴露敏感信息
     * 3. 统一异常处理，返回标准化响应格式
     * 
     * @param user 当前登录用户
     * @return 包含知识库DTO列表的API响应对象
     */
    @Override
    public ApiResponse<List<KnowledgeBaseDTO>> listKnowledgeBase(User user) {
        log.info("获取用户知识库列表，用户：{}", user.getUsername());
        try {
            List<KnowledgeBase> knowledgeBases = knowledgeBaseRepository.findByUserOrderByCreatedAtDesc(user);
            // 转换为DTO返回，避免暴露敏感信息
            List<KnowledgeBaseDTO> knowledgeBaseDtos = KnowledgeBaseDTO.fromEntityList(knowledgeBases);
            return ApiResponse.success(knowledgeBaseDtos);
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return ApiResponse.error("获取知识库列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建知识库
     * 
     * 实现思路：
     * 1. 校验输入参数的有效性（名称不能为空）
     * 2. 检查同用户下是否已存在同名知识库，避免重复创建
     * 3. 构建知识库实体对象，包含用户指定的配置信息
     * 4. 保存到数据库并返回创建结果
     * 5. 使用事务确保数据一致性
     * 
     * @param createKbRequest 创建知识库的请求参数，包含名称、描述、嵌入模型等配置
     * @param user 当前登录用户
     * @return 创建结果的API响应对象，成功返回true，失败返回错误信息
     */
    @Override
    @Transactional
    public ApiResponse<Boolean> createKnowledgeBase(CreateKbRequest createKbRequest, User user) {
        log.info("创建知识库，用户：{}，知识库名称：{}", user.getUsername(), createKbRequest.getName());

        // 参数校验
        if (!StringUtils.hasText(createKbRequest.getName())) {
            return ApiResponse.error("知识库名称不能为空");
        }

        // 查询用户是否有已经存在的同名称知识库
        List<KnowledgeBase> existingKbs = knowledgeBaseRepository.findByUserOrderByCreatedAtDesc(user);
        boolean nameExists = existingKbs.stream()
                .anyMatch(kb -> kb.getName().equals(createKbRequest.getName().trim()));

        if (nameExists) {
            return ApiResponse.error("已存在同名知识库，请更换名称后重试");
        }

        try {
            // 创建知识库实体
            KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                    .name(createKbRequest.getName())
                    .description(createKbRequest.getDescription())
                    .embeddingModel(createKbRequest.getEmbeddingModel())
                    .rag(createKbRequest.getRagMethod())
                    .indexParam(createKbRequest.getIndexParam())
                    .user(user)
                    .build();

            // 保存到数据库
            knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

            log.info("知识库创建成功，ID：{}", knowledgeBase.getId());
            return ApiResponse.success("知识库创建成功", true);
        } catch (Exception e) {
            log.error("创建知识库异常", e);
            return ApiResponse.error("创建知识库失败：" + e.getMessage());
        }
    }

    /**
     * 删除知识库
     * 
     * 实现思路：
     * 1. 校验知识库ID的有效性
     * 2. 检查知识库是否存在
     * 3. 验证用户权限（只有知识库所有者才能删除）
     * 4. 删除知识库中的所有文档（包括Minio中的文件）
     * 5. 删除Milvus中的索引集合
     * 6. 删除数据库中的知识库记录
     * 7. 使用事务确保数据一致性
     * 
     * @param knowledgeBaseId 要删除的知识库ID
     * @param user 当前登录用户
     * @return 删除结果的API响应对象，成功返回true，失败返回错误信息
     */
    @Override
    @Transactional
    public ApiResponse<Boolean> deleteKnowledgeBase(Long knowledgeBaseId, User user) {
        log.info("删除知识库，ID：{}，用户：{}", knowledgeBaseId, user.getUsername());

        // 参数校验
        if (knowledgeBaseId == null) {
            return ApiResponse.error("知识库ID不能为空");
        }

        try {
            // 检查知识库是否存在
            Optional<KnowledgeBase> knowledgeBaseOpt = knowledgeBaseRepository.findById(knowledgeBaseId);
            if (knowledgeBaseOpt.isEmpty()) {
                return ApiResponse.error("知识库不存在");
            }

            KnowledgeBase knowledgeBase = knowledgeBaseOpt.get();

            // 检查权限（知识库所有者）
            if (!Objects.equals(knowledgeBase.getUser().getId(), user.getId())) {
                return ApiResponse.error("您没有权限删除此知识库");
            }

            // 删除知识库中的所有文档
            documentRepository.findAll().stream()
                    .filter(doc -> Objects.equals(doc.getKnowledgeBaseId(), knowledgeBaseId))
                    .forEach(doc -> {
                        // 删除 Minio 中的文档
                        minioService.deleteFile(doc.getFilePath());

                        // 删除文档表中的记录
                        documentRepository.delete(doc);

                        log.info("文档从知识库中移除，文档ID：{}", doc.getId());
                    });

            // TODO 改为 RAGMethodService.deleteIndex()
            // 删除知识库的索引
            ConnectConfig connectConfig = ConnectConfig.builder()
                    .uri(milvusUri)
                    .build();

            MilvusClientV2 milvusClient = new MilvusClientV2(connectConfig);
            milvusClient.dropCollection(DropCollectionReq.builder().collectionName(getStoreKnowledgeBaseName(knowledgeBase.getName())).build());
            log.info("删除知识库索引，ID：{}", knowledgeBaseId);

            // 删除知识库表中的记录
            knowledgeBaseRepository.delete(knowledgeBase);

            log.info("知识库删除成功，ID：{}", knowledgeBaseId);
            return ApiResponse.success("知识库删除成功", true);
        } catch (Exception e) {
            log.error("删除知识库异常", e);
            return ApiResponse.error("删除知识库失败：" + e.getMessage());
        }
    }

    /**
     * 获取可用的嵌入模型列表
     * 
     * 实现思路：
     * 1. 从模型配置中获取所有嵌入模型信息
     * 2. 将模型配置转换为前端需要的格式（label、description、value）
     * 3. 返回标准化的API响应
     * 
     * @return 包含嵌入模型列表的API响应对象，每个模型包含标签、描述和值
     */
    @Override
    public ApiResponse<List<Map<String, String>>> listEmbeddingModels() {
        log.info("获取嵌入模型列表");

        List<Map<String, String>> embeddingModels = modelConfig.getEmbedding().values()
                .stream()
                .map(mp -> {
                    Map<String, String> modelMap = new HashMap<>();
                    modelMap.put("label", mp.getModelName());
                    modelMap.put("description", mp.getDescription());
                    modelMap.put("value", mp.getModelName()); // 使用模型名称作为值
                    return modelMap;
                })
                .toList();

        return ApiResponse.success(embeddingModels);
    }

    /**
     * 获取知识库详情
     * 
     * 实现思路：
     * 1. 校验知识库ID参数的有效性
     * 2. 检查知识库是否存在于数据库中
     * 3. 验证用户权限（只有知识库所有者才能访问详情）
     * 4. 将知识库实体转换为DTO对象
     * 5. 解析索引参数JSON字符串为Map对象，便于前端展示
     * 6. 统一异常处理，确保服务的稳定性
     * 
     * @param id 知识库ID
     * @param user 当前登录用户
     * @return 包含知识库详情DTO的API响应对象
     */
    @Override
    public ApiResponse<KnowledgeBaseDTO> getKnowledgeBase(Long id, User user) {
        log.info("获取知识库详情，ID：{}，用户：{}", id, user.getUsername());

        // 参数校验
        if (id == null) {
            return ApiResponse.error("知识库ID不能为空");
        }

        try {
            // 检查知识库是否存在
            Optional<KnowledgeBase> knowledgeBaseOpt = knowledgeBaseRepository.findById(id);
            if (knowledgeBaseOpt.isEmpty()) {
                return ApiResponse.error("知识库不存在");
            }

            KnowledgeBase knowledgeBase = knowledgeBaseOpt.get();

            // 检查权限（知识库所有者）
            if (!Objects.equals(knowledgeBase.getUser().getId(), user.getId())) {
                return ApiResponse.error("您没有权限访问此知识库");
            }

            // 转换为DTO
            KnowledgeBaseDTO dto = KnowledgeBaseDTO.fromEntity(knowledgeBase);

            // 如果有索引参数，将JSON字符串转换为Map
            if (StringUtils.hasText(knowledgeBase.getIndexParam())) {
                try {
                    // 使用TypeReference处理泛型类型转换
                    Map<String, Object> ragParams = objectMapper.readValue(
                            knowledgeBase.getIndexParam(),
                            new TypeReference<Map<String, Object>>() {
                            }
                    );
                    dto.setRagParams(ragParams);
                } catch (Exception e) {
                    log.error("解析索引参数JSON失败", e);
                    // 解析失败时不设置ragParams，但不影响其他数据返回
                }
            }

            return ApiResponse.success(dto);
        } catch (Exception e) {
            log.error("获取知识库详情失败", e);
            return ApiResponse.error("获取知识库详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取知识库文档列表
     * 
     * 实现思路：
     * 1. 校验知识库ID参数的有效性
     * 2. 检查知识库是否存在于数据库中
     * 3. 验证用户权限（只有知识库所有者才能访问）
     * 4. 查询知识库下的所有文档，按创建时间倒序排列
     * 5. 将文档实体转换为VO对象，满足前端展示需求
     * 6. 统一异常处理，确保服务的稳定性
     * 
     * @param knowledgeBaseId 知识库ID
     * @param user 当前登录用户
     * @return 包含文档列表的API响应对象
     */
    @Override
    public ApiResponse<List<DocumentVO>> listKnowledgeBaseDocs(Long knowledgeBaseId, User user) {
        log.info("获取知识库文档列表，知识库ID：{}，用户：{}", knowledgeBaseId, user.getUsername());

        // 参数校验
        if (knowledgeBaseId == null) {
            return ApiResponse.error("知识库ID不能为空");
        }

        try {
            // 检查知识库是否存在
            Optional<KnowledgeBase> knowledgeBaseOpt = knowledgeBaseRepository.findById(knowledgeBaseId);
            if (knowledgeBaseOpt.isEmpty()) {
                return ApiResponse.error("知识库不存在");
            }

            KnowledgeBase knowledgeBase = knowledgeBaseOpt.get();

            // 检查权限（知识库所有者）
            if (!Objects.equals(knowledgeBase.getUser().getId(), user.getId())) {
                return ApiResponse.error("您没有权限访问此知识库的文档");
            }

            // 获取知识库的文档列表
            List<DocumentPO> documents = documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(knowledgeBaseId);

            // 将 Document 实体转换为 DocumentVO
            List<DocumentVO> documentVos = documents.stream()
                    .map(doc -> DocumentVO.builder()
                            .id(doc.getId())
                            .title(doc.getTitle())
                            .fileName(doc.getFileName())
                            .indexed(doc.getIndexed())
                            .fileType(doc.getFileType())
                            .fileSize(doc.getFileSize())
                            .fileUrl(doc.getFilePath())
                            .createdAt(doc.getCreatedAt())
                            .updatedAt(doc.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());

            log.info("成功获取知识库文档列表，知识库ID：{}，文档数量：{}", knowledgeBaseId, documentVos.size());
            return ApiResponse.success(documentVos);
        } catch (Exception e) {
            log.error("获取知识库文档列表失败", e);
            return ApiResponse.error("获取知识库文档列表失败：" + e.getMessage());
        }
    }

    /**
     * 批量向指定知识库添加文档
     *
     * 实现思路：
     * 1. 遍历传入的文件数组和标题数组
     * 2. 对每个非空文件调用documentService.uploadDocument进行上传
     * 3. 记录每个文档的上传结果（成功/失败）
     * 4. 返回包含所有上传结果的列表
     * 5. 异常处理确保批量操作的稳定性
     * 
     * @param id     知识库ID
     * @param user   当前登录用户
     * @param files  要上传的文件列表
     * @param titles 与 files 数组一一对应的文档标题列表
     * @return 每个布尔值表示对应文档的上传结果（true：成功，false：失败）
     */
    @Override
    public ApiResponse<List<Boolean>> addDocs(Long id, User user, MultipartFile[] files, String[] titles) {

        List<Boolean> uploaded = new ArrayList<>();
        try {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isEmpty()) {
                    DocumentPO document = documentService.uploadDocument(files[i], titles[i], user, id);

                    if (null != document) {
                        uploaded.add(true);

                        log.info("用户 {} 上传了文档 {}", user.getUsername(), titles[i]);
                    } else {
                        uploaded.add(false);

                        log.info("用户 {} 上传文档 {} 失败", user.getUsername(), titles[i]);
                    }

                }
            }
            return ApiResponse.success("文档批量上传成功", uploaded);
        } catch (Exception e) {
            log.error("Error batch uploading documents: {}", e.getMessage(), e);
            return ApiResponse.error("文档批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 构建知识库索引
     * 
     * 实现思路：
     * 1. 根据知识库ID查询知识库信息，获取嵌入模型和RAG方法配置
     * 2. 从文档库中查询该知识库下未被索引的文档
     * 3. 解析索引参数JSON，合并嵌入模型名称到参数中
     * 4. 使用RAG策略工厂创建对应的RAG策略对象
     * 5. 调用RAG策略的buildIndex方法构建索引
     * 6. 更新成功构建索引的文档状态为已索引
     * 7. 返回构建结果的响应
     * 
     * @param id 知识库ID字符串
     * @return 索引构建结果的API响应对象
     */
    @Override
    public ApiResponse<String> buildIndex(String id) {
        /*
        根据知识库 id 从知识库查询信息 embedding index
        根据知识库 id 从文档库中查询知识库的文档（没有被索引的）
        根据 embedding 模型和 rag 方法开始构建索引
        获取文档内容，文档切分，向量库构建
         */
        Optional<KnowledgeBase> knowledgeBaseOpt = knowledgeBaseRepository.findById(Long.valueOf(id));

        if (knowledgeBaseOpt.isEmpty()) {
            return ApiResponse.error("知识库不存在");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseOpt.get();

        String kbName = knowledgeBase.getName();

        String ragMethodName = knowledgeBase.getRag();

        String embeddingModelName = knowledgeBase.getEmbeddingModel();

        String indexParam = knowledgeBase.getIndexParam();

        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(indexParam)) {
            try {
                params.putAll(objectMapper.readValue(indexParam, new TypeReference<Map<String, Object>>() {
                }));
            } catch (Exception e) {
                log.error("解析索引参数JSON失败", e);
            }
        }

        try {
            // 使用 ragStrategyFactory 创建 RAG 策略对象
            BaseRag rag = ragStrategyFactory.getStrategy(ragMethodName);

            // 查询未被索引的文档
            List<DocumentPO> documentPoList = documentRepository.findByKnowledgeBaseIdOrderByCreatedAtDesc(Long.valueOf(id))
                    .stream()
                    .filter(doc -> !Boolean.TRUE.equals(doc.getIndexed()))
                    .toList();

            if (documentPoList.isEmpty()) {
                return ApiResponse.success("没有未被索引的文档");
            }

            //Integer chunkSize, Boolean generateAbstract, String embeddingModelName
            params.put("embeddingModelName", embeddingModelName);

            List<Boolean> success = rag.buildIndex(kbName, documentPoList, params);

            for (int i = 0; i < success.size(); i++) {
                // TODO 没成功的现在没有提示
                if (success.get(i)) {
                    // 更新文档的索引状态
                    DocumentPO documentPo = documentPoList.get(i);
                    documentPo.setIndexed(true);
                    documentRepository.save(documentPo);
                }
            }

            return ApiResponse.success("索引构建成功：");
        } catch (Exception e) {
            log.error("索引构建失败", e);
            return ApiResponse.error("索引构建失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户的ID
     *
     * 实现思路：
     * 1. 从Spring Security上下文中获取认证信息
     * 2. 检查认证状态和认证主体类型
     * 3. 根据不同的主体类型提取用户ID
     * 4. 统一异常处理，确保服务的稳定性
     *
     * @return 当前登录用户ID，获取失败时返回null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getId();
            } else if (principal instanceof UserDetails) {
                // 如果是自定义的UserDetails实现，可能需要强制转换为您的User类
                String username = ((UserDetails) principal).getUsername();
                // 这里可以根据username查询用户ID，或者扩展UserDetails接口添加getId方法
                log.warn("无法直接获取用户ID，仅获取到用户名: {}", username);
                return null;
            }
        }
        log.warn("未能获取到当前登录用户");
        return null;
    }

    /**
     * 生成存储在向量数据库中的知识库名称
     * 
     * 实现思路：
     * 1. 通过格式化字符串将用户ID和知识库名称组合
     * 2. 确保不同用户的同名知识库在向量数据库中有唯一标识
     * 3. 使用前缀"kb_"便于识别和管理
     * 
     * @param kbName 知识库名称
     * @return 格式化后的知识库存储名称，格式为"kb_{用户ID}_{知识库名称}"
     */
    public static String getStoreKnowledgeBaseName(String kbName) {
        return "kb_" + getCurrentUserId() + "_" + kbName;
    }

    /**
     * 基于朴素RAG的问答服务
     * 
     * 实现思路：
     * 1. 验证知识库ID的有效性，检查知识库是否存在
     * 2. 根据意图识别参数判断是否需要进行知识检索
     * 3. 如果启用查询重写，对用户问题进行优化处理
     * 4. 设置查询参数（topk等）并调用朴素RAG策略
     * 5. 返回流式响应，支持实时对话体验
     * 
     * @param id 知识库ID
     * @param question 用户问题
     * @param topk 检索结果数量限制
     * @param ir 是否启用意图识别
     * @param qr 是否启用查询重写
     * @param qd 是否启用问题分解（当前未实现）
     * @return 流式响应对象，包含AI回答内容
     */
    @Override
    public Flux<String> naiveQa(Long id, String question, int topk, boolean ir, boolean qr, boolean qd) {
        Optional<KnowledgeBase> knowledgeBaseOpt = knowledgeBaseRepository.findById(id);

        if (knowledgeBaseOpt.isEmpty()) {
            log.error("知识库: {}, 不存在，请确认知识库ID是否正确。", id);
            // 发送错误信息
            return sseUtil.sendFluxMessage("知识库不存在，请确认知识库ID是否正确。");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseOpt.get();

        // 意图识别
        if (ir && needRetrieve(question, null)) {
            return sseUtil.handleStreamingChatResponse(question, null);
        }

        // 查询重写
        if (qr) {
            question = queryRewrite.rewriteQuery("", question).getFinalQuery();
        }

        // 查询分解
        if(qd){
            QueryDecomposeResult result = queryDecompose.decomposeQuery(question);
            for (QueryDecomposeResult.QueryItem item : result.getQueries()) {
                System.out.println("类型: " + item.getType() + ", 查询: " + item.getQuery());
            }
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("topk", topk);

        //  TODO 问题分解，然后把问题列表传入
        return naiveRag.chat(knowledgeBase, question, params);
    }

    /**
     * 基于HiSem分层语义RAG的问答服务
     * 
     * 实现思路：
     * 1. 验证知识库ID的有效性，检查知识库是否存在
     * 2. 根据意图识别参数判断是否需要进行知识检索，与naiveQa逻辑相反
     * 3. 如果启用查询重写，对用户问题进行优化处理
     * 4. 设置查询参数（maxRes等）并调用HiSem RAG策略
     * 5. 返回流式响应，支持更高质量的对话体验
     * 
     * @param id 知识库ID
     * @param question 用户问题
     * @param maxRes 最大检索结果数量
     * @param ir 是否启用意图识别
     * @param qr 是否启用查询重写
     * @param qd 是否启用问题分解（当前未实现）
     * @return 流式响应对象，包含AI回答内容
     */
    @Override
    public Flux<String> hisemQa(Long id, String question, int maxRes, boolean ir, boolean qr, boolean qd) {
        Optional<KnowledgeBase> knowledgeBaseOpt = knowledgeBaseRepository.findById(id);

        if (knowledgeBaseOpt.isEmpty()) {
            log.error("知识库: {}, 不存在，请确认知识库ID是否正确。", id);
            // 发送错误信息
            return sseUtil.sendFluxMessage("知识库不存在，请确认知识库ID是否正确。");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseOpt.get();

        // 意图识别
        if (ir && !needRetrieve(question, null)) {
            return sseUtil.handleStreamingChatResponse(question, null);
        }

        // 查询重写
        if (qr) {
            question = queryRewrite.rewriteQuery("", question).getFinalQuery();
        }

        // 查询分解
        if(qd){
            QueryDecomposeResult result = queryDecompose.decomposeQuery(question);
            for (QueryDecomposeResult.QueryItem item : result.getQueries()) {
                System.out.println("类型: " + item.getType() + ", 查询: " + item.getQuery());
            }
        }

        HashMap<String, Object> params = new HashMap<>();
        params.put("maxRes", maxRes);

        //  TODO 问题分解，然后把问题列表传入
        return hiSemRag.chat(knowledgeBase, question, params);
    }

    /**
     * 判断问题是否需要进行知识检索
     * 
     * 实现思路：
     * 1. 使用意图分类器分析用户问题的意图
     * 2. 根据分析结果判断是否需要在知识库中检索相关信息
     * 3. 记录分析过程和结果，便于调试和优化
     * 
     * @param question 用户问题
     * @param history 对话历史（可选）
     * @return true表示需要检索，false表示无需检索
     */
    private boolean needRetrieve(String question, String history) {
        IntentResult intentResult = intentClassifier.analyzeIntent(question, history);

        if (intentResult.isParseSuccess()) {
            String reason = intentResult.getReason();
            String questionType = intentResult.getQuestionType();
            if (intentResult.isNeedRetrieval()) {
                log.info("问题: {} 需要检索, 原因: {}", question, reason);
                return true;
            }
            log.info("问题: {} 无需检索, 原因: {}, 类型: {}", question, reason, questionType);
        }

        return false;
    }
}