export type AppRole = "PERSON" | "HR" | "MEDICAL" | "ADMIN";

export type AuthSession = {
  roles: AppRole[];
  userId: string;
};

export function normalizeRoles(value: unknown): AppRole[] {
  if (!Array.isArray(value)) return [];
  const allowed = new Set<AppRole>(["PERSON", "HR", "MEDICAL", "ADMIN"]);
  return value.filter((role): role is AppRole => typeof role === "string" && allowed.has(role as AppRole));
}
