import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiGet, apiGetBlob } from "../shared/api/client";
import { ApiHttpError, DepartmentDto, PageResponse, RevaccinationDueItem } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { getDateSearchValues, matchesSearchQuery } from "../shared/search";

const DEFAULT_DAYS = 30;
const PAGE_SIZE = 10;
type ExportFormat = "csv" | "xlsx" | "pdf";

function getStatus(daysLeft: number): "overdue" | "soon" | "planned" {
  if (daysLeft < 0) return "overdue";
  if (daysLeft <= 7) return "soon";
  return "planned";
}

function buildHistoryLink(employeeId: string, vaccineName: string, lastVaccinationDate: string): string {
  const params = new URLSearchParams({
    vaccineName,
    vaccinationDate: lastVaccinationDate
  });
  return `/employees/${employeeId}/vaccinations?${params.toString()}`;
}

export function RevaccinationDuePage() {
  const { session } = useAuth();
  const { locale, t } = useI18n();
  const [data, setData] = useState<PageResponse<RevaccinationDueItem> | null>(null);
  const [departments, setDepartments] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [appliedDays, setAppliedDays] = useState(DEFAULT_DAYS);
  const [draftDays, setDraftDays] = useState(String(DEFAULT_DAYS));
  const [exportFormat, setExportFormat] = useState<ExportFormat>("csv");
  const [exporting, setExporting] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadDepartments() {
      try {
        const response = await apiGet<DepartmentDto[]>("/departments");
        if (cancelled) return;
        setDepartments(
          Object.fromEntries(response.map((item) => [item.id, item.name]))
        );
      } catch {
        if (!cancelled) {
          setDepartments({});
        }
      }
    }

    loadDepartments();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiGet<PageResponse<RevaccinationDueItem>>(
          `/reports/revaccination-due?days=${appliedDays}&page=${page}&size=${PAGE_SIZE}`
        );
        if (!cancelled) {
          setData(response);
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("revaccination.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [appliedDays, page, t]);

  function onApplyFilters(e: FormEvent) {
    e.preventDefault();
    const nextDays = Number.parseInt(draftDays, 10);
    if (Number.isNaN(nextDays) || nextDays < 0) {
      setError(t("revaccination.invalidDays"));
      return;
    }

    setError(null);
    setPage(0);
    setAppliedDays(nextDays);
  }

  async function exportReport() {
    setError(null);
    setExporting(true);
    try {
      const { blob, contentDisposition } = await apiGetBlob(
        `/reports/revaccination-due/export?days=${appliedDays}&format=${exportFormat}`,
        {
          headers: {
            "Accept-Language": locale
          }
        }
      );
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement("a");
      const fileNameMatch = contentDisposition?.match(/filename="?(.*?)"?$/i);
      link.href = url;
      link.download = fileNameMatch?.[1] || `revaccination-due.${exportFormat}`;
      link.style.display = "none";
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("revaccination.exportError");
      setError(message);
    } finally {
      setExporting(false);
    }
  }

  const formatter = new Intl.DateTimeFormat(locale === "ru" ? "ru-RU" : "en-US");
  const totalPages = data?.totalPages ?? 0;
  const canGoPrevious = page > 0 && !loading;
  const canGoNext = Boolean(data && page + 1 < data.totalPages) && !loading;
  const isPersonView = Boolean(session?.roles.includes("PERSON"));
  const filteredItems = useMemo(
    () =>
      (data?.content ?? []).filter((item) =>
        matchesSearchQuery(
          searchQuery,
          item.fullName,
          item.vaccineName,
          ...getDateSearchValues(item.lastVaccinationDate, locale === "ru" ? "ru-RU" : "en-US"),
          ...getDateSearchValues(item.revaccinationDate, locale === "ru" ? "ru-RU" : "en-US"),
          item.daysLeft,
          departments[item.departmentId]
        )
      ),
    [data?.content, departments, locale, searchQuery]
  );

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{isPersonView ? t("revaccination.personalTitle") : t("revaccination.title")}</h2>
            <p className="muted">{isPersonView ? t("revaccination.personalDescription") : t("revaccination.description")}</p>
          </div>
          <span className="summary-pill">
            {t("revaccination.total")}: {data?.totalElements ?? 0}
          </span>
        </div>

        <form className="toolbar" onSubmit={onApplyFilters}>
          <label className="toolbar-field">
            <span>{t("revaccination.daysFilter")}</span>
            <input
              type="number"
              min="0"
              step="1"
              value={draftDays}
              onChange={(e) => setDraftDays(e.target.value)}
            />
          </label>
          <div className="toolbar-actions">
            <button type="submit" disabled={loading}>
              {t("revaccination.apply")}
            </button>
          </div>
          <label className="toolbar-field">
            <span>{t("revaccination.exportFormat")}</span>
            <select value={exportFormat} onChange={(e) => setExportFormat(e.target.value as ExportFormat)} disabled={exporting}>
              <option value="csv">CSV</option>
              <option value="xlsx">XLSX</option>
              <option value="pdf">PDF</option>
            </select>
          </label>
          <div className="toolbar-actions">
            <button type="button" className="button-secondary" onClick={() => void exportReport()} disabled={exporting}>
              {exporting ? t("revaccination.exportDownloading") : t("revaccination.export")}
            </button>
          </div>
        </form>

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

        {!loading && !error && data && filteredItems.length === 0 ? (
          <div className="empty-state">
            <h3>{t("revaccination.emptyTitle")}</h3>
            <p>{t("revaccination.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && data && filteredItems.length > 0 ? (
          <>
            <div className="due-list">
              {filteredItems.map((item) => {
                const status = getStatus(item.daysLeft);
                return (
                  <article key={`${item.employeeId}-${item.vaccineName}-${item.revaccinationDate}`} className={`due-item is-${status}`}>
                    <div className="due-item-head">
                      <div>
                        {!isPersonView ? (
                          <h3>
                            <Link
                              to={buildHistoryLink(item.employeeId, item.vaccineName, item.lastVaccinationDate)}
                              className="inline-link"
                            >
                              {item.fullName}
                            </Link>
                          </h3>
                        ) : null}
                        <p className="muted">{item.vaccineName}</p>
                      </div>
                      <span className={`status-pill is-${status}`}>
                        {status === "overdue" ? t("revaccination.statusOverdue") : null}
                        {status === "soon" ? t("revaccination.statusSoon") : null}
                        {status === "planned" ? t("revaccination.statusPlanned") : null}
                      </span>
                    </div>

                    <dl className="meta-grid">
                      <div>
                        <dt>{t("revaccination.lastVaccinationDate")}</dt>
                        <dd>{formatter.format(new Date(item.lastVaccinationDate))}</dd>
                      </div>
                      <div>
                        <dt>{t("revaccination.revaccinationDate")}</dt>
                        <dd>{formatter.format(new Date(item.revaccinationDate))}</dd>
                      </div>
                      <div>
                        <dt>{t("revaccination.daysLeft")}</dt>
                        <dd>{item.daysLeft}</dd>
                      </div>
                      {!isPersonView ? (
                        <div>
                          <dt>{t("revaccination.department")}</dt>
                          <dd>{departments[item.departmentId] ?? t("revaccination.departmentUnknown")}</dd>
                        </div>
                      ) : null}
                      {isPersonView ? (
                        <div>
                          <dt>{t("revaccination.details")}</dt>
                          <dd>
                            <Link
                              to={buildHistoryLink(item.employeeId, item.vaccineName, item.lastVaccinationDate)}
                              className="inline-link"
                            >
                              {t("revaccination.details")}
                            </Link>
                          </dd>
                        </div>
                      ) : null}
                    </dl>
                  </article>
                );
              })}
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
