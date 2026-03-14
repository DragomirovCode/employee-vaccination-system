import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { apiGet, apiPost, apiPut } from "../shared/api/client";
import { ApiHttpError, DepartmentDto, EmployeeDto, EmployeeWriteRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type EmployeeFormState = {
  userId: string;
  departmentId: string;
  firstName: string;
  lastName: string;
  middleName: string;
  birthDate: string;
  position: string;
  hireDate: string;
};

const EMPTY_FORM: EmployeeFormState = {
  userId: "",
  departmentId: "",
  firstName: "",
  lastName: "",
  middleName: "",
  birthDate: "",
  position: "",
  hireDate: ""
};

function toFormState(employee: EmployeeDto): EmployeeFormState {
  return {
    userId: employee.userId ?? "",
    departmentId: employee.departmentId,
    firstName: employee.firstName,
    lastName: employee.lastName,
    middleName: employee.middleName ?? "",
    birthDate: employee.birthDate ?? "",
    position: employee.position ?? "",
    hireDate: employee.hireDate ?? ""
  };
}

export function EmployeeEditorPage() {
  const { employeeId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [formState, setFormState] = useState<EmployeeFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const isEditMode = Boolean(employeeId);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const departmentsResponse = await apiGet<DepartmentDto[]>("/departments");
        const employeeResponse = employeeId ? await apiGet<EmployeeDto>(`/employees/${employeeId}`) : null;
        if (cancelled) return;
        setDepartments(departmentsResponse);
        setFormState(employeeResponse ? toFormState(employeeResponse) : EMPTY_FORM);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("employees.unexpectedApiError");
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
  }, [employeeId, t]);

  async function submitForm(e: FormEvent) {
    e.preventDefault();
    if (!formState.departmentId || !formState.firstName.trim() || !formState.lastName.trim()) {
      setError(t("employees.validation"));
      return;
    }

    const payload: EmployeeWriteRequest = {
      userId: formState.userId.trim() || null,
      departmentId: formState.departmentId,
      firstName: formState.firstName.trim(),
      lastName: formState.lastName.trim(),
      middleName: formState.middleName.trim() || null,
      birthDate: formState.birthDate || null,
      position: formState.position.trim() || null,
      hireDate: formState.hireDate || null
    };

    setSubmitting(true);
    setError(null);
    try {
      if (isEditMode && employeeId) {
        await apiPut(`/employees/${employeeId}`, payload);
      } else {
        await apiPost("/employees", payload);
      }
      navigate("/employees", { replace: true });
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("employees.unexpectedApiError");
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-actions">
        <Link to="/employees" className="inline-link">
          {t("employees.backToList")}
        </Link>
      </div>

      <article className="card">
        <div className="page-head">
          <div>
            <h2>{isEditMode ? t("employees.editTitle") : t("employees.createTitle")}</h2>
            <p className="muted">{t("employees.editorDescription")}</p>
          </div>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading ? (
          <form className="editor-form" onSubmit={submitForm}>
            <label>
              {t("employees.lastName")}
              <input value={formState.lastName} onChange={(e) => setFormState((current) => ({ ...current, lastName: e.target.value }))} />
            </label>
            <label>
              {t("employees.firstName")}
              <input value={formState.firstName} onChange={(e) => setFormState((current) => ({ ...current, firstName: e.target.value }))} />
            </label>
            <label>
              {t("employees.middleName")}
              <input value={formState.middleName} onChange={(e) => setFormState((current) => ({ ...current, middleName: e.target.value }))} />
            </label>
            <label>
              {t("employees.department")}
              <select
                value={formState.departmentId}
                onChange={(e) => setFormState((current) => ({ ...current, departmentId: e.target.value }))}
              >
                <option value="">{t("employees.allDepartments")}</option>
                {departments.map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {t("employees.birthDate")}
              <input type="date" value={formState.birthDate} onChange={(e) => setFormState((current) => ({ ...current, birthDate: e.target.value }))} />
            </label>
            <label>
              {t("employees.hireDate")}
              <input type="date" value={formState.hireDate} onChange={(e) => setFormState((current) => ({ ...current, hireDate: e.target.value }))} />
            </label>
            <label>
              {t("employees.position")}
              <input value={formState.position} onChange={(e) => setFormState((current) => ({ ...current, position: e.target.value }))} />
            </label>
            <label>
              {t("employees.userId")}
              <input
                value={formState.userId}
                placeholder={t("employees.userIdPlaceholder")}
                onChange={(e) => setFormState((current) => ({ ...current, userId: e.target.value }))}
              />
            </label>

            <div className="editor-actions editor-form-span">
              <Link to="/employees">
                <button type="button" className="button-secondary">{t("employees.cancel")}</button>
              </Link>
              <button type="submit" disabled={submitting}>
                {submitting ? t("employees.saving") : t("employees.save")}
              </button>
            </div>
          </form>
        ) : null}
      </article>
    </section>
  );
}
