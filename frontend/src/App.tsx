import { useEffect } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { AppLayout } from "./layout/AppLayout";
import { CoverageReportPage } from "./pages/CoverageReportPage";
import { AdminUserEditorPage } from "./pages/AdminUserEditorPage";
import { AdminUsersPage } from "./pages/AdminUsersPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DepartmentEditorPage } from "./pages/DepartmentEditorPage";
import { DepartmentsPage } from "./pages/DepartmentsPage";
import { DiseaseEditorPage } from "./pages/DiseaseEditorPage";
import { DiseasesPage } from "./pages/DiseasesPage";
import { EmployeesPage } from "./pages/EmployeesPage";
import { EmployeeVaccinationsPage } from "./pages/EmployeeVaccinationsPage";
import { EmployeeEditorPage } from "./pages/EmployeeEditorPage";
import { ForbiddenPage } from "./pages/ForbiddenPage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { RevaccinationDuePage } from "./pages/RevaccinationDuePage";
import { VaccinationRegistryPage } from "./pages/VaccinationRegistryPage";
import { VaccineEditorPage } from "./pages/VaccineEditorPage";
import { VaccinesPage } from "./pages/VaccinesPage";
import { RequireAuth } from "./features/auth/RequireAuth";
import { RequireRole } from "./features/auth/RequireRole";
import { useAuth } from "./features/auth/AuthContext";

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
              <RequireRole allowedRoles={["HR", "MEDICAL", "ADMIN"]}>
                <EmployeesPage />
              </RequireRole>
            }
          />
          <Route
            path="departments"
            element={
              <RequireRole allowedRoles={["HR", "ADMIN"]}>
                <DepartmentsPage />
              </RequireRole>
            }
          />
          <Route
            path="departments/new"
            element={
              <RequireRole allowedRoles={["HR", "ADMIN"]}>
                <DepartmentEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="departments/:departmentId/edit"
            element={
              <RequireRole allowedRoles={["HR", "ADMIN"]}>
                <DepartmentEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="employees/new"
            element={
              <RequireRole allowedRoles={["HR", "ADMIN"]}>
                <EmployeeEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="employees/:employeeId/edit"
            element={
              <RequireRole allowedRoles={["HR", "ADMIN"]}>
                <EmployeeEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="vaccinations"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <VaccinationRegistryPage />
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
          <Route
            path="vaccines/new"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <VaccineEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="vaccines/:vaccineId/edit"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <VaccineEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="diseases"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <DiseasesPage />
              </RequireRole>
            }
          />
          <Route
            path="diseases/new"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <DiseaseEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="diseases/:diseaseId/edit"
            element={
              <RequireRole allowedRoles={["MEDICAL", "ADMIN"]}>
                <DiseaseEditorPage />
              </RequireRole>
            }
          />
          <Route path="employees/:employeeId/vaccinations" element={<EmployeeVaccinationsPage />} />
          <Route path="notifications" element={<DashboardPage />} />
          <Route
            path="admin-sandbox"
            element={
              <RequireRole allowedRoles={["ADMIN"]}>
                <Navigate to="/admin/users" replace />
              </RequireRole>
            }
          />
          <Route
            path="admin/users"
            element={
              <RequireRole allowedRoles={["ADMIN"]}>
                <AdminUsersPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/users/new"
            element={
              <RequireRole allowedRoles={["ADMIN"]}>
                <AdminUserEditorPage />
              </RequireRole>
            }
          />
          <Route
            path="admin/users/:userId/edit"
            element={
              <RequireRole allowedRoles={["ADMIN"]}>
                <AdminUserEditorPage />
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
