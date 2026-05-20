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
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
  background: #fff;
  min-height: 520px;
  height: 100%;
  overflow: hidden;
}

.directory-tree-panel__header {
  display: grid;
  gap: 4px;
  padding: 14px 16px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.directory-tree-panel__header strong {
  font-size: 14px;
  color: #0f172a;
}

.directory-tree-panel__header span {
  font-size: 12px;
  color: #64748b;
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
  padding: 12px 12px 16px;
}

.directory-tree-panel__root {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}

.directory-tree-panel__root-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  min-width: max-content;
  padding: 8px 10px;
  border: 0;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.04);
  color: #0f172a;
  cursor: pointer;
  font-weight: 600;
}

.directory-tree-panel__root-button:hover {
  background: rgba(59, 130, 246, 0.08);
}

.directory-tree-panel__root-button.is-active {
  background: rgba(59, 130, 246, 0.14);
  color: #1d4ed8;
}

.directory-tree-panel__root-title {
  white-space: nowrap;
}

.directory-tree-panel__root-helper {
  color: #64748b;
  font-size: 12px;
  padding-left: 10px;
}

.directory-tree-panel__list {
  margin: 0;
  padding: 0;
}
</style>
