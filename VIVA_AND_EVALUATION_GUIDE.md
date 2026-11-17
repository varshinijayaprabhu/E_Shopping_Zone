# E-ShoppingZone: Master Architecture, Evaluation & Viva Voce Guide

This document is the definitive technical reference for the **E-ShoppingZone Spring Boot Microservices Platform**. It comprehensively covers the architecture, security models, distributed transactions, design patterns, and one-on-one evaluation parameters.

---

## Table of Contents
1. [Case Study Requirements & Evaluation Parameters](#1-case-study-requirements--evaluation-parameters)
2. [API Gateway & Service Discovery Deep Dive](#2-api-gateway--service-discovery-deep-dive)
3. [Service-by-Service Architecture Breakdown](#3-service-by-service-architecture-breakdown)
4. [Enterprise Concepts Applied (Bonus Features)](#4-enterprise-concepts-applied-bonus-features)
5. [Zero-Trust Security & JWT Evaluation](#5-zero-trust-security--jwt-evaluation)
6. [Data Integrity & Database Craftsmanship](#6-data-integrity--database-craftsmanship)
7. [Clean Architecture & Coding Standards](#7-clean-architecture--coding-standards)
8. [End-to-End Transaction Flow (Saga Checkout)](#8-end-to-end-transaction-flow-saga-checkout)
9. [Master Viva Voce Q&A Cheat Sheet](#9-master-viva-voce-qa-cheat-sheet)

---

## 1. Case Study Requirements & Evaluation Parameters

Based on the **Capgemini Java Full Stack (JFS) Case Study Rubric**, the project is evaluated across 10 distinct parameters:

| # | Evaluation Parameter | Project Compliance Status | Implementation Details |
|---|---|:---:|---|
| **1** | **Requirement Understanding** | ✅ **100% Done** | Full multi-vendor e-commerce domain: Customer and Merchant separation, Catalog, Cart, Checkout, Digital Wallet, Order Tracking. |
| **2** | **Design Diagrams** | ⚠️ **In Progress** | Database ER Diagram documented in `docs/database/entity-relationships.md`. Use Case & Class Diagrams to be finalized. |
| **3** | **Working Demonstration / E2E Demo** | ✅ **100% Done** | Spring Boot 2.7.x + Spring Cloud 2021.x + Spring Data JPA + MySQL 8 + React 18 frontend with end-to-end checkout. |
| **4** | **Ability to Apply Concepts (Bonus)** | ✅ **100% Done** | Saga Orchestration with rollbacks, Refresh Token Rotation, Pessimistic Row Locking, Anti-IDOR checks, GitHub OAuth2 SSO. |
| **5** | **Coding Standards & Clean Techniques** | ✅ **100% Done** | Standard package structure, zero entity leakage (DTO layer), interface-driven services, PascalCase/camelCase conventions. |
| **6** | **AOP & Loggers (SLF4J + Logfile)** | ✅ **100% Done** | Spring AOP Logging Aspect (`@Aspect`, `@Around`) intercepting all Controllers/Services; automated rotating log files (`logs/<service>.log`). |
| **7** | **JUnit + Mockito Testing** | ✅ **Passed** | Unit and integration test suites present for Profile, Product, Cart, Order, and Wallet services; 20 frontend Vitest suites passing. |
| **8** | **Exception Handling & Data Validation** | ✅ **100% Done** | Global `@RestControllerAdvice` exception handlers in every service; `@Valid` Bean Validation on all request DTOs. |
| **9** | **DevOps Implementation** | ✅ **100% Done** | Maven multi-module parent POM configured with JaCoCo test coverage and SonarQube static code quality analysis dashboard. |
| **10** | **Viva Voce (Technical Knowledge)** | ✅ **Prepared** | Complete conceptual and architectural knowledge documented in Section 9 of this guide. |

---

## 2. API Gateway & Service Discovery Deep Dive

The **API Gateway** (`api-gateway`) is the single ingress edge service for the entire application cluster, running on port **`8080`**.

```
 Browser / React Client (:5173)
               │
               ▼  All requests hit Port 8080
┌─────────────────────────────────────────────────────────────┐
│                    API GATEWAY (:8080)                      │
│                                                             │
│  1. Reactive CORS WebFilter (Allows :5173 + Credentials)    │
│  2. CorrelationIdGlobalFilter (Injects X-Correlation-Id)    │
│  3. Route Predicates (/api/products/**, /api/orders/**)     │
│  4. Dynamic Eureka Resolution & Round-Robin Load Balancing  │
└──────────────────────────────┬──────────────────────────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼ (lb://)               ▼ (lb://)               ▼ (lb://)
Profile Service         Product Service          Order Service
  (Port 8081)             (Port 8082)             (Port 8085)
```

### Why Spring Cloud Gateway instead of Netflix Zuul?
* **Netflix Zuul 1.x**: Blocking, servlet-based architecture running on Tomcat. One thread per request; easily exhausted during connection spikes.
* **Spring Cloud Gateway**: Built on **Spring 5, Project Reactor, and Netty**. Non-blocking asynchronous event loop; handles tens of thousands of concurrent connections with minimal CPU and memory footprints.

### All Configured Routes (`application.yml`):
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: profile-service-auth
          uri: lb://PROFILE-SERVICE
          predicates:
            - Path=/api/auth/**
        - id: profile-service-profile
          uri: lb://PROFILE-SERVICE
          predicates:
            - Path=/api/profile/**
        - id: profile-service-oauth2
          uri: lb://PROFILE-SERVICE
          predicates:
            - Path=/oauth2/**, /login/oauth2/**
          filters:
            - PreserveHostHeader
        - id: product-service-products
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
        - id: product-service-categories
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/categories/**
        - id: cart-service
          uri: lb://CART-SERVICE
          predicates:
            - Path=/api/cart/**
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
        - id: wallet-service
          uri: lb://WALLET-SERVICE
          predicates:
            - Path=/api/wallet/**
```

### Key Gateway Code Components:
1. **`CorrelationIdGlobalFilter.java`**:
   * Inspects every request for `X-Correlation-Id`. If missing, generates `UUID.randomUUID().toString()`.
   * Passes the ID downstream to microservices and attaches it to the HTTP response header.
   * Enables end-to-end distributed tracing across independent microservice logs.
2. **`CorsConfiguration.java`**:
   * Implements a reactive `CorsWebFilter` bean permitting `http://localhost:5173`.
   * Sets `corsConfig.setAllowCredentials(true)` so the browser securely sends and receives the `HttpOnly` refresh token cookie.
   * Exposes `Set-Cookie` and `X-Correlation-Id`.
3. **`PreserveHostHeader` Filter**:
   * Prevents host header rewriting so third-party OAuth2 providers (GitHub) redirect back to `localhost:8080` instead of internal container IPs.

---

## 3. Service-by-Service Architecture Breakdown

| Microservice | Port | Database | Primary Responsibilities |
|---|:---:|---|---|
| **`discovery-server`** | `8761` | None | Netflix Eureka registry; maintains microservice instances and heartbeats. |
| **`api-gateway`** | `8080` | None | Ingress router, Netty reactive engine, CORS gatekeeper, correlation tracing. |
| **`profile-service`** | `8081` | `eshopping_profile` | Customer & Merchant registration, login, BCrypt hashing, JWT issuance, Refresh Token Rotation, GitHub OAuth2, customer profiles. |
| **`product-service`** | `8082` | `eshopping_product` | Catalog browsing, pagination, categories, merchant inventory CRUD, Anti-IDOR ownership checks, soft-delete, Saga reserve/release endpoints. |
| **`cart-service`** | `8083` | `eshopping_cart` | Persistent cart items, quantity modification, live price sync with `product-service`, auto-clear on checkout. |
| **`wallet-service`** | `8084` | `eshopping_wallet` | Customer digital balance, deposit funds, checkout payment under pessimistic row lock, Saga compensating refunds, immutable transaction ledger. |
| **`order-service`** | `8085` | `eshopping_order` | Order history, merchant sales items, historical price/title snapshots, **Checkout Saga Orchestrator**. |

---

## 4. Enterprise Concepts Applied (Bonus Features)

### A. The Saga Pattern with Compensating Rollbacks
* **Problem**: In microservices, `order-service`, `product-service`, and `wallet-service` have separate databases. Traditional `@Transactional` across three databases does not work without heavy, locking 2-Phase Commit (2PC).
* **Solution**: `CheckoutSagaOrchestrator.java` acts as the conductor:
  1. **Step 1**: Calls `product-service` (`POST /api/products/reserve`) to hold inventory.
  2. **Step 2**: Calls `wallet-service` (`POST /api/wallet/pay`) to deduct money under a pessimistic lock.
  3. **Step 3**: Calls `cart-service` (`DELETE /api/cart`) to clear the basket and marks order `CONFIRMED`.
* **Compensating Rollback**:
  If Step 2 fails (e.g., insufficient wallet balance), the Saga immediately triggers compensation:
  * Calls `POST /api/products/release` to put held stock back into inventory.
  * Leaves the customer's cart intact.
  * Transitions order state to `FAILED`.

### B. Refresh Token Rotation (RTR) & Replay Attack Defense
* **Problem**: Access tokens stored in `localStorage` can be stolen via Cross-Site Scripting (XSS). Long-lived static refresh tokens grant permanent attacker access if leaked.
* **Solution**:
  * Access tokens expire in **15 minutes** (in-memory).
  * Refresh tokens are stored in an **`HttpOnly; SameSite=Lax` cookie** (JavaScript cannot read it).
  * **Both Tokens Rotate**: Every time `POST /api/auth/refresh` is called:
    1. The old refresh token is marked `revoked = true` in MySQL.
    2. A brand-new refresh token is generated and sent in a new cookie.
    3. A brand-new access token is generated and returned in JSON.
  * **Theft Detection**: If an attacker attempts to reuse an old revoked refresh token, the server detects the replay attack, marks the user compromised, and **revokes all active sessions immediately**.

### C. Pessimistic Row Locking (`PESSIMISTIC_WRITE`)
* **Problem**: A user double-clicks "Pay" or opens two checkout tabs simultaneously. Both threads read `balance = 1000` and deduct ₹1,000, creating negative balances and double spending (Race Condition).
* **Solution** (`WalletRepository.java`):
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
  Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
  ```
  Generates a MySQL `SELECT ... FOR UPDATE` query. MySQL locks that exact customer row. The second transaction must wait in line until the first transaction commits or rolls back.

### D. Anti-IDOR Defense (Insecure Direct Object Reference)
* **Problem**: Merchant A calls `DELETE /api/products/12` to sabotage Merchant B's products.
* **Solution**: The backend extracts the authenticated merchant ID directly from verified JWT claims:
  ```java
  if (!product.getMerchantId().equals(authenticatedMerchantId)) {
      throw new UnauthorizedMerchantException("Merchant ID " + authenticatedMerchantId 
          + " is not authorized to modify product ID " + id);
  }
  ```
  Merchants are strictly isolated to their own product inventory and order line items.

---

## 5. Zero-Trust Security & JWT Evaluation

Every microservice implements its own `JwtAuthenticationFilter` and `SecurityConfig`:

```
 Client Request (with Bearer JWT)
               │
               ▼
       [ API Gateway :8080 ]
               │ (Routes to microservice)
               ▼
┌──────────────────────────────────────────────┐
│  Target Microservice (e.g., Order Service)   │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │       JwtAuthenticationFilter          │  │
│  │  1. Extract Bearer token from header   │  │
│  │  2. Validate HMAC-256 signature        │  │
│  │  3. Check expiration (exp)             │  │
│  │  4. Extract userId & roles             │  │
│  │  5. Populate SecurityContextHolder     │  │
│  └───────────────────┬────────────────────┘  │
│                      ▼                       │
│  ┌────────────────────────────────────────┐  │
│  │      Spring Security Authorization     │  │
│  │  • Has valid token? (authenticated)    │  │
│  │  • Has required role? (CUSTOMER /      │  │
│  │    MERCHANT / ADMIN)                   │  │
│  └───────────────────┬────────────────────┘  │
│                      ▼                       │
│              [ Controller API ]              │
└──────────────────────────────────────────────┘
```

### Public vs. Protected Endpoints:
* **Public Endpoints (`.permitAll()`)**:
  * Registration & Login: `POST /api/auth/register`, `POST /api/auth/register-merchant`, `POST /api/auth/login`.
  * Token Refresh: `POST /api/auth/refresh`.
  * Catalog Browsing: `GET /api/products/**`, `GET /api/categories/**`.
  * Documentation: `/swagger-ui/**`, `/v3/api-docs/**`.
* **Protected Endpoints (`.authenticated()`)**:
  * Cart: `/api/cart/**` (`ROLE_CUSTOMER`).
  * Orders: `/api/orders/**` (`ROLE_CUSTOMER` or `ROLE_MERCHANT`).
  * Wallet: `/api/wallet/**` (`ROLE_CUSTOMER`).
  * Merchant Management: `POST/PUT/DELETE /api/products/**` (`ROLE_MERCHANT`).

---

## 6. Data Integrity & Database Craftsmanship

1. **Historical Snapshots on Order Items**:
   When an order is created, `OrderItem.java` clones `productNameSnapshot`, `unitPriceSnapshot`, and `merchantId`. Even if a seller updates the product name or price later, past customer receipts and invoices remain historically accurate.
2. **Soft-Delete Lifecycle**:
   Deleting a product sets `active = false` instead of executing a SQL `DELETE`. This prevents foreign key integrity constraint violations on past orders.
3. **Immutable Double-Entry Financial Ledger**:
   `WalletTransaction.java` stores permanent rows for every `DEPOSIT`, `PURCHASE`, and `REFUND`. Current balance can always be mathematically audited against transaction history.
4. **Database-per-Service**:
   Each service connects to its own independent MySQL schema (`eshopping_profile`, `eshopping_product`, `eshopping_cart`, `eshopping_order`, `eshopping_wallet`).

---

## 7. Clean Architecture & Coding Standards

### Package Structure:
```
com.eshopping.<service>/
├── config/         # SecurityConfig, OpenApiConfig, CorsConfig
├── controller/     # REST Endpoints only (@Valid, HTTP codes, Swagger)
├── service/        # Business logic INTERFACES
│   └── impl/       # Implementation classes
├── repository/     # Spring Data JPA interfaces
├── entity/         # Database schema entities (@Entity, @Table)
├── dto/            # Data Transfer Objects (Request/Response)
└── exception/      # Domain exceptions & @RestControllerAdvice
```

### Clean Coding Principles:
* **Zero Entity Leakage**: JPA entities (`User`, `Product`, `Order`) are never returned directly from controllers. DTOs are always used to prevent infinite JSON recursion and data leakage.
* **Interface-Driven Services**: Controllers depend on `ProductService` interfaces, allowing effortless unit testing and Mockito mocking.
* **Standard Naming Conventions**:
  * PascalCase for classes (`ProductResponseDTO`, `GlobalExceptionHandler`).
  * camelCase for methods and variables (`findByMerchantId`, `accessToken`).
  * UPPER_SNAKE_CASE for constants (`REFRESH_COOKIE_NAME`).

---

## 8. End-to-End Transaction Flow (Saga Checkout)

```
[Customer clicks "Place Order" on Frontend]
                       │
                       ▼ POST /api/orders/checkout (with JWT)
              ┌─────────────────┐
              │   API GATEWAY   │  (Port 8080)
              └────────┬────────┘
                       │ Attaches X-Correlation-Id, routes to ORDER-SERVICE
                       ▼
            ┌─────────────────────┐
            │    ORDER-SERVICE    │  (Saga Orchestrator)
            └──────────┬──────────┘
                       │
   ┌───────────────────┼───────────────────┐
   │ 1. Reserve Stock  │ 2. Deduct Funds   │ 3. Clear Cart
   ▼                   ▼                   ▼
PRODUCT-SERVICE     WALLET-SERVICE       CART-SERVICE
- Holds inventory   - Pessimistic Row   - Clears cart
- If stock low:       Lock (FOR UPDATE) - Order marked
  ABORT SAGA!       - If balance low:     CONFIRMED!
                      COMPENSATE!
                      (Release stock)
```

---

## 9. Master Viva Voce Q&A Cheat Sheet

### Q1: What is the difference between Monolithic and Microservices architectures?
> **Answer**: *"A monolith packages all business modules into a single artifact sharing one database. While simple to deploy initially, it suffers from single points of failure, scaling bottlenecks, and tight database coupling. Microservices decompose the system into small, independently deployable services around business capabilities, each with its own database, communicating via lightweight REST APIs. This allows independent scaling, fault isolation, and technology flexibility."*

### Q2: What is the role of Eureka Server in your project?
> **Answer**: *"Eureka Server is our service discovery registry running on port 8761. In a dynamic microservices environment, IP addresses and ports change dynamically. Microservices register their instance details with Eureka and send heartbeats every 30 seconds. The API Gateway queries Eureka to dynamically locate healthy instances using the `lb://` load-balancing protocol without hardcoded IP addresses."*

### Q3: Why did you choose Spring Cloud Gateway over Netflix Zuul?
> **Answer**: *"Netflix Zuul 1.x uses blocking, servlet-based I/O on Apache Tomcat, allocating one thread per request. Spring Cloud Gateway is built on Spring 5, Spring WebFlux, and Project Reactor running on embedded Netty. It uses non-blocking asynchronous event loops, allowing a small pool of threads to handle thousands of concurrent requests with significantly lower memory usage."*

### Q4: How does JWT Authentication work across your microservices?
> **Answer**: *"We follow a Zero-Trust Security model. When a user logs in via `profile-service`, they receive a short-lived 15-minute JWT access token and a 7-day refresh token in an HttpOnly cookie. For subsequent requests, the frontend passes the Bearer JWT. Every functional microservice has its own `JwtAuthenticationFilter` in its Spring Security filter chain to independently verify the cryptographic HMAC-SHA256 signature, validate expiration, extract the user's ID and roles, and enforce Role-Based Access Control."*

### Q5: What is Refresh Token Rotation (RTR) and why do both tokens rotate?
> **Answer**: *"In Refresh Token Rotation, every time a client requests a new access token, the server revokes the old refresh token in MySQL and issues both a new access token and a brand-new refresh token. If an attacker intercepts and reuses an old refresh token, the server detects that the token was already revoked, recognizes a replay attack, and automatically invalidates all active sessions for that user."*

### Q6: How does the Saga Pattern solve distributed transactions in your system?
> **Answer**: *"Because our microservices have separate databases, we cannot use `@Transactional` across Order, Inventory, and Wallet. We implemented an Orchestration Saga in `order-service`. The orchestrator executes local transactions step-by-step: first reserving stock in Product Service, then deducting balance in Wallet Service, and finally clearing the cart. If any step fails—such as insufficient funds—the orchestrator invokes compensating transactions to undo previous steps, such as releasing the reserved stock back to inventory."*

### Q7: How do you prevent race conditions and double spending in the Digital Wallet?
> **Answer**: *"We use Pessimistic Row Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) in `WalletRepository`. This executes a MySQL `SELECT ... FOR UPDATE` on the customer's wallet row. If two payment requests arrive at the same millisecond, MySQL forces the second transaction to wait until the first payment transaction commits or rolls back, completely eliminating race conditions."*

### Q8: What is Anti-IDOR defense and where is it implemented?
> **Answer**: *"IDOR stands for Insecure Direct Object Reference. In our multi-tenant merchant portal, we never trust product ownership based on client request parameters. When a merchant updates or deletes a product, `ProductServiceImpl` extracts the merchant ID directly from the cryptographic JWT claims and verifies that `product.getMerchantId().equals(authenticatedMerchantId)`. Unauthorized attempts are rejected with a 403 Forbidden error."*

### Q9: Why is DTO abstraction strictly enforced in your codebase?
> **Answer**: *"We never expose JPA entities directly from REST controllers. Entities represent internal database schemas, which may include sensitive data like password hashes or cause infinite recursion during JSON serialization with bidirectional relationships. DTOs decouple internal database schemas from external API contracts, allowing tables to change without breaking frontend clients."*

### Q10: How do you track a single user request across multiple microservice logs?
> **Answer**: *"Our API Gateway uses a `CorrelationIdGlobalFilter` that inspects incoming requests for an `X-Correlation-Id` header. If missing, it generates a unique UUID and propagates it to all downstream microservices and the response headers. All microservices include this correlation ID in their SLF4J logs, allowing developers to trace the entire lifecycle of a distributed transaction using a single ID."*

### Q11: Why did you disable CSRF (`.csrf().disable()`) in Spring Security, and why does SonarQube flag it as a Security Hotspot?
> **Answer**: *"CSRF (Cross-Site Request Forgery) attacks exploit stateful browser session cookies (`JSESSIONID`) that browsers automatically attach to cross-origin requests. Our microservices architecture is completely stateless (`SessionCreationPolicy.STATELESS`) and uses JWT authentication passed in custom `Authorization: Bearer <token>` headers. Because browsers never automatically attach custom authorization headers to cross-site requests, CSRF attacks are fundamentally impossible on stateless JWT APIs. SonarQube flags `.csrf().disable()` as a 'Security Hotspot' for human verification; in SonarQube, this is reviewed and marked as 'Reviewed as Safe' because the API is stateless."*

### Q12: How do you ensure code quality, test coverage, and security compliance across your microservices?
> **Answer**: *"We integrated SonarQube static code analysis and JaCoCo code coverage into our Maven multi-module build pipeline. Across all 7 microservices, 120 JUnit 5 and Mockito tests run during the build, and JaCoCo measures test coverage. SonarQube enforces Quality Gates covering bugs, maintainability ratings (Grade A), technical debt, code smells, and security hotspots (such as secrets detection and CSRF validation), ensuring enterprise-grade software standards."*


