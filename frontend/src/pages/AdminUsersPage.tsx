import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiGet, apiPatch } from "../shared/api/client";
import { ApiHttpError, AuthUserDto, AuthUserStatusRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { getDateSearchValues, matchesSearchQuery } from "../shared/search";

function formatDateTime(value: string, locale: string): string {
  return new Date(value).toLocaleString(locale === "ru" ? "ru-RU" : "en-US");
}

export function AdminUsersPage() {
  const { locale, t } = useI18n();
  const [users, setUsers] = useState<AuthUserDto[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusBusyId, setStatusBusyId] = useState<string | null>(null);
  const [copiedUserId, setCopiedUserId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiGet<AuthUserDto[]>("/auth/users");
        if (!cancelled) {
          setUsers(response);
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("adminUsers.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [t]);

  const sortedUsers = useMemo(
    () =>
      [...users]
        .filter((user) =>
          matchesSearchQuery(
            searchQuery,
            user.email,
            user.id,
            ...getDateSearchValues(user.createdAt, locale === "ru" ? "ru-RU" : "en-US", { dateStyle: "short", timeStyle: "medium" }),
            ...getDateSearchValues(user.updatedAt, locale === "ru" ? "ru-RU" : "en-US", { dateStyle: "short", timeStyle: "medium" })
          )
        )
        .sort((left, right) => left.email.localeCompare(right.email)),
    [locale, searchQuery, users]
  );

  async function toggleStatus(user: AuthUserDto) {
    setStatusBusyId(user.id);
    setError(null);
    try {
      const payload: AuthUserStatusRequest = { active: !user.isActive };
      const updated = await apiPatch<AuthUserDto>(`/auth/users/${user.id}/status`, payload);
      setUsers((current) => current.map((item) => (item.id === user.id ? updated : item)));
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("adminUsers.statusError");
      setError(message);
    } finally {
      setStatusBusyId(null);
    }
  }

  async function copyToken(userId: string) {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(userId);
      } else {
        const tempInput = document.createElement("input");
        tempInput.value = userId;
        document.body.appendChild(tempInput);
        tempInput.select();
        document.execCommand("copy");
        document.body.removeChild(tempInput);
      }
      setCopiedUserId(userId);
      window.setTimeout(() => setCopiedUserId((current) => (current === userId ? null : current)), 2000);
    } catch {
      setError(t("adminUsers.copyTokenError"));
    }
  }

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("adminUsers.title")}</h2>
            <p className="muted">{t("adminUsers.description")}</p>
          </div>
          <div className="toolbar-actions">
            <span className="summary-pill">
              {t("adminUsers.total")}: {sortedUsers.length}
            </span>
            <Link to="/admin/users/new">
              <button type="button" className="button-secondary">{t("adminUsers.create")}</button>
            </Link>
          </div>
        </div>

        <div className="toolbar">
          <label className="toolbar-field">
            <span>{t("common.search")}</span>
            <input
              type="search"
              value={searchQuery}
              placeholder={t("common.searchPlaceholder")}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </label>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && sortedUsers.length === 0 ? (
          <div className="empty-state">
            <h3>{t("adminUsers.emptyTitle")}</h3>
            <p>{t("adminUsers.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && sortedUsers.length > 0 ? (
          <div className="employee-list">
            {sortedUsers.map((user) => (
              <article key={user.id} className="employee-item">
                <div className="employee-head">
                  <div>
                    <h3>{user.email}</h3>
                    <p className="muted">
                      {t("adminUsers.token")}: <code>{user.id}</code>
                    </p>
                    <p className="muted">
                      {t("adminUsers.createdAt")}: {formatDateTime(user.createdAt, locale)}
                    </p>
                    <p className="muted">
                      {t("adminUsers.updatedAt")}: {formatDateTime(user.updatedAt, locale)}
                    </p>
                  </div>
                  <span className={`status-pill ${user.isActive ? "is-planned" : "is-overdue"}`}>
                    {user.isActive ? t("adminUsers.active") : t("adminUsers.inactive")}
                  </span>
                </div>

                <div className="history-actions">
                  <Link to={`/admin/users/${user.id}/edit`}>
                    <button type="button" className="button-secondary">{t("adminUsers.edit")}</button>
                  </Link>
                  <button type="button" className="button-secondary" onClick={() => void copyToken(user.id)}>
                    {copiedUserId === user.id ? t("adminUsers.copied") : t("adminUsers.copyToken")}
                  </button>
                  <button type="button" onClick={() => void toggleStatus(user)} disabled={statusBusyId === user.id}>
                    {statusBusyId === user.id
                      ? t("adminUsers.statusUpdating")
                      : user.isActive
                        ? t("adminUsers.deactivate")
                        : t("adminUsers.activate")}
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}
