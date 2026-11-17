/**
 * Formats a numeric value into INR currency string with standard Indian locale formatting (e.g. ₹14,999.00)
 */
export const formatCurrency = (amount: number | null | undefined): string => {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '₹0.00';
  }
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

/**
 * Formats an ISO date/timestamp string into a localized readable date.
 */
export const formatDate = (dateStr: string | null | undefined): string => {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
};

/**
 * Masks sensitive strings (e.g. phone numbers, addresses, account identifiers) for PII safety.
 */
export const maskPhone = (phone: string | null | undefined): string => {
  if (!phone) return '';
  if (phone.length <= 4) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(-3)}`;
};

