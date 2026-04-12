import React from 'react';
import { motion, HTMLMotionProps, Variants } from 'framer-motion';

// 统一的贝塞尔曲线（ease-out-quart，比弹簧更克制）
const EASE_OUT = [0.25, 0.46, 0.45, 0.94] as const;
const EASE_IN_OUT = [0.4, 0, 0.2, 1] as const;
const HOVER_LIFT_Y = -2;
const HOVER_LIFT_Y_SOFT = -1;

// ==================== 基础动画 ====================

// 基础淡入动画
export const FadeIn: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.22, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 向上滑入动画（减小 y 距离，使用贝塞尔曲线，更克制）
export const SlideInUp: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -8 }}
        transition={{ duration: 0.28, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 向下滑入动画
export const SlideInDown: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, y: -12 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 8 }}
        transition={{ duration: 0.28, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 从左滑入动画
export const SlideInLeft: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        exit={{ opacity: 0, x: 16 }}
        transition={{ duration: 0.28, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 从右滑入动画
export const SlideInRight: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        exit={{ opacity: 0, x: -16 }}
        transition={{ duration: 0.28, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 缩放动画（用于 Modal 内容）
export const ScaleIn: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.96 }}
        transition={{ duration: 0.22, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 列表动画 ====================

// 列表容器（子元素依次出现，减小 stagger 间隔）
export const StaggerContainer: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial="hidden"
        animate="visible"
        variants={{
            visible: {
                transition: {
                    staggerChildren: 0.06,
                    delayChildren: 0.04,
                }
            }
        }}
        {...props}
    >
        {children}
    </motion.div>
);

// 列表项（配合 StaggerContainer 使用）
export const StaggerItem: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        variants={{
            hidden: { opacity: 0, y: 10, scale: 0.98 },
            visible: {
                opacity: 1,
                y: 0,
                scale: 1,
                transition: { duration: 0.22, ease: EASE_OUT }
            }
        }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 交互动画 ====================

// 悬停放大效果（轻量版）
export const HoverScale: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        whileHover={{ scale: 1.02, y: HOVER_LIFT_Y_SOFT }}
        whileTap={{ scale: 0.985, y: 0 }}
        transition={{ duration: 0.18, ease: EASE_IN_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 悬停上浮效果（用上移替代 scale，更优雅）
export const HoverLift: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        whileHover={{ y: HOVER_LIFT_Y }}
        whileTap={{ y: 0 }}
        transition={{ duration: 0.2, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 悬停卡片效果（上移 + 阴影加深，替代 scale 避免膨胀感）
export const HoverCard: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        whileHover={{
            y: HOVER_LIFT_Y,
            transition: { duration: 0.2, ease: EASE_OUT }
        }}
        whileTap={{ y: 0, transition: { duration: 0.1 } }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 状态动画 ====================

// 脉冲动画（用于加载状态，更柔和）
export const Pulse: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        animate={{ opacity: [0.45, 1, 0.45] }}
        transition={{ duration: 1.8, repeat: Infinity, ease: "easeInOut" }}
        {...props}
    >
        {children}
    </motion.div>
);

// 旋转加载动画
export const Spin: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        animate={{ rotate: 360 }}
        transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        {...props}
    >
        {children}
    </motion.div>
);

// 弹跳动画
export const Bounce: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        animate={{ y: [0, -8, 0] }}
        transition={{ duration: 0.7, repeat: Infinity, ease: "easeInOut" }}
        {...props}
    >
        {children}
    </motion.div>
);

// 抖动动画（用于错误提示）
export const Shake: React.FC<HTMLMotionProps<"div"> & { trigger?: boolean }> = ({ children, trigger, ...props }) => (
    <motion.div
        animate={trigger ? { x: [-8, 8, -8, 8, 0] } : {}}
        transition={{ duration: 0.35 }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 组合动画 ====================

// 渐显缩放（用于 Modal 内容）
export const FadeInScale: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.96 }}
        transition={{ duration: 0.2, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// 渐显向上滑入（用于卡片/通知）
export const FadeInSlideUp: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -10 }}
        transition={{ duration: 0.28, ease: EASE_OUT }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 高级动画变体 ====================

// 卡片网格动画变体
export const cardGridVariants: Variants = {
    hidden: { opacity: 0 },
    visible: {
        opacity: 1,
        transition: {
            staggerChildren: 0.06,
            delayChildren: 0.05
        }
    }
};

export const cardItemVariants: Variants = {
    hidden: { opacity: 0, y: 14, scale: 0.97 },
    visible: {
        opacity: 1,
        y: 0,
        scale: 1,
        transition: {
            duration: 0.24,
            ease: [0.25, 0.46, 0.45, 0.94]
        }
    }
};

// 页面过渡动画变体（简洁淡入+上移）
export const pageVariants: Variants = {
    initial: { opacity: 0, y: 10 },
    animate: {
        opacity: 1,
        y: 0,
        transition: {
            duration: 0.26,
            ease: [0.25, 0.46, 0.45, 0.94]
        }
    },
    exit: {
        opacity: 0,
        y: -8,
        transition: {
            duration: 0.18,
            ease: [0.4, 0, 1, 1]
        }
    }
};
