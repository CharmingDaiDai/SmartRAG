package com.mtmn.smartdoc.enums;

/**
 * HisemRAG 树节点类型
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public enum TreeNodeType {
    /**
     * 根节点 - 文档的根
     */
    ROOT,

    /**
     * 内部节点 - 有子节点的中间层节点
     */
    INTERNAL,

    /**
     * 叶子节点 - 没有子节点的终端节点
     */
    LEAF
}