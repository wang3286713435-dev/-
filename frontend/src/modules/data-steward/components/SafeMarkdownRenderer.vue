<template>
  <article class="safe-markdown" v-html="html" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { renderSafeMarkdown } from '@/modules/data-steward/utils/safeMarkdown';

const props = defineProps<{
  content: string;
}>();

const html = computed(() => renderSafeMarkdown(props.content || ''));
</script>

<style scoped>
.safe-markdown {
  color: var(--el-text-color-primary);
  font-size: 14px;
  line-height: 1.75;
  min-width: 0;
  overflow-wrap: anywhere;
}

.safe-markdown :deep(*) {
  box-sizing: border-box;
}

.safe-markdown :deep(:first-child) {
  margin-top: 0;
}

.safe-markdown :deep(:last-child) {
  margin-bottom: 0;
}

.safe-markdown :deep(h1),
.safe-markdown :deep(h2),
.safe-markdown :deep(h3),
.safe-markdown :deep(h4) {
  color: var(--el-text-color-primary);
  line-height: 1.35;
  margin: 18px 0 8px;
}

.safe-markdown :deep(h1) {
  font-size: 20px;
}

.safe-markdown :deep(h2) {
  font-size: 18px;
}

.safe-markdown :deep(h3) {
  font-size: 16px;
}

.safe-markdown :deep(h4) {
  font-size: 15px;
}

.safe-markdown :deep(p),
.safe-markdown :deep(ul),
.safe-markdown :deep(ol),
.safe-markdown :deep(blockquote),
.safe-markdown :deep(pre),
.safe-markdown :deep(table) {
  margin: 0 0 12px;
}

.safe-markdown :deep(ul),
.safe-markdown :deep(ol) {
  padding-left: 20px;
}

.safe-markdown :deep(li + li) {
  margin-top: 4px;
}

.safe-markdown :deep(blockquote) {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  padding: 10px 12px;
}

.safe-markdown :deep(code) {
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 5px;
  color: var(--el-color-danger);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  padding: 1px 5px;
}

.safe-markdown :deep(pre) {
  background: #111827;
  border-radius: 8px;
  color: #e5e7eb;
  max-width: 100%;
  overflow-x: auto;
  padding: 12px;
}

.safe-markdown :deep(pre code) {
  background: transparent;
  border: 0;
  color: inherit;
  display: block;
  padding: 0;
}

.safe-markdown :deep(table) {
  border-collapse: collapse;
  display: block;
  max-width: 100%;
  overflow-x: auto;
  width: 100%;
}

.safe-markdown :deep(th),
.safe-markdown :deep(td) {
  border: 1px solid var(--el-border-color);
  padding: 8px 10px;
  text-align: left;
  vertical-align: top;
}

.safe-markdown :deep(th) {
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.safe-markdown :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.safe-markdown :deep(a:hover),
.safe-markdown :deep(a:focus-visible) {
  text-decoration: underline;
}

.safe-markdown :deep(input[type='checkbox']) {
  margin-right: 6px;
}
</style>
