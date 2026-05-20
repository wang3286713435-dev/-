<template>
  <section class="data-steward-answer">
    <header class="data-steward-answer__head">
      <div>
        <EvidenceModeBadge :mode="response.evidenceMode" />
        <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
      </div>
      <el-tag size="small" :type="answerSourceTagType">{{ answerSourceLabel }}</el-tag>
    </header>

    <p class="data-steward-answer__text">{{ response.answer }}</p>

    <section v-if="response.pathHints?.length" class="data-steward-answer__paths">
      <header>
        <strong>路径提示</strong>
        <el-tag size="small" type="info">path_not_exposable</el-tag>
      </header>
      <div
        v-for="path in response.pathHints"
        :key="`${path.provider}-${path.matchStrategy}-${path.displayPath}`"
        class="data-steward-answer__path"
      >
        <span>{{ path.displayPath }}</span>
        <small>{{ path.pathHint }}</small>
      </div>
    </section>

    <el-descriptions :column="1" border size="small">
      <el-descriptions-item label="权限">
        <el-tag :type="response.permission.permissionStatus === 'allowed' ? 'success' : 'danger'" size="small">
          {{ response.permission.permissionStatus === 'allowed' ? '允许' : '拒绝' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="证据模式">
        {{ response.assetCatalogOnly ? 'catalog-only' : response.evidenceMode }}
      </el-descriptions-item>
      <el-descriptions-item label="资产来源">
        {{ response.sourceView || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="项目范围">
        {{ response.permission.projectScopeChecked ? '已校验' : '未校验' }}
      </el-descriptions-item>
      <el-descriptions-item label="权限标签">
        {{ response.permission.permissionTagsChecked ? '已校验' : '未校验' }}
      </el-descriptions-item>
      <el-descriptions-item label="安全兜底">
        {{ response.permission.failClosedApplied ? '已启用 fail closed' : '未触发' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="response.permission.reasonCode" label="原因">
        {{ reasonLabel(response.permission.reasonCode) }}
      </el-descriptions-item>
    </el-descriptions>

    <el-collapse class="data-steward-answer__diagnostic">
      <el-collapse-item name="diagnostic">
        <template #title>
          <span class="data-steward-answer__diagnostic-title">
            诊断编号
            <small>{{ compactDiagnosticId }}</small>
          </span>
        </template>
        <dl class="data-steward-answer__diagnostic-list">
          <div>
            <dt>query_id</dt>
            <dd>{{ response.queryId || '-' }}</dd>
          </div>
          <div>
            <dt>trace_id</dt>
            <dd>{{ response.traceId || '-' }}</dd>
          </div>
          <div>
            <dt>request_id</dt>
            <dd>{{ response.trace.requestId || '-' }}</dd>
          </div>
        </dl>
      </el-collapse-item>
    </el-collapse>

    <MissingEvidenceNotice :items="response.missingEvidence" />

    <section v-if="response.citations.length" class="data-steward-answer__citations">
      <strong>引用范围</strong>
      <el-tag
        v-for="citation in response.citations"
        :key="`${citation.sourceView}-${citation.assetRef}-${citation.projectRef}`"
        size="small"
        type="info"
      >
        {{ citation.sourceView }} / {{ citation.citationType }}
      </el-tag>
    </section>

    <el-alert
      type="info"
      title="Hermes 不会自动执行操作。下方建议仅作为人工审批前的草案。"
      :closable="false"
      show-icon
    />

    <OperationPlanPreview :plan="response.operationPlan" />
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { HermesChatResponse } from '@/modules/data-steward/api/dataSteward';
import EvidenceModeBadge from '@/modules/data-steward/components/EvidenceModeBadge.vue';
import MissingEvidenceNotice from '@/modules/data-steward/components/MissingEvidenceNotice.vue';
import OperationPlanPreview from '@/modules/data-steward/components/OperationPlanPreview.vue';

const props = defineProps<{
  response: HermesChatResponse;
}>();

const statusLabel = computed(() => {
  const labels: Record<string, string> = {
    catalog_only: '目录辅助',
    missing_evidence: '缺少证据',
    denied: '权限拒绝',
    error: '不可用',
    ok: '完成'
  };
  return labels[props.response.status] ?? props.response.status;
});

const statusType = computed(() => {
  if (props.response.status === 'denied' || props.response.status === 'error') return 'danger';
  if (props.response.status === 'missing_evidence') return 'warning';
  return 'success';
});

const answerSourceLabel = computed(() => {
  return props.response.trace?.agentMode === 'openai_compatible_catalog_only'
    ? '真实 Hermes 回答'
    : '平台安全兜底';
});

const answerSourceTagType = computed(() => {
  return props.response.trace?.agentMode === 'openai_compatible_catalog_only' ? 'success' : 'info';
});

const compactDiagnosticId = computed(() => {
  const value = props.response.queryId || props.response.traceId || props.response.trace.requestId || '';
  return value ? `...${value.slice(-8)}` : '-';
});

function reasonLabel(value: string) {
  const labels: Record<string, string> = {
    MISSING_PROJECT_SCOPE: '缺少项目范围',
    UNSUPPORTED_SOURCE_VIEW: '不支持的资产来源',
    ASSET_CONTEXT_NOT_FOUND: '资产上下文不存在或不可用',
    ASSET_PROJECT_SCOPE_MISMATCH: '资产不属于当前项目',
    PERMISSION_TAGS_MISSING: '权限标签缺失',
    PROJECT_SCOPE_DENIED: '当前用户无权访问该项目',
    GATEWAY_NOT_READONLY: '网关未处于只读模式',
    REQUIRED_ACTIONS_MISSING: '缺少目录查询能力'
  };
  return labels[value] ?? value;
}
</script>

<style scoped>
.data-steward-answer {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  display: grid;
  gap: 12px;
  padding: 12px;
}

.data-steward-answer__head {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.data-steward-answer__head > div {
  display: flex;
  gap: 8px;
}

.data-steward-answer__text {
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
}

.data-steward-answer__paths {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px;
}

.data-steward-answer__paths header {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.data-steward-answer__path {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.data-steward-answer__path span,
.data-steward-answer__path small {
  min-width: 0;
  overflow-wrap: anywhere;
}

.data-steward-answer__path small {
  color: var(--el-text-color-secondary);
}

.data-steward-answer__citations {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.data-steward-answer__diagnostic {
  --el-collapse-header-height: 34px;
}

.data-steward-answer__diagnostic-title {
  align-items: center;
  color: var(--el-text-color-secondary);
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.data-steward-answer__diagnostic-title small,
.data-steward-answer__diagnostic-list dd {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.data-steward-answer__diagnostic-list {
  display: grid;
  gap: 6px;
  margin: 0;
}

.data-steward-answer__diagnostic-list div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.data-steward-answer__diagnostic-list dt,
.data-steward-answer__diagnostic-list dd {
  margin: 0;
  min-width: 0;
  overflow-wrap: anywhere;
}

.data-steward-answer__diagnostic-list dt {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
