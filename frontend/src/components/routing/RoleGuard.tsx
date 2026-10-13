import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAppSelector } from '../../app/hooks';

interface RoleGuardProps {
  allowedRoles: string[];
}

export const RoleGuard: React.FC<RoleGuardProps> = ({ allowedRoles }) => {
  const { user } = useAppSelector((state) => state.auth);

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const hasPermission = user.roles.some((role) => allowedRoles.includes(role));

  if (!hasPermission) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <Outlet />;
};
