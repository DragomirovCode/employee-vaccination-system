import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiDelete, apiGet } from "../shared/api/client";
import { ApiHttpError, DepartmentDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

export function DepartmentsPage() {
  const { session } = useAuth();
  const { t } = useI18n();
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const response = await apiGet<DepartmentDto[]>("/departments");
        if (!cancelled) {
          setDepartments(response);
        }
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
  }, [t]);

  const canManageDepartments = Boolean(session?.roles.some((role) => role === "HR" || role === "ADMIN"));

  const departmentMap = useMemo(
    () => Object.fromEntries(departments.map((department) => [department.id, department.name])),
    [departments]
  );

  const sortedDepartments = useMemo(
    () => [...departments].sort((left, right) => left.name.localeCompare(right.name)),
    [departments]
  );

  async function deleteDepartment(departmentId: string) {
    if (!window.confirm(t("departments.deleteConfirm"))) {
      return;
    }

    setDeletingId(departmentId);
    setError(null);
    try {
      await apiDelete(`/departments/${departmentId}`);
      setDepartments((current) => current.filter((department) => department.id !== departmentId));
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("departments.deleteError");
      setError(message);
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section className="stack-lg">
      <article className="card">
        <div className="page-head">
          <div>
            <h2>{t("departments.title")}</h2>
            <p className="muted">{t("departments.description")}</p>
          </div>
          <div className="toolbar-actions">
            <span className="summary-pill">
              {t("departments.total")}: {sortedDepartments.length}
            </span>
            {canManageDepartments ? (
              <Link to="/departments/new">
                <button type="button" className="button-secondary">{t("departments.create")}</button>
              </Link>
            ) : null}
          </div>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && sortedDepartments.length === 0 ? (
          <div className="empty-state">
            <h3>{t("departments.emptyTitle")}</h3>
            <p>{t("departments.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && sortedDepartments.length > 0 ? (
          <div className="employee-list">
            {sortedDepartments.map((department) => (
              <article key={department.id} className="employee-item">
                <div className="employee-head">
                  <div>
                    <h3>{department.name}</h3>
                    <p className="muted">
                      {t("departments.parent")}:{" "}
                      {department.parentId ? departmentMap[department.parentId] ?? t("employees.notSpecified") : t("departments.noParent")}
                    </p>
                  </div>
                </div>

                {canManageDepartments ? (
                  <div className="history-actions">
                    <Link to={`/departments/${department.id}/edit`}>
                      <button type="button" className="button-secondary">{t("departments.edit")}</button>
                    </Link>
                    <button type="button" onClick={() => void deleteDepartment(department.id)} disabled={deletingId === department.id}>
                      {deletingId === department.id ? t("departments.deleting") : t("departments.delete")}
                    </button>
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        ) : null}
      </article>
    </section>
  );
}
