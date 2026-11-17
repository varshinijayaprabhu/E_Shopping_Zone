import React from 'react';

interface OrderStatusBadgeProps {
  status: string;
}

export const OrderStatusBadge: React.FC<OrderStatusBadgeProps> = ({ status }) => {
  const normalized = status ? status.toUpperCase() : 'UNKNOWN';

  let colorClasses = 'bg-slate-100 text-slate-700 border-slate-300';
  let dotColor = 'bg-slate-400';

  switch (normalized) {
    case 'CONFIRMED':
    case 'DELIVERED':
    case 'SUCCESS':
      colorClasses = 'bg-emerald-50 text-emerald-700 border-emerald-200';
      dotColor = 'bg-emerald-500';
      break;
    case 'PLACED':
    case 'PENDING':
    case 'PROCESSING':
      colorClasses = 'bg-blue-50 text-blue-700 border-blue-200';
      dotColor = 'bg-blue-500';
      break;
    case 'SHIPPED':
      colorClasses = 'bg-purple-50 text-purple-700 border-purple-200';
      dotColor = 'bg-purple-500';
      break;
    case 'CANCELLED':
    case 'FAILED':
      colorClasses = 'bg-rose-50 text-rose-700 border-rose-200';
      dotColor = 'bg-rose-500';
      break;
    default:
      colorClasses = 'bg-slate-100 text-slate-700 border-slate-200';
      dotColor = 'bg-slate-400';
  }

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${colorClasses}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${dotColor}`} />
      {normalized}
    </span>
  );
};

