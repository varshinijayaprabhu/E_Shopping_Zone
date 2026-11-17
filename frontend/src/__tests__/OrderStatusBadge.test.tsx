import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OrderStatusBadge } from '../components/order/OrderStatusBadge';

describe('OrderStatusBadge Component', () => {
  it('renders CONFIRMED status with appropriate styling', () => {
    render(<OrderStatusBadge status="CONFIRMED" />);
    const badge = screen.getByText('CONFIRMED');
    expect(badge).toBeInTheDocument();
  });

  it('renders CANCELLED status correctly', () => {
    render(<OrderStatusBadge status="CANCELLED" />);
    const badge = screen.getByText('CANCELLED');
    expect(badge).toBeInTheDocument();
  });

  it('handles lowercase or mixed-case status cleanly', () => {
    render(<OrderStatusBadge status="placed" />);
    const badge = screen.getByText('PLACED');
    expect(badge).toBeInTheDocument();
  });
});

