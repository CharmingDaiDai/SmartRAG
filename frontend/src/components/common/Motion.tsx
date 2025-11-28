import React from 'react';
import { motion, HTMLMotionProps } from 'framer-motion';

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
