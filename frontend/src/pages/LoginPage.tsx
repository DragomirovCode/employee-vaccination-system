import { FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { AppRole, normalizeAuthToken } from "../features/auth/session";
import { apiGet } from "../shared/api/client";
import { useI18n } from "../shared/i18n/I18nContext";
import { LanguageSwitch } from "../shared/i18n/LanguageSwitch";
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
  const { t } = useI18n();
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
      setError(t("login.tokenRequired"));
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
        setError(t("login.sessionExpired"));
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
        setError(t("common.networkCorsError"));
      } else if (e instanceof Error) {
        setError(e.message);
      } else {
        setError(t("login.unableToSignIn"));
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="center">
      <form className="card auth-card" onSubmit={onSubmit}>
        <div className="auth-lang auth-lang-inside">
          <LanguageSwitch />
        </div>
        <h2>{t("login.title")}</h2>
        <p className="caption">{t("login.hint")}</p>
        {reason === "expired" ? <p className="warn">{t("login.sessionExpired")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}
        <label>
          {t("login.tokenLabel")}
          <input
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder={t("login.tokenPlaceholder")}
            autoFocus
          />
        </label>
        <button type="submit" disabled={submitting}>
          {submitting ? t("login.submitting") : t("login.submit")}
        </button>
      </form>
    </section>
  );
}
