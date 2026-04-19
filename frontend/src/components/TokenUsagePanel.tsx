import React from 'react';
import { Collapse, Progress } from 'antd';
import type { TokenUsageReport } from '../types';
import { formatNumber } from '../utils/formatters';

interface Props {
    tokenUsage: TokenUsageReport;
}

const formatDuration = (ms: number): string => {
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
};

export const TokenUsagePanel: React.FC<Props> = ({ tokenUsage }) => {
    const { entries, total } = tokenUsage;
    if (!entries?.length) return null;

    return (
        <Collapse
            ghost
            size="small"
            style={{ marginTop: 4 }}
            items={[{
                key: '1',
                label: (
                    <span style={{ fontSize: 11, color: '#999' }}>
                        Token 用量：{formatNumber(total.totalTokens)}
                        <span style={{ marginLeft: 8, color: '#bbb' }}>
                            （↑{formatNumber(total.inputTokens)} 输入 / ↓{formatNumber(total.outputTokens)} 输出 / ⏱ {formatDuration(total.durationMs)}）
                        </span>
                    </span>
                ),
                children: (
                    <div style={{ fontSize: 11 }}>
                        {/* 表头 */}
                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: '1fr 60px 60px 60px 52px',
                            gap: 4,
                            color: '#aaa',
                            marginBottom: 6,
                            padding: '0 2px'
                        }}>
                            <span>环节</span>
                            <span style={{ textAlign: 'right' }}>输入</span>
                            <span style={{ textAlign: 'right' }}>输出</span>
                            <span style={{ textAlign: 'right' }}>合计</span>
                            <span style={{ textAlign: 'right' }}>耗时</span>
                        </div>

                        {/* 各条目 */}
                        {entries.map((entry, i) => {
                            const pct = total.totalTokens > 0
                                ? Math.round(entry.totalTokens / total.totalTokens * 100)
                                : 0;
                            return (
                                <div key={i} style={{ marginBottom: 8 }}>
                                    <div style={{
                                        display: 'grid',
                                        gridTemplateColumns: '1fr 60px 60px 60px 52px',
                                        gap: 4,
                                        padding: '0 2px',
                                        color: '#555'
                                    }}>
                                        <span>{entry.label}</span>
                                        <span style={{ textAlign: 'right', color: '#6366f1' }}>
                                            {formatNumber(entry.inputTokens)}
                                        </span>
                                        <span style={{ textAlign: 'right', color: '#10b981' }}>
                                            {formatNumber(entry.outputTokens)}
                                        </span>
                                        <span style={{ textAlign: 'right', fontWeight: 500 }}>
                                            {formatNumber(entry.totalTokens)}
                                        </span>
                                        <span style={{ textAlign: 'right', color: '#f59e0b' }}>
                                            {formatDuration(entry.durationMs)}
                                        </span>
                                    </div>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                                        <Progress
                                            percent={pct}
                                            size="small"
                                            showInfo={false}
                                            strokeColor="#6366f1"
                                            style={{ flex: 1, margin: 0 }}
                                        />
                                        <span style={{ color: '#bbb', fontSize: 10, minWidth: 28, textAlign: 'right' }}>
                                            {pct}%
                                        </span>
                                    </div>
                                </div>
                            );
                        })}

                        {/* 合计行 */}
                        <div style={{
                            borderTop: '1px solid #f0f0f0',
                            paddingTop: 6,
                            display: 'grid',
                            gridTemplateColumns: '1fr 60px 60px 60px 52px',
                            gap: 4,
                            padding: '6px 2px 0',
                            fontWeight: 600,
                            color: '#333'
                        }}>
                            <span>合计</span>
                            <span style={{ textAlign: 'right', color: '#6366f1' }}>
                                {formatNumber(total.inputTokens)}
                            </span>
                            <span style={{ textAlign: 'right', color: '#10b981' }}>
                                {formatNumber(total.outputTokens)}
                            </span>
                            <span style={{ textAlign: 'right' }}>
                                {formatNumber(total.totalTokens)}
                            </span>
                            <span style={{ textAlign: 'right', color: '#f59e0b' }}>
                                {formatDuration(total.durationMs)}
                            </span>
                        </div>
                    </div>
                ),
            }]}
        />
    );
};
