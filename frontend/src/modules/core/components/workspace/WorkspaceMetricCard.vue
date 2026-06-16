<template>
  <article class="workspace-metric-card" :class="`workspace-metric-card--${tone}`">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
    <em>{{ helper }}</em>
  </article>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string;
  value: string | number;
  helper?: string;
  tone?: 'default' | 'accent' | 'success' | 'warning' | 'danger';
}>(), {
  helper: '',
  tone: 'default'
});
</script>

<style scoped>
.workspace-metric-card {
  display: grid;
  gap: 5px;
  min-width: 0;
  min-height: 104px;
  padding: 16px;
  border: 1px solid var(--ux4-border);
  border-radius: var(--ux4-radius-card);
  background: var(--ux4-surface-soft);
}

.workspace-metric-card--accent {
  border-color: var(--ux4-blue-border);
  background: var(--ux4-blue-soft);
}

.workspace-metric-card--success {
  border-color: var(--ux4-green-border);
  background: var(--ux4-green-soft);
}

.workspace-metric-card--warning {
  border-color: var(--ux4-amber-border);
  background: var(--ux4-amber-soft);
}

.workspace-metric-card--danger {
  border-color: var(--ux4-red-border);
  background: var(--ux4-red-soft);
}

.workspace-metric-card span {
  color: var(--ux4-text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.workspace-metric-card strong {
  color: var(--ux4-text-strong);
  font-size: 22px;
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  line-height: 1.15;
}

.workspace-metric-card em {
  color: var(--ux4-text-soft);
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
}
</style>
