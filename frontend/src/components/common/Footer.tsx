import React from 'react';
import { Link } from 'react-router-dom';
import {
  ShoppingBag,
  ShieldCheck,
  Truck,
  RotateCcw,
  Clock,
  CreditCard,
  Heart,
  Mail,
  MapPin,
} from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="bg-slate-900 text-slate-400 text-sm mt-auto border-t border-slate-800">
      {/* Trust Badges Strip */}
      <div className="border-b border-slate-800 bg-slate-950/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6 text-center sm:text-left">
            <div className="flex items-center gap-3.5 justify-center sm:justify-start">
              <div className="w-10 h-10 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center flex-shrink-0">
                <Truck className="w-5 h-5" />
              </div>
              <div>
                <h5 className="text-white font-bold text-xs uppercase tracking-wider">Free Shipping</h5>
                <p className="text-[11px] text-slate-400">On all orders above ₹999</p>
              </div>
            </div>

            <div className="flex items-center gap-3.5 justify-center sm:justify-start">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center flex-shrink-0">
                <RotateCcw className="w-5 h-5" />
              </div>
              <div>
                <h5 className="text-white font-bold text-xs uppercase tracking-wider">7-Day Returns</h5>
                <p className="text-[11px] text-slate-400">Easy hassle-free replacements</p>
              </div>
            </div>

            <div className="flex items-center gap-3.5 justify-center sm:justify-start">
              <div className="w-10 h-10 rounded-xl bg-sky-500/10 text-sky-400 flex items-center justify-center flex-shrink-0">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <h5 className="text-white font-bold text-xs uppercase tracking-wider">100% Authentic</h5>
                <p className="text-[11px] text-slate-400">Verified trusted sellers only</p>
              </div>
            </div>

            <div className="flex items-center gap-3.5 justify-center sm:justify-start">
              <div className="w-10 h-10 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center flex-shrink-0">
                <Clock className="w-5 h-5" />
              </div>
              <div>
                <h5 className="text-white font-bold text-xs uppercase tracking-wider">24/7 Shopping</h5>
                <p className="text-[11px] text-slate-400">Shop and place orders anytime</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Main Footer Links */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
          {/* Brand info */}
          <div className="space-y-3.5 md:col-span-1">
            <Link to="/" className="flex items-center gap-2 group">
              <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white">
                <ShoppingBag className="w-4 h-4" />
              </div>
              <span className="font-bold text-white text-base">
                E-Shopping<span className="text-indigo-400">Zone</span>
              </span>
            </Link>
            <p className="text-xs text-slate-400 leading-relaxed">
              Your one-stop destination for genuine electronics, premium fashion, bestseller books, and daily personal care essentials.
            </p>
            <div className="space-y-1.5 text-xs text-slate-400 pt-1">
              <div className="flex items-center gap-2">
                <MapPin className="w-3.5 h-3.5 text-indigo-400 flex-shrink-0" />
                <span>Cyber City, Hyderabad, Telangana 500081</span>
              </div>
              <div className="flex items-center gap-2">
                <Mail className="w-3.5 h-3.5 text-indigo-400 flex-shrink-0" />
                <span>support@eshoppingzone.com</span>
              </div>
            </div>
          </div>

          {/* Popular Categories */}
          <div>
            <h4 className="text-white font-semibold text-xs tracking-wider uppercase mb-3">
              Shop Categories
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/products?category=1" className="hover:text-white transition-colors">
                  Electronics & Gadgets
                </Link>
              </li>
              <li>
                <Link to="/products?category=2" className="hover:text-white transition-colors">
                  Fashion & Apparel
                </Link>
              </li>
              <li>
                <Link to="/products?category=3" className="hover:text-white transition-colors">
                  Bestseller Books
                </Link>
              </li>
              <li>
                <Link to="/products?category=4" className="hover:text-white transition-colors">
                  Beauty & Personal Care
                </Link>
              </li>
              <li>
                <Link to="/products" className="text-indigo-400 hover:text-indigo-300 font-semibold transition-colors">
                  View All Products →
                </Link>
              </li>
            </ul>
          </div>

          {/* Quick Navigation */}
          <div>
            <h4 className="text-white font-semibold text-xs tracking-wider uppercase mb-3">
              Customer Services
            </h4>
            <ul className="space-y-2 text-xs">
              <li>
                <Link to="/orders" className="hover:text-white transition-colors">
                  Track Your Orders
                </Link>
              </li>
              <li>
                <Link to="/cart" className="hover:text-white transition-colors">
                  Shopping Cart
                </Link>
              </li>
              <li>
                <Link to="/wallet" className="hover:text-white transition-colors">
                  E-Shopping Wallet
                </Link>
              </li>
              <li>
                <Link to="/profile" className="hover:text-white transition-colors">
                  My Profile & Addresses
                </Link>
              </li>
              <li>
                <Link to="/merchant" className="hover:text-white transition-colors">
                  Sell on E-ShoppingZone
                </Link>
              </li>
            </ul>
          </div>

          {/* Payment & Security */}
          <div>
            <h4 className="text-white font-semibold text-xs tracking-wider uppercase mb-3 flex items-center gap-1.5">
              <CreditCard className="w-3.5 h-3.5 text-indigo-400" />
              Secure Payment Options
            </h4>
            <p className="text-xs text-slate-400 leading-relaxed mb-3">
              We support fast instant Digital Wallet checkout and Cash on Delivery (COD) across India.
            </p>
            <div className="bg-slate-800/80 p-3 rounded-xl border border-slate-700/60 text-xs text-slate-300 space-y-1.5">
              <div className="flex items-center gap-2 text-emerald-400 font-semibold text-[11px]">
                <ShieldCheck className="w-4 h-4" />
                <span>100% Safe & Encrypted Payments</span>
              </div>
              <p className="text-[11px] text-slate-400">
                Your payment data is fully encrypted with 256-bit SSL security.
              </p>
            </div>
          </div>
        </div>

        {/* Bottom Copyright & Currency Notice */}
        <div className="border-t border-slate-800 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs">
          <p className="flex items-center gap-1">
            © 2026 E-ShoppingZone. Crafted with <Heart className="w-3.5 h-3.5 text-rose-500 fill-rose-500" /> for shoppers across India.
          </p>
          <div className="flex items-center gap-4 text-slate-400 text-[11px]">
            <span>Privacy Policy</span>
            <span>•</span>
            <span>Terms of Service</span>
            <span>•</span>
            <span className="font-medium text-slate-300">All prices in Indian Rupee (₹ INR)</span>
          </div>
        </div>
      </div>
    </footer>
  );
};
