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
} from 'lucide-react';
import { orderApi } from '../api/orderApi';
import { Order } from '../types';
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

  const fetchOrder = useCallback(async () => {
    if (!id || isNaN(Number(id))) {
      setError('Invalid order ID');
      setIsLoading(false);
      return;
    }
    try {
      const res = await orderApi.getOrderById(Number(id));
      if (res.success && res.data) {
        setOrder(res.data);
      } else {
        setError(res.message || 'Order not found');
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
      </div>

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
    </div>
  );
};

