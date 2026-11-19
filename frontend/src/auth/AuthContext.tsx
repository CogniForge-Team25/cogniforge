import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { apiFetch, setAccessToken, setOnUnauthorized } from '../api/client';
import type { LoginResponse, Role } from '../api/client';

interface AuthContextValue {
  role: Role | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [role, setRole] = useState<Role | null>(null);

  const logout = useCallback(() => {
    setAccessToken(null);
    setRole(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const result = await apiFetch<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    setAccessToken(result.accessToken);
    setRole(result.role);
  }, []);

  // If the server rejects our token (for example it expired), sign the user out.
  useEffect(() => {
    setOnUnauthorized(logout);
    return () => setOnUnauthorized(null);
  }, [logout]);

  const value = useMemo<AuthContextValue>(
    () => ({ role, isAuthenticated: role !== null, login, logout }),
    [role, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return context;
}
