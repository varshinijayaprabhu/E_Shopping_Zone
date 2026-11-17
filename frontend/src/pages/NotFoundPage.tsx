import React from 'react';
import { Link } from 'react-router-dom';
import { ShoppingBag, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-[60vh] flex items-center justify-center py-16 px-4">
      <div className="max-w-md w-full bg-white p-8 sm:p-12 rounded-3xl border border-slate-200 text-center shadow-xs space-y-4">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mx-auto mb-2">
          <ShoppingBag className="w-8 h-8" />
        </div>
        <span className="text-4xl font-black text-slate-900 block">404</span>
        <h1 className="text-xl font-bold text-slate-800">Page Not Found</h1>
        <p className="text-xs text-slate-500 leading-relaxed">
          The page you are looking for doesn't exist, has been removed, or is temporarily unavailable.
        </p>
        <div className="pt-2">
          <Link
            to="/"
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 text-white font-bold text-xs rounded-xl hover:bg-indigo-700 transition shadow-md shadow-indigo-100"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Return to Homepage</span>
          </Link>
        </div>
      </div>
    </div>
  );
};

