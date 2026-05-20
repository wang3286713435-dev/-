import { computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

export function useProjectWorkspaceContext() {
  const route = useRoute();
  const authStore = useAuthStore();

  const routeProjectId = computed<number | null>(() => {
    const id = Number(route.params.projectId);
    return Number.isFinite(id) ? id : null;
  });

  const assetProjectContext = computed(() => route.meta.assetProjectContext === true);

  const workspaceProjectId = computed<number | null>(() => {
    return routeProjectId.value ?? authStore.currentProjectId;
  });

  watch(
    () => [routeProjectId.value, assetProjectContext.value] as const,
    async ([nextId, isAssetProjectContext]) => {
      if (!isAssetProjectContext && nextId && nextId !== authStore.currentProjectId) {
        await authStore.changeProject(nextId);
      }
    },
    { immediate: true }
  );

  return {
    assetProjectContext,
    routeProjectId,
    workspaceProjectId
  };
}
