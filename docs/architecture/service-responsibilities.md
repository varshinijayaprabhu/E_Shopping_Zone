# Service Responsibilities & Domain Boundaries

## 1. Discovery Server (`discovery-server`)
- **Core Technology**: Spring Cloud Netflix Eureka Server (`spring-cloud-starter-netflix-eureka-server`).
- **Domain**: System Infrastructure.
- **Responsibilities**:
  1. Maintain a dynamic registry of live microservice instances with health heartbeats (default renew interval: 30s).
  2. Provide cluster state and metadata to API Gateway and microservice clients.
  3. Expose the Eureka web dashboard on `http://localhost:8761`.

---

## 2. API Gateway (`api-gateway`)
- **Core Technology**: Spring Cloud Gateway (`spring-cloud-starter-gateway`), Reactive Spring Security (`spring-boot-starter-security`).
- **Domain**: Edge Routing & Security Perimeter.
- **Responsibilities**:
  1. Route all inbound traffic to downstream microservices using Eureka service discovery (`lb://SERVICE-NAME`).
  2. Enforce reactive JWT access token validation filter on protected endpoints.
  3. Extract claims (`userId`, `role`) and inject trusted internal forwarding headers (`X-User-Id`, `X-User-Email`, `X-User-Role`).
  4. Generate and propagate `X-Correlation-Id` for distributed trace logging.
  5. Centrally handle Cross-Origin Resource Sharing (CORS) for `http://localhost:5173`.
  6. Return uniform JSON error responses for unauthenticated (401) or forbidden (403) edge requests.

---

## 3. Profile Microservice (`profile-service`)
- **Core Technology**: Spring Boot Web, Spring Data JPA, Spring Security, JJWT (`jjwt-api`, `jjwt-impl`), Spring Security OAuth2 Client.
- **Domain**: Identity, Access Management, and User Profile.
- **Responsibilities**:
  1. Customer and Merchant registration with BCrypt password hashing.
  2. Local username/password authentication generating short-lived JWT Access Token and long-lived Refresh Token.
  3. Secure Refresh Token lifecycle: opaque token generation, SHA-256 hash storage in MySQL, rotation on renewal, and reuse detection.
  4. GitHub OAuth 2.0 social authentication and account linking.
  5. User profile retrieval and updating (Name, phone, address, profile photo URL).
  6. Merchant account management and status verification.
  7. Session revocation (`/logout` for current session, `/logout-all` for all user sessions).

---

## 4. Product Microservice (`product-service`)
- **Core Technology**: Spring Boot Web, Spring Data JPA, Hibernate, MySQL Connector/J.
- **Domain**: Product Catalog & Inventory.
- **Responsibilities**:
  1. Product CRUD operations with strict merchant ownership checks.
  2. Category management and hierarchy.
  3. Multi-criteria product search (by keyword, category, price range, stock availability).
  4. Pagination and sorting for large catalog listings.
  5. Authoritative inventory management and stock reservation APIs for checkout processing.
  6. Stock release / compensating rollback endpoint for cancelled checkouts.

---

## 5. Cart Microservice (`cart-service`)
- **Core Technology**: Spring Boot Web, Spring Data JPA, RestTemplate.
- **Domain**: Shopping Cart & Pre-Order Aggregation.
- **Responsibilities**:
  1. Maintain customer shopping carts and line items (`CartItem`).
  2. Add, update, and remove products from customer cart.
  3. Validate product availability and fetch authoritative pricing in real-time from `Product Service`.
  4. Compute line item sub-totals, cart item counts, and grand total.
  5. Empty/clear the cart after successful order placement.

---

## 6. Order Microservice (`order-service`)
- **Core Technology**: Spring Boot Web, Spring Data JPA, RestTemplate, Spring Cloud LoadBalancer.
- **Domain**: Order Orchestration & Fulfillment.
- **Responsibilities**:
  1. Checkout orchestration via distributed compensating Saga.
  2. Historical snapshotting of product names, descriptions, and prices at order creation time.
  3. Inter-service calls to Cart Service (fetch items), Product Service (reserve inventory), and Wallet Service (deduct balance for wallet payment).
  4. Support both `WALLET` and `CASH_ON_DELIVERY` payment methods.
  5. Compensating rollbacks: release reserved inventory and refund wallet deductions if order persistence fails.
  6. Order cancellation workflows with status transitions (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`).
  7. Historical order queries for customers and relevant orders for merchants.

---

## 7. Wallet Microservice (`wallet-service`)
- **Core Technology**: Spring Boot Web, Spring Data JPA, MySQL Connector/J.
- **Domain**: Financial Ledger & Payments.
- **Responsibilities**:
  1. Customer digital wallet creation upon initial registration/onboarding.
  2. Balance inquiries with strict owner-only access controls.
  3. Funds deposit and withdrawal with atomic ledger logging (`WalletTransaction`).
  4. Idempotent payment deduction endpoint invoked by `Order Service`.
  5. Compensating refund endpoint invoked during order failure recovery.
  6. Use of `BigDecimal` for all monetary arithmetic to eliminate floating-point rounding errors.
  7. Transaction history listing with filtering by transaction type (`DEPOSIT`, `WITHDRAWAL`, `PAYMENT`, `REFUND`).

