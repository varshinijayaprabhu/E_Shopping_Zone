import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LoadingSpinner } from './LoadingSpinner';

interface RoleRouteProps {
  children: React.ReactElement;
  requiredRole: 'ROLE_CUSTOMER' | 'ROLE_MERCHANT' | 'ROLE_ADMIN';
}

export const RoleRoute: React.FC<RoleRouteProps> = ({ children, requiredRole }) => {
  const { user, isAuthenticated, isLoading, isAdmin, isMerchant, isCustomer } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Verifying permissions..." />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  const hasRole =
    (requiredRole === 'ROLE_ADMIN' && isAdmin) ||
    (requiredRole === 'ROLE_MERCHANT' && isMerchant) ||
    (requiredRole === 'ROLE_CUSTOMER' && isCustomer) ||
    user?.roles?.includes(requiredRole);

  if (!hasRole) {
    return (
      <div className="max-w-2xl mx-auto my-16 p-8 text-center bg-white rounded-2xl shadow-sm border border-slate-200">
        <h2 className="text-2xl font-bold text-slate-800">Access Denied</h2>
        <p className="mt-2 text-slate-600">
          You do not have the required permissions ({requiredRole}) to access this page.
        </p>
        <div className="mt-6">
          <a
            href="/"
            className="inline-block px-5 py-2.5 bg-indigo-600 text-white font-semibold rounded-lg hover:bg-indigo-700 transition"
          >
            Return to Store
          </a>
        </div>
      </div>
    );
  }

  return children;
};

