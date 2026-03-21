import { FormEvent, useEffect, useRef, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiDelete, apiGet, apiGetBlob, apiPost, apiPostForm, apiPut } from "../shared/api/client";
import {
  ApiHttpError,
  DepartmentDto,
  DocumentDto,
  DocumentWriteRequest,
  DocumentWriteResponse,
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
  const [documentErrors, setDocumentErrors] = useState<Record<string, string | null>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [documentBusyId, setDocumentBusyId] = useState<string | null>(null);
  const [formState, setFormState] = useState<VaccinationFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const editorCardRef = useRef<HTMLElement | null>(null);
  const vaccineFieldRef = useRef<HTMLSelectElement | null>(null);

  async function fetchDocumentsForVaccination(vaccinationId: string): Promise<DocumentDto[]> {
    return apiGet<DocumentDto[]>(`/vaccinations/${vaccinationId}/documents`);
  }

  async function refreshDocumentsForVaccination(vaccinationId: string): Promise<void> {
    const refreshedDocuments = await fetchDocumentsForVaccination(vaccinationId);
    setDocumentsByVaccination((current) => ({
      ...current,
      [vaccinationId]: refreshedDocuments
    }));
  }

  function setDocumentError(vaccinationId: string, message: string | null) {
    setDocumentErrors((current) => ({
      ...current,
      [vaccinationId]: message
    }));
  }

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
            const documents = await fetchDocumentsForVaccination(vaccination.id);
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

  function focusEditor() {
    window.requestAnimationFrame(() => {
      editorCardRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      window.setTimeout(() => vaccineFieldRef.current?.focus(), 200);
    });
  }

  function startCreate() {
    setFormError(null);
    setFormState(EMPTY_FORM);
    focusEditor();
  }

  function startEdit(item: VaccinationReadDto) {
    setFormError(null);
    setFormState(toFormState(item));
    focusEditor();
  }

  function cancelEdit() {
    setFormError(null);
    setFormState(EMPTY_FORM);
  }

  async function submitForm(e: FormEvent) {
    e.preventDefault();
    if (!employeeId) return;

    const doseNumber = Number.parseInt(formState.doseNumber, 10);
    if (!formState.vaccineId || !formState.vaccinationDate || !formState.expirationDate || Number.isNaN(doseNumber) || doseNumber <= 0) {
      setFormError(t("vaccinationEditor.validation"));
      return;
    }

    const payload: VaccinationWriteRequest = {
      employeeId,
      vaccineId: formState.vaccineId,
      vaccinationDate: formState.vaccinationDate,
      doseNumber,
      batchNumber: formState.batchNumber.trim() || null,
      expirationDate: formState.expirationDate,
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
      setDocumentError(updatedItem.id, null);
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

  async function uploadDocument(vaccinationId: string, file: File) {
    setDocumentBusyId(vaccinationId);
    setDocumentError(vaccinationId, null);
    let createdDocumentId: string | null = null;
    try {
      const metadataPayload: DocumentWriteRequest = {
        vaccinationId,
        fileName: file.name,
        filePath: `pending/${file.name}`,
        fileSize: file.size,
        mimeType: file.type || "application/octet-stream"
      };
      const created = await apiPost<DocumentWriteResponse>("/documents", metadataPayload);
      createdDocumentId = created.id;
      const formData = new FormData();
      formData.append("file", file);
      const updatedDocument = await apiPostForm<DocumentDto>(`/documents/${created.id}/content`, formData);
      setDocumentsByVaccination((current) => ({
        ...current,
        [vaccinationId]: [...(current[vaccinationId] ?? []).filter((item) => item.id !== updatedDocument.id), updatedDocument]
      }));
      window.setTimeout(() => {
        void refreshDocumentsForVaccination(vaccinationId).catch(() => undefined);
      }, 1500);
    } catch (e) {
      if (createdDocumentId) {
        try {
          await apiDelete(`/documents/${createdDocumentId}`);
        } catch {
          // Ignore cleanup failure and show the original upload error.
        }
      }
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("documents.unexpectedApiError");
      setDocumentError(vaccinationId, message);
    } finally {
      setDocumentBusyId(null);
    }
  }

  async function downloadDocument(fileDocument: DocumentDto) {
    setDocumentBusyId(fileDocument.id);
    setDocumentError(fileDocument.vaccinationId, null);
    try {
      const { blob, contentDisposition } = await apiGetBlob(`/documents/${fileDocument.id}/content`);
      const url = URL.createObjectURL(blob);
      const link = window.document.createElement("a");
      const fileNameMatch = contentDisposition?.match(/filename="?(.*?)"?$/i);
      link.href = url;
      link.download = fileNameMatch?.[1] || fileDocument.fileName;
      link.style.display = "none";
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 10000);
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("documents.unexpectedApiError");
      setDocumentError(fileDocument.vaccinationId, message);
    } finally {
      setDocumentBusyId(null);
    }
  }

  async function deleteDocument(vaccinationId: string, documentId: string) {
    setDocumentBusyId(documentId);
    setDocumentError(vaccinationId, null);
    try {
      try {
        await apiDelete(`/documents/${documentId}/content`);
      } catch (e) {
        if (!(e instanceof ApiHttpError) || e.status !== 404) {
          throw e;
        }
      }
      await apiDelete(`/documents/${documentId}`);
      setDocumentsByVaccination((current) => ({
        ...current,
        [vaccinationId]: (current[vaccinationId] ?? []).filter((item) => item.id !== documentId)
      }));
      await refreshDocumentsForVaccination(vaccinationId);
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("documents.unexpectedApiError");
      setDocumentError(vaccinationId, message);
    } finally {
      setDocumentBusyId(null);
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
                <article key={item.id} className={`history-item ${formState.id === item.id ? "is-editing" : ""}`}>
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
                    {documentErrors[item.id] ? <p className="warn">{documentErrors[item.id]}</p> : null}
                    {documents.length === 0 ? <p>{t("employeeVaccinations.noDocuments")}</p> : null}
                    {documents.length > 0 ? (
                      <ul className="doc-list">
                        {documents.map((document) => (
                          <li key={document.id}>
                            <div className="doc-meta">
                              <strong>{document.fileName}</strong>
                              <span className="muted">{formatBytes(document.fileSize, locale)}</span>
                            </div>
                            <div className="doc-actions">
                              <button
                                type="button"
                                className="button-secondary"
                                onClick={() => downloadDocument(document)}
                                disabled={documentBusyId === document.id}
                              >
                                {documentBusyId === document.id ? t("documents.downloading") : t("documents.download")}
                              </button>
                              {canManageVaccinations ? (
                                <button
                                  type="button"
                                  onClick={() => deleteDocument(item.id, document.id)}
                                  disabled={documentBusyId === document.id}
                                >
                                  {documentBusyId === document.id ? t("documents.deleting") : t("documents.delete")}
                                </button>
                              ) : null}
                            </div>
                          </li>
                        ))}
                      </ul>
                    ) : null}
                    {canManageVaccinations ? (
                      <div className="document-upload">
                        <label className="upload-button">
                          <input
                            type="file"
                            onChange={(e) => {
                              const file = e.target.files?.[0];
                              if (file) {
                                void uploadDocument(item.id, file);
                                e.target.value = "";
                              }
                            }}
                            disabled={documentBusyId === item.id}
                          />
                          <span>{documentBusyId === item.id ? t("documents.uploading") : t("documents.add")}</span>
                        </label>
                      </div>
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
        <article ref={editorCardRef} className={`card ${formState.id ? "editor-card is-editing" : "editor-card"}`}>
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
              <select
                ref={vaccineFieldRef}
                value={formState.vaccineId}
                onChange={(e) => setFormState((current) => ({ ...current, vaccineId: e.target.value }))}
              >
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
                required
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
