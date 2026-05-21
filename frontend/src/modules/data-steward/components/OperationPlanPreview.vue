<template>
  <section class="operation-plan-preview" aria-label="治理建议草案">
    <div class="operation-plan-preview__head">
      <strong>治理建议草案</strong>
      <el-tag :type="plan.available ? 'warning' : 'info'" size="small">
        {{ plan.available ? '草案' : '不可用' }}
      </el-tag>
    </div>
    <p>
      {{ plan.requiresHumanApproval
        ? 'Hermes 只生成建议草案，不在抽屉内执行任何平台写操作。'
        : '当前没有可执行动作。' }}
    </p>
    <div v-if="plan.actions.length" class="operation-plan-preview__actions">
      <el-tag v-for="action in plan.actions" :key="`${action.actionType}-${action.status}`" size="small">
        {{ actionLabel(action.actionType) }} / {{ statusLabel(action.status) }}
      </el-tag>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { HermesOperationPlan } from '@/modules/data-steward/api/dataSteward';

defineProps<{
  plan: HermesOperationPlan;
}>();

function actionLabel(value: string) {
  const labels: Record<string, string> = {
    manual_review_required: '需要人工复核'
  };
  return labels[value] ?? value;
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    draft_only: '仅草案'
  };
  return labels[value] ?? value;
}
</script>

<style scoped>
.operation-plan-preview {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
}

.operation-plan-preview__head {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.operation-plan-preview p {
  color: var(--el-text-color-secondary);
  margin: 0 0 8px;
}

.operation-plan-preview__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
