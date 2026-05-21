<template>
  <aside class="directory-tree-panel" :class="{ 'is-disabled': !enabled }">
    <div class="directory-tree-panel__header">
      <strong>目录结构</strong>
      <span>{{ rootLabel }}</span>
    </div>

    <el-empty
      v-if="!enabled"
      :description="disabledDescription"
      :image-size="72"
      class="directory-tree-panel__empty"
    />
    <el-empty
      v-else-if="!loading && model.nodes.length === 0"
      :description="emptyDescription"
      :image-size="72"
      class="directory-tree-panel__empty"
    />
    <div v-else v-loading="loading" class="directory-tree-panel__scroll">
      <div class="directory-tree-panel__content">
        <div class="directory-tree-panel__root">
          <button
            class="directory-tree-panel__root-button"
            :class="{ 'is-active': activePath === '' }"
            type="button"
            @click="$emit('select', '')"
            @dblclick="$emit('enter', '')"
          >
            <span class="directory-tree-panel__root-title">{{ rootLabel }}</span>
            <el-tag size="small" type="info">{{ model.rootFileCount }}</el-tag>
          </button>
          <span class="directory-tree-panel__root-helper">项目根目录</span>
        </div>

        <ul class="directory-tree-panel__list">
          <DirectoryTreeNodeItem
            v-for="node in model.nodes"
            :key="node.id"
        :node="node"
        :active-path="activePath"
        :expanded-paths="expandedPaths"
        @select="$emit('select', $event)"
        @enter="$emit('enter', $event)"
        @toggle-expand="(path, isExpanded) => $emit('toggle-expand', path, isExpanded)"
      />
        </ul>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { CatalogDirectory } from '@/modules/data-steward/api/dataSteward';
import { buildDirectoryTree } from '@/modules/data-steward/utils/directoryTree';
import DirectoryTreeNodeItem from '@/modules/data-steward/components/DirectoryTreeNodeItem.vue';

const props = withDefaults(defineProps<{
  directories: CatalogDirectory[];
  activePath: string;
  expandedPaths?: string[];
  rootLabel: string;
  enabled?: boolean;
  loading?: boolean;
  emptyDescription?: string;
  disabledDescription?: string;
}>(), {
  enabled: true,
  loading: false,
  expandedPaths: () => [],
  emptyDescription: '暂无目录',
  disabledDescription: '选择项目后浏览目录'
});

defineEmits<{
  select: [path: string];
  enter: [path: string];
  'toggle-expand': [path: string, expanded: boolean];
}>();

const model = computed(() => buildDirectoryTree(props.directories));
</script>

<style scoped>
.directory-tree-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  min-height: 520px;
  height: 100%;
  overflow: hidden;
}

.directory-tree-panel__header {
  display: grid;
  gap: 4px;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border-bottom: var(--zy-border-soft);
  background: var(--zy-surface-soft);
}

.directory-tree-panel__header strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.directory-tree-panel__header span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.directory-tree-panel__empty {
  min-height: 300px;
}

.directory-tree-panel__scroll {
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable both-edges;
}

.directory-tree-panel__content {
  min-width: max-content;
  padding: var(--zy-sp-3) var(--zy-sp-3) var(--zy-sp-4);
  font-family: var(--zy-font-sans);
}

.directory-tree-panel__root {
  display: grid;
  gap: 4px;
  margin-bottom: var(--zy-sp-2);
}

.directory-tree-panel__root-button {
  display: inline-flex;
  align-items: center;
  gap: var(--zy-sp-2);
  width: fit-content;
  min-width: max-content;
  padding: 6px 10px;
  border: 0;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-bg);
  color: var(--zy-ink);
  cursor: pointer;
  font-family: inherit;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
  transition: background var(--zy-duration-2) var(--zy-ease);
}

.directory-tree-panel__root-button:hover {
  background: var(--zy-blue-50);
}

.directory-tree-panel__root-button.is-active {
  background: var(--zy-blue-100);
  color: var(--zy-blue-700);
}

.directory-tree-panel__root-title {
  white-space: nowrap;
}

.directory-tree-panel__root-helper {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  padding-left: 10px;
}

.directory-tree-panel__list {
  margin: 0;
  padding: 0;
}
</style>
