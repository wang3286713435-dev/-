export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  currentProjectId: number | null;
}

const SESSION_KEY = 'delivery-platform/session';

export function readSession(): SessionTokens | null {
  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionTokens;
  } catch {
    window.localStorage.removeItem(SESSION_KEY);
    return null;
  }
}

export function writeSession(session: SessionTokens | null) {
  if (!session) {
    window.localStorage.removeItem(SESSION_KEY);
    return;
  }
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}
