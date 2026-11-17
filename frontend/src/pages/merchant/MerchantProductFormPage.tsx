import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, Package } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { Category, ProductCreatePayload, ProductUpdatePayload } from '../../types';
import { useNotification } from '../../context/NotificationContext';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';

export const MerchantProductFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const { showSuccess, showError } = useNotification();

  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(isEdit);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form Fields
  const [name, setName] = useState('');
  const [sku, setSku] = useState('');
  const [categoryId, setCategoryId] = useState<number>(1);
  const [price, setPrice] = useState<string>('999.00');
  const [stock, setStock] = useState<string>('25');
  const [imageUrl, setImageUrl] = useState('');
  const [description, setDescription] = useState('');
  const [active, setActive] = useState(true);

  useEffect(() => {
    // Load categories
    productApi.getCategories().then((res) => {
      if (res.success && res.data) {
        setCategories(res.data);
        if (!isEdit && res.data.length > 0) {
          setCategoryId(res.data[0].id);
        }
      }
    });

    if (isEdit && id) {
      productApi
        .getProductById(Number(id))
        .then((res) => {
          if (res.success && res.data) {
            const p = res.data;
            setName(p.name);
            setSku(p.sku);
            setCategoryId(p.categoryId);
            setPrice(p.price.toString());
            setStock(p.availableStock.toString());
            setImageUrl(p.imageUrl || '');
            setDescription(p.description || '');
            setActive(p.active);
          } else {
            showError('Product not found');
            navigate('/merchant/products');
          }
        })
        .catch(() => {
          showError('Failed to load product details');
          navigate('/merchant/products');
        })
        .finally(() => {
          setIsLoading(false);
        });
    }
  }, [id, isEdit, navigate, showError]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const numPrice = parseFloat(price);
    const numStock = parseInt(stock, 10);

    if (!name.trim()) return showError('Product name is required');
    if (!sku.trim()) return showError('SKU identifier is required');
    if (isNaN(numPrice) || numPrice <= 0) return showError('Valid price greater than 0 is required');
    if (isNaN(numStock) || numStock < 0) return showError('Valid stock quantity is required');

    setIsSubmitting(true);
    try {
      if (isEdit && id) {
        const updatePayload: ProductUpdatePayload = {
          categoryId,
          name: name.trim(),
          description: description.trim(),
          price: numPrice,
          imageUrl: imageUrl.trim(),
          availableStock: numStock,
          active,
        };
        const res = await productApi.updateProduct(Number(id), updatePayload);
        if (res.success) {
          showSuccess('Product updated successfully!');
          navigate('/merchant/products');
        } else {
          showError(res.message || 'Failed to update product');
        }
      } else {
        const createPayload: ProductCreatePayload = {
          categoryId,
          name: name.trim(),
          sku: sku.trim().toUpperCase(),
          description: description.trim(),
          price: numPrice,
          imageUrl: imageUrl.trim(),
          initialStock: numStock,
        };
        const res = await productApi.createProduct(createPayload);
        if (res.success) {
          showSuccess('Product created successfully!');
          navigate('/merchant/products');
        } else {
          showError(res.message || 'Failed to create product');
        }
      }
    } catch (err: unknown) {
      const errorResponse = err as { response?: { data?: { message?: string } }; message?: string };
      showError(errorResponse.response?.data?.message || 'Operation failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-[50vh] flex items-center justify-center">
        <LoadingSpinner size="lg" message="Loading product data..." />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-16">
      <div>
        <Link
          to="/merchant/products"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition mb-2"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Merchant Products
        </Link>
        <h1 className="text-2xl font-black text-slate-900 tracking-tight">
          {isEdit ? 'Edit Product Listing' : 'Create New Product'}
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          {isEdit
            ? 'Update pricing, description, stock count, and active visibility'
            : 'Publish a new item to the E-ShoppingZone marketplace catalog'}
        </p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="sm:col-span-2">
            <label htmlFor="productNameInput" className="block text-xs font-semibold text-slate-700 mb-1">
              Product Title *
            </label>
            <input
              id="productNameInput"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Ultra Slim 4K UHD Smart Television 55-inch"
              className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label htmlFor="productSkuInput" className="block text-xs font-semibold text-slate-700 mb-1">
              SKU Code *
            </label>
            <input
              id="productSkuInput"
              type="text"
              required
              disabled={isEdit}
              value={sku}
              onChange={(e) => setSku(e.target.value)}
              placeholder="e.g. ELEC-TV-55"
              className="w-full px-3.5 py-2.5 text-xs font-mono uppercase bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-slate-100 disabled:cursor-not-allowed"
            />
          </div>

          <div>
            <label htmlFor="productCategorySelect" className="block text-xs font-semibold text-slate-700 mb-1">
              Category *
            </label>
            <select
              id="productCategorySelect"
              value={categoryId}
              onChange={(e) => setCategoryId(Number(e.target.value))}
              className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="productPriceInput" className="block text-xs font-semibold text-slate-700 mb-1">
              Price (₹ INR) *
            </label>
            <input
              id="productPriceInput"
              type="number"
              step="0.01"
              min="0.01"
              required
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              placeholder="999.00"
              className="w-full px-3.5 py-2.5 text-xs font-bold bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label htmlFor="productStockInput" className="block text-xs font-semibold text-slate-700 mb-1">
              {isEdit ? 'Available Stock *' : 'Initial Stock Quantity *'}
            </label>
            <input
              id="productStockInput"
              type="number"
              min="0"
              required
              value={stock}
              onChange={(e) => setStock(e.target.value)}
              placeholder="25"
              className="w-full px-3.5 py-2.5 text-xs font-bold bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="sm:col-span-2">
            <label htmlFor="productImageUrlInput" className="block text-xs font-semibold text-slate-700 mb-1">
              Image URL
            </label>
            <input
              id="productImageUrlInput"
              type="url"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
              placeholder="https://images.unsplash.com/photo-..."
              className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div className="sm:col-span-2">
            <label htmlFor="productDescriptionInput" className="block text-xs font-semibold text-slate-700 mb-1">
              Description
            </label>
            <textarea
              id="productDescriptionInput"
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Detailed product features, specifications, and warranty information..."
              className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          {isEdit && (
            <div className="sm:col-span-2 flex items-center gap-2 pt-2">
              <input
                type="checkbox"
                id="activeStatusToggle"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500"
              />
              <label htmlFor="activeStatusToggle" className="text-xs font-semibold text-slate-800">
                Active in Storefront (available for purchase)
              </label>
            </div>
          )}
        </div>

        <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
          <Link
            to="/merchant/products"
            className="px-4 py-2.5 text-xs font-semibold text-slate-600 hover:text-slate-900 transition"
          >
            Cancel
          </Link>

          <button
            type="submit"
            disabled={isSubmitting}
            className="inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-xs font-bold bg-indigo-600 text-white hover:bg-indigo-700 active:scale-95 transition disabled:opacity-50 shadow-md shadow-indigo-100"
          >
            <Save className="w-4 h-4" />
            <span>{isSubmitting ? 'Saving...' : isEdit ? 'Update Product' : 'Create Product'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};

