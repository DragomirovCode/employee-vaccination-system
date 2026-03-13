import { FormEvent, useEffect, useState } from "react";
import { apiGet } from "../shared/api/client";
import {
  ApiHttpError,
  DepartmentDto,
  VaccinationCoverageDepartmentItem,
  VaccinationCoverageVaccineItem
} from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type CoverageMode = "department" | "vaccine";

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

export function CoverageReportPage() {
  const { t } = useI18n();
  const defaults = getDefaultDateRange();
  const [mode, setMode] = useState<CoverageMode>("department");
  const [dateFrom, setDateFrom] = useState(defaults.dateFrom);
  const [dateTo, setDateTo] = useState(defaults.dateTo);
  const [draftDateFrom, setDraftDateFrom] = useState(defaults.dateFrom);
  const [draftDateTo, setDraftDateTo] = useState(defaults.dateTo);
  const [departmentId, setDepartmentId] = useState("");
  const [draftDepartmentId, setDraftDepartmentId] = useState("");
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [items, setItems] = useState<Array<VaccinationCoverageDepartmentItem | VaccinationCoverageVaccineItem>>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
        const params = new URLSearchParams({
          dateFrom,
          dateTo
        });
        if (departmentId) {
          params.set("departmentId", departmentId);
        }

        const path =
          mode === "department"
            ? `/reports/vaccination-coverage?${params.toString()}`
            : `/reports/vaccination-coverage-by-vaccine?${params.toString()}`;

        const response =
          mode === "department"
            ? await apiGet<VaccinationCoverageDepartmentItem[]>(path)
            : await apiGet<VaccinationCoverageVaccineItem[]>(path);

        if (!cancelled) {
          setItems(response);
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

    load();
    return () => {
      cancelled = true;
    };
  }, [dateFrom, dateTo, departmentId, mode, t]);

  function onApplyFilters(e: FormEvent) {
    e.preventDefault();
    if (!draftDateFrom || !draftDateTo) {
      setError(t("coverage.invalidDateRange"));
      return;
    }

    setError(null);
    setDateFrom(draftDateFrom);
    setDateTo(draftDateTo);
    setDepartmentId(draftDepartmentId);
  }

  return (
    <section className="stack-lg">
      <article className="card">
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
              className={mode === "vaccine" ? "is-active" : ""}
              onClick={() => setMode("vaccine")}
            >
              {t("coverage.byVaccine")}
            </button>
          </div>
        </div>

        <form className="toolbar" onSubmit={onApplyFilters}>
          <label className="toolbar-field">
            <span>{t("coverage.dateFrom")}</span>
            <input type="date" value={draftDateFrom} onChange={(e) => setDraftDateFrom(e.target.value)} />
          </label>
          <label className="toolbar-field">
            <span>{t("coverage.dateTo")}</span>
            <input type="date" value={draftDateTo} onChange={(e) => setDraftDateTo(e.target.value)} />
          </label>
          <label className="toolbar-field">
            <span>{t("coverage.department")}</span>
            <select value={draftDepartmentId} onChange={(e) => setDraftDepartmentId(e.target.value)}>
              <option value="">{t("coverage.allDepartments")}</option>
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
          </label>
          <div className="toolbar-actions">
            <button type="submit" disabled={loading}>
              {t("coverage.apply")}
            </button>
          </div>
        </form>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && items.length === 0 ? (
          <div className="empty-state">
            <h3>{t("coverage.emptyTitle")}</h3>
            <p>{t("coverage.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && items.length > 0 ? (
          <div className="coverage-table">
            <div className="coverage-row coverage-row-head">
              <span>{mode === "department" ? t("coverage.department") : t("coverage.vaccine")}</span>
              <span>{t("coverage.totalEmployees")}</span>
              <span>{t("coverage.coveredEmployees")}</span>
              <span>{t("coverage.coveragePercent")}</span>
            </div>
            {items.map((item) => {
              const tone = getCoverageTone(item.coveragePercent);
              const name = "departmentName" in item ? item.departmentName : item.vaccineName;
              return (
                <div key={"departmentId" in item ? item.departmentId : item.vaccineId} className="coverage-row">
                  <span className="coverage-name">{name}</span>
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
      </article>
    </section>
  );
}
