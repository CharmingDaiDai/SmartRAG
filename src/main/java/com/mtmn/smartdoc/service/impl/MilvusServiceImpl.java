package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.service.MilvusService;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Milvus向量数据库服务实现
 * 负责创建和管理Milvus嵌入存储
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/5/9 10:17
 */
@Service
public class MilvusServiceImpl implements MilvusService {

    @Value("${milvus.host}")
    String host;

    @Value("${milvus.port}")
    Integer port;

    /**
     * 获取Milvus嵌入存储实例
     * <p>
     * 实现思路：
     * 1. 使用Builder模式构建MilvusEmbeddingStore实例
     * 2. 配置Milvus服务器连接参数：host和port
     * 3. 设置集合名称（collectionName）用于数据隔离
     * 4. 配置向量维度（dimension）以匹配嵌入模型
     * 5. 设置索引类型为FLAT，适合小到中等规模数据
     * 6. 配置距离度量类型为COSINE余弦相似度
     * 7. 设置一致性级别为EVENTUALLY，平衡性能和一致性
     * 8. 禁用自动刷新（autoFlushOnInsert=false）以提高批量插入性能
     * 9. 配置字段名称映射：
     * - id字段：向量记录的唯一标识
     * - text字段：存储原始文本内容
     * - metadata字段：存储文档元数据
     * - vector字段：存储向量数据
     * 10. 返回配置完成的Milvus嵌入存储实例
     *
     * @param collectionName 集合名称，用于数据库中的集合标识
     * @param dimension      向量维度，需要与嵌入模型输出维度一致
     * @return 配置完成的Milvus嵌入存储实例
     */
    public MilvusEmbeddingStore getEmbeddingStore(String collectionName, Integer dimension) {
        return MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collectionName)
                .dimension(dimension)
                .indexType(IndexType.FLAT)
                .metricType(MetricType.COSINE)
                .consistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                .autoFlushOnInsert(false)
                .idFieldName("id")
                .textFieldName("text")
                .metadataFieldName("metadata")
                .vectorFieldName("vector")
                .build();
    }

}