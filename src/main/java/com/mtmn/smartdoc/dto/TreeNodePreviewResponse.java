package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.enums.TreeNodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * TreeNode 预览 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodePreviewResponse {

    /**
     * 节点 ID
     */
    private String nodeId;

    /**
     * 父节点 ID
     */
    private String parentNodeId;

    /**
     * 标题路径
     */
    private List<String> titlePath;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 节点类型
     */
    private TreeNodeType nodeType;

    /**
     * 核心知识点
     */
    private String keyKnowledge;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 内容（可能被截断）
     */
    private String content;

    /**
     * 子节点列表
     */
    private List<TreeNodePreviewResponse> children;

    /**
     * 是否已向量化
     */
    private Boolean isVectorized;
}