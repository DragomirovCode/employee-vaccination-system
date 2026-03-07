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
    return { token: parsed.token, roles: normalizeRoles(parsed.roles) };
  } catch {
    return null;
  }
}

export function writeSession(session: AuthSession): void {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  localStorage.removeItem(SESSION_KEY);
}

function normalizeRoles(value: unknown): AppRole[] {
  if (!Array.isArray(value)) return [];
  const allowed = new Set<AppRole>(["PERSON", "HR", "MEDICAL", "ADMIN"]);
  return value.filter((role): role is AppRole => typeof role === "string" && allowed.has(role as AppRole));
}
