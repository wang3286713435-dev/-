import { reactive } from 'vue';
import { defineStore } from 'pinia';

import type { HermesChatResponse } from '@/modules/data-steward/api/dataSteward';

export type HermesConversationEntry =
  | {
      id: string;
      role: 'user';
      content: string;
      previousResponseRef: string;
      createdAt: string;
    }
  | {
      id: string;
      role: 'assistant';
      response: HermesChatResponse;
      createdAt: string;
    }
  | {
      id: string;
      role: 'system';
      content: string;
      createdAt: string;
    };

export interface HermesProjectConversationState {
  sessionRef: string;
  previousResponseRef: string;
  entries: HermesConversationEntry[];
  resetVersion: number;
}

export const useHermesConversationStore = defineStore('hermesConversation', () => {
  const sessions = reactive<Record<string, HermesProjectConversationState>>({});

  function ensureProjectSession(projectId: number) {
    const key = projectKey(projectId);
    if (!sessions[key]) {
      sessions[key] = createProjectSession(projectId, 0);
    }
    return sessions[key];
  }

  function resetProject(projectId: number, message = '') {
    const key = projectKey(projectId);
    const previousVersion = sessions[key]?.resetVersion ?? 0;
    sessions[key] = createProjectSession(projectId, previousVersion + 1);
    if (message) {
      sessions[key].entries.push(createSystemEntry(message));
    }
    return sessions[key];
  }

  function setPreviousResponseRef(projectId: number, value: string) {
    ensureProjectSession(projectId).previousResponseRef = value;
  }

  function appendEntry(projectId: number, entry: HermesConversationEntry) {
    ensureProjectSession(projectId).entries.push(entry);
  }

  function removeSystemEntries(projectId: number) {
    const session = ensureProjectSession(projectId);
    session.entries = session.entries.filter((entry) => entry.role !== 'system');
  }

  function createUserEntry(content: string, previousResponseRef: string): HermesConversationEntry {
    return {
      id: createConversationId('user'),
      role: 'user',
      content,
      previousResponseRef,
      createdAt: new Date().toISOString()
    };
  }

  function createAssistantEntry(response: HermesChatResponse): HermesConversationEntry {
    return {
      id: createConversationId('assistant'),
      role: 'assistant',
      response,
      createdAt: new Date().toISOString()
    };
  }

  return {
    sessions,
    ensureProjectSession,
    resetProject,
    setPreviousResponseRef,
    appendEntry,
    removeSystemEntries,
    createUserEntry,
    createAssistantEntry
  };
});

function createProjectSession(projectId: number, resetVersion: number): HermesProjectConversationState {
  return {
    sessionRef: createHermesSessionRef(projectId),
    previousResponseRef: '',
    entries: [],
    resetVersion
  };
}

function createSystemEntry(content: string): HermesConversationEntry {
  return {
    id: createConversationId('system'),
    role: 'system',
    content,
    createdAt: new Date().toISOString()
  };
}

function createHermesSessionRef(projectId: number) {
  const random = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `session:platform-ui:project:${safeProjectSegment(projectId)}:${safeRefSegment(random, 'local')}`;
}

function createConversationId(prefix: string) {
  const random = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}:${safeRefSegment(random, 'local')}`;
}

function projectKey(projectId: number) {
  return `project:${safeProjectSegment(projectId)}`;
}

function safeProjectSegment(projectId: number) {
  return Number.isFinite(projectId) ? String(projectId) : 'unknown';
}

function safeRefSegment(value: string, fallback: string) {
  const segment = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '_')
    .replace(/^_+|_+$/g, '');
  return segment ? segment.slice(0, 48) : fallback;
}
