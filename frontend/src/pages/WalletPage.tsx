import React, { useState, useEffect, useCallback } from 'react';
import {
  Wallet as WalletIcon,
  ArrowUpRight,
  ArrowDownLeft,
  RotateCcw,
  PlusCircle,
  ShieldCheck,
  History,
  CreditCard,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useWallet } from '../context/WalletContext';
import { useNotification } from '../context/NotificationContext';
import { walletApi } from '../api/walletApi';
import { WalletTransaction } from '../types';
import { formatCurrency, formatDate } from '../utils/formatters';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const WalletPage: React.FC = () => {
  const { user } = useAuth();
  const { wallet, balance, isLoading: walletLoading, deposit } = useWallet();
  const { showSuccess, showError } = useNotification();

  const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
  const [loadingTxns, setLoadingTxns] = useState(true);

  // Top-up form state
  const [depositAmount, setDepositAmount] = useState<string>('1000');
  const [isDepositing, setIsDepositing] = useState(false);
  const [depositError, setDepositError] = useState<string | null>(null);

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

  const loadTransactions = useCallback(async () => {
    setLoadingTxns(true);
    try {
      const res = await walletApi.getTransactions(0, 30);
      if (res.success && res.data) {
        setTransactions(res.data.content);
      }
    } catch {
      setTransactions([]);
    } finally {
      setLoadingTxns(false);
    }
  }, []);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const handleDepositSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setDepositError(null);
    const amt = parseFloat(depositAmount);
    if (isNaN(amt) || amt < 10) {
      setDepositError('Minimum deposit amount is ₹10.00');
      return;
    }
    if (amt > 100000) {
      setDepositError('Maximum deposit amount per transaction is ₹1,00,000.00');
      return;
    }

    setIsDepositing(true);
    try {
      const loaded = await loadRazorpayScript();
      if (!loaded) {
        showError('Failed to load Razorpay payment SDK. Please check your internet connection.');
        setIsDepositing(false);
        return;
      }

      const intentRes = await walletApi.createRazorpayTopupOrder(amt);
      if (!intentRes.success || !intentRes.data) {
        showError(intentRes.message || 'Failed to initialize Razorpay top-up order');
        setIsDepositing(false);
        return;
      }

      const intent = intentRes.data;

      const options = {
        key: intent.keyId,
        amount: intent.amountInPaise,
        currency: intent.currency || 'INR',
        name: 'E-ShoppingZone Wallet',
        description: `Wallet Top-Up: ${formatCurrency(amt)}`,
        order_id: intent.razorpayOrderId.startsWith('order_topup_sim_') ? undefined : intent.razorpayOrderId,
        prefill: {
          name: user?.fullName || 'Alice Customer',
          email: user?.email || '',
          contact: user?.phone || '9876543210',
        },
        theme: {
          color: '#4f46e5',
        },
        modal: {
          ondismiss: () => {
            setIsDepositing(false);
            showError('Payment process was cancelled by customer');
          },
        },
        handler: async (response: any) => {
          try {
            const verifyRes = await walletApi.verifyRazorpayTopupPayment({
              razorpayOrderId: response.razorpay_order_id || intent.razorpayOrderId,
              razorpayPaymentId: response.razorpay_payment_id || `pay_sim_${Date.now()}`,
              razorpaySignature: response.razorpay_signature || 'mock_valid_signature',
              amount: amt,
            });

            if (verifyRes.success && verifyRes.data) {
              showSuccess(`${formatCurrency(amt)} successfully credited to wallet via Razorpay!`);
              setDepositAmount('1000');
              await Promise.all([deposit(0, 'NOOP', 'refresh'), loadTransactions()]);
            } else {
              showError(verifyRes.message || 'Payment signature verification failed');
            }
          } catch (err: any) {
            showError(err.response?.data?.message || err.message || 'Payment verification failed');
          } finally {
            setIsDepositing(false);
          }
        },
      };

      const razorpayInstance = new (window as any).Razorpay(options);
      razorpayInstance.on('payment.failed', (resp: any) => {
        showError(`Payment failed: ${resp.error?.description || 'Transaction declined'}`);
        setIsDepositing(false);
      });
      razorpayInstance.open();
    } catch (err: any) {
      showError(err.response?.data?.message || err.message || 'Failed to initialize payment');
      setIsDepositing(false);
    }
  };

  const quickAmounts = [500, 1000, 2500, 5000];

  return (
    <div className="space-y-8 pb-16">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">E-Shopping Wallet</h1>
        <p className="text-xs text-slate-500 mt-1">
          Instant 1-click checkout balance with real-time transaction updates
        </p>
      </div>

      {/* Balance Card & Deposit Form */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Balance Card */}
        <div className="bg-gradient-to-br from-indigo-700 via-indigo-800 to-slate-900 rounded-3xl p-8 text-white shadow-xl flex flex-col justify-between space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-10 h-10 rounded-xl bg-white/10 backdrop-blur-xs flex items-center justify-center">
                <WalletIcon className="w-5 h-5 text-indigo-200" />
              </div>
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-200">
                Active Balance
              </span>
            </div>
            <span className="px-2.5 py-1 text-[11px] font-bold rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
              {wallet?.status || 'ACTIVE'}
            </span>
          </div>

          <div>
            <span className="text-xs text-indigo-200 block font-medium">Available to Spend</span>
            {walletLoading ? (
              <div className="h-12 w-40 bg-white/10 rounded-lg animate-pulse mt-1" />
            ) : (
              <span className="text-4xl sm:text-5xl font-black tracking-tight block mt-1">
                {formatCurrency(balance)}
              </span>
            )}
            <span className="text-[11px] text-indigo-300/80 block mt-1 font-mono">
              Wallet ID: #{wallet?.walletId || '---'}
            </span>
          </div>

          <div className="pt-4 border-t border-white/10 flex items-center justify-between text-xs text-indigo-200">
            <div className="flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Secure Digital Wallet</span>
            </div>
            <span className="font-mono">INR (₹)</span>
          </div>
        </div>

        {/* Right: Quick Deposit Widget */}
        <div className="lg:col-span-2 bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-5">
          <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <PlusCircle className="w-5 h-5 text-indigo-600" />
            Add Funds to Wallet
          </h2>

          {/* Quick Select Amount Pills */}
          <div>
            <span className="block text-xs font-semibold text-slate-700 mb-2">Select Amount</span>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              {quickAmounts.map((amt) => (
                <button
                  key={amt}
                  type="button"
                  onClick={() => setDepositAmount(amt.toString())}
                  className={`py-2 px-3 text-xs font-bold rounded-xl border transition-all ${depositAmount === amt.toString()
                      ? 'border-indigo-600 bg-indigo-50 text-indigo-700'
                      : 'border-slate-200 hover:border-slate-300 text-slate-700 bg-slate-50'
                    }`}
                >
                  +{formatCurrency(amt)}
                </button>
              ))}
            </div>
          </div>

          {/* Deposit Form */}
          <form onSubmit={handleDepositSubmit} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="customAmountInput" className="block text-xs font-semibold text-slate-700 mb-1">
                  Custom Amount (₹)
                </label>
                <div className="relative">
                  <span className="absolute left-3.5 top-2.5 text-slate-400 font-bold text-xs">₹</span>
                  <input
                    id="customAmountInput"
                    type="number"
                    min="10"
                    max="100000"
                    step="1"
                    value={depositAmount}
                    onChange={(e) => setDepositAmount(e.target.value)}
                    placeholder="1000"
                    className="w-full pl-8 pr-4 py-2.5 text-xs font-bold bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Payment Gateway
                </label>
                <div className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl font-medium text-slate-800 flex items-center justify-between">
                  <span className="font-semibold text-slate-900">Razorpay Secure Gateway</span>
                  <span className="text-[11px] font-semibold text-indigo-700 bg-indigo-50 border border-indigo-200/60 px-2.5 py-0.5 rounded-md">
                    Cards, Netbanking & Wallets
                  </span>
                </div>
              </div>
            </div>

            <div className="p-3 bg-indigo-50/70 border border-indigo-100 rounded-xl flex items-center gap-2.5 text-xs text-indigo-800">
              <CreditCard className="w-4 h-4 text-indigo-600 flex-shrink-0" />
              <span>
                Clicking top-up will open the official <strong>Razorpay Payment Gateway</strong> dialog to pay via Cards, Netbanking, or Wallets.
              </span>
            </div>

            {depositError && <p className="text-xs text-rose-600 font-medium">{depositError}</p>}

            <button
              type="submit"
              disabled={isDepositing}
              className="inline-flex items-center justify-center gap-2 px-6 py-3 rounded-xl font-bold text-xs bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 shadow-md shadow-indigo-100"
            >
              {isDepositing ? (
                <>
                  <LoadingSpinner size="sm" />
                  <span>Processing Deposit...</span>
                </>
              ) : (
                <>
                  <span>
                    Pay & Top-up {formatCurrency(parseFloat(depositAmount) || 0)} via Razorpay
                  </span>
                  <ArrowUpRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>
        </div>
      </div>

      {/* Transaction History Table */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <History className="w-5 h-5 text-indigo-600" />
            Transaction History
          </h2>
          <button
            onClick={loadTransactions}
            className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold"
          >
            Refresh History
          </button>
        </div>

        {loadingTxns ? (
          <div className="py-8">
            <LoadingSpinner size="md" message="Loading transactions..." />
          </div>
        ) : transactions.length === 0 ? (
          <div className="py-12 text-center text-slate-500 text-xs">
            No transactions recorded yet. Add funds or place an order to see your transaction activity here.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 uppercase tracking-wider text-[10px]">
                  <th className="py-3 px-3">Type</th>
                  <th className="py-3 px-3">Reference / Order</th>
                  <th className="py-3 px-3">Date</th>
                  <th className="py-3 px-3 text-right">Amount</th>
                  <th className="py-3 px-3 text-right">Balance Before</th>
                  <th className="py-3 px-3 text-right">Balance After</th>
                  <th className="py-3 px-3 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {transactions.map((tx) => {
                  const isCredit = tx.transactionType === 'DEPOSIT' || tx.transactionType === 'REFUND';
                  return (
                    <tr key={tx.transactionId} className="hover:bg-slate-50 transition-colors">
                      <td className="py-3.5 px-3">
                        <div className="flex items-center gap-2">
                          <div
                            className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${isCredit ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-700'
                              }`}
                          >
                            {tx.transactionType === 'DEPOSIT' && <ArrowUpRight className="w-4 h-4" />}
                            {tx.transactionType === 'CHECKOUT_PAYMENT' && <ArrowDownLeft className="w-4 h-4" />}
                            {tx.transactionType === 'REFUND' && <RotateCcw className="w-4 h-4" />}
                          </div>
                          <div>
                            <span className="font-bold text-slate-800 block">{tx.transactionType}</span>
                            {tx.description && (
                              <span className="text-[10px] text-slate-400 block line-clamp-1">
                                {tx.description}
                              </span>
                            )}
                          </div>
                        </div>
                      </td>

                      <td className="py-3.5 px-3 font-mono text-slate-500">
                        <div>{tx.transactionReference}</div>
                        {tx.orderReference && (
                          <div className="text-[10px] text-indigo-600">{tx.orderReference}</div>
                        )}
                      </td>

                      <td className="py-3.5 px-3 text-slate-500 whitespace-nowrap">
                        {formatDate(tx.createdAt)}
                      </td>

                      <td
                        className={`py-3.5 px-3 text-right font-extrabold whitespace-nowrap ${isCredit ? 'text-emerald-600' : 'text-slate-900'
                          }`}
                      >
                        {isCredit ? '+' : '-'}
                        {formatCurrency(tx.amount)}
                      </td>

                      <td className="py-3.5 px-3 text-right text-slate-500 font-mono">
                        {formatCurrency(tx.balanceBefore)}
                      </td>

                      <td className="py-3.5 px-3 text-right font-bold text-slate-800 font-mono">
                        {formatCurrency(tx.balanceAfter)}
                      </td>

                      <td className="py-3.5 px-3 text-center">
                        <span className="inline-block px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          {tx.status}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

