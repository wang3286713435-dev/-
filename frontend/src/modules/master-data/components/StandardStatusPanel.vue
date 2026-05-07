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
  </el-alert>
</template>

<script setup lang="ts">
import type { StandardStatus } from '@/modules/master-data/api/masterData';

defineProps<{
  status: StandardStatus | null;
}>();
</script>
