import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ShieldCheck, Zap, Truck, CreditCard, Sparkles } from 'lucide-react';
import { productApi } from '../api/productApi';
import { Category, Product } from '../types';
import { ProductCard } from '../components/product/ProductCard';
import { SkeletonCard } from '../components/common/SkeletonCard';

export const HomePage: React.FC = () => {
  const [categories, setCategories] = useState<Category[]>([]);
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    const loadHomeData = async () => {
      try {
        const [catRes, prodRes] = await Promise.all([
          productApi.getCategories(),
          productApi.getProducts({ page: 0, size: 8 }),
        ]);

        if (isMounted) {
          if (catRes.success && catRes.data) {
            setCategories(catRes.data);
          }
          if (prodRes.success && prodRes.data?.content) {
            setFeaturedProducts(prodRes.data.content);
          }
        }
      } catch {
        // Fallbacks will keep UI functioning
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    loadHomeData();
    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="space-y-16 pb-16">
      {/* Hero Section */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-indigo-900 via-indigo-800 to-slate-900 text-white shadow-xl mt-4">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_30%_30%,rgba(99,102,241,0.25),transparent)] pointer-events-none" />
        <div className="relative max-w-7xl mx-auto px-6 py-20 lg:py-28 flex flex-col lg:flex-row items-center justify-between gap-12">
          <div className="max-w-2xl text-center lg:text-left space-y-6">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-indigo-500/20 border border-indigo-400/30 text-indigo-200 text-xs font-semibold backdrop-blur-xs">
              <Sparkles className="w-3.5 h-3.5 text-indigo-300" />
              <span>Summer Mega Deals • Up to 60% Off</span>
            </div>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black tracking-tight leading-tight">
              Next-Generation <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-300 via-sky-300 to-emerald-300">
                E-Commerce Shopping
              </span>
            </h1>

            <p className="text-base sm:text-lg text-slate-300 leading-relaxed max-w-xl">
              Shop trending gadgets, designer fashion, bestseller books, and daily personal care
              essentials with ultra-fast delivery, 1-click digital wallet payments, and guaranteed buyer protection.
            </p>

            <div className="flex flex-wrap items-center justify-center lg:justify-start gap-4 pt-2">
              <Link
                to="/products"
                className="inline-flex items-center gap-2 px-6 py-3.5 rounded-xl font-bold text-sm bg-white text-slate-900 hover:bg-slate-100 shadow-lg shadow-black/20 hover:scale-105 transition-all"
              >
                <span>Browse Products</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
              <Link
                to="/wallet"
                className="inline-flex items-center gap-2 px-6 py-3.5 rounded-xl font-bold text-sm bg-indigo-700/60 hover:bg-indigo-700 text-white border border-indigo-500/40 backdrop-blur-xs transition-all"
              >
                <CreditCard className="w-4 h-4" />
                <span>My Wallet</span>
              </Link>
            </div>
          </div>

          {/* Hero Feature Badges Card */}
          <div className="w-full lg:w-96 bg-white/10 backdrop-blur-md rounded-2xl border border-white/15 p-6 shadow-2xl space-y-4">
            <h3 className="text-sm font-bold text-indigo-200 uppercase tracking-wider flex items-center gap-2">
              <ShieldCheck className="w-4 h-4" />
              Why Choose E-ShoppingZone?
            </h3>

            <div className="space-y-3 text-xs">
              <div className="flex items-start gap-3 p-2.5 rounded-xl bg-white/5 border border-white/10">
                <ShieldCheck className="w-4 h-4 text-emerald-400 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="font-semibold text-white">Bank-Grade Payment Security</p>
                  <p className="text-slate-300 text-[11px]">256-bit SSL encrypted transactions; zero sensitive payment data saved on your device.</p>
                </div>
              </div>

              <div className="flex items-start gap-3 p-2.5 rounded-xl bg-white/5 border border-white/10">
                <Zap className="w-4 h-4 text-amber-400 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="font-semibold text-white">Instant One-Tap Checkout</p>
                  <p className="text-slate-300 text-[11px]">Seamless digital wallet deductions, instant order confirmation, and Cash on Delivery.</p>
                </div>
              </div>

              <div className="flex items-start gap-3 p-2.5 rounded-xl bg-white/5 border border-white/10">
                <Truck className="w-4 h-4 text-sky-400 mt-0.5 flex-shrink-0" />
                <div>
                  <p className="font-semibold text-white">Tracked Express Delivery</p>
                  <p className="text-slate-300 text-[11px]">Doorstep delivery across 19,000+ pin codes with easy 7-day hassle-free returns.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Value Proposition Strip */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center flex-shrink-0">
            <Truck className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-900">Fast Delivery</h4>
            <p className="text-[11px] text-slate-500">Pan-India express shipping</p>
          </div>
        </div>

        <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0">
            <CreditCard className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-900">Instant Wallet Pay</h4>
            <p className="text-[11px] text-slate-500">0-click seamless checkout</p>
          </div>
        </div>

        <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center flex-shrink-0">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-900">Buyer Protection</h4>
            <p className="text-[11px] text-slate-500">100% money-back guarantee</p>
          </div>
        </div>

        <div className="p-4 bg-white rounded-2xl border border-slate-200 shadow-xs flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center flex-shrink-0">
            <Zap className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-900">24/7 Availability</h4>
            <p className="text-[11px] text-slate-500">Shop and place orders anytime</p>
          </div>
        </div>
      </section>

      {/* Featured Categories */}
      {categories.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-slate-900">Popular Categories</h2>
              <p className="text-xs text-slate-500">Browse by departmental classification</p>
            </div>
            <Link
              to="/products"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              <span>View all</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {categories.map((cat) => (
              <Link
                key={cat.id}
                to={`/products?category=${cat.id}`}
                className="group p-5 bg-white rounded-2xl border border-slate-200 shadow-xs hover:border-indigo-300 hover:shadow-md transition-all text-center flex flex-col items-center justify-center"
              >
                <div className="w-12 h-12 rounded-2xl bg-indigo-50 group-hover:bg-indigo-600 text-indigo-600 group-hover:text-white flex items-center justify-center transition-colors mb-3">
                  <span className="text-base font-extrabold uppercase">{cat.name.slice(0, 2)}</span>
                </div>
                <h3 className="font-bold text-sm text-slate-800 group-hover:text-indigo-600 transition-colors">
                  {cat.name}
                </h3>
                {cat.description && (
                  <p className="mt-1 text-[11px] text-slate-400 line-clamp-1">{cat.description}</p>
                )}
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Featured Products Grid */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Featured Products</h2>
            <p className="text-xs text-slate-500">Hand-picked bestsellers ready for instant dispatch</p>
          </div>
          <Link
            to="/products"
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
          >
            <span>Explore catalog</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {Array.from({ length: 4 }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        ) : featuredProducts.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center border border-slate-200">
            <p className="text-slate-500 text-sm">No products found in the catalog.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {featuredProducts.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

