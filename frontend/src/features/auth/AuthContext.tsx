import { createContext, useContext, useMemo, useState } from "react";
import { AuthSession, clearSession, readSession, writeSession } from "./session";

type AuthContextValue = {
  session: AuthSession | null;
  login: (session: AuthSession) => void;
  logout: () => void;
  isAuthenticated: boolean;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => readSession());

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      login(next) {
        setSession(next);
        writeSession(next);
      },
      logout() {
        setSession(null);
        clearSession();
      },
      isAuthenticated: Boolean(session?.token)
    }),
    [session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
