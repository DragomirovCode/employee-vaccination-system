import { ReactElement } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { AppRole } from "./session";

export function RequireRole({
  allowedRoles,
  children
}: {
  allowedRoles: AppRole[];
  children: ReactElement;
}) {
  const { session, isLoading } = useAuth();

  if (isLoading) {
    return children;
  }

  const roles = session?.roles ?? [];
  const isAllowed = allowedRoles.some((role) => roles.includes(role));

  if (!isAllowed) {
    return <Navigate to="/forbidden" replace />;
  }

  return children;
}
