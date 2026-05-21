<template>
  <section class="data-steward-answer">
    <header class="data-steward-answer__head">
      <div>
        <EvidenceModeBadge :mode="response.evidenceMode" />
        <el-tag :type="statusType" size="small">{{ statusLabel }}</el-tag>
      </div>
      <el-tag size="small" :type="answerSourceTagType">{{ answerSourceLabel }}</el-tag>
    </header>

    <section class="data-steward-answer__body" aria-label="Hermes 回答">
      <div class="data-steward-answer__body-head">
        <strong>回答</strong>
        <span>按 catalog-only 边界渲染 Markdown，不代表已读取文件正文。</span>
      </div>
      <SafeMarkdownRenderer :content="response.answer" />
    </section>

    <MissingEvidenceNotice :items="response.missingEvidence" />

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

    <section class="data-steward-answer__boundary" aria-label="安全边界">
      <div>
        <span>权限</span>
        <strong>{{ response.permission.permissionStatus === 'allowed' ? '允许' : '拒绝' }}</strong>
      </div>
      <div>
        <span>证据</span>
        <strong>{{ response.assetCatalogOnly ? 'catalog-only' : response.evidenceMode }}</strong>
      </div>
      <div>
        <span>项目范围</span>
        <strong>{{ response.permission.projectScopeChecked ? '已校验' : '未校验' }}</strong>
      </div>
      <div>
        <span>安全兜底</span>
        <strong>{{ response.permission.failClosedApplied ? '已触发' : '未触发' }}</strong>
      </div>
    </section>

    <el-alert
      v-if="response.permission.reasonCode"
      type="warning"
      :title="reasonLabel(response.permission.reasonCode)"
      :closable="false"
      show-icon
    />

    <el-collapse class="data-steward-answer__diagnostic">
      <el-collapse-item name="diagnostic">
        <template #title>
          <span class="data-steward-answer__diagnostic-title">
            追踪信息
            <small>{{ compactDiagnosticId }}</small>
          </span>
        </template>
        <dl class="data-steward-answer__diagnostic-list">
          <div>
            <dt>query_id</dt>
            <dd>{{ response.queryId || '-' }}</dd>
          </div>
          <div>
            <dt>response_id</dt>
            <dd>{{ response.responseId || '-' }}</dd>
          </div>
          <div>
            <dt>trace_id</dt>
            <dd>{{ response.traceId || '-' }}</dd>
          </div>
          <div>
            <dt>request_id</dt>
            <dd>{{ response.trace.requestId || '-' }}</dd>
          </div>
          <div>
            <dt>session_ref</dt>
            <dd>{{ response.sessionRef || '-' }}</dd>
          </div>
          <div>
            <dt>thread_ref</dt>
            <dd>{{ response.threadRef || '-' }}</dd>
          </div>
          <div>
            <dt>previous_response_ref</dt>
            <dd>{{ response.previousResponseRef || '-' }}</dd>
          </div>
          <div>
            <dt>authority_health</dt>
            <dd>{{ authorityHealthText }}</dd>
          </div>
          <div>
            <dt>context_refs</dt>
            <dd>{{ response.sanitizedContextRefs.length }} safe refs</dd>
          </div>
          <div>
            <dt>memory_candidates</dt>
            <dd>{{ response.safeMemoryCandidates.length }} candidates</dd>
          </div>
        </dl>
      </el-collapse-item>
    </el-collapse>

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
      title="Hermes 不会自动执行操作。下方内容只是平台治理建议草案。"
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
import SafeMarkdownRenderer from '@/modules/data-steward/components/SafeMarkdownRenderer.vue';

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
    ? 'Hermes catalog-only 回答'
    : '平台安全兜底';
});

const answerSourceTagType = computed(() => {
  return props.response.trace?.agentMode === 'openai_compatible_catalog_only' ? 'success' : 'info';
});

const compactDiagnosticId = computed(() => {
  const value = props.response.queryId || props.response.traceId || props.response.trace.requestId || '';
  return value ? `...${value.slice(-8)}` : '-';
});

const authorityHealthText = computed(() => {
  const health = props.response.authorityHealth;
  return `${health.architectureAuthorityHealth} / ${health.mode}`;
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
    REQUIRED_ACTIONS_MISSING: '缺少目录查询能力',
    USER_PROMPT_FORBIDDEN_FIELD_DETECTED: '问题命中安全红线，已 fail closed',
    HERMES_RESPONSE_FORBIDDEN_FIELD_DETECTED: '回答命中安全红线，已 fail closed'
  };
  return labels[value] ?? value;
}
</script>

<style scoped>
.data-steward-answer {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 14px;
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

.data-steward-answer__body {
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
}

.data-steward-answer__body-head {
  display: grid;
  gap: 3px;
}

.data-steward-answer__body-head strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.data-steward-answer__body-head span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.data-steward-answer__boundary {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.data-steward-answer__boundary div {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 9px 10px;
}

.data-steward-answer__boundary span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-answer__boundary strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
  overflow-wrap: anywhere;
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

@media (max-width: 640px) {
  .data-steward-answer__head {
    align-items: flex-start;
    flex-direction: column;
  }

  .data-steward-answer__boundary {
    grid-template-columns: 1fr;
  }
}
</style>
