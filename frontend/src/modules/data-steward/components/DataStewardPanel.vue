<template>
  <section class="data-steward-panel">
    <header class="data-steward-panel__head">
      <div>
        <h3>Hermes 数据管家</h3>
        <p>只基于资产目录、受控项目路径、权限证明和当前项目上下文回答。</p>
      </div>
      <div class="data-steward-panel__badges">
        <EvidenceModeBadge mode="catalog_only" />
        <el-tag :type="healthTagType(health)" size="small">{{ healthLabel(health) }}</el-tag>
      </div>
    </header>

    <el-alert
      type="info"
      title="当前不读取 PDF、Office、CAD、BIM 正文，不执行数据库或 NAS 写操作。"
      :closable="false"
      show-icon
    />

    <section class="data-steward-panel__context" aria-label="当前页面上下文">
      <div>
        <span>当前上下文</span>
        <strong>{{ contextTitle }}</strong>
      </div>
      <p>{{ contextDescription }}</p>
    </section>

    <div class="data-steward-panel__status">
      <div class="data-steward-panel__status-item">
        <span>当前模式</span>
        <strong>资产目录辅助</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>正文问答</span>
        <strong>未开放</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>项目路径</span>
        <strong>受控查询</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>写操作</span>
        <strong>不会执行</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>生产发布</span>
        <strong>未开放</strong>
      </div>
    </div>

    <el-descriptions v-if="capabilities" :column="1" border size="small">
      <el-descriptions-item label="能力名称">{{ capabilities.agentName }}</el-descriptions-item>
      <el-descriptions-item label="模式">{{ modeLabel(capabilities.mode) }}</el-descriptions-item>
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
      <el-descriptions-item v-if="health" label="Hermes真实回答">
        {{ health.agentAnswerIntegrationEnabled ? '已接入' : '未接入' }}
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

    <el-input
      v-model="question"
      type="textarea"
      :rows="4"
      maxlength="500"
      show-word-limit
      :disabled="asking"
      placeholder="问 Hermes，例如：这个项目路径在哪里？这个项目有哪些已登记文件？哪些资料缺少可引用证据？"
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
      <el-button type="primary" :loading="asking" :disabled="!canAsk || asking" @click="submitQuestion">
        问 Hermes
      </el-button>
      <el-button :loading="capabilityLoading" @click="loadCapabilities">刷新能力</el-button>
    </div>

    <el-alert
      v-if="asking"
      class="data-steward-panel__thinking"
      type="info"
      title="真实 Hermes 正在组织回答，可能需要 10-30 秒。平台未执行任何写操作。"
      :closable="false"
      show-icon
    />

    <DataStewardAnswerCard v-if="answer" :response="answer" />

    <section v-if="catalogLoading || catalogPreview" class="data-steward-panel__catalog">
      <header>
        <div>
          <strong>资产目录预览</strong>
          <span>仅展示目录元数据，不代表已读取文件正文。</span>
        </div>
        <el-tag size="small" type="info">asset_catalog_preview</el-tag>
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
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
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
import EvidenceModeBadge from '@/modules/data-steward/components/EvidenceModeBadge.vue';

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

const question = ref('');
const capabilities = ref<HermesCapabilities | null>(null);
const health = ref<HermesHealth | null>(null);
const answer = ref<HermesChatResponse | null>(null);
const catalogPreview = ref<CatalogSearchResponse | null>(null);
const asking = ref(false);
const catalogLoading = ref(false);
const capabilityLoading = ref(false);
const contextTitle = computed(() => props.pageTitle || pageTypeLabel(props.pageType));
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
    '这个页面是干什么的？',
    '我下一步应该做什么？',
    '这个项目路径在哪里？',
    '这个项目有哪些已登记文件？',
    '哪些资料缺少可引用证据？'
  ];
  if (props.pageType.includes('governance')) {
    items.splice(2, 0, '交付治理助手能做什么？');
  }
  if (props.pageType.includes('initialization') || props.pageType.includes('onboarding')) {
    items.splice(2, 0, '为什么模板只是草案？');
  }
  return items;
});

const canAsk = computed(() => Number.isFinite(props.projectId) && question.value.trim().length > 0);

watch(
  () => [props.projectId, props.assetId, props.sourceView] as const,
  () => {
    answer.value = null;
    catalogPreview.value = null;
  }
);

onMounted(() => {
  void loadCapabilities();
});

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
    ElMessage.error(error instanceof Error ? error.message : '数据管家能力加载失败');
  } finally {
    capabilityLoading.value = false;
  }
}

async function submitQuestion() {
  if (!canAsk.value || asking.value) return;
  asking.value = true;
  catalogLoading.value = true;
  const text = question.value.trim();
  answer.value = null;
  catalogPreview.value = null;
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

function hermesErrorMessage(error: unknown) {
  const message = error instanceof Error ? error.message : '';
  if (/timeout|exceeded|ECONNABORTED/i.test(message)) {
    return '真实 Hermes 回答超时，请稍后重试；平台未执行任何写操作。';
  }
  return message || '真实 Hermes 暂时无法回答，请稍后重试；平台未执行任何写操作。';
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
    catalog_only: '资产目录辅助',
    read_only_gateway: '只读网关'
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

.data-steward-panel__status {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.data-steward-panel__status-item {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
}

.data-steward-panel__status-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__status-item strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.data-steward-panel__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.data-steward-panel__thinking {
  min-width: 0;
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

.data-steward-panel__catalog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

@media (max-width: 640px) {
  .data-steward-panel__status {
    grid-template-columns: 1fr;
  }
}
</style>
