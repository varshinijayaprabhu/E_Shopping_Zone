import { describe, it, expect } from 'vitest';
import { formatCurrency, formatDate, maskPhone } from '../utils/formatters';

describe('Formatters Unit Tests', () => {
  describe('formatCurrency', () => {
    it('formats positive numbers in INR format', () => {
      const formatted = formatCurrency(14999);
      expect(formatted).toContain('₹');
      expect(formatted).toContain('14,999.00');
    });

    it('formats zero correctly', () => {
      const formatted = formatCurrency(0);
      expect(formatted).toContain('₹');
      expect(formatted).toContain('0.00');
    });

    it('handles null and undefined gracefully', () => {
      expect(formatCurrency(null)).toBe('₹0.00');
      expect(formatCurrency(undefined)).toBe('₹0.00');
    });

    it('handles decimal precision to two places', () => {
      const formatted = formatCurrency(1234.567);
      expect(formatted).toContain('1,234.57');
    });
  });

  describe('formatDate', () => {
    it('returns N/A for null or empty values', () => {
      expect(formatDate(null)).toBe('N/A');
      expect(formatDate('')).toBe('N/A');
    });

    it('formats ISO timestamps into readable localized date string', () => {
      const result = formatDate('2026-11-07T10:30:00Z');
      expect(result).not.toBe('N/A');
      expect(result).toContain('2026');
    });
  });

  describe('maskPhone', () => {
    it('masks standard 10-digit phone numbers', () => {
      const result = maskPhone('9876543210');
      expect(result).toBe('987****210');
    });

    it('handles short strings safely', () => {
      expect(maskPhone('123')).toBe('123');
      expect(maskPhone(null)).toBe('');
    });
  });
});

