import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ShoppingBag,
  ShoppingCart,
  Wallet as WalletIcon,
  User as UserIcon,
  Search,
  Menu,
  X,
  LogOut,
  PackageCheck,
  Store,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { useWallet } from '../../context/WalletContext';
import { formatCurrency } from '../../utils/formatters';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, isMerchant, isCustomer, logout } = useAuth();
  const { itemCount } = useCart();
  const { balance } = useWallet();
  const navigate = useNavigate();

  const [searchQuery, setSearchQuery] = useState('');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`);
      setMobileMenuOpen(false);
    }
  };

  const handleLogout = async () => {
    setUserDropdownOpen(false);
    setMobileMenuOpen(false);
    await logout();
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-4">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 flex-shrink-0 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white shadow-md shadow-indigo-200 group-hover:scale-105 transition-transform">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-lg text-slate-900 tracking-tight leading-none group-hover:text-indigo-600 transition-colors">
                E-Shopping<span className="text-indigo-600">Zone</span>
              </span>
              <span className="text-[10px] text-slate-400 font-medium tracking-wider uppercase">
                Online Shopping & Deals
              </span>
            </div>
          </Link>

          {/* Search bar (Desktop) */}
          <form onSubmit={handleSearch} className="hidden md:flex flex-1 max-w-md mx-4">
            <div className="relative w-full">
              <input
                type="text"
                placeholder="Search laptops, smartphones, fashion, home..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            </div>
          </form>

          {/* Desktop Navigation Items */}
          <nav className="hidden lg:flex items-center gap-2">
            <Link
              to="/products"
              className="px-3 py-2 text-sm font-medium text-slate-700 hover:text-indigo-600 hover:bg-slate-50 rounded-lg transition-colors"
            >
              Catalog
            </Link>

            {isMerchant && (
              <Link
                to="/merchant"
                className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-indigo-700 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition-colors"
              >
                <Store className="w-4 h-4" />
                Merchant Portal
              </Link>
            )}

            {isCustomer && (
              <>
                <Link
                  to="/orders"
                  className="px-3 py-2 text-sm font-medium text-slate-700 hover:text-indigo-600 hover:bg-slate-50 rounded-lg transition-colors"
                >
                  My Orders
                </Link>

                {/* Wallet Balance Pill */}
                <Link
                  to="/wallet"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-full transition-colors"
                  title="Your E-ShoppingZone Wallet"
                >
                  <WalletIcon className="w-3.5 h-3.5 text-emerald-600" />
                  <span>{formatCurrency(balance)}</span>
                </Link>

                {/* Cart Icon & Badge */}
                <Link
                  to="/cart"
                  className="relative p-2 text-slate-700 hover:text-indigo-600 hover:bg-slate-50 rounded-xl transition-colors"
                  aria-label="Shopping Cart"
                >
                  <ShoppingCart className="w-5 h-5" />
                  {itemCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-indigo-600 text-white font-bold text-[10px] w-5 h-5 rounded-full flex items-center justify-center border-2 border-white shadow-xs">
                      {itemCount > 99 ? '99+' : itemCount}
                    </span>
                  )}
                </Link>
              </>
            )}

            {/* User Dropdown / Auth CTA */}
            {isAuthenticated ? (
              <div className="relative ml-2">
                <button
                  type="button"
                  onClick={() => setUserDropdownOpen(!userDropdownOpen)}
                  className="flex items-center gap-2 p-1.5 rounded-xl hover:bg-slate-100 transition-colors focus:outline-none"
                  aria-expanded={userDropdownOpen}
                >
                  <div className="w-8 h-8 rounded-full bg-indigo-600 text-white flex items-center justify-center font-bold text-xs uppercase shadow-xs">
                    {user?.fullName ? user.fullName[0] : (user?.email ? user.email[0] : 'U')}
                  </div>
                  <span className="text-xs font-semibold text-slate-800 max-w-[120px] truncate">
                    {user?.fullName || user?.email}
                  </span>
                </button>

                {userDropdownOpen && (
                  <div
                    className="absolute right-0 mt-2 w-56 bg-white rounded-xl shadow-xl border border-slate-100 py-1.5 z-50 animate-fadeIn"
                    onMouseLeave={() => setUserDropdownOpen(false)}
                  >
                    <div className="px-4 py-2 border-b border-slate-100">
                      <p className="text-xs font-semibold text-slate-900 truncate">
                        {user?.fullName || 'User'}
                      </p>
                      <p className="text-[11px] text-slate-500 truncate">{user?.email}</p>
                      <span className="mt-1 inline-block px-2 py-0.5 text-[10px] font-bold rounded-md bg-slate-100 text-slate-700">
                        {user?.roles?.join(', ')}
                      </span>
                    </div>

                    <Link
                      to="/profile"
                      onClick={() => setUserDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                    >
                      <UserIcon className="w-4 h-4 text-slate-400" />
                      My Profile
                    </Link>

                    {isCustomer && (
                      <Link
                        to="/orders"
                        onClick={() => setUserDropdownOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                      >
                        <PackageCheck className="w-4 h-4 text-slate-400" />
                        Order History
                      </Link>
                    )}

                    {isMerchant && (
                      <Link
                        to="/merchant"
                        onClick={() => setUserDropdownOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                      >
                        <Store className="w-4 h-4 text-slate-400" />
                        Merchant Console
                      </Link>
                    )}

                    <div className="border-t border-slate-100 my-1" />

                    <button
                      type="button"
                      onClick={handleLogout}
                      className="w-full flex items-center gap-2 px-4 py-2 text-xs font-medium text-rose-600 hover:bg-rose-50 transition-colors"
                    >
                      <LogOut className="w-4 h-4 text-rose-500" />
                      Sign Out
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2 ml-2">
                <Link
                  to="/login"
                  className="px-4 py-2 text-xs font-semibold text-slate-700 hover:text-indigo-600 transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs transition-colors"
                >
                  Register
                </Link>
              </div>
            )}
          </nav>

          {/* Mobile Menu Button */}
          <div className="flex items-center gap-2 lg:hidden">
            {isCustomer && (
              <Link to="/cart" className="relative p-2 text-slate-700" aria-label="Shopping Cart">
                <ShoppingCart className="w-5 h-5" />
                {itemCount > 0 && (
                  <span className="absolute -top-1 -right-1 bg-indigo-600 text-white font-bold text-[10px] w-4 h-4 rounded-full flex items-center justify-center">
                    {itemCount}
                  </span>
                )}
              </Link>
            )}

            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-slate-600 hover:text-slate-900 rounded-lg hover:bg-slate-100"
              aria-label="Open navigation menu"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile Search Bar */}
        <div className="md:hidden pb-3">
          <form onSubmit={handleSearch} className="relative w-full">
            <input
              type="text"
              placeholder="Search products..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
          </form>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="lg:hidden border-t border-slate-200 bg-white px-4 pt-3 pb-6 space-y-2">
          {isAuthenticated && (
            <div className="p-3 bg-slate-50 rounded-xl mb-3 flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-slate-900">{user?.fullName || 'User'}</p>
                <p className="text-[11px] text-slate-500">{user?.email}</p>
              </div>
              {isCustomer && (
                <div className="text-right">
                  <span className="text-[10px] text-slate-400 block">Wallet</span>
                  <span className="text-xs font-bold text-emerald-600">
                    {formatCurrency(balance)}
                  </span>
                </div>
              )}
            </div>
          )}

          <Link
            to="/products"
            onClick={() => setMobileMenuOpen(false)}
            className="block px-3 py-2 text-sm font-medium text-slate-800 rounded-lg hover:bg-slate-50"
          >
            Product Catalog
          </Link>

          {isCustomer && (
            <>
              <Link
                to="/orders"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-sm font-medium text-slate-800 rounded-lg hover:bg-slate-50"
              >
                My Orders
              </Link>
              <Link
                to="/wallet"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-sm font-medium text-slate-800 rounded-lg hover:bg-slate-50"
              >
                My Wallet ({formatCurrency(balance)})
              </Link>
              <Link
                to="/cart"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-sm font-medium text-slate-800 rounded-lg hover:bg-slate-50"
              >
                Shopping Cart ({itemCount})
              </Link>
            </>
          )}

          {isMerchant && (
            <Link
              to="/merchant"
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 text-sm font-semibold text-indigo-600 rounded-lg bg-indigo-50"
            >
              Merchant Console
            </Link>
          )}

          {isAuthenticated ? (
            <>
              <Link
                to="/profile"
                onClick={() => setMobileMenuOpen(false)}
                className="block px-3 py-2 text-sm font-medium text-slate-800 rounded-lg hover:bg-slate-50"
              >
                Profile & Settings
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="w-full text-left px-3 py-2 text-sm font-semibold text-rose-600 rounded-lg hover:bg-rose-50"
              >
                Sign Out
              </button>
            </>
          ) : (
            <div className="pt-2 flex flex-col gap-2">
              <Link
                to="/login"
                onClick={() => setMobileMenuOpen(false)}
                className="w-full text-center py-2 text-sm font-semibold text-slate-700 border border-slate-200 rounded-xl"
              >
                Sign In
              </Link>
              <Link
                to="/register"
                onClick={() => setMobileMenuOpen(false)}
                className="w-full text-center py-2 text-sm font-semibold text-white bg-indigo-600 rounded-xl"
              >
                Create Account
              </Link>
            </div>
          )}
        </div>
      )}
    </header>
  );
};

