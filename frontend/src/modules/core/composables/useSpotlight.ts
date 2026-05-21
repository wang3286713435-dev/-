import { onBeforeUnmount, onMounted, type Ref } from 'vue';

/**
 * 鼠标追踪光斑：在容器上 listen pointermove，更新容器 CSS variables
 *   --spot-x, --spot-y, --pointer-x, --pointer-y
 *
 * 配合 .zy-spotlight 或自定义 background 使用，呈现"光跟手"效果。
 * 在 prefers-reduced-motion 下自动停用。
 */
export function useSpotlight(target: Ref<HTMLElement | null>) {
  let raf = 0;
  let lastX = 50;
  let lastY = 24;
  let prefersReducedMotion = false;
  let mq: MediaQueryList | null = null;
  let mqListener: ((event: MediaQueryListEvent) => void) | null = null;

  function apply(x: number, y: number) {
    const el = target.value;
    if (!el) return;
    el.style.setProperty('--spot-x', `${x}%`);
    el.style.setProperty('--spot-y', `${y}%`);
    el.style.setProperty('--pointer-x', `${x}%`);
    el.style.setProperty('--pointer-y', `${y}%`);
  }

  function handle(event: PointerEvent) {
    const el = target.value;
    if (!el || prefersReducedMotion) return;
    const rect = el.getBoundingClientRect();
    const x = ((event.clientX - rect.left) / rect.width) * 100;
    const y = ((event.clientY - rect.top) / rect.height) * 100;
    lastX = Math.max(0, Math.min(100, x));
    lastY = Math.max(0, Math.min(100, y));
    if (!raf) {
      raf = requestAnimationFrame(() => {
        raf = 0;
        apply(lastX, lastY);
      });
    }
  }

  onMounted(() => {
    mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    prefersReducedMotion = mq.matches;
    mqListener = (event: MediaQueryListEvent) => {
      prefersReducedMotion = event.matches;
    };
    mq.addEventListener?.('change', mqListener);

    const el = target.value;
    if (el) {
      apply(50, 24);
      el.addEventListener('pointermove', handle, { passive: true });
    }
  });

  onBeforeUnmount(() => {
    const el = target.value;
    if (el) el.removeEventListener('pointermove', handle);
    if (raf) cancelAnimationFrame(raf);
    if (mq && mqListener) mq.removeEventListener?.('change', mqListener);
  });
}
