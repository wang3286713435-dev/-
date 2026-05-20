<template>
  <div v-if="items.length" class="missing-evidence">
    <el-alert
      v-for="item in items"
      :key="`${item.reason}-${item.message}`"
      type="warning"
      :title="item.message"
      :closable="false"
      show-icon
    >
      <template #default>
        <span class="missing-evidence__reason">{{ reasonLabel(item.reason) }}</span>
      </template>
    </el-alert>
  </div>
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
</script>

<style scoped>
.missing-evidence {
  display: grid;
  gap: 8px;
}

.missing-evidence__reason {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
