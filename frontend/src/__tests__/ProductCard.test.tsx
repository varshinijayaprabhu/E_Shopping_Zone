import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ProductCard } from '../components/product/ProductCard';
import { Product } from '../types';

// Mock CartContext
const mockAddToCart = vi.fn().mockResolvedValue(true);
vi.mock('../context/CartContext', () => ({
  useCart: () => ({
    addToCart: mockAddToCart,
    cart: null,
    items: [],
    itemCount: 0,
    cartSubtotal: 0,
    isLoading: false,
  }),
}));

const sampleProduct: Product = {
  id: 101,
  merchantId: 2,
  categoryId: 1,
  categoryName: 'Electronics',
  name: 'Wireless Bluetooth Earbuds',
  sku: 'ELEC-EAR-01',
  description: 'Premium noise-cancelling wireless earbuds',
  price: 2999,
  imageUrl: 'https://example.com/earbuds.jpg',
  availableStock: 15,
  active: true,
};

describe('ProductCard Component', () => {
  it('renders product details and price formatted in INR', () => {
    render(
      <BrowserRouter>
        <ProductCard product={sampleProduct} />
      </BrowserRouter>
    );

    expect(screen.getByText('Wireless Bluetooth Earbuds')).toBeInTheDocument();
    expect(screen.getByText('ELEC-EAR-01')).toBeInTheDocument();
    expect(screen.getByText('Electronics')).toBeInTheDocument();
    expect(screen.getByText(/2,999/)).toBeInTheDocument();
  });

  it('triggers addToCart when clicking Add button', async () => {
    render(
      <BrowserRouter>
        <ProductCard product={sampleProduct} />
      </BrowserRouter>
    );

    const addBtn = screen.getByRole('button', { name: /Add Wireless Bluetooth Earbuds to cart/i });
    await act(async () => {
      fireEvent.click(addBtn);
    });
    expect(mockAddToCart).toHaveBeenCalledWith(101, 1);
  });

  it('shows out of stock badge and disables button when availableStock is 0', () => {
    const outOfStockProduct = { ...sampleProduct, availableStock: 0 };
    render(
      <BrowserRouter>
        <ProductCard product={outOfStockProduct} />
      </BrowserRouter>
    );

    expect(screen.getByText('Out of Stock')).toBeInTheDocument();
    const addBtn = screen.getByRole('button', { name: /Add Wireless Bluetooth Earbuds to cart/i });
    expect(addBtn).toBeDisabled();
  });
});
