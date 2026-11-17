import React, { useState } from 'react';
import { User as UserIcon, Mail, Phone, ShieldCheck, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useNotification } from '../context/NotificationContext';
import { authApi } from '../api/authApi';
import { maskPhone } from '../utils/formatters';

export const ProfilePage: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const { showSuccess, showError } = useNotification();

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [isUpdating, setIsUpdating] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      showError('Full name cannot be blank.');
      return;
    }

    setIsUpdating(true);
    try {
      const res = await authApi.updateProfile({
        fullName: fullName.trim(),
        phone: phone.trim() || undefined,
      });

      if (res.success) {
        showSuccess('Profile updated successfully!');
        await refreshUser();
      } else {
        showError(res.message || 'Failed to update profile.');
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      showError(errorResponse.response?.data?.message || 'Profile update failed.');
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 pb-16">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">Account Profile</h1>
        <p className="text-xs text-slate-500 mt-1">Manage your identity and personal preferences</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left Col: User Summary Card */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs flex flex-col items-center text-center space-y-4">
          <div className="w-20 h-20 rounded-full bg-gradient-to-tr from-indigo-600 to-violet-500 text-white flex items-center justify-center text-2xl font-black shadow-lg shadow-indigo-100 uppercase">
            {user?.fullName ? user.fullName[0] : (user?.email ? user.email[0] : 'U')}
          </div>

          <div>
            <h2 className="text-lg font-black text-slate-900">{user?.fullName || 'User'}</h2>
            <p className="text-xs text-slate-500 mt-0.5">{user?.email}</p>
          </div>

          <div className="flex flex-wrap gap-1.5 justify-center">
            {user?.roles?.map((role) => (
              <span
                key={role}
                className="px-2.5 py-1 text-[10px] font-bold uppercase rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200"
              >
                {role}
              </span>
            ))}
          </div>

          <div className="w-full pt-4 border-t border-slate-100 text-left text-xs space-y-2">
            <div className="flex items-center gap-2 text-slate-600">
              <Mail className="w-4 h-4 text-slate-400" />
              <span className="truncate">{user?.email}</span>
            </div>
            {user?.phone && (
              <div className="flex items-center gap-2 text-slate-600">
                <Phone className="w-4 h-4 text-slate-400" />
                <span>{maskPhone(user.phone)}</span>
              </div>
            )}
            <div className="flex items-center gap-2 text-emerald-600 font-semibold">
              <CheckCircle2 className="w-4 h-4" />
              <span>Active Account</span>
            </div>
          </div>
        </div>

        {/* Right 2 Cols: Edit Profile Form */}
        <div className="md:col-span-2 bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-6">
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2 pb-3 border-b border-slate-100">
            <UserIcon className="w-5 h-5 text-indigo-600" />
            Edit Profile Details
          </h3>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="emailInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Email Address (Read-only)
              </label>
              <input
                id="emailInput"
                type="email"
                disabled
                value={user?.email || ''}
                className="w-full px-3.5 py-2.5 text-xs bg-slate-100 border border-slate-200 rounded-xl text-slate-500 cursor-not-allowed"
              />
            </div>

            <div>
              <label htmlFor="profileFullNameInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Full Name *
              </label>
              <input
                id="profileFullNameInput"
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Your Full Name"
                className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div>
              <label htmlFor="profilePhoneInput" className="block text-xs font-semibold text-slate-700 mb-1">
                Phone Number
              </label>
              <input
                id="profilePhoneInput"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="10-digit mobile number"
                className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={isUpdating}
                className="px-6 py-2.5 rounded-xl font-bold text-xs bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 shadow-md shadow-indigo-100"
              >
                {isUpdating ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>

          {/* Security & Privacy Box */}
          <div className="mt-6 pt-6 border-t border-slate-100">
            <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-500" />
              Account Security & Privacy
            </h4>
            <div className="bg-slate-50 rounded-xl p-3 text-[11px] text-slate-600 space-y-1">
              <p>• Your personal data and profile information are encrypted and protected.</p>
              <p>• Secure session authentication with automatic protection against unauthorized access.</p>
              <p>• Logging out securely terminates your active session across all devices.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

