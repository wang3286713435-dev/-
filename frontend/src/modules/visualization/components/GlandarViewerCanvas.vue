<template>
  <section
    ref="viewerShellRef"
    class="glandar-viewer"
    :class="{ 'is-embedded': embedded, 'is-light': theme === 'light', 'is-app-fullscreen': fullscreenActive }"
    v-loading="loading"
  >
    <aside class="glandar-viewer__toolbar" aria-label="模型工具栏">
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('main')">主视角</button>
      <button
        type="button"
        :disabled="!viewerReady || !componentInteractionAvailable"
        :title="componentInteractionUnavailableReason"
        @click="runViewerAction('blow', { mode: 'SPHERE', amount: 0.45 })"
      >
        爆炸
      </button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('model-visible-toggle')">显示</button>
      <button type="button" :disabled="!viewerReady || !componentInteractionAvailable || !selectedFeatureId" :title="componentInteractionUnavailableReason" @click="runViewerAction('feature-visible-toggle')">隐藏</button>
      <button type="button" :disabled="!viewerReady || !componentInteractionAvailable || !selectedFeatureId" :title="componentInteractionUnavailableReason" @click="runViewerAction('locate')">定位</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('measure-distance')">距离</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('measure-area')">面积</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('measure-clear')">清除</button>
      <button type="button" :disabled="!viewerReady" @click="runViewerAction('fullscreen')">
        {{ fullscreenActive ? '退出' : '全屏' }}
      </button>
    </aside>

    <section class="glandar-viewer__canvas-card">
      <div :id="containerId" ref="viewerRef" class="glandar-viewer__canvas" />
      <div v-if="!viewerReady && !loading" class="glandar-viewer__empty">
        <strong>{{ emptyTitle }}</strong>
        <span>{{ emptyDescription }}</span>
      </div>
      <div v-if="selectedFeatureId" class="glandar-viewer__feature-popover" aria-live="polite">
        <header>
          <span>当前选中构件</span>
          <button type="button" @click="runViewerAction('clear-selection')">清除</button>
        </header>
        <strong>{{ selectedFeatureId }}</strong>
        <dl>
          <div>
            <dt>Revit ID</dt>
            <dd>{{ selectedFeatureRevitId || '引擎未返回' }}</dd>
          </div>
          <div>
            <dt>所属模型</dt>
            <dd>{{ fileName || ticket?.lightweightName || '-' }}</dd>
          </div>
          <div>
            <dt>文件 ID</dt>
            <dd>{{ modelFileIdText }}</dd>
          </div>
          <div>
            <dt>批次信息</dt>
            <dd>{{ selectedFeatureBatchText }}</dd>
          </div>
        </dl>
        <section class="glandar-viewer__feature-properties" aria-label="构件属性">
          <div class="glandar-viewer__feature-properties-head">
            <span>构件属性</span>
            <em v-if="featurePropertiesLoading">读取中</em>
            <em v-else>{{ selectedFeaturePropertyCountText }}</em>
          </div>
          <p v-if="featurePropertiesError">{{ featurePropertiesError }}</p>
          <template v-else-if="selectedFeatureProperties?.groups?.length">
            <article v-for="group in selectedFeatureProperties.groups.slice(0, 4)" :key="`${group.groupName}-${group.setName}`">
              <strong>{{ group.setName || group.groupName }}</strong>
              <dl>
                <div v-for="item in group.properties.slice(0, 6)" :key="`${group.groupName}-${group.setName}-${item.name}`">
                  <dt>{{ item.name }}</dt>
                  <dd>{{ item.value || '-' }}</dd>
                </div>
              </dl>
            </article>
          </template>
          <p v-else>已拾取构件，暂无可展示属性；可能是该模型未生成属性库。</p>
        </section>
        <div class="glandar-viewer__feature-actions">
          <button type="button" @click="runViewerAction('locate')">定位</button>
          <button type="button" @click="runViewerAction('hide')">隐藏</button>
          <button type="button" @click="runViewerAction('measure-feature-area')">面积</button>
          <button type="button" @click="runViewerAction('measure-feature-volume')">体积</button>
        </div>
      </div>
      <div v-if="lastMeasurementText" class="glandar-viewer__measurement" aria-live="polite">
        <span>测量结果</span>
        <strong>{{ lastMeasurementText }}</strong>
      </div>
      <div v-if="viewerReady && !componentInteractionAvailable" class="glandar-viewer__capability-note" aria-live="polite">
        <strong>当前仅支持整体模型预览</strong>
        <span>{{ componentInteractionUnavailableReason }}</span>
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
        <em>{{ componentInteractionAvailable ? '左键旋转，滚轮缩放，右键平移；短按左键可选择构件。' : '左键旋转，滚轮缩放，右键平移；当前产物未启用构件拾取。' }}</em>
      </article>
      <article v-if="!componentInteractionAvailable">
        <span>构件级能力</span>
        <strong>待引擎产物支持</strong>
        <em>{{ componentInteractionUnavailableReason }}</em>
      </article>
      <article v-if="selectedFeatureId">
        <span>当前构件</span>
        <strong>{{ selectedFeatureId }}</strong>
        <em>{{ selectedFeatureBatchText }} · 可定位或隐藏当前选中构件。</em>
      </article>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import {
  fetchGlandarComponentProperties,
  issueLightweightViewerTicket,
  type GlandarComponentPropertyResponse,
  type LightweightViewerTicketResponse
} from '@/modules/visualization/api/visualization';
import { useAuthStore } from '@/stores/auth';

declare global {
  interface Window {
    GlendaleEngine?: (options: Record<string, unknown>) => Promise<GlendaleApi>;
    __zhuoyuGlandarWorkerBridgeInstalled?: boolean;
    __zhuoyuNativeWorker?: typeof Worker;
  }
}

type GlendaleApi = {
  Model?: {
    add?: (options: Record<string, unknown>) => void | Promise<void>;
    zoomTo?: (options: Record<string, unknown>) => void | Promise<void>;
    setVisible?: (options: Record<string, unknown>) => void | Promise<void>;
    blow?: (options: Record<string, unknown>) => void | Promise<void>;
    clip?: (options: Record<string, unknown>) => void | Promise<void>;
    closeClip?: (options: Record<string, unknown>) => void | Promise<void>;
  };
  Camera?: {
    transitionsView?: (options: Record<string, unknown>) => void | Promise<void>;
    setStationaryViewPort?: (options: Record<string, unknown>) => void | Promise<void>;
    saveScreenShot?: () => string | Promise<string>;
    getViewPort?: () => Record<string, unknown> | Promise<Record<string, unknown>>;
    setImmersiveRoamConfig?: (options: Record<string, unknown>) => void | Promise<void>;
    startImmersiveRoam?: (options: Record<string, unknown>) => void | Promise<void>;
    pauseImmersiveRoam?: () => void | Promise<void>;
    continueImmersiveRoam?: () => void | Promise<void>;
    stopImmersiveRoam?: () => void | Promise<void>;
  };
  Feature?: {
    getByEvent?: (options: Record<string, unknown>) => void | Promise<void>;
    highlight?: (options: Record<string, unknown>) => void | Promise<void>;
    zoomTo?: (options: Record<string, unknown>) => void | Promise<void>;
    setVisible?: (options: Record<string, unknown>) => void | Promise<void>;
    setColor?: (options: Record<string, unknown>) => void | Promise<void>;
  };
  Measurement?: {
    distance?: (options: Record<string, unknown>) => void | Promise<void>;
    area?: (options: Record<string, unknown>) => void | Promise<void>;
    pointToFace?: (options: Record<string, unknown>) => void | Promise<void>;
    featureArea?: (options: Record<string, unknown>) => void | Promise<void>;
    featureVolume?: (options: Record<string, unknown>) => void | Promise<void>;
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
    pickupCoordinate?: (options: Record<string, unknown>) => Promise<Record<string, unknown>>;
    self?: Record<string, unknown> & {
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

type GlandarRendererLike = {
  setClearColor?: (color: string | number | unknown, alpha?: number) => void;
  setClearAlpha?: (alpha: number) => void;
  clearColor?: () => void;
};

type GlandarSceneLike = {
  background?: unknown;
  environment?: unknown;
  fog?: unknown;
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
  featureId?: string | number;
  FeatureId?: string | number;
  externalId?: string | number;
  ExternalId?: string | number;
  objectId?: string | number;
  componentId?: string | number;
  batchId?: string | number | Array<string | number>;
  BatchId?: string | number | Array<string | number>;
  revitId?: string | number;
  RevitId?: string | number;
};

type ViewerAction =
  | 'fit'
  | 'main'
  | 'blow'
  | 'model-visible-toggle'
  | 'feature-visible-toggle'
  | 'top'
  | 'front'
  | 'left'
  | 'right'
  | 'screenshot'
  | 'measure-distance'
  | 'measure-area'
  | 'measure-feature-area'
  | 'measure-feature-volume'
  | 'measure-clear'
  | 'pick'
  | 'locate'
  | 'hide'
  | 'show-hidden'
  | 'roam-start'
  | 'roam-pause'
  | 'roam-stop'
  | 'clip'
  | 'clip-clear'
  | 'clear-selection'
  | 'fullscreen'
  | 'clear';

type ViewerActionPayload = {
  mode?: 'SPHERE' | 'LINEAR' | 'sphere' | 'linear';
  amount?: number;
  durationMs?: number;
};

const props = withDefaults(defineProps<{
  projectId: number;
  jobId: string;
  fileName?: string;
  embedded?: boolean;
  autoRotate?: boolean;
  theme?: 'dark' | 'light';
  showInfo?: boolean;
  containerId?: string;
  modelFileId?: string | number;
}>(), {
  fileName: '',
  embedded: false,
  autoRotate: false,
  theme: 'dark',
  showInfo: true,
  containerId: '',
  modelFileId: ''
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
const viewerShellRef = ref<HTMLElement | null>(null);
const viewerRef = ref<HTMLElement | null>(null);
const selectedFeatureId = ref('');
const selectedFeature = ref<GlandarFeature | null>(null);
const selectedFeatureProperties = ref<GlandarComponentPropertyResponse | null>(null);
const featurePropertiesLoading = ref(false);
const featurePropertiesError = ref('');
const engineComponentApiReady = ref(false);
const fullscreenActive = ref(false);
const instance = getCurrentInstance();
const containerId = computed(() => props.containerId || `glandar-viewer-canvas-${instance?.uid ?? 'inline'}`);
const selectedFeatureBatchText = computed(() => {
  const batchId = selectedFeature.value?.batchId;
  if (Array.isArray(batchId)) return `批次 ${batchId.join(', ')}`;
  return batchId ? `批次 ${batchId}` : '未返回批次信息';
});
const selectedFeatureRevitId = computed(() => {
  const revitId = selectedFeature.value?.revitId;
  if (revitId !== undefined && revitId !== null && String(revitId)) return String(revitId);
  const featureId = selectedFeatureId.value;
  const parts = featureId.split('^');
  return parts.length > 1 ? parts[parts.length - 1] : '';
});
const modelFileIdText = computed(() => String(props.modelFileId || '见平台模型清单'));
const componentInteractionAvailable = computed(() => {
  const currentTicket = ticket.value as (LightweightViewerTicketResponse & Record<string, unknown>) | null;
  return (viewerReady.value && engineComponentApiReady.value)
    || currentTicket?.componentIndexAvailable === true
    || currentTicket?.featureIndexAvailable === true
    || currentTicket?.featurePickingAvailable === true
    || currentTicket?.modelExplosionAvailable === true;
});
const componentInteractionUnavailableReason = computed(() => (
  '当前 Viewer 尚未返回可用构件交互 API，暂不能拾取构件或执行模型爆炸。'
));
const selectedFeaturePropertyCountText = computed(() => {
  if (!selectedFeatureProperties.value) return '未读取';
  return selectedFeatureProperties.value.propertyAvailable
    ? `${selectedFeatureProperties.value.propertyCount} 项`
    : '无属性';
});
const lastMeasurementText = ref('');
let viewerApi: GlendaleApi | null = null;
let currentModelTag = '';
let modelTagAliases: string[] = [];
let navigationDrag: NavigationDrag | null = null;
let disposeViewerInteraction: (() => void) | null = null;
let autoRotateFrame = 0;
let autoRotatePausedUntil = 0;
let blowAnimationFrame = 0;
let currentBlowAmount = 0;
let currentBlowMode: 'SPHERE' | 'LINEAR' = 'SPHERE';
let roamRunning = false;
let clipEnabled = false;
let modelVisible = true;
let activeInteractionMode: 'navigate' | 'pick' | 'measure' | 'roam' = 'navigate';
const hiddenFeatureIds = new Set<string>();

const emptyTitle = computed(() => {
  if (errorMessage.value) return 'Viewer 暂不可用';
  return ticket.value?.statusLabel || 'Viewer 暂不可用';
});
const emptyDescription = computed(() => {
  if (errorMessage.value) return errorMessage.value;
  return ticket.value?.blockedReason || '请确认模型已完成轻量化转换。';
});

onMounted(() => {
  if (props.embedded) {
    window.addEventListener('message', handleHostMessage);
  }
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  document.addEventListener('keydown', handleFullscreenKeydown);
  void loadViewer();
});

watch(
  () => [props.projectId, props.jobId],
  () => {
    void loadViewer();
  }
);

watch(
  () => props.autoRotate,
  () => updateAutoRotate()
);

onBeforeUnmount(() => {
  window.removeEventListener('message', handleHostMessage);
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
  document.removeEventListener('keydown', handleFullscreenKeydown);
  stopAutoRotate();
  stopBlowAnimation();
  cleanupViewerInteraction();
  viewerApi = null;
});

defineExpose({
  loadViewer,
  runViewerAction
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
    stopAutoRotate();
    stopBlowAnimation();
    cleanupViewerInteraction();
    viewerApi = null;
    currentModelTag = '';
    modelTagAliases = [];
    engineComponentApiReady.value = false;
    currentBlowAmount = 0;
    currentBlowMode = 'SPHERE';
    modelVisible = true;
    setSelectedFeature(null);
    viewerRef.value && (viewerRef.value.innerHTML = '');
    await ensureProjectContext(props.projectId);
    const nextTicket = await issueLightweightViewerTicket(props.projectId, props.jobId);
    ticket.value = nextTicket;
    emit('ticketChange', nextTicket);
    postViewerCapabilities();
    if (!nextTicket.viewerAvailable || !nextTicket.modelAccessAddress || !nextTicket.engineStaticBase) {
      errorMessage.value = nextTicket.blockedReason || '模型仍在转换中，暂时无法打开 Viewer。';
      return;
    }
    await assertModelAccessAddress(nextTicket.modelAccessAddress);
    await loadEngineScript(nextTicket.engineStaticBase);
    await mountModel(nextTicket);
    viewerReady.value = true;
    emit('readyChange', true);
    postViewerEvent('ready', { ready: true, jobId: props.jobId });
    postViewerCapabilities();
    updateAutoRotate();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '葛兰岱尔 Viewer 加载失败';
    postViewerEvent('error', { message: errorMessage.value });
  } finally {
    loading.value = false;
    if (!viewerReady.value) stopAutoRotate();
  }
}

function handleHostMessage(event: MessageEvent) {
  if (event.origin !== window.location.origin) return;
  const payload = event.data as {
    type?: string;
    action?: ViewerAction;
    requestId?: string;
    payload?: ViewerActionPayload;
  } | null;
  if (!payload || payload.type !== 'glandar-viewer-command' || !payload.action) return;
  void runViewerAction(payload.action, payload.payload);
}

function postViewerEvent(type: string, payload: Record<string, unknown> = {}) {
  if (!props.embedded || window.parent === window) return;
  window.parent.postMessage({
    type: 'glandar-viewer-event',
    event: type,
    payload
  }, window.location.origin);
}

function postViewerCapabilities() {
  postViewerEvent('capabilities', {
    componentIndexAvailable: componentInteractionAvailable.value,
    reason: componentInteractionAvailable.value ? '' : componentInteractionUnavailableReason.value
  });
}

function updateEngineComponentCapability() {
  engineComponentApiReady.value = Boolean(
    viewerApi?.Model?.blow
      || viewerApi?.Feature?.getByEvent
      || viewerApi?.Feature?.zoomTo
      || viewerApi?.Feature?.setVisible
  );
  postViewerCapabilities();
}

function rememberModelTag(tag: unknown) {
  const value = String(tag || '').trim();
  if (!value) return;
  if (!currentModelTag) {
    currentModelTag = value;
  }
  if (!modelTagAliases.includes(value)) {
    modelTagAliases.push(value);
  }
}

function rememberModelTagsFromPayload(payload: unknown, depth = 0) {
  if (!payload || depth > 3) return;
  if (typeof payload === 'string' || typeof payload === 'number') {
    rememberModelTag(payload);
    return;
  }
  if (Array.isArray(payload)) {
    payload.forEach((item) => rememberModelTagsFromPayload(item, depth + 1));
    return;
  }
  if (typeof payload !== 'object') return;
  const record = payload as Record<string, unknown>;
  [
    record.tag,
    record.modelTag,
    record.ModelTag,
    record.modelName,
    record.ModelName,
    record.name
  ].forEach(rememberModelTag);
  [
    record.model,
    record.Model,
    record.data,
    record.datas,
    record.result,
    record.info,
    record.detail
  ].forEach((item) => rememberModelTagsFromPayload(item, depth + 1));
}

function currentModelTags() {
  const values = [currentModelTag, ...modelTagAliases]
    .map((value) => String(value || '').trim())
    .filter(Boolean);
  return [...new Set(values)];
}

function modelTaggedPayloads(payload: Record<string, unknown>, options: { includeNoTag?: boolean } = {}) {
  const tags = currentModelTags();
  const payloads: Record<string, unknown>[] = [];
  tags.forEach((tag) => {
    payloads.push({ ...payload, tag });
    payloads.push({ ...payload, modelTag: tag });
  });
  if (tags.length > 1) {
    payloads.push({ ...payload, tags });
    payloads.push({ ...payload, modelTags: tags });
  }
  if (options.includeNoTag !== false) {
    payloads.push({ ...payload });
  }
  const seen = new Set<string>();
  return payloads.filter((item) => {
    const key = JSON.stringify(item);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

async function callCompatibleApi(
  api: (options: Record<string, unknown>) => void | Promise<void>,
  payload: Record<string, unknown>
) {
  const payloads = modelTaggedPayloads(payload);
  let lastError: unknown = null;
  for (const item of payloads) {
    try {
      await api(item);
      return;
    } catch (error) {
      lastError = error;
    }
  }
  if (lastError instanceof Error) {
    throw lastError;
  }
}

async function ensureProjectContext(targetProjectId: number) {
  if (authStore.currentProjectId === targetProjectId) return;
  await authStore.changeProject(targetProjectId);
}

async function assertModelAccessAddress(modelAccessAddress: string) {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 5000);
  try {
    const response = await fetch(modelAccessAddress, {
      cache: 'no-store',
      signal: controller.signal
    });
    const contentType = response.headers.get('content-type') || '';
    if (!response.ok || contentType.includes('text/html')) {
      throw new Error(`HTTP ${response.status}`);
    }
  } catch (error) {
    const reason = error instanceof Error ? error.message : '模型地址不可访问';
    throw new Error(`葛兰岱尔模型输出服务暂不可用，无法加载 root.glt（${reason}）。请先恢复 Station 18086 模型输出服务。`);
  } finally {
    window.clearTimeout(timer);
  }
}

async function loadEngineScript(engineStaticBase: string) {
  if (window.GlendaleEngine) return;
  const base = platformEngineStaticBase(engineStaticBase);
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
  const engineSitePath = resolveEngineSitePath(engineStaticBase);
  if (!viewerRef.value || !window.GlendaleEngine || !nextTicket.modelAccessAddress) return;
  installGlandarWorkerUrlBridge();
  viewerRef.value.innerHTML = '';
  viewerApi = await window.GlendaleEngine({
    containerID: containerId.value,
    environmentType: props.embedded ? 'Color' : 'HDR',
    environmentTypeName: props.embedded ? '' : 'evening_road_01_puresky_2k',
    backgroundBlurriness: 0,
    backgroundColor: viewerBackgroundNumber(),
    selectedColor: 0xffff00,
    frameRate: false,
    logarithmicDepthBuffer: true,
    showSide: 2,
    sitePath: engineSitePath,
    serverIp: '',
    serverPort: 0,
    useHttps: false,
    modelList: [],
    renderFactor: 1,
    renderMode: 1,
    mappingMode: 3,
    secretKey: nextTicket.viewerTicket || ''
  });
  updateEngineComponentCapability();
  forceViewerSceneBackground();
  await applyMouseNavigation();
  bindViewerInteraction();
  currentModelTag = `glandar-${nextTicket.jobId}`;
  rememberModelTag(currentModelTag);
  await viewerApi.Model?.add?.({
    url: nextTicket.modelAccessAddress,
    tag: currentModelTag,
    flyTo: true,
    flyto: true,
    readyPromise: () => {
      requestViewerRender();
    },
    callback: async (payload?: unknown) => {
      rememberModelTagsFromPayload(payload);
      await applyMouseNavigation();
      updateEngineComponentCapability();
      forceViewerSceneBackground();
      settleViewerCamera();
    }
  });
  updateEngineComponentCapability();
  forceViewerSceneBackground();
  settleViewerCamera();
}

function settleViewerCamera() {
  const fitModel = async () => {
    if (!viewerApi || !currentModelTag) return;
    forceViewerSceneBackground();
    await viewerApi.Camera?.transitionsView?.({ showViewMode: '3D', enableTransition: false });
    await viewerApi.Model?.zoomTo?.({ tag: currentModelTag });
    reconnectCameraControls();
    updateCameraControls();
    forceViewerSceneBackground();
  };
  window.requestAnimationFrame(() => {
    void fitModel();
  });
  window.setTimeout(() => {
    void fitModel();
  }, 300);
  window.setTimeout(() => {
    void fitModel();
  }, 900);
}

function forceViewerSceneBackground() {
  const color = viewerBackgroundColor();
  const self = viewerApi?.Public?.self;
  if (!self) {
    paintViewerCanvasElement(color);
    return;
  }
  const renderer = firstExisting<GlandarRendererLike>(self, [
    'GLE_renderer',
    'GLE_Renderer',
    'renderer',
    'webglRenderer',
    'GLE_webGLRenderer',
    'GLE_webglRenderer'
  ]);
  try {
    renderer?.setClearColor?.(createThreeColor(color), 1);
    renderer?.setClearAlpha?.(1);
    renderer?.clearColor?.();
  } catch {
    renderer?.setClearColor?.(color, 1);
  }

  const scene = firstExisting<GlandarSceneLike>(self, [
    'GLE_scene',
    'GLE_Scene',
    'scene',
    'threeScene',
    'GLE_threeScene'
  ]);
  if (scene) {
    scene.background = createThreeColor(color);
    if (props.embedded) {
      scene.fog = undefined;
    }
  }

  paintViewerCanvasElement(color);
  requestViewerRender();
}

function firstExisting<T>(source: Record<string, unknown>, keys: string[]): T | null {
  for (const key of keys) {
    const value = source[key];
    if (value) return value as T;
  }
  return null;
}

function createThreeColor(color: string) {
  const three = (window as unknown as { THREE?: { Color?: new (value: string) => unknown } }).THREE;
  return three?.Color ? new three.Color(color) : color;
}

function paintViewerCanvasElement(color: string) {
  if (!viewerRef.value) return;
  viewerRef.value.style.background = color;
  viewerRef.value.querySelectorAll('canvas').forEach((canvas) => {
    (canvas as HTMLCanvasElement).style.background = color;
  });
}

async function runViewerAction(action: ViewerAction, payload: ViewerActionPayload = {}) {
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
    if (action === 'blow') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await animateModelBlow({
        mode: normalizeBlowMode(payload.mode),
        amount: clampBlowAmount(payload.amount),
        durationMs: Number(payload.durationMs || 720)
      });
      return;
    }
    if (action === 'model-visible-toggle') {
      await toggleModelVisible();
      return;
    }
    if (action === 'top' || action === 'front' || action === 'left' || action === 'right') {
      const viewTypeMap: Record<string, string> = {
        top: 'TOP',
        front: 'FRONT',
        left: 'LEFT',
        right: 'RIGHT'
      };
      await viewerApi.Camera?.setStationaryViewPort?.({
        type: viewTypeMap[action],
        enableTransition: true
      });
      ElMessage.success('已切换模型视角');
      return;
    }
    if (action === 'pick') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await clearMeasurement();
      activeInteractionMode = 'pick';
      await enableFeaturePick();
      ElMessage.info('已进入构件选择模式，单击模型构件进行选择');
      return;
    }
    if (action === 'locate') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await locateSelectedFeature();
      return;
    }
    if (action === 'hide') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await toggleSelectedFeatureVisibility();
      return;
    }
    if (action === 'feature-visible-toggle') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await toggleSelectedFeatureVisibility();
      return;
    }
    if (action === 'show-hidden') {
      await restoreHiddenFeatures();
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
      await exitRoamSilently();
      activeInteractionMode = 'measure';
      lastMeasurementText.value = '';
      await viewerApi.Measurement?.distance?.({
        mouseMoveAdsorbVertex: true,
        showXYZ: true,
        callback: (result: Record<string, unknown>) => {
          lastMeasurementText.value = formatMeasurementResult('距离', result);
          postViewerEvent('measurement', { label: lastMeasurementText.value, result });
        }
      });
      ElMessage.info('距离测量已开启');
      return;
    }
    if (action === 'measure-area') {
      await exitRoamSilently();
      activeInteractionMode = 'measure';
      lastMeasurementText.value = '';
      await viewerApi.Measurement?.area?.({
        mouseMoveAdsorbVertex: true,
        isAddLab: true,
        callback: (result: Record<string, unknown>) => {
          lastMeasurementText.value = formatMeasurementResult('面积', result);
          postViewerEvent('measurement', { label: lastMeasurementText.value, result });
        }
      });
      ElMessage.info('面积测量已开启，右键结束拾取');
      return;
    }
    if (action === 'measure-feature-area') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await measureSelectedFeature('area');
      return;
    }
    if (action === 'measure-feature-volume') {
      if (!componentInteractionAvailable.value) {
        ElMessage.info(componentInteractionUnavailableReason.value);
        return;
      }
      await measureSelectedFeature('volume');
      return;
    }
    if (action === 'measure-clear') {
      await clearMeasurement();
      ElMessage.success('已清除测量痕迹');
      return;
    }
    if (action === 'roam-start') {
      await startRoam();
      return;
    }
    if (action === 'roam-pause') {
      await viewerApi.Camera?.pauseImmersiveRoam?.();
      ElMessage.info('漫游已暂停');
      return;
    }
    if (action === 'roam-stop') {
      await exitRoamSilently();
      ElMessage.success('漫游已停止');
      return;
    }
    if (action === 'clip') {
      await toggleClip();
      return;
    }
    if (action === 'clip-clear') {
      await closeClip();
      return;
    }
    if (action === 'clear-selection') {
      await clearSelectedFeature({ toast: true });
      return;
    }
    if (action === 'fullscreen') {
      await toggleFullscreen();
      return;
    }
    if (action === 'clear') {
      await closeClip();
      await clearMeasurement();
      await restoreHiddenFeatures({ silent: true });
      await exitRoamSilently();
      await clearSelectedFeature({ toast: true });
      ElMessage.success('已清除当前 Viewer 操作状态');
      return;
    }
    await clearMeasurement();
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '当前 Viewer 工具暂不可用');
  }
}

async function clearMeasurement() {
  try {
    await viewerApi?.Measurement?.exit?.();
  } catch (error) {
    console.warn(error);
  }
  try {
    if (viewerApi?.Measurement?.clearAllTrace) {
      await viewerApi.Measurement.clearAllTrace();
    }
  } catch (error) {
    console.warn(error);
  }
  try {
    await viewerApi?.Measurement?.clear?.();
  } catch (error) {
    console.warn(error);
  }
  lastMeasurementText.value = '';
  activeInteractionMode = 'navigate';
  postViewerEvent('measurement', { label: '' });
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
  element.addEventListener('click', stopViewerClickEvent, { capture: true });
  element.addEventListener('pointerdown', handleViewerPointerDown);
  element.addEventListener('pointermove', handleViewerPointerMove);
  element.addEventListener('pointerup', handleViewerPointerUp);
  element.addEventListener('pointercancel', handleViewerPointerUp);
  element.addEventListener('wheel', handleViewerWheel, { passive: false });
  disposeViewerInteraction = () => {
    element.removeEventListener('contextmenu', preventContextMenu);
    element.removeEventListener('pointerdown', focusCanvas, { capture: true });
    element.removeEventListener('click', stopViewerClickEvent, { capture: true });
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

function stopViewerClickEvent(event: MouseEvent) {
  if (activeInteractionMode === 'measure' || activeInteractionMode === 'roam') return;
  event.preventDefault();
  event.stopPropagation();
}

function handleViewerPointerDown(event: PointerEvent) {
  if (activeInteractionMode === 'measure' || activeInteractionMode === 'roam') return;
  if (!viewerApi || !currentModelTag || (event.button !== 0 && event.button !== 2)) return;
  autoRotatePausedUntil = performance.now() + 3200;
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
  if (activeInteractionMode === 'measure' || activeInteractionMode === 'roam') return;
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
  if (activeInteractionMode === 'measure' || activeInteractionMode === 'roam') return;
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
    if (componentInteractionAvailable.value) {
      await pickFeatureFromPointer(event);
    }
  }
  event.preventDefault();
}

function handleViewerWheel(event: WheelEvent) {
  if (activeInteractionMode === 'measure' || activeInteractionMode === 'roam') return;
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
  const positions = enginePickPositionCandidates(event);
  for (const position of positions) {
    const picked = await pickFeatureFromEnginePosition(position, { missToast: false });
    if (picked) return;
  }
  await clearSelectedFeature({ toast: true, missToast: true });
}

async function pickFeatureFromEnginePosition(
  position: Record<string, unknown> | undefined,
  options: { missToast?: boolean } = {}
) {
  if (!viewerApi?.Feature?.getByEvent || !position) return;
  try {
    const candidate = normalizePickPosition(position);
    if (!candidate) {
      await clearSelectedFeature({ toast: true, missToast: options.missToast });
      return false;
    }
    const feature = await getFeatureAtPosition(candidate);
    if (feature?.id) {
      await selectFeature(feature);
      return true;
    }
    await clearSelectedFeature({ toast: true, missToast: options.missToast });
    return false;
  } catch (error) {
    ElMessage.warning(error instanceof Error ? error.message : '构件拾取暂不可用');
    return false;
  }
}

function getFeatureAtPosition(position: { x: number; y: number }) {
  return new Promise<GlandarFeature | null>((resolve) => {
    let resolved = false;
    const timer = window.setTimeout(() => {
      if (!resolved) {
        resolved = true;
        resolve(null);
      }
    }, 320);
    try {
      const result = viewerApi?.Feature?.getByEvent?.({
        position,
        callback: (feature: GlandarFeature | null) => {
          if (resolved) return;
          const normalized = normalizePickedFeature(feature);
          resolved = true;
          window.clearTimeout(timer);
          resolve(normalized);
        }
      });
      if (result && typeof (result as Promise<GlandarFeature | null>).then === 'function') {
        void (result as Promise<GlandarFeature | null>).then((feature) => {
          const normalized = normalizePickedFeature(feature);
          if (resolved || !normalized?.id) return;
          resolved = true;
          window.clearTimeout(timer);
          resolve(normalized);
        }).catch(() => {
          // callback mode remains the normal path; promise rejection is treated as miss.
        });
      } else {
        const normalized = normalizePickedFeature(result as GlandarFeature | null);
        if (!normalized?.id) return;
        resolved = true;
        window.clearTimeout(timer);
        resolve(normalized);
      }
    } catch (error) {
      if (!resolved) {
        resolved = true;
        window.clearTimeout(timer);
        resolve(null);
      }
      console.warn(error);
    }
  });
}

function normalizePickedFeature(feature: GlandarFeature | null | undefined) {
  if (!feature || typeof feature !== 'object') return null;
  const id = firstFeatureText(
    feature.id,
    feature.featureId,
    feature.FeatureId,
    feature.externalId,
    feature.ExternalId,
    feature.objectId,
    feature.componentId
  );
  if (!id) return null;
  return {
    ...feature,
    id,
    batchId: feature.batchId ?? feature.BatchId,
    revitId: firstFeatureText(feature.revitId, feature.RevitId, feature.externalId, feature.ExternalId)
  };
}

function firstFeatureText(...values: unknown[]) {
  for (const value of values) {
    if (value === undefined || value === null) continue;
    const text = String(value).trim();
    if (text) return text;
  }
  return '';
}

function normalizePickPosition(position: Record<string, unknown>) {
  const x = Number(position.x ?? position.clientX);
  const y = Number(position.y ?? position.clientY);
  if (!Number.isFinite(x) || !Number.isFinite(y)) return null;
  return { x, y };
}

async function enableFeaturePick() {
  if (!viewerApi?.Feature?.getByEvent) {
    ElMessage.info(componentInteractionUnavailableReason.value);
    return;
  }
  clearEngineLeftClickHandlers();
  activeInteractionMode = 'pick';
}

function normalizeBlowMode(mode: ViewerActionPayload['mode']) {
  const value = String(mode || 'SPHERE').toUpperCase();
  return value === 'LINEAR' ? 'LINEAR' : 'SPHERE';
}

function clampBlowAmount(value: unknown) {
  const next = Number(value ?? 0);
  if (!Number.isFinite(next)) return 0;
  return Math.max(0, Math.min(1, next));
}

function stopBlowAnimation() {
  if (!blowAnimationFrame) return;
  window.cancelAnimationFrame(blowAnimationFrame);
  blowAnimationFrame = 0;
}

async function applyModelBlow(mode: 'SPHERE' | 'LINEAR', amount: number) {
  if (!viewerApi?.Model?.blow) {
    throw new Error('当前引擎版本暂不支持模型爆炸');
  }
  const payloads = buildExplosionPayloads(mode, amount);
  let lastError: unknown = null;
  for (const payload of payloads) {
    try {
      await viewerApi.Model.blow(payload);
      requestViewerRender();
      return;
    } catch (error) {
      lastError = error;
    }
  }
  throw lastError instanceof Error ? lastError : new Error('当前引擎版本暂不支持模型爆炸');
}

function buildExplosionPayloads(mode: 'SPHERE' | 'LINEAR', amount: number) {
  const base = { type: mode, value: amount, showAxis: true };
  const tags = currentModelTags();
  const payloads: Record<string, unknown>[] = [];
  tags.forEach((tag) => {
    payloads.push({ ...base, tag });
    payloads.push({ ...base, modelTag: tag });
  });
  if (tags.length > 0) {
    payloads.push({ ...base, modelTags: tags });
  }
  if (tags.length > 1) {
    payloads.push({ ...base, tags });
  }
  payloads.push({ ...base });
  const seen = new Set<string>();
  return payloads.filter((payload) => {
    const key = JSON.stringify(payload);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function easeInOutCubic(value: number) {
  return value < 0.5
    ? 4 * value * value * value
    : 1 - Math.pow(-2 * value + 2, 3) / 2;
}

function animateModelBlow(options: { mode: 'SPHERE' | 'LINEAR'; amount: number; durationMs: number }) {
  const durationMs = Math.max(120, options.durationMs || 720);
  const from = currentBlowAmount;
  const to = options.amount;
  stopBlowAnimation();
  return new Promise<void>((resolve, reject) => {
    const startedAt = performance.now();
    if (currentBlowMode !== options.mode && currentBlowAmount > 0) {
      void applyModelBlow(currentBlowMode, 0);
    }
    currentBlowMode = options.mode;
    const tick = async (time: number) => {
      const progress = Math.min((time - startedAt) / durationMs, 1);
      const nextAmount = from + (to - from) * easeInOutCubic(progress);
      try {
        await applyModelBlow(options.mode, nextAmount);
      } catch (error) {
        blowAnimationFrame = 0;
        reject(error);
        return;
      }
      currentBlowAmount = nextAmount;
      if (progress >= 1) {
        blowAnimationFrame = 0;
        currentBlowAmount = to;
        ElMessage.success(to > 0 ? '已应用模型爆炸' : '模型爆炸已复原');
        postViewerEvent('blow', { mode: options.mode, amount: to });
        resolve();
        return;
      }
      blowAnimationFrame = window.requestAnimationFrame((nextTime) => {
        void tick(nextTime);
      });
    };
    blowAnimationFrame = window.requestAnimationFrame((nextTime) => {
      void tick(nextTime);
    });
  });
}

async function locateSelectedFeature() {
  if (!selectedFeatureId.value || !viewerApi?.Feature?.zoomTo) return;
  await callCompatibleApi(viewerApi.Feature.zoomTo.bind(viewerApi.Feature), {
    featureIds: selectedFeatureId.value,
    batchId: selectedFeature.value?.batchId
  });
  ElMessage.success('已定位到选中构件');
}

async function toggleModelVisible() {
  if (!viewerApi?.Model?.setVisible || !currentModelTag) {
    ElMessage.warning('当前引擎版本暂不支持整模显示/隐藏');
    return;
  }
  const nextVisible = !modelVisible;
  await viewerApi.Model.setVisible({
    tag: currentModelTag,
    visible: nextVisible
  });
  modelVisible = nextVisible;
  requestViewerRender();
  ElMessage.success(nextVisible ? '已显示整个模型' : '已隐藏整个模型');
  postViewerEvent('model-visibility', { visible: nextVisible });
}

async function toggleSelectedFeatureVisibility() {
  if (!selectedFeatureId.value || !viewerApi?.Feature?.setVisible) return;
  const featureId = selectedFeatureId.value;
  const nextVisible = hiddenFeatureIds.has(featureId);
  if (!nextVisible) {
    await clearFeatureHighlight();
  }
  await callCompatibleApi(viewerApi.Feature.setVisible.bind(viewerApi.Feature), {
    featureIds: featureId,
    visible: nextVisible
  });
  if (nextVisible) {
    hiddenFeatureIds.delete(featureId);
    if (selectedFeature.value) {
      await selectFeature(selectedFeature.value);
    }
  } else {
    hiddenFeatureIds.add(featureId);
  }
  requestViewerRender();
  postViewerEvent('feature-visibility', { featureId, visible: nextVisible });
  ElMessage.success(nextVisible ? '已重新显示选中构件' : '已隐藏选中构件');
}

async function restoreHiddenFeatures(options: { silent?: boolean } = {}) {
  if (!viewerApi?.Feature?.setVisible || hiddenFeatureIds.size === 0) return;
  const featureIds = [...hiddenFeatureIds].join('#');
  await callCompatibleApi(viewerApi.Feature.setVisible.bind(viewerApi.Feature), {
    featureIds,
    visible: true
  });
  hiddenFeatureIds.clear();
  requestViewerRender();
  if (!options.silent) ElMessage.success('已恢复隐藏构件');
}

async function measureSelectedFeature(kind: 'area' | 'volume') {
  const featureId = selectedFeatureId.value;
  if (!featureId) {
    ElMessage.info('请先点击模型构件，再进行构件测量');
    return;
  }
  const api = kind === 'area' ? viewerApi?.Measurement?.featureArea : viewerApi?.Measurement?.featureVolume;
  if (!api) {
    ElMessage.warning('当前引擎版本暂不支持该构件测量能力');
    return;
  }
  await api({
    featureId,
    callback: (result: Record<string, unknown>) => {
      lastMeasurementText.value = formatMeasurementResult(kind === 'area' ? '构件面积' : '构件体积', result);
      postViewerEvent('measurement', { label: lastMeasurementText.value, result });
    }
  });
}

async function startRoam() {
  if (!viewerApi?.Camera?.startImmersiveRoam) {
    ElMessage.warning('当前引擎版本暂不支持漫游模式');
    return;
  }
  await clearMeasurement();
  const viewport = await viewerApi.Camera.getViewPort?.();
  const birthPoint = normalizeRoamBirthPoint(viewport);
  await viewerApi.Camera.setImmersiveRoamConfig?.({
    lookFactor: 90,
    moveRate: 1,
    horizontalDistance: 1,
    gravityRate: 9.82,
    footerHeight: 1.5,
    canCrossWall: false,
    canGravity: true,
    mouseEnabled: true,
    keyboardEnabled: true
  });
  await viewerApi.Camera.startImmersiveRoam({
    roamingType: 0,
    birthPoint
  });
  roamRunning = true;
  activeInteractionMode = 'roam';
  postViewerEvent('roam', { state: 'started' });
  ElMessage.success('漫游已开启，可用方向键和鼠标视角控制');
}

async function exitRoamSilently() {
  if (!roamRunning) return;
  try {
    await viewerApi?.Camera?.stopImmersiveRoam?.();
  } catch (error) {
    console.warn(error);
  } finally {
    roamRunning = false;
    activeInteractionMode = 'navigate';
    postViewerEvent('roam', { state: 'stopped' });
  }
}

async function toggleClip() {
  if (!viewerApi?.Model?.clip || !currentModelTag) {
    ElMessage.warning('当前引擎版本暂不支持剖切');
    return;
  }
  if (clipEnabled) {
    await closeClip();
    return;
  }
  await viewerApi.Model.clip({
    tag: currentModelTag,
    type: 'Z',
    value: 0
  });
  clipEnabled = true;
  ElMessage.success('已开启模型剖切');
}

async function closeClip() {
  if (!clipEnabled) return;
  await viewerApi?.Model?.closeClip?.({ tag: currentModelTag });
  clipEnabled = false;
}

function normalizeRoamBirthPoint(viewport: Record<string, unknown> | undefined) {
  const target = viewport?.target;
  const position = viewport?.position;
  if (Array.isArray(target) && target.length >= 3) return target;
  if (Array.isArray(position) && position.length >= 3) return position;
  if (target && typeof target === 'object') {
    const point = target as Record<string, unknown>;
    return [Number(point.x || 0), Number(point.y || 0), Number(point.z || 0)];
  }
  return [0, 0, 0];
}

function formatMeasurementResult(label: string, result: Record<string, unknown>) {
  const distance = numberValue(result.dis);
  const area = numberValue(result.area);
  const volume = numberValue(result.volume);
  if (distance !== null) return `${label} ${distance.toFixed(2)} m`;
  if (area !== null) return `${label} ${area.toFixed(2)} m²`;
  if (volume !== null) return `${label} ${volume.toFixed(2)} m³`;
  return `${label}完成`;
}

function numberValue(value: unknown) {
  const next = Number(value);
  return Number.isFinite(next) ? next : null;
}

function setSelectedFeature(feature: GlandarFeature | null) {
  selectedFeature.value = feature;
  selectedFeatureId.value = feature?.id ? String(feature.id) : '';
  if (feature?.id) {
    void loadSelectedFeatureProperties(feature);
  } else {
    selectedFeatureProperties.value = null;
    featurePropertiesError.value = '';
    featurePropertiesLoading.value = false;
  }
  postViewerEvent('feature-selected', {
    featureId: selectedFeatureId.value,
    revitId: selectedFeatureRevitId.value,
    batchId: feature?.batchId ?? null,
    modelFileId: props.modelFileId || null,
    fileName: props.fileName || ticket.value?.lightweightName || ''
  });
}

async function loadSelectedFeatureProperties(feature: GlandarFeature) {
  const featureId = feature.id ? String(feature.id) : '';
  if (!featureId || !ticket.value?.jobId) {
    selectedFeatureProperties.value = null;
    featurePropertiesError.value = '';
    return;
  }
  const requestFeatureId = featureId;
  featurePropertiesLoading.value = true;
  featurePropertiesError.value = '';
  selectedFeatureProperties.value = null;
  try {
    const response = await fetchGlandarComponentProperties(
      props.projectId,
      ticket.value.jobId,
      requestFeatureId,
      selectedFeatureRevitId.value || null
    );
    if (selectedFeatureId.value !== requestFeatureId) return;
    selectedFeatureProperties.value = response;
    featurePropertiesError.value = response.unavailableReason || '';
    postViewerEvent('feature-properties', {
      featureId: response.featureId,
      propertyAvailable: response.propertyAvailable,
      propertyCount: response.propertyCount
    });
  } catch (error) {
    if (selectedFeatureId.value !== requestFeatureId) return;
    featurePropertiesError.value = error instanceof Error ? error.message : '构件属性读取失败';
    postViewerEvent('feature-properties', {
      featureId: requestFeatureId,
      propertyAvailable: false,
      propertyCount: 0
    });
  } finally {
    if (selectedFeatureId.value === requestFeatureId) {
      featurePropertiesLoading.value = false;
    }
  }
}

async function clearFeatureHighlight() {
  const featureId = selectedFeatureId.value;
  if (!featureId || !viewerApi?.Feature) return;
  try {
    if (typeof viewerApi.Feature.setColor === 'function') {
      await callCompatibleApi(viewerApi.Feature.setColor.bind(viewerApi.Feature), {
        featureIds: featureId,
        color: 'rgb(255, 255, 255)'
      });
    }
  } catch (error) {
    console.warn(error);
  }
}

async function clearSelectedFeature(options: { toast?: boolean; missToast?: boolean } = {}) {
  const hadFeature = Boolean(selectedFeatureId.value);
  await clearFeatureHighlight();
  setSelectedFeature(null);
  requestViewerRender();
  if (options.toast && hadFeature) {
    ElMessage.info('已取消构件选择');
  } else if (options.toast && options.missToast) {
    ElMessage.info('未拾取到构件，请点击模型实体');
  }
}

async function selectFeature(feature: GlandarFeature) {
  if (!feature?.id) {
    await clearSelectedFeature({ toast: true });
    return;
  }
  await clearFeatureHighlight();
  setSelectedFeature(feature);
  try {
    if (typeof viewerApi?.Feature?.setColor === 'function') {
      await callCompatibleApi(viewerApi.Feature.setColor.bind(viewerApi.Feature), {
        featureIds: feature.id,
        color: 'rgb(255, 210, 64)'
      });
    } else if (typeof viewerApi?.Feature?.highlight === 'function') {
      await callCompatibleApi(viewerApi.Feature.highlight.bind(viewerApi.Feature), {
        featureIds: feature.id,
        batchId: feature.batchId
      });
    }
    requestViewerRender();
  } catch (error) {
    console.warn(error);
  }
  ElMessage.success(`已选中构件：${feature.id}`);
}

function clearEngineLeftClickHandlers() {
  if (!viewerApi?.Public?.clearHandler) return;
  try {
    void viewerApi.Public.clearHandler({ event: 'LEFT_CLICK' });
  } catch (error) {
    console.warn(error);
  }
}

function handleFullscreenChange() {
  fullscreenActive.value = document.fullscreenElement === viewerShellRef.value;
  window.setTimeout(() => {
    reconnectCameraControls();
    updateCameraControls();
  }, 120);
}

function handleFullscreenKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape' || !fullscreenActive.value || document.fullscreenElement) return;
  fullscreenActive.value = false;
  reconnectCameraControls();
  updateCameraControls();
}

async function toggleFullscreen() {
  const target = viewerShellRef.value;
  if (!target) return;
  if (fullscreenActive.value) {
    fullscreenActive.value = false;
    if (document.fullscreenElement === target) {
      await document.exitFullscreen?.();
    }
    reconnectCameraControls();
    updateCameraControls();
    return;
  }
  if (typeof target.requestFullscreen !== 'function') {
    fullscreenActive.value = true;
    window.setTimeout(() => {
      reconnectCameraControls();
      updateCameraControls();
    }, 120);
    return;
  }
  try {
    await target.requestFullscreen();
  } catch {
    fullscreenActive.value = true;
    window.setTimeout(() => {
      reconnectCameraControls();
      updateCameraControls();
    }, 120);
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

function updateAutoRotate() {
  if (!props.autoRotate || !viewerReady.value) {
    stopAutoRotate();
    return;
  }
  startAutoRotate();
}

function startAutoRotate() {
  if (autoRotateFrame) return;
  const tick = (time: number) => {
    autoRotateFrame = window.requestAnimationFrame(tick);
    if (!props.autoRotate || !viewerReady.value || time < autoRotatePausedUntil) return;
    const controls = getCameraControls();
    if (!controls || typeof controls.rotate !== 'function') return;
    controls.rotate(0.0022, 0, false);
    updateCameraControls();
  };
  autoRotateFrame = window.requestAnimationFrame(tick);
}

function stopAutoRotate() {
  if (!autoRotateFrame) return;
  window.cancelAnimationFrame(autoRotateFrame);
  autoRotateFrame = 0;
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
  return options.button === 0 && duration >= 0 && duration <= 450 && distance <= 4;
}

function screenToEnginePickPosition(event: PointerEvent) {
  return {
    x: event.clientX,
    y: event.clientY,
    clientX: event.clientX,
    clientY: event.clientY,
    screenX: event.screenX,
    screenY: event.screenY,
    pageX: event.pageX,
    pageY: event.pageY
  };
}

function enginePickPositionCandidates(event: PointerEvent) {
  const candidates: Array<{ x: number; y: number }> = [];
  const canvas = viewerRef.value?.querySelector('canvas') as HTMLCanvasElement | null;
  const target = canvas || viewerRef.value;
  const rect = target?.getBoundingClientRect();
  pushUniquePosition(candidates, event.clientX, event.clientY);
  if (rect) {
    const canvasX = event.clientX - rect.left;
    const canvasY = event.clientY - rect.top;
    pushUniquePosition(candidates, canvasX, canvasY);
    const renderWidth = canvas?.width || target?.clientWidth || 0;
    const renderHeight = canvas?.height || target?.clientHeight || 0;
    if (rect.width > 0 && rect.height > 0 && renderWidth > 0 && renderHeight > 0) {
      pushUniquePosition(candidates, canvasX * (renderWidth / rect.width), canvasY * (renderHeight / rect.height));
    }
  }
  return candidates;
}

function pushUniquePosition(list: Array<{ x: number; y: number }>, x: number, y: number) {
  if (!Number.isFinite(x) || !Number.isFinite(y)) return;
  const position = {
    x: Math.round(x * 1000) / 1000,
    y: Math.round(y * 1000) / 1000
  };
  const key = `${position.x}:${position.y}`;
  if (!list.some((item) => `${item.x}:${item.y}` === key)) {
    list.push(position);
  }
}

function normalizeEngineStaticBase(value: string) {
  const trimmed = value.replace(/\/$/, '');
  if (trimmed.endsWith('/static/ThreeJsEngine')) return trimmed;
  if (trimmed.endsWith('/ThreeJsEngine')) return trimmed;
  return `${trimmed}/static/ThreeJsEngine`;
}

function platformEngineStaticBase(value: string) {
  const normalized = normalizeEngineStaticBase(value);
  if (normalized.includes('/static/ThreeJsEngine')) {
    return `${window.location.origin}/api/visualization-adapter/glandar/static/ThreeJsEngine`;
  }
  return normalized;
}

function resolveEngineSitePath(engineStaticBase: string) {
  const normalized = normalizeEngineStaticBase(engineStaticBase);
  if (props.embedded || normalized.includes('/static/ThreeJsEngine')) {
    return `${window.location.origin}/glandar-engine/`;
  }
  return `${normalized.replace(/\/$/, '')}/`;
}

function installGlandarWorkerUrlBridge() {
  if (window.__zhuoyuGlandarWorkerBridgeInstalled || typeof window.Worker !== 'function') return;
  const NativeWorker = window.Worker;
  const BridgedWorker = function WorkerBridge(
    this: Worker,
    scriptURL: string | URL,
    options?: WorkerOptions
  ) {
    return new NativeWorker(rewriteGlandarWorkerUrl(scriptURL), options);
  } as unknown as typeof Worker;
  BridgedWorker.prototype = NativeWorker.prototype;
  window.__zhuoyuNativeWorker = NativeWorker;
  window.Worker = BridgedWorker;
  window.__zhuoyuGlandarWorkerBridgeInstalled = true;
}

function rewriteGlandarWorkerUrl(scriptURL: string | URL) {
  const urlText = String(scriptURL);
  const workerName = [
    'gleBatchTextureWorker.js',
    'gleEdgesWorker.js',
    'PickWorker.js',
    'RaycastWorker.js'
  ].find((name) => urlText.includes(name));
  if (!workerName) return scriptURL;
  const workerFolder = urlText.includes('/third/worker/') ? 'third/worker' : 'worker';
  return `${window.location.origin}/glandar-engine/${workerFolder}/${workerName}`;
}

function viewerBackgroundColor() {
  return props.theme === 'light' ? '#eef8ff' : '#061423';
}

function viewerBackgroundNumber() {
  return props.theme === 'light' ? 0xeef8ff : 0x061423;
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

.glandar-viewer:fullscreen {
  border: 0;
  border-radius: 0;
  grid-template-columns: 84px minmax(0, 1fr) 280px;
  height: 100dvh;
  min-height: 100dvh;
  padding: var(--zy-sp-4);
  width: 100dvw;
}

.glandar-viewer.is-app-fullscreen {
  border: 0;
  border-radius: 0;
  grid-template-columns: 84px minmax(0, 1fr) 280px;
  height: 100dvh;
  inset: 0;
  min-height: 100dvh;
  padding: var(--zy-sp-4);
  position: fixed;
  width: 100dvw;
  z-index: 9999;
}

.glandar-viewer:fullscreen .glandar-viewer__canvas-card {
  min-height: calc(100dvh - 2 * var(--zy-sp-4));
}

.glandar-viewer.is-app-fullscreen .glandar-viewer__canvas-card {
  min-height: calc(100dvh - 2 * var(--zy-sp-4));
}

.glandar-viewer.is-embedded {
  background:
    radial-gradient(circle at 50% 42%, rgb(34 211 238 / 16%), transparent 34rem),
    linear-gradient(90deg, rgb(91 180 255 / 12%) 1px, transparent 1px),
    linear-gradient(180deg, rgb(91 180 255 / 10%) 1px, transparent 1px),
    linear-gradient(180deg, #0d2940, #071626 58%, #05101d);
  background-size: auto, 48px 48px, 48px 48px, auto;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  display: block;
  height: 100%;
  min-height: 100%;
  padding: 0;
}

.glandar-viewer.is-embedded.is-light {
  background:
    radial-gradient(circle at 48% 36%, rgb(59 130 246 / 10%), transparent 32rem),
    linear-gradient(90deg, rgb(37 99 235 / 9%) 1px, transparent 1px),
    linear-gradient(180deg, rgb(37 99 235 / 7%) 1px, transparent 1px),
    linear-gradient(180deg, #f5fbff, #eef8ff 62%, #e6f1fb);
  background-size: auto, 48px 48px, 48px 48px, auto;
}

.glandar-viewer.is-embedded .glandar-viewer__toolbar {
  display: none;
}

.glandar-viewer.is-embedded .glandar-viewer__canvas-card {
  border-radius: 0;
  height: 100%;
  min-height: 100%;
}

.glandar-viewer.is-embedded .glandar-viewer__canvas-card::before {
  content: "";
  inset: 0;
  pointer-events: none;
  position: absolute;
  z-index: 2;
}

.glandar-viewer.is-embedded.is-light .glandar-viewer__canvas-card::before {
  content: "";
}

.glandar-viewer.is-embedded .glandar-viewer__canvas {
  cursor: grab;
  height: 100%;
  min-height: 100%;
  width: 100%;
}

.glandar-viewer.is-embedded .glandar-viewer__canvas:active {
  cursor: grabbing;
}

.glandar-viewer.is-embedded .glandar-viewer__canvas :deep(canvas) {
  display: block;
  height: 100% !important;
  width: 100% !important;
}

.glandar-viewer__feature-popover {
  backdrop-filter: blur(10px);
  background: color-mix(in srgb, #101827 82%, transparent);
  border: 1px solid color-mix(in srgb, #facc15 54%, transparent);
  border-radius: 12px;
  bottom: 16px;
  box-shadow: 0 10px 30px rgb(0 0 0 / 22%);
  color: #f8fafc;
  display: grid;
  gap: 10px;
  max-width: min(380px, calc(100% - 32px));
  padding: 14px;
  position: absolute;
  right: 16px;
  z-index: 4;
}

.glandar-viewer__feature-popover header {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.glandar-viewer__feature-popover header span,
.glandar-viewer__measurement span {
  color: #fde68a;
  font-size: 12px;
  font-weight: 720;
}

.glandar-viewer__feature-popover strong,
.glandar-viewer__measurement strong {
  color: #f8fafc;
  font-size: 13px;
  font-weight: 760;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.glandar-viewer__feature-popover dl {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}

.glandar-viewer__feature-popover dl div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.glandar-viewer__feature-popover dt {
  color: rgb(186 230 253 / 0.78);
  font-size: 11px;
}

.glandar-viewer__feature-popover dd {
  color: #e0f2fe;
  font-size: 12px;
  margin: 0;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.glandar-viewer__feature-popover p {
  color: rgb(226 232 240 / 0.76);
  font-size: 12px;
  line-height: 1.45;
  margin: 0;
}

.glandar-viewer__feature-properties {
  background: rgb(8 20 36 / 0.72);
  border: 1px solid rgb(125 211 252 / 0.2);
  border-radius: 10px;
  display: grid;
  gap: 8px;
  max-height: 210px;
  overflow: auto;
  padding: 10px;
}

.glandar-viewer__feature-properties-head {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.glandar-viewer__feature-properties-head em {
  color: rgb(191 219 254 / 0.84);
  font-size: 11px;
  font-style: normal;
}

.glandar-viewer__feature-properties article {
  display: grid;
  gap: 6px;
}

.glandar-viewer__feature-properties article strong {
  color: #bfdbfe;
  font-size: 12px;
}

.glandar-viewer__feature-properties article dl {
  grid-template-columns: 1fr;
}

.glandar-viewer__feature-properties article dl div {
  border-top: 1px solid rgb(125 211 252 / 0.12);
  grid-template-columns: minmax(72px, 0.45fr) minmax(0, 1fr);
  padding-top: 5px;
}

.glandar-viewer__feature-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.glandar-viewer__feature-popover button,
.glandar-viewer__feature-actions button {
  background: rgb(15 23 42 / 0.72);
  border: 1px solid rgb(125 211 252 / 0.36);
  border-radius: 7px;
  color: #f8fafc;
  cursor: pointer;
  font-size: 12px;
  min-height: 28px;
  padding: 0 10px;
}

.glandar-viewer__measurement {
  backdrop-filter: blur(10px);
  background: color-mix(in srgb, #101827 76%, transparent);
  border: 1px solid color-mix(in srgb, #38bdf8 46%, transparent);
  border-radius: 999px;
  display: inline-flex;
  gap: 8px;
  left: 16px;
  min-height: 34px;
  padding: 0 12px;
  position: absolute;
  top: 16px;
  z-index: 4;
}

.glandar-viewer__capability-note {
  backdrop-filter: blur(10px);
  background: color-mix(in srgb, #071626 78%, transparent);
  border: 1px solid color-mix(in srgb, #38bdf8 34%, transparent);
  border-radius: 12px;
  bottom: 16px;
  color: #dbeafe;
  display: grid;
  gap: 4px;
  left: 16px;
  max-width: min(460px, calc(100% - 32px));
  padding: 12px 14px;
  position: absolute;
  z-index: 4;
}

.glandar-viewer__capability-note strong {
  color: #e0f2fe;
  font-size: 13px;
}

.glandar-viewer__capability-note span {
  color: rgb(191 219 254 / 0.78);
  font-size: 12px;
  line-height: 1.45;
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
