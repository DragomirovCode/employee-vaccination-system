import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { apiGet, apiPost, apiPut } from "../shared/api/client";
import { ApiHttpError, DepartmentDto, DepartmentWriteRequest } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

type DepartmentFormState = {
  name: string;
  parentId: string;
};

const EMPTY_FORM: DepartmentFormState = {
  name: "",
  parentId: ""
};

function toFormState(department: DepartmentDto): DepartmentFormState {
  return {
    name: department.name,
    parentId: department.parentId ?? ""
  };
}

export function DepartmentEditorPage() {
  const { departmentId } = useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [formState, setFormState] = useState<DepartmentFormState>(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const isEditMode = Boolean(departmentId);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const departmentsResponse = await apiGet<DepartmentDto[]>("/departments");
        const departmentResponse = departmentId ? await apiGet<DepartmentDto>(`/departments/${departmentId}`) : null;
        if (cancelled) return;
        setDepartments(departmentsResponse);
        setFormState(departmentResponse ? toFormState(departmentResponse) : EMPTY_FORM);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("departments.unexpectedApiError");
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
  }, [departmentId, t]);

  const availableParents = useMemo(
    () => departments.filter((department) => department.id !== departmentId).sort((left, right) => left.name.localeCompare(right.name)),
    [departmentId, departments]
  );

  async function submitForm(e: FormEvent) {
    e.preventDefault();
    if (!formState.name.trim()) {
      setError(t("departments.validation"));
      return;
    }

    const payload: DepartmentWriteRequest = {
      name: formState.name.trim(),
      parentId: formState.parentId || null
    };

    setSubmitting(true);
    setError(null);
    try {
      if (isEditMode && departmentId) {
        await apiPut(`/departments/${departmentId}`, payload);
      } else {
        await apiPost("/departments", payload);
      }
      navigate("/departments", { replace: true });
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("departments.unexpectedApiError");
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="stack-lg">
      <div className="page-actions">
        <Link to="/departments" className="inline-link">
          {t("departments.backToList")}
        </Link>
      </div>

      <article className="card">
        <div className="page-head">
          <div>
            <h2>{isEditMode ? t("departments.editTitle") : t("departments.createTitle")}</h2>
            <p className="muted">{t("departments.editorDescription")}</p>
          </div>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading ? (
          <form className="editor-form" onSubmit={submitForm}>
            <label>
              {t("departments.name")}
              <input value={formState.name} onChange={(e) => setFormState((current) => ({ ...current, name: e.target.value }))} />
            </label>
            <label>
              {t("departments.parent")}
              <select value={formState.parentId} onChange={(e) => setFormState((current) => ({ ...current, parentId: e.target.value }))}>
                <option value="">{t("departments.noParent")}</option>
                {availableParents.map((department) => (
                  <option key={department.id} value={department.id}>
                    {department.name}
                  </option>
                ))}
              </select>
            </label>

            <div className="editor-actions editor-form-span">
              <Link to="/departments">
                <button type="button" className="button-secondary">{t("departments.cancel")}</button>
              </Link>
              <button type="submit" disabled={submitting}>
                {submitting ? t("departments.saving") : t("departments.save")}
              </button>
            </div>
          </form>
        ) : null}
      </article>
    </section>
  );
}
