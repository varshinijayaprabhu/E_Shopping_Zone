import React, { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Filter, ChevronLeft, ChevronRight, X, SlidersHorizontal } from 'lucide-react';
import { productApi, ProductQueryParams } from '../api/productApi';
import { Category, Product } from '../types';
import { ProductCard } from '../components/product/ProductCard';
import { SkeletonCard } from '../components/common/SkeletonCard';

export const ProductListPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Pagination metadata
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters from searchParams or defaults
  const currentPage = parseInt(searchParams.get('page') || '0', 10);
  const currentCategory = searchParams.get('category') ? parseInt(searchParams.get('category')!, 10) : undefined;
  const currentSearch = searchParams.get('search') || '';
  const currentSort = searchParams.get('sortBy') || 'id';
  const currentDirection = (searchParams.get('direction') as 'ASC' | 'DESC') || 'DESC';

  const [searchInput, setSearchInput] = useState(currentSearch);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  // Load categories once
  useEffect(() => {
    productApi.getCategories().then((res) => {
      if (res.success && res.data) {
        setCategories(res.data);
      }
    });
  }, []);

  // Fetch products whenever params change
  const fetchProducts = useCallback(async () => {
    setIsLoading(true);
    try {
      if (currentSearch.trim()) {
        const res = await productApi.searchProducts(currentSearch.trim(), currentPage, 12);
        if (res.success && res.data) {
          setProducts(res.data.content);
          setTotalPages(res.data.totalPages);
          setTotalElements(res.data.totalElements);
        }
      } else {
        const query: ProductQueryParams = {
          page: currentPage,
          size: 12,
          categoryId: currentCategory,
          sortBy: currentSort,
          direction: currentDirection,
        };
        const res = await productApi.getProducts(query);
        if (res.success && res.data) {
          setProducts(res.data.content);
          setTotalPages(res.data.totalPages);
          setTotalElements(res.data.totalElements);
        }
      }
    } catch {
      setProducts([]);
    } finally {
      setIsLoading(false);
    }
  }, [currentSearch, currentPage, currentCategory, currentSort, currentDirection]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Update query params
  const updateQuery = (key: string, value: string | undefined) => {
    const nextParams = new URLSearchParams(searchParams);
    if (value !== undefined && value !== '') {
      nextParams.set(key, value);
    } else {
      nextParams.delete(key);
    }
    // Reset to page 0 on filter change (unless changing page itself)
    if (key !== 'page') {
      nextParams.set('page', '0');
    }
    setSearchParams(nextParams);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateQuery('search', searchInput.trim());
  };

  const clearAllFilters = () => {
    setSearchInput('');
    setSearchParams(new URLSearchParams());
  };

  return (
    <div className="space-y-6 pb-16">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Product Catalog</h1>
          <p className="text-xs text-slate-500 mt-1">
            Showing {products.length} of {totalElements} available items
          </p>
        </div>

        {/* Quick Search */}
        <form onSubmit={handleSearchSubmit} className="flex items-center gap-2 max-w-sm w-full">
          <div className="relative w-full">
            <input
              type="text"
              placeholder="Filter by keyword..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="w-full pl-9 pr-8 py-2 text-xs bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5 pointer-events-none" />
            {searchInput && (
              <button
                type="button"
                onClick={() => {
                  setSearchInput('');
                  updateQuery('search', '');
                }}
                className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
          <button
            type="submit"
            className="px-3.5 py-2 text-xs font-semibold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition"
          >
            Search
          </button>
        </form>
      </div>

      {/* Main Grid: Filters Sidebar + Products */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Mobile filter toggle */}
        <div className="lg:hidden flex items-center justify-between bg-white p-3 rounded-xl border border-slate-200">
          <button
            type="button"
            onClick={() => setMobileFilterOpen(!mobileFilterOpen)}
            className="flex items-center gap-2 text-xs font-semibold text-slate-700"
          >
            <SlidersHorizontal className="w-4 h-4" />
            <span>Filter Categories & Sort</span>
          </button>
          {(currentCategory || currentSearch) && (
            <button
              onClick={clearAllFilters}
              className="text-xs font-medium text-rose-600 hover:underline"
            >
              Clear filters
            </button>
          )}
        </div>

        {/* Filters Sidebar */}
        <div
          className={`${mobileFilterOpen ? 'block' : 'hidden'
            } lg:block lg:col-span-1 space-y-6 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs h-fit`}
        >
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="font-bold text-sm text-slate-900 flex items-center gap-2">
              <Filter className="w-4 h-4 text-indigo-600" />
              Filters
            </h3>
            {(currentCategory || currentSearch) && (
              <button
                onClick={clearAllFilters}
                className="text-xs text-rose-600 hover:underline font-medium"
              >
                Reset
              </button>
            )}
          </div>

          {/* Sort By */}
          <div>
            <label htmlFor="sortBySelect" className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
              Sort By
            </label>
            <select
              id="sortBySelect"
              value={`${currentSort}-${currentDirection}`}
              onChange={(e) => {
                const [sort, dir] = e.target.value.split('-');
                const next = new URLSearchParams(searchParams);
                next.set('sortBy', sort);
                next.set('direction', dir);
                next.set('page', '0');
                setSearchParams(next);
              }}
              className="w-full text-xs p-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500"
            >
              <option value="id-DESC">Newest First</option>
              <option value="price-ASC">Price: Low to High</option>
              <option value="price-DESC">Price: High to Low</option>
              <option value="name-ASC">Name: A to Z</option>
            </select>
          </div>

          {/* Categories */}
          <div>
            <span className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
              Category
            </span>
            <div className="space-y-1.5">
              <button
                type="button"
                onClick={() => updateQuery('category', undefined)}
                className={`w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition ${currentCategory === undefined
                    ? 'bg-indigo-50 text-indigo-700 font-bold'
                    : 'text-slate-600 hover:bg-slate-50'
                  }`}
              >
                All Categories
              </button>

              {categories.map((cat) => (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => updateQuery('category', cat.id.toString())}
                  className={`w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition ${currentCategory === cat.id
                      ? 'bg-indigo-50 text-indigo-700 font-bold'
                      : 'text-slate-600 hover:bg-slate-50'
                    }`}
                >
                  {cat.name}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Product Grid Area */}
        <div className="lg:col-span-3 space-y-6">
          {isLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {Array.from({ length: 6 }).map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </div>
          ) : products.length === 0 ? (
            <div className="bg-white rounded-2xl p-16 text-center border border-slate-200 shadow-xs space-y-3">
              <p className="text-base font-bold text-slate-700">No products match your criteria.</p>
              <p className="text-xs text-slate-500">
                Try adjusting your search terms, changing category or clearing filters.
              </p>
              <button
                onClick={clearAllFilters}
                className="mt-2 px-4 py-2 text-xs font-semibold bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition"
              >
                Clear all filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {products.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}

          {/* Pagination Controls */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between bg-white px-6 py-4 rounded-2xl border border-slate-200 shadow-xs">
              <button
                type="button"
                disabled={currentPage === 0}
                onClick={() => updateQuery('page', (currentPage - 1).toString())}
                className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-slate-700 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                <ChevronLeft className="w-4 h-4" />
                Previous
              </button>

              <span className="text-xs font-semibold text-slate-700">
                Page {currentPage + 1} of {totalPages}
              </span>

              <button
                type="button"
                disabled={currentPage >= totalPages - 1}
                onClick={() => updateQuery('page', (currentPage + 1).toString())}
                className="inline-flex items-center gap-1 px-3 py-1.5 text-xs font-medium text-slate-700 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                Next
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

