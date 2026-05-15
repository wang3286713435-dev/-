<template>
  <el-menu :default-active="route.path" router class="sidebar-menu">
    <template v-for="item in menus" :key="item.key">
      <el-sub-menu v-if="item.children?.length" :index="item.path">
        <template #title>
          <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
          <span>{{ item.label }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.key" :index="child.path">
          <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
          <span>{{ child.label }}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="item.path">
        <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import {
  Box,
  Connection,
  DataBoard,
  Document,
  Files,
  FolderOpened,
  House,
  Monitor,
  OfficeBuilding,
  Picture,
  Search,
  Warning,
  Tickets
} from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';

import type { MenuItem } from '@/modules/core/api/types';

defineProps<{
  menus: MenuItem[];
}>();

const route = useRoute();

const icons = {
  Box,
  Connection,
  DataBoard,
  Document,
  Files,
  FolderOpened,
  House,
  Monitor,
  OfficeBuilding,
  Picture,
  Search,
  Warning,
  Tickets
};

function resolveIcon(icon: string) {
  return icons[icon as keyof typeof icons] ?? Files;
}
</script>
