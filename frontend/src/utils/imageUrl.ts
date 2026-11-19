/**
 * Utility to resolve product image URLs, handle search engine redirect links,
 * provide high-quality inline SVG fallbacks for all product categories,
 * and support local client-side image compression for direct base64 uploads.
 */

export function resolveImageUrl(rawUrl?: string | null): string {
  if (!rawUrl || typeof rawUrl !== 'string') return '';
  const trimmed = rawUrl.trim();
  if (!trimmed) return '';

  // Already a data URI (base64 or svg)
  if (trimmed.startsWith('data:image/')) {
    return trimmed;
  }

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
 * Returns a sleek, modern inline SVG Data URI tailored to the product name and category.
 * Used as an instant fallback when an external image link fails or is offline.
 */
export function getProductFallbackSvg(name?: string, category?: string): string {
  const query = `${name || ''} ${category || ''}`.toLowerCase();

  let gradientFrom = '#4f46e5';
  let gradientTo = '#7c3aed';
  let iconPath = '';
  let label = 'Product';

  if (query.includes('watch')) {
    gradientFrom = '#6366f1';
    gradientTo = '#4338ca';
    label = 'Smart Watch';
    // Elegant smartwatch SVG path: strap + rounded dial + screen + hands
    iconPath = `
      <rect x="155" y="40" width="90" height="70" rx="14" fill="#312e81" opacity="0.6"/>
      <rect x="155" y="290" width="90" height="70" rx="14" fill="#312e81" opacity="0.6"/>
      <rect x="120" y="90" width="160" height="220" rx="36" fill="#1e1b4b" stroke="#818cf8" stroke-width="4"/>
      <rect x="135" y="105" width="130" height="190" rx="24" fill="#0f172a"/>
      <circle cx="200" cy="200" r="55" fill="none" stroke="#4f46e5" stroke-width="4" stroke-dasharray="8 4"/>
      <circle cx="200" cy="200" r="5" fill="#38bdf8"/>
      <line x1="200" y1="200" x2="200" y2="165" stroke="#38bdf8" stroke-width="3" stroke-linecap="round"/>
      <line x1="200" y1="200" x2="225" y2="200" stroke="#f43f5e" stroke-width="2.5" stroke-linecap="round"/>
      <text x="200" y="275" font-family="system-ui, -apple-system, sans-serif" font-size="14" font-weight="600" fill="#a5b4fc" text-anchor="middle">10:45 AM</text>
    `;
  } else if (query.includes('headphone') || query.includes('earphone') || query.includes('audio') || query.includes('buds')) {
    gradientFrom = '#ec4899';
    gradientTo = '#be185d';
    label = 'Audio / Headphones';
    // Sleek over-ear headphones SVG
    iconPath = `
      <path d="M120 220 C120 120 280 120 280 220" fill="none" stroke="#fbcfe8" stroke-width="14" stroke-linecap="round"/>
      <rect x="100" y="200" width="40" height="85" rx="20" fill="#831843" stroke="#f472b6" stroke-width="4"/>
      <rect x="260" y="200" width="40" height="85" rx="20" fill="#831843" stroke="#f472b6" stroke-width="4"/>
      <circle cx="200" cy="235" r="30" fill="none" stroke="#fbcfe8" stroke-width="3" opacity="0.4"/>
      <path d="M190 225 L215 235 L190 245 Z" fill="#fbcfe8"/>
    `;
  } else if (query.includes('laptop') || query.includes('macbook') || query.includes('pc') || query.includes('computer')) {
    gradientFrom = '#0284c7';
    gradientTo = '#0369a1';
    label = 'Laptop / Computing';
    // Sleek open laptop SVG
    iconPath = `
      <rect x="110" y="110" width="180" height="125" rx="10" fill="#0f172a" stroke="#38bdf8" stroke-width="3"/>
      <rect x="120" y="120" width="160" height="105" rx="6" fill="#1e293b"/>
      <circle cx="200" cy="170" r="16" fill="#38bdf8" opacity="0.8"/>
      <path d="M70 245 L330 245 C335 245 338 250 334 255 L320 270 C316 274 310 276 304 276 L96 276 C90 276 84 274 80 270 L66 255 C62 250 65 245 70 245 Z" fill="#334155" stroke="#64748b" stroke-width="2"/>
      <rect x="170" y="248" width="60" height="5" rx="2.5" fill="#94a3b8"/>
    `;
  } else if (query.includes('phone') || query.includes('smartphone') || query.includes('mobile')) {
    gradientFrom = '#0d9488';
    gradientTo = '#0f766e';
    label = 'Smartphone';
    // Sleek mobile phone SVG
    iconPath = `
      <rect x="135" y="80" width="130" height="240" rx="26" fill="#111827" stroke="#2dd4bf" stroke-width="4"/>
      <rect x="145" y="96" width="110" height="208" rx="16" fill="#1f2937"/>
      <rect x="175" y="87" width="50" height="5" rx="2.5" fill="#4b5563"/>
      <circle cx="200" cy="285" r="7" fill="#2dd4bf" opacity="0.6"/>
      <circle cx="200" cy="170" r="30" fill="#14b8a6" opacity="0.3"/>
    `;
  } else if (query.includes('shoe') || query.includes('footwear') || query.includes('sneaker')) {
    gradientFrom = '#ea580c';
    gradientTo = '#c2410c';
    label = 'Footwear';
    // Modern running sneaker silhouette
    iconPath = `
      <path d="M80 260 L320 260 C325 260 330 252 322 245 L285 210 C275 200 260 195 245 195 L190 195 L160 145 C155 138 145 135 135 140 L110 155 C105 158 102 165 105 170 L115 195 L90 220 C80 230 78 245 80 260 Z" fill="#fff7ed" stroke="#fb923c" stroke-width="3"/>
      <path d="M75 260 L325 260 L325 272 L75 272 Z" fill="#7c2d12" rx="4"/>
    `;
  } else if (query.includes('cloth') || query.includes('shirt') || query.includes('dress') || query.includes('fashion') || query.includes('jacket')) {
    gradientFrom = '#8b5cf6';
    gradientTo = '#6d28d9';
    label = 'Fashion & Apparel';
    // T-shirt / Apparel silhouette
    iconPath = `
      <path d="M150 100 Q200 130 250 100 L290 135 L260 175 L235 160 L235 290 L165 290 L165 160 L140 175 L110 135 Z" fill="#f5f3ff" stroke="#c4b5fd" stroke-width="3"/>
    `;
  } else {
    // Premium 3D isometric gift box / parcel
    gradientFrom = '#475569';
    gradientTo = '#1e293b';
    label = 'Official Product';
    iconPath = `
      <path d="M200 100 L280 145 L200 190 L120 145 Z" fill="#64748b" stroke="#94a3b8" stroke-width="2"/>
      <path d="M120 145 L200 190 L200 280 L120 235 Z" fill="#334155" stroke="#94a3b8" stroke-width="2"/>
      <path d="M280 145 L200 190 L200 280 L280 235 Z" fill="#1e293b" stroke="#94a3b8" stroke-width="2"/>
      <circle cx="200" cy="190" r="12" fill="#38bdf8"/>
    `;
  }

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" width="100%" height="100%">
    <defs>
      <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="${gradientFrom}"/>
        <stop offset="100%" stop-color="${gradientTo}"/>
      </linearGradient>
      <pattern id="grid" width="20" height="20" patternUnits="userSpaceOnUse">
        <circle cx="10" cy="10" r="1" fill="#ffffff" opacity="0.08"/>
      </pattern>
    </defs>
    <rect width="400" height="400" fill="url(#g)"/>
    <rect width="400" height="400" fill="url(#grid)"/>
    ${iconPath}
    <rect x="20" y="340" width="360" height="36" rx="10" fill="#000000" opacity="0.35"/>
    <text x="200" y="364" font-family="system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif" font-size="13" font-weight="700" letter-spacing="1" fill="#ffffff" text-anchor="middle" text-transform="uppercase">${label}</text>
  </svg>`;

  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

/**
 * Compresses an image file from user's local disk and converts it to a compact Base64 Data URL.
 * Automatically limits dimension to maxWidth (default 800px) and quality (default 0.8)
 * so it fits cleanly into the database without size bottlenecks.
 */
export function compressAndConvertToBase64(
  file: File,
  maxWidth = 800,
  maxHeight = 800,
  quality = 0.8
): Promise<string> {
  return new Promise((resolve, reject) => {
    if (!file.type.startsWith('image/')) {
      return reject(new Error('Selected file is not an image'));
    }

    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.onload = (readerEvent) => {
      const img = new Image();
      img.onerror = () => reject(new Error('Failed to load image element'));
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        if (width > maxWidth || height > maxHeight) {
          if (width > height) {
            height = Math.round((height * maxWidth) / width);
            width = maxWidth;
          } else {
            width = Math.round((width * maxHeight) / height);
            height = maxHeight;
          }
        }

        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d');
        if (!ctx) {
          return resolve(readerEvent.target?.result as string);
        }

        ctx.drawImage(img, 0, 0, width, height);
        // Use jpeg compression for high-quality low-byte storage
        const mimeType = file.type === 'image/png' ? 'image/png' : 'image/jpeg';
        const dataUrl = canvas.toDataURL(mimeType, quality);
        resolve(dataUrl);
      };
      img.src = readerEvent.target?.result as string;
    };
    reader.readAsDataURL(file);
  });
}

/**
 * Curated sample catalog presets with high-res Unsplash links and instant offline SVG fallbacks.
 */
export const SAMPLE_PRODUCT_IMAGES = [
  {
    name: 'Smart Watch',
    url: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80',
    svgFallback: getProductFallbackSvg('smart watch', 'watches'),
  },
  {
    name: 'Headphones',
    url: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80',
    svgFallback: getProductFallbackSvg('headphones', 'electronics'),
  },
  {
    name: 'Laptop',
    url: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop&q=80',
    svgFallback: getProductFallbackSvg('laptop', 'computers'),
  },
  {
    name: 'Smartphone',
    url: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop&q=80',
    svgFallback: getProductFallbackSvg('smartphone', 'mobile'),
  },
  {
    name: 'Running Shoes',
    url: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop&q=80',
    svgFallback: getProductFallbackSvg('running shoes', 'footwear'),
  },
  {
    name: 'Backpack',
    url: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80',
    svgFallback: getProductFallbackSvg('backpack', 'accessories'),
  },
];
