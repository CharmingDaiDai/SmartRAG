import React from 'react';
import { motion, HTMLMotionProps, Variants } from 'framer-motion';

// ==================== 基础动画 ====================

// 基础淡入动画
export const FadeIn: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.3 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 向上滑入动画
export const SlideInUp: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 向下滑入动画
export const SlideInDown: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: 20 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 从左滑入动画
export const SlideInLeft: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, x: -30 }}
        animate={{ opacity: 1, x: 0 }}
        exit={{ opacity: 0, x: 30 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 从右滑入动画
export const SlideInRight: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, x: 30 }}
        animate={{ opacity: 1, x: 0 }}
        exit={{ opacity: 0, x: -30 }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 缩放动画
export const ScaleIn: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.9 }}
        transition={{ type: "spring", stiffness: 400, damping: 25 }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 列表动画 ====================

// 列表容器（子元素依次出现）
export const StaggerContainer: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial="hidden"
        animate="visible"
        variants={{
            visible: {
                transition: {
                    staggerChildren: 0.1
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
            hidden: { opacity: 0, y: 20 },
            visible: { opacity: 1, y: 0 }
        }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 交互动画 ====================

// 悬停放大效果
export const HoverScale: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.98 }}
        transition={{ type: "spring", stiffness: 400, damping: 17 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 悬停上浮效果
export const HoverLift: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        whileHover={{ y: -4, boxShadow: "0 8px 16px rgba(0,0,0,0.12)" }}
        whileTap={{ y: 0 }}
        transition={{ type: "spring", stiffness: 400, damping: 25 }}
        {...props}
    >
        {children}
    </motion.div>
);

// 悬停高亮效果（卡片）
export const HoverCard: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        whileHover={{ 
            scale: 1.02,
            boxShadow: "0 8px 24px rgba(0,0,0,0.15)",
            transition: { duration: 0.2 }
        }}
        whileTap={{ scale: 0.98 }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 状态动画 ====================

// 脉冲动画（用于加载状态）
export const Pulse: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        animate={{ opacity: [0.5, 1, 0.5] }}
        transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
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
        animate={{ y: [0, -10, 0] }}
        transition={{ duration: 0.6, repeat: Infinity, ease: "easeInOut" }}
        {...props}
    >
        {children}
    </motion.div>
);

// 抖动动画（用于错误提示）
export const Shake: React.FC<HTMLMotionProps<"div"> & { trigger?: boolean }> = ({ children, trigger, ...props }) => (
    <motion.div
        animate={trigger ? { x: [-10, 10, -10, 10, 0] } : {}}
        transition={{ duration: 0.4 }}
        {...props}
    >
        {children}
    </motion.div>
);

// ==================== 组合动画 ====================

// 渐显放大组合（适用于模态框）
export const FadeInScale: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        transition={{ duration: 0.2, ease: "easeOut" }}
        {...props}
    >
        {children}
    </motion.div>
);

// 渐显向上滑入组合（适用于通知）
export const FadeInSlideUp: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
    <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0, y: -20 }}
        transition={{ type: "spring", stiffness: 500, damping: 35 }}
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
            staggerChildren: 0.08,
            delayChildren: 0.1
        }
    }
};

export const cardItemVariants: Variants = {
    hidden: { opacity: 0, y: 20, scale: 0.95 },
    visible: {
        opacity: 1,
        y: 0,
        scale: 1,
        transition: {
            type: "spring",
            stiffness: 300,
            damping: 24
        }
    }
};

// 页面过渡动画变体
export const pageVariants: Variants = {
    initial: { opacity: 0, x: -20 },
    animate: { 
        opacity: 1, 
        x: 0,
        transition: {
            duration: 0.3,
            ease: "easeOut"
        }
    },
    exit: { 
        opacity: 0, 
        x: 20,
        transition: {
            duration: 0.2
        }
    }
};
