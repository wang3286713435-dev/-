<template>
  <el-tag :type="tagType" size="small">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  mode: string;
}>();

const label = computed(() => {
  const labels: Record<string, string> = {
    catalog_only: '资产目录辅助',
    missing_evidence: '缺少正文证据',
    denied: '权限拒绝',
    operation_plan_draft: '操作草案'
  };
  return labels[props.mode] ?? props.mode;
});

const tagType = computed(() => {
  if (props.mode === 'missing_evidence') return 'warning';
  if (props.mode === 'denied') return 'danger';
  if (props.mode === 'catalog_only') return 'info';
  return 'info';
});
</script>
