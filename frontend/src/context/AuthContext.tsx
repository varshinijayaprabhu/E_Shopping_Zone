import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { User, AuthResponse } from '../types';
import { authApi } from '../api/authApi';
import { setAccessToken, setAuthFailureHandler } from '../api/axiosClient';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isMerchant: boolean;
  isCustomer: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<AuthResponse>;
  registerCustomer: (payload: { fullName: string; email: string; password: string; phone?: string }) => Promise<User>;
  registerMerchant: (payload: { fullName: string; email: string; password: string; phone?: string; inviteCode: string }) => Promise<User>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  setAuthSession: (user: User, accessToken: string) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    try {
      const stored = localStorage.getItem('eshopping_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const handleLogoutState = useCallback(() => {
    setUser(null);
    setAccessToken(null);
    try {
      localStorage.removeItem('eshopping_user');
    } catch {}
  }, []);

  const setAuthSession = useCallback((userData: User, token: string) => {
    setAccessToken(token);
    setUser(userData);
    try {
      localStorage.setItem('eshopping_user', JSON.stringify(userData));
    } catch {}
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      // Check for active HttpOnly session cookie
      const res = await authApi.refreshToken();
      if (res.success && res.data) {
        setAuthSession(res.data.user, res.data.accessToken);
      } else {
        if (!localStorage.getItem('eshopping_user')) {
          handleLogoutState();
        }
      }
    } catch {
      if (!localStorage.getItem('eshopping_user')) {
        handleLogoutState();
      }
    } finally {
      setIsLoading(false);
    }
  }, [handleLogoutState, setAuthSession]);

  useEffect(() => {
    setAuthFailureHandler(handleLogoutState);
    refreshUser();
  }, [handleLogoutState, refreshUser]);

  const login = async (email: string, password: string): Promise<AuthResponse> => {
    setIsLoading(true);
    try {
      const res = await authApi.login(email, password);
      if (res.success && res.data) {
        setAuthSession(res.data.user, res.data.accessToken);
        return res.data;
      }
      throw new Error(res.message || 'Login failed');
    } finally {
      setIsLoading(false);
    }
  };

  const registerCustomer = async (payload: {
    fullName: string;
    email: string;
    password: string;
    phone?: string;
  }): Promise<User> => {
    setIsLoading(true);
    try {
      const res = await authApi.registerCustomer(payload);
      if (res.success && res.data) {
        const authData = res.data as any;
        const userData = authData.user || authData;
        if (authData.accessToken && authData.user) {
          setAuthSession(authData.user, authData.accessToken);
        }
        return userData as User;
      }
      throw new Error(res.message || 'Customer registration failed');
    } finally {
      setIsLoading(false);
    }
  };

  const registerMerchant = async (payload: {
    fullName: string;
    email: string;
    password: string;
    phone?: string;
    inviteCode: string;
  }): Promise<User> => {
    setIsLoading(true);
    try {
      const res = await authApi.registerMerchant(payload);
      if (res.success && res.data) {
        const authData = res.data as any;
        const userData = authData.user || authData;
        if (authData.accessToken && authData.user) {
          setAuthSession(authData.user, authData.accessToken);
        }
        return userData as User;
      }
      throw new Error(res.message || 'Merchant registration failed');
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      await authApi.logout();
    } catch {
      // Ignore network errors on logout
    } finally {
      handleLogoutState();
    }
  };

  const isAdmin = !!user?.roles?.some((r) => r === 'ROLE_ADMIN' || r === 'ADMIN');
  const isMerchant = !!user?.roles?.some((r) => r === 'ROLE_MERCHANT' || r === 'MERCHANT');
  const isCustomer = !!user?.roles?.some((r) => r === 'ROLE_CUSTOMER' || r === 'CUSTOMER');

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isAdmin,
        isMerchant,
        isCustomer,
        isLoading,
        login,
        registerCustomer,
        registerMerchant,
        logout,
        refreshUser,
        setAuthSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

