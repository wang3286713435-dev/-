<template>
  <nav class="master-step-nav" aria-label="工程主数据步骤导航">
    <button
      v-for="step in steps"
      :key="step.key"
      type="button"
      class="master-step-nav__item"
      :class="{ 'is-active': step.key === active }"
      @click="goStep(step.routeName)"
    >
      <span>{{ step.index }}</span>
      <strong>{{ step.title }}</strong>
      <small>{{ step.description }}</small>
    </button>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useProjectWorkspaceContext } from '@/modules/core/composables/useProjectWorkspaceContext';

type StepKey = 'initialization' | 'sections' | 'nodeTypes' | 'deliverableStandard';

defineProps<{
  active: StepKey;
}>();

const route = useRoute();
const router = useRouter();
const { workspaceProjectId } = useProjectWorkspaceContext();

const projectId = computed(() => Number(route.params.projectId) || workspaceProjectId.value || 0);

const steps: Array<{
  key: StepKey;
  index: string;
  title: string;
  description: string;
  routeName: string;
}> = [
  {
    key: 'initialization',
    index: '01',
    title: '接入向导',
    description: '从资产线索生成草案',
    routeName: 'project-master-data-initialization'
  },
  {
    key: 'sections',
    index: '02',
    title: '部位树',
    description: '确认项目结构',
    routeName: 'project-master-data-sections'
  },
  {
    key: 'nodeTypes',
    index: '03',
    title: '节点类型',
    description: '锁定交付层级',
    routeName: 'project-master-data-node-types'
  },
  {
    key: 'deliverableStandard',
    index: '04',
    title: '交付物标准',
    description: '定义应交资料',
    routeName: 'project-master-data-deliverable-standard'
  }
];

function goStep(routeName: string) {
  if (!projectId.value) return;
  router.push({ name: routeName, params: { projectId: projectId.value } });
}
</script>
