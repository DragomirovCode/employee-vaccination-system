import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiGet, apiGetBlob } from "../shared/api/client";
import {
  ApiHttpError,
  DepartmentDto,
  VaccinationCoverageDepartmentItem,
  VaccinationCoverageEmployeeItem,
  VaccinationCoverageVaccineItem
} from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { matchesSearchQuery } from "../shared/search";

type CoverageMode = "department" | "employee" | "vaccine";
type ExportFormat = "csv" | "xlsx" | "pdf";
type CoverageSummaryItem = VaccinationCoverageDepartmentItem | VaccinationCoverageVaccineItem;
type EmployeeStatusFilter = "ALL" | VaccinationCoverageEmployeeItem["status"];
type EmployeeScope =
  | { kind: "department"; department: VaccinationCoverageDepartmentItem }
  | { kind: "all" };

function getDefaultDateRange() {
  const now = new Date();
  const year = now.getFullYear();
  return {
    dateFrom: `${year}-01-01`,
    dateTo: `${year}-12-31`
  };
}

function getCoverageTone(coveragePercent: number): "low" | "medium" | "high" {
  if (coveragePercent < 50) return "low";
  if (coveragePercent < 80) return "medium";
  return "high";
}

function getEmployeeStatusLabel(item: VaccinationCoverageEmployeeItem, t: (key: string) => string) {
  switch (item.status) {
    case "CURRENT":
      return t("coverage.employeeStatusCurrentShort");
    case "DUE_SOON":
      return t("coverage.employeeStatusDueSoonShort");
    case "MISSING":
      return t("coverage.employeeStatusMissingShort");
  }
}

function buildEmployeeHistoryLink(employeeId: string): string {
  return `/employees/${employeeId}/vaccinations`;
}

export function CoverageReportPage() {
  const { session } = useAuth();
  const { locale, t } = useI18n();
  const defaults = getDefaultDateRange();
  const allDepartmentsLabel = session?.roles.includes("HR") ? t("coverage.myDepartments") : t("coverage.allDepartments");
  const [mode, setMode] = useState<CoverageMode>("department");
  const [dateFrom, setDateFrom] = useState(defaults.dateFrom);
  const [dateTo, setDateTo] = useState(defaults.dateTo);
  const [draftDateFrom, setDraftDateFrom] = useState(defaults.dateFrom);
  const [draftDateTo, setDraftDateTo] = useState(defaults.dateTo);
  const [departmentId, setDepartmentId] = useState("");
  const [draftDepartmentId, setDraftDepartmentId] = useState("");
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [items, setItems] = useState<CoverageSummaryItem[]>([]);
  const [employeeItems, setEmployeeItems] = useState<VaccinationCoverageEmployeeItem[]>([]);
  const [employeeScope, setEmployeeScope] = useState<EmployeeScope | null>(null);
  const [loading, setLoading] = useState(false);
  const [employeeLoading, setEmployeeLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [employeeError, setEmployeeError] = useState<string | null>(null);
  const [exportFormat, setExportFormat] = useState<ExportFormat>("csv");
  const [exporting, setExporting] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [employeeStatusFilter, setEmployeeStatusFilter] = useState<EmployeeStatusFilter>("ALL");
  const [employeeRevaccinationDateFrom, setEmployeeRevaccinationDateFrom] = useState("");
  const [employeeRevaccinationDateTo, setEmployeeRevaccinationDateTo] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadDepartments() {
      try {
        const response = await apiGet<DepartmentDto[]>("/departments");
        if (!cancelled) {
          setDepartments(response);
        }
      } catch {
        if (!cancelled) {
          setDepartments([]);
        }
      }
    }

    void loadDepartments();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      if (mode === "employee") {
        setItems([]);
        setLoading(false);
        setError(null);
        return;
      }

      setLoading(true);
      setError(null);

      const params = new URLSearchParams({
        dateFrom,
        dateTo
      });
      if (departmentId) {
        params.set("departmentId", departmentId);
      }

      try {
        const summaryPath =
          mode === "department"
            ? `/reports/vaccination-coverage?${params.toString()}`
            : `/reports/vaccination-coverage-by-vaccine?${params.toString()}`;

        const summaryResponse =
          mode === "department"
            ? await apiGet<VaccinationCoverageDepartmentItem[]>(summaryPath)
            : await apiGet<VaccinationCoverageVaccineItem[]>(summaryPath);

        if (!cancelled) {
          setItems(summaryResponse);
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("coverage.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    setEmployeeScope(mode === "employee" ? { kind: "all" } : null);
    setEmployeeItems([]);
    setEmployeeError(null);
    setEmployeeStatusFilter("ALL");
    setEmployeeRevaccinationDateFrom("");
    setEmployeeRevaccinationDateTo("");
    void load();
    return () => {
      cancelled = true;
    };
  }, [dateFrom, dateTo, departmentId, mode, t]);

  useEffect(() => {
    let cancelled = false;

    if ((mode !== "department" && mode !== "employee") || !employeeScope) {
      setEmployeeItems([]);
      setEmployeeError(null);
      setEmployeeLoading(false);
      return () => {
        cancelled = true;
      };
    }

    const activeEmployeeScope = employeeScope;

    async function loadEmployeeCoverage() {
      setEmployeeLoading(true);
      setEmployeeError(null);

      try {
        const params = new URLSearchParams({
          dateFrom,
          dateTo
        });
        if (activeEmployeeScope.kind === "department") {
          params.set("departmentId", activeEmployeeScope.department.departmentId);
        } else if (departmentId) {
          params.set("departmentId", departmentId);
        }
        const response = await apiGet<VaccinationCoverageEmployeeItem[]>(`/reports/vaccination-coverage-by-employee?${params.toString()}`);
        if (!cancelled) {
          setEmployeeItems(response);
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("coverage.employeeUnexpectedApiError");
        setEmployeeError(message);
      } finally {
        if (!cancelled) {
          setEmployeeLoading(false);
        }
      }
    }

    void loadEmployeeCoverage();
    return () => {
      cancelled = true;
    };
  }, [dateFrom, dateTo, departmentId, employeeScope, mode, t]);

  function applyFilters(nextDateFrom: string, nextDateTo: string, nextDepartmentId: string) {
    if (!nextDateFrom || !nextDateTo) {
      setError(t("coverage.invalidDateRange"));
      return;
    }

    setError(null);
    setEmployeeError(null);
    setSearchQuery("");
    setEmployeeStatusFilter("ALL");
    setDateFrom(nextDateFrom);
    setDateTo(nextDateTo);
    setDepartmentId(nextDepartmentId);
  }

  async function exportReport() {
    setError(null);
    setExporting(true);
    try {
      const params = new URLSearchParams({
        dateFrom,
        dateTo,
        format: exportFormat
      });
      if (departmentId) {
        params.set("departmentId", departmentId);
      }

      const path =
        mode === "department"
          ? `/reports/vaccination-coverage/export?${params.toString()}`
          : `/reports/vaccination-coverage-by-vaccine/export?${params.toString()}`;

      const { blob, contentDisposition } = await apiGetBlob(path, {
        headers: {
          "Accept-Language": locale
        }
      });
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement("a");
      const fallbackName = mode === "department" ? `vaccination-coverage.${exportFormat}` : `vaccination-coverage-by-vaccine.${exportFormat}`;
      const fileNameMatch = contentDisposition?.match(/filename="?(.*?)"?$/i);
      link.href = url;
      link.download = fileNameMatch?.[1] || fallbackName;
      link.style.display = "none";
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("coverage.exportError");
      setError(message);
    } finally {
      setExporting(false);
    }
  }

  async function exportEmployeeReport() {
    if (!employeeScope) {
      return;
    }

    const activeEmployeeScope = employeeScope;

    setEmployeeError(null);
    setExporting(true);
    try {
      const params = new URLSearchParams({
        dateFrom,
        dateTo,
        format: exportFormat
      });
      if (activeEmployeeScope.kind === "department") {
        params.set("departmentId", activeEmployeeScope.department.departmentId);
      } else if (departmentId) {
        params.set("departmentId", departmentId);
      }
      if (employeeStatusFilter !== "ALL") {
        params.set("status", employeeStatusFilter);
      }
      if (employeeRevaccinationDateFrom) {
        params.set("revaccinationDateFrom", employeeRevaccinationDateFrom);
      }
      if (employeeRevaccinationDateTo) {
        params.set("revaccinationDateTo", employeeRevaccinationDateTo);
      }

      const path = `/reports/vaccination-coverage-by-employee/export?${params.toString()}`;
      const { blob, contentDisposition } = await apiGetBlob(path, {
        headers: {
          "Accept-Language": locale
        }
      });
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement("a");
      const fileNameMatch = contentDisposition?.match(/filename="?(.*?)"?$/i);
      link.href = url;
      link.download = fileNameMatch?.[1] || `vaccination-coverage-by-employee.${exportFormat}`;
      link.style.display = "none";
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("coverage.exportError");
      setEmployeeError(message);
    } finally {
      setExporting(false);
    }
  }

  const filteredItems = useMemo(
    () =>
      items.filter((item) => {
        const name = "departmentName" in item ? item.departmentName : item.vaccineName;
        return matchesSearchQuery(
          searchQuery,
          name,
          item.employeesTotal,
          item.employeesCovered,
          item.coveragePercent.toFixed(1)
        );
      }),
    [items, searchQuery]
  );

  const filteredEmployeeItems = useMemo(
    () =>
      employeeItems.filter((item) => {
        if (employeeStatusFilter !== "ALL" && item.status !== employeeStatusFilter) {
          return false;
        }
        if (employeeRevaccinationDateFrom && (!item.revaccinationDate || item.revaccinationDate < employeeRevaccinationDateFrom)) {
          return false;
        }
        if (employeeRevaccinationDateTo && (!item.revaccinationDate || item.revaccinationDate > employeeRevaccinationDateTo)) {
          return false;
        }

        return matchesSearchQuery(
          searchQuery,
          item.fullName,
          item.departmentName,
          getEmployeeStatusLabel(item, t),
          item.revaccinationDate ?? t("coverage.notSpecified")
        );
      }),
    [employeeItems, employeeRevaccinationDateFrom, employeeRevaccinationDateTo, employeeStatusFilter, searchQuery, t]
  );

  const isDepartmentDrilldown = mode === "department" && employeeScope?.kind === "department";
  const isEmployeeView = mode === "employee" || employeeScope !== null;
  const drilldownSubtitle =
    employeeScope?.kind === "department"
      ? `${t("coverage.department")}: ${employeeScope.department.departmentName}. ${t("coverage.coveredEmployees")}: ${employeeScope.department.employeesCovered} ${t("coverage.ofEmployees")} ${employeeScope.department.employeesTotal}`
      : t("coverage.allEmployeesDescription");

  return (
    <section className="stack-lg">
      <article className="card">
        {!isDepartmentDrilldown ? (
          <>
            <div className="page-head">
              <div>
                <h2>{t("coverage.title")}</h2>
                <p className="muted">{t("coverage.description")}</p>
              </div>
              <div className="segmented-control" role="tablist" aria-label={t("coverage.modeLabel")}>
                <button
                  type="button"
                  className={mode === "department" ? "is-active" : ""}
                  onClick={() => setMode("department")}
                >
                  {t("coverage.byDepartment")}
                </button>
                <button
                  type="button"
                  className={mode === "employee" ? "is-active" : ""}
                  onClick={() => setMode("employee")}
                >
                  {t("coverage.byAllEmployees")}
                </button>
                <button
                  type="button"
                  className={mode === "vaccine" ? "is-active" : ""}
                  onClick={() => setMode("vaccine")}
                >
                  {t("coverage.byVaccine")}
                </button>
              </div>
            </div>

            <div className="toolbar">
              <label className="toolbar-field">
                <span>{t("coverage.dateFrom")}</span>
                <input
                  type="date"
                  value={draftDateFrom}
                  onChange={(e) => {
                    const nextDateFrom = e.target.value;
                    setDraftDateFrom(nextDateFrom);
                    applyFilters(nextDateFrom, draftDateTo, draftDepartmentId);
                  }}
                />
              </label>
              <label className="toolbar-field">
                <span>{t("coverage.dateTo")}</span>
                <input
                  type="date"
                  value={draftDateTo}
                  onChange={(e) => {
                    const nextDateTo = e.target.value;
                    setDraftDateTo(nextDateTo);
                    applyFilters(draftDateFrom, nextDateTo, draftDepartmentId);
                  }}
                />
              </label>
              <label className="toolbar-field">
                <span>{t("coverage.department")}</span>
                <select
                  value={draftDepartmentId}
                  onChange={(e) => {
                    const nextDepartmentId = e.target.value;
                    setDraftDepartmentId(nextDepartmentId);
                    applyFilters(draftDateFrom, draftDateTo, nextDepartmentId);
                  }}
                >
                  <option value="">{allDepartmentsLabel}</option>
                  {departments.map((department) => (
                    <option key={department.id} value={department.id}>
                      {department.name}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          </>
        ) : (
          <div className="page-head">
            <div>
              <h2>{t("coverage.employeeDrilldownTitle")}</h2>
              <p className="muted">{drilldownSubtitle}</p>
            </div>
          </div>
        )}

        {isDepartmentDrilldown && mode === "department" ? (
          <div className="coverage-drilldown-bar">
            <button type="button" className="inline-link-button" onClick={() => setEmployeeScope(null)}>
              {t("coverage.backToDepartmentsArrow")}
            </button>
          </div>
        ) : null}

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
          {isEmployeeView ? (
            <>
              <label className="toolbar-field">
                <span>{t("coverage.employeeStatusFilter")}</span>
                <select value={employeeStatusFilter} onChange={(e) => setEmployeeStatusFilter(e.target.value as EmployeeStatusFilter)}>
                  <option value="ALL">{t("coverage.employeeStatusAll")}</option>
                  <option value="CURRENT">{t("coverage.employeeStatusCurrent")}</option>
                  <option value="DUE_SOON">{t("coverage.employeeStatusDueSoon")}</option>
                  <option value="MISSING">{t("coverage.employeeStatusMissing")}</option>
                </select>
              </label>
              <label className="toolbar-field">
                <span>{t("coverage.employeeRevaccinationDateFrom")}</span>
                <input type="date" value={employeeRevaccinationDateFrom} onChange={(e) => setEmployeeRevaccinationDateFrom(e.target.value)} />
              </label>
              <label className="toolbar-field">
                <span>{t("coverage.employeeRevaccinationDateTo")}</span>
                <input type="date" value={employeeRevaccinationDateTo} onChange={(e) => setEmployeeRevaccinationDateTo(e.target.value)} />
              </label>
              <div className="toolbar-actions">
                <label className="toolbar-field toolbar-field-compact">
                  <span>{t("coverage.exportFormat")}</span>
                  <select value={exportFormat} onChange={(e) => setExportFormat(e.target.value as ExportFormat)} disabled={exporting}>
                    <option value="csv">CSV</option>
                    <option value="xlsx">XLSX</option>
                    <option value="pdf">PDF</option>
                  </select>
                </label>
                <button type="button" className="button-secondary" onClick={() => void exportEmployeeReport()} disabled={exporting}>
                  {exporting ? t("coverage.exportDownloading") : t("coverage.export")}
                </button>
              </div>
            </>
          ) : (
            <div className="toolbar-actions">
              <label className="toolbar-field toolbar-field-compact">
                <span>{t("coverage.exportFormat")}</span>
                <select value={exportFormat} onChange={(e) => setExportFormat(e.target.value as ExportFormat)} disabled={exporting}>
                  <option value="csv">CSV</option>
                  <option value="xlsx">XLSX</option>
                  <option value="pdf">PDF</option>
                </select>
              </label>
              <button type="button" className="button-secondary" onClick={() => void exportReport()} disabled={exporting}>
                {exporting ? t("coverage.exportDownloading") : t("coverage.export")}
              </button>
            </div>
          )}
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && !isEmployeeView && filteredItems.length === 0 ? (
          <div className="empty-state">
            <h3>{t("coverage.emptyTitle")}</h3>
            <p>{t("coverage.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && mode !== "employee" && !isEmployeeView && filteredItems.length > 0 ? (
          <div className="coverage-table">
            <div className="coverage-row coverage-row-head">
              <span>{mode === "department" ? t("coverage.department") : t("coverage.vaccine")}</span>
              <span>{t("coverage.totalEmployees")}</span>
              <span>{t("coverage.coveredEmployees")}</span>
              <span>{t("coverage.coveragePercent")}</span>
            </div>
            {filteredItems.map((item) => {
              const tone = getCoverageTone(item.coveragePercent);
              const name = "departmentName" in item ? item.departmentName : item.vaccineName;
              const isDepartmentRow = "departmentId" in item;

              return (
                <div key={isDepartmentRow ? item.departmentId : item.vaccineId} className="coverage-row">
                  <span className="coverage-name">
                    {isDepartmentRow ? (
                      <button type="button" className="inline-link-button" onClick={() => setEmployeeScope({ kind: "department", department: item })}>
                        {name}
                      </button>
                    ) : (
                      name
                    )}
                  </span>
                  <span>{item.employeesTotal}</span>
                  <span>{item.employeesCovered}</span>
                  <span>
                    <span className={`coverage-pill is-${tone}`}>{item.coveragePercent.toFixed(1)}%</span>
                  </span>
                </div>
              );
            })}
          </div>
        ) : null}

        {isEmployeeView ? (
          <>
            {employeeLoading ? <p>{t("common.loading")}</p> : null}
            {employeeError ? <p className="warn">{employeeError}</p> : null}

            {!employeeLoading && !employeeError && filteredEmployeeItems.length === 0 ? (
              <div className="empty-state">
                <h3>{t("coverage.employeeEmptyTitle")}</h3>
                <p>{t("coverage.employeeEmptyDescription")}</p>
              </div>
            ) : null}

            {!employeeLoading && !employeeError && filteredEmployeeItems.length > 0 ? (
              <div className="coverage-table">
                <div className="coverage-row coverage-row-head coverage-employee-row">
                  <span>{t("coverage.employee")}</span>
                  <span>{t("coverage.department")}</span>
                  <span>{t("coverage.employeeRevaccinationDate")}</span>
                  <span>{t("coverage.employeeStatus")}</span>
                </div>
                {filteredEmployeeItems.map((item) => (
                  <div key={item.employeeId} className="coverage-row coverage-employee-row">
                    <span className="coverage-name">
                      <Link
                        to={buildEmployeeHistoryLink(item.employeeId)}
                        state={{ source: "coverage" }}
                        className="inline-link"
                      >
                        {item.fullName}
                      </Link>
                    </span>
                    <span>{item.departmentName}</span>
                    <span>{item.revaccinationDate ?? t("coverage.notSpecified")}</span>
                    <span>
                      <span className={`coverage-pill ${item.status === "MISSING" ? "is-low" : item.status === "DUE_SOON" ? "is-medium" : "is-high"}`}>
                        {getEmployeeStatusLabel(item, t)}
                      </span>
                    </span>
                  </div>
                ))}
              </div>
            ) : null}
          </>
        ) : null}
      </article>
    </section>
  );
}
