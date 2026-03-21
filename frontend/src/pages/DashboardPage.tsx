import { useEffect, useMemo, useState } from "react";
import { apiGet, apiPatch } from "../shared/api/client";
import { useI18n } from "../shared/i18n/I18nContext";
import { ApiHttpError, NotificationBulkReadResponse, NotificationPage } from "../shared/api/types";
import { getDateSearchValues, matchesSearchQuery } from "../shared/search";

const PAGE_SIZE = 10;

export function DashboardPage() {
  const { locale, t } = useI18n();
  const [data, setData] = useState<NotificationPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [onlyUnread, setOnlyUnread] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);
  const [markingAll, setMarkingAll] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiGet<NotificationPage>(
          `/notifications?onlyUnread=${onlyUnread}&page=${page}&size=${PAGE_SIZE}`
        );
        if (!cancelled) setData(response);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("notifications.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [onlyUnread, page, t]);

  async function markAsRead(notificationId: string) {
    setBusyId(notificationId);
    setError(null);
    try {
      await apiPatch(`/notifications/${notificationId}/read`);
      setData((current) =>
        current
          ? {
              ...current,
              content: current.content.map((item) =>
                item.id === notificationId ? { ...item, isRead: true, readAt: new Date().toISOString() } : item
              )
            }
          : current
      );
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("notifications.unexpectedApiError");
      setError(message);
    } finally {
      setBusyId(null);
    }
  }

  async function markAllAsRead() {
    setMarkingAll(true);
    setError(null);
    try {
      await apiPatch<NotificationBulkReadResponse>("/notifications/read-all");
      setData((current) =>
        current
          ? {
              ...current,
              content: current.content.map((item) => ({
                ...item,
                isRead: true,
                readAt: item.readAt ?? new Date().toISOString()
              }))
            }
          : current
      );
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("notifications.unexpectedApiError");
      setError(message);
    } finally {
      setMarkingAll(false);
    }
  }

  const formatter = new Intl.DateTimeFormat(locale === "ru" ? "ru-RU" : "en-US", {
    dateStyle: "medium",
    timeStyle: "short"
  });
  const totalPages = data?.totalPages ?? 0;
  const canGoPrevious = page > 0 && !loading;
  const canGoNext = Boolean(data && page + 1 < data.totalPages) && !loading;
  const unreadCount = data?.content.filter((item) => !item.isRead).length ?? 0;
  const filteredItems = useMemo(
    () =>
      (data?.content ?? []).filter((item) =>
        matchesSearchQuery(
          searchQuery,
          item.title,
          item.message,
          ...getDateSearchValues(item.createdAt, locale === "ru" ? "ru-RU" : "en-US", { dateStyle: "medium", timeStyle: "short" })
        )
      ),
    [data?.content, locale, searchQuery]
  );

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("notifications.title")}</h2>
            <p className="muted">{t("notifications.description")}</p>
          </div>
          <span className="summary-pill">
            {t("notifications.total")}: {data?.totalElements ?? 0}
          </span>
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
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={onlyUnread}
              onChange={(e) => {
                setOnlyUnread(e.target.checked);
                setPage(0);
              }}
            />
            <span>{t("notifications.onlyUnread")}</span>
          </label>
          {unreadCount > 1 ? (
            <div className="toolbar-actions">
              <button type="button" className="button-secondary" onClick={markAllAsRead} disabled={markingAll || loading}>
                {markingAll ? t("notifications.markingAll") : t("notifications.markAllAsRead")}
              </button>
            </div>
          ) : null}
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && data && filteredItems.length === 0 ? (
          <div className="empty-state">
            <h3>{t("notifications.emptyTitle")}</h3>
            <p>{t("notifications.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && data && filteredItems.length > 0 ? (
          <>
            <div className="notification-list">
              {filteredItems.map((item) => (
                <article key={item.id} className={`notification-item ${item.isRead ? "is-read" : "is-unread"}`}>
                  <div className="notification-head">
                    <div>
                      <h3>{item.title}</h3>
                      <p className="muted">{formatter.format(new Date(item.createdAt))}</p>
                    </div>
                    <span className={`status-pill ${item.isRead ? "is-planned" : "is-soon"}`}>
                      {item.isRead ? t("notifications.read") : t("notifications.unread")}
                    </span>
                  </div>
                  <p>{item.message}</p>
                  {!item.isRead ? (
                    <div className="notification-actions">
                      <button type="button" className="button-secondary" onClick={() => markAsRead(item.id)} disabled={busyId === item.id || markingAll}>
                        {busyId === item.id ? t("notifications.markingOne") : t("notifications.markAsRead")}
                      </button>
                    </div>
                  ) : null}
                </article>
              ))}
            </div>

            {totalPages > 1 ? (
              <div className="pagination">
                <button type="button" className="button-secondary" onClick={() => setPage((value) => value - 1)} disabled={!canGoPrevious}>
                  {t("pagination.previous")}
                </button>
                <p>
                  {t("pagination.page")} {page + 1} {t("pagination.of")} {totalPages}
                </p>
                <button type="button" className="button-secondary" onClick={() => setPage((value) => value + 1)} disabled={!canGoNext}>
                  {t("pagination.next")}
                </button>
              </div>
            ) : null}
          </>
        ) : null}
      </article>
    </section>
  );
}
