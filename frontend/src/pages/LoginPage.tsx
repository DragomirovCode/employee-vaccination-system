import { FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { AppRole, normalizeAuthToken } from "../features/auth/session";
import { apiGet } from "../shared/api/client";
import { useI18n } from "../shared/i18n/I18nContext";
import { LanguageSwitch } from "../shared/i18n/LanguageSwitch";
import { ApiHttpError } from "../shared/api/types";

type AuthMeResponse = {
  userId: string;
  roles: AppRole[];
};

function canAccessPath(pathname: string, roles: AppRole[]): boolean {
  if (pathname === "/" || pathname === "/notifications") return true;
  if (pathname === "/reports/coverage") return roles.some((role) => role === "HR" || role === "MEDICAL" || role === "ADMIN");
  if (pathname === "/employees") return roles.some((role) => role === "HR" || role === "MEDICAL" || role === "ADMIN");
  if (pathname === "/employees/new") return roles.some((role) => role === "HR" || role === "ADMIN");
  if (/^\/employees\/[^/]+\/edit$/.test(pathname)) return roles.some((role) => role === "HR" || role === "ADMIN");
  if (/^\/employees\/[^/]+\/vaccinations$/.test(pathname)) return true;
  if (pathname === "/departments") return roles.some((role) => role === "HR" || role === "ADMIN");
  if (pathname === "/departments/new") return roles.some((role) => role === "HR" || role === "ADMIN");
  if (/^\/departments\/[^/]+\/edit$/.test(pathname)) return roles.some((role) => role === "HR" || role === "ADMIN");
  if (pathname === "/vaccines" || pathname === "/diseases") {
    return roles.some((role) => role === "MEDICAL" || role === "ADMIN");
  }
  if (pathname === "/vaccines/new") return roles.some((role) => role === "MEDICAL" || role === "ADMIN");
  if (/^\/vaccines\/[^/]+\/edit$/.test(pathname)) return roles.some((role) => role === "MEDICAL" || role === "ADMIN");
  if (pathname === "/vaccinations") return roles.some((role) => role === "MEDICAL" || role === "ADMIN");
  if (pathname === "/admin-sandbox") return roles.includes("ADMIN");
  return false;
}

function resolveRedirectPath(pathname: string, roles: AppRole[]): string {
  return canAccessPath(pathname, roles) ? pathname : "/";
}

function isUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const { t } = useI18n();
  const [token, setToken] = useState("");
  const [errorKey, setErrorKey] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = (location.state as { from?: string } | undefined)?.from ?? "/";
  const reason = (location.state as { reason?: string } | undefined)?.reason;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting) return;

    const normalizedToken = normalizeAuthToken(token);
    if (!normalizedToken) {
      setErrorKey("login.tokenRequired");
      setErrorMessage(null);
      return;
    }
    if (!isUuid(normalizedToken)) {
      setErrorKey("login.tokenNotFound");
      setErrorMessage(null);
      return;
    }

    setSubmitting(true);
    setErrorKey(null);
    setErrorMessage(null);

    try {
      const me = await apiGet<AuthMeResponse>("/auth/me", {
        authToken: normalizedToken,
        suppressAuthEvents: true
      });
      login({
        token: normalizedToken,
        roles: me.roles,
        userId: me.userId
      });
      navigate(resolveRedirectPath(redirectTo, me.roles), { replace: true });
    } catch (e) {
      if (e instanceof ApiHttpError && e.status === 401) {
        setErrorKey("login.tokenNotFound");
        setErrorMessage(null);
      } else if (e instanceof ApiHttpError && e.status === 403) {
        setErrorKey(null);
        setErrorMessage(e.payload?.message ?? e.message);
      } else if (e instanceof ApiHttpError) {
        setErrorKey(null);
        setErrorMessage(e.payload?.message ?? e.message);
      } else if (e instanceof TypeError) {
        setErrorKey("common.networkCorsError");
        setErrorMessage(null);
      } else if (e instanceof Error) {
        setErrorKey(null);
        setErrorMessage(e.message);
      } else {
        setErrorKey("login.unableToSignIn");
        setErrorMessage(null);
      }
    } finally {
      setSubmitting(false);
    }
  }

  const error = errorMessage ?? (errorKey ? t(errorKey) : null);

  return (
    <section className="center">
      <form className="card auth-card" onSubmit={onSubmit}>
        <div className="auth-lang auth-lang-inside">
          <LanguageSwitch />
        </div>
        <h2>{t("login.title")}</h2>
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
