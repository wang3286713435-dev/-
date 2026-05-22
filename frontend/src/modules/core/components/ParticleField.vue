<template>
  <canvas
    ref="canvasRef"
    class="zy-particle-field"
    :class="{ 'is-overlay': overlay }"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

interface Props {
  count?: number;
  /** primary color rgba string, low saturation */
  color?: string;
  /** secondary color used when linking */
  linkColor?: string;
  /** max link distance in CSS pixels */
  linkDistance?: number;
  /** base point radius in CSS pixels */
  radius?: number;
  /** speed factor */
  speed?: number;
  /** whether to render as absolute overlay (defaults true) */
  overlay?: boolean;
  /** disable when true */
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  count: 18,
  color: 'rgba(37, 99, 235, 0.55)',
  linkColor: 'rgba(37, 99, 235, 0.18)',
  linkDistance: 140,
  radius: 1.4,
  speed: 0.32,
  overlay: true,
  disabled: false
});

const canvasRef = ref<HTMLCanvasElement | null>(null);

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  r: number;
}

let particles: Particle[] = [];
let rafId = 0;
let ro: ResizeObserver | null = null;
let dpr = 1;
let stopped = false;
let prefersReducedMotion = false;
let reducedMotionMq: MediaQueryList | null = null;
let reducedMotionListener: ((event: MediaQueryListEvent) => void) | null = null;

function resize() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  const w = Math.max(1, Math.floor(rect.width));
  const h = Math.max(1, Math.floor(rect.height));
  dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  const ctx = canvas.getContext('2d');
  if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}

function seed() {
  const canvas = canvasRef.value;
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  const w = rect.width;
  const h = rect.height;
  particles = [];
  for (let i = 0; i < props.count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * props.speed,
      vy: (Math.random() - 0.5) * props.speed,
      r: props.radius * (0.7 + Math.random() * 0.6)
    });
  }
}

function step() {
  const canvas = canvasRef.value;
  if (!canvas || stopped) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  const rect = canvas.getBoundingClientRect();
  const w = rect.width;
  const h = rect.height;

  ctx.clearRect(0, 0, w, h);

  // Update positions
  for (const p of particles) {
    p.x += p.vx;
    p.y += p.vy;
    if (p.x < -10) p.x = w + 10;
    if (p.x > w + 10) p.x = -10;
    if (p.y < -10) p.y = h + 10;
    if (p.y > h + 10) p.y = -10;
  }

  // Draw links (between nearby pairs)
  ctx.lineWidth = 0.6;
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const a = particles[i];
      const b = particles[j];
      const dx = a.x - b.x;
      const dy = a.y - b.y;
      const d2 = dx * dx + dy * dy;
      const max2 = props.linkDistance * props.linkDistance;
      if (d2 < max2) {
        const alpha = 1 - d2 / max2;
        ctx.strokeStyle = props.linkColor.replace(/[\d.]+\)$/, `${(alpha * 0.6).toFixed(3)})`);
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(b.x, b.y);
        ctx.stroke();
      }
    }
  }

  // Draw points
  ctx.fillStyle = props.color;
  for (const p of particles) {
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
    ctx.fill();
  }

  rafId = requestAnimationFrame(step);
}

function start() {
  if (props.disabled || prefersReducedMotion) return;
  if (rafId) cancelAnimationFrame(rafId);
  stopped = false;
  resize();
  seed();
  rafId = requestAnimationFrame(step);
}

function stop() {
  stopped = true;
  if (rafId) cancelAnimationFrame(rafId);
  rafId = 0;
  const canvas = canvasRef.value;
  if (canvas) {
    const ctx = canvas.getContext('2d');
    if (ctx) ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
}

onMounted(() => {
  reducedMotionMq = window.matchMedia('(prefers-reduced-motion: reduce)');
  prefersReducedMotion = reducedMotionMq.matches;
  reducedMotionListener = (event: MediaQueryListEvent) => {
    prefersReducedMotion = event.matches;
    if (prefersReducedMotion) {
      stop();
    } else if (!props.disabled) {
      start();
    }
  };
  reducedMotionMq.addEventListener?.('change', reducedMotionListener);

  if (canvasRef.value && 'ResizeObserver' in window) {
    ro = new ResizeObserver(() => {
      resize();
      seed();
    });
    ro.observe(canvasRef.value);
  }

  start();
});

watch(
  () => props.disabled,
  (next) => {
    if (next) stop();
    else start();
  }
);

onBeforeUnmount(() => {
  stop();
  ro?.disconnect();
  ro = null;
  if (reducedMotionMq && reducedMotionListener) {
    reducedMotionMq.removeEventListener?.('change', reducedMotionListener);
  }
});
</script>

<style scoped>
.zy-particle-field {
  display: block;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.zy-particle-field.is-overlay {
  position: absolute;
  inset: 0;
  z-index: 0;
}
</style>
