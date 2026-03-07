import { FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { AppRole } from "../features/auth/session";

function parseRoles(raw: string): AppRole[] {
  if (!raw.trim()) return [];
  const allowed = new Set<AppRole>(["PERSON", "HR", "MEDICAL", "ADMIN"]);
  return raw
    .split(",")
    .map((it) => it.trim().toUpperCase())
    .filter((it): it is AppRole => allowed.has(it as AppRole));
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const [token, setToken] = useState("");
  const [roles, setRoles] = useState("");
  const [error, setError] = useState<string | null>(null);

  const redirectTo = (location.state as { from?: string } | undefined)?.from ?? "/";
  const reason = (location.state as { reason?: string } | undefined)?.reason;

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!token.trim()) {
      setError("Token is required");
      return;
    }

    login({
      token: token.trim(),
      roles: parseRoles(roles)
    });
    navigate(redirectTo, { replace: true });
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
        <label>
          Roles (optional, comma-separated)
          <input value={roles} onChange={(e) => setRoles(e.target.value)} placeholder="PERSON,HR" />
        </label>
        <button type="submit">Sign in</button>
      </form>
    </section>
  );
}
