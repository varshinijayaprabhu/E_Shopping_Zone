# E-ShoppingZone Comprehensive Testing & Quality Assurance Guide

[![Java 8](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5.8.2-blue.svg)](https://junit.org/junit5/)
[![Vitest](https://img.shields.io/badge/Vitest-1.6.0-green.svg)](https://vitest.dev/)
[![Tests](https://img.shields.io/badge/Backend%20Tests-120%2F120%20Passing-brightgreen.svg)]()
[![Tests](https://img.shields.io/badge/Frontend%20Tests-20%2F20%20Passing-brightgreen.svg)]()

This guide documents the comprehensive testing strategy, execution procedures, test suite breakdowns, concurrency test mechanics, and audit resolution for the **E-ShoppingZone** microservices platform.

---

## 1. Testing Strategy & Multi-Tier Architecture

The testing harness spans multiple automated layers to guarantee correctness, security boundaries, financial accuracy, and data consistency:

```mermaid
flowchart TD
    subgraph L4 ["Tier 4: Live System End-to-End Testing"]
        E2E["PowerShell E2E Verification (verify-full-system-e2e.ps1)<br/>8 Test Phases across all 7 Live Running Services"]
        Postman["Postman Automated Collection (37 Endpoints)"]
    end

    subgraph L3 ["Tier 3: Frontend Component & Unit Tests"]
        Vitest["Vitest 1.6.0 + React Testing Library<br/>20 Unit & Component Tests"]
    end

    subgraph L2 ["Tier 2: Backend Integration & MockMvc Tests"]
        MockMvc["Spring MockMvc Controller Tests (HTTP, Status, JSON Serialization)"]
        SecurityTests["Security & Anti-IDOR Tests (RBAC, Replay Detection, CSRF)"]
        ConcurrencyTests["Multi-Threaded Concurrency Tests (ExecutorService + CountDownLatch)"]
    end

    subgraph L1 ["Tier 1: Backend Unit & Repository Tests"]
        UnitTests["JUnit 5 + Mockito Service Logic Tests"]
        JpaTests["@DataJpaTest Repository & Constraint Validation"]
    end

    L4 --> L3
    L3 --> L2
    L2 --> L1
```

---

## 2. Backend Test Execution & Surefire Report Audit

### 2.1 Running the Full Backend Reactor Test Suite
Execute the entire backend test suite across all 8 modules from the project root:
```powershell
cd backend
mvn test
cd ..
```

### 2.2 Microservice Surefire Test Breakdown
Every microservice contains comprehensive JUnit 5 and Mockito tests. All tests execute in standard CI/CD environments without external service dependencies:

| Module / Microservice | Tests Run | Failures | Errors | Skipped | Status | Primary Test Coverage |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| `discovery-server` | 1 | 0 | 0 | 0 | **PASSED** | Eureka server context load and self-registration |
| `api-gateway` | 1 | 0 | 0 | 0 | **PASSED** | Gateway route configuration and context load |
| `profile-service` | 26 | 0 | 0 | 0 | **PASSED** | BCrypt hashing, JWT generation, RTR, Replay attack detection, Anti-CSRF filter, OAuth exchange |
| `product-service` | 25 | 0 | 0 | 0 | **PASSED** | Category CRUD, dynamic product filtering, search, merchant IDOR, Saga stock reservation & release |
| `cart-service` | 16 | 0 | 0 | 0 | **PASSED** | Line item aggregation, live price calculation, stock boundary check, Anti-IDOR isolation |
| `wallet-service` | 24 | 0 | 0 | 0 | **PASSED** | Pessimistic locking deductions, 10-thread concurrency, immutable ledger, idempotency keys, refund compensation |
| `order-service` | 27 | 0 | 0 | 0 | **PASSED** | Checkout Saga orchestration, COD workflow, compensation on payment failure, Anti-IDOR 404s, merchant item view |
| **Total Backend Reactor** | **120** | **0** | **0** | **0** | **100% PASS** | **Zero Regressions Across All Modules** |

---

## 3. Discrepancy Investigation: The 121 vs 120 Test Count Resolution

> [!NOTE]
> **Resolution of Test Count Discrepancy (Phase 7 cited 121 vs Phase 9/10 actual 120)**:
> In the Phase 7 completion report, the textual breakdown mistakenly enumerated 12 test scenarios for `OrderControllerIntegrationTest.java`. However, inspecting `backend/order-service/src/test/java/com/eshopping/order/OrderControllerIntegrationTest.java` confirms there are exactly **11 `@Test` methods**.
> 
> ```powershell
> Select-String -Path "backend\order-service\src\test\java\com\eshopping\order\OrderControllerIntegrationTest.java" -Pattern "@Test" | Measure-Object
> # Count: 11
> ```
> 
> The physical breakdown of `order-service` test methods is:
> - `OrderServiceApplicationTests.java`: 1 test
> - `OrderServiceTest.java`: 15 tests
> - `OrderControllerIntegrationTest.java`: 11 tests
> - **Total in Order Service**: 1 + 15 + 11 = **27 tests**.
> 
> Summing across all modules:
> `1 (discovery) + 1 (gateway) + 26 (profile) + 25 (product) + 16 (cart) + 24 (wallet) + 27 (order) = 120 tests`.
> 
> **Definitive Conclusion**: Exactly **120 tests** have always existed and executed in the Maven test reactor. Zero tests were ever deleted, consolidated, disabled, or removed. The number 121 in the Phase 7 report was simply a manual textual tally off-by-one error (12 tabulated instead of 11).

---

## 4. Specialized Concurrency, Security & IDOR Tests

### 4.1 Digital Wallet Multi-Threaded Concurrency Testing
File: `backend/wallet-service/src/test/java/com/eshopping/wallet/WalletServiceConcurrencyTest.java`
- **Objective**: Prove that simultaneous checkout payment attempts against a single wallet balance cannot overdraft or cause race conditions.
- **Mechanism**:
  - Uses `ExecutorService` with 10 concurrent worker threads.
  - Uses `CountDownLatch(1)` to hold all threads until simultaneous release.
  - Initial balance: ₹1,000.00.
  - 10 threads attempt to deduct ₹200.00 each simultaneously (total demand ₹2,000.00).
- **Result**: Exactly 5 threads succeed (total ₹1,000.00 deducted) and exactly 5 threads fail with `InsufficientBalanceException`. Final wallet balance is exactly ₹0.00. No negative balance occurs.

### 4.2 Refresh Token Rotation (RTR) & Replay Attack Defense Testing
File: `backend/profile-service/src/test/java/com/eshopping/profile/RefreshTokenServiceTest.java`
- **Objective**: Verify that reuse of a previously rotated refresh token triggers instantaneous revocation of all active sessions for that user.
- **Mechanism**:
  - Issues token $T_1$.
  - Rotates $T_1$ to issue $T_2$ ($T_1$ marked `revoked = TRUE`).
  - Simulates attacker replaying revoked token $T_1$.
- **Result**: System intercepts replayed $T_1$, throws `SecurityException`, and invalidates $T_2$ and all family tokens.

### 4.3 Anti-IDOR Customer Isolation Testing
File: `backend/order-service/src/test/java/com/eshopping/order/OrderControllerIntegrationTest.java`
- **Objective**: Verify that Customer A cannot read Customer B's order details.
- **Mechanism**:
  - Alice (User 1) places Order #101.
  - Bob (User 2) authenticates and sends `GET /api/orders/101`.
- **Result**: System responds with `404 Not Found` (deliberately masking existence to prevent IDOR enumeration).

---

## 5. Frontend Test Execution (Vitest 1.6.0)

### 5.1 Running the Frontend Test Suite
From the project root:
```powershell
cd frontend
npm test
cd ..
```

### 5.2 Frontend Test Suite Breakdown
The React test suite tests core components, auth context state management, cart recalculation, and route guards:

| Test File | Tests Run | Status | Coverage |
| :--- | :---: | :---: | :--- |
| `src/context/__tests__/AuthContext.test.tsx` | 5 | **PASSED** | Login state, memory JWT storage, logout cookie purge |
| `src/context/__tests__/CartContext.test.tsx` | 5 | **PASSED** | Item addition, quantity decrement, cart grand total |
| `src/components/__tests__/ProductCard.test.tsx` | 4 | **PASSED** | Price formatting, out-of-stock badge, add button |
| `src/pages/__tests__/OrderHistory.test.tsx` | 3 | **PASSED** | Order list rendering, status badges, cancel button |
| `src/utils/__tests__/formatters.test.ts` | 3 | **PASSED** | Currency formatting (INR `₹`), date localized display |
| **Total Frontend Tests** | **20** | **100% PASS** | **20 Passing, 0 Failing** |

---

## 6. Full-System End-to-End Verification (`verify-full-system-e2e.ps1`)

The platform includes a master end-to-end integration verification script: `verify-full-system-e2e.ps1`. This script tests the entire distributed ecosystem against live running services on Windows.

### 6.1 Executing the E2E Script
```powershell
# Ensure services are running (.\start-all-services.ps1)
.\verify-full-system-e2e.ps1
```

### 6.2 The 8 E2E Test Phases
```
================================================================================
  E-ShoppingZone Full-System End-to-End Integration Verification
================================================================================
[PHASE 1] Infrastructure & Service Discovery Health Checks
  - Probing Eureka Registry (:8761) ... UP
  - Probing API Gateway (:8080) ... UP
  - Probing Profile Service (:8081) ... UP
  - Probing Product Service (:8082) ... UP
  - Probing Cart Service (:8083) ... UP
  - Probing Wallet Service (:8085) ... UP
  - Probing Order Service (:8084) ... UP
  - Verifying Eureka registered instances ... 6 instances UP

[PHASE 2] Customer Authentication & Profile Verification
  - Authenticating demo customer (customer@eshopping.com) ... SUCCESS
  - Verifying JWT claims (sub, userId, roles) ... VERIFIED
  - Verifying user profile retrieval (GET /api/profile) ... VERIFIED

[PHASE 3] Merchant Catalog & Product Verification
  - Authenticating demo merchant (merchant@eshopping.com) ... SUCCESS
  - Fetching merchant owned products (GET /api/products/merchant) ... VERIFIED
  - Verifying category hierarchies ... 4 categories VERIFIED

[PHASE 4] Shopping Cart Management & Live Price Validation
  - Clearing customer shopping cart ... SUCCESS
  - Adding OnePlus 12 5G (Quantity: 1) ... SUCCESS
  - Adding Sony WH-1000XM5 (Quantity: 1) ... SUCCESS
  - Verifying live price calculation & grand total ... VERIFIED (Subtotal: ₹94,989.00)

[PHASE 5] Digital Wallet Operations & Concurrency Idempotency
  - Fetching initial wallet balance ... ₹100,000.00
  - Submitting deposit (₹25,000.00) with IdempotencyKey-1 ... SUCCESS
  - Replaying deposit with same IdempotencyKey-1 ... SUCCESS (No double credit)

[PHASE 6] Distributed Checkout Saga (Step A -> Step B -> Step C -> Step D)
  - Executing Checkout Saga with WALLET payment ... SUCCESS
  - Order Number: ORD-20261107-XXXX generated with status PLACED
  - Verifying stock reservation in Product Service ... Stock decremented by 1
  - Verifying wallet balance deduction in Wallet Service ... Deducted accurately
  - Verifying customer cart clearance ... Cart is EMPTY

[PHASE 7] Order Cancellation & Distributed Saga Compensating Rollback
  - Cancelling Order ORD-20261107-XXXX ... SUCCESS
  - Verifying stock compensation in Product Service ... Stock RESTORED
  - Verifying wallet refund in Wallet Service ... Balance REFUNDED accurately
  - Verifying order status transitioned to CANCELLED / REFUNDED ... VERIFIED

[PHASE 8] Cash On Delivery (COD) Checkout & Anti-IDOR Security Checks
  - Re-adding item to cart ... SUCCESS
  - Executing Checkout with COD payment method ... SUCCESS (Status: PLACED, COD_PENDING)
  - Testing Anti-IDOR customer isolation ... Accessing unauthorized order returns 404 NOT FOUND
  - Testing Merchant role access control ... Accessing customer wallet returns 403 FORBIDDEN

================================================================================
  VERIFICATION RESULT: 8 / 8 PHASES PASSED (0 FAILURES)
================================================================================
```

---

## 7. Postman Automated API Collection

The platform includes a comprehensive Postman test suite:
- **Collection**: `postman/collections/E-ShoppingZone-Postman-Collection.json`
- **Environment**: `postman/environments/Local-Environment.json`

### 7.1 Running via Newman CLI
```powershell
newman run postman\collections\E-ShoppingZone-Postman-Collection.json -e postman\environments\Local-Environment.json
```
*Result*: 37 API assertions verify status codes, response times, JWT token extractions, and payload schemas.

