import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, Package, CheckCircle2, AlertCircle, Sparkles, Upload, Image as ImageIcon } from 'lucide-react';
import { productApi } from '../../api/productApi';
import { Category, ProductCreatePayload, ProductUpdatePayload } from '../../types';
import { useNotification } from '../../context/NotificationContext';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { resolveImageUrl, SAMPLE_PRODUCT_IMAGES, compressAndConvertToBase64, getProductFallbackSvg } from '../../utils/imageUrl';

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
  const [isCustomCategory, setIsCustomCategory] = useState(false);
  const [customCategoryName, setCustomCategoryName] = useState('');
  const [price, setPrice] = useState<string>('999.00');
  const [stock, setStock] = useState<string>('25');
  const [imageUrl, setImageUrl] = useState('');
  const [previewError, setPreviewError] = useState(false);
  const [description, setDescription] = useState('');
  const [active, setActive] = useState(true);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageUrlChange = (val: string) => {
    const resolved = resolveImageUrl(val);
    setImageUrl(resolved);
    setPreviewError(false);
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const base64 = await compressAndConvertToBase64(file);
      setImageUrl(base64);
      setPreviewError(false);
      showSuccess(`Image "${file.name}" loaded successfully`);
    } catch (err: unknown) {
      showError((err as Error).message || 'Failed to process image file');
    }
  };

  const handleApplySvgGraphic = () => {
    const catName = isCustomCategory
      ? customCategoryName
      : categories.find((c) => c.id === categoryId)?.name;
    const svgData = getProductFallbackSvg(name, catName);
    setImageUrl(svgData);
    setPreviewError(false);
    showSuccess('Applied offline vector artwork');
  };

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
      let finalCategoryId = categoryId;

      if (isCustomCategory) {
        const trimmed = customCategoryName.trim();
        if (!trimmed) {
          setIsSubmitting(false);
          return showError('Custom category name is required');
        }

        const existing = categories.find(
          (c) => c.name.toLowerCase() === trimmed.toLowerCase()
        );

        if (existing) {
          finalCategoryId = existing.id;
        } else {
          const slug = trimmed
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/(^-|-$)/g, '');

          const catRes = await productApi.createCategory({
            name: trimmed,
            slug,
            description: `${trimmed} category`,
          });

          if (catRes.success && catRes.data) {
            finalCategoryId = catRes.data.id;
            setCategories((prev) => [...prev, catRes.data]);
          } else {
            setIsSubmitting(false);
            return showError(catRes.message || 'Failed to create category');
          }
        }
      }

      const cleanImageUrl = resolveImageUrl(imageUrl.trim());

      if (isEdit && id) {
        const updatePayload: ProductUpdatePayload = {
          categoryId: finalCategoryId,
          name: name.trim(),
          description: description.trim(),
          price: numPrice,
          imageUrl: cleanImageUrl,
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
          categoryId: finalCategoryId,
          name: name.trim(),
          sku: sku.trim().toUpperCase(),
          description: description.trim(),
          price: numPrice,
          imageUrl: cleanImageUrl,
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
      const errorResponse = err as {
        response?: {
          data?: {
            message?: string;
            fieldErrors?: Array<{ field: string; message: string }>;
          };
        };
        message?: string;
      };
      const fieldErrors = errorResponse.response?.data?.fieldErrors;
      if (fieldErrors && Array.isArray(fieldErrors) && fieldErrors.length > 0) {
        showError(fieldErrors.map((fe) => `${fe.field}: ${fe.message}`).join(', '));
      } else {
        showError(errorResponse.response?.data?.message || errorResponse.message || 'Operation failed');
      }
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
            <div className="flex items-center justify-between mb-1">
              <label htmlFor="productCategorySelect" className="block text-xs font-semibold text-slate-700">
                Category *
              </label>
              <button
                type="button"
                onClick={() => {
                  setIsCustomCategory(!isCustomCategory);
                  if (isCustomCategory) {
                    setCustomCategoryName('');
                  }
                }}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition"
              >
                {isCustomCategory ? '← Choose from List' : '+ Type Custom Category'}
              </button>
            </div>

            {isCustomCategory ? (
              <input
                id="customCategoryInput"
                type="text"
                required
                value={customCategoryName}
                onChange={(e) => setCustomCategoryName(e.target.value)}
                placeholder="e.g. Watches, Gaming, Footwear"
                className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            ) : (
              <select
                id="productCategorySelect"
                value={categoryId}
                onChange={(e) => {
                  if (e.target.value === '__NEW__') {
                    setIsCustomCategory(true);
                  } else {
                    setCategoryId(Number(e.target.value));
                  }
                }}
                className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
                <option value="__NEW__">+ Type Custom Category...</option>
              </select>
            )}
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

          <div className="sm:col-span-2 space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <label htmlFor="productImageUrlInput" className="block text-xs font-semibold text-slate-700">
                Product Image & Visuals
              </label>
              <div className="flex items-center gap-2">
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileUpload}
                  accept="image/png,image/jpeg,image/webp,image/svg+xml"
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded-xl transition-colors border border-indigo-200"
                >
                  <Upload className="w-3.5 h-3.5" />
                  <span>Upload Image File</span>
                </button>
                <button
                  type="button"
                  onClick={handleApplySvgGraphic}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-slate-100 text-slate-700 hover:bg-slate-200 rounded-xl transition-colors border border-slate-200"
                >
                  <ImageIcon className="w-3.5 h-3.5 text-indigo-500" />
                  <span>Use Offline Artwork</span>
                </button>
              </div>
            </div>

            <div className="space-y-1.5">
              <input
                id="productImageUrlInput"
                type="text"
                value={imageUrl}
                onChange={(e) => handleImageUrlChange(e.target.value)}
                placeholder="Paste web image URL (Unsplash, CDN) or upload a file above..."
                className="w-full px-3.5 py-2.5 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
              <div className="flex flex-wrap items-center gap-1.5 text-[11px] text-slate-500">
                <Sparkles className="w-3.5 h-3.5 text-indigo-500" />
                <span>Presets:</span>
                {SAMPLE_PRODUCT_IMAGES.map((sample) => (
                  <button
                    key={sample.name}
                    type="button"
                    onClick={() => handleImageUrlChange(sample.url)}
                    className="px-2 py-0.5 text-[10px] font-medium bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 rounded-md transition-colors"
                  >
                    {sample.name}
                  </button>
                ))}
              </div>
            </div>

            {/* Live Image Preview */}
            {imageUrl.trim() && (
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 flex items-center gap-4">
                <div className="w-18 h-18 rounded-xl bg-white border border-slate-200 overflow-hidden flex-shrink-0 flex items-center justify-center shadow-2xs">
                  {!previewError ? (
                    <img
                      src={imageUrl.trim()}
                      alt="Preview"
                      referrerPolicy="no-referrer"
                      onError={() => setPreviewError(true)}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <Package className="w-8 h-8 text-slate-300" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  {!previewError ? (
                    <div className="space-y-1">
                      <p className="text-xs font-semibold text-emerald-600 flex items-center gap-1.5">
                        <CheckCircle2 className="w-4 h-4" /> Image Preview Active
                      </p>
                      <p className="text-[11px] text-slate-500 truncate">
                        {imageUrl.startsWith('data:image/')
                          ? 'Local embedded image (100% offline & portable)'
                          : imageUrl}
                      </p>
                    </div>
                  ) : (
                    <div className="space-y-1">
                      <p className="text-xs font-semibold text-rose-600 flex items-center gap-1.5">
                        <AlertCircle className="w-4 h-4" /> Image failed to load
                      </p>
                      <p className="text-[11px] text-slate-500 leading-relaxed">
                        Tip: Click <strong>"Upload Image File"</strong> to pick a photo from your computer, or click <strong>"Use Offline Artwork"</strong> for an instant category vector visual.
                      </p>
                    </div>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setImageUrl('');
                    setPreviewError(false);
                  }}
                  className="text-xs text-slate-400 hover:text-rose-500 px-2 py-1 transition-colors"
                >
                  Clear
                </button>
              </div>
            )}
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

