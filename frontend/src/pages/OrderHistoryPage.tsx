import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Package, ArrowRight, Calendar, ShoppingBag } from 'lucide-react';
import { orderApi } from '../api/orderApi';
import { Order } from '../types';
import { formatCurrency, formatDate } from '../utils/formatters';
import { OrderStatusBadge } from '../components/order/OrderStatusBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const OrderHistoryPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    orderApi
      .getMyOrders(0, 30)
      .then((res) => {
        if (res.success && res.data) {
          setOrders(res.data.content);
        }
      })
      .catch(() => {
        setOrders([]);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Loading your order history..." />
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="max-w-xl mx-auto my-16 bg-white p-12 rounded-3xl border border-slate-200 text-center shadow-xs space-y-4">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-2">
          <ShoppingBag className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-slate-900">No Orders Placed Yet</h2>
        <p className="text-sm text-slate-500 max-w-sm mx-auto">
          You haven't completed any orders yet. Discover great deals in our catalog today.
        </p>
        <div className="pt-2">
          <Link
            to="/products"
            className="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white font-bold text-xs rounded-xl hover:bg-indigo-700 transition shadow-md shadow-indigo-100"
          >
            <span>Explore Catalog</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">Order History</h1>
        <p className="text-xs text-slate-500 mt-1">
          Track, review, or inspect your past and active orders
        </p>
      </div>

      <div className="space-y-4">
        {orders.map((order) => (
          <div
            key={order.id}
            className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs hover:border-indigo-300 transition-all flex flex-col md:flex-row items-start md:items-center justify-between gap-6"
          >
            {/* Left Order Metadata */}
            <div className="space-y-2">
              <div className="flex flex-wrap items-center gap-3">
                <span className="font-mono text-sm font-bold text-slate-900">
                  #{order.orderNumber}
                </span>
                <OrderStatusBadge status={order.orderStatus} />
                <span className="text-[11px] font-medium text-slate-400 flex items-center gap-1">
                  <Calendar className="w-3.5 h-3.5" />
                  {formatDate(order.createdAt)}
                </span>
              </div>

              {/* Items summary */}
              <div className="flex items-center gap-2 text-xs text-slate-600">
                <Package className="w-4 h-4 text-slate-400" />
                <span>
                  {order.items?.length || 0} item{order.items?.length !== 1 ? 's' : ''}:{' '}
                  <span className="font-medium text-slate-800">
                    {order.items?.map((it) => it.productNameSnapshot).join(', ')}
                  </span>
                </span>
              </div>
            </div>

            {/* Right Totals & CTA */}
            <div className="flex items-center justify-between md:justify-end gap-6 w-full md:w-auto border-t md:border-t-0 pt-4 md:pt-0 border-slate-100">
              <div className="text-left md:text-right">
                <span className="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">
                  Total
                </span>
                <span className="text-lg font-black text-slate-900">
                  {formatCurrency(order.totalAmount)}
                </span>
              </div>

              <Link
                to={`/orders/${order.id}`}
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 hover:bg-black text-white text-xs font-semibold rounded-xl transition shadow-xs"
              >
                <span>View Details</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

