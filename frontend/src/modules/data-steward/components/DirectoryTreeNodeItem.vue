<template>
  <li class="directory-tree-node">
    <div class="directory-tree-node__line" :style="{ paddingLeft: `${depth * 18}px` }">
      <button
        v-if="hasChildren"
        class="directory-tree-node__toggle"
        type="button"
        :aria-label="expanded ? '折叠目录' : '展开目录'"
        @click.stop="toggleExpanded"
      >
        <el-icon class="directory-tree-node__toggle-icon" :class="{ 'is-expanded': expanded }">
          <CaretRight />
        </el-icon>
      </button>
      <span v-else class="directory-tree-node__toggle-spacer" />

      <button
        class="directory-tree-node__button"
        :class="{ 'is-active': activePath === node.fullPath }"
        type="button"
        :title="node.relativePath"
        @click="$emit('select', node.fullPath)"
        @dblclick.stop="enterDirectory"
      >
        <el-icon class="directory-tree-node__folder">
          <component :is="expanded && hasChildren ? FolderOpened : Folder" />
        </el-icon>
        <span class="directory-tree-node__name">{{ node.name }}</span>
        <el-tag size="small" type="info">{{ node.fileCount }}</el-tag>
      </button>
    </div>

    <ul v-if="hasChildren && expanded" class="directory-tree-node__children">
      <DirectoryTreeNodeItem
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :depth="depth + 1"
        :active-path="activePath"
        :expanded-paths="expandedPaths"
        @select="$emit('select', $event)"
        @enter="$emit('enter', $event)"
        @toggle-expand="(path, isExpanded) => $emit('toggle-expand', path, isExpanded)"
      />
    </ul>
  </li>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { CaretRight, Folder, FolderOpened } from '@element-plus/icons-vue';

import type { DirectoryTreeNode } from '@/modules/data-steward/utils/directoryTree';

defineOptions({
  name: 'DirectoryTreeNodeItem'
});

const props = withDefaults(defineProps<{
  node: DirectoryTreeNode;
  depth?: number;
  activePath: string;
  expandedPaths?: string[];
}>(), {
  depth: 0,
  expandedPaths: () => []
});

const emit = defineEmits<{
  select: [path: string];
  enter: [path: string];
  'toggle-expand': [path: string, expanded: boolean];
}>();

const hasChildren = computed(() => props.node.children.length > 0);
const expanded = computed(() =>
  props.depth === 0
  || props.expandedPaths.includes(props.node.fullPath)
  || isActiveAncestor(props.node.fullPath, props.activePath)
);

function enterDirectory() {
  if (hasChildren.value) {
    emit('toggle-expand', props.node.fullPath, true);
  }
  emit('enter', props.node.fullPath);
}

function toggleExpanded() {
  emit('toggle-expand', props.node.fullPath, !expanded.value);
}

function isActiveAncestor(nodePath: string, activePath: string) {
  if (!nodePath || !activePath) return false;
  const normalizedNode = nodePath.replace(/\/+$/, '');
  const normalizedActive = activePath.replace(/\/+$/, '');
  return normalizedActive === normalizedNode || normalizedActive.startsWith(`${normalizedNode}/`);
}
</script>

<style scoped>
.directory-tree-node {
  list-style: none;
}

.directory-tree-node__children {
  margin: 0;
  padding: 0;
}

.directory-tree-node__line {
  display: flex;
  align-items: stretch;
  min-width: max-content;
}

.directory-tree-node__toggle,
.directory-tree-node__toggle-spacer {
  width: 22px;
  flex: 0 0 22px;
}

.directory-tree-node__toggle {
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.directory-tree-node__toggle-icon {
  transition: transform 0.15s ease;
}

.directory-tree-node__toggle-icon.is-expanded {
  transform: rotate(90deg);
}

.directory-tree-node__button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: max-content;
  border: 0;
  background: transparent;
  color: #475569;
  cursor: pointer;
  padding: 6px 10px 6px 0;
  border-radius: 6px;
}

.directory-tree-node__button:hover {
  background: rgba(59, 130, 246, 0.08);
}

.directory-tree-node__button.is-active {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
  font-weight: 600;
}

.directory-tree-node__folder {
  color: inherit;
}

.directory-tree-node__name {
  white-space: nowrap;
}
</style>
