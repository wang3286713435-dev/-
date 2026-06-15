<template>
  <section class="standard-status" :class="{ 'is-ready': ready }">
    <div class="standard-status__summary">
      <span>工程主数据状态</span>
      <strong>{{ ready ? '可进入交付' : '仍需确认' }}</strong>
      <p>{{ guidanceText }}</p>
    </div>
    <div class="standard-status__cards">
      <article :class="{ 'is-ready': status?.hasSectionTree }">
        <span>部位树</span>
        <strong>{{ status?.sectionNodeCount ?? 0 }}</strong>
        <small>{{ status?.hasSectionTree ? '已建立' : '未建立' }}</small>
      </article>
      <article :class="{ 'is-ready': status?.nodeTypesLocked }">
        <span>节点类型</span>
        <strong>{{ status?.nodeTypeCount ?? 0 }}</strong>
        <small>{{ status?.nodeTypesLocked ? '已锁定' : '未锁定' }}</small>
      </article>
      <article :class="{ 'is-ready': status?.deliverableStandardReady }">
        <span>交付定义</span>
        <strong>{{ status?.deliverableDefinitionCount ?? 0 }}</strong>
        <small>{{ status?.deliverableStandardReady ? '已就绪' : '待配置' }}</small>
      </article>
      <article :class="{ 'is-ready': status?.hasDirectoryTemplates }">
        <span>目录模板</span>
        <strong>{{ status?.directoryTemplateCount ?? 0 }}</strong>
        <small>{{ status?.hasDirectoryTemplates ? '已配置' : '待配置' }}</small>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

import type { StandardStatus } from '@/modules/master-data/api/masterData';

const props = defineProps<{
  status: StandardStatus | null;
}>();

const ready = computed(() => Boolean(
  props.status?.hasSectionTree
    && props.status?.nodeTypesLocked
    && props.status?.deliverableStandardReady
));

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
