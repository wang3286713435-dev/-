<template>
  <section class="workspace-boundary-ui" aria-label="安全边界提示">
    <strong>{{ title }}</strong>
    <span v-for="item in items" :key="item">{{ item }}</span>
    <button v-if="actionLabel" type="button" @click="$emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string;
  items: string[];
  actionLabel?: string;
}>(), {
  title: '安全边界提示',
  actionLabel: ''
});

defineEmits<{
  action: [];
}>();
</script>

<style scoped>
.workspace-boundary-ui {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 10px 16px;
  border: 1px solid var(--ux4-blue-border);
  border-radius: var(--ux4-radius-card);
  background: var(--ux4-blue-soft);
  color: var(--ux4-text);
  font-size: 13px;
}

.workspace-boundary-ui strong {
  color: var(--ux4-blue);
  font-weight: 650;
}

.workspace-boundary-ui span {
  flex: 0 0 auto;
  padding: 3px 10px;
  border: 1px solid var(--ux4-blue-border);
  border-radius: 999px;
  background: var(--ux4-surface);
  color: var(--ux4-text);
}

.workspace-boundary-ui button {
  appearance: none;
  min-height: 36px;
  margin-left: auto;
  border: 0;
  background: transparent;
  color: var(--ux4-blue);
  cursor: pointer;
  font-family: inherit;
  font-weight: 650;
}

.workspace-boundary-ui button:focus-visible {
  outline: 3px solid var(--ux4-focus);
  outline-offset: 2px;
}

@media (max-width: 1280px) {
  .workspace-boundary-ui {
    flex-wrap: wrap;
  }

  .workspace-boundary-ui button {
    margin-left: 0;
  }
}
</style>
