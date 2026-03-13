import { FormEvent, useEffect, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiDelete, apiGet, apiPost, apiPut } from "../shared/api/client";
import {
  ApiHttpError,
  DepartmentDto,
  DocumentDto,
  EmployeeDto,
  VaccinationReadDto,
  VaccinationWriteRequest,
  VaccinationWriteResponse,
  VaccineDto
} from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

function formatEmployeeName(employee: EmployeeDto): string {
  return [employee.lastName, employee.firstName, employee.middleName].filter(Boolean).join(" ");
}

function formatBytes(bytes: number, locale: string): string {
  return new Intl.NumberFormat(locale === "ru" ? "ru-RU" : "en-US").format(bytes) + " B";
}

type VaccinationFormState = {
  id: string | null;
  vaccineId: string;
  vaccinationDate: string;
  doseNumber: string;
  batchNumber: string;
  expirationDate: string;
  notes: string;
};

const EMPTY_FORM: VaccinationFormState = {
  id: null,
  vaccineId: "",
  vaccinationDate: "",
  doseNumber: "1",
  batchNumber: "",
  expirationDate: "",
  notes: ""
};

function toFormState(item: VaccinationReadDto): VaccinationFormState {
  return {
    id: item.id,
    vaccineId: item.vaccineId,
    vaccinationDate: item.vaccinationDate,
    doseNumber: String(item.doseNumber),
    batchNumber: item.batchNumber ?? "",
    expirationDate: item.expirationDate ?? "",
    notes: item.notes ?? ""
  };
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
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [formState, setFormState] = useState<VaccinationFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);

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
  const canManageVaccinations = Boolean(session?.roles.some((role) => role === "MEDICAL" || role === "ADMIN"));
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

  function startCreate() {
    setFormError(null);
    setFormState(EMPTY_FORM);
  }

  function startEdit(item: VaccinationReadDto) {
    setFormError(null);
    setFormState(toFormState(item));
  }

  function cancelEdit() {
    setFormError(null);
    setFormState(EMPTY_FORM);
  }

  async function submitForm(e: FormEvent) {
    e.preventDefault();
    if (!employeeId) return;

    const doseNumber = Number.parseInt(formState.doseNumber, 10);
    if (!formState.vaccineId || !formState.vaccinationDate || Number.isNaN(doseNumber) || doseNumber <= 0) {
      setFormError(t("vaccinationEditor.validation"));
      return;
    }

    const payload: VaccinationWriteRequest = {
      employeeId,
      vaccineId: formState.vaccineId,
      vaccinationDate: formState.vaccinationDate,
      doseNumber,
      batchNumber: formState.batchNumber.trim() || null,
      expirationDate: formState.expirationDate || null,
      notes: formState.notes.trim() || null
    };

    setSubmitting(true);
    setFormError(null);
    try {
      const response = formState.id
        ? await apiPut<VaccinationWriteResponse>(`/vaccinations/${formState.id}`, payload)
        : await apiPost<VaccinationWriteResponse>("/vaccinations", payload);

      const updatedItem = await apiGet<VaccinationReadDto>(`/vaccinations/${response.id}`);
      setItems((current) => {
        const next = formState.id ? current.map((item) => (item.id === updatedItem.id ? updatedItem : item)) : [updatedItem, ...current];
        return [...next].sort((left, right) => right.vaccinationDate.localeCompare(left.vaccinationDate));
      });
      setDocumentsByVaccination((current) =>
        current[updatedItem.id]
          ? current
          : {
              ...current,
              [updatedItem.id]: []
            }
      );
      setFormState(EMPTY_FORM);
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("vaccinationEditor.unexpectedApiError");
      setFormError(message);
    } finally {
      setSubmitting(false);
    }
  }

  async function deleteVaccination(id: string) {
    setDeletingId(id);
    setFormError(null);
    try {
      await apiDelete(`/vaccinations/${id}`);
      setItems((current) => current.filter((item) => item.id !== id));
      setDocumentsByVaccination((current) => {
        const next = { ...current };
        delete next[id];
        return next;
      });
      if (formState.id === id) {
        setFormState(EMPTY_FORM);
      }
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("vaccinationEditor.unexpectedApiError");
      setFormError(message);
    } finally {
      setDeletingId(null);
    }
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

                  {canManageVaccinations ? (
                    <div className="history-actions">
                      <button type="button" className="button-secondary" onClick={() => startEdit(item)}>
                        {t("vaccinationEditor.edit")}
                      </button>
                      <button type="button" onClick={() => deleteVaccination(item.id)} disabled={deletingId === item.id}>
                        {deletingId === item.id ? t("vaccinationEditor.deleting") : t("vaccinationEditor.delete")}
                      </button>
                    </div>
                  ) : null}
                </article>
              );
            })}
          </div>
        ) : null}
      </article>

      {canManageVaccinations ? (
        <article className="card">
          <div className="page-head">
            <div>
              <h2>{formState.id ? t("vaccinationEditor.editTitle") : t("vaccinationEditor.createTitle")}</h2>
              <p className="muted">{t("vaccinationEditor.description")}</p>
            </div>
            {!formState.id ? (
              <button type="button" className="button-secondary" onClick={startCreate}>
                {t("vaccinationEditor.reset")}
              </button>
            ) : null}
          </div>

          <form className="editor-form" onSubmit={submitForm}>
            <label>
              {t("vaccinationEditor.vaccine")}
              <select value={formState.vaccineId} onChange={(e) => setFormState((current) => ({ ...current, vaccineId: e.target.value }))}>
                <option value="">{t("vaccinationEditor.selectVaccine")}</option>
                {Object.entries(vaccineNames).map(([id, name]) => (
                  <option key={id} value={id}>
                    {name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {t("vaccinationEditor.vaccinationDate")}
              <input
                type="date"
                value={formState.vaccinationDate}
                onChange={(e) => setFormState((current) => ({ ...current, vaccinationDate: e.target.value }))}
              />
            </label>
            <label>
              {t("vaccinationEditor.doseNumber")}
              <input
                type="number"
                min="1"
                step="1"
                value={formState.doseNumber}
                onChange={(e) => setFormState((current) => ({ ...current, doseNumber: e.target.value }))}
              />
            </label>
            <label>
              {t("vaccinationEditor.batchNumber")}
              <input value={formState.batchNumber} onChange={(e) => setFormState((current) => ({ ...current, batchNumber: e.target.value }))} />
            </label>
            <label>
              {t("vaccinationEditor.expirationDate")}
              <input
                type="date"
                value={formState.expirationDate}
                onChange={(e) => setFormState((current) => ({ ...current, expirationDate: e.target.value }))}
              />
            </label>
            <label className="editor-form-span">
              {t("vaccinationEditor.notes")}
              <textarea value={formState.notes} onChange={(e) => setFormState((current) => ({ ...current, notes: e.target.value }))} rows={4} />
            </label>

            {formError ? <p className="warn editor-form-span">{formError}</p> : null}

            <div className="editor-actions editor-form-span">
              {formState.id ? (
                <button type="button" className="button-secondary" onClick={cancelEdit}>
                  {t("vaccinationEditor.cancel")}
                </button>
              ) : null}
              <button type="submit" disabled={submitting}>
                {submitting ? t("vaccinationEditor.saving") : t("vaccinationEditor.save")}
              </button>
            </div>
          </form>
        </article>
      ) : null}
    </section>
  );
}
