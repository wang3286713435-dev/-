<template>
  <section class="glandar-viewer" :class="{ 'is-embedded': embedded }" v-loading="loading">
    <aside class="glandar-viewer__toolbar" aria-label="模型工具栏">
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('fit')">适配</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('main')">主视角</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('pick')">选择</button>
      <button type="button" :disabled="!viewerReady || !selectedFeatureId" @click="runViewerAction('locate')">定位</button>
      <button type="button" :disabled="!viewerReady || !selectedFeatureId" @click="runViewerAction('hide')">隐藏</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('screenshot')">截图</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('measure-distance')">距离</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('measure-clear')">清除</button>
    </aside>

    <section class="glandar-viewer__canvas-card">
      <div :id="containerId" ref="viewerRef" class="glandar-viewer__canvas" />
      <div v-if="!viewerReady && !loading" class="glandar-viewer__empty">
        <strong>{{ emptyTitle }}</strong>
        <span>{{ emptyDescription }}</span>
      </div>
    </section>

    <aside v-if="showInfo" class="glandar-viewer__info">
      <article>
        <span>任务状态</span>
        <strong>{{ ticket?.statusLabel || '-' }}</strong>
        <em>{{ ticket?.blockedReason || '平台已校验项目权限和 Viewer 入口。' }}</em>
      </article>
      <article>
        <span>轻量化任务</span>
        <strong>{{ ticket?.lightweightName || '-' }}</strong>
        <em>不展示 NAS 路径、对象 key 或引擎 token。</em>
      </article>
      <article>
        <span>操作提示</span>
        <strong>BIM 模式</strong>
        <em>左键旋转，滚轮缩放，右键平移；短按左键可选择构件。</em>
      </article>
      <article v-if="selectedFeatureId">
        <span>当前构件</span>
        <strong>{{ selectedFeatureId }}</strong>
        <em>可定位或隐藏当前选中构件。</em>
      </article>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import {
  issueLightweightViewerTicket,
  type LightweightViewerTicketResponse
} from '@/modules/visualization/api/visualization';
import { useAuthStore } from '@/stores/auth';

declare global {
  interface Window {
    GlendaleEngine?: (options: Record<string, unknown>) => Promise<GlendaleApi>;
  }
}

type GlendaleApi = {
  Model?: {
    add?: (options: Record<string, unknown>) => void | Promise<void>;
    zoomTo?: (options: Record<string, unknown>) => void | Promise<void>;
    setVisible?: (options: Record<string, unknown>) => void | Promise<void>;
  };
  Camera?: {
    transitionsView?: (options: Record<string, unknown>) => void | Promise<void>;
    setStationaryViewPort?: (options: Record<string, unknown>) => void | Promise<void>;
    saveScreenShot?: () => string | Promise<string>;
  };
  Feature?: {
    getByEvent?: (options: Record<string, unknown>) => void | Promise<void>;
    highlight?: (options: Record<string, unknown>) => void | Promise<void>;
    zoomTo?: (options: Record<string, unknown>) => void | Promise<void>;
    setVisible?: (options: Record<string, unknown>) => void | Promise<void>;
  };
  Measurement?: {
    distance?: (options: Record<string, unknown>) => void | Promise<void>;
    area?: (options: Record<string, unknown>) => void | Promise<void>;
    exit?: () => void | Promise<void>;
    clear?: () => void | Promise<void>;
    clearAllTrace?: () => void | Promise<void>;
  };
  Public?: {
    resetEventType?: () => void | Promise<void>;
    changeEventType?: (options: Record<string, unknown>) => void | Promise<void>;
    setMouseActionState?: (options: Record<string, unknown>) => void | Promise<void>;
    clearHandler?: (options: Record<string, unknown>) => void | Promise<void>;
    event?: (options: Record<string, unknown>) => void | Promise<void>;
    requestRender?: () => void;
    self?: {
      GLE_cameraControls?: GlandarCameraControls;
      render?: () => void;
    };
  };
  EventType?: Record<string, number>;
};

type GlandarCameraControls = {
  enabled?: boolean;
  mouseButtons?: Record<string, number>;
  distance?: number;
  _lastDistance?: number;
  _domElement?: HTMLElement;
  connect?: (element: HTMLElement) => void;
  rotate?: (x: number, y: number, enableTransition?: boolean) => void;
  truck?: (x: number, y: number, enableTransition?: boolean) => void;
  dolly?: (distance: number, enableTransition?: boolean) => void;
  update?: (delta?: number) => void;
};

type NavigationDrag = {
  button: number;
  pointerId?: number;
  startedAt: number;
  startX: number;
  startY: number;
  x: number;
  y: number;
};

type GlandarFeature = {
  id?: string | number;
  batchId?: string | number;
};

const props = withDefaults(defineProps<{
  projectId: number;
  jobId: string;
  fileName?: string;
  embedded?: boolean;
  showInfo?: boolean;
  containerId?: string;
}>(), {
  fileName: '',
  embedded: false,
  showInfo: true,
  containerId: ''
});

const emit = defineEmits<{
  readyChange: [ready: boolean];
  ticketChange: [ticket: LightweightViewerTicketResponse | null];
}>();

const authStore = useAuthStore();
const loading = ref(false);
const ticket = ref<LightweightViewerTicketResponse | null>(null);
const errorMessage = ref('');
const viewerReady = ref(false);
const viewerRef = ref<HTMLElement | null>(null);
const selectedFeatureId = ref('');
const instance = getCurrentInstance();
const containerId = computed(() => props.containerId || `glandar-viewer-canvas-${instance?.uid ?? 'inline'}`);
let viewerApi: GlendaleApi | null = null;
let currentModelTag = '';
let navigationDrag: NavigationDrag | null = null;
let disposeViewerInteraction: (() => void) | null = null;

const emptyTitle = computed(() => {
  if (errorMessage.value) return 'Viewer 暂不可用';
  return ticket.value?.statusLabel || 'Viewer 暂不可用';
});
const emptyDescription = computed(() => {
  if (errorMessage.value) return errorMessage.value;
  return ticket.value?.blockedReason || '请确认模型已完成轻量化转换。';
});

onMounted(() => {
  void loadViewer();
});

watch(
  () => [props.projectId, props.jobId],
  () => {
    void loadViewer();
  }
);

onBeforeUnmount(() => {
  cleanupViewerInteraction();
  viewerApi = null;
});

defineExpose({
  loadViewer
});

async function loadViewer() {
  if (!Number.isFinite(props.projectId) || props.projectId <= 0 || !props.jobId) {
    errorMessage.value = '缺少项目或轻量化任务上下文，无法打开模型预览。';
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  viewerReady.value = false;
  emit('readyChange', false);
  try {
    cleanupViewerInteraction();
    viewerApi = null;
    currentModelTag = '';
    selectedFeatureId.value = '';
    viewerRef.value && (viewerRef.value.innerHTML = '');
    await ensureProjectContext(props.projectId);
    const nextTicket = await issueLightweightViewerTicket(props.projectId, props.jobId);
    ticket.value = nextTicket;
    emit('ticketChange', nextTicket);
    if (!nextTicket.viewerAvailable || !nextTicket.modelAccessAddress || !nextTicket.engineStaticBase) {
      errorMessage.value = nextTicket.blockedReason || '模型仍在转换中，暂时无法打开 Viewer。';
      return;
    }
    await loadEngineScript(nextTicket.engineStaticBase);
    await mountModel(nextTicket);
    viewerReady.value = true;
    emit('readyChange', true);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '葛兰岱尔 Viewer 加载失败';
  } finally {
    loading.value = false;
  }
}

async function ensureProjectContext(targetProjectId: number) {
  if (authStore.currentProjectId === targetProjectId) return;
  await authStore.changeProject(targetProjectId);
}

async function loadEngineScript(engineStaticBase: string) {
  if (window.GlendaleEngine) return;
  const base = normalizeEngineStaticBase(engineStaticBase);
  const scriptId = 'glandar-glendale-engine';
  const existing = document.getElementById(scriptId) as HTMLScriptElement | null;
  if (existing) {
    await waitForScript(existing);
    return;
  }
  const script = document.createElement('script');
  script.id = scriptId;
  script.src = `${base}/glendale.v1.umd.js`;
  document.body.appendChild(script);
  await waitForScript(script);
  if (!window.GlendaleEngine) {
    throw new Error('葛兰岱尔引擎脚本已加载，但没有找到 GlendaleEngine。');
  }
}

function waitForScript(script: HTMLScriptElement) {
  return new Promise<void>((resolve, reject) => {
    if (window.GlendaleEngine) {
      resolve();
      return;
    }
    script.addEventListener('load', () => resolve(), { once: true });
    script.addEventListener('error', () => reject(new Error(`无法加载引擎脚本：${script.src}`)), { once: true });
  });
}

async function mountModel(nextTicket: LightweightViewerTicketResponse) {
  const engineStaticBase = normalizeEngineStaticBase(nextTicket.engineStaticBase || '');
  if (!viewerRef.value || !window.GlendaleEngine || !nextTicket.modelAccessAddress) return;
  viewerRef.value.innerHTML = '';
  viewerApi = await window.GlendaleEngine({
    containerID: containerId.value,
    environmentType: 'HDR',
    environmentTypeName: 'evening_road_01_puresky_2k',
    backgroundBlurriness: 0,
    backgroundColor: '#101827',
    selectedColor: 0xffff00,
    frameRate: false,
    logarithmicDepthBuffer: false,
    showSide: 2,
    sitePath: engineStaticBase,
    serverIp: '',
    serverPort: 0,
    useHttps: false,
    modelList: [],
    renderFactor: 1,
    mappingMode: 3,
    secretKey: ''
  });
  await applyMouseNavigation();
  bindViewerInteraction();
  currentModelTag = `glandar-${nextTicket.jobId}`;
  await viewerApi.Model?.add?.({
    url: nextTicket.modelAccessAddress,
    tag: currentModelTag,
    flyto: true,
    readyPromise: () => {
      requestViewerRender();
    },
    callback: async () => {
      await applyMouseNavigation();
      await viewerApi?.Model?.zoomTo?.({ tag: currentModelTag });
      requestViewerRender();
    }
  });
}

async function runViewerAction(action: 'fit' | 'main' | 'screenshot' | 'measure-distance' | 'measure-clear' | 'pick' | 'locate' | 'hide') {
  if (!viewerApi || !ticket.value) return;
  try {
    if (action === 'fit') {
      await viewerApi.Model?.zoomTo?.({ tag: currentModelTag });
      ElMessage.success('已适配当前模型视图');
      return;
    }
    if (action === 'main') {
      await viewerApi.Camera?.transitionsView?.({ showViewMode: '3D', enableTransition: true });
      await viewerApi.Model?.zoomTo?.({ tag: currentModelTag });
      ElMessage.success('已回到主视角');
      return;
    }
    if (action === 'pick') {
      await enableFeaturePick();
      ElMessage.info('已进入构件选择模式，单击模型构件进行选择');
      return;
    }
    if (action === 'locate') {
      await locateSelectedFeature();
      return;
    }
    if (action === 'hide') {
      await hideSelectedFeature();
      return;
    }
    if (action === 'screenshot') {
      const base64 = await viewerApi.Camera?.saveScreenShot?.();
      if (base64) {
        const href = String(base64).startsWith('data:') ? String(base64) : `data:image/png;base64,${base64}`;
        const link = document.createElement('a');
        link.href = href;
        link.download = `glandar-preview-${Date.now()}.png`;
        document.body.appendChild(link);
        link.click();
        link.remove();
      }
      return;
    }
    if (action === 'measure-distance') {
      await viewerApi.Measurement?.distance?.({ mouseMoveAdsorbVertex: true, showXYZ: true });
      ElMessage.info('距离测量已开启');
      return;
    }
    await viewerApi.Measurement?.exit?.();
    if (viewerApi.Measurement?.clearAllTrace) {
      await viewerApi.Measurement.clearAllTrace();
    }
    await viewerApi.Measurement?.clear?.();
    ElMessage.success('测量已清除');
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '当前 Viewer 工具暂不可用');
  }
}

async function applyMouseNavigation() {
  if (!viewerApi?.Public) return;
  const bindings = navigationBindings('bim');
  if (typeof viewerApi.Public.resetEventType === 'function') {
    await viewerApi.Public.resetEventType();
  }
  if (typeof viewerApi.Public.changeEventType === 'function') {
    await viewerApi.Public.changeEventType({
      zoomEventTypes: eventTypeValue(bindings.zoom),
      moveEventTypes: eventTypeValue(bindings.move),
      rotateEventTypes: eventTypeValue(bindings.rotate)
    });
  }
  if (typeof viewerApi.Public.setMouseActionState === 'function') {
    await viewerApi.Public.setMouseActionState({ state: true });
  }
  clearEngineLeftClickHandlers();
  reconnectCameraControls();
}

function bindViewerInteraction() {
  const element = viewerRef.value;
  if (!element) return;
  cleanupViewerInteraction();
  const preventContextMenu = (event: MouseEvent) => event.preventDefault();
  const focusCanvas = () => {
    const canvas = element.querySelector('canvas') as HTMLCanvasElement | null;
    canvas?.focus?.();
  };
  element.addEventListener('contextmenu', preventContextMenu);
  element.addEventListener('pointerdown', focusCanvas, { capture: true });
  element.addEventListener('pointerdown', handleViewerPointerDown);
  element.addEventListener('pointermove', handleViewerPointerMove);
  element.addEventListener('pointerup', handleViewerPointerUp);
  element.addEventListener('pointercancel', handleViewerPointerUp);
  element.addEventListener('wheel', handleViewerWheel, { passive: false });
  disposeViewerInteraction = () => {
    element.removeEventListener('contextmenu', preventContextMenu);
    element.removeEventListener('pointerdown', focusCanvas, { capture: true });
    element.removeEventListener('pointerdown', handleViewerPointerDown);
    element.removeEventListener('pointermove', handleViewerPointerMove);
    element.removeEventListener('pointerup', handleViewerPointerUp);
    element.removeEventListener('pointercancel', handleViewerPointerUp);
    element.removeEventListener('wheel', handleViewerWheel);
  };
}

function cleanupViewerInteraction() {
  navigationDrag = null;
  disposeViewerInteraction?.();
  disposeViewerInteraction = null;
}

function handleViewerPointerDown(event: PointerEvent) {
  if (!viewerApi || !currentModelTag || (event.button !== 0 && event.button !== 2)) return;
  navigationDrag = {
    button: event.button,
    pointerId: event.pointerId,
    startedAt: performance.now(),
    startX: event.clientX,
    startY: event.clientY,
    x: event.clientX,
    y: event.clientY
  };
  try {
    viewerRef.value?.setPointerCapture?.(event.pointerId);
  } catch (error) {
    console.warn(error);
  }
  event.preventDefault();
}

function handleViewerPointerMove(event: PointerEvent) {
  if (!navigationDrag) return;
  if (navigationDrag.pointerId !== undefined && event.pointerId !== navigationDrag.pointerId) return;
  const controls = getCameraControls();
  if (!controls) return;

  const dx = event.clientX - navigationDrag.x;
  const dy = event.clientY - navigationDrag.y;
  navigationDrag.x = event.clientX;
  navigationDrag.y = event.clientY;
  if (dx === 0 && dy === 0) return;

  const bindings = navigationBindings('bim');
  const dragName = navigationDrag.button === 0 ? 'LEFT_DRAG' : 'RIGHT_DRAG';

  if (dragName === bindings.rotate && typeof controls.rotate === 'function') {
    controls.rotate(-dx * 0.006, -dy * 0.006, false);
  } else if (dragName === bindings.move && typeof controls.truck === 'function') {
    const distance = Number(controls.distance || controls._lastDistance || 100);
    const delta = panTruckDelta({
      dx,
      dy,
      distance,
      width: viewerRef.value?.clientWidth || 1,
      height: viewerRef.value?.clientHeight || 1
    });
    controls.truck(delta.x, delta.y, false);
  }

  updateCameraControls();
  event.preventDefault();
}

async function handleViewerPointerUp(event: PointerEvent) {
  if (!navigationDrag) return;
  if (navigationDrag.pointerId !== undefined && event.pointerId !== navigationDrag.pointerId) return;
  const drag = navigationDrag;
  navigationDrag = null;
  try {
    viewerRef.value?.releasePointerCapture?.(event.pointerId);
  } catch (error) {
    console.warn(error);
  }
  const shouldPick = event.type !== 'pointercancel' && pointerPickIntent({
    button: drag.button,
    startedAt: drag.startedAt,
    endedAt: performance.now(),
    startX: drag.startX,
    startY: drag.startY,
    endX: event.clientX,
    endY: event.clientY
  });
  if (shouldPick) {
    await pickFeatureFromPointer(event);
  }
  event.preventDefault();
}

function handleViewerWheel(event: WheelEvent) {
  if (!viewerApi || !currentModelTag) return;
  const controls = getCameraControls();
  if (!controls || typeof controls.dolly !== 'function') return;
  const distance = Number(controls.distance || controls._lastDistance || 100);
  const step = Math.max(distance * 0.0012, 0.08);
  controls.dolly(-event.deltaY * step, false);
  updateCameraControls();
  event.preventDefault();
}

async function pickFeatureFromPointer(event: PointerEvent) {
  if (!viewerApi?.Feature?.getByEvent) return;
  const position = screenToEnginePickPosition(event);
  await viewerApi.Feature.getByEvent({
    position,
    callback: (feature: GlandarFeature | null) => {
      if (feature?.id) {
        selectedFeatureId.value = String(feature.id);
        void viewerApi?.Feature?.highlight?.({
          featureIds: feature.id,
          batchId: feature.batchId,
          tag: currentModelTag
        });
        ElMessage.success('已选择构件');
      } else {
        selectedFeatureId.value = '';
      }
    }
  });
}

async function enableFeaturePick() {
  if (!viewerApi?.Public?.event) return;
  clearEngineLeftClickHandlers();
  await viewerApi.Public.event({
    event: 'LEFT_CLICK',
    callback: (click: { position?: Record<string, unknown> }) => {
      if (!viewerApi?.Feature?.getByEvent) return;
      void viewerApi.Feature.getByEvent({
        position: click.position,
        callback: (feature: GlandarFeature | null) => {
          if (feature?.id) {
            selectedFeatureId.value = String(feature.id);
            void viewerApi?.Feature?.highlight?.({
              featureIds: feature.id,
              batchId: feature.batchId,
              tag: currentModelTag
            });
          }
        }
      });
    }
  });
}

async function locateSelectedFeature() {
  if (!selectedFeatureId.value || !viewerApi?.Feature?.zoomTo) return;
  await viewerApi.Feature.zoomTo({
    featureIds: selectedFeatureId.value,
    tag: currentModelTag
  });
  ElMessage.success('已定位到选中构件');
}

async function hideSelectedFeature() {
  if (!selectedFeatureId.value || !viewerApi?.Feature?.setVisible) return;
  await viewerApi.Feature.setVisible({
    featureIds: selectedFeatureId.value,
    visible: false,
    tag: currentModelTag
  });
  selectedFeatureId.value = '';
  ElMessage.success('已隐藏选中构件');
}

function clearEngineLeftClickHandlers() {
  if (!viewerApi?.Public?.clearHandler) return;
  try {
    void viewerApi.Public.clearHandler({ event: 'LEFT_CLICK' });
  } catch (error) {
    console.warn(error);
  }
}

function reconnectCameraControls() {
  const controls = getCameraControls();
  if (!controls) return;
  controls.enabled = true;
  if (controls.mouseButtons) {
    controls.mouseButtons.left = 1;
    controls.mouseButtons.right = 2;
    controls.mouseButtons.wheel = 8;
  }
  const canvas = viewerRef.value?.querySelector('canvas') as HTMLElement | null;
  const domElement = controls._domElement || canvas || viewerRef.value;
  if (domElement && typeof controls.connect === 'function') {
    controls.connect(domElement);
  }
}

function getCameraControls() {
  return viewerApi?.Public?.self?.GLE_cameraControls;
}

function requestViewerRender() {
  if (typeof viewerApi?.Public?.requestRender === 'function') {
    viewerApi.Public.requestRender();
    return;
  }
  viewerApi?.Public?.self?.render?.();
}

function updateCameraControls() {
  const controls = getCameraControls();
  controls?.update?.(0.016);
  requestViewerRender();
}

function navigationBindings(mode: 'bim' | 'cad') {
  return mode === 'cad'
    ? { zoom: 'WHEEL', move: 'LEFT_DRAG', rotate: 'RIGHT_DRAG' }
    : { zoom: 'WHEEL', move: 'RIGHT_DRAG', rotate: 'LEFT_DRAG' };
}

function eventTypeValue(name: string) {
  const fallbackEventTypes: Record<string, number> = {
    LEFT_DRAG: 0,
    RIGHT_DRAG: 1,
    MIDDLE_DRAG: 2,
    WHEEL: 3,
    PINCH: 4
  };
  const eventTypes = (window as Window & { EventType?: Record<string, number> }).EventType || viewerApi?.EventType;
  return eventTypes?.[name] ?? fallbackEventTypes[name];
}

function panTruckDelta(options: { dx: number; dy: number; distance: number; width: number; height: number }) {
  const scale = Math.max(options.distance, 1) / Math.max(options.width, options.height, 1) * 1.6;
  return {
    x: -options.dx * scale,
    y: -options.dy * scale,
    scale
  };
}

function pointerPickIntent(options: {
  button: number;
  startedAt: number;
  endedAt: number;
  startX: number;
  startY: number;
  endX: number;
  endY: number;
}) {
  const distance = Math.hypot(options.endX - options.startX, options.endY - options.startY);
  const duration = options.endedAt - options.startedAt;
  return options.button === 0 && duration >= 0 && duration <= 260 && distance <= 5;
}

function screenToEnginePickPosition(event: PointerEvent) {
  return {
    x: event.clientX,
    y: event.clientY
  };
}

function normalizeEngineStaticBase(value: string) {
  const trimmed = value.replace(/\/$/, '');
  if (trimmed.endsWith('/static/ThreeJsEngine')) return trimmed;
  if (trimmed.endsWith('/ThreeJsEngine')) return trimmed;
  return `${trimmed}/static/ThreeJsEngine`;
}
</script>

<style scoped>
.glandar-viewer {
  background: var(--zy-surface);
  border: 1px solid color-mix(in srgb, var(--zy-line) 72%, transparent);
  border-radius: var(--zy-radius-base);
  box-shadow: var(--zy-shadow-xs);
  display: grid;
  gap: var(--zy-sp-3);
  grid-template-columns: 76px minmax(0, 1fr) 260px;
  height: min(720px, calc(100dvh - 220px));
  min-height: min(720px, calc(100dvh - 220px));
  min-width: 0;
  overflow: hidden;
  padding: var(--zy-sp-3);
}

.glandar-viewer.is-embedded {
  height: clamp(560px, 64dvh, 760px);
  min-height: 560px;
}

.glandar-viewer__toolbar {
  align-content: start;
  display: grid;
  gap: 8px;
}

.glandar-viewer__toolbar button {
  background: color-mix(in srgb, var(--zy-blue-600) 10%, var(--zy-surface));
  border: 1px solid color-mix(in srgb, var(--zy-blue-600) 20%, transparent);
  border-radius: 8px;
  color: var(--zy-ink);
  cursor: pointer;
  font-size: var(--zy-fs-xs);
  min-height: 40px;
}

.glandar-viewer__toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.glandar-viewer__canvas-card {
  background:
    radial-gradient(circle at 20% 20%, rgba(37, 99, 235, 0.18), transparent 30%),
    #101827;
  border-radius: 10px;
  height: 100%;
  min-height: 520px;
  min-width: 0;
  overflow: hidden;
  position: relative;
}

.glandar-viewer__canvas {
  inset: 0;
  overflow: hidden;
  position: absolute;
  touch-action: none;
}

.glandar-viewer__empty {
  align-items: center;
  color: #dbeafe;
  display: grid;
  gap: 8px;
  inset: 0;
  justify-items: center;
  padding: var(--zy-sp-5);
  position: absolute;
  text-align: center;
}

.glandar-viewer__empty strong {
  font-size: var(--zy-fs-xl);
}

.glandar-viewer__info {
  align-content: start;
  display: grid;
  gap: var(--zy-sp-3);
}

.glandar-viewer__info article {
  background: var(--zy-surface);
  border: 1px solid color-mix(in srgb, var(--zy-line) 72%, transparent);
  border-radius: var(--zy-radius-base);
  box-shadow: var(--zy-shadow-xs);
  display: grid;
  gap: 6px;
  padding: var(--zy-sp-3);
}

.glandar-viewer__info span {
  color: var(--zy-blue-600);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.glandar-viewer__info strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
}

.glandar-viewer__info em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  font-style: normal;
}

@media (max-width: 1180px) {
  .glandar-viewer {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .glandar-viewer__info {
    grid-column: 1 / -1;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
