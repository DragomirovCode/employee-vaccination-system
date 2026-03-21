export type AppRole = "PERSON" | "HR" | "MEDICAL" | "ADMIN";

export type AuthSession = {
  token: string;
  roles: AppRole[];
  userId: string;
};

const SESSION_KEY = "evs.frontend.session";

export function readSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as Partial<AuthSession>;
    if (!parsed.token || typeof parsed.token !== "string") return null;
    if (!parsed.userId || typeof parsed.userId !== "string") return null;
    const token = normalizeAuthToken(parsed.token);
    const userId = parsed.userId.trim();
    if (!isUuid(token) || !isUuid(userId)) return null;
    return { token, roles: normalizeRoles(parsed.roles), userId };
  } catch {
    return null;
  }
}

export function writeSession(session: AuthSession): void {
  const token = normalizeAuthToken(session.token);
  const userId = session.userId.trim();
  if (!isUuid(token) || !isUuid(userId)) {
    localStorage.removeItem(SESSION_KEY);
    return;
  }
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      ...session,
      token,
      userId
    })
  );
}

export function clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
}

function normalizeRoles(value: unknown): AppRole[] {
  if (!Array.isArray(value)) return [];
  const allowed = new Set<AppRole>(["PERSON", "HR", "MEDICAL", "ADMIN"]);
  return value.filter((role): role is AppRole => typeof role === "string" && allowed.has(role as AppRole));
}

export function normalizeAuthToken(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  return trimmed.replace(/^Bearer\s+/i, "").trim();
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}
