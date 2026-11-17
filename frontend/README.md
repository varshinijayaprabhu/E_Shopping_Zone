# E-ShoppingZone Frontend Application (React 18 + TypeScript)

Production-grade e-commerce single-page application built for the **Capgemini E-ShoppingZone Microservices Architecture**.

---

## 1. Technology Baseline

| Component | Specification |
| :--- | :--- |
| **Framework** | React 18.3.1 (Strictly React 18) |
| **Language** | TypeScript 5.6 |
| **Bundler / Tooling** | Vite 5.4 |
| **Styling** | Tailwind CSS v3.4 |
| **Routing** | React Router DOM v6.28 |
| **State Management**| React Context API (Auth, Cart, Wallet, Notifications) |
| **HTTP Client** | Axios 1.7 with Axios Interceptors |
| **Iconography** | Lucide React |
| **Unit & Component Testing** | Vitest 2.1 + React Testing Library + jsdom |

---

## 2. Architecture & Security Policies

1. **Dual-Token Authentication Flow**:
   - Short-lived Access Tokens (15 min) are held exclusively in **in-memory application state** (`AuthContext` & `axiosClient.ts`). They are never stored in `localStorage` or `sessionStorage` to mitigate XSS exfiltration risks.
   - Long-lived Refresh Tokens (7 days) are handled automatically by the browser via **HttpOnly SameSite cookies** (`withCredentials: true`).
   - Seamless token refreshment: Axios 401 response interceptor queues failed requests, invokes `/api/auth/refresh`, updates the in-memory access token, and retries pending requests.
2. **Reverse Proxy Configuration**:
   - In development, Vite reverse-proxies `/api` directly to `http://localhost:8080` (Spring Cloud API Gateway).
   - This eliminates CORS friction while preserving browser cookie transmission.
3. **Zero-Trust Access Control**:
   - `ProtectedRoute`: Guarantees that only authenticated users access customer or merchant dashboards.
   - `RoleRoute`: Strictly separates `ROLE_CUSTOMER` workflows from `ROLE_MERCHANT` management consoles.
   - Client-side checks are purely for UX; the backend microservices strictly enforce ownership and role scopes.
4. **Localization & Financial Standards**:
   - All currencies are displayed strictly in Indian Rupee format (`₹ 14,999.00`).
   - Customer phone numbers and PII are masked in UI views (`987****210`).

---

## 3. Directory Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── axiosClient.ts       # In-memory token management, 401 refresh queue
│   │   ├── authApi.ts           # Login, registration, profile, refresh
│   │   ├── productApi.ts        # Catalog, categories, search, merchant CRUD
│   │   ├── cartApi.ts           # Cart operations, quantity updates
│   │   ├── walletApi.ts         # Wallet balance, ledger, deposit
│   │   └── orderApi.ts          # Checkout saga, customer/merchant orders
│   ├── components/
│   │   ├── common/
│   │   │   ├── Navbar.tsx       # Responsive nav with search, cart badge, wallet pill
│   │   │   ├── Footer.tsx       # System overview, microservices status, links
│   │   │   ├── LoadingSpinner.tsx
│   │   │   ├── ToastContainer.tsx
│   │   │   ├── ConfirmationModal.tsx # Non-native modal replacing confirm()
│   │   │   ├── ProtectedRoute.tsx
│   │   │   ├── RoleRoute.tsx
│   │   │   └── SkeletonCard.tsx # Loading placeholders
│   │   ├── product/
│   │   │   └── ProductCard.tsx  # Product card with stock indicator & add-to-cart
│   │   └── order/
│   │       └── OrderStatusBadge.tsx
│   ├── context/
│   │   ├── AuthContext.tsx      # User session, login, register, role checks
│   │   ├── CartContext.tsx      # Cart item count, sync with Cart Service
│   │   ├── WalletContext.tsx    # Wallet balance, simulated UPI deposits
│   │   └── NotificationContext.tsx # Global toast notification stack
│   ├── hooks/
│   │   ├── useDebounce.ts       # Debounced search inputs
│   │   └── index.ts
│   ├── pages/
│   │   ├── HomePage.tsx         # Hero banner, popular categories, featured items
│   │   ├── ProductListPage.tsx  # Search, filter by category/price, sort, pagination
│   │   ├── ProductDetailPage.tsx# Full product specs, quantity, instant buy
│   │   ├── CartPage.tsx         # Line items, quantity modify, subtotal
│   │   ├── CheckoutPage.tsx     # Delivery address, Wallet vs COD, saga trigger
│   │   ├── WalletPage.tsx       # Top-up form (UPI/Card), transaction ledger table
│   │   ├── OrderHistoryPage.tsx # Order list with status pills & tracking
│   │   ├── OrderDetailPage.tsx  # Order snapshot, item breakdown, cancel order
│   │   ├── ProfilePage.tsx      # User profile view and update
│   │   ├── LoginPage.tsx        # Sign-in with one-click demo logins & GitHub OAuth
│   │   ├── RegisterPage.tsx     # Customer & Merchant (invite-code) signup
│   │   ├── OAuthRedirectHandler.tsx
│   │   ├── NotFoundPage.tsx
│   │   └── merchant/
│   │       ├── MerchantDashboardPage.tsx  # Merchant metrics, recent orders
│   │       ├── MerchantProductsPage.tsx   # Catalog management, inventory
│   │       ├── MerchantProductFormPage.tsx# Create / Edit product
│   │       └── MerchantOrdersPage.tsx     # Items ordered by customers
│   ├── types/
│   │   └── index.ts             # Complete TypeScript interfaces
│   ├── utils/
│   │   └── formatters.ts        # formatCurrency (₹), formatDate, maskPhone
│   ├── App.tsx                  # Root router & layout configuration
│   └── main.tsx                 # Application entry point
├── package.json
├── tsconfig.json
├── vite.config.ts
└── tailwind.config.js
```

---

## 4. Running the Application

### Development Mode:
```bash
cd frontend
npm run dev
```
The application will launch on **`http://localhost:5173`**.

### Running Automated Tests:
```bash
cd frontend
npm test
```
Executes all 20 component and unit test suites across formatters, route guards, product cards, order status badges, and confirmation modals.

### Building for Production:
```bash
cd frontend
npm run build
```
Generates production-optimized JavaScript, CSS, and HTML bundles in `frontend/dist/`.
