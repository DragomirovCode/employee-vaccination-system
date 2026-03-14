import { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { apiGet } from "../shared/api/client";
import { ApiHttpError, EmployeeDto } from "../shared/api/types";
import { useI18n } from "../shared/i18n/I18nContext";
import { LanguageSwitch } from "../shared/i18n/LanguageSwitch";

function formatEmployeeName(employee: EmployeeDto): string {
  return [employee.lastName, employee.firstName, employee.middleName].filter(Boolean).join(" ");
}

export function AppLayout() {
  const { session, logout } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const canOpenAdminSandbox = Boolean(session?.roles.includes("ADMIN"));
  const canOpenCoverage = Boolean(session?.roles.some((role) => role === "HR" || role === "MEDICAL" || role === "ADMIN"));
  const canOpenEmployees = Boolean(session?.roles.some((role) => role === "HR" || role === "MEDICAL" || role === "ADMIN"));
  const canOpenVaccines = Boolean(session?.roles.some((role) => role === "MEDICAL" || role === "ADMIN"));
  const canOpenDiseases = Boolean(session?.roles.some((role) => role === "MEDICAL" || role === "ADMIN"));
  const navClassName = ({ isActive }: { isActive: boolean }) => (isActive ? "active" : undefined);
  const [displayName, setDisplayName] = useState<string>("");

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  useEffect(() => {
    if (!session) {
      setDisplayName("");
      return;
    }
    const currentSession = session;

    let cancelled = false;

    async function loadProfileName() {
      try {
        const employees = await apiGet<EmployeeDto[]>("/employees");
        if (cancelled) return;
        const currentEmployee = employees.find((item) => item.userId === currentSession.userId);
        setDisplayName(currentEmployee ? formatEmployeeName(currentEmployee) : t("layout.user"));
      } catch (error) {
        if (cancelled) return;
        if (error instanceof ApiHttpError) {
          setDisplayName(t("layout.user"));
          return;
        }
        setDisplayName(t("layout.user"));
      }
    }

    loadProfileName();
    return () => {
      cancelled = true;
    };
  }, [session, t]);

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <h1>{t("layout.title")}</h1>
        </div>
        <nav className="nav">
          <NavLink to="/" end className={navClassName}>
            {t("layout.revaccination")}
          </NavLink>
          {canOpenCoverage ? (
            <NavLink to="/reports/coverage" className={navClassName}>
              {t("layout.coverage")}
            </NavLink>
          ) : null}
          {canOpenEmployees ? (
            <NavLink to="/employees" className={navClassName}>
              {t("layout.employees")}
            </NavLink>
          ) : null}
          {canOpenVaccines ? (
            <NavLink to="/vaccines" className={navClassName}>
              {t("layout.vaccines")}
            </NavLink>
          ) : null}
          {canOpenDiseases ? (
            <NavLink to="/diseases" className={navClassName}>
              {t("layout.diseases")}
            </NavLink>
          ) : null}
          <NavLink to="/notifications" className={navClassName}>
            {t("layout.notifications")}
          </NavLink>
          {canOpenAdminSandbox ? (
            <NavLink to="/admin-sandbox" className={navClassName}>
              {t("layout.adminSandbox")}
            </NavLink>
          ) : null}
        </nav>
        <div className="session">
          <span>{displayName}</span>
          <LanguageSwitch />
          <button onClick={handleLogout}>{t("common.signOut")}</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
