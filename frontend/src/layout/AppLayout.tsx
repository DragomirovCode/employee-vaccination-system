import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { useI18n } from "../shared/i18n/I18nContext";
import { LanguageSwitch } from "../shared/i18n/LanguageSwitch";

export function AppLayout() {
  const { session, logout } = useAuth();
  const { t } = useI18n();
  const rolesLabel = session?.roles.length ? session.roles.join(", ") : t("layout.noRoles");

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <h1>{t("layout.title")}</h1>
          <p className="caption">{t("layout.subtitle")}</p>
        </div>
        <nav className="nav">
          <Link to="/">{t("layout.dashboard")}</Link>
          <Link to="/admin-sandbox">{t("layout.adminSandbox")}</Link>
        </nav>
        <div className="session">
          <span>{rolesLabel}</span>
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
