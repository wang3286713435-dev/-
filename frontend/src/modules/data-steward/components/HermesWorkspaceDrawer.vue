<template>
  <el-drawer
    v-model="drawerVisible"
    class="hermes-workspace-drawer"
    :size="drawerSize"
    :with-header="false"
    direction="rtl"
  >
    <section class="hermes-workspace-drawer__shell" aria-label="Hermes 会话工作区">
      <button
        class="hermes-workspace-drawer__resize"
        type="button"
        aria-label="调整 Hermes 抽屉宽度"
        title="拖动调整宽度"
        @pointerdown.prevent="startResize"
      />
      <header class="hermes-workspace-drawer__head">
        <div>
          <span>Hermes Enterprise Kernel</span>
          <strong>{{ title }}</strong>
        </div>
        <div class="hermes-workspace-drawer__tools">
          <el-button size="small" text @click="resetWidth">重置宽度</el-button>
          <el-button :icon="Close" aria-label="关闭 Hermes 抽屉" circle text @click="drawerVisible = false" />
        </div>
      </header>
      <slot />
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const props = withDefaults(defineProps<{
  modelValue: boolean;
  title?: string;
  storageKey?: string;
}>(), {
  title: 'Hermes 会话工作区',
  storageKey: 'delivery-platform/hermes-drawer-width'
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const MIN_WIDTH = 520;
const DEFAULT_WIDTH = 760;
const width = ref(DEFAULT_WIDTH);
let resizing = false;

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});

const drawerSize = computed(() => `min(${width.value}px, 100vw)`);

onMounted(() => {
  const saved = Number(window.localStorage.getItem(props.storageKey));
  if (Number.isFinite(saved) && saved >= MIN_WIDTH) {
    width.value = clampWidth(saved);
  }
});

onBeforeUnmount(() => {
  stopResize();
});

function clampWidth(value: number) {
  const max = Math.max(MIN_WIDTH, Math.min(1120, window.innerWidth - 24));
  return Math.min(max, Math.max(MIN_WIDTH, Math.round(value)));
}

function startResize(event: PointerEvent) {
  resizing = true;
  (event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId);
  window.addEventListener('pointermove', resize);
  window.addEventListener('pointerup', stopResize);
}

function resize(event: PointerEvent) {
  if (!resizing) return;
  width.value = clampWidth(window.innerWidth - event.clientX);
}

function stopResize() {
  if (!resizing) return;
  resizing = false;
  window.removeEventListener('pointermove', resize);
  window.removeEventListener('pointerup', stopResize);
  window.localStorage.setItem(props.storageKey, String(width.value));
}

function resetWidth() {
  width.value = clampWidth(DEFAULT_WIDTH);
  window.localStorage.setItem(props.storageKey, String(width.value));
}
</script>

<style scoped>
:global(.hermes-workspace-drawer .el-drawer__body) {
  overflow: hidden;
  padding: 0;
}

.hermes-workspace-drawer__shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-width: 0;
  position: relative;
}

.hermes-workspace-drawer__resize {
  background: transparent;
  border: 0;
  cursor: col-resize;
  height: 100%;
  left: 0;
  padding: 0;
  position: absolute;
  top: 0;
  width: 10px;
  z-index: 2;
}

.hermes-workspace-drawer__resize::after {
  background: var(--el-border-color);
  border-radius: 999px;
  content: '';
  height: 48px;
  left: 4px;
  opacity: 0;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  transition: opacity 160ms ease-out;
  width: 2px;
}

.hermes-workspace-drawer__resize:hover::after,
.hermes-workspace-drawer__resize:focus-visible::after {
  opacity: 1;
}

.hermes-workspace-drawer__head {
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-lighter);
  display: flex;
  gap: 12px;
  justify-content: space-between;
  min-width: 0;
  padding: 16px 20px 14px 24px;
}

.hermes-workspace-drawer__head > div:first-child {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.hermes-workspace-drawer__head span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.3;
}

.hermes-workspace-drawer__head strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
  line-height: 1.3;
}

.hermes-workspace-drawer__tools {
  align-items: center;
  display: flex;
  gap: 4px;
}

@media (max-width: 640px) {
  .hermes-workspace-drawer__resize,
  .hermes-workspace-drawer__tools .el-button:first-child {
    display: none;
  }

  .hermes-workspace-drawer__head {
    padding: 14px 14px 12px;
  }
}
</style>
