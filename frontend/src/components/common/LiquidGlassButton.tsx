import React from 'react';
import LiquidGlassWrapper from './LiquidGlassWrapper';

interface LiquidGlassButtonProps {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
  variant?: 'primary' | 'secondary' | 'ghost';
  size?: 'small' | 'medium' | 'large';
  disabled?: boolean;
  style?: React.CSSProperties;
}

const LiquidGlassButton: React.FC<LiquidGlassButtonProps> = ({
  children,
  onClick,
  className = '',
  variant = 'primary',
  size = 'medium',
  disabled = false,
  style,
}) => {
  // 根据变体和尺寸计算样式
  const getButtonStyles = () => {
    let baseStyles = 'inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 cursor-pointer ';

    // 尺寸样式
    switch(size) {
      case 'small':
        baseStyles += 'px-3 py-1.5 text-sm ';
        break;
      case 'large':
        baseStyles += 'px-6 py-3 text-lg ';
        break;
      default: // medium
        baseStyles += 'px-4 py-2 text-base ';
    }

    // 变体样式
    switch(variant) {
      case 'primary':
        baseStyles += 'text-white bg-indigo-600 hover:bg-indigo-700 ';
        break;
      case 'secondary':
        baseStyles += 'text-gray-700 bg-gray-200 hover:bg-gray-300 dark:text-gray-200 dark:bg-gray-600 dark:hover:bg-gray-500 ';
        break;
      case 'ghost':
        baseStyles += 'text-indigo-600 hover:bg-indigo-50 dark:text-indigo-400 dark:hover:bg-gray-700 ';
        break;
    }

    if(disabled) {
      baseStyles += 'opacity-50 cursor-not-allowed ';
    }

    return baseStyles;
  };

  return (
    <LiquidGlassWrapper
      variant="button"
      className={`${getButtonStyles()} ${className}`}
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={style}
    >
      <span className="relative z-10">{children}</span>
    </LiquidGlassWrapper>
  );
};

export default LiquidGlassButton;