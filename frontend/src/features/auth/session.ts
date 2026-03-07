export type AppRole = "PERSON" | "HR" | "MEDICAL" | "ADMIN";

export type AuthSession = {
  token: string;
  roles: AppRole[];
};

const SESSION_KEY = "evs.frontend.session";

export function readSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as Partial<AuthSession>;
    if (!parsed.token || typeof parsed.token !== "string") return null;
    const token = normalizeAuthToken(parsed.token);
    if (!token) return null;
    return { token, roles: normalizeRoles(parsed.roles) };
  } catch {
    return null;
  }
}

export function writeSession(session: AuthSession): void {
  const token = normalizeAuthToken(session.token);
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      ...session,
      token
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
