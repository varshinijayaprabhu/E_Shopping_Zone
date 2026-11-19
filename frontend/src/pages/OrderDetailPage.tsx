import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ArrowLeft,
  Truck,
  CreditCard,
  Package,
  Calendar,
  XCircle,
  AlertTriangle,
  ShieldCheck,
  Clock,
  CheckCircle2,
} from 'lucide-react';
import { orderApi } from '../api/orderApi';
import { adminApi } from '../api/adminApi';
import { Order, DisputeTicket } from '../types';
import { formatCurrency, formatDate, maskPhone } from '../utils/formatters';
import { OrderStatusBadge } from '../components/order/OrderStatusBadge';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ConfirmationModal } from '../components/common/ConfirmationModal';
import { useNotification } from '../context/NotificationContext';
import { useWallet } from '../context/WalletContext';

export const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess, showError } = useNotification();
  const { fetchWallet } = useWallet();

  const [order, setOrder] = useState<Order | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Cancellation state
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);

  // Dispute state
  const [disputeModalOpen, setDisputeModalOpen] = useState(false);
  const [isRaisingDispute, setIsRaisingDispute] = useState(false);
  const [disputeReason, setDisputeReason] = useState('Product Damaged in Transit');
  const [disputeComment, setDisputeComment] = useState('');
  const [existingDispute, setExistingDispute] = useState<DisputeTicket | null>(null);

  const fetchOrder = useCallback(async () => {
    if (!id || isNaN(Number(id))) {
      setError('Invalid order ID');
      setIsLoading(false);
      return;
    }
    try {
      const [orderRes, disputesRes] = await Promise.all([
        orderApi.getOrderById(Number(id)),
        adminApi.getMyDisputes().catch(() => ({ success: false, data: [] })),
      ]);

      if (orderRes.success && orderRes.data) {
        setOrder(orderRes.data);
      } else {
        setError(orderRes.message || 'Order not found');
      }

      if (disputesRes.success && Array.isArray(disputesRes.data)) {
        const found = disputesRes.data.find((d: DisputeTicket) => d.orderId === Number(id));
        setExistingDispute(found || null);
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      setError(errorResponse.response?.data?.message || 'Failed to retrieve order');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchOrder();
  }, [fetchOrder]);

  const handleCancelOrder = async () => {
    if (!order) return;
    setIsCancelling(true);
    try {
      const res = await orderApi.cancelOrder(order.id, 'Cancelled by customer from web UI');
      if (res.success && res.data) {
        showSuccess(`Order #${order.orderNumber} has been successfully cancelled.`);
        setOrder(res.data);
        setCancelModalOpen(false);
        // Refresh wallet balance if a refund occurred
        await fetchWallet();
      } else {
        showError(res.message || 'Failed to cancel order.');
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      showError(errorResponse.response?.data?.message || 'Order cancellation failed');
    } finally {
      setIsCancelling(false);
    }
  };

  const handleRaiseDispute = async () => {
    if (!order) return;
    setIsRaisingDispute(true);
    try {
      const merchantId = order.items?.[0]?.merchantId || 11;
      const res = await adminApi.createDispute({
        orderId: order.id,
        merchantId,
        issueReason: disputeReason,
        customerComment: disputeComment.trim() || undefined,
        refundAmount: order.totalAmount,
      });
      if (res.success && res.data) {
        showSuccess('Dispute raised successfully. Merchant has 2 minutes to respond.');
        setExistingDispute(res.data);
        setDisputeModalOpen(false);
      } else {
        showError(res.message || 'Failed to raise dispute');
      }
    } catch (err: any) {
      showError(err.response?.data?.message || err.message || 'Failed to raise dispute');
    } finally {
      setIsRaisingDispute(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Loading order details..." />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="max-w-xl mx-auto my-16 bg-white p-8 rounded-2xl border border-slate-200 text-center shadow-xs">
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-500 mx-auto flex items-center justify-center mb-3">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold text-slate-800">Order Not Found</h2>
        <p className="mt-2 text-xs text-slate-500">{error || 'Unable to locate the requested order.'}</p>
        <Link
          to="/orders"
          className="mt-6 inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-xs font-semibold rounded-xl hover:bg-indigo-700 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to My Orders</span>
        </Link>
      </div>
    );
  }

  const canCancel =
    order.orderStatus === 'CONFIRMED' ||
    order.orderStatus === 'PLACED' ||
    order.orderStatus === 'PENDING';

  const canDispute =
    order.orderStatus !== 'CANCELLED' &&
    order.orderStatus !== 'REFUNDED_BY_ADMIN' &&
    order.orderStatus !== 'FAILED';

  return (
    <div className="space-y-8 pb-16">
      {/* Header with Back button and Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <button
            type="button"
            onClick={() => navigate('/orders')}
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors mb-2"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Orders
          </button>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Order #{order.orderNumber}
            </h1>
            <OrderStatusBadge status={order.orderStatus} />
          </div>
          <p className="text-xs text-slate-500 mt-1 flex items-center gap-1.5">
            <Calendar className="w-3.5 h-3.5" />
            Placed on {formatDate(order.createdAt)}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {canCancel && (
            <button
              type="button"
              onClick={() => setCancelModalOpen(true)}
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs font-bold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 transition-colors shadow-xs self-start sm:self-auto"
            >
              <XCircle className="w-4 h-4" />
              <span>Cancel Order</span>
            </button>
          )}

          {canDispute && !existingDispute && (
            <button
              type="button"
              onClick={() => setDisputeModalOpen(true)}
              className="inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs font-bold text-amber-800 bg-amber-50 hover:bg-amber-100 border border-amber-300 transition-colors shadow-xs self-start sm:self-auto"
            >
              <AlertTriangle className="w-4 h-4" />
              <span>Raise Dispute</span>
            </button>
          )}
        </div>
      </div>

      {/* Active Dispute Banner */}
      {existingDispute && (
        <div className="bg-amber-50/70 border border-amber-200 rounded-3xl p-6 shadow-xs space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-amber-600" />
              <h3 className="text-sm font-bold text-amber-900">
                Dispute Ticket #{existingDispute.id}
              </h3>
            </div>
            <span
              className={`px-3 py-1 rounded-full text-[11px] font-bold ${
                existingDispute.status === 'REFUNDED_BY_ADMIN' || existingDispute.status === 'MERCHANT_ACCEPTED'
                  ? 'bg-emerald-100 text-emerald-800'
                  : existingDispute.status === 'RESOLVED_NO_REFUND' || existingDispute.status === 'MERCHANT_REJECTED'
                  ? 'bg-rose-100 text-rose-800'
                  : 'bg-amber-100 text-amber-800'
              }`}
            >
              {existingDispute.status.replace(/_/g, ' ')}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs pt-2 border-t border-amber-200/60">
            <div>
              <span className="text-amber-800 block font-semibold">Issue Reason</span>
              <span className="text-slate-800 font-medium">{existingDispute.issueReason}</span>
            </div>
            <div>
              <span className="text-amber-800 block font-semibold">Claim Amount</span>
              <span className="text-slate-800 font-bold">{formatCurrency(existingDispute.refundAmount)}</span>
            </div>
            <div>
              <span className="text-amber-800 block font-semibold">Customer Notes</span>
              <span className="text-slate-800">{existingDispute.customerComment || 'No comments provided'}</span>
            </div>
          </div>

          {existingDispute.merchantResponse && (
            <div className="p-3 bg-white/80 rounded-xl border border-amber-200 text-xs">
              <span className="font-semibold text-slate-700 block">Merchant Response:</span>
              <span className="text-slate-600">{existingDispute.merchantResponse}</span>
            </div>
          )}

          {existingDispute.adminNotes && (
            <div className="p-3 bg-indigo-50/80 rounded-xl border border-indigo-200 text-xs">
              <span className="font-semibold text-indigo-900 block">Admin Resolution Notes:</span>
              <span className="text-indigo-700">{existingDispute.adminNotes}</span>
            </div>
          )}
        </div>
      )}

      {/* 2-Column Grid: Left Items + Right Delivery/Payment */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Order Items */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-4">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Package className="w-5 h-5 text-indigo-600" />
              Ordered Items ({order.items?.length || 0})
            </h2>

            <div className="divide-y divide-slate-100">
              {order.items?.map((item) => (
                <div key={item.id} className="py-4 flex items-center justify-between gap-4 text-xs">
                  <div className="min-w-0 flex-1">
                    <span className="font-mono text-[10px] text-slate-400 block">
                      {item.productSkuSnapshot || `Product #${item.productId}`}
                    </span>
                    <h3 className="font-bold text-slate-900 text-sm">{item.productNameSnapshot}</h3>
                    <p className="text-slate-500 mt-0.5">
                      {formatCurrency(item.unitPriceSnapshot)} × {item.quantity} unit
                      {item.quantity > 1 ? 's' : ''}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="text-sm font-extrabold text-slate-900 block">
                      {formatCurrency(item.subtotal)}
                    </span>
                  </div>
                </div>
              ))}
            </div>

            {/* Financial Summary */}
            <div className="pt-4 border-t border-slate-100 space-y-2 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Items Subtotal</span>
                <span className="font-semibold text-slate-900">{formatCurrency(order.subtotalAmount)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Shipping Fee</span>
                <span className="font-semibold text-slate-900">{formatCurrency(order.shippingFee)}</span>
              </div>
              <div className="border-t border-slate-100 pt-3 flex justify-between items-baseline">
                <span className="text-sm font-bold text-slate-900">Total Paid / Due</span>
                <span className="text-xl font-black text-slate-900">{formatCurrency(order.totalAmount)}</span>
              </div>
            </div>
          </div>

          {/* Order & Delivery Guarantee */}
          <div className="bg-slate-50 rounded-2xl border border-slate-200 p-5 text-xs space-y-2">
            <h4 className="font-bold text-slate-800 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-600" />
              Order & Delivery Guarantee
            </h4>
            <div className="text-slate-600 space-y-1.5 text-xs">
              <p className="flex items-center gap-1.5 text-slate-700">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                <span>100% Genuine & Authentic Products</span>
              </p>
              <p className="flex items-center gap-1.5 text-slate-700">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                <span>7-Day Hassle-Free Returns & Replacement</span>
              </p>
              <p className="flex items-center gap-1.5 text-slate-700">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                <span>Zero Hidden Fees • 256-Bit SSL Encrypted Payment</span>
              </p>
              {order.sagaFailureReason && (
                <div className="mt-2 p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-700">
                  <span className="font-semibold block">Order Status Notice:</span>
                  <span>{order.sagaFailureReason}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right Col: Delivery & Payment Details */}
        <div className="space-y-6">
          {/* Shipping Address Box */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-3">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Truck className="w-4 h-4 text-indigo-600" />
              Delivery Destination
            </h3>
            <div className="text-xs text-slate-600 space-y-1">
              <p className="font-bold text-slate-900">{order.shippingName}</p>
              <p className="font-mono text-slate-500">{maskPhone(order.shippingPhone)}</p>
              <p className="leading-relaxed whitespace-pre-line pt-1 text-slate-700">
                {order.shippingAddress}
              </p>
            </div>
          </div>

          {/* Payment Status Box */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-3">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <CreditCard className="w-4 h-4 text-indigo-600" />
              Payment Summary
            </h3>
            <div className="text-xs space-y-2">
              <div className="flex justify-between">
                <span className="text-slate-500">Method</span>
                <span className="font-bold text-slate-900">
                  {order.payment?.paymentMethod || 'WALLET'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Payment Status</span>
                <span className="font-semibold text-emerald-600">
                  {order.payment?.paymentStatus || 'COMPLETED'}
                </span>
              </div>
              {order.payment?.paymentReference && (
                <div className="pt-2 border-t border-slate-100">
                  <span className="text-[10px] text-slate-400 block uppercase font-bold">Reference</span>
                  <span className="font-mono text-[11px] text-slate-600 break-all">
                    {order.payment.paymentReference}
                  </span>
                </div>
              )}
              {order.payment?.gatewayPaymentId && (
                <div className="pt-1.5 flex justify-between items-center">
                  <span className="text-[10px] text-slate-400 block uppercase font-bold">Gateway Payment ID</span>
                  <span className="font-mono text-[11px] text-indigo-600 font-bold">
                    {order.payment.gatewayPaymentId}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modal for Order Cancellation */}
      <ConfirmationModal
        isOpen={cancelModalOpen}
        title={`Cancel Order #${order.orderNumber}?`}
        message="Are you sure you want to cancel this order? Reserved items will be released back to the catalog, and any wallet payment will be automatically refunded."
        confirmText="Yes, Cancel Order"
        cancelText="Keep Order"
        isDestructive={true}
        isLoading={isCancelling}
        onConfirm={handleCancelOrder}
        onCancel={() => setCancelModalOpen(false)}
      />

      {/* Dispute Modal */}
      {disputeModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-slate-100 space-y-5">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">Raise Dispute for Order #{order.orderNumber}</h3>
                <p className="text-xs text-slate-500">Submit a claim to request a merchant review or admin refund</p>
              </div>
            </div>

            <div className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 mb-1.5">Issue Reason *</label>
                <select
                  value={disputeReason}
                  onChange={(e) => setDisputeReason(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white"
                >
                  <option value="Product Damaged in Transit">Product Damaged in Transit</option>
                  <option value="Incorrect Item Received">Incorrect Item Received</option>
                  <option value="Package Not Received / Delivery Issue">Package Not Received / Delivery Issue</option>
                  <option value="Defective or Quality Mismatch">Defective or Quality Mismatch</option>
                  <option value="Merchant SLA Breached">Merchant SLA Breached</option>
                  <option value="Other Issue">Other Issue</option>
                </select>
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1.5">Additional Details / Comments</label>
                <textarea
                  rows={3}
                  value={disputeComment}
                  onChange={(e) => setDisputeComment(e.target.value)}
                  placeholder="Explain the problem in detail (e.g., package arrived torn, item missing parts)..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
                />
              </div>

              <div>
                <label className="block font-bold text-slate-700 mb-1">Claim Refund Amount</label>
                <div className="text-sm font-extrabold text-slate-900 p-2.5 bg-slate-50 rounded-xl border border-slate-200">
                  {formatCurrency(order.totalAmount)}
                </div>
              </div>

              <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-[11px] text-amber-800 space-y-1">
                <span className="font-bold block">How SLA & Resolution works:</span>
                <p>1. The merchant is notified and has <strong>2 minutes</strong> to accept (full refund) or reject.</p>
                <p>2. If rejected or timed out, it automatically escalates to the <strong>Platform Admin</strong> for final resolution.</p>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setDisputeModalOpen(false)}
                disabled={isRaisingDispute}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleRaiseDispute}
                disabled={isRaisingDispute}
                className="px-5 py-2.5 rounded-xl text-xs font-bold text-white bg-amber-600 hover:bg-amber-700 transition shadow-xs disabled:opacity-50 flex items-center gap-1.5"
              >
                {isRaisingDispute ? (
                  <span>Submitting...</span>
                ) : (
                  <>
                    <AlertTriangle className="w-3.5 h-3.5" />
                    <span>Submit Dispute</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

