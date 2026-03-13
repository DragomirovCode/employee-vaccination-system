import { useEffect } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { AppLayout } from "./layout/AppLayout";
import { CoverageReportPage } from "./pages/CoverageReportPage";
import { DashboardPage } from "./pages/DashboardPage";
import { EmployeesPage } from "./pages/EmployeesPage";
import { EmployeeVaccinationsPage } from "./pages/EmployeeVaccinationsPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { RevaccinationDuePage } from "./pages/RevaccinationDuePage";
import { VaccinesPage } from "./pages/VaccinesPage";
import { RequireAuth } from "./features/auth/RequireAuth";
import { RequireRole } from "./features/auth/RequireRole";
import { useAuth } from "./features/auth/AuthContext";
import { useI18n } from "./shared/i18n/I18nContext";

function EventBridge() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  useEffect(() => {
    const onUnauthorized = () => {
      logout();
      navigate("/login", { replace: true, state: { reason: "expired" } });
    };
    const onForbidden = () => navigate("/forbidden");

    window.addEventListener("api:unauthorized", onUnauthorized);
    window.addEventListener("api:forbidden", onForbidden);
    return () => {
      window.removeEventListener("api:unauthorized", onUnauthorized);
      window.removeEventListener("api:forbidden", onForbidden);
    };
  }, [logout, navigate]);

  return null;
}

export function App() {
  const { t } = useI18n();

  return (
    <>
      <EventBridge />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forbidden" element={<ForbiddenPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route index element={<RevaccinationDuePage />} />
          <Route
            path="reports/coverage"
            element={
              <RequireRole allowedRoles={["HR", "MEDICAL", "ADMIN"]}>
                <CoverageReportPage />
              </RequireRole>
            }
          />
          <Route
            path="employees"
            element={
              <RequireRole allowedRoles={["HR", "ADMIN"]}>
                <EmployeesPage />
              </RequireRole>
            }
          />
          <Route
            path="vaccines"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <VaccinesPage />
              </RequireRole>
            }
          />
          <Route path="employees/:employeeId/vaccinations" element={<EmployeeVaccinationsPage />} />
          <Route path="notifications" element={<DashboardPage />} />
          <Route
            path="admin-sandbox"
            element={
              <RequireRole allowedRoles={["ADMIN"]}>
                <section className="card">
                  <h2>{t("admin.title")}</h2>
                  <p>{t("admin.description")}</p>
                </section>
              </RequireRole>
            }
          />
        </Route>
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </>
  );
}
