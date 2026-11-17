import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, Mail, Lock, Phone, KeyRound, ShoppingBag, Store, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const RegisterPage: React.FC = () => {
  const { registerCustomer, registerMerchant } = useAuth();
  const { showSuccess, showError } = useNotification();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState<'CUSTOMER' | 'MERCHANT'>('CUSTOMER');

  // Form Fields
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [inviteCode, setInviteCode] = useState('');

  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim() || !email.trim() || !password) {
      showError('Please fill out all required fields.');
      return;
    }

    if (password.length < 8) {
      showError('Password must be at least 8 characters long.');
      return;
    }

    setIsLoading(true);
    try {
      if (activeTab === 'CUSTOMER') {
        await registerCustomer({
          fullName: fullName.trim(),
          email: email.trim(),
          password,
          phone: phone.trim() || undefined,
        });
        showSuccess('Account created successfully! Welcome to E-ShoppingZone.');
        navigate('/');
      } else {
        if (!inviteCode.trim()) {
          showError('Merchant invite code is required to register as a merchant.');
          setIsLoading(false);
          return;
        }

        await registerMerchant({
          fullName: fullName.trim(),
          email: email.trim(),
          password,
          phone: phone.trim() || undefined,
          inviteCode: inviteCode.trim(),
        });
        showSuccess('Merchant account created successfully! Welcome to your Merchant Portal.');
        navigate('/merchant');
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      showError(errorResponse.response?.data?.message || 'Registration failed.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[75vh] flex items-center justify-center py-12 px-4">
      <div className="max-w-md w-full space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-indigo-600 text-white shadow-lg shadow-indigo-100 mb-2">
            <ShoppingBag className="w-6 h-6" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Create an Account
          </h1>
          <p className="text-xs text-slate-500">
            Join E-ShoppingZone to start buying or selling products
          </p>
        </div>

        {/* Tab Toggle: Customer vs Merchant */}
        <div className="grid grid-cols-2 p-1 bg-slate-100 rounded-2xl">
          <button
            type="button"
            onClick={() => setActiveTab('CUSTOMER')}
            className={`py-2 text-xs font-bold rounded-xl flex items-center justify-center gap-2 transition-all ${activeTab === 'CUSTOMER'
                ? 'bg-white text-indigo-700 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
              }`}
          >
            <User className="w-4 h-4" />
            <span>Customer</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('MERCHANT')}
            className={`py-2 text-xs font-bold rounded-xl flex items-center justify-center gap-2 transition-all ${activeTab === 'MERCHANT'
                ? 'bg-white text-indigo-700 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
              }`}
          >
            <Store className="w-4 h-4" />
            <span>Merchant</span>
          </button>
        </div>

        {/* Registration Form Box */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-4">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="regFullNameInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Full Name *
              </label>
              <div className="relative">
                <input
                  id="regFullNameInput"
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="e.g. Alice Sharma"
                  className="w-full pl-9 pr-4 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <User className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              </div>
            </div>

            <div>
              <label htmlFor="regEmailInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Email Address *
              </label>
              <div className="relative">
                <input
                  id="regEmailInput"
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full pl-9 pr-4 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              </div>
            </div>

            <div>
              <label htmlFor="regPasswordInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Password *
              </label>
              <div className="relative">
                <input
                  id="regPasswordInput"
                  type="password"
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Min 8 characters"
                  className="w-full pl-9 pr-4 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              </div>
            </div>

            <div>
              <label htmlFor="regPhoneInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Phone Number (Optional)
              </label>
              <div className="relative">
                <input
                  id="regPhoneInput"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="10-digit mobile number"
                  className="w-full pl-9 pr-4 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              </div>
            </div>

            {/* Merchant Invite Code Field (only in Merchant tab) */}
            {activeTab === 'MERCHANT' && (
              <div>
                <label htmlFor="regInviteCodeInput" className="block text-xs font-semibold text-slate-700 mb-1">
                  Merchant Provisioning Invite Code *
                </label>
                <div className="relative">
                  <input
                    id="regInviteCodeInput"
                    type="text"
                    required
                    value={inviteCode}
                    onChange={(e) => setInviteCode(e.target.value)}
                    placeholder="Enter merchant secret invite code"
                    className="w-full pl-9 pr-4 py-2.5 text-xs bg-amber-50/50 border border-amber-300 rounded-xl font-mono text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <KeyRound className="w-4 h-4 text-amber-500 absolute left-3 top-3 pointer-events-none" />
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-bold text-xs bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 shadow-md shadow-indigo-100"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Registering...</span>
                </>
              ) : (
                <>
                  <span>Create {activeTab === 'CUSTOMER' ? 'Customer' : 'Merchant'} Account</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        </div>

        {/* Link to Login */}
        <p className="text-center text-xs text-slate-500">
          Already registered?{' '}
          <Link to="/login" className="font-bold text-indigo-600 hover:text-indigo-800">
            Sign In &rarr;
          </Link>
        </p>
      </div>
    </div>
  );
};

