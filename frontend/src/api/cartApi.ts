import axiosClient from './axiosClient';
import { ApiResponse, Cart, CartItem } from '../types';

export const cartApi = {
  getCart: async (): Promise<ApiResponse<Cart>> => {
    const res = await axiosClient.get<ApiResponse<Cart>>('/api/cart');
    return res.data;
  },

  addToCart: async (productId: number, quantity = 1): Promise<ApiResponse<CartItem>> => {
    const res = await axiosClient.post<ApiResponse<CartItem>>('/api/cart/items', {
      productId,
      quantity,
    });
    return res.data;
  },

  updateCartItem: async (itemId: number, quantity: number): Promise<ApiResponse<CartItem>> => {
    const res = await axiosClient.put<ApiResponse<CartItem>>(`/api/cart/items/${itemId}`, {
      quantity,
    });
    return res.data;
  },

  removeCartItem: async (itemId: number): Promise<ApiResponse<void>> => {
    const res = await axiosClient.delete<ApiResponse<void>>(`/api/cart/items/${itemId}`);
    return res.data;
  },

  clearCart: async (): Promise<ApiResponse<void>> => {
    const res = await axiosClient.delete<ApiResponse<void>>('/api/cart');
    return res.data;
  },
};

