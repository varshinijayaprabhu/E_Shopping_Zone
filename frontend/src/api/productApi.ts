import axiosClient from './axiosClient';
import {
  ApiResponse,
  Category,
  PaginatedResponse,
  Product,
  ProductCreatePayload,
  ProductUpdatePayload,
} from '../types';

export interface ProductQueryParams {
  page?: number;
  size?: number;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
}

function normalizePaginated<T>(res: any): ApiResponse<PaginatedResponse<T>> {
  if (res && res.data && Array.isArray(res.data)) {
    const items = res.data;
    const meta = res.pagination || {};
    res.data = {
      content: items,
      totalElements: meta.totalElements ?? items.length,
      totalPages: meta.totalPages ?? 1,
      pageNumber: meta.page ?? 0,
      pageSize: meta.size ?? items.length,
      first: (meta.page ?? 0) === 0,
      last: (meta.page ?? 0) >= (meta.totalPages ?? 1) - 1,
      empty: items.length === 0,
    };
  }
  return res;
}

export const productApi = {
  getProducts: async (params: ProductQueryParams = {}): Promise<ApiResponse<PaginatedResponse<Product>>> => {
    const res = await axiosClient.get<any>('/api/products', {
      params,
    });
    return normalizePaginated<Product>(res.data);
  },

  searchProducts: async (
    keyword: string,
    page = 0,
    size = 12
  ): Promise<ApiResponse<PaginatedResponse<Product>>> => {
    const res = await axiosClient.get<any>('/api/products/search', {
      params: { query: keyword, page, size },
    });
    return normalizePaginated<Product>(res.data);
  },

  getProductById: async (id: number): Promise<ApiResponse<Product>> => {
    const res = await axiosClient.get<ApiResponse<Product>>(`/api/products/${id}`);
    return res.data;
  },

  getCategories: async (): Promise<ApiResponse<Category[]>> => {
    const res = await axiosClient.get<ApiResponse<Category[]>>('/api/categories');
    return res.data;
  },

  getMerchantProducts: async (page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<Product>>> => {
    const res = await axiosClient.get<any>('/api/products/merchant', {
      params: { page, size },
    });
    return normalizePaginated<Product>(res.data);
  },

  createProduct: async (payload: ProductCreatePayload): Promise<ApiResponse<Product>> => {
    const res = await axiosClient.post<ApiResponse<Product>>('/api/products', payload);
    return res.data;
  },

  updateProduct: async (id: number, payload: ProductUpdatePayload): Promise<ApiResponse<Product>> => {
    const res = await axiosClient.put<ApiResponse<Product>>(`/api/products/${id}`, payload);
    return res.data;
  },

  deactivateProduct: async (id: number): Promise<ApiResponse<void>> => {
    const res = await axiosClient.delete<ApiResponse<void>>(`/api/products/${id}`);
    return res.data;
  },
};

