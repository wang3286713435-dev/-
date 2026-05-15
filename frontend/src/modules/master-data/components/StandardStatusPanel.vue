<template>
  <el-alert
    class="standard-status"
    :type="status?.hasSectionTree && status?.nodeTypesLocked ? 'success' : 'warning'"
    :closable="false"
    show-icon
  >
    <template #title>
      <div class="standard-status__title">
        <span>标准前置条件</span>
        <el-tag size="small" :type="status?.hasSectionTree ? 'success' : 'warning'">
          部位树 {{ status?.hasSectionTree ? '已建立' : '未建立' }}
        </el-tag>
        <el-tag size="small" :type="status?.nodeTypesLocked ? 'success' : 'info'">
          节点类型 {{ status?.nodeTypesLocked ? '已锁定' : '未锁定' }}
        </el-tag>
        <el-tag size="small" :type="status?.deliverableStandardReady ? 'success' : 'info'">
          交付物标准 {{ status?.deliverableStandardReady ? '已就绪' : '未就绪' }}
        </el-tag>
      </div>
    </template>
    <div class="standard-status__meta">
      <span>部位节点 {{ status?.sectionNodeCount ?? 0 }} 个</span>
      <span>节点类型 {{ status?.nodeTypeCount ?? 0 }} 个</span>
      <span>交付物定义 {{ status?.deliverableDefinitionCount ?? 0 }} 个</span>
      <span>交付物类型 {{ status?.deliverableTypeCount ?? 0 }} 个</span>
      <span>属性 {{ status?.deliverableAttributeCount ?? 0 }} 个</span>
      <span>目录模板 {{ status?.directoryTemplateCount ?? 0 }} 个</span>
    </div>
    <p class="standard-status__hint">{{ guidanceText }}</p>
  </el-alert>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { StandardStatus } from '@/modules/master-data/api/masterData';

const props = defineProps<{
  status: StandardStatus | null;
}>();

const guidanceText = computed(() => {
  const status = props.status;
  if (!status?.hasSectionTree) {
    return '先建立工程部位树。后续交付缺失项会按这些部位生成，例如楼栋、楼层、机房或系统。';
  }
  if (!status.nodeTypesLocked) {
    return '下一步锁定节点类型。节点类型锁定后，平台才会把当前部位结构作为交付标准配置的稳定底座。';
  }
  if (!status.deliverableStandardReady) {
    return '继续配置交付物标准。先定义要交什么，再配置交哪类文件，最后补属性和目录模板。';
  }
  return '标准底座已就绪，可以到文档交付或图纸交付中查看缺失项，并选择文件完成补交。';
});
</script>
