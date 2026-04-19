package com.mtmn.smartrag.rag.retriever;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索路径树节点，用于可视化层级检索过程
 *
 * <p>每个节点对应一次 Milvus 搜索命中的文档块，记录其在检索树中的位置：
 * 是否通过动态阈值筛选、是否最终进入结果集、以及子层下钻的节点列表。
 *
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalTreeNode {

    /** 对应 Milvus metadata 中的 node_id */
    private String nodeId;

    /** 完整标题路径（从 metadata title_path） */
    private String titlePath;

    /** 相似度分数 */
    private double score;

    /** 是否通过动态阈值（进入下钻或收录） */
    private boolean passedThreshold;

    /** 是否包含在最终结果中（叶节点或退化节点） */
    private boolean inResults;

    /** 子节点（下一层通过阈值的节点） */
    @Builder.Default
    private List<RetrievalTreeNode> children = new ArrayList<>();
}
