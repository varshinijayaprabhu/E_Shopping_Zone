import React from 'react';

export const SkeletonCard: React.FC = () => {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs animate-pulse">
      <div className="w-full h-48 bg-slate-200" />
      <div className="p-4 space-y-3">
        <div className="h-4 bg-slate-200 rounded w-1/4" />
        <div className="h-5 bg-slate-200 rounded w-3/4" />
        <div className="h-4 bg-slate-200 rounded w-1/2" />
        <div className="pt-2 flex items-center justify-between">
          <div className="h-6 bg-slate-200 rounded w-1/3" />
          <div className="h-9 bg-slate-200 rounded-lg w-1/3" />
        </div>
      </div>
    </div>
  );
};

