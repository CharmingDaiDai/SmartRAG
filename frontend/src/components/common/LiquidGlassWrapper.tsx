import React, { forwardRef } from 'react';
import LiquidGlass from 'liquid-glass-react';
import { useAppStore } from '../../store/useAppStore'; // 使用项目中的状态管理
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import './LiquidGlassWrapper.css';

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
 * 全局液态玻璃包装组件
 * 提供统一的液态玻璃效果，与项目现有的玻璃效果系统兼容
 */
const LiquidGlassWrapper = forwardRef<HTMLDivElement, LiquidGlassWrapperProps>(
  (
    {
      children,
      className = '',
      displacementScale = 70,
      blurAmount = 0.0625,
      saturation = 140,
      aberrationIntensity = 2,
      elasticity = 0.15,
      cornerRadius = 999,
      padding,
      onClick,
      mouseContainer = null,
      mode = 'standard',
      globalMousePos,
      mouseOffset,
      overLight = false,
      style = {},
      variant = 'card',
      disabled = false,
      ...props
    },
    ref
  ) => {
    const themeMode = useAppStore(state => state.themeMode); // 获取主题模式

    // 根据不同变体调整参数
    const getVariantParams = () => {
      switch (variant) {
        case 'button':
          return {
            displacementScale: displacementScale || 64,
            blurAmount: blurAmount || 0.1,
            saturation: saturation || 130,
            aberrationIntensity: aberrationIntensity || 2,
            elasticity: elasticity || 0.35,
            cornerRadius: cornerRadius || 100,
          };
        case 'panel':
          return {
            displacementScale: displacementScale || 80,
            blurAmount: blurAmount || 0.08,
            saturation: saturation || 135,
            aberrationIntensity: aberrationIntensity || 1.5,
            elasticity: elasticity || 0.25,
            cornerRadius: cornerRadius || 20,
          };
        case 'dialog':
          return {
            displacementScale: displacementScale || 75,
            blurAmount: blurAmount || 0.07,
            saturation: saturation || 145,
            aberrationIntensity: aberrationIntensity || 2.2,
            elasticity: elasticity || 0.2,
            cornerRadius: cornerRadius || 16,
          };
        case 'menu':
          return {
            displacementScale: displacementScale || 50,
            blurAmount: blurAmount || 0.05,
            saturation: saturation || 125,
            aberrationIntensity: aberrationIntensity || 1.8,
            elasticity: elasticity || 0.18,
            cornerRadius: cornerRadius || 12,
          };
        default: // card
          return {
            displacementScale: displacementScale || 70,
            blurAmount: blurAmount || 0.0625,
            saturation: saturation || 140,
            aberrationIntensity: aberrationIntensity || 2,
            elasticity: elasticity || 0.15,
            cornerRadius: cornerRadius || 999,
          };
      }
    };

    const variantParams = getVariantParams();

    // 根据主题调整参数
    const themedParams = {
      ...variantParams,
      blurAmount: themeMode === 'dark' ? variantParams.blurAmount * 1.1 : variantParams.blurAmount,
      saturation: themeMode === 'dark' ? variantParams.saturation * 1.05 : variantParams.saturation,
    };

    if (disabled) {
      // 如果禁用，则直接返回子元素，不应用液态玻璃效果
      return <div className={cn(className)} style={style}>{children}</div>;
    }

    // 创建一个容器元素，然后将其传递给 LiquidGlass
    const ContainerElement = React.useMemo(() => {
      return React.forwardRef<HTMLDivElement>((props, containerRef) => (
        <div
          ref={containerRef as React.ForwardedRef<HTMLDivElement>}
          {...props}
          className={cn(
            className
          )}
        >
          {children}
        </div>
      ));
    }, [children, className]);

    return (
      <div className={cn('liquid-glass-flow-layout', `liquid-glass-flow-layout--${variant}`)}>
        <LiquidGlass
          className="liquid-glass-flow-layout__root"
          displacementScale={themedParams.displacementScale}
          blurAmount={themedParams.blurAmount}
          saturation={themedParams.saturation}
          aberrationIntensity={themedParams.aberrationIntensity}
          elasticity={themedParams.elasticity}
          cornerRadius={themedParams.cornerRadius}
          padding={padding ?? '0px'}
          onClick={onClick}
          mouseContainer={mouseContainer}
          mode={mode}
          globalMousePos={globalMousePos}
          mouseOffset={mouseOffset}
          overLight={overLight}
          style={style}
          {...props}
        >
          <ContainerElement ref={ref as React.ForwardedRef<HTMLDivElement>} />
        </LiquidGlass>
      </div>
    );
  }
);

LiquidGlassWrapper.displayName = 'LiquidGlassWrapper';

export default LiquidGlassWrapper;