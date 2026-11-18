import axiosClient from './axiosClient';
import { ApiResponse, AuthResponse, User } from '../types';

export const authApi = {
  login: async (email: string, password: string): Promise<ApiResponse<AuthResponse>> => {
    const res = await axiosClient.post<any>('/api/auth/login', {
      email,
      password,
    });
    const payload = res.data?.data || res.data;
    return {
      success: true,
      data: payload,
    };
  },

  registerCustomer: async (payload: {
    fullName: string;
    email: string;
    password: string;
    phone?: string;
  }): Promise<ApiResponse<AuthResponse>> => {
    const res = await axiosClient.post<any>('/api/auth/register', {
      fullName: payload.fullName,
      email: payload.email,
      password: payload.password,
      phoneNumber: payload.phone || null,
    });
    const data = res.data?.data || res.data;
    return {
      success: true,
      data,
    };
  },

  registerMerchant: async (payload: {
    fullName: string;
    email: string;
    password: string;
    phone?: string;
    inviteCode?: string;
  }): Promise<ApiResponse<AuthResponse>> => {
    const res = await axiosClient.post<any>('/api/auth/register-merchant', {
      fullName: payload.fullName,
      email: payload.email,
      password: payload.password,
      phoneNumber: payload.phone || null,
      merchantInviteCode: payload.inviteCode || null,
    });
    const data = res.data?.data || res.data;
    return {
      success: true,
      data,
    };
  },

  refreshToken: async (): Promise<ApiResponse<AuthResponse>> => {
    const res = await axiosClient.post<any>('/api/auth/refresh');
    const data = res.data?.data || res.data;
    return {
      success: true,
      data,
    };
  },

  logout: async (): Promise<ApiResponse<void>> => {
    const res = await axiosClient.post<any>('/api/auth/logout');
    return {
      success: true,
      data: undefined,
    };
  },

  getProfile: async (): Promise<ApiResponse<User>> => {
    const res = await axiosClient.get<any>('/api/profile');
    const data = res.data?.data || res.data;
    return {
      success: true,
      data,
    };
  },

  updateProfile: async (payload: { fullName?: string; phone?: string }): Promise<ApiResponse<User>> => {
    const res = await axiosClient.put<any>('/api/profile', {
      fullName: payload.fullName,
      phoneNumber: payload.phone,
    });
    const data = res.data?.data || res.data;
    return {
      success: true,
      data,
    };
  },
};
