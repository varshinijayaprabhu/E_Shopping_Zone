import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, Trash2, ArrowRight, ArrowLeft, Package, AlertCircle } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { formatCurrency } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { ConfirmationModal } from '../components/common/ConfirmationModal';

export const CartPage: React.FC = () => {
  const { cart, items, cartSubtotal, itemCount, isLoading, updateQuantity, removeItem, clearCart } = useCart();
  const navigate = useNavigate();

  const [confirmClearOpen, setConfirmClearOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState<number | null>(null);

  const handleUpdate = async (itemId: number, newQty: number) => {
    if (newQty < 1) return;
    setIsUpdating(itemId);
    try {
      await updateQuantity(itemId, newQty);
    } finally {
      setIsUpdating(null);
    }
  };

  const handleRemove = async (itemId: number) => {
    setIsUpdating(itemId);
    try {
      await removeItem(itemId);
    } finally {
      setIsUpdating(null);
    }
  };

  const handleConfirmClear = async () => {
    await clearCart();
    setConfirmClearOpen(false);
  };

  if (isLoading && !cart) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Loading shopping cart..." />
      </div>
    );
  }

  if (!items || items.length === 0) {
    return (
      <div className="max-w-2xl mx-auto my-16 bg-white p-12 rounded-3xl border border-slate-200 text-center shadow-xs space-y-4">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-full flex items-center justify-center mx-auto mb-2">
          <ShoppingCart className="w-8 h-8" />
        </div>
        <h2 className="text-2xl font-black text-slate-900">Your Cart is Empty</h2>
        <p className="text-sm text-slate-500 max-w-md mx-auto">
          Explore our vast catalog of electronics, smartphones, fashion, and home products.
        </p>
        <div className="pt-4">
          <Link
            to="/products"
            className="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white font-bold text-sm rounded-xl hover:bg-indigo-700 transition shadow-md shadow-indigo-100"
          >
            <span>Start Shopping</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">Shopping Cart</h1>
          <p className="text-xs text-slate-500 mt-1">Review your selected items before proceeding to checkout</p>
        </div>
        <button
          type="button"
          onClick={() => setConfirmClearOpen(true)}
          className="text-xs font-semibold text-rose-600 hover:text-rose-800 flex items-center gap-1 self-start sm:self-auto"
        >
          <Trash2 className="w-4 h-4" />
          <span>Clear entire cart</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left: Cart Items List */}
        <div className="lg:col-span-2 space-y-4">
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs divide-y divide-slate-100">
            {items.map((item) => (
              <div key={item.itemId} className="p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center gap-4">
                {/* Item Image */}
                <div className="w-20 h-20 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0 border border-slate-100">
                  {item.imageUrl ? (
                    <img
                      src={item.imageUrl}
                      alt={item.productName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-slate-400">
                      <Package className="w-8 h-8" />
                    </div>
                  )}
                </div>

                {/* Item Details */}
                <div className="flex-1 min-w-0">
                  <span className="text-[10px] font-mono text-slate-400 block">{item.productSku}</span>
                  <Link
                    to={`/products/${item.productId}`}
                    className="text-sm font-bold text-slate-900 hover:text-indigo-600 transition-colors line-clamp-1"
                  >
                    {item.productName}
                  </Link>
                  <span className="text-xs font-semibold text-slate-700 mt-1 block">
                    {formatCurrency(item.unitPrice)} each
                  </span>

                  {!item.inStock && (
                    <span className="mt-1 inline-flex items-center gap-1 text-[11px] font-bold text-rose-600">
                      <AlertCircle className="w-3.5 h-3.5" />
                      Out of stock or unavailable
                    </span>
                  )}
                </div>

                {/* Quantity Controls */}
                <div className="flex items-center gap-3">
                  <div className="flex items-center border border-slate-200 rounded-xl overflow-hidden bg-slate-50">
                    <button
                      type="button"
                      disabled={item.quantity <= 1 || isUpdating === item.itemId}
                      onClick={() => handleUpdate(item.itemId, item.quantity - 1)}
                      className="px-2.5 py-1 text-xs font-bold text-slate-600 hover:bg-slate-200 transition disabled:opacity-30"
                    >
                      -
                    </button>
                    <span className="px-3 py-1 text-xs font-bold text-slate-800 bg-white min-w-[32px] text-center">
                      {item.quantity}
                    </span>
                    <button
                      type="button"
                      disabled={isUpdating === item.itemId}
                      onClick={() => handleUpdate(item.itemId, item.quantity + 1)}
                      className="px-2.5 py-1 text-xs font-bold text-slate-600 hover:bg-slate-200 transition disabled:opacity-30"
                    >
                      +
                    </button>
                  </div>

                  {/* Subtotal */}
                  <div className="text-right min-w-[80px]">
                    <span className="text-sm font-extrabold text-slate-900 block">
                      {formatCurrency(item.itemSubtotal)}
                    </span>
                  </div>

                  {/* Remove Button */}
                  <button
                    type="button"
                    disabled={isUpdating === item.itemId}
                    onClick={() => handleRemove(item.itemId)}
                    className="p-2 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors disabled:opacity-30"
                    title="Remove item"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <Link
            to="/products"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Continue Shopping</span>
          </Link>
        </div>

        {/* Right: Order Summary */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-6">
            <h3 className="text-base font-black text-slate-900 pb-3 border-b border-slate-100">
              Order Summary
            </h3>

            <div className="space-y-3 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Items Subtotal ({itemCount} items)</span>
                <span className="font-semibold text-slate-900">{formatCurrency(cartSubtotal)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Estimated Shipping</span>
                <span className="font-semibold text-emerald-600">FREE</span>
              </div>
              <div className="border-t border-slate-100 pt-3 flex justify-between items-baseline">
                <span className="text-sm font-bold text-slate-900">Total Amount</span>
                <span className="text-xl font-black text-slate-900">{formatCurrency(cartSubtotal)}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => navigate('/checkout')}
              className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition shadow-lg shadow-indigo-100"
            >
              <span>Proceed to Checkout</span>
              <ArrowRight className="w-4 h-4" />
            </button>

            <div className="pt-2 text-center">
              <span className="text-[11px] text-slate-400 font-medium">
                100% Safe & Secure Checkout • Encrypted & Verified
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modal for Clearing Cart */}
      <ConfirmationModal
        isOpen={confirmClearOpen}
        title="Clear Entire Cart?"
        message="Are you sure you want to remove all items from your shopping cart? This action cannot be undone."
        confirmText="Clear Cart"
        cancelText="Keep Items"
        isDestructive={true}
        onConfirm={handleConfirmClear}
        onCancel={() => setConfirmClearOpen(false)}
      />
    </div>
  );
};

