/**
 * 液体玻璃态卡片组件 (LiquidGlassCard)
 * 
 * 功能逻辑：
 * 1. 业务层面：对基础卡片元素提供玻璃态质感支持，常用于 Dashboard 中的统计数据承载或高亮区域。
 * 2. 视觉表现：它依赖 `<LiquidGlassWrapper>` 提供的 Blur/透明背景与边框泛光特效。
 * 3. 约束能力：对外暴露的 Props(如 variant, padding) 对内会散列为明确的一组 Tailwind CSS 原子类集。支持直接定义 title 与 subtitle（带底部分割线）。
 * 
 * 组件设计：
 * - 兼顾内容区的相对层叠 (`relative z-10`)，避免其子元素的交互被外圈玻璃蒙版或光晕层阻挡。
 * - 设计上与 Tailwind 耦合（例如 padding -> p-4），提供简易且统一的响应式接口。
 */
import React from 'react';
import LiquidGlassWrapper from './LiquidGlassWrapper';

interface LiquidGlassCardProps {
  children: React.ReactNode;
  className?: string; // 允许从外部混入额外的 Tailwind Classes 以便覆写样式
  title?: string;
  subtitle?: string;
  variant?: 'default' | 'elevated' | 'compact'; // 定义其投射的阴影强度与层级深度
  padding?: 'small' | 'medium' | 'large';
  disabled?: boolean;
  style?: React.CSSProperties; // 极少数场景下的内联样式透传
}

const LiquidGlassCard: React.FC<LiquidGlassCardProps> = ({
  children,
  className = '',
  title,
  subtitle,
  variant = 'default',
  padding = 'medium',
  disabled = false,
  style,
}) => {
  // 根据内边距设置计算具体的 Tailwind 内边距类名
  const getPaddingClass = () => {
    switch(padding) {
      case 'small':
        return 'p-3';
      case 'large':
        return 'p-6';
      default: // medium 默认适中
        return 'p-4';
    }
  };

  // 根据变体类型计算悬浮阴影大小类名
  const getVariantClass = () => {
    switch(variant) {
      case 'elevated':
        return 'shadow-lg';
      case 'compact':
        return 'shadow-sm';
      default: // default
        return 'shadow-md';
    }
  };

  // 根据传入配置拼接出最终需要赋给 Wrapper 的 class
  return (
    <LiquidGlassWrapper
      variant="card"
      className={`rounded-xl ${getVariantClass()} ${getPaddingClass()} ${className}`}
      disabled={disabled}
      style={style}
    >
      {/* 头部渲染区，存在其一则渲染标题边框块 */}
      {(title || subtitle) && (
        <div className="mb-3 pb-2 border-b border-gray-200 dark:border-gray-700">
          {title && <h3 className="text-lg font-semibold text-gray-800 dark:text-white">{title}</h3>}
          {subtitle && <p className="text-sm text-gray-600 dark:text-gray-300">{subtitle}</p>}
        </div>
      )}
      {/* 透传儿童节点内容，抬升 Z-Index 保证按键或可点内容的可抵达性 */}
      <div className="relative z-10">
        {children}
      </div>
    </LiquidGlassWrapper>
  );
};

export default LiquidGlassCard;