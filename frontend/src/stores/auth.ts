import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import { readSession, writeSession, type SessionTokens } from '@/app/session';
import { fetchCurrentUser, login, logout, switchProject } from '@/modules/auth/api/auth';
import type { CurrentUser } from '@/modules/core/api/types';

export const useAuthStore = defineStore('auth', () => {
  const session = ref<SessionTokens | null>(readSession());
  const currentUser = ref<CurrentUser | null>(null);
  const bootstrapped = ref(false);
  const hermesContextVersion = ref(0);

  const isAuthenticated = computed(() => Boolean(session.value?.accessToken));
  const currentProjectId = computed(() => currentUser.value?.currentProject.id ?? session.value?.currentProjectId ?? null);

  function saveSession(nextSession: SessionTokens | null) {
    session.value = nextSession;
    writeSession(nextSession);
  }

  async function signIn(username: string, password: string) {
    const tokenResponse = await login({ username, password });
    saveSession({
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      currentProjectId: tokenResponse.currentProjectId
    });
    await loadCurrentUser();
  }

  async function loadCurrentUser() {
    if (!session.value?.accessToken) {
      currentUser.value = null;
      return null;
    }
    const user = await fetchCurrentUser();
    currentUser.value = user;
    saveSession({
      accessToken: session.value.accessToken,
      refreshToken: session.value.refreshToken,
      currentProjectId: user.currentProject.id
    });
    bootstrapped.value = true;
    return user;
  }

  async function changeProject(projectId: number) {
    const tokenResponse = await switchProject(projectId);
    saveSession({
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      currentProjectId: tokenResponse.currentProjectId
    });
    invalidateHermesContext(projectId);
    return loadCurrentUser();
  }

  async function signOut() {
    try {
      if (session.value?.accessToken) {
        await logout();
      }
    } finally {
      reset();
    }
  }

  function reset() {
    saveSession(null);
    currentUser.value = null;
    bootstrapped.value = true;
  }

  function hydrate() {
    session.value = readSession();
    bootstrapped.value = false;
  }

  function invalidateHermesContext(projectId: number | null) {
    hermesContextVersion.value += 1;
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('hermes-context-invalidated', {
        detail: {
          projectId,
          reason: 'project_switch'
        }
      }));
    }
  }

  return {
    session,
    currentUser,
    bootstrapped,
    hermesContextVersion,
    isAuthenticated,
    currentProjectId,
    hydrate,
    signIn,
    loadCurrentUser,
    changeProject,
    signOut,
    reset
  };
});
