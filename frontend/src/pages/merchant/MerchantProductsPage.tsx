import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Edit2, Trash2, Package, CheckCircle2, XCircle } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { Product } from '../../types';
import { formatCurrency } from '../../utils/formatters';
import { resolveImageUrl, getProductFallbackSvg } from '../../utils/imageUrl';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { ConfirmationModal } from '../../components/common/ConfirmationModal';
import { useNotification } from '../../context/NotificationContext';

export const MerchantProductsPage: React.FC = () => {
  const navigate = useNavigate();
  const { showSuccess, showError } = useNotification();

  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Deletion modal state
  const [deletingProduct, setDeletingProduct] = useState<Product | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchProducts = useCallback(async (page = 0) => {
    setIsLoading(true);
    try {
      const res = await productApi.getMerchantProducts(page, 10);
      if (res.success && res.data) {
        setProducts(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
        setCurrentPage(page);
      }
    } catch {
      setProducts([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProducts(0);
  }, [fetchProducts]);

  const handleDeleteConfirm = async () => {
    if (!deletingProduct) return;
    setIsDeleting(true);
    try {
      const res = await productApi.deleteProduct(deletingProduct.id);
      if (res.success) {
        showSuccess(`Product "${deletingProduct.name}" removed successfully.`);
        setDeletingProduct(null);
        await fetchProducts(currentPage);
      } else {
        showError(res.message || 'Failed to remove product.');
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      showError(errorResponse.response?.data?.message || 'Failed to delete product.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="space-y-8 pb-16">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Your Products</h1>
          <p className="text-xs text-slate-500 mt-1">
            Manage product listings, inventory levels, pricing, and active statuses ({totalElements}{' '}
            items total)
          </p>
        </div>

        <Link
          to="/merchant/products/new"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 text-white text-xs font-bold rounded-xl hover:bg-indigo-700 transition shadow-md shadow-indigo-100 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add New Product</span>
        </Link>
      </div>

      {/* Table Container */}
      <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-xs">
        {isLoading ? (
          <div className="py-16">
            <LoadingSpinner size="lg" message="Loading your catalog..." />
          </div>
        ) : products.length === 0 ? (
          <div className="py-16 text-center text-slate-500 text-xs space-y-3">
            <Package className="w-10 h-10 mx-auto text-slate-300" />
            <p className="text-sm font-bold text-slate-700">No products listed yet.</p>
            <p className="text-slate-400">Click below to create your very first product listing.</p>
            <div className="pt-2">
              <Link
                to="/merchant/products/new"
                className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white text-xs font-semibold rounded-xl"
              >
                <Plus className="w-4 h-4" />
                <span>Add Product</span>
              </Link>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-100 text-slate-400 uppercase tracking-wider text-[10px] bg-slate-50/50">
                  <th className="py-3.5 px-4">Product</th>
                  <th className="py-3.5 px-4">Category</th>
                  <th className="py-3.5 px-4 text-right">Price</th>
                  <th className="py-3.5 px-4 text-center">Available Stock</th>
                  <th className="py-3.5 px-4 text-center">Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {products.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/70 transition-colors">
                    <td className="py-3.5 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-slate-100 overflow-hidden flex-shrink-0 border border-slate-100">
                          <img
                            src={resolveImageUrl(p.imageUrl) || getProductFallbackSvg(p.name, p.categoryName)}
                            alt={p.name}
                            referrerPolicy="no-referrer"
                            onError={(e) => {
                              (e.currentTarget as HTMLImageElement).src = getProductFallbackSvg(p.name, p.categoryName);
                            }}
                            className="w-full h-full object-cover"
                          />
                        </div>
                        <div className="min-w-0">
                          <span className="font-bold text-slate-900 block truncate">{p.name}</span>
                          <span className="font-mono text-[10px] text-slate-400 block">{p.sku}</span>
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4">
                      <span className="px-2.5 py-1 text-[11px] font-medium rounded-lg bg-slate-100 text-slate-700">
                        {p.categoryName}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-right font-extrabold text-slate-900 font-mono">
                      {formatCurrency(p.price)}
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <span
                        className={`inline-block px-2.5 py-1 rounded-full text-xs font-bold ${p.availableStock > 5
                            ? 'bg-emerald-50 text-emerald-700'
                            : p.availableStock > 0
                              ? 'bg-amber-50 text-amber-700'
                              : 'bg-rose-50 text-rose-700'
                          }`}
                      >
                        {p.availableStock}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      {p.active ? (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600">
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-slate-400">
                          <XCircle className="w-3.5 h-3.5" />
                          Inactive
                        </span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 text-right space-x-1">
                      <button
                        type="button"
                        onClick={() => navigate(`/merchant/products/edit/${p.id}`)}
                        className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                        title="Edit product"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeletingProduct(p)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                        title="Delete product"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-slate-100 px-6 py-3 bg-slate-50/50">
            <button
              disabled={currentPage === 0}
              onClick={() => fetchProducts(currentPage - 1)}
              className="text-xs font-semibold text-slate-600 hover:text-indigo-600 disabled:opacity-40"
            >
              &larr; Previous
            </button>
            <span className="text-xs text-slate-500 font-medium">
              Page {currentPage + 1} of {totalPages}
            </span>
            <button
              disabled={currentPage >= totalPages - 1}
              onClick={() => fetchProducts(currentPage + 1)}
              className="text-xs font-semibold text-slate-600 hover:text-indigo-600 disabled:opacity-40"
            >
              Next &rarr;
            </button>
          </div>
        )}
      </div>

      {/* Delete Confirmation Modal */}
      <ConfirmationModal
        isOpen={Boolean(deletingProduct)}
        title="Delete Product?"
        message={`Are you sure you want to delete "${deletingProduct?.name}"? Customers will no longer be able to purchase this product.`}
        confirmText="Delete Product"
        cancelText="Cancel"
        isDestructive={true}
        isLoading={isDeleting}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeletingProduct(null)}
      />
    </div>
  );
};

