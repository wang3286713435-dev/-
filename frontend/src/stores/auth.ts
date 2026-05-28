import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import { frontendMockOnly, mockCurrentUser, mockSessionToken } from '@/app/runtime';
import { readSession, writeSession, type SessionTokens } from '@/app/session';
import { fetchCurrentUser, login, logout, switchProject } from '@/modules/auth/api/auth';
import type { CurrentUser } from '@/modules/core/api/types';

function createMockSession(): SessionTokens {
  return {
    accessToken: mockSessionToken.accessToken,
    refreshToken: mockSessionToken.refreshToken,
    currentProjectId: mockSessionToken.currentProjectId
  };
}

function createMockUser(projectId = mockCurrentUser.currentProject?.id ?? null): CurrentUser {
  const user = JSON.parse(JSON.stringify(mockCurrentUser)) as CurrentUser;
  const selectedProject = user.projects.find((project) => project.id === projectId) ?? user.projects[0] ?? null;
  user.currentProject = selectedProject;
  return user;
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<SessionTokens | null>(frontendMockOnly ? createMockSession() : readSession());
  const currentUser = ref<CurrentUser | null>(frontendMockOnly ? createMockUser() : null);
  const bootstrapped = ref(false);

  const isAuthenticated = computed(() => frontendMockOnly || Boolean(session.value?.accessToken));
  const currentProjectId = computed(() => currentUser.value?.currentProject?.id ?? session.value?.currentProjectId ?? null);

  function saveSession(nextSession: SessionTokens | null) {
    session.value = nextSession;
    writeSession(nextSession);
  }

  function applyMockSession(projectId = mockSessionToken.currentProjectId) {
    const nextSession = createMockSession();
    nextSession.currentProjectId = projectId;
    saveSession(nextSession);
    currentUser.value = createMockUser(projectId);
    bootstrapped.value = true;
    return currentUser.value;
  }

  async function signIn(username: string, password: string) {
    if (frontendMockOnly) {
      applyMockSession();
      return;
    }
    const tokenResponse = await login({ username, password });
    saveSession({
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      currentProjectId: tokenResponse.currentProjectId
    });
    await loadCurrentUser();
  }

  async function loadCurrentUser() {
    if (frontendMockOnly) {
      return applyMockSession(session.value?.currentProjectId ?? mockSessionToken.currentProjectId);
    }
    if (!session.value?.accessToken) {
      currentUser.value = null;
      return null;
    }
    const user = await fetchCurrentUser();
    currentUser.value = user;
    saveSession({
      accessToken: session.value.accessToken,
      refreshToken: session.value.refreshToken,
      currentProjectId: user.currentProject?.id ?? null
    });
    bootstrapped.value = true;
    return user;
  }

  async function changeProject(projectId: number) {
    if (frontendMockOnly) {
      return applyMockSession(projectId);
    }
    const tokenResponse = await switchProject(projectId);
    saveSession({
      accessToken: tokenResponse.accessToken,
      refreshToken: tokenResponse.refreshToken,
      currentProjectId: tokenResponse.currentProjectId
    });
    return loadCurrentUser();
  }

  async function signOut() {
    if (frontendMockOnly) {
      reset();
      return;
    }
    try {
      if (session.value?.accessToken) {
        await logout();
      }
    } finally {
      reset();
    }
  }

  function reset() {
    if (frontendMockOnly) {
      applyMockSession();
      return;
    }
    saveSession(null);
    currentUser.value = null;
    bootstrapped.value = true;
  }

  function hydrate() {
    if (frontendMockOnly) {
      applyMockSession(session.value?.currentProjectId ?? mockSessionToken.currentProjectId);
      return;
    }
    session.value = readSession();
    bootstrapped.value = false;
  }

  return {
    session,
    currentUser,
    bootstrapped,
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
