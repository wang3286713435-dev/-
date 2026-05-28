export interface SessionTokens {
  accessToken: string;
  refreshToken: string;
  currentProjectId: number | null;
}

const SESSION_KEY = 'ctower-digital-delivery/session';
const LEGACY_SESSION_KEYS = ['delivery-platform/session'];

export function readSession(): SessionTokens | null {
  LEGACY_SESSION_KEYS.forEach((key) => window.localStorage.removeItem(key));
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
