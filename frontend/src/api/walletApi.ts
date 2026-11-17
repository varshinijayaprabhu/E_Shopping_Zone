import axiosClient from './axiosClient';
import { ApiResponse, PaginatedResponse, Wallet, WalletTransaction } from '../types';

export const walletApi = {
  getWallet: async (): Promise<ApiResponse<Wallet>> => {
    const res = await axiosClient.get<ApiResponse<Wallet>>('/api/wallet');
    return res.data;
  },

  getTransactions: async (page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<WalletTransaction>>> => {
    const res = await axiosClient.get<ApiResponse<PaginatedResponse<WalletTransaction>>>('/api/wallet/transactions', {
      params: { page, size },
    });
    return res.data;
  },

  deposit: async (payload: {
    amount: number;
    idempotencyKey: string;
    fundingSource?: string;
    description?: string;
  }): Promise<ApiResponse<WalletTransaction>> => {
    const res = await axiosClient.post<ApiResponse<WalletTransaction>>('/api/wallet/deposit', payload);
    return res.data;
  },
};

