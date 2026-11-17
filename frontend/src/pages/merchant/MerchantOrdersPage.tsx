import React, { useState, useEffect, useCallback } from 'react';
import { ShoppingBag, Truck, Calendar } from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { MerchantOrderItem } from '../../types';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { OrderStatusBadge } from '../../components/order/OrderStatusBadge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const MerchantOrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<MerchantOrderItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchOrders = useCallback(async (page = 0) => {
    setIsLoading(true);
    try {
      const res = await orderApi.getMerchantOrders(page, 15);
      if (res.success && res.data) {
        setOrders(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
        setCurrentPage(page);
      }
    } catch {
      setOrders([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchOrders(0);
  }, [fetchOrders]);

  return (
    <div className="space-y-8 pb-16">
      {/* Header */}
      <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">Merchant Orders</h1>
        <p className="text-xs text-slate-500 mt-1">
          Review customer purchases containing items from your store inventory ({totalElements} items total)
        </p>
      </div>

      {/* Orders Table Container */}
      <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-xs">
        {isLoading ? (
          <div className="py-16">
            <LoadingSpinner size="lg" message="Loading merchant orders..." />
          </div>
        ) : orders.length === 0 ? (
          <div className="py-16 text-center text-slate-500 text-xs space-y-3">
            <ShoppingBag className="w-10 h-10 mx-auto text-slate-300" />
            <p className="text-sm font-bold text-slate-700">No customer orders yet.</p>
            <p className="text-slate-400">
              When customers purchase items from your inventory, they will appear here.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 uppercase tracking-wider text-[10px] bg-slate-50/50">
                  <th className="py-3.5 px-4">Order / Date</th>
                  <th className="py-3.5 px-4">Item Snapshot</th>
                  <th className="py-3.5 px-4 text-center">Qty</th>
                  <th className="py-3.5 px-4 text-right">Subtotal</th>
                  <th className="py-3.5 px-4">Shipping Destination</th>
                  <th className="py-3.5 px-4 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {orders.map((o) => (
                  <tr key={o.orderItemId} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3.5 px-4">
                      <div>
                        <span className="font-mono font-bold text-slate-900 block">
                          #{o.orderNumber}
                        </span>
                        <span className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                          <Calendar className="w-3 h-3" />
                          {formatDate(o.orderCreatedAt)}
                        </span>
                      </div>
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="min-w-0">
                        <span className="font-bold text-slate-900 block">{o.productNameSnapshot}</span>
                        <span className="font-mono text-[10px] text-slate-400">
                          {o.productSkuSnapshot || `Product #${o.productId}`} • {formatCurrency(o.unitPriceSnapshot)}
                        </span>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-center font-bold text-slate-800">
                      {o.quantity}
                    </td>

                    <td className="py-3.5 px-4 text-right font-extrabold text-slate-900 font-mono">
                      {formatCurrency(o.subtotal)}
                    </td>

                    <td className="py-3.5 px-4 max-w-xs">
                      <div className="text-[11px] text-slate-600 space-y-0.5">
                        <span className="font-bold text-slate-900 block flex items-center gap-1">
                          <Truck className="w-3 h-3 text-indigo-500" />
                          {o.shippingName}
                        </span>
                        <p className="line-clamp-2 text-[10px] text-slate-500">{o.shippingAddress}</p>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <OrderStatusBadge status={o.orderStatus} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-6 py-3 bg-slate-50/50">
            <button
              disabled={currentPage === 0}
              onClick={() => fetchOrders(currentPage - 1)}
              className="text-xs font-semibold text-slate-600 hover:text-indigo-600 disabled:opacity-40"
            >
              &larr; Previous
            </button>
            <span className="text-xs text-slate-500 font-medium">
              Page {currentPage + 1} of {totalPages}
            </span>
            <button
              disabled={currentPage >= totalPages - 1}
              onClick={() => fetchOrders(currentPage + 1)}
              className="text-xs font-semibold text-slate-600 hover:text-indigo-600 disabled:opacity-40"
            >
              Next &rarr;
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

