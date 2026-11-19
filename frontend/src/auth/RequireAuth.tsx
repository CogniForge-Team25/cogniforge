import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '../api/client';
import { useAuth } from './AuthContext';

interface RequireAuthProps {
  allowedRoles?: Role[];
}

export default function RequireAuth({ allowedRoles }: RequireAuthProps) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (allowedRoles && (role === null || !allowedRoles.includes(role))) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
