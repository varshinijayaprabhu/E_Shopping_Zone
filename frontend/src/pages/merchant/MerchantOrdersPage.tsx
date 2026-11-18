import React, { useState, useEffect, useCallback } from 'react';
import { ShoppingBag, Truck, Calendar, CheckCircle2, PackageCheck, AlertTriangle } from 'lucide-react';
import { orderApi } from '../../api/orderApi';
import { adminApi } from '../../api/adminApi';
import { MerchantOrderItem, DisputeTicket } from '../../types';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { OrderStatusBadge } from '../../components/order/OrderStatusBadge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { useNotification } from '../../context/NotificationContext';

export const MerchantOrdersPage: React.FC = () => {
  const { showSuccess, showError } = useNotification();
  const [orders, setOrders] = useState<MerchantOrderItem[]>([]);
  const [disputes, setDisputes] = useState<DisputeTicket[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [updatingOrderNum, setUpdatingOrderNum] = useState<string | null>(null);
  const [respondingDisputeId, setRespondingDisputeId] = useState<number | null>(null);

  const fetchOrders = useCallback(async (page = 0) => {
    setIsLoading(true);
    try {
      const [orderRes, disputesRes] = await Promise.all([
        orderApi.getMerchantOrders(page, 15),
        adminApi.getMyDisputes().catch(() => ({ success: false, data: [] })),
      ]);

      if (orderRes.success && orderRes.data) {
        setOrders(orderRes.data.content);
        setTotalPages(orderRes.data.totalPages);
        setTotalElements(orderRes.data.totalElements);
        setCurrentPage(page);
      }
      if (disputesRes.success && Array.isArray(disputesRes.data)) {
        setDisputes(disputesRes.data);
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

  const handleUpdateStatus = async (orderNumber: string, newStatus: string) => {
    setUpdatingOrderNum(orderNumber);
    try {
      const res = await orderApi.updateOrderStatus(orderNumber, newStatus);
      if (res.success) {
        showSuccess(`Order #${orderNumber} marked as ${newStatus}!`);
        setOrders((prev) =>
          prev.map((o) => (o.orderNumber === orderNumber ? { ...o, orderStatus: newStatus } : o))
        );
      } else {
        showError(res.message || 'Failed to update order status');
      }
    } catch (err: any) {
      showError(err.response?.data?.message || err.message || 'Failed to update order status');
    } finally {
      setUpdatingOrderNum(null);
    }
  };

  const handleMerchantDisputeResponse = async (
    disputeId: number,
    action: 'ACCEPT' | 'REJECT'
  ) => {
    setRespondingDisputeId(disputeId);
    try {
      const comment =
        action === 'ACCEPT'
          ? 'Merchant accepted customer dispute claim. Refund authorized.'
          : 'Merchant rejected claim. Evidence supports proper delivery. Escalated to admin.';
      const res = await adminApi.merchantRespond(disputeId, action, comment);
      if (res.success && res.data) {
        showSuccess(
          action === 'ACCEPT'
            ? 'Dispute accepted. Customer refund processed automatically.'
            : 'Dispute rejected and escalated to Platform Admin for final review.'
        );
        setDisputes((prev) => prev.map((d) => (d.id === disputeId ? res.data : d)));
        await fetchOrders(currentPage);
      } else {
        showError(res.message || 'Failed to submit response');
      }
    } catch (err: any) {
      showError(err.response?.data?.message || err.message || 'Dispute action failed');
    } finally {
      setRespondingDisputeId(null);
    }
  };

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
                  <th className="py-3.5 px-4 text-center">Fulfillment Action</th>
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

                    <td className="py-3.5 px-4 text-center">
                      {(() => {
                        const orderDispute = o.orderId ? disputes.find((d) => d.orderId === o.orderId) : null;
                        return (
                          <div className="space-y-2">
                            {(o.orderStatus === 'CONFIRMED' || o.orderStatus === 'PLACED') && (
                              <button
                                type="button"
                                disabled={updatingOrderNum === o.orderNumber}
                                onClick={() => handleUpdateStatus(o.orderNumber, 'SHIPPED')}
                                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl text-[11px] font-bold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 transition-colors shadow-2xs"
                              >
                                <Truck className="w-3.5 h-3.5" />
                                <span>{updatingOrderNum === o.orderNumber ? 'Updating...' : 'Mark Shipped'}</span>
                              </button>
                            )}
                            {o.orderStatus === 'SHIPPED' && (
                              <button
                                type="button"
                                disabled={updatingOrderNum === o.orderNumber}
                                onClick={() => handleUpdateStatus(o.orderNumber, 'DELIVERED')}
                                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl text-[11px] font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 transition-colors shadow-2xs"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                <span>{updatingOrderNum === o.orderNumber ? 'Updating...' : 'Mark Delivered'}</span>
                              </button>
                            )}
                            {o.orderStatus === 'DELIVERED' && (
                              <span className="text-[11px] font-semibold text-emerald-600 flex items-center justify-center gap-1">
                                <CheckCircle2 className="w-3.5 h-3.5" /> Fulfilled
                              </span>
                            )}
                            {o.orderStatus === 'CANCELLED' && (
                              <span className="text-[11px] font-medium text-slate-400">
                                Cancelled
                              </span>
                            )}
                            {o.orderStatus === 'REFUNDED_BY_ADMIN' && (
                              <span className="text-[11px] font-medium text-amber-600">
                                Refunded
                              </span>
                            )}

                            {/* Dispute Notice & Actions */}
                            {orderDispute && (
                              <div className="mt-2 p-2 bg-amber-50/90 border border-amber-200 rounded-xl text-[10px] text-left space-y-1">
                                <div className="flex items-center justify-between text-amber-900 font-bold">
                                  <span className="flex items-center gap-1">
                                    <AlertTriangle className="w-3 h-3 text-amber-600" />
                                    Dispute #{orderDispute.id}
                                  </span>
                                  <span className="text-[9px] px-1.5 py-0.5 rounded-sm bg-amber-100 text-amber-800">
                                    {orderDispute.status.replace(/_/g, ' ')}
                                  </span>
                                </div>
                                <p className="text-slate-600 truncate font-medium">
                                  {orderDispute.issueReason}
                                </p>

                                {orderDispute.status === 'PENDING_MERCHANT' && (
                                  <div className="flex gap-1 pt-1 border-t border-amber-200/60">
                                    <button
                                      type="button"
                                      disabled={respondingDisputeId === orderDispute.id}
                                      onClick={() => handleMerchantDisputeResponse(orderDispute.id, 'ACCEPT')}
                                      className="flex-1 px-1.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded font-bold text-[9px] shadow-2xs transition"
                                    >
                                      Accept & Refund
                                    </button>
                                    <button
                                      type="button"
                                      disabled={respondingDisputeId === orderDispute.id}
                                      onClick={() => handleMerchantDisputeResponse(orderDispute.id, 'REJECT')}
                                      className="flex-1 px-1.5 py-1 bg-rose-600 hover:bg-rose-700 text-white rounded font-bold text-[9px] shadow-2xs transition"
                                    >
                                      Reject (Escalate)
                                    </button>
                                  </div>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })()}
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

