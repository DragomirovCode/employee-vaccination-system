import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiGet } from "../shared/api/client";
import { ApiHttpError, EmployeeDto, VaccinationPage, VaccinationReadDto, VaccineDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { getDateSearchValues, matchesSearchQuery } from "../shared/search";

const PAGE_SIZE = 20;

function formatEmployeeName(employee: EmployeeDto): string {
  return [employee.lastName, employee.firstName, employee.middleName].filter(Boolean).join(" ");
}

function buildHistoryLink(employeeId: string, vaccineName: string, vaccinationDate: string): string {
  const params = new URLSearchParams({
    vaccineName,
    vaccinationDate
  });
  return `/employees/${employeeId}/vaccinations?${params.toString()}`;
}

export function VaccinationRegistryPage() {
  const { locale, t } = useI18n();
  const [employees, setEmployees] = useState<EmployeeDto[]>([]);
  const [vaccines, setVaccines] = useState<VaccineDto[]>([]);
  const [data, setData] = useState<VaccinationPage | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [employeeId, setEmployeeId] = useState("");
  const [vaccineId, setVaccineId] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [draftEmployeeId, setDraftEmployeeId] = useState("");
  const [draftVaccineId, setDraftVaccineId] = useState("");
  const [draftDateFrom, setDraftDateFrom] = useState("");
  const [draftDateTo, setDraftDateTo] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadReferenceData() {
      try {
        const [employeesResponse, vaccinesResponse] = await Promise.all([
          apiGet<EmployeeDto[]>("/employees"),
          apiGet<VaccineDto[]>("/vaccines")
        ]);
        if (cancelled) return;
        setEmployees(employeesResponse);
        setVaccines(vaccinesResponse);
      } catch {
        if (cancelled) return;
        setEmployees([]);
        setVaccines([]);
      }
    }

    void loadReferenceData();
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
          page: String(page),
          size: String(PAGE_SIZE)
        });
        if (employeeId) params.set("employeeId", employeeId);
        if (vaccineId) params.set("vaccineId", vaccineId);
        if (dateFrom) params.set("dateFrom", dateFrom);
        if (dateTo) params.set("dateTo", dateTo);

        const response = await apiGet<VaccinationPage>(`/vaccinations?${params.toString()}`);
        if (!cancelled) {
          setData(response);
        }
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("vaccinationRegistry.unexpectedApiError");
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
  }, [dateFrom, dateTo, employeeId, page, t, vaccineId]);

  const employeeMap = useMemo(
    () => Object.fromEntries(employees.map((employee) => [employee.id, formatEmployeeName(employee)])),
    [employees]
  );

  const vaccineMap = useMemo(
    () => Object.fromEntries(vaccines.map((vaccine) => [vaccine.id, vaccine.name])),
    [vaccines]
  );

  function onApplyFilters(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setPage(0);
    setEmployeeId(draftEmployeeId);
    setVaccineId(draftVaccineId);
    setDateFrom(draftDateFrom);
    setDateTo(draftDateTo);
  }

  function onResetFilters() {
    setError(null);
    setPage(0);
    setEmployeeId("");
    setVaccineId("");
    setDateFrom("");
    setDateTo("");
    setDraftEmployeeId("");
    setDraftVaccineId("");
    setDraftDateFrom("");
    setDraftDateTo("");
  }

  const items = data?.content ?? [];
  const filteredItems = items.filter((item: VaccinationReadDto) =>
    matchesSearchQuery(
      searchQuery,
      employeeMap[item.employeeId],
      vaccineMap[item.vaccineId],
      ...getDateSearchValues(item.vaccinationDate, locale === "ru" ? "ru-RU" : "en-US"),
      item.doseNumber,
      ...getDateSearchValues(item.nextDoseDate, locale === "ru" ? "ru-RU" : "en-US"),
      ...getDateSearchValues(item.revaccinationDate, locale === "ru" ? "ru-RU" : "en-US")
    )
  );
  const totalPages = data?.totalPages ?? 0;
  const canGoPrevious = page > 0 && !loading;
  const canGoNext = Boolean(data && page + 1 < data.totalPages) && !loading;
  const formatter = new Intl.DateTimeFormat(locale === "ru" ? "ru-RU" : "en-US");

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("vaccinationRegistry.title")}</h2>
            <p className="muted">{t("vaccinationRegistry.description")}</p>
          </div>
          <span className="summary-pill">
            {t("vaccinationRegistry.total")}: {data?.totalElements ?? 0}
          </span>
        </div>

        <form className="toolbar" onSubmit={onApplyFilters}>
          <label className="toolbar-field">
            <span>{t("vaccinationRegistry.employee")}</span>
            <select value={draftEmployeeId} onChange={(e) => setDraftEmployeeId(e.target.value)}>
              <option value="">{t("vaccinationRegistry.allEmployees")}</option>
              {employees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {formatEmployeeName(employee)}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-field">
            <span>{t("vaccinationRegistry.vaccine")}</span>
            <select value={draftVaccineId} onChange={(e) => setDraftVaccineId(e.target.value)}>
              <option value="">{t("vaccinationRegistry.allVaccines")}</option>
              {vaccines.map((vaccine) => (
                <option key={vaccine.id} value={vaccine.id}>
                  {vaccine.name}
                </option>
              ))}
            </select>
          </label>
          <label className="toolbar-field">
            <span>{t("vaccinationRegistry.dateFrom")}</span>
            <input type="date" value={draftDateFrom} onChange={(e) => setDraftDateFrom(e.target.value)} />
          </label>
          <label className="toolbar-field">
            <span>{t("vaccinationRegistry.dateTo")}</span>
            <input type="date" value={draftDateTo} onChange={(e) => setDraftDateTo(e.target.value)} />
          </label>
          <div className="toolbar-actions">
            <button type="submit" disabled={loading}>
              {t("vaccinationRegistry.apply")}
            </button>
            <button type="button" className="button-secondary" onClick={onResetFilters} disabled={loading}>
              {t("vaccinationRegistry.reset")}
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

        {!loading && !error && filteredItems.length === 0 ? (
          <div className="empty-state">
            <h3>{t("vaccinationRegistry.emptyTitle")}</h3>
            <p>{t("vaccinationRegistry.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && filteredItems.length > 0 ? (
          <>
            <div className="registry-table">
              <div className="registry-row registry-row-head">
                <span>{t("vaccinationRegistry.employee")}</span>
                <span>{t("vaccinationRegistry.vaccine")}</span>
                <span>{t("vaccinationRegistry.vaccinationDate")}</span>
                <span>{t("vaccinationRegistry.dose")}</span>
                <span>{t("vaccinationRegistry.nextDoseDate")}</span>
                <span>{t("vaccinationRegistry.revaccinationDate")}</span>
              </div>
              {filteredItems.map((item: VaccinationReadDto) => {
                const employeeName = employeeMap[item.employeeId] ?? t("vaccinationRegistry.employeeUnknown");
                const vaccineName = vaccineMap[item.vaccineId] ?? t("vaccinationRegistry.vaccineUnknown");
                return (
                  <div key={item.id} className="registry-row">
                    <span>
                      <Link to={buildHistoryLink(item.employeeId, vaccineName, item.vaccinationDate)} className="inline-link">
                        {employeeName}
                      </Link>
                    </span>
                    <span>{vaccineName}</span>
                    <span>{formatter.format(new Date(item.vaccinationDate))}</span>
                    <span>{item.doseNumber}</span>
                    <span>{item.nextDoseDate ? formatter.format(new Date(item.nextDoseDate)) : t("vaccinationRegistry.notSpecified")}</span>
                    <span>
                      {item.revaccinationDate ? formatter.format(new Date(item.revaccinationDate)) : t("vaccinationRegistry.notSpecified")}
                    </span>
                  </div>
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
