import React, { useEffect, useState, useCallback } from 'react';
import {
  ShieldAlert,
  Users,
  Store,
  IndianRupee,
  Package,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Ban,
  Check,
  FileText,
  RefreshCw,
  Search,
} from 'lucide-react';
import { adminApi } from '../../api/adminApi';
import { DashboardStats, MerchantProfile, DisputeTicket, AdminAuditLog } from '../../types';
import { useNotification } from '../../context/NotificationContext';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const AdminDashboardPage: React.FC = () => {
  const { showSuccess, showError } = useNotification();

  const [activeTab, setActiveTab] = useState<'metrics' | 'merchants' | 'disputes' | 'audit'>('metrics');
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [merchants, setMerchants] = useState<MerchantProfile[]>([]);
  const [disputes, setDisputes] = useState<DisputeTicket[]>([]);
  const [auditLogs, setAuditLogs] = useState<AdminAuditLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [refreshing, setRefreshing] = useState<boolean>(false);

  // Search & Filter
  const [merchantSearch, setMerchantSearch] = useState<string>('');
  const [disputeFilter, setDisputeFilter] = useState<string>('ALL');

  // Modals / Action states
  const [selectedMerchant, setSelectedMerchant] = useState<MerchantProfile | null>(null);
  const [merchantActionReason, setMerchantActionReason] = useState<string>('');
  const [merchantModalAction, setMerchantModalAction] = useState<'APPROVE' | 'SUSPEND' | null>(null);

  const [selectedDispute, setSelectedDispute] = useState<DisputeTicket | null>(null);
  const [disputeResolveAction, setDisputeResolveAction] = useState<'REFUND' | 'NO_REFUND'>('REFUND');
  const [adminNotes, setAdminNotes] = useState<string>('');

  const loadData = useCallback(async () => {
    try {
      const [statsRes, merchantsRes, disputesRes, auditRes] = await Promise.all([
        adminApi.getMetrics(),
        adminApi.getMerchants(),
        adminApi.getDisputes(),
        adminApi.getAuditLogs(0, 20),
      ]);

      if (statsRes.success && statsRes.data) setStats(statsRes.data);
      if (merchantsRes.success && merchantsRes.data) setMerchants(merchantsRes.data);
      if (disputesRes.success && disputesRes.data) setDisputes(disputesRes.data.content || []);
      if (auditRes.success && auditRes.data) setAuditLogs(auditRes.data.content || []);
    } catch (err: any) {
      showError(err.message || 'Failed to fetch administrator records', 'Error loading admin data');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [showError]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Periodic polling for dispute SLA countdowns
  useEffect(() => {
    const timer = setInterval(() => {
      setDisputes((prev) =>
        prev.map((d) => {
          if (d.status === 'PENDING_MERCHANT' && d.merchantDeadline) {
            const diff = Math.max(
              0,
              Math.floor((new Date(d.merchantDeadline).getTime() - Date.now()) / 1000)
            );
            return { ...d, remainingSeconds: diff };
          }
          return d;
        })
      );
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const handleRefresh = async () => {
    setRefreshing(true);
    await loadData();
  };

  const handleConfirmMerchantAction = async () => {
    if (!selectedMerchant || !merchantModalAction) return;
    const shouldEnable = merchantModalAction === 'APPROVE';
    try {
      const res = await adminApi.updateMerchantStatus(
        selectedMerchant.id,
        shouldEnable,
        merchantActionReason || undefined
      );
      if (res.success) {
        showSuccess(
          `${selectedMerchant.fullName} is now ${shouldEnable ? 'ACTIVE' : 'SUSPENDED'}.`,
          shouldEnable ? 'Merchant Approved' : 'Merchant Suspended'
        );
        setSelectedMerchant(null);
        setMerchantModalAction(null);
        setMerchantActionReason('');
        loadData();
      }
    } catch (err: any) {
      showError(
        err.response?.data?.message || err.message || 'Failed to update merchant status',
        'Action Failed'
      );
    }
  };

  const handleConfirmDisputeResolve = async () => {
    if (!selectedDispute) return;
    try {
      const res = await adminApi.resolveDispute(
        selectedDispute.id,
        disputeResolveAction,
        adminNotes || 'Resolved by administrator'
      );
      if (res.success) {
        showSuccess(
          `Dispute #${selectedDispute.id} for Order #${selectedDispute.orderId} resolved successfully.`,
          disputeResolveAction === 'REFUND' ? 'Refund Processed' : 'Dispute Closed'
        );
        setSelectedDispute(null);
        setAdminNotes('');
        loadData();
      }
    } catch (err: any) {
      showError(
        err.response?.data?.message || err.message || 'Failed to resolve dispute ticket',
        'Resolution Failed'
      );
    }
  };

  const filteredMerchants = merchants.filter(
    (m) =>
      m.fullName.toLowerCase().includes(merchantSearch.toLowerCase()) ||
      m.email.toLowerCase().includes(merchantSearch.toLowerCase())
  );

  const filteredDisputes = disputes.filter((d) => {
    if (disputeFilter === 'ALL') return true;
    return d.status === disputeFilter;
  });

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[500px]">
        <RefreshCw className="w-10 h-10 text-indigo-600 animate-spin mb-4" />
        <p className="text-slate-600 font-medium">Loading Administrator Governance Portal...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 pb-16">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-500/30 text-indigo-300 border border-indigo-400/30">
              ROLE_ADMIN PORTAL
            </span>
            <span className="text-xs text-slate-400">Microservice Port: 8086</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Platform Governance & Administration
          </h1>
          <p className="text-slate-300 text-sm mt-1 max-w-2xl">
            Real-time multi-service oversight, merchant verification & freeze gates, automated dispute SLA escalation, and immutable audit logs.
          </p>
        </div>

        <button
          onClick={handleRefresh}
          disabled={refreshing}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-sm font-medium backdrop-blur transition-all disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
          {refreshing ? 'Refreshing...' : 'Refresh Platform'}
        </button>
      </div>

      {/* Responsibility 3: Platform Summary Dashboard ("Big Numbers") */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Card 1: Orders */}
        <div className="bg-white rounded-xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Orders</span>
            <div className="w-9 h-9 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center">
              <Package className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-slate-900">{stats?.totalOrders ?? 0}</span>
            <p className="text-xs text-slate-500 mt-1">Cross-merchant fulfilled orders</p>
          </div>
        </div>

        {/* Card 2: Revenue */}
        <div className="bg-white rounded-xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Gross Platform Revenue</span>
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
              <IndianRupee className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-slate-900">
              {formatCurrency(stats?.totalRevenue ?? 0)}
            </span>
            <p className="text-xs text-slate-500 mt-1">Total completed platform sales</p>
          </div>
        </div>

        {/* Card 3: Active Customers */}
        <div className="bg-white rounded-xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Active Customers</span>
            <div className="w-9 h-9 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center">
              <Users className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-slate-900">{stats?.activeCustomers ?? 0}</span>
            <p className="text-xs text-slate-500 mt-1">{stats?.totalUsers ?? 0} total registered users</p>
          </div>
        </div>

        {/* Card 4: Registered Merchants */}
        <div className="bg-white rounded-xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Registered Merchants</span>
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <Store className="w-5 h-5" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-slate-900">{stats?.registeredMerchants ?? 0}</span>
            <p className="text-xs text-slate-500 mt-1">
              <span className="text-amber-600 font-semibold">{stats?.pendingMerchants ?? 0}</span> unverified/pending
            </p>
          </div>
        </div>
      </div>

      {/* Mini-Badges: Dispute Health */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-amber-50/70 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
          <Clock className="w-6 h-6 text-amber-600 flex-shrink-0" />
          <div>
            <div className="text-lg font-bold text-amber-900">{stats?.pendingDisputes ?? 0}</div>
            <div className="text-xs text-amber-700">Disputes Pending Merchant SLA (2m)</div>
          </div>
        </div>

        <div className="bg-rose-50/70 border border-rose-200 rounded-xl p-4 flex items-center gap-3">
          <AlertTriangle className="w-6 h-6 text-rose-600 flex-shrink-0" />
          <div>
            <div className="text-lg font-bold text-rose-900">{stats?.escalatedDisputes ?? 0}</div>
            <div className="text-xs text-rose-700">Escalated to Admin Override Queue</div>
          </div>
        </div>

        <div className="bg-emerald-50/70 border border-emerald-200 rounded-xl p-4 flex items-center gap-3">
          <CheckCircle2 className="w-6 h-6 text-emerald-600 flex-shrink-0" />
          <div>
            <div className="text-lg font-bold text-emerald-900">{stats?.resolvedDisputes ?? 0}</div>
            <div className="text-xs text-emerald-700">Disputes Resolved / Refunded</div>
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="border-b border-slate-200 flex gap-4">
        <button
          onClick={() => setActiveTab('metrics')}
          className={`pb-3 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'metrics'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          Platform Overview
        </button>
        <button
          onClick={() => setActiveTab('merchants')}
          className={`pb-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'merchants'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <span>Merchant Governance & Ban Gate</span>
          <span className="px-2 py-0.5 rounded-full text-xs bg-slate-100 text-slate-700">
            {merchants.length}
          </span>
        </button>
        <button
          onClick={() => setActiveTab('disputes')}
          className={`pb-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'disputes'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <span>Dispute SLA & Administrative Refund</span>
          {(stats?.escalatedDisputes ?? 0) > 0 && (
            <span className="px-2 py-0.5 rounded-full text-xs bg-rose-100 text-rose-700 font-bold animate-pulse">
              {stats?.escalatedDisputes} Escalated
            </span>
          )}
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          className={`pb-3 text-sm font-semibold border-b-2 transition-colors flex items-center gap-2 ${
            activeTab === 'audit'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <span>Immutable Audit Logs</span>
        </button>
      </div>

      {/* TAB 1: Platform Overview */}
      {activeTab === 'metrics' && (
        <div className="space-y-6">
          <div className="bg-white rounded-xl p-6 border border-slate-200 shadow-sm">
            <h2 className="text-lg font-bold text-slate-900 mb-2">Microservices Architecture Health</h2>
            <p className="text-sm text-slate-500 mb-4">
              Decentralized database-per-service configuration with Eureka service discovery and Spring Cloud Gateway routing.
            </p>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="p-4 rounded-lg bg-slate-50 border border-slate-100">
                <div className="text-xs font-semibold text-slate-400">admin-service</div>
                <div className="text-sm font-bold text-slate-800 mt-1">Port 8086 • admin_db</div>
                <div className="inline-flex items-center gap-1.5 mt-2 text-xs text-emerald-600 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  Active & Scheduled SLA Timer
                </div>
              </div>

              <div className="p-4 rounded-lg bg-slate-50 border border-slate-100">
                <div className="text-xs font-semibold text-slate-400">profile-service</div>
                <div className="text-sm font-bold text-slate-800 mt-1">Port 8081 • profile_db</div>
                <div className="inline-flex items-center gap-1.5 mt-2 text-xs text-emerald-600 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                  RBAC & Governance Gate
                </div>
              </div>

              <div className="p-4 rounded-lg bg-slate-50 border border-slate-100">
                <div className="text-xs font-semibold text-slate-400">order-service</div>
                <div className="text-sm font-bold text-slate-800 mt-1">Port 8084 • order_db</div>
                <div className="inline-flex items-center gap-1.5 mt-2 text-xs text-emerald-600 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                  Saga Compensation & Refunds
                </div>
              </div>

              <div className="p-4 rounded-lg bg-slate-50 border border-slate-100">
                <div className="text-xs font-semibold text-slate-400">wallet-service</div>
                <div className="text-sm font-bold text-slate-800 mt-1">Port 8085 • wallet_db</div>
                <div className="inline-flex items-center gap-1.5 mt-2 text-xs text-emerald-600 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                  Ledger Transactions
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: Responsibility 1 - Merchant Governance & Ban/Freeze Gate */}
      {activeTab === 'merchants' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="relative w-full sm:w-80">
              <input
                type="text"
                placeholder="Search merchant name or email..."
                value={merchantSearch}
                onChange={(e) => setMerchantSearch(e.target.value)}
                className="w-full pl-9 pr-4 py-2 text-sm bg-white border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            </div>

            <div className="text-xs text-slate-500">
              Showing {filteredMerchants.length} of {merchants.length} merchants
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider text-xs">
                  <tr>
                    <th className="px-6 py-3 font-semibold">Merchant / Store</th>
                    <th className="px-6 py-3 font-semibold">Email</th>
                    <th className="px-6 py-3 font-semibold">Location</th>
                    <th className="px-6 py-3 font-semibold">Registered At</th>
                    <th className="px-6 py-3 font-semibold">Status</th>
                    <th className="px-6 py-3 font-semibold text-right">Governance Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {filteredMerchants.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                        No merchants found matching your query.
                      </td>
                    </tr>
                  ) : (
                    filteredMerchants.map((merchant) => (
                      <tr key={merchant.id} className="hover:bg-slate-50/50 transition-colors">
                        <td className="px-6 py-4 font-medium text-slate-900 flex items-center gap-2">
                          <Store className="w-4 h-4 text-slate-400" />
                          {merchant.fullName}
                        </td>
                        <td className="px-6 py-4 text-slate-600">{merchant.email}</td>
                        <td className="px-6 py-4 text-slate-500">
                          {merchant.city ? `${merchant.city}, ${merchant.state || merchant.country}` : 'Not provided'}
                        </td>
                        <td className="px-6 py-4 text-slate-500">
                          {merchant.createdAt ? formatDate(merchant.createdAt) : 'N/A'}
                        </td>
                        <td className="px-6 py-4">
                          {merchant.enabled ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200">
                              <Check className="w-3 h-3" />
                              Active / Approved
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200">
                              <Ban className="w-3 h-3" />
                              Suspended / Banned
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-right">
                          {merchant.enabled ? (
                            <button
                              onClick={() => {
                                setSelectedMerchant(merchant);
                                setMerchantModalAction('SUSPEND');
                                setMerchantActionReason('');
                              }}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 transition-colors"
                            >
                              <Ban className="w-3.5 h-3.5" />
                              Suspend / Ban
                            </button>
                          ) : (
                            <button
                              onClick={() => {
                                setSelectedMerchant(merchant);
                                setMerchantModalAction('APPROVE');
                                setMerchantActionReason('');
                              }}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200 transition-colors"
                            >
                              <Check className="w-3.5 h-3.5" />
                              Approve Account
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: Responsibility 2 - Time-Based Dispute Escalation & Admin Refund */}
      {activeTab === 'disputes' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex gap-2">
              {['ALL', 'PENDING_MERCHANT', 'ESCALATED_TO_ADMIN', 'REFUNDED_BY_ADMIN', 'MERCHANT_ACCEPTED'].map(
                (filter) => (
                  <button
                    key={filter}
                    onClick={() => setDisputeFilter(filter)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                      disputeFilter === filter
                        ? 'bg-indigo-600 text-white'
                        : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {filter.replace(/_/g, ' ')}
                  </button>
                )
              )}
            </div>

            <div className="text-xs text-slate-500">
              Showing {filteredDisputes.length} disputes
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider text-xs">
                  <tr>
                    <th className="px-6 py-3 font-semibold">Ticket ID / Order</th>
                    <th className="px-6 py-3 font-semibold">Issue Reason</th>
                    <th className="px-6 py-3 font-semibold">Claim Amount</th>
                    <th className="px-6 py-3 font-semibold">SLA Countdown</th>
                    <th className="px-6 py-3 font-semibold">Status</th>
                    <th className="px-6 py-3 font-semibold text-right">Admin Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700">
                  {filteredDisputes.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-12 text-center text-slate-400">
                        No dispute tickets found for the selected filter.
                      </td>
                    </tr>
                  ) : (
                    filteredDisputes.map((dispute) => (
                      <tr key={dispute.id} className="hover:bg-slate-50/50 transition-colors">
                        <td className="px-6 py-4">
                          <div className="font-bold text-slate-900">Ticket #{dispute.id}</div>
                          <div className="text-xs text-slate-500">Order ID: #{dispute.orderId}</div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="font-medium text-slate-800">{dispute.issueReason}</div>
                          {dispute.customerComment && (
                            <div className="text-xs text-slate-500 italic mt-0.5 max-w-xs truncate">
                              "{dispute.customerComment}"
                            </div>
                          )}
                        </td>
                        <td className="px-6 py-4 font-bold text-slate-900">
                          {formatCurrency(dispute.refundAmount)}
                        </td>
                        <td className="px-6 py-4">
                          {dispute.status === 'PENDING_MERCHANT' ? (
                            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200 font-mono text-xs">
                              <Clock className="w-3.5 h-3.5 animate-spin" />
                              <span>{dispute.remainingSeconds ?? 0}s SLA remaining</span>
                            </div>
                          ) : dispute.status === 'ESCALATED_TO_ADMIN' ? (
                            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-rose-50 text-rose-700 border border-rose-200 font-medium text-xs">
                              <AlertTriangle className="w-3.5 h-3.5" />
                              <span>SLA Expired • In Admin Queue</span>
                            </div>
                          ) : (
                            <span className="text-xs text-slate-400">SLA Closed</span>
                          )}
                        </td>
                        <td className="px-6 py-4">
                          {dispute.status === 'PENDING_MERCHANT' && (
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                              PENDING MERCHANT
                            </span>
                          )}
                          {dispute.status === 'ESCALATED_TO_ADMIN' && (
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200 animate-pulse">
                              ESCALATED TO ADMIN
                            </span>
                          )}
                          {dispute.status === 'REFUNDED_BY_ADMIN' && (
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                              REFUNDED BY ADMIN
                            </span>
                          )}
                          {dispute.status === 'MERCHANT_ACCEPTED' && (
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                              MERCHANT ACCEPTED
                            </span>
                          )}
                          {dispute.status === 'RESOLVED_NO_REFUND' && (
                            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-300">
                              CLOSED (NO REFUND)
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-right">
                          {(dispute.status === 'ESCALATED_TO_ADMIN' || dispute.status === 'PENDING_MERCHANT') && (
                            <button
                              onClick={() => {
                                setSelectedDispute(dispute);
                                setDisputeResolveAction('REFUND');
                                setAdminNotes('');
                              }}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm transition-colors"
                            >
                              Resolve Dispute
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: Immutable Audit Logs */}
      {activeTab === 'audit' && (
        <div className="space-y-4">
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                <thead className="bg-slate-50 text-slate-500 uppercase tracking-wider text-xs">
                  <tr>
                    <th className="px-6 py-3 font-semibold">Audit ID</th>
                    <th className="px-6 py-3 font-semibold">Action Type</th>
                    <th className="px-6 py-3 font-semibold">Target</th>
                    <th className="px-6 py-3 font-semibold">Reason / Description</th>
                    <th className="px-6 py-3 font-semibold">IP Address</th>
                    <th className="px-6 py-3 font-semibold">Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 text-slate-700 font-mono text-xs">
                  {auditLogs.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="px-6 py-12 text-center text-slate-400 font-sans">
                        No administrative audit logs recorded yet.
                      </td>
                    </tr>
                  ) : (
                    auditLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-50/50">
                        <td className="px-6 py-3.5 text-slate-400">#{log.id}</td>
                        <td className="px-6 py-3.5">
                          <span className="font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                            {log.actionType}
                          </span>
                        </td>
                        <td className="px-6 py-3.5 text-slate-800">
                          {log.targetEntityType} #{log.targetEntityId}
                        </td>
                        <td className="px-6 py-3.5 text-slate-600 font-sans">{log.reason || 'N/A'}</td>
                        <td className="px-6 py-3.5 text-slate-400">{log.ipAddress || '127.0.0.1'}</td>
                        <td className="px-6 py-3.5 text-slate-500 font-sans">{formatDate(log.createdAt)}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Merchant Approve/Suspend Confirmation */}
      {selectedMerchant && merchantModalAction && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div
                className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                  merchantModalAction === 'APPROVE'
                    ? 'bg-emerald-50 text-emerald-600'
                    : 'bg-rose-50 text-rose-600'
                }`}
              >
                {merchantModalAction === 'APPROVE' ? <Check className="w-5 h-5" /> : <Ban className="w-5 h-5" />}
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {merchantModalAction === 'APPROVE' ? 'Approve Merchant Account' : 'Suspend / Ban Merchant'}
                </h3>
                <p className="text-xs text-slate-500">{selectedMerchant.fullName} ({selectedMerchant.email})</p>
              </div>
            </div>

            <p className="text-sm text-slate-600">
              {merchantModalAction === 'APPROVE'
                ? 'Approving this merchant grants immediate access to catalog management and customer fulfillment.'
                : 'Suspending this merchant will freeze their store and invalidate their product listings immediately.'}
            </p>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Mandatory Audit Reason
              </label>
              <textarea
                rows={3}
                placeholder="Enter justification for audit log (e.g. Document verification passed / Suspicious order rate)..."
                value={merchantActionReason}
                onChange={(e) => setMerchantActionReason(e.target.value)}
                className="w-full p-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => {
                  setSelectedMerchant(null);
                  setMerchantModalAction(null);
                }}
                className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmMerchantAction}
                className={`px-4 py-2 text-sm font-semibold text-white rounded-xl shadow-sm transition-colors ${
                  merchantModalAction === 'APPROVE'
                    ? 'bg-emerald-600 hover:bg-emerald-700'
                    : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                Confirm {merchantModalAction === 'APPROVE' ? 'Approval' : 'Suspension'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Administrative Dispute Resolution */}
      {selectedDispute && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                <ShieldAlert className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  Admin Dispute Resolution & Refund Override
                </h3>
                <p className="text-xs text-slate-500">
                  Ticket #{selectedDispute.id} • Order #{selectedDispute.orderId} • Claim: {formatCurrency(selectedDispute.refundAmount)}
                </p>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 text-xs text-slate-700 space-y-1">
              <div><span className="font-semibold">Reason:</span> {selectedDispute.issueReason}</div>
              {selectedDispute.customerComment && (
                <div><span className="font-semibold">Customer Comment:</span> {selectedDispute.customerComment}</div>
              )}
              {selectedDispute.merchantResponse && (
                <div><span className="font-semibold">Merchant Response:</span> {selectedDispute.merchantResponse}</div>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-2">
                Resolution Decision
              </label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setDisputeResolveAction('REFUND')}
                  className={`p-3 rounded-xl border text-left text-sm transition-all ${
                    disputeResolveAction === 'REFUND'
                      ? 'border-emerald-500 bg-emerald-50/70 text-emerald-900 font-semibold ring-2 ring-emerald-500/20'
                      : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <div className="font-bold flex items-center gap-1.5 text-emerald-700">
                    <CheckCircle2 className="w-4 h-4" />
                    Approve Full Refund
                  </div>
                  <div className="text-xs text-slate-500 mt-1">
                    Triggers Saga wallet compensation of {formatCurrency(selectedDispute.refundAmount)} to buyer
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setDisputeResolveAction('NO_REFUND')}
                  className={`p-3 rounded-xl border text-left text-sm transition-all ${
                    disputeResolveAction === 'NO_REFUND'
                      ? 'border-slate-800 bg-slate-100 text-slate-900 font-semibold ring-2 ring-slate-800/20'
                      : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <div className="font-bold flex items-center gap-1.5 text-slate-800">
                    <Ban className="w-4 h-4" />
                    Reject / No Refund
                  </div>
                  <div className="text-xs text-slate-500 mt-1">
                    Closes dispute in merchant's favor with no wallet compensation
                  </div>
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 uppercase mb-1">
                Administrative Notes (Immutable Audit Trail)
              </label>
              <textarea
                rows={3}
                placeholder="Enter justification for the resolution decision..."
                value={adminNotes}
                onChange={(e) => setAdminNotes(e.target.value)}
                className="w-full p-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setSelectedDispute(null)}
                className="px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmDisputeResolve}
                className="px-5 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-md transition-colors"
              >
                Execute Resolution
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
