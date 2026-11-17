import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Wallet, WalletTransaction } from '../types';
import { walletApi } from '../api/walletApi';
import { useAuth } from './AuthContext';
import { useNotification } from './NotificationContext';

interface WalletContextType {
  wallet: Wallet | null;
  balance: number;
  isLoading: boolean;
  fetchWallet: () => Promise<void>;
  deposit: (amount: number, fundingSource?: string, description?: string) => Promise<WalletTransaction | null>;
}

const WalletContext = createContext<WalletContextType | undefined>(undefined);

export const WalletProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isCustomer } = useAuth();
  const { showSuccess, showError } = useNotification();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const fetchWallet = useCallback(async () => {
    if (!isAuthenticated || !isCustomer) {
      setWallet(null);
      return;
    }
    setIsLoading(true);
    try {
      const res = await walletApi.getWallet();
      if (res.success && res.data) {
        setWallet(res.data);
      }
    } catch {
      setWallet(null);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, isCustomer]);

  useEffect(() => {
    fetchWallet();
  }, [fetchWallet]);

  const deposit = async (
    amount: number,
    fundingSource = 'SIMULATED_UPI',
    description = 'Wallet Top-up'
  ): Promise<WalletTransaction | null> => {
    try {
      const idempotencyKey = `DEP-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      const res = await walletApi.deposit({
        amount,
        idempotencyKey,
        fundingSource,
        description,
      });

      if (res.success && res.data) {
        showSuccess(`₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })} successfully added to wallet!`);
        await fetchWallet();
        return res.data;
      }
      showError(res.message || 'Deposit failed');
      return null;
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to complete wallet deposit';
      showError(msg);
      return null;
    }
  };

  const balance = wallet?.currentBalance || 0;

  return (
    <WalletContext.Provider
      value={{
        wallet,
        balance,
        isLoading,
        fetchWallet,
        deposit,
      }}
    >
      {children}
    </WalletContext.Provider>
  );
};

export const useWallet = () => {
  const context = useContext(WalletContext);
  if (!context) {
    throw new Error('useWallet must be used within a WalletProvider');
  }
  return context;
};

