import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  ShoppingCart,
  ArrowLeft,
  Package,
  ShieldCheck,
  Truck,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  Store,
} from 'lucide-react';
import { productApi } from '../api/productApi';
import { Product } from '../types';
import { formatCurrency } from '../utils/formatters';
import { resolveImageUrl, getProductFallbackSvg } from '../utils/imageUrl';
import { useCart } from '../context/CartContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const ProductDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToCart } = useCart();

  const [product, setProduct] = useState<Product | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [isAdding, setIsAdding] = useState(false);
  const [imageError, setImageError] = useState(false);

  useEffect(() => {
    if (!id || isNaN(Number(id))) {
      setError('Invalid product ID');
      setIsLoading(false);
      return;
    }

    productApi
      .getProductById(Number(id))
      .then((res) => {
        if (res.success && res.data) {
          setProduct(res.data);
        } else {
          setError(res.message || 'Product not found');
        }
      })
      .catch((err) => {
        setError(err.response?.data?.message || 'Failed to load product details');
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [id]);

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Loading product details..." />
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="max-w-xl mx-auto my-16 bg-white p-8 rounded-2xl border border-slate-200 text-center shadow-xs">
        <div className="w-12 h-12 rounded-full bg-rose-50 text-rose-500 mx-auto flex items-center justify-center mb-3">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-xl font-bold text-slate-800">Product Not Found</h2>
        <p className="mt-2 text-xs text-slate-500">{error || 'This product does not exist or has been deactivated.'}</p>
        <Link
          to="/products"
          className="mt-6 inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white text-xs font-semibold rounded-xl hover:bg-indigo-700 transition"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Catalog</span>
        </Link>
      </div>
    );
  }

  const isOutOfStock = product.availableStock <= 0;
  const maxPurchaseQty = Math.min(product.availableStock, 10);

  const handleAddToCart = async () => {
    if (isOutOfStock) return;
    setIsAdding(true);
    try {
      await addToCart(product.id, quantity);
    } finally {
      setIsAdding(false);
    }
  };

  const handleBuyNow = async () => {
    if (isOutOfStock) return;
    setIsAdding(true);
    try {
      const ok = await addToCart(product.id, quantity);
      if (ok) {
        navigate('/cart');
      }
    } finally {
      setIsAdding(false);
    }
  };

  return (
    <div className="space-y-8 pb-16">
      {/* Back Button */}
      <div>
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to products
        </button>
      </div>

      {/* Main Product Card */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-10 shadow-xs grid grid-cols-1 lg:grid-cols-2 gap-10">
        {/* Left: Product Image */}
        <div className="space-y-4">
          <div className="relative aspect-square rounded-2xl overflow-hidden bg-slate-100 border border-slate-100">
            {resolveImageUrl(product.imageUrl) && !imageError ? (
              <img
                src={resolveImageUrl(product.imageUrl)}
                alt={product.name}
                referrerPolicy="no-referrer"
                onError={() => setImageError(true)}
                className="w-full h-full object-cover object-center"
              />
            ) : (
              <img
                src={getProductFallbackSvg(product.name, product.categoryName)}
                alt={product.name}
                className="w-full h-full object-cover object-center"
              />
            )}

            <div className="absolute top-4 left-4">
              <span className="px-3 py-1 text-xs font-bold rounded-lg bg-slate-900/80 text-white backdrop-blur-xs">
                {product.categoryName}
              </span>
            </div>
          </div>
        </div>

        {/* Right: Info & Actions */}
        <div className="flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <span className="text-xs font-mono text-slate-400">SKU: {product.sku}</span>
              <span className="text-slate-300">•</span>
              <span className="text-xs text-indigo-600 font-semibold flex items-center gap-1">
                <Store className="w-3.5 h-3.5" />
                Merchant ID #{product.merchantId}
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight leading-snug">
              {product.name}
            </h1>

            {/* Price block */}
            <div className="pt-2">
              <span className="text-3xl sm:text-4xl font-extrabold text-slate-900">
                {formatCurrency(product.price)}
              </span>
              <span className="text-xs text-slate-400 block mt-1">Inclusive of all taxes</span>
            </div>

            {/* Stock indicator */}
            <div className="pt-1">
              {isOutOfStock ? (
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  <span>Out of Stock</span>
                </div>
              ) : (
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>In Stock ({product.availableStock} units available)</span>
                </div>
              )}
            </div>

            {/* Description */}
            <div className="pt-3">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">Description</h3>
              <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-line">
                {product.description}
              </p>
            </div>
          </div>

          {/* Quantity & CTA */}
          <div className="pt-6 border-t border-slate-100 space-y-4">
            {!isOutOfStock && (
              <div className="flex items-center gap-4">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-700">Quantity</span>
                <div className="flex items-center border border-slate-200 rounded-xl overflow-hidden bg-slate-50">
                  <button
                    type="button"
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    disabled={quantity <= 1}
                    className="px-3 py-1.5 text-sm font-bold text-slate-600 hover:bg-slate-200 transition disabled:opacity-40"
                  >
                    -
                  </button>
                  <span className="px-4 py-1.5 text-xs font-bold text-slate-800 bg-white min-w-[40px] text-center">
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((q) => Math.min(maxPurchaseQty, q + 1))}
                    disabled={quantity >= maxPurchaseQty}
                    className="px-3 py-1.5 text-sm font-bold text-slate-600 hover:bg-slate-200 transition disabled:opacity-40"
                  >
                    +
                  </button>
                </div>
                <span className="text-[11px] text-slate-400">(Max 10 per order)</span>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              <button
                type="button"
                onClick={handleAddToCart}
                disabled={isOutOfStock || isAdding}
                className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 disabled:cursor-not-allowed shadow-md shadow-indigo-100"
              >
                <ShoppingCart className="w-4 h-4" />
                <span>{isAdding ? 'Adding...' : 'Add to Cart'}</span>
              </button>

              <button
                type="button"
                onClick={handleBuyNow}
                disabled={isOutOfStock || isAdding}
                className="w-full inline-flex items-center justify-center gap-2 px-6 py-3.5 rounded-xl font-bold text-sm bg-slate-900 text-white hover:bg-black active:scale-95 transition disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span>Buy Now</span>
              </button>
            </div>

            {/* Trust Assurances */}
            <div className="grid grid-cols-3 gap-2 pt-4 border-t border-slate-100 text-center">
              <div className="p-2">
                <Truck className="w-5 h-5 mx-auto text-indigo-600 mb-1" />
                <span className="text-[11px] font-semibold text-slate-700 block">Fast Shipping</span>
                <span className="text-[10px] text-slate-400">Within 48 hours</span>
              </div>
              <div className="p-2">
                <RotateCcw className="w-5 h-5 mx-auto text-indigo-600 mb-1" />
                <span className="text-[11px] font-semibold text-slate-700 block">Easy Returns</span>
                <span className="text-[10px] text-slate-400">7-Day Guarantee</span>
              </div>
              <div className="p-2">
                <ShieldCheck className="w-5 h-5 mx-auto text-indigo-600 mb-1" />
                <span className="text-[11px] font-semibold text-slate-700 block">Secure Payment</span>
                <span className="text-[10px] text-slate-400">100% Protected</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

