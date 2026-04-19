/**
 * 知识库检索溯源层级树可视化组件 (RetrievalTreeViewer)
 * 
 * 功能逻辑：
 * 1. 专门用于展现带有层级关系的 RAG 检索策略（例如 HiSem-SADP 或树形索引方法）在搜寻阶段命中过的文档/段落路径。
 * 2. 接收包含嵌套 `children` 层的 `RetrievalTreeNode` 数据。
 * 3. 核心功能：动态剪枝 (`pruneToResultPaths`)。因为一次复杂的搜寻可能触及成百上千个段落，此处只保留最终带有 `inResults=true` (即被送入大模型的上下文) 的节点及其前置父级链路。
 * 4. 利用 Ant Design 的 `<Tree>` 与 `<Collapse>` 组件递归绘制树状结构。
 * 
 * 视觉实现：
 * - 剥离冗长的面包屑 (`titlePath`)，只取末端切片名字作为叶子节点标签。
 * - 节点侧面拼接带有置信度/匹配率 (Score) 的 `<Tag>`，并根据是否命中变更为不同的状态色。
 */
import React from 'react';
import { Collapse, Tree, Tag } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { RetrievalTreeNode } from '../types';

interface RetrievalTreeViewerProps {
  treeRoots: RetrievalTreeNode[];
}

/**
 * 黑魔法递归剪枝器：只保留有 inResults=true 的节点分支（以及通向它的直系祖先链）
 * 大幅降低无关噪音数据带来的树形图庞杂感，让用户一眼看出最终送进了大模型的知识锚点。
 * 
 * @param nodes 待剪枝的原始树数组
 * @returns 剪枝瘦身后的树数组
 */
const pruneToResultPaths = (nodes: RetrievalTreeNode[]): RetrievalTreeNode[] => {
  return nodes.flatMap((node) => {
    // 优先深搜子节点
    const prunedChildren = pruneToResultPaths(node.children);
    // 只有当自己就是命中项，或者自己的子代（重孙代）里存在命中项时，自己才配存活
    if (node.inResults || prunedChildren.length > 0) {
      return [{ ...node, children: prunedChildren }];
    }
    return [];
  });
};

/**
 * 将业务型树状数据转为 UI 框架 (Ant Design Tree) 认得的 DataNode
 */
const buildTreeData = (nodes: RetrievalTreeNode[]): DataNode[] =>
  nodes.map((n) => {
    // 剥离全路径前缀：取 titlePath 最后一段作为节点展示名（例如 "A -> B -> C" 在渲染 C 时只显示 "C"）
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
      // 递归组装
      children:
        n.children && n.children.length > 0
          ? buildTreeData(n.children)
          : undefined,
    };
  });

const RetrievalTreeViewer: React.FC<RetrievalTreeViewerProps> = ({
  treeRoots,
}) => {
  // 无可用数据则退场
  if (!treeRoots || treeRoots.length === 0) return null;

  // 挂载前通过剪枝方法过滤掉陪跑的不相关分支
  const pruned = pruneToResultPaths(treeRoots);
  // 如果剪枝后光杆了（说明策略没搜出所以然），退场
  if (pruned.length === 0) return null;

  // 递归统计最终真命中的核心锚点数目，用于外层折叠板标题展示
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
              defaultExpandAll // 规模已经被剪枝控制得很小了，干脆默认展开查阅
              showLine={{ showLeafIcon: false }} // 展现层级线索连接关系
              style={{ background: 'transparent', fontSize: 13 }}
            />
          ),
        },
      ]}
    />
  );
};

export default RetrievalTreeViewer;

