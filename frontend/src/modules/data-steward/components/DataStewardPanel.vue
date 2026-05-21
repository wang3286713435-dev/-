<template>
  <section ref="panelRef" class="data-steward-panel">
    <header class="data-steward-panel__head">
      <div>
        <h3>Hermes 企业 Agent</h3>
        <p>平台只是 Hermes 的一个受控 UI；当前仅开放 catalog-only 安全接入。</p>
      </div>
      <div class="data-steward-panel__badges">
        <el-tag size="small" type="info">平台 UI</el-tag>
        <el-tag size="small" type="warning">共享工作区规划</el-tag>
        <el-tag :type="healthTagType(health)" size="small">{{ healthLabel(health) }}</el-tag>
      </div>
    </header>

    <section class="data-steward-panel__context" aria-label="当前页面上下文">
      <div>
        <span>Agent 上下文</span>
        <strong>{{ contextTitle }}</strong>
        <el-tag size="small" type="info">project scoped</el-tag>
        <el-tag v-if="conversationTurnCount" size="small" type="success">
          {{ conversationTurnCount }} 条会话
        </el-tag>
      </div>
      <p>{{ contextDescription }}</p>
      <small>会话引用已保留，native runtime 未启用；项目、权限或角色切换时会失效并等待 Gateway 重新校验。</small>
    </section>

    <el-alert
      v-if="asking"
      class="data-steward-panel__thinking"
      type="info"
      title="Hermes catalog-only 通道正在组织回答，可能需要 10-30 秒。平台未执行任何写操作。"
      :closable="false"
      show-icon
    />

    <section class="data-steward-panel__conversation" aria-label="Hermes 会话流">
      <el-empty
        v-if="!conversationEntries.length && !asking"
        description="Hermes 会话还没有开始"
      />
      <article
        v-for="entry in conversationEntries"
        :key="entry.id"
        :class="['data-steward-panel__turn', `data-steward-panel__turn--${entry.role}`]"
      >
        <template v-if="entry.role === 'user'">
          <div class="data-steward-panel__turn-meta">
            <span>你</span>
            <small v-if="entry.previousResponseRef">追问 {{ compactRef(entry.previousResponseRef) }}</small>
          </div>
          <p>{{ entry.content }}</p>
        </template>
        <template v-else-if="entry.role === 'assistant'">
          <div class="data-steward-panel__turn-meta">
            <span>Hermes</span>
            <small>{{ compactRef(entry.response.responseId) }}</small>
          </div>
          <DataStewardAnswerCard :response="entry.response" />
        </template>
        <template v-else>
          <div class="data-steward-panel__system-note">{{ entry.content }}</div>
        </template>
      </article>
    </section>

    <section v-if="catalogLoading || catalogPreview" class="data-steward-panel__catalog">
      <header>
        <div>
          <strong>相关资产目录</strong>
          <span>这些是目录命中，不代表 Hermes 已读取文件正文。</span>
        </div>
        <el-tag size="small" type="info">catalog-only</el-tag>
      </header>

      <el-skeleton v-if="catalogLoading" :rows="3" animated />
      <el-empty
        v-else-if="catalogPreview && catalogPreview.results.length === 0"
        description="未找到匹配的目录资产"
      />
      <div v-else class="data-steward-panel__catalog-list">
        <article
          v-for="item in catalogPreview?.results"
          :key="item.assetRef"
          class="data-steward-panel__catalog-item"
        >
          <div>
            <strong>{{ item.fileName }}</strong>
            <span>{{ item.projectCode }} {{ item.projectName }} · {{ item.displayPath || item.pathHint }}</span>
          </div>
          <div class="data-steward-panel__catalog-tags">
            <el-tag size="small">{{ fileKindLabel(item.assetKind) }}</el-tag>
            <el-tag size="small" type="info">{{ item.fileExt || '未知格式' }}</el-tag>
            <el-tag size="small" type="success">{{ item.indexEligibility }}</el-tag>
            <el-tag v-if="!item.contentEvidenceAvailable" size="small" type="warning">无正文证据</el-tag>
          </div>
        </article>
      </div>
    </section>

    <el-collapse v-if="capabilities" class="data-steward-panel__capabilities">
      <el-collapse-item name="capabilities">
        <template #title>
          <span class="data-steward-panel__capability-title">Hermes 能力披露与安全边界</span>
        </template>
        <p class="data-steward-panel__capability-copy">
          当前平台只开放安全只读表面；Hermes 独立 Agent 内核、共享工作区、证据检索和记忆连续性按契约逐步接入。
        </p>
        <div class="data-steward-panel__roadmap">
          <article v-for="item in capabilityRoadmap" :key="item.title">
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.description }}</span>
            </div>
            <el-tag :type="item.type" size="small">{{ item.status }}</el-tag>
          </article>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="平台当前开放">{{ modeLabel(capabilities.mode) }}</el-descriptions-item>
          <el-descriptions-item label="Hermes 身份">企业 Agent 内核，不是平台插件</el-descriptions-item>
          <el-descriptions-item label="共享工作区">规划接入独立 Hermes workspace / session / context</el-descriptions-item>
          <el-descriptions-item label="合同">{{ capabilities.contractVersion }}</el-descriptions-item>
          <el-descriptions-item v-if="health" label="网关状态">
            {{ health.status === 'ok' ? '正常' : '降级运行' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="health" label="Hermes连接">
            {{ healthLabel(health) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="health" label="运行模式">{{ modeLabel(health.mode) }}</el-descriptions-item>
          <el-descriptions-item v-if="health" label="运行时写入">
            {{ health.runtimeWriteEnabled ? '已开放' : '未开放' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="health" label="Platform Gateway 通道">
            {{ health.agentAnswerIntegrationEnabled ? 'catalog-only 已接入' : '未接入' }}
          </el-descriptions-item>
          <el-descriptions-item label="authorityHealth">
            {{ capabilities.authorityHealth.architectureAuthorityHealth }} / {{ capabilities.authorityHealth.mode }}
          </el-descriptions-item>
          <el-descriptions-item label="目录辅助">
            {{ capabilities.supports.catalogQuery ? '可用' : '未开放' }}
          </el-descriptions-item>
          <el-descriptions-item label="正文问答">
            {{ capabilities.supports.documentContentAnswer ? '可用' : '未开放' }}
          </el-descriptions-item>
          <el-descriptions-item label="数据库 / NAS 写操作">
            {{ capabilities.supports.dbCrud || capabilities.supports.nasCrud ? '可用' : '不会执行' }}
          </el-descriptions-item>
          <el-descriptions-item label="生产发布">
            {{ capabilities.supports.productionRollout ? '可用' : '未开放' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="health?.unavailableReason" label="不可用原因">
            {{ health.unavailableReason }}
          </el-descriptions-item>
        </el-descriptions>
      </el-collapse-item>
    </el-collapse>

    <section class="data-steward-panel__ask" aria-label="向 Hermes 提问">
      <header>
        <div>
          <strong>和 Hermes 对话</strong>
          <span>当前平台通道按 catalog-only 边界回答；这不代表 Hermes 本体只能 catalog-only。</span>
        </div>
        <el-button :loading="capabilityLoading" size="small" text @click="loadCapabilities">刷新状态</el-button>
      </header>

      <el-input
        v-model="question"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        :disabled="asking"
        placeholder="继续问 Hermes，或基于上一轮追问。"
      />

      <div class="data-steward-panel__quick">
        <el-button
          v-for="item in quickQuestions"
          :key="item"
          size="small"
          :disabled="asking || !Number.isFinite(projectId)"
          @click="askQuickQuestion(item)"
        >
          {{ item }}
        </el-button>
      </div>

      <div class="data-steward-panel__actions">
        <span v-if="previousResponseRef" class="data-steward-panel__followup-hint">
          将作为追问发送：{{ compactRef(previousResponseRef) }}
        </span>
        <span v-else class="data-steward-panel__followup-hint">
          当前项目会话可被所有 Hermes 入口共享
        </span>
        <el-button :disabled="asking || !conversationEntries.length" size="small" text @click="clearHermesContext">
          清空上下文
        </el-button>
        <el-button type="primary" :loading="asking" :disabled="!canAsk || asking" @click="submitQuestion">
          问 Hermes
        </el-button>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  askHermes,
  fetchHermesCapabilities,
  fetchHermesHealth,
  searchCatalogPreview,
  type CatalogSearchResponse,
  type HermesCapabilities,
  type HermesChatResponse,
  type HermesHealth
} from '@/modules/data-steward/api/dataSteward';
import DataStewardAnswerCard from '@/modules/data-steward/components/DataStewardAnswerCard.vue';
import { useHermesConversationStore } from '@/modules/data-steward/stores/hermesConversation';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  projectId: number;
  pageType: string;
  sourceView?: string;
  assetId?: number;
  currentRoute?: string;
  projectCode?: string;
  projectName?: string;
  pageTitle?: string;
}>();

const authStore = useAuthStore();
const panelRef = ref<HTMLElement | null>(null);
const question = ref('');
const capabilities = ref<HermesCapabilities | null>(null);
const health = ref<HermesHealth | null>(null);
const answer = ref<HermesChatResponse | null>(null);
const catalogPreview = ref<CatalogSearchResponse | null>(null);
const asking = ref(false);
const catalogLoading = ref(false);
const capabilityLoading = ref(false);
const hermesConversationStore = useHermesConversationStore();
const projectConversation = computed(() => hermesConversationStore.ensureProjectSession(props.projectId));
const sessionRef = computed(() => projectConversation.value.sessionRef);
const previousResponseRef = computed(() => projectConversation.value.previousResponseRef);
const conversationEntries = computed(() => projectConversation.value.entries);
const conversationTurnCount = computed(() => {
  return conversationEntries.value.filter((entry) => entry.role !== 'system').length;
});
const contextTitle = computed(() => props.pageTitle || pageTypeLabel(props.pageType));
const threadRef = computed(() => {
  return `thread:platform-ui:project:${safeProjectSegment(props.projectId)}:${safeRefSegment(props.pageType, 'data_steward')}`;
});
const sanitizedContextRefs = computed<Record<string, unknown>[]>(() => {
  const refs: Record<string, unknown>[] = [
    {
      refType: 'project',
      ref: `project:${safeProjectSegment(props.projectId)}`,
      source: 'platform_ui',
      revalidation: 'gateway_required'
    },
    {
      refType: 'page',
      ref: `page:${safeRefSegment(props.pageType, 'data_steward')}`,
      source: 'platform_ui',
      nativeRuntimeEnabled: false
    }
  ];
  if (Number.isFinite(props.assetId)) {
    refs.push({
      refType: 'asset',
      ref: `asset:${props.assetId}`,
      source: 'platform_ui',
      sourceView: safeRefSegment(props.sourceView || 'ProjectAssetView', 'project_asset_view')
    });
  }
  return refs;
});
const projectLabel = computed(() => {
  if (props.projectCode || props.projectName) {
    return [props.projectCode, props.projectName].filter(Boolean).join(' ');
  }
  return `项目 ${props.projectId}`;
});
const contextDescription = computed(() => {
  const routeText = props.currentRoute ? ` · ${props.currentRoute}` : '';
  return `${projectLabel.value}${routeText}`;
});
const quickQuestions = computed(() => {
  const items = [
    '这个项目有哪些上下文？',
    '当前平台开放了哪些 Hermes 能力？',
    '这个项目有哪些已登记文件？',
    '哪些资料缺少可引用证据？',
    '后续如何进入证据检索和记忆连续性？'
  ];
  if (props.pageType.includes('governance')) {
    items.splice(2, 0, '交付治理需要哪些 Hermes 上下文？');
  }
  if (props.pageType.includes('initialization') || props.pageType.includes('onboarding')) {
    items.splice(2, 0, '真实项目接入需要哪些证据？');
  }
  return items;
});

const canAsk = computed(() => Number.isFinite(props.projectId) && question.value.trim().length > 0);
const capabilityRoadmap = [
  {
    title: 'Catalog Layer',
    status: '平台当前开放',
    type: 'success' as const,
    description: '通过 Gateway 只读查询资产目录、权限、Missing Evidence 和安全引用。'
  },
  {
    title: 'Agent Context',
    status: '必要上下文',
    type: 'warning' as const,
    description: '平台传入项目、页面、资产和权限上下文；切换项目后必须失效重校。'
  },
  {
    title: 'Shared Workspace',
    status: '规划保留',
    type: 'info' as const,
    description: '后续与独立 Hermes Agent 共享 workspace、session、thread 和任务状态。'
  },
  {
    title: 'Evidence Layer',
    status: '后续解锁',
    type: 'info' as const,
    description: '只对已授权、已解析、已索引的内容开放证据检索和 citation。'
  },
  {
    title: 'Memory Layer',
    status: '低敏规划',
    type: 'info' as const,
    description: '只记录 query/trace、related ids、反馈和偏好，不写正文或 raw path。'
  },
  {
    title: 'NAS Governance',
    status: '未来接口',
    type: 'info' as const,
    description: '预留授权小批读取、解析、索引、引用和临时副本治理能力。'
  }
];

watch(
  () => props.projectId,
  (next, previous) => {
    if (previous !== undefined && next !== previous) {
      question.value = '';
      resetHermesContext('项目上下文已切换，Hermes 项目会话已失效，下一次提问将由 Gateway 重新校验。');
    }
  }
);

watch(
  () => authStore.hermesContextVersion,
  () => {
    question.value = '';
    resetHermesContext('项目、权限或角色已切换，Hermes 项目会话已失效。');
  }
);

watch(
  () => projectConversation.value.resetVersion,
  () => {
    answer.value = null;
    catalogPreview.value = null;
    question.value = '';
  }
);

onMounted(() => {
  void loadCapabilities();
});

function resetHermesContext(message = '') {
  answer.value = null;
  catalogPreview.value = null;
  hermesConversationStore.resetProject(props.projectId, message);
}

async function clearHermesContext() {
  if (asking.value) return;
  question.value = '';
  resetHermesContext('当前项目的 Hermes 上下文已清空。所有入口都会从新的项目会话继续。');
  await scrollConversationToLatest('smooth');
}

async function loadCapabilities() {
  capabilityLoading.value = true;
  try {
    const [capabilityResult, healthResult] = await Promise.all([
      fetchHermesCapabilities(),
      fetchHermesHealth()
    ]);
    capabilities.value = capabilityResult;
    health.value = healthResult;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Hermes 能力状态加载失败');
  } finally {
    capabilityLoading.value = false;
  }
}

async function submitQuestion() {
  if (!canAsk.value || asking.value) return;
  asking.value = true;
  catalogLoading.value = true;
  const text = question.value.trim();
  const previousRefForRequest = previousResponseRef.value;
  const userEntry = hermesConversationStore.createUserEntry(text, previousRefForRequest);
  hermesConversationStore.removeSystemEntries(props.projectId);
  hermesConversationStore.appendEntry(props.projectId, userEntry);
  question.value = '';
  answer.value = null;
  catalogPreview.value = null;
  void scrollConversationToLatest('smooth');
  try {
    const [answerResult, catalogResult] = await Promise.allSettled([
      askHermes({
        pageType: props.pageType,
        projectId: props.projectId,
        assetId: props.assetId,
        sourceView: props.sourceView,
        currentRoute: props.currentRoute,
        projectCode: props.projectCode,
        projectName: props.projectName,
        pageTitle: props.pageTitle,
        sessionId: sessionRef.value,
        threadId: threadRef.value,
        previousResponseId: previousRefForRequest,
        sanitizedContextRefs: sanitizedContextRefs.value,
        question: text
      }),
      searchCatalogPreview({
        query: text,
        projectFilters: [String(props.projectId)],
        filters: {
          assetKind: ['FILE'],
          indexEligibility: ['catalog_only']
        },
        page: {
          limit: 5,
          cursor: null
        }
      })
    ]);

    if (answerResult.status === 'fulfilled') {
      answer.value = answerResult.value;
      hermesConversationStore.setPreviousResponseRef(props.projectId, answerResult.value.responseId);
      hermesConversationStore.appendEntry(
        props.projectId,
        hermesConversationStore.createAssistantEntry(answerResult.value)
      );
      void scrollConversationToLatest('smooth');
    } else {
      throw answerResult.reason;
    }
    catalogPreview.value = catalogResult.status === 'fulfilled' ? catalogResult.value : null;
  } catch (error) {
    ElMessage.error(hermesErrorMessage(error));
  } finally {
    asking.value = false;
    catalogLoading.value = false;
  }
}

async function scrollConversationToLatest(behavior: ScrollBehavior = 'auto') {
  await nextTick();
  const panel = panelRef.value;
  if (!panel) return;
  panel.scrollTo({
    top: panel.scrollHeight,
    behavior
  });
}

function hermesErrorMessage(error: unknown) {
  const message = error instanceof Error ? error.message : '';
  if (/timeout|exceeded|ECONNABORTED/i.test(message)) {
    return 'Hermes catalog-only 回答超时，请稍后重试；平台未执行任何写操作。';
  }
  return message || 'Hermes catalog-only 通道暂时无法回答，请稍后重试；平台未执行任何写操作。';
}

async function askQuickQuestion(value: string) {
  if (asking.value) return;
  question.value = value;
  await submitQuestion();
}

function fileKindLabel(value: string) {
  const labels: Record<string, string> = {
    MODEL: '模型',
    DRAWING: '图纸',
    DOCUMENT: '文档',
    SPREADSHEET: '表格',
    PRESENTATION: '汇报',
    ARCHIVE: '归档',
    FILE: '文件'
  };
  return labels[value] ?? value;
}

function modeLabel(mode: string) {
  const labels: Record<string, string> = {
    catalog_only: '平台 catalog-only 安全开放',
    read_only_gateway: 'Platform Gateway 只读包装'
  };
  return labels[mode] ?? mode;
}

function pageTypeLabel(value: string) {
  const labels: Record<string, string> = {
    assets_overview: '资产总览',
    project_detail: '项目工作台',
    real_project_onboarding: '真实项目接入向导',
    g2_real_project_onboarding: '真实项目接入向导',
    agent_governance: '交付治理助手',
    'agent-governance': '交付治理助手'
  };
  return labels[value] ?? value;
}

function compactRef(value: string) {
  if (!value) return '-';
  return value.length > 18 ? `...${value.slice(-12)}` : value;
}

function safeProjectSegment(projectId: number) {
  return Number.isFinite(projectId) ? String(projectId) : 'unknown';
}

function safeRefSegment(value: string, fallback: string) {
  const segment = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '_')
    .replace(/^_+|_+$/g, '');
  return segment ? segment.slice(0, 48) : fallback;
}

function healthLabel(value: HermesHealth | null) {
  if (!value) return '检测中';
  if (value.hermesAvailable) return 'Hermes 可用';
  if (!value.gatewayEnabled) return '本地目录兜底';
  return 'Hermes 暂不可用';
}

function healthTagType(value: HermesHealth | null) {
  if (!value) return 'info';
  if (value.hermesAvailable) return 'success';
  if (!value.gatewayEnabled) return 'info';
  return 'warning';
}
</script>

<style scoped>
.data-steward-panel {
  display: grid;
  gap: 14px;
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 20px 0 24px;
  scroll-padding-bottom: 220px;
}

.data-steward-panel__head {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
}

.data-steward-panel__head h3 {
  font-size: 18px;
  margin: 0 0 4px;
}

.data-steward-panel__head p {
  color: var(--el-text-color-secondary);
  margin: 0;
}

.data-steward-panel__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.data-steward-panel__context {
  background: #f8fafc;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 12px;
}

.data-steward-panel__context div {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__context span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__context strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.data-steward-panel__context p {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  margin: 0;
  overflow-wrap: anywhere;
}

.data-steward-panel__context small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.data-steward-panel__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__actions {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.data-steward-panel__ask {
  background: color-mix(in srgb, var(--el-fill-color-blank) 94%, #e8f3ff 6%);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  bottom: 0;
  box-shadow: 0 -12px 24px rgb(15 23 42 / 0.08);
  display: grid;
  gap: 12px;
  margin: 2px -20px 0 -24px;
  min-width: 0;
  padding: 12px 20px 14px 24px;
  position: sticky;
  z-index: 4;
}

.data-steward-panel__ask > header {
  align-items: flex-start;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
}

.data-steward-panel__ask > header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.data-steward-panel__ask strong,
.data-steward-panel__catalog strong {
  color: var(--el-text-color-primary);
}

.data-steward-panel__ask span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.data-steward-panel__followup-hint {
  color: var(--el-text-color-secondary);
  flex: 1;
  font-size: 12px;
  min-width: 0;
  overflow-wrap: anywhere;
}

.data-steward-panel__compact-list {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.data-steward-panel__compact-list {
  margin: 0;
  padding-left: 18px;
}

.data-steward-panel__compact-list li + li {
  margin-top: 4px;
}

.data-steward-panel__catalog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.data-steward-panel__thinking {
  min-width: 0;
}

.data-steward-panel__conversation {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.data-steward-panel__turn {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__turn--user {
  justify-items: end;
}

.data-steward-panel__turn--assistant {
  justify-items: stretch;
}

.data-steward-panel__turn-meta {
  align-items: center;
  color: var(--el-text-color-secondary);
  display: flex;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__turn-meta span {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.data-steward-panel__turn-meta small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  overflow-wrap: anywhere;
}

.data-steward-panel__turn--user p {
  background: #e8f3ff;
  border: 1px solid #c7e0ff;
  border-radius: 8px;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  margin: 0;
  max-width: min(560px, 88%);
  min-width: 0;
  overflow-wrap: anywhere;
  padding: 10px 12px;
}

.data-steward-panel__system-note {
  background: #fff8e6;
  border: 1px solid #f2dfaa;
  border-radius: 8px;
  color: #7a5b10;
  font-size: 12px;
  line-height: 1.6;
  min-width: 0;
  overflow-wrap: anywhere;
  padding: 10px 12px;
}

.data-steward-panel__catalog {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 12px;
}

.data-steward-panel__catalog header {
  align-items: flex-start;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
}

.data-steward-panel__catalog header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.data-steward-panel__catalog header span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__catalog-list {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__catalog-item {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px;
}

.data-steward-panel__catalog-item strong,
.data-steward-panel__catalog-item span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-steward-panel__catalog-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__capabilities {
  --el-collapse-header-height: 36px;
}

.data-steward-panel__capability-title {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__capability-copy {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
  margin: 0 0 10px;
}

.data-steward-panel__roadmap {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.data-steward-panel__roadmap article {
  align-items: flex-start;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
  min-width: 0;
  padding: 10px 12px;
}

.data-steward-panel__roadmap article > div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.data-steward-panel__roadmap strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.data-steward-panel__roadmap span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .data-steward-panel {
    padding: 14px 14px 0;
  }

  .data-steward-panel__ask > header,
  .data-steward-panel__catalog header {
    align-items: stretch;
    flex-direction: column;
  }

  .data-steward-panel__ask {
    margin: 0 -14px;
    padding: 12px 14px 14px;
  }

  .data-steward-panel__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .data-steward-panel__roadmap article {
    grid-template-columns: 1fr;
  }
}
</style>
