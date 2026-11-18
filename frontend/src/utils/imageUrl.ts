/**
 * Utility to resolve product image URLs.
 * Handles search engine redirect links (e.g., Bing images search URLs with mediaurl=
 * or Google images search URLs with imgurl=) and extracts the actual direct image link.
 */
export function resolveImageUrl(rawUrl?: string | null): string {
  if (!rawUrl || typeof rawUrl !== 'string') return '';
  const trimmed = rawUrl.trim();
  if (!trimmed) return '';

  try {
    if (
      trimmed.includes('bing.com') ||
      trimmed.includes('google.') ||
      trimmed.includes('mediaurl=') ||
      trimmed.includes('imgurl=') ||
      trimmed.includes('img_url=')
    ) {
      const url = new URL(trimmed);
      const mediaUrl =
        url.searchParams.get('mediaurl') ||
        url.searchParams.get('imgurl') ||
        url.searchParams.get('img_url') ||
        url.searchParams.get('url');

      if (mediaUrl) {
        return decodeURIComponent(mediaUrl);
      }
    }
  } catch {
    // If not a parseable URL object, return raw trimmed string
  }

  return trimmed;
}

/**
 * Curated high-resolution Unsplash sample images for testing catalog products.
 */
export const SAMPLE_PRODUCT_IMAGES = [
  {
    name: 'Smart Watch',
    url: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80',
  },
  {
    name: 'Headphones',
    url: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80',
  },
  {
    name: 'Laptop',
    url: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop&q=80',
  },
  {
    name: 'Smartphone',
    url: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop&q=80',
  },
  {
    name: 'Running Shoes',
    url: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop&q=80',
  },
  {
    name: 'Backpack',
    url: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80',
  },
];
