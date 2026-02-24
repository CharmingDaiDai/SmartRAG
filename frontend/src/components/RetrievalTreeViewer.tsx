import React from 'react';
import { Collapse, Tree, Tag } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { RetrievalTreeNode } from '../types';

interface RetrievalTreeViewerProps {
  treeRoots: RetrievalTreeNode[];
}

/**
 * 剪枝：只保留有 inResults=true 节点的路径（及其祖先链）
 * 不在最终结果中的叶节点所在的整条分支会被移除
 */
const pruneToResultPaths = (nodes: RetrievalTreeNode[]): RetrievalTreeNode[] => {
  return nodes.flatMap((node) => {
    const prunedChildren = pruneToResultPaths(node.children);
    if (node.inResults || prunedChildren.length > 0) {
      return [{ ...node, children: prunedChildren }];
    }
    return [];
  });
};

const buildTreeData = (nodes: RetrievalTreeNode[]): DataNode[] =>
  nodes.map((n) => {
    // 取 titlePath 最后一段作为节点显示名（避免冗余的完整路径）
    const shortName =
      n.titlePath && n.titlePath.includes(' -> ')
        ? n.titlePath.split(' -> ').pop()!
        : n.titlePath || n.nodeId || '未知节点';

    return {
      key: n.nodeId || n.titlePath,
      title: (
        <span style={{ display: 'inline-flex', gap: 6, alignItems: 'center' }}>
          <span>{shortName}</span>
          <Tag
            color={n.inResults ? 'success' : 'blue'}
            style={{ margin: 0, fontSize: 11 }}
          >
            {(n.score * 100).toFixed(0)}%
          </Tag>
        </span>
      ),
      children:
        n.children && n.children.length > 0
          ? buildTreeData(n.children)
          : undefined,
    };
  });

const RetrievalTreeViewer: React.FC<RetrievalTreeViewerProps> = ({
  treeRoots,
}) => {
  if (!treeRoots || treeRoots.length === 0) return null;

  // 剪枝后只展示有参考价值的路径
  const pruned = pruneToResultPaths(treeRoots);
  if (pruned.length === 0) return null;

  // 统计最终命中的叶节点数量
  const countLeaves = (nodes: RetrievalTreeNode[]): number =>
    nodes.reduce((acc, n) => acc + (n.inResults ? 1 : 0) + countLeaves(n.children), 0);
  const hitCount = countLeaves(pruned);

  return (
    <Collapse
      size="small"
      style={{ marginTop: 8 }}
      items={[
        {
          key: '1',
          label: `检索路径（${hitCount} 个命中节点）`,
          children: (
            <Tree
              treeData={buildTreeData(pruned)}
              defaultExpandAll
              showLine={{ showLeafIcon: false }}
              style={{ background: 'transparent', fontSize: 13 }}
            />
          ),
        },
      ]}
    />
  );
};

export default RetrievalTreeViewer;

