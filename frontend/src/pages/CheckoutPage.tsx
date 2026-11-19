import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  CreditCard,
  Truck,
  ShieldCheck,
  AlertCircle,
  ArrowRight,
  Wallet as WalletIcon,
  CheckCircle2,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';
import { useWallet } from '../context/WalletContext';
import { useNotification } from '../context/NotificationContext';
import { orderApi } from '../api/orderApi';
import { CheckoutPayload } from '../types';
import { formatCurrency } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const CheckoutPage: React.FC = () => {
  const { user } = useAuth();
  const { cart, items, cartSubtotal, itemCount, fetchCart } = useCart();
  const { balance, fetchWallet } = useWallet();
  const { showSuccess, showError } = useNotification();
  const navigate = useNavigate();

  const [shippingName, setShippingName] = useState(user?.fullName || '');
  const [shippingPhone, setShippingPhone] = useState(user?.phone || '');
  const [shippingAddress, setShippingAddress] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'WALLET' | 'CASH_ON_DELIVERY' | 'RAZORPAY'>('WALLET');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState<{ [key: string]: string }>({});

  const loadRazorpayScript = (): Promise<boolean> => {
    return new Promise((resolve) => {
      if ((window as any).Razorpay) {
        resolve(true);
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  useEffect(() => {
    if (user?.fullName && !shippingName) setShippingName(user.fullName);
    if (user?.phone && !shippingPhone) setShippingPhone(user.phone);
  }, [user, shippingName, shippingPhone]);

  // If cart is empty, redirect back to cart
  if (!items || items.length === 0) {
    return (
      <div className="max-w-xl mx-auto my-16 bg-white p-8 rounded-2xl border border-slate-200 text-center shadow-xs">
        <p className="text-sm font-bold text-slate-700">Your cart is empty.</p>
        <Link
          to="/products"
          className="mt-4 inline-block px-4 py-2 bg-indigo-600 text-white text-xs font-semibold rounded-xl hover:bg-indigo-700 transition"
        >
          Browse Products
        </Link>
      </div>
    );
  }

  const validate = () => {
    const errors: { [key: string]: string } = {};
    if (!shippingName.trim()) errors.shippingName = 'Recipient name is required';
    if (!shippingPhone.trim()) {
      errors.shippingPhone = 'Phone number is required';
    } else if (shippingPhone.replace(/\D/g, '').length < 10) {
      errors.shippingPhone = 'Please enter a valid 10-digit phone number';
    }
    if (!shippingAddress.trim() || shippingAddress.trim().length < 10) {
      errors.shippingAddress = 'Please provide a complete delivery address (min 10 characters)';
    }

    if (paymentMethod === 'WALLET' && balance < cartSubtotal) {
      errors.payment = `Insufficient wallet balance. You need ${formatCurrency(
        cartSubtotal - balance
      )} more. Please top up your wallet or select Cash on Delivery.`;
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    if (paymentMethod === 'RAZORPAY') {
      setIsSubmitting(true);
      try {
        const loaded = await loadRazorpayScript();
        if (!loaded) {
          showError('Failed to load Razorpay payment SDK. Please check your internet connection.');
          setIsSubmitting(false);
          return;
        }

        const idempotencyKey = `ORD-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
        const intentRes = await orderApi.createRazorpayOrder({
          shippingName: shippingName.trim(),
          shippingPhone: shippingPhone.trim(),
          shippingAddress: shippingAddress.trim(),
          idempotencyKey,
        });

        if (!intentRes.success || !intentRes.data) {
          showError(intentRes.message || 'Failed to initialize payment gateway order');
          setIsSubmitting(false);
          return;
        }

        const intent = intentRes.data;

        // Open Official Razorpay Checkout Modal
        const options = {
          key: intent.keyId,
          amount: intent.amountInPaise,
          currency: intent.currency || 'INR',
          name: 'E-ShoppingZone',
          description: `Order #${intent.orderNumber}`,
          order_id: intent.razorpayOrderId.startsWith('order_sim_') ? undefined : intent.razorpayOrderId,
          prefill: {
            name: shippingName.trim(),
            email: user?.email || '',
            contact: shippingPhone.trim(),
          },
          theme: {
            color: '#4f46e5',
          },
          modal: {
            ondismiss: () => {
              setIsSubmitting(false);
              showError('Payment process was cancelled by customer');
            },
          },
          handler: async (response: any) => {
            try {
              const verifyRes = await orderApi.verifyRazorpayPayment({
                razorpayOrderId: response.razorpay_order_id || intent.razorpayOrderId,
                razorpayPaymentId: response.razorpay_payment_id || `pay_sim_${Date.now()}`,
                razorpaySignature: response.razorpay_signature || 'mock_valid_signature',
                orderNumber: intent.orderNumber,
                shippingName: shippingName.trim(),
                shippingPhone: shippingPhone.trim(),
                shippingAddress: shippingAddress.trim(),
                idempotencyKey,
              });

              if (verifyRes.success && verifyRes.data) {
                showSuccess(`Payment verified! Order #${verifyRes.data.orderNumber} placed successfully.`);
                await Promise.all([fetchCart(), fetchWallet()]);
                navigate(`/orders/${verifyRes.data.id}`);
              } else {
                showError(verifyRes.message || 'Payment signature verification failed');
              }
            } catch (err: any) {
              showError(err.response?.data?.message || err.message || 'Payment verification failed');
            } finally {
              setIsSubmitting(false);
            }
          },
        };

        const razorpayInstance = new (window as any).Razorpay(options);
        razorpayInstance.on('payment.failed', (resp: any) => {
          showError(`Payment failed: ${resp.error?.description || 'Transaction declined'}`);
          setIsSubmitting(false);
        });
        razorpayInstance.open();
      } catch (err: any) {
        showError(err.response?.data?.message || err.message || 'Failed to initialize payment');
        setIsSubmitting(false);
      }
      return;
    }

    setIsSubmitting(true);
    try {
      const idempotencyKey = `ORD-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
      const payload: CheckoutPayload = {
        idempotencyKey,
        paymentMethod,
        shippingName: shippingName.trim(),
        shippingPhone: shippingPhone.trim(),
        shippingAddress: shippingAddress.trim(),
      };

      const res = await orderApi.checkout(payload);

      if (res.success && res.data) {
        showSuccess(`Order #${res.data.orderNumber} placed successfully!`);
        // Refresh cart and wallet balance in background
        await Promise.all([fetchCart(), fetchWallet()]);
        // Navigate to order detail page
        navigate(`/orders/${res.data.id}`);
      } else {
        showError(res.message || 'Checkout failed. Please try again.');
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      const msg =
        errorResponse.response?.data?.message ||
        errorResponse.message ||
        'Order placement failed. If your payment was deducted, it will be automatically reconciled.';
      showError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const isWalletInsufficient = paymentMethod === 'WALLET' && balance < cartSubtotal;

  return (
    <div className="space-y-8 pb-16">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">Checkout</h1>
        <p className="text-xs text-slate-500 mt-1">
          Complete your delivery details and choose your preferred payment option
        </p>
      </div>

      <form onSubmit={handlePlaceOrder} className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Shipping & Payment Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Shipping Address Section */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-4">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Truck className="w-5 h-5 text-indigo-600" />
              1. Delivery Address
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="shippingNameInput" className="block text-xs font-semibold text-slate-700 mb-1">
                  Recipient Full Name *
                </label>
                <input
                  id="shippingNameInput"
                  type="text"
                  value={shippingName}
                  onChange={(e) => setShippingName(e.target.value)}
                  placeholder="e.g. Alice Sharma"
                  className={`w-full px-3.5 py-2.5 text-xs bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 ${formErrors.shippingName ? 'border-rose-400' : 'border-slate-200'
                    }`}
                />
                {formErrors.shippingName && (
                  <p className="text-[11px] text-rose-600 mt-1">{formErrors.shippingName}</p>
                )}
              </div>

              <div>
                <label htmlFor="shippingPhoneInput" className="block text-xs font-semibold text-slate-700 mb-1">
                  Contact Phone Number *
                </label>
                <input
                  id="shippingPhoneInput"
                  type="tel"
                  value={shippingPhone}
                  onChange={(e) => setShippingPhone(e.target.value)}
                  placeholder="e.g. 9876543210"
                  className={`w-full px-3.5 py-2.5 text-xs bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 ${formErrors.shippingPhone ? 'border-rose-400' : 'border-slate-200'
                    }`}
                />
                {formErrors.shippingPhone && (
                  <p className="text-[11px] text-rose-600 mt-1">{formErrors.shippingPhone}</p>
                )}
              </div>

              <div className="sm:col-span-2">
                <label htmlFor="shippingAddressInput" className="block text-xs font-semibold text-slate-700 mb-1">
                  Street Address & Pincode *
                </label>
                <textarea
                  id="shippingAddressInput"
                  rows={3}
                  value={shippingAddress}
                  onChange={(e) => setShippingAddress(e.target.value)}
                  placeholder="Apartment / Flat, Street, Area, City, State - PIN Code"
                  className={`w-full px-3.5 py-2.5 text-xs bg-slate-50 border rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 ${formErrors.shippingAddress ? 'border-rose-400' : 'border-slate-200'
                    }`}
                />
                {formErrors.shippingAddress && (
                  <p className="text-[11px] text-rose-600 mt-1">{formErrors.shippingAddress}</p>
                )}
              </div>
            </div>
          </div>

          {/* Payment Method Section */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-4">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <CreditCard className="w-5 h-5 text-indigo-600" />
              2. Payment Method
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {/* Razorpay Gateway Option */}
              <div
                onClick={() => setPaymentMethod('RAZORPAY')}
                className={`cursor-pointer p-4 rounded-2xl border-2 transition-all flex flex-col justify-between ${paymentMethod === 'RAZORPAY'
                    ? 'border-indigo-600 bg-indigo-50/40 shadow-xs'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                  }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <CreditCard className="w-5 h-5 text-indigo-600" />
                      <span className="font-bold text-sm text-slate-900">Razorpay</span>
                    </div>
                    {paymentMethod === 'RAZORPAY' && (
                      <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                    )}
                  </div>
                  <p className="text-xs text-slate-500">Cards, Netbanking & Wallets</p>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-[11px] text-slate-500">Gateway:</span>
                  <span className="text-[11px] font-bold text-indigo-600 px-2 py-0.5 rounded-md bg-indigo-50">
                    Instant & Secure
                  </span>
                </div>
              </div>

              {/* E-ShoppingZone Wallet Option */}
              <div
                onClick={() => setPaymentMethod('WALLET')}
                className={`cursor-pointer p-4 rounded-2xl border-2 transition-all flex flex-col justify-between ${paymentMethod === 'WALLET'
                    ? 'border-indigo-600 bg-indigo-50/40 shadow-xs'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                  }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <WalletIcon className="w-5 h-5 text-indigo-600" />
                      <span className="font-bold text-sm text-slate-900">Wallet</span>
                    </div>
                    {paymentMethod === 'WALLET' && (
                      <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                    )}
                  </div>
                  <p className="text-xs text-slate-500">Instant 1-step deduction with ledger audit</p>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-[11px] text-slate-500">Balance:</span>
                  <span
                    className={`text-xs font-extrabold ${balance >= cartSubtotal ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                  >
                    {formatCurrency(balance)}
                  </span>
                </div>
              </div>

              {/* Cash on Delivery Option */}
              <div
                onClick={() => setPaymentMethod('CASH_ON_DELIVERY')}
                className={`cursor-pointer p-4 rounded-2xl border-2 transition-all flex flex-col justify-between ${paymentMethod === 'CASH_ON_DELIVERY'
                    ? 'border-indigo-600 bg-indigo-50/40 shadow-xs'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                  }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <Truck className="w-5 h-5 text-indigo-600" />
                      <span className="font-bold text-sm text-slate-900">Cash on Delivery</span>
                    </div>
                    {paymentMethod === 'CASH_ON_DELIVERY' && (
                      <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                    )}
                  </div>
                  <p className="text-xs text-slate-500">Pay cash upon package arrival at doorstep</p>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-[11px] text-slate-500">Availability:</span>
                  <span className="text-xs font-semibold text-emerald-600">Eligible</span>
                </div>
              </div>
            </div>

            {/* Wallet Insufficient Warning */}
            {isWalletInsufficient && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3 text-xs text-amber-800">
                <AlertCircle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="font-semibold">Insufficient wallet balance</p>
                  <p className="text-[11px] mt-0.5">
                    Your wallet has {formatCurrency(balance)}, but the total is{' '}
                    {formatCurrency(cartSubtotal)}. Top up your wallet or switch to Cash on Delivery.
                  </p>
                  <Link
                    to="/wallet"
                    className="inline-block mt-2 text-indigo-600 hover:text-indigo-800 font-bold underline"
                  >
                    Top up wallet now &rarr;
                  </Link>
                </div>
              </div>
            )}

            {formErrors.payment && (
              <p className="text-xs text-rose-600 font-medium">{formErrors.payment}</p>
            )}
          </div>
        </div>

        {/* Right Col: Order Items Review & CTA */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-4">
            <h3 className="text-base font-black text-slate-900 pb-2 border-b border-slate-100">
              Items in Order ({itemCount})
            </h3>

            <div className="max-h-60 overflow-y-auto space-y-2.5 pr-1 divide-y divide-slate-50">
              {items.map((item) => (
                <div key={item.itemId} className="pt-2 flex items-center justify-between gap-3 text-xs">
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold text-slate-800 truncate">{item.productName}</p>
                    <p className="text-slate-400 text-[11px]">
                      Qty: {item.quantity} × {formatCurrency(item.unitPrice)}
                    </p>
                  </div>
                  <span className="font-bold text-slate-900 flex-shrink-0">
                    {formatCurrency(item.itemSubtotal)}
                  </span>
                </div>
              ))}
            </div>

            <div className="pt-4 border-t border-slate-100 space-y-2 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Items Subtotal</span>
                <span className="font-semibold text-slate-900">{formatCurrency(cartSubtotal)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Shipping Fee</span>
                <span className="font-semibold text-emerald-600">FREE</span>
              </div>
              <div className="border-t border-slate-100 pt-3 flex justify-between items-baseline">
                <span className="text-sm font-bold text-slate-900">Total Amount</span>
                <span className="text-xl font-black text-slate-900">{formatCurrency(cartSubtotal)}</span>
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-indigo-100"
            >
              {isSubmitting ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Confirming Your Order...</span>
                </>
              ) : (
                <>
                  <span>Place Order ({formatCurrency(cartSubtotal)})</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>

            <div className="text-center pt-2">
              <span className="text-[11px] text-slate-400 flex items-center justify-center gap-1 font-medium">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
                Instant Order Confirmation • Zero Double-Charges
              </span>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
};

