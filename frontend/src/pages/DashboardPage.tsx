import { useEffect, useState } from "react";
import { useAuth } from "../features/auth/AuthContext";
import { apiGet } from "../shared/api/client";
import { useI18n } from "../shared/i18n/I18nContext";
import { ApiHttpError, NotificationPage } from "../shared/api/types";

export function DashboardPage() {
  const { session } = useAuth();
  const { t } = useI18n();
  const [data, setData] = useState<NotificationPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!session) return;
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiGet<NotificationPage>("/notifications?page=0&size=10");
        if (!cancelled) setData(response);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("dashboard.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [session]);

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("dashboard.notifications")}</h2>
            <p className="muted">{t("dashboard.notificationsHint")}</p>
          </div>
        </div>
        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}
        {!loading && !error && data && data.content.length === 0 ? <p>{t("dashboard.noNotifications")}</p> : null}
        {!loading && !error && data && data.content.length > 0 ? (
          <ul className="list">
            {data.content.map((item) => (
              <li key={item.id}>
                <strong>{item.title}</strong>
                <p>{item.message}</p>
              </li>
            ))}
          </ul>
        ) : null}
      </article>
    </section>
  );
}
