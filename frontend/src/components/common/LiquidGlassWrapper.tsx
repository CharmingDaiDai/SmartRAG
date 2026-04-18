import React, { forwardRef } from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

// 类型定义
export interface LiquidGlassWrapperProps {
  children: React.ReactNode;
  className?: string;
  displacementScale?: number;
  blurAmount?: number;
  saturation?: number;
  aberrationIntensity?: number;
  elasticity?: number;
  cornerRadius?: number;
  padding?: string;
  onClick?: () => void;
  mouseContainer?: React.RefObject<HTMLElement | null>;
  mode?: 'standard' | 'polar' | 'prominent' | 'shader';
  globalMousePos?: { x: number; y: number };
  mouseOffset?: { x: number; y: number };
  overLight?: boolean;
  style?: React.CSSProperties;
  variant?: 'card' | 'button' | 'panel' | 'dialog' | 'menu';
  disabled?: boolean;
  [key: string]: any; // 支持额外属性
}

// 合并类名的工具函数
const cn = (...inputs: ClassValue[]) => twMerge(clsx(inputs));

/**
 * 全局玻璃包装组件（液态效果已移除）
 * 保留统一容器接口，避免影响现有调用方
 */
const LiquidGlassWrapper = forwardRef<HTMLDivElement, LiquidGlassWrapperProps>(
  (
    {
      children,
      className = '',
      padding,
      onClick,
      style = {},
      disabled = false,
      displacementScale: _displacementScale,
      blurAmount: _blurAmount,
      saturation: _saturation,
      aberrationIntensity: _aberrationIntensity,
      elasticity: _elasticity,
      cornerRadius: _cornerRadius,
      mouseContainer: _mouseContainer,
      mode: _mode,
      globalMousePos: _globalMousePos,
      mouseOffset: _mouseOffset,
      overLight: _overLight,
      variant: _variant,
      ...domProps
    },
    ref
  ) => {
    const mergedStyle = padding ? { ...style, padding } : style;

    if (disabled) {
      return (
        <div ref={ref} className={cn(className)} style={mergedStyle} aria-disabled="true" {...domProps}>
          {children}
        </div>
      );
    }

    return (
      <div ref={ref} className={cn(className)} style={mergedStyle} onClick={onClick} {...domProps}>
        {children}
      </div>
    );
  }
);

LiquidGlassWrapper.displayName = 'LiquidGlassWrapper';

export default LiquidGlassWrapper;