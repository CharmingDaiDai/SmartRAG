import { useRef } from 'react';
import { motion, useInView } from 'framer-motion';
import { useAppStore } from '../store/useAppStore';

interface ScrollRevealProps {
  children: React.ReactNode;
  delay?: number;
  className?: string;
  style?: React.CSSProperties;
}

export default function ScrollReveal({ children, delay = 0, className, style }: ScrollRevealProps) {
  const { uiStyle } = useAppStore();
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true, margin: '-50px' });

  // Non-fancy mode: pass through without animation
  if (uiStyle !== 'fancy') {
    return <div className={className} style={style}>{children}</div>;
  }

  return (
    <motion.div
      ref={ref}
      className={className}
      style={style}
      initial={{ opacity: 0, y: 30 }}
      animate={isInView ? { opacity: 1, y: 0 } : { opacity: 0, y: 30 }}
      transition={{
        duration: 0.5,
        delay,
        ease: [0.34, 1.56, 0.64, 1],
      }}
    >
      {children}
    </motion.div>
  );
}
