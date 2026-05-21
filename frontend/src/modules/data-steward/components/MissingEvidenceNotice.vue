<template>
  <section v-if="items.length" class="missing-evidence" aria-label="缺少证据">
    <header>
      <strong>缺少证据</strong>
      <span>Hermes 会说明当前不能证明什么，而不是把目录元数据当成正文证据。</span>
    </header>

    <article v-for="item in items" :key="`${item.reason}-${item.message}`" class="missing-evidence__item">
      <div>
        <el-tag type="warning" size="small">{{ reasonLabel(item.reason) }}</el-tag>
        <strong>{{ item.message }}</strong>
      </div>
      <p>{{ nextStepLabel(item.reason) }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { HermesMissingEvidence } from '@/modules/data-steward/api/dataSteward';

defineProps<{
  items: HermesMissingEvidence[];
}>();

function reasonLabel(reason: string) {
  const labels: Record<string, string> = {
    asset_catalog_only: '资产目录级',
    index_eligibility_catalog_only: '索引资格限制',
    confidentiality_unknown: '密级待确认',
    lifecycle_not_active: '生命周期待复核',
    permission_denied: '权限拒绝',
    agent_unavailable: 'Hermes 暂不可用',
    PERMISSION_TAGS_MISSING: '权限标签缺失',
    MISSING_PROJECT_SCOPE: '缺少项目范围'
  };
  return labels[reason] ?? reason;
}

function nextStepLabel(reason: string) {
  const labels: Record<string, string> = {
    asset_catalog_only: '下一步：使用平台目录、预览状态或人工复核入口确认文件内容。',
    index_eligibility_catalog_only: '下一步：等待正文证据索引或解析能力开放后再做内容级判断。',
    confidentiality_unknown: '下一步：先确认密级和权限标签，再决定是否允许进一步处理。',
    lifecycle_not_active: '下一步：先复核资产生命周期，避免引用过期或未生效资料。',
    permission_denied: '下一步：切换到有权限的项目或联系项目管理员确认授权。',
    agent_unavailable: '下一步：稍后刷新 Hermes 状态，当前平台未执行任何写操作。',
    PERMISSION_TAGS_MISSING: '下一步：补齐权限标签后再重新发起目录级问答。',
    MISSING_PROJECT_SCOPE: '下一步：先选择有效项目，Gateway 会重新校验项目范围。'
  };
  return labels[reason] ?? '下一步：请在平台中补齐对应证据或安排人工复核。';
}
</script>

<style scoped>
.missing-evidence {
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: 8px;
  display: grid;
  gap: 10px;
  padding: 12px;
}

.missing-evidence header,
.missing-evidence__item,
.missing-evidence__item div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.missing-evidence header strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.missing-evidence header span,
.missing-evidence__item p {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.missing-evidence__item {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 10px;
}

.missing-evidence__item strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.missing-evidence__item p {
  margin: 0;
}
</style>
