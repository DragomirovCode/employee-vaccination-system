import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiDelete, apiGet } from "../shared/api/client";
import { ApiHttpError, DepartmentDto, EmployeeDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";

function formatEmployeeName(employee: EmployeeDto): string {
  return [employee.lastName, employee.firstName, employee.middleName].filter(Boolean).join(" ");
}

export function EmployeesPage() {
  const { session } = useAuth();
  const { locale, t } = useI18n();
  const [employees, setEmployees] = useState<EmployeeDto[]>([]);
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [departmentFilter, setDepartmentFilter] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [employeesResponse, departmentsResponse] = await Promise.all([
          apiGet<EmployeeDto[]>("/employees"),
          apiGet<DepartmentDto[]>("/departments")
        ]);

        if (cancelled) return;
        setEmployees(employeesResponse);
        setDepartments(departmentsResponse);
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("employees.unexpectedApiError");
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [t]);

  const canManageEmployees = Boolean(session?.roles.some((role) => role === "HR" || role === "ADMIN"));

  const departmentMap = useMemo(
    () => Object.fromEntries(departments.map((department) => [department.id, department.name])),
    [departments]
  );

  const filteredEmployees = useMemo(
    () =>
      employees.filter((employee) => {
        if (!departmentFilter) return true;
        return employee.departmentId === departmentFilter;
      }),
    [departmentFilter, employees]
  );

  const formatter = new Intl.DateTimeFormat(locale === "ru" ? "ru-RU" : "en-US");

  async function deleteEmployee(employeeId: string) {
    if (!window.confirm(t("employees.deleteConfirm"))) {
      return;
    }

    setDeletingId(employeeId);
    setError(null);
    try {
      await apiDelete(`/employees/${employeeId}`);
      setEmployees((current) => current.filter((employee) => employee.id !== employeeId));
    } catch (e) {
      const message = e instanceof ApiHttpError ? e.payload?.message ?? e.message : t("employees.deleteError");
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
            <h2>{t("employees.title")}</h2>
            <p className="muted">{t("employees.description")}</p>
          </div>
          <div className="toolbar-actions">
            <span className="summary-pill">
              {t("employees.total")}: {filteredEmployees.length}
            </span>
            {canManageEmployees ? (
              <Link to="/employees/new">
                <button type="button" className="button-secondary">{t("employees.create")}</button>
              </Link>
            ) : null}
          </div>
        </div>

        <div className="toolbar">
          <label className="toolbar-field">
            <span>{t("employees.department")}</span>
            <select value={departmentFilter} onChange={(e) => setDepartmentFilter(e.target.value)}>
              <option value="">{t("employees.allDepartments")}</option>
              {departments.map((department) => (
                <option key={department.id} value={department.id}>
                  {department.name}
                </option>
              ))}
            </select>
          </label>
        </div>

        {loading ? <p>{t("common.loading")}</p> : null}
        {error ? <p className="warn">{error}</p> : null}

        {!loading && !error && filteredEmployees.length === 0 ? (
          <div className="empty-state">
            <h3>{t("employees.emptyTitle")}</h3>
            <p>{t("employees.emptyDescription")}</p>
          </div>
        ) : null}

        {!loading && !error && filteredEmployees.length > 0 ? (
          <div className="employee-list">
            {filteredEmployees.map((employee) => (
              <article key={employee.id} className="employee-item">
                <div className="employee-head">
                  <div>
                    <h3>
                      <Link to={`/employees/${employee.id}/vaccinations`} className="inline-link">
                        {formatEmployeeName(employee)}
                      </Link>
                    </h3>
                    <p className="muted">{departmentMap[employee.departmentId] ?? t("employees.departmentUnknown")}</p>
                  </div>
                  {employee.userId ? <span className="status-pill is-planned">{t("employees.accountLinked")}</span> : null}
                </div>

                <dl className="meta-grid">
                  <div>
                    <dt>{t("employees.position")}</dt>
                    <dd>{employee.position ?? t("employees.notSpecified")}</dd>
                  </div>
                  <div>
                    <dt>{t("employees.hireDate")}</dt>
                    <dd>{employee.hireDate ? formatter.format(new Date(employee.hireDate)) : t("employees.notSpecified")}</dd>
                  </div>
                </dl>

                {canManageEmployees ? (
                  <div className="history-actions">
                    <Link to={`/employees/${employee.id}/edit`}>
                      <button type="button" className="button-secondary">{t("employees.edit")}</button>
                    </Link>
                    <button type="button" onClick={() => void deleteEmployee(employee.id)} disabled={deletingId === employee.id}>
                      {deletingId === employee.id ? t("employees.deleting") : t("employees.delete")}
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
