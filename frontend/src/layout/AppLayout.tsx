import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";

export function AppLayout() {
  const { session, logout } = useAuth();
  const rolesLabel = session?.roles.length ? session.roles.join(", ") : "No roles";

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <h1>Employee Vaccination System</h1>
          <p className="caption">Frontend Core</p>
        </div>
        <nav className="nav">
          <Link to="/">Dashboard</Link>
          <Link to="/admin-sandbox">Admin Sandbox</Link>
        </nav>
        <div className="session">
          <span>{rolesLabel}</span>
          <button onClick={logout}>Sign out</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
