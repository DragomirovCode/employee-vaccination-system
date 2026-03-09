import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { useI18n } from "../shared/i18n/I18nContext";
import { LanguageSwitch } from "../shared/i18n/LanguageSwitch";

export function AppLayout() {
  const { session, logout } = useAuth();
  const { t } = useI18n();
  const canOpenAdminSandbox = Boolean(session?.roles.includes("ADMIN"));

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <h1>{t("layout.title")}</h1>
        </div>
        <nav className="nav">
          <Link to="/">{t("layout.dashboard")}</Link>
          {canOpenAdminSandbox ? <Link to="/admin-sandbox">{t("layout.adminSandbox")}</Link> : null}
        </nav>
        <div className="session">
          <span>{session?.token ?? ""}</span>
          <LanguageSwitch />
          <button onClick={logout}>{t("common.signOut")}</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
