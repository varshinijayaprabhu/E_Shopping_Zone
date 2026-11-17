import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Store, Package, ShoppingBag, Plus, ArrowRight, TrendingUp } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { orderApi } from '../../api/orderApi';
import { Product, MerchantOrderItem } from '../../types';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const MerchantDashboardPage: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [orders, setOrders] = useState<MerchantOrderItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    const loadMerchantData = async () => {
      try {
        const [prodRes, ordRes] = await Promise.all([
          productApi.getMerchantProducts(0, 10),
          orderApi.getMerchantOrders(0, 10),
        ]);

        if (isMounted) {
          if (prodRes.success && prodRes.data?.content) {
            setProducts(prodRes.data.content);
          }
          if (ordRes.success && ordRes.data?.content) {
            setOrders(ordRes.data.content);
          }
        }
      } catch {
        // Continue with empty states if any fails
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    loadMerchantData();
    return () => {
      isMounted = false;
    };
  }, []);

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Loading merchant console..." />
      </div>
    );
  }

  // Calculate gross sales from merchant orders
  const grossSales = orders.reduce((sum, item) => sum + (item.subtotal || 0), 0);
  const totalStockUnits = products.reduce((sum, p) => sum + (p.availableStock || 0), 0);

  return (
    <div className="space-y-8 pb-16">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-xl bg-indigo-50 text-indigo-600">
              <Store className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Merchant Console</h1>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Manage your store catalog, monitor inventory levels and fulfill incoming orders
          </p>
        </div>

        <Link
          to="/merchant/products/new"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 text-white text-xs font-bold rounded-xl hover:bg-indigo-700 transition shadow-md shadow-indigo-100 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Product</span>
        </Link>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center flex-shrink-0">
            <Package className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Active Catalog Items
            </span>
            <span className="text-2xl font-black text-slate-900">{products.length} Products</span>
            <span className="text-[11px] text-slate-500 block mt-0.5">
              {totalStockUnits} total units in stock
            </span>
          </div>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Gross Order Value
            </span>
            <span className="text-2xl font-black text-emerald-600">
              {formatCurrency(grossSales)}
            </span>
            <span className="text-[11px] text-slate-500 block mt-0.5">From recent purchases</span>
          </div>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0">
            <ShoppingBag className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Merchant Order Items
            </span>
            <span className="text-2xl font-black text-slate-900">{orders.length} Fulfilled</span>
            <span className="text-[11px] text-slate-500 block mt-0.5">Completed & verified sales</span>
          </div>
        </div>
      </div>

      {/* 2-Column: Quick Product Overview & Recent Orders */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left: Your Products */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Package className="w-4 h-4 text-indigo-600" />
              Your Products
            </h2>
            <Link
              to="/merchant/products"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              <span>Manage all</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {products.length === 0 ? (
            <div className="py-8 text-center text-slate-500 text-xs">
              You haven't listed any products yet.
            </div>
          ) : (
            <div className="space-y-3">
              {products.slice(0, 5).map((p) => (
                <div
                  key={p.id}
                  className="p-3 bg-slate-50 rounded-xl flex items-center justify-between gap-3 text-xs"
                >
                  <div className="min-w-0 flex-1">
                    <p className="font-bold text-slate-900 truncate">{p.name}</p>
                    <p className="text-slate-400 text-[10px] font-mono">{p.sku}</p>
                  </div>
                  <div className="text-right">
                    <span className="font-bold text-slate-900 block">{formatCurrency(p.price)}</span>
                    <span
                      className={`text-[10px] font-semibold ${p.availableStock > 0 ? 'text-emerald-600' : 'text-rose-600'
                        }`}
                    >
                      {p.availableStock} in stock
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right: Recent Orders */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <ShoppingBag className="w-4 h-4 text-indigo-600" />
              Recent Customer Orders
            </h2>
            <Link
              to="/merchant/orders"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              <span>View all</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {orders.length === 0 ? (
            <div className="py-8 text-center text-slate-500 text-xs">
              No orders have been placed for your products yet.
            </div>
          ) : (
            <div className="space-y-3">
              {orders.slice(0, 5).map((o) => (
                <div
                  key={o.orderItemId}
                  className="p-3 bg-slate-50 rounded-xl flex items-center justify-between gap-3 text-xs"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-slate-900">#{o.orderNumber}</span>
                      <span className="text-[10px] text-slate-400">{formatDate(o.orderCreatedAt)}</span>
                    </div>
                    <p className="text-slate-700 font-medium truncate mt-0.5">
                      {o.productNameSnapshot} (×{o.quantity})
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="font-extrabold text-slate-900 block">
                      {formatCurrency(o.subtotal)}
                    </span>
                    <span className="text-[10px] font-bold text-indigo-600 uppercase">
                      {o.orderStatus}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

