import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { apiGet } from "../../shared/api/client";
import { ApiHttpError } from "../../shared/api/types";
import { AuthSession, normalizeRoles } from "./session";

type AuthContextValue = {
  session: AuthSession | null;
  login: (session: AuthSession) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function restoreSession() {
      try {
        const current = await apiGet<{ userId: string; roles: unknown }>("/auth/me", {
          suppressAuthEvents: true
        });
        if (cancelled) return;
        setSession({
          userId: current.userId,
          roles: normalizeRoles(current.roles)
        });
      } catch (error) {
        if (cancelled) return;
        if (!(error instanceof ApiHttpError && error.status === 401)) {
          setSession(null);
        } else {
          setSession(null);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    restoreSession();
    return () => {
      cancelled = true;
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      login(next) {
        setSession(next);
        setIsLoading(false);
      },
      logout() {
        setSession(null);
        setIsLoading(false);
      },
      isAuthenticated: Boolean(session),
      isLoading
    }),
    [isLoading, session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
