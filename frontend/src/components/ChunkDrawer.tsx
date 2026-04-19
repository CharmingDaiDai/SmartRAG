import React, { useEffect, useState, useCallback } from 'react';
import {
    Drawer, List, Tag, Button, Input, Space, Spin, Empty,
    Typography, Divider, Tooltip, message as antMessage, theme
} from 'antd';
import {
    EditOutlined, RightOutlined, DownOutlined,
    ExpandAltOutlined, CompressOutlined
} from '@ant-design/icons';
import { ChunkVO, DocumentItem, TreeNodeVO } from '../types';
import { chunkService } from '../services/chunkService';
import { normalizeStrategyType } from '../config/ragConfig';

interface ChunkDrawerProps {
    open: boolean;
    onClose: () => void;
    document: DocumentItem | null;
    kbId: string;
    indexStrategyType: string; // 'NAIVE_RAG' | 'HISEM_RAG_FAST' | 'HISEM_RAG'
}

// ─── 树形节点颜色映射 ───────────────────────────────────────────────────────
const NODE_DOT_COLOR: Record<string, string> = {
    ROOT:     '#ef4444',
    INTERNAL: '#3b82f6',
    LEAF:     '#10b981',
};
const NODE_TAG_COLOR: Record<string, string> = {
    ROOT:     'red',
    INTERNAL: 'blue',
    LEAF:     'green',
};
const NODE_LABEL: Record<string, string> = {
    ROOT:     '根节点',
    INTERNAL: '节点',
    LEAF:     '叶子',
};

// ─── TreeNodeItem 组件（独立 React 组件，保证 reconciliation 正确） ──────────
interface TreeNodeItemProps {
    node: TreeNodeVO;
    depth: number;
    expandedIds: Set<string>;
    onToggle: (nodeId: string) => void;
    editingId: number | null;
    editContent: string;
    saving: boolean;
    onStartEdit: (id: number, content: string) => void;
    onCancelEdit: () => void;
    onSave: (node: TreeNodeVO) => void;
    onEditChange: (content: string) => void;
}

const TreeNodeItem: React.FC<TreeNodeItemProps> = (props) => {
    const {
        node, depth, expandedIds, onToggle,
        editingId, editContent, saving,
        onStartEdit, onCancelEdit, onSave, onEditChange,
    } = props;
    const { token } = theme.useToken();

    const isLeaf      = node.nodeType === 'LEAF';
    const hasChildren = node.children.length > 0;
    const canExpand   = isLeaf || hasChildren;
    const expanded    = expandedIds.has(node.nodeId);
    const isEditing   = editingId === node.id;

    const dotColor  = NODE_DOT_COLOR[node.nodeType] ?? token.colorTextTertiary;
    const tagColor  = NODE_TAG_COLOR[node.nodeType] ?? 'default';
    const typeLabel = NODE_LABEL[node.nodeType] ?? node.nodeType;

    const shortTitle =
        node.titlePath?.includes(' -> ')
            ? node.titlePath.split(' -> ').pop()!
            : node.titlePath || node.nodeId;

    const handleHeaderClick = () => {
        if (canExpand) onToggle(node.nodeId);
    };

    return (
        <div style={{ marginBottom: 2 }}>
            {/* ── 节点标题行 ───────────────────────────────────── */}
            <div
                onClick={handleHeaderClick}
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '5px 8px',
                    marginLeft: depth * 20,
                    borderRadius: 6,
                    cursor: canExpand ? 'pointer' : 'default',
                    userSelect: 'none',
                    background: isEditing ? token.colorPrimaryBg : 'transparent',
                    transition: 'background 0.15s',
                }}
                onMouseEnter={e => {
                    if (!isEditing)
                        (e.currentTarget as HTMLElement).style.background = token.colorFillTertiary;
                }}
                onMouseLeave={e => {
                    if (!isEditing)
                        (e.currentTarget as HTMLElement).style.background = '';
                }}
            >
                {/* 展开/收起图标占位 */}
                <span style={{ width: 14, flexShrink: 0, display: 'flex', alignItems: 'center' }}>
                    {canExpand && (
                        expanded
                            ? <DownOutlined  style={{ fontSize: 10, color: token.colorTextTertiary }} />
                            : <RightOutlined style={{ fontSize: 10, color: token.colorTextTertiary }} />
                    )}
                </span>

                {/* 类型圆点 */}
                <span style={{
                    width: 8, height: 8, borderRadius: '50%',
                    background: dotColor, flexShrink: 0,
                }} />

                {/* 标题（单行截断，hover tooltip） */}
                <Tooltip title={node.titlePath} placement="topLeft" mouseEnterDelay={0.5}>
                    <span style={{
                        fontSize: 13,
                        fontWeight: isLeaf ? 400 : 600,
                        flex: 1, minWidth: 0,
                        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                    }}>
                        {shortTitle}
                    </span>
                </Tooltip>

                {/* 节点类型标签 */}
                <Tag color={tagColor} style={{ margin: 0, fontSize: 11, flexShrink: 0 }}>
                    {typeLabel}
                </Tag>

                {/* 叶子节点字数提示 */}
                {isLeaf && (
                    <span style={{ fontSize: 11, color: token.colorTextTertiary, flexShrink: 0 }}>
                        {node.content ? `${node.content.length} 字` : '空'}
                    </span>
                )}
            </div>

            {/* ── 展开内容区 ───────────────────────────────────── */}
            {expanded && (
                <div
                    style={{
                        marginLeft: depth * 20 + 14,
                        paddingLeft: 16,
                        borderLeft: `2px solid ${dotColor}40`,
                        marginTop: 2,
                        marginBottom: 6,
                    }}
                >
                    {/* 叶子节点内容与编辑 */}
                    {isLeaf && (
                        <div style={{ padding: '4px 0 6px' }}>
                            {isEditing ? (
                                <div onClick={e => e.stopPropagation()}>
                                    <Input.TextArea
                                        value={editContent}
                                        onChange={e => onEditChange(e.target.value)}
                                        autoSize={{ minRows: 3, maxRows: 12 }}
                                        style={{ fontSize: 13, width: '100%' }}
                                    />
                                    <Space style={{ marginTop: 8 }}>
                                        <Button
                                            type="primary" size="small" loading={saving}
                                            onClick={() => onSave(node)}
                                        >
                                            保存
                                        </Button>
                                        <Button size="small" onClick={onCancelEdit}>取消</Button>
                                    </Space>
                                </div>
                            ) : (
                                <div>
                                    <Typography.Paragraph
                                        style={{
                                            fontSize: 13,
                                            color: node.content ? token.colorTextSecondary : token.colorTextTertiary,
                                            whiteSpace: 'pre-wrap', wordBreak: 'break-word',
                                            marginBottom: 6,
                                        }}
                                    >
                                        {node.content || '（内容为空）'}
                                    </Typography.Paragraph>
                                    <Button
                                        type="link" size="small" icon={<EditOutlined />}
                                        onClick={e => {
                                            e.stopPropagation();
                                            onStartEdit(node.id, node.content || '');
                                        }}
                                        style={{ padding: 0 }}
                                    >
                                        编辑
                                    </Button>
                                </div>
                            )}
                        </div>
                    )}

                    {/* 子节点递归 */}
                    {hasChildren && node.children.map(child => (
                        <TreeNodeItem key={child.nodeId} {...props} node={child} depth={depth + 1} />
                    ))}
                </div>
            )}
        </div>
    );
};

// ─── ChunkDrawer 主组件 ────────────────────────────────────────────────────
const ChunkDrawer: React.FC<ChunkDrawerProps> = ({
    open,
    onClose,
    document,
    kbId,
    indexStrategyType,
}) => {
    const isTree = normalizeStrategyType(indexStrategyType) === 'HISEM_RAG';

    const [loading,      setLoading]      = useState(false);
    const [chunks,       setChunks]       = useState<ChunkVO[]>([]);
    const [treeNodes,    setTreeNodes]    = useState<TreeNodeVO[]>([]);
    const [expandedIds,  setExpandedIds]  = useState<Set<string>>(new Set());
    const [editingId,    setEditingId]    = useState<number | null>(null);
    const [editContent,  setEditContent]  = useState('');
    const [saving,       setSaving]       = useState(false);

    // ── 数据加载 ──────────────────────────────────────────────────────────────
    useEffect(() => {
        if (open && document) {
            fetchData();
        } else {
            setChunks([]);
            setTreeNodes([]);
            setExpandedIds(new Set());
            setEditingId(null);
            setEditContent('');
        }
    }, [open, document]);

    // 数据加载完成后自动展开 ROOT / INTERNAL 节点
    useEffect(() => {
        if (treeNodes.length === 0) return;
        const autoExpanded = new Set<string>();
        const collect = (nodes: TreeNodeVO[]) => {
            nodes.forEach(n => {
                if (n.nodeType !== 'LEAF') {
                    autoExpanded.add(n.nodeId);
                    collect(n.children);
                }
            });
        };
        collect(treeNodes);
        setExpandedIds(autoExpanded);
    }, [treeNodes]);

    const fetchData = async () => {
        if (!document) return;
        setLoading(true);
        try {
            if (isTree) {
                const res: any = await chunkService.listTreeNodes(document.id, kbId);
                if (res.code === 200) setTreeNodes(res.data || []);
            } else {
                const res: any = await chunkService.listChunks(document.id, kbId);
                if (res.code === 200) setChunks(res.data || []);
            }
        } catch {
            antMessage.error('加载切分块失败');
        } finally {
            setLoading(false);
        }
    };

    // ── 展开/收起控制 ─────────────────────────────────────────────────────────
    const handleToggle = useCallback((nodeId: string) => {
        setExpandedIds(prev => {
            const next = new Set(prev);
            if (next.has(nodeId)) next.delete(nodeId);
            else next.add(nodeId);
            return next;
        });
    }, []);

    const handleExpandAll = () => {
        const allIds = new Set<string>();
        const collect = (nodes: TreeNodeVO[]) =>
            nodes.forEach(n => { allIds.add(n.nodeId); collect(n.children); });
        collect(treeNodes);
        setExpandedIds(allIds);
    };

    const handleCollapseAll = () => setExpandedIds(new Set());

    // ── 编辑控制 ──────────────────────────────────────────────────────────────
    const startEdit   = (id: number, content: string) => { setEditingId(id); setEditContent(content); };
    const cancelEdit  = () => { setEditingId(null); setEditContent(''); };
    const handleEditChange = (content: string) => setEditContent(content);

    const saveChunk = async (chunk: ChunkVO) => {
        if (!editContent.trim()) { antMessage.warning('内容不能为空'); return; }
        setSaving(true);
        try {
            const res: any = await chunkService.updateChunk(chunk.id, kbId, editContent);
            if (res.code === 200) {
                antMessage.success('保存成功，正在重建索引...');
                setChunks(prev => prev.map(c => c.id === chunk.id ? { ...c, content: editContent } : c));
                setEditingId(null);
            } else {
                antMessage.error(res.message || '保存失败');
            }
        } catch (e: any) {
            antMessage.error(e?.response?.data?.message || '保存失败');
        } finally {
            setSaving(false);
        }
    };

    const saveTreeNode = async (node: TreeNodeVO) => {
        if (!editContent.trim()) { antMessage.warning('内容不能为空'); return; }
        setSaving(true);
        try {
            const res: any = await chunkService.updateTreeNode(node.id, kbId, editContent);
            if (res.code === 200) {
                antMessage.success('保存成功，向量库已同步');
                const updateNode = (nodes: TreeNodeVO[]): TreeNodeVO[] =>
                    nodes.map(n => n.id === node.id
                        ? { ...n, content: editContent }
                        : { ...n, children: updateNode(n.children) });
                setTreeNodes(prev => updateNode(prev));
                setEditingId(null);
            } else {
                antMessage.error(res.message || '保存失败');
            }
        } catch (e: any) {
            antMessage.error(e?.response?.data?.message || '保存失败');
        } finally {
            setSaving(false);
        }
    };

    // ── 统计 ─────────────────────────────────────────────────────────────────
    const countLeafNodes = (nodes: TreeNodeVO[]): number =>
        nodes.reduce((acc, n) => acc + (n.nodeType === 'LEAF' ? 1 : 0) + countLeafNodes(n.children), 0);

    const leafCount = countLeafNodes(treeNodes);
    const subtitle  = isTree
        ? `${leafCount} 个叶子节点 · ${expandedIds.size} 个节点已展开`
        : `${chunks.length} 个切分块`;

    // ── 平铺列表（NAIVE_RAG / HISEM_RAG_FAST） ───────────────────────────────
    const renderFlatList = () => (
        <List
            dataSource={chunks}
            locale={{ emptyText: <Empty description="暂无切分块" /> }}
            renderItem={(chunk) => (
                <List.Item
                    key={chunk.id}
                    style={{ flexDirection: 'column', alignItems: 'flex-start', padding: '12px 0' }}
                >
                    <Space wrap style={{ marginBottom: 6 }}>
                        <Tag color="default">#{chunk.chunkIndex + 1}</Tag>
                        {chunk.summary && <Tag color="blue">已增强</Tag>}
                    </Space>

                    {editingId === chunk.id ? (
                        <>
                            <Input.TextArea
                                value={editContent}
                                onChange={e => setEditContent(e.target.value)}
                                autoSize={{ minRows: 3, maxRows: 12 }}
                                style={{ width: '100%', fontFamily: 'inherit', fontSize: 13 }}
                            />
                            <Space style={{ marginTop: 8 }}>
                                <Button type="primary" size="small" loading={saving} onClick={() => saveChunk(chunk)}>保存</Button>
                                <Button size="small" onClick={cancelEdit}>取消</Button>
                            </Space>
                        </>
                    ) : (
                        <>
                            <Typography.Paragraph
                                style={{ whiteSpace: 'pre-wrap', fontSize: 13, marginBottom: 4, width: '100%', wordBreak: 'break-word' }}
                            >
                                {chunk.content}
                            </Typography.Paragraph>
                            <Button
                                type="link" size="small" icon={<EditOutlined />}
                                onClick={() => startEdit(chunk.id, chunk.content)}
                                style={{ padding: 0 }}
                            >
                                编辑
                            </Button>
                        </>
                    )}
                </List.Item>
            )}
        />
    );

    // ── 树形列表（HISEM_RAG） ────────────────────────────────────────────────
    const treeNodeItemProps = {
        expandedIds,
        onToggle: handleToggle,
        editingId,
        editContent,
        saving,
        onStartEdit: startEdit,
        onCancelEdit: cancelEdit,
        onSave: saveTreeNode,
        onEditChange: handleEditChange,
    };

    const renderTreeList = () => {
        if (treeNodes.length === 0) return <Empty description="暂无树形节点" />;
        return (
            <>
                {/* 展开/收起全部操作栏 */}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginBottom: 8 }}>
                    <Button
                        size="small" type="text" icon={<ExpandAltOutlined />}
                        onClick={handleExpandAll}
                    >
                        全部展开
                    </Button>
                    <Button
                        size="small" type="text" icon={<CompressOutlined />}
                        onClick={handleCollapseAll}
                    >
                        全部收起
                    </Button>
                </div>
                {treeNodes.map(node => (
                    <TreeNodeItem key={node.nodeId} node={node} depth={0} {...treeNodeItemProps} />
                ))}
            </>
        );
    };

    // ── 渲染 ─────────────────────────────────────────────────────────────────
    return (
        <Drawer
            title={
                <div>
                    <div style={{ fontWeight: 600, fontSize: 15 }}>
                        {document?.filename || document?.fileName || '文档切分详情'}
                    </div>
                    <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 400 }}>
                        {subtitle}
                    </Typography.Text>
                </div>
            }
            placement="right"
            size={720}
            open={open}
            onClose={onClose}
            destroyOnClose
            styles={{ body: { padding: '12px 24px' } }}
        >
            {loading ? (
                <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
                    <Spin size="large" />
                </div>
            ) : (
                <>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {isTree
                            ? '点击节点行可展开/收起；叶子节点（绿色）支持内容编辑，修改后同步到向量库'
                            : '点击「编辑」修改内容，保存后将重新构建该文档的向量索引'}
                    </Typography.Text>
                    <Divider style={{ margin: '10px 0' }} />
                    {isTree ? renderTreeList() : renderFlatList()}
                </>
            )}
        </Drawer>
    );
};

export default ChunkDrawer;
