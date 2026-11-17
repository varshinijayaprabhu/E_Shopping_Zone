# CORS, CSRF, and XSS Defense Strategy

## 1. Cross-Origin Resource Sharing (CORS) Policy

Because the React single-page application runs on `http://localhost:5173` and the API Gateway runs on `http://localhost:8080`, Cross-Origin Resource Sharing is centrally configured at the Gateway level using a reactive `CorsWebFilter`.

### Configuration Parameters:
- **Allowed Origins**: `http://localhost:5173` (Explicit allowlist; wildcard `*` is strictly forbidden when credentials are enabled).
- **Allowed Methods**: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
- **Allowed Headers**: `Authorization`, `Content-Type`, `X-Requested-With`, `Accept`, `Origin`, `Access-Control-Request-Method`, `Access-Control-Request-Headers`, `X-Correlation-Id`, `X-CSRF-Protection`.
- **Exposed Headers**: `Set-Cookie`, `X-Correlation-Id`.
- **Allow Credentials**: `true` (Mandatory for transmitting the HttpOnly `eshopping_refresh_token` cookie across origins).
- **Max Age**: 3600 seconds (Caches preflight OPTIONS responses to minimize network roundtrips).

---

## 2. Explicit Cross-Site Request Forgery (CSRF) Defense Strategy

Because refresh tokens and logout endpoints rely on HTTP cookies (`eshopping_refresh_token`), E-ShoppingZone implements a multi-layered defense to prevent CSRF exploits:

### 2.1 Defense Layer 1: Strict Origin & Referer Verification
All incoming requests to cookie-consuming endpoints (`/api/auth/refresh`, `/api/auth/logout`, `/api/auth/logout-all`) must match the approved client origin:
- Gateway checks that `Origin` or `Referer` matches `http://localhost:5173`.
- Any cross-origin request originating from an arbitrary site is immediately rejected with `403 Forbidden`.

### 2.2 Defense Layer 2: Custom Request Header Verification (`X-Requested-With`)
Modern browsers restrict custom headers in cross-origin environments unless explicitly permitted by CORS preflight:
- The React client attaches `X-Requested-With: XMLHttpRequest` to all auth refresh and logout requests.
- Downstream filter verifies the presence and value of this header.
- Because malicious cross-site forms or scripts cannot set custom headers without passing a CORS preflight that only `http://localhost:5173` passes, cross-site CSRF attacks are neutralized.

### 2.3 Defense Layer 3: Cookie Attributes
- `SameSite=Lax`: Browsers will not attach the cookie to background cross-site image, script, or iframe requests.
- `Path=/api/auth`: Restricts cookie transmission exclusively to the authentication endpoints, preventing the cookie from being leaked or sent on other API routes.

---

## 3. Comprehensive XSS Defense Strategy

Because access tokens are stored in browser memory, a Cross-Site Scripting (XSS) vulnerability could allow malicious scripts running in the page to access in-memory tokens. E-ShoppingZone implements defense-in-depth against XSS:

### 3.1 Content Security Policy (CSP)
The frontend application and Gateway inject strict CSP headers:
```http
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://images.unsplash.com; connect-src 'self' http://localhost:8080; object-src 'none'; base-uri 'self';
```
- Restricts executable scripts strictly to application bundles served from `'self'`.
- Prohibits `unsafe-eval` and remote unverified script hosts.

### 3.2 Framework-Native JSX Output Encoding
- All user-controlled text (e.g. product names, customer descriptions, comments) is rendered using standard React JSX curly brace interpolation (`<div>{product.name}</div>`).
- React automatically escapes HTML entities, neutralizing HTML injection and `<script>` breakout.
- Usage of `dangerouslySetInnerHTML` is strictly prohibited throughout the codebase.

### 3.3 Strict In-Memory Scope Isolation
- Access tokens are encapsulated inside an unexposed React `AuthContext` closure.
- Tokens are never assigned to `window` globals or exported to public variables accessible by third-party scripts.
