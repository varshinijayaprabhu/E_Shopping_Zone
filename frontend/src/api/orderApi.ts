import axiosClient from './axiosClient';
import { ApiResponse, CheckoutPayload, MerchantOrderItem, Order, PaginatedResponse } from '../types';

export const orderApi = {
  checkout: async (payload: CheckoutPayload): Promise<ApiResponse<Order>> => {
    const res = await axiosClient.post<ApiResponse<Order>>('/api/orders/checkout', payload);
    return res.data;
  },

  getCustomerOrders: async (page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<Order>>> => {
    const res = await axiosClient.get<ApiResponse<PaginatedResponse<Order>>>('/api/orders', {
      params: { page, size },
    });
    return res.data;
  },

  getMyOrders: async (page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<Order>>> => {
    const res = await axiosClient.get<ApiResponse<PaginatedResponse<Order>>>('/api/orders', {
      params: { page, size },
    });
    return res.data;
  },

  getOrderById: async (id: number): Promise<ApiResponse<Order>> => {
    const res = await axiosClient.get<ApiResponse<Order>>(`/api/orders/${id}`);
    return res.data;
  },

  getOrderStatus: async (orderNumber: string): Promise<ApiResponse<Order>> => {
    const res = await axiosClient.get<ApiResponse<Order>>(`/api/orders/status/${orderNumber}`);
    return res.data;
  },

  cancelOrder: async (id: number, reason?: string): Promise<ApiResponse<Order>> => {
    const res = await axiosClient.post<ApiResponse<Order>>(`/api/orders/${id}/cancel`, {
      reason: reason || 'Customer requested cancellation',
    });
    return res.data;
  },

  getMerchantOrders: async (page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<MerchantOrderItem>>> => {
    const res = await axiosClient.get<ApiResponse<PaginatedResponse<MerchantOrderItem>>>('/api/orders/merchant', {
      params: { page, size },
    });
    return res.data;
  },
};
