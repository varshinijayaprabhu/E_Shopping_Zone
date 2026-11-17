import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Lock, Mail, ArrowRight, Github, ShoppingBag, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const { showSuccess, showError } = useNotification();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const from = (location.state as { from?: { pathname?: string } })?.from?.pathname || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      showError('Please enter both email and password.');
      return;
    }

    setIsLoading(true);
    try {
      const auth = await login(email.trim(), password);
      showSuccess(`Welcome back, ${auth.user.fullName || auth.user.email}!`);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      showError(errorResponse.response?.data?.message || 'Invalid email or password.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickFill = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('Password123!');
  };

  const handleGitHubLogin = () => {
    // Route through API Gateway so the session cookie matches GitHub callback domain
    const host = window.location.hostname || 'localhost';
    window.location.href = `http://${host}:8080/oauth2/authorization/github`;
  };

  return (
    <div className="min-h-[75vh] flex items-center justify-center py-12 px-4">
      <div className="max-w-md w-full space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-indigo-600 text-white shadow-lg shadow-indigo-100 mb-2">
            <ShoppingBag className="w-6 h-6" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Sign In to E-ShoppingZone
          </h1>
          <p className="text-xs text-slate-500">
            Enter your credentials to access your account and shopping cart
          </p>
        </div>

        {/* Demo Fast-Login Pills */}
        <div className="bg-indigo-50/70 border border-indigo-100 rounded-2xl p-3.5 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-indigo-900 uppercase tracking-wider">
              One-Click Demo Fill
            </span>
            <ShieldCheck className="w-3.5 h-3.5 text-indigo-600" />
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => handleQuickFill('alice@eshopping.com')}
              className="py-1.5 px-2 bg-white rounded-xl text-left border border-indigo-200 hover:border-indigo-400 transition-colors shadow-2xs"
            >
              <span className="block text-[11px] font-bold text-slate-900">Alice (Customer)</span>
              <span className="block text-[10px] text-slate-500">alice@eshopping.com</span>
            </button>
            <button
              type="button"
              onClick={() => handleQuickFill('bob@eshopping.com')}
              className="py-1.5 px-2 bg-white rounded-xl text-left border border-indigo-200 hover:border-indigo-400 transition-colors shadow-2xs"
            >
              <span className="block text-[11px] font-bold text-slate-900">Bob (Merchant)</span>
              <span className="block text-[10px] text-slate-500">bob@eshopping.com</span>
            </button>
          </div>
        </div>

        {/* Login Form Box */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-5">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="loginEmailInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Email Address
              </label>
              <div className="relative">
                <input
                  id="loginEmailInput"
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
              <label htmlFor="loginPasswordInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Password
              </label>
              <div className="relative">
                <input
                  id="loginPasswordInput"
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-9 pr-4 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3 pointer-events-none" />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-bold text-xs bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 shadow-md shadow-indigo-100"
            >
              {isLoading ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Verifying Credentials...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Social Divider */}
          <div className="relative my-4">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-slate-200" />
            </div>
            <div className="relative flex justify-center text-[10px] uppercase font-bold text-slate-400">
              <span className="bg-white px-3">Or continue with</span>
            </div>
          </div>

          {/* GitHub OAuth Button */}
          <button
            type="button"
            onClick={handleGitHubLogin}
            className="w-full inline-flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl font-semibold text-xs text-slate-800 bg-slate-50 hover:bg-slate-100 border border-slate-200 transition"
          >
            <Github className="w-4 h-4 text-slate-900" />
            <span>GitHub OAuth 2.0</span>
          </button>
        </div>

        {/* Link to Register */}
        <p className="text-center text-xs text-slate-500">
          Don&apos;t have an account yet?{' '}
          <Link to="/register" className="font-bold text-indigo-600 hover:text-indigo-800">
            Create an Account &rarr;
          </Link>
        </p>
      </div>
    </div>
  );
};

