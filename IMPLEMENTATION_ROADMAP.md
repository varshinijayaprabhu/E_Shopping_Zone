# E-ShoppingZone Implementation Roadmap

This roadmap governs the systematic, phase-by-phase execution of the E-ShoppingZone application.

---

## Phase 1: Analysis & Architectural Blueprint (COMPLETED)
- **Objective**: Thoroughly analyze `E-ShoppingZone.pdf`, establish architecture, domain boundaries, database schemas, API contracts, security models, and project documentation.
- **Key Deliverables**:
  - Full requirements breakdown (functional and non-functional).
  - Compatibility verification matrix (Java 8, Spring Boot 2.7.x, Spring Cloud 2021.0.x).
  - 46 comprehensive documentation specifications in `docs/`.
  - Database schema scripts in `database/schema/init-all-databases.sql`.
  - Seed data in `database/seed/seed-data.sql`.
  - Master roadmap, project status, and development guides.

---

## Phase 2: Project Foundation & Service Discovery
- **Objective**: Establish monorepo structure, root Maven POM, Eureka Discovery Server, and Spring Cloud API Gateway.
- **Tasks**:
  1. Initialize root `pom.xml` with Java 8 compiler settings and strict Spring Boot 2.7.18 / Spring Cloud 2021.0.9 dependency management.
  2. Implement `discovery-server` with `@EnableEurekaServer` on port `8761`.
  3. Implement `api-gateway` with Spring Cloud Gateway on port `8080`.
  4. Configure reactive CORS filter, correlation ID filter, and perimeter route definitions.
  5. Verify Eureka registration and Gateway reverse proxy routing.

---

## Phase 3: Profile Microservice & Security
- **Objective**: Build identity, account management, and security infrastructure.
- **Tasks**:
  1. Create `profile-service` on port `8081` connecting to `profile_db`.
  2. Implement `User` and `RefreshToken` entities and Spring Data repositories.
  3. Implement registration and login with BCrypt password hashing (`Password@123`).
  4. Implement `JwtTokenProvider` generating 15-minute access tokens with user claims.
  5. Implement Refresh Token Rotation (RTR): 7-day opaque tokens stored as SHA-256 hashes in MySQL, delivered via `HttpOnly` cookie.
  6. Implement replay attack detection with total session invalidation.
  7. Implement GitHub OAuth 2.0 authorization code flow.
  8. Implement profile query and update endpoints (`GET/PUT /api/profile`).
  9. Add Springdoc OpenAPI 1.8.x Swagger UI and unit/integration tests.

---

## Phase 4: Product Microservice
- **Objective**: Implement product catalog, category hierarchy, and inventory control.
- **Tasks**:
  1. Create `product-service` on port `8082` connecting to `product_db`.
  2. Implement `Category`, `Product`, and `Inventory` entities.
  3. Implement public product catalog listing with pagination, sorting, category filtering, and price filters.
  4. Implement product search endpoint (`GET /api/products/search`).
  5. Implement merchant product CRUD with strict resource ownership validation (`ROLE_MERCHANT`).
  6. Implement internal stock reservation (`POST /api/products/reserve`) and stock release (`POST /api/products/release`).
  7. Add Swagger UI and unit/integration tests.

---

## Phase 5: Cart Microservice
- **Objective**: Implement customer shopping cart and authoritative price synchronization.
- **Tasks**:
  1. Create `cart-service` on port `8083` connecting to `cart_db`.
  2. Implement `Cart` and `CartItem` entities.
  3. Implement `@LoadBalanced RestTemplate` client `ProductServiceClient` to fetch live prices and verify stock.
  4. Implement add item, update quantity, remove item, and get cart endpoints.
  5. Implement cart clear endpoint (`DELETE /api/cart`).
  6. Add Swagger UI and unit/integration tests.

---

## Phase 6: Wallet Microservice
- **Objective**: Build the digital wallet balance ledger and idempotent payment engine.
- **Tasks**:
  1. Create `wallet-service` on port `8085` connecting to `wallet_db`.
  2. Implement `Wallet` and `WalletTransaction` entities with `BigDecimal` monetary values.
  3. Implement balance inquiry with auto-creation of default wallet.
  4. Implement deposit and withdrawal endpoints.
  5. Implement idempotent payment deduction with pessimistic locking (`SELECT ... FOR UPDATE`).
  6. Implement compensating refund endpoint.
  7. Implement transaction ledger query endpoint.
  8. Add Swagger UI and unit/integration tests.

---

## Phase 7: Order Microservice
- **Objective**: Orchestrate multi-service checkout and distributed compensating Saga.
- **Tasks**:
  1. Create `order-service` on port `8084` connecting to `order_db`.
  2. Implement `Order`, `OrderItem`, and `PaymentRecord` entities with immutable historical price snapshots.
  3. Implement `OrderCheckoutSagaOrchestrator`:
     - Query Cart Service for active items.
     - Call Product Service to reserve stock.
     - Call Wallet Service to deduct funds (or mark COD).
     - Persist order record.
     - Clear Cart Service.
     - Execute compensating actions (refund wallet, release stock) if persistence fails.
  4. Implement order cancellation with compensating rollbacks.
  5. Implement customer order history and merchant order visibility.
  6. Add Swagger UI and unit/integration tests.

---

## Phase 8: React Frontend Application
- **Objective**: Build modern, responsive, and secure UI with React 18, TypeScript, and Vite.
- **Tasks**:
  1. Initialize Vite React + TypeScript project under `frontend/e-shopping-ui`.
  2. Implement centralized Axios API client with in-memory token state and automatic refresh interceptor.
  3. Implement `AuthContext` supporting login, registration, GitHub OAuth callback, and clean logout.
  4. Implement Customer views: Homepage, Catalog, Search, Product Details, Cart, Checkout, Wallet, Orders, Profile.
  5. Implement Merchant views: Dashboard, Product Management, Add/Edit Product, Order List.
  6. Implement Protected Route wrappers based on `ROLE_CUSTOMER` and `ROLE_MERCHANT`.

---

## Phase 9: Testing & Quality Assurance
- **Objective**: Validate complete business behavior, security boundaries, and edge cases.
- **Tasks**:
  1. Execute unit test suites across all 7 backend services.
  2. Execute controller and security integration tests.
  3. Build and execute Postman collections covering all customer and merchant user journeys.
  4. Verify security tests (unauthorized access, token replay, expired JWT, merchant cross-tenant tampering).

---

## Phase 10: Final Documentation & Verification
- **Objective**: Complete end-to-end verification and deliver final artifacts.
- **Tasks**:
  1. Verify Swagger UI on all microservices (`http://localhost:8081..8085/swagger-ui/index.html`).
  2. Verify all markdown documentation and diagrams reflect code reality.
  3. Final review against all 37 acceptance criteria.

