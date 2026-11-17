import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Cart, CartItem } from '../types';
import { cartApi } from '../api/cartApi';
import { useAuth } from './AuthContext';
import { useNotification } from './NotificationContext';

interface CartContextType {
  cart: Cart | null;
  items: CartItem[];
  itemCount: number;
  cartSubtotal: number;
  isLoading: boolean;
  fetchCart: () => Promise<void>;
  addToCart: (productId: number, quantity?: number) => Promise<boolean>;
  updateQuantity: (itemId: number, quantity: number) => Promise<boolean>;
  removeItem: (itemId: number) => Promise<boolean>;
  clearCart: () => Promise<boolean>;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isCustomer } = useAuth();
  const { showSuccess, showError } = useNotification();
  const [cart, setCart] = useState<Cart | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const fetchCart = useCallback(async () => {
    if (!isAuthenticated || !isCustomer) {
      setCart(null);
      return;
    }
    setIsLoading(true);
    try {
      const res = await cartApi.getCart();
      if (res.success && res.data) {
        setCart(res.data);
      }
    } catch {
      // Cart may not exist yet or empty
      setCart(null);
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, isCustomer]);

  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  const addToCart = async (productId: number, quantity = 1): Promise<boolean> => {
    if (!isAuthenticated) {
      showError('Please sign in as a customer to add items to your cart.');
      return false;
    }
    try {
      const res = await cartApi.addToCart(productId, quantity);
      if (res.success) {
        showSuccess('Item added to cart!');
        await fetchCart();
        return true;
      }
      showError(res.message || 'Failed to add item to cart');
      return false;
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Could not add item to cart';
      showError(msg);
      return false;
    }
  };

  const updateQuantity = async (itemId: number, quantity: number): Promise<boolean> => {
    try {
      const res = await cartApi.updateCartItem(itemId, quantity);
      if (res.success) {
        await fetchCart();
        return true;
      }
      showError(res.message || 'Failed to update quantity');
      return false;
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Could not update item quantity';
      showError(msg);
      return false;
    }
  };

  const removeItem = async (itemId: number): Promise<boolean> => {
    try {
      const res = await cartApi.removeCartItem(itemId);
      if (res.success) {
        showSuccess('Item removed from cart');
        await fetchCart();
        return true;
      }
      return false;
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Failed to remove item';
      showError(msg);
      return false;
    }
  };

  const clearCart = async (): Promise<boolean> => {
    try {
      const res = await cartApi.clearCart();
      if (res.success) {
        setCart(null);
        return true;
      }
      return false;
    } catch {
      return false;
    }
  };

  const items = cart?.items || [];
  const itemCount = cart?.totalItems || items.reduce((acc, it) => acc + it.quantity, 0);
  const cartSubtotal = cart?.cartSubtotal || items.reduce((acc, it) => acc + it.itemSubtotal, 0);

  return (
    <CartContext.Provider
      value={{
        cart,
        items,
        itemCount,
        cartSubtotal,
        isLoading,
        fetchCart,
        addToCart,
        updateQuantity,
        removeItem,
        clearCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

