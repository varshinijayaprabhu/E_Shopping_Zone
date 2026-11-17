import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ProtectedRoute } from '../components/common/ProtectedRoute';
import { RoleRoute } from '../components/common/RoleRoute';

// Mock AuthContext
let mockAuthState = {
  user: null as any,
  isAuthenticated: false,
  isAdmin: false,
  isMerchant: false,
  isCustomer: false,
  isLoading: false,
};

vi.mock('../context/AuthContext', () => ({
  useAuth: () => mockAuthState,
}));

describe('Route Guards', () => {
  describe('ProtectedRoute', () => {
    it('redirects unauthenticated users to /login', () => {
      mockAuthState = {
        user: null,
        isAuthenticated: false,
        isMerchant: false,
        isCustomer: false,
        isLoading: false,
      };

      render(
        <MemoryRouter initialEntries={['/protected']}>
          <Routes>
            <Route
              path="/protected"
              element={
                <ProtectedRoute>
                  <div>Secret Content</div>
                </ProtectedRoute>
              }
            />
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByText('Secret Content')).not.toBeInTheDocument();
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });

    it('renders protected child when authenticated', () => {
      mockAuthState = {
        user: { id: 1, email: 'alice@eshopping.com', roles: ['ROLE_CUSTOMER'] },
        isAuthenticated: true,
        isMerchant: false,
        isCustomer: true,
        isLoading: false,
      };

      render(
        <MemoryRouter initialEntries={['/protected']}>
          <Routes>
            <Route
              path="/protected"
              element={
                <ProtectedRoute>
                  <div>Secret Content</div>
                </ProtectedRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByText('Secret Content')).toBeInTheDocument();
    });
  });

  describe('RoleRoute', () => {
    it('shows Access Denied when user lacks required role', () => {
      mockAuthState = {
        user: { id: 1, email: 'alice@eshopping.com', roles: ['ROLE_CUSTOMER'] },
        isAuthenticated: true,
        isMerchant: false,
        isCustomer: true,
        isLoading: false,
      };

      render(
        <MemoryRouter initialEntries={['/merchant-only']}>
          <Routes>
            <Route
              path="/merchant-only"
              element={
                <RoleRoute requiredRole="ROLE_MERCHANT">
                  <div>Merchant Portal</div>
                </RoleRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByText('Merchant Portal')).not.toBeInTheDocument();
      expect(screen.getByText('Access Denied')).toBeInTheDocument();
    });

    it('allows access when user holds required role', () => {
      mockAuthState = {
        user: { id: 2, email: 'bob@eshopping.com', roles: ['ROLE_MERCHANT'] },
        isAuthenticated: true,
        isMerchant: true,
        isCustomer: false,
        isLoading: false,
      };

      render(
        <MemoryRouter initialEntries={['/merchant-only']}>
          <Routes>
            <Route
              path="/merchant-only"
              element={
                <RoleRoute requiredRole="ROLE_MERCHANT">
                  <div>Merchant Portal</div>
                </RoleRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByText('Merchant Portal')).toBeInTheDocument();
    });

    it('shows Access Denied when non-admin accesses ROLE_ADMIN route', () => {
      mockAuthState = {
        user: { id: 1, email: 'alice@eshopping.com', roles: ['ROLE_CUSTOMER'] },
        isAuthenticated: true,
        isAdmin: false,
        isMerchant: false,
        isCustomer: true,
        isLoading: false,
      };

      render(
        <MemoryRouter initialEntries={['/admin-only']}>
          <Routes>
            <Route
              path="/admin-only"
              element={
                <RoleRoute requiredRole="ROLE_ADMIN">
                  <div>Admin Governance Portal</div>
                </RoleRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByText('Admin Governance Portal')).not.toBeInTheDocument();
      expect(screen.getByText('Access Denied')).toBeInTheDocument();
    });

    it('allows access to admin portal when user holds ROLE_ADMIN', () => {
      mockAuthState = {
        user: { id: 4, email: 'admin@eshopping.com', roles: ['ROLE_ADMIN'] },
        isAuthenticated: true,
        isAdmin: true,
        isMerchant: false,
        isCustomer: false,
        isLoading: false,
      };

      render(
        <MemoryRouter initialEntries={['/admin-only']}>
          <Routes>
            <Route
              path="/admin-only"
              element={
                <RoleRoute requiredRole="ROLE_ADMIN">
                  <div>Admin Governance Portal</div>
                </RoleRoute>
              }
            />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByText('Admin Governance Portal')).toBeInTheDocument();
    });
  });
});

