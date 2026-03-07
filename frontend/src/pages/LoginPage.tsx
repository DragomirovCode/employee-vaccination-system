import { FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { AppRole, normalizeAuthToken } from "../features/auth/session";
import { apiGet } from "../shared/api/client";
import { ApiHttpError } from "../shared/api/types";

function extractRoles(payload: unknown): AppRole[] {
  const allowed = new Set<AppRole>(["PERSON", "HR", "MEDICAL", "ADMIN"]);
  const roles = new Set<AppRole>();

  const walk = (value: unknown): void => {
    if (Array.isArray(value)) {
      value.forEach((item) => walk(item));
      return;
    }
    if (!value || typeof value !== "object") return;

    const obj = value as Record<string, unknown>;
    const maybeRole = typeof obj.role === "string" ? obj.role : undefined;
    const maybeRoles = Array.isArray(obj.roles) ? obj.roles : undefined;

    if (maybeRole) {
      const normalized = maybeRole.toUpperCase();
      if (allowed.has(normalized as AppRole)) {
        roles.add(normalized as AppRole);
      }
    }
    if (maybeRoles) {
      maybeRoles.forEach((role) => {
        if (typeof role !== "string") return;
        const normalized = role.toUpperCase();
        if (allowed.has(normalized as AppRole)) {
          roles.add(normalized as AppRole);
        }
      });
    }
  };

  walk(payload);
  return Array.from(roles);
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [token, setToken] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = (location.state as { from?: string } | undefined)?.from ?? "/";
  const reason = (location.state as { reason?: string } | undefined)?.reason;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting) return;

    const normalizedToken = normalizeAuthToken(token);
    if (!normalizedToken) {
      setError("Token is required");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const users = await apiGet<unknown>("/auth/users", {
        authToken: normalizedToken,
        suppressAuthEvents: true
      });
      const roles = extractRoles(users);

      login({
        token: normalizedToken,
        roles
      });
      navigate(redirectTo, { replace: true });
    } catch (e) {
      if (e instanceof ApiHttpError && e.status === 401) {
        setError("Session expired. Please sign in again");
      } else if (e instanceof ApiHttpError && e.status === 403) {
        login({
          token: normalizedToken,
          roles: []
        });
        navigate(redirectTo, { replace: true });
        return;
      } else if (e instanceof ApiHttpError) {
        setError(e.payload?.message ?? e.message);
      } else if (e instanceof TypeError) {
        setError("Network/CORS error. Check backend URL and CORS for X-Auth-Token header.");
      } else if (e instanceof Error) {
        setError(e.message);
      } else {
        setError("Unable to sign in. Try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="center">
      <form className="card auth-card" onSubmit={onSubmit}>
        <h2>Sign in</h2>
        <p className="caption">Enter user UUID or Bearer UUID token.</p>
        {reason === "expired" ? <p className="warn">Session expired. Please sign in again.</p> : null}
        {error ? <p className="warn">{error}</p> : null}
        <label>
          User token
          <input
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="550e8400-e29b-41d4-a716-446655440000"
            autoFocus
          />
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? "Signing in..." : "Sign in"}
        </button>
      </form>
    </section>
  );
}
