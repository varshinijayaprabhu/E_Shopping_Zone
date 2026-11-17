import axiosClient from './axiosClient';
import {
  ApiResponse,
  DashboardStats,
  MerchantProfile,
  DisputeTicket,
  AdminAuditLog,
  PaginatedResponse,
} from '../types';

export const adminApi = {
  // Platform Governance / Big Numbers
  getMetrics: async (): Promise<ApiResponse<DashboardStats>> => {
    const res = await axiosClient.get<ApiResponse<DashboardStats>>('/api/admin/metrics');
    return res.data;
  },

  // Merchant Management
  getMerchants: async (): Promise<ApiResponse<MerchantProfile[]>> => {
    const res = await axiosClient.get<ApiResponse<MerchantProfile[]>>('/api/admin/merchants');
    return res.data;
  },

  updateMerchantStatus: async (
    id: number,
    enabled: boolean,
    reason?: string
  ): Promise<ApiResponse<void>> => {
    const res = await axiosClient.put<ApiResponse<void>>(`/api/admin/merchants/${id}/status`, {
      enabled,
      reason: reason || (enabled ? 'Account approved by administrator' : 'Account suspended by administrator'),
    });
    return res.data;
  },

  // Dispute Management (Admin Queue)
  getDisputes: async (
    status?: string,
    page = 0,
    size = 10
  ): Promise<ApiResponse<PaginatedResponse<DisputeTicket>>> => {
    const res = await axiosClient.get<ApiResponse<PaginatedResponse<DisputeTicket>>>('/api/admin/disputes', {
      params: { status, page, size },
    });
    return res.data;
  },

  resolveDispute: async (
    id: number,
    action: 'REFUND' | 'NO_REFUND',
    adminNotes: string
  ): Promise<ApiResponse<DisputeTicket>> => {
    const res = await axiosClient.post<ApiResponse<DisputeTicket>>(`/api/admin/disputes/${id}/resolve`, {
      action,
      adminNotes,
    });
    return res.data;
  },

  // Audit Logs
  getAuditLogs: async (page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<AdminAuditLog>>> => {
    const res = await axiosClient.get<ApiResponse<PaginatedResponse<AdminAuditLog>>>('/api/admin/audit-logs', {
      params: { page, size },
    });
    return res.data;
  },

  // Customer & Merchant Dispute Actions
  createDispute: async (payload: {
    orderId: number;
    merchantId: number;
    issueReason: string;
    customerComment?: string;
    refundAmount: number;
  }): Promise<ApiResponse<DisputeTicket>> => {
    const res = await axiosClient.post<ApiResponse<DisputeTicket>>('/api/disputes/create', payload);
    return res.data;
  },

  getMyDisputes: async (): Promise<ApiResponse<DisputeTicket[]>> => {
    const res = await axiosClient.get<ApiResponse<DisputeTicket[]>>('/api/disputes/my');
    return res.data;
  },

  merchantRespond: async (
    disputeId: number,
    action: 'ACCEPT' | 'REJECT',
    responseComment: string
  ): Promise<ApiResponse<DisputeTicket>> => {
    const res = await axiosClient.post<ApiResponse<DisputeTicket>>(
      `/api/disputes/${disputeId}/merchant-response`,
      { action, responseComment }
    );
    return res.data;
  },

  getDisputeById: async (id: number): Promise<ApiResponse<DisputeTicket>> => {
    const res = await axiosClient.get<ApiResponse<DisputeTicket>>(`/api/disputes/${id}`);
    return res.data;
  },
};
