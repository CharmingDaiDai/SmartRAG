package com.mtmn.smartdoc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * TreeNode 响应 VO（含 children 树形结构，供前端渲染）
 *
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeVO {

    /** DB 主键（用于编辑请求） */
    private Long id;
    /** 文档树节点 ID */
    private String nodeId;
    private String parentNodeId;
    private String titlePath;
    private Integer level;
    /** 节点类型："ROOT" / "INTERNAL" / "LEAF" */
    private String nodeType;
    private String content;
    private String summary;
    @Builder.Default
    private List<TreeNodeVO> children = new ArrayList<>();
}
