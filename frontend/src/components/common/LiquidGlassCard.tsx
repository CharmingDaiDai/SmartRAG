import React from 'react';
import LiquidGlassWrapper from './LiquidGlassWrapper';

interface LiquidGlassCardProps {
  children: React.ReactNode;
  className?: string;
  title?: string;
  subtitle?: string;
  variant?: 'default' | 'elevated' | 'compact';
  padding?: 'small' | 'medium' | 'large';
  disabled?: boolean;
  style?: React.CSSProperties;
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
  // 根据内边距设置计算样式
  const getPaddingClass = () => {
    switch(padding) {
      case 'small':
        return 'p-3';
      case 'large':
        return 'p-6';
      default: // medium
        return 'p-4';
    }
  };

  // 根据变体计算样式
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

  return (
    <LiquidGlassWrapper
      variant="card"
      className={`rounded-xl ${getVariantClass()} ${getPaddingClass()} ${className}`}
      disabled={disabled}
      style={style}
    >
      {(title || subtitle) && (
        <div className="mb-3 pb-2 border-b border-gray-200 dark:border-gray-700">
          {title && <h3 className="text-lg font-semibold text-gray-800 dark:text-white">{title}</h3>}
          {subtitle && <p className="text-sm text-gray-600 dark:text-gray-300">{subtitle}</p>}
        </div>
      )}
      <div className="relative z-10">
        {children}
      </div>
    </LiquidGlassWrapper>
  );
};

export default LiquidGlassCard;