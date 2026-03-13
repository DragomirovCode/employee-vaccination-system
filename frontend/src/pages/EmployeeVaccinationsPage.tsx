import { useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiGet } from "../shared/api/client";
import {
  ApiHttpError,
  DepartmentDto,
  DocumentDto,
  EmployeeDto,
  VaccinationReadDto,
  VaccineDto
} from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

function formatEmployeeName(employee: EmployeeDto): string {
  return [employee.lastName, employee.firstName, employee.middleName].filter(Boolean).join(" ");
}

function formatBytes(bytes: number, locale: string): string {
  return new Intl.NumberFormat(locale === "ru" ? "ru-RU" : "en-US").format(bytes) + " B";
}

export function EmployeeVaccinationsPage() {
  const { session } = useAuth();
  const { employeeId = "" } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();
  const { locale, t } = useI18n();
  const [employee, setEmployee] = useState<EmployeeDto | null>(null);
  const [departmentName, setDepartmentName] = useState<string>("");
  const [items, setItems] = useState<VaccinationReadDto[]>([]);
  const [vaccineNames, setVaccineNames] = useState<Record<string, string>>({});
  const [documentsByVaccination, setDocumentsByVaccination] = useState<Record<string, DocumentDto[]>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!employeeId) {
      setError(t("employeeVaccinations.notFound"));
      return;
    }

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);

      try {
        const [employees, departments, vaccines, vaccinations] = await Promise.all([
          apiGet<EmployeeDto[]>("/employees"),
          apiGet<DepartmentDto[]>("/departments"),
          apiGet<VaccineDto[]>("/vaccines"),
          apiGet<VaccinationReadDto[]>(`/employees/${employeeId}/vaccinations`)
        ]);

        if (cancelled) return;

        const currentEmployee = employees.find((item) => item.id === employeeId) ?? null;
        setEmployee(currentEmployee);

        const departmentMap = Object.fromEntries(departments.map((item) => [item.id, item.name]));
        setDepartmentName(currentEmployee ? departmentMap[currentEmployee.departmentId] ?? "" : "");

        setVaccineNames(Object.fromEntries(vaccines.map((item) => [item.id, item.name])));
        setItems(vaccinations);

        const documentsEntries = await Promise.all(
          vaccinations.map(async (vaccination) => {
            const documents = await apiGet<DocumentDto[]>(`/vaccinations/${vaccination.id}/documents`);
            return [vaccination.id, documents] as const;
          })
        );

        if (cancelled) return;
        setDocumentsByVaccination(Object.fromEntries(documentsEntries));
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("employeeVaccinations.unexpectedApiError");
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
  }, [employeeId, t]);

  const formatter = new Intl.DateTimeFormat(locale === "ru" ? "ru-RU" : "en-US");
  const focusedVaccineName = searchParams.get("vaccineName");
  const focusedVaccinationDate = searchParams.get("vaccinationDate");
  const isOwnHistory = Boolean(session?.userId && employee?.userId === session.userId);
  const title = employee ? formatEmployeeName(employee) : t("employeeVaccinations.title");
  const visibleItems =
    focusedVaccineName && focusedVaccinationDate
      ? items.filter(
          (item) =>
            (vaccineNames[item.vaccineId] ?? "") === focusedVaccineName && item.vaccinationDate === focusedVaccinationDate
        )
      : items;
  const canShowFullHistory = items.length > visibleItems.length;

  function openFullHistory() {
    setSearchParams({});
  }

  return (
    <section className="stack-lg">
      <div className="page-actions">
        <Link to="/" className="inline-link">
          {t("employeeVaccinations.back")}
        </Link>
      </div>

      <article className="card">
        <div className="page-head">
          <div>
            <h2>{title}</h2>
            <p className="muted">{isOwnHistory ? t("employeeVaccinations.ownSubtitle") : t("employeeVaccinations.subtitle")}</p>
          </div>
          {departmentName ? <span className="summary-pill">{departmentName}</span> : null}
        </div>

        {focusedVaccineName && focusedVaccinationDate && canShowFullHistory ? (
          <div className="notice-bar">
            <p>{t("employeeVaccinations.focusedNotice")}</p>
            <button type="button" className="button-secondary" onClick={openFullHistory}>
              {t("employeeVaccinations.showAll")}
            </button>
          </div>
        ) : null}

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && visibleItems.length === 0 ? (
          <div className="empty-state">
            <h3>{t("employeeVaccinations.emptyTitle")}</h3>
            <p>{t("employeeVaccinations.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && visibleItems.length > 0 ? (
          <div className="history-list">
            {visibleItems.map((item) => {
              const documents = documentsByVaccination[item.id] ?? [];
              return (
                <article key={item.id} className="history-item">
                  <div className="history-head">
                    <div>
                      <h3>{vaccineNames[item.vaccineId] ?? t("employeeVaccinations.unknownVaccine")}</h3>
                      <p className="muted">
                        {t("employeeVaccinations.vaccinationDate")}: {formatter.format(new Date(item.vaccinationDate))}
                      </p>
                    </div>
                    <span className="summary-pill">
                      {t("employeeVaccinations.dose")} {item.doseNumber}
                    </span>
                  </div>

                  <dl className="meta-grid">
                    <div>
                      <dt>{t("employeeVaccinations.revaccinationDate")}</dt>
                      <dd>{item.revaccinationDate ? formatter.format(new Date(item.revaccinationDate)) : t("employeeVaccinations.notSpecified")}</dd>
                    </div>
                    <div>
                      <dt>{t("employeeVaccinations.batchNumber")}</dt>
                      <dd>{item.batchNumber ?? t("employeeVaccinations.notSpecified")}</dd>
                    </div>
                    <div>
                      <dt>{t("employeeVaccinations.nextDoseDate")}</dt>
                      <dd>{item.nextDoseDate ? formatter.format(new Date(item.nextDoseDate)) : t("employeeVaccinations.notSpecified")}</dd>
                    </div>
                    <div>
                      <dt>{t("employeeVaccinations.expirationDate")}</dt>
                      <dd>{item.expirationDate ? formatter.format(new Date(item.expirationDate)) : t("employeeVaccinations.notSpecified")}</dd>
                    </div>
                  </dl>

                  {item.notes ? (
                    <div className="history-block">
                      <h4>{t("employeeVaccinations.notes")}</h4>
                      <p>{item.notes}</p>
                    </div>
                  ) : null}

                  <div className="history-block">
                    <h4>{t("employeeVaccinations.documents")}</h4>
                    {documents.length === 0 ? <p>{t("employeeVaccinations.noDocuments")}</p> : null}
                    {documents.length > 0 ? (
                      <ul className="doc-list">
                        {documents.map((document) => (
                          <li key={document.id}>
                            <span>{document.fileName}</span>
                            <span className="muted">
                              {document.mimeType}, {formatBytes(document.fileSize, locale)}
                            </span>
                          </li>
                        ))}
                      </ul>
                    ) : null}
                  </div>
                </article>
              );
            })}
          </div>
        ) : null}
      </article>
    </section>
  );
}
