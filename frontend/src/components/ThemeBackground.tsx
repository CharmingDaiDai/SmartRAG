import { useEffect, useState } from 'react';

export default function ThemeBackground() {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) {
      return;
    }

    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const update = () => setPrefersReducedMotion(mediaQuery.matches);

    update();

    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', update);
      return () => mediaQuery.removeEventListener('change', update);
    }

    mediaQuery.addListener(update);
    return () => mediaQuery.removeListener(update);
  }, []);

  return (
    <div
      className={`app-ambient-bg${prefersReducedMotion ? ' app-ambient-bg-reduced' : ''}`}
      aria-hidden="true"
    >
      <div className="app-ambient-orb app-ambient-orb-one" />
      <div className="app-ambient-orb app-ambient-orb-two" />
    </div>
  );
}
