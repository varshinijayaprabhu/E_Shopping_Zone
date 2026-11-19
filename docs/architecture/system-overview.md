# E-ShoppingZone System Overview

## 1. Executive Summary

**E-ShoppingZone** is an enterprise-grade e-commerce microservices platform built upon the original Capgemini case study specification. The platform delivers a scalable, highly available, and secure distributed shopping architecture designed for two primary stakeholder roles: **Customers** and **Merchants**.

The platform is engineered around strict **Java 8**, **Spring Boot 2.7.x**, and **Spring Cloud 2021.0.x** baseline standards, incorporating **Spring Security 5.7.x**, **Spring Cloud Netflix Eureka** for service discovery, **Spring Cloud Gateway** for reactive perimeter routing, and a modern **React + TypeScript** single-page application (SPA).

---

## 2. Core Business Objectives

1. **Seamless Customer Shopping Journey**: Unauthenticated product browsing and searching, secure customer onboarding, automated cart management, synchronized checkout, dual payment modes (Wallet Balance and Cash on Delivery), order status tracking, and profile administration.
2. **Empowered Merchant Operations**: Secure merchant portal for product lifecycle management (creation, inventory adjustment, pricing, categorisation) and fulfillment visibility.
3. **Resilient Distributed Transactions**: Robust coordination between Cart, Product inventory, Order, and Wallet balances using compensating actions (Saga orchestration pattern) to avoid inconsistent distributed state.
4. **Defense-in-Depth Security**: Perimeter JWT access token verification at API Gateway coupled with downstream signature/role enforcement, cryptographically strong opaque refresh tokens with rotation and replay detection in HttpOnly cookies, and GitHub OAuth 2.0 social login.

---

## 3. High-Level Architecture Diagram

```mermaid
graph TD
    subgraph ClientLayer ["Client Layer"]
        SPA["React 18 + TypeScript SPA<br/>(Port: 5173)"]
        PM["Postman API Client"]
    end

    subgraph PerimeterLayer ["Edge & Discovery Layer"]
        EUREKA["Eureka Discovery Server<br/>(Port: 8761)"]
        GATEWAY["Spring Cloud Gateway<br/>(Port: 8080)<br/>• CORS & Rate Limiting<br/>• Perimeter JWT Validation<br/>• Request Routing"]
    end

    subgraph ServiceLayer ["Core Microservices Layer (Java 8 / Spring Boot 2.7)"]
        PROFILE["Profile Service<br/>(Port: 8081)<br/>• Auth & JWT / Refresh<br/>• GitHub OAuth 2.0<br/>• User Profiles"]
        PRODUCT["Product Service<br/>(Port: 8082)<br/>• Product Catalog<br/>• Categories & Search<br/>• Inventory Control"]
        CART["Cart Service<br/>(Port: 8083)<br/>• Cart Aggregation<br/>• Live Price Verification<br/>• Quantity Updates"]
        ORDER["Order Service<br/>(Port: 8084)<br/>• Checkout Orchestration<br/>• Order Lifecycle<br/>• Compensating Sagas"]
        WALLET["Wallet Service<br/>(Port: 8085)<br/>• Digital Wallet Ledger<br/>• Deposits & Withdrawals<br/>• Idempotent Deductions"]
        ADMIN["Admin Service<br/>(Port: 8086)<br/>• Platform Governance<br/>• Dispute SLA & Escalation<br/>• Immutable Audit Logging"]
    end

    subgraph DatabaseLayer ["Database Per Service (MySQL 8.0)"]
        DB_PROFILE[(profile_db)]
        DB_PRODUCT[(product_db)]
        DB_CART[(cart_db)]
        DB_ORDER[(order_db)]
        DB_WALLET[(wallet_db)]
        DB_ADMIN[(admin_db)]
    end

    SPA -->|HTTP REST / Cookies| GATEWAY
    PM -->|HTTP REST / Bearer| GATEWAY

    GATEWAY -.->|Discovers Instances| EUREKA
    PROFILE -.->|Registers| EUREKA
    PRODUCT -.->|Registers| EUREKA
    CART -.->|Registers| EUREKA
    ORDER -.->|Registers| EUREKA
    WALLET -.->|Registers| EUREKA
    ADMIN -.->|Registers| EUREKA

    GATEWAY -->|/api/auth/**, /api/profile/**| PROFILE
    GATEWAY -->|/api/products/**, /api/categories/**| PRODUCT
    GATEWAY -->|/api/cart/**| CART
    GATEWAY -->|/api/orders/**| ORDER
    GATEWAY -->|/api/wallet/**| WALLET
    GATEWAY -->|/api/admin/**, /api/disputes/**| ADMIN

    CART -->|RestTemplate /api/products/{id}| PRODUCT
    ORDER -->|RestTemplate /api/cart| CART
    ORDER -->|RestTemplate /api/products/reserve| PRODUCT
    ORDER -->|RestTemplate /api/wallet/payment| WALLET
    ADMIN -->|RestTemplate /api/profile/internal/**| PROFILE
    ADMIN -->|RestTemplate /api/orders/internal/**| ORDER

    PROFILE --> DB_PROFILE
    PRODUCT --> DB_PRODUCT
    CART --> DB_CART
    ORDER --> DB_ORDER
    WALLET --> DB_WALLET
    ADMIN --> DB_ADMIN
```

---

## 4. Key Architectural Characteristics

| Characteristic | Architectural Approach | Rationale |
| :--- | :--- | :--- |
| **Monorepo Structure** | Backend services, React UI, SQL migrations, Postman collections, and docs in one repository | Single source of truth, simplified continuous integration, atomic versioning |
| **Java 8 Runtime** | Spring Boot 2.7.18, Spring Cloud 2021.0.9 | Strict adherence to original specification; avoids Java 17/Jakarta EE 9+ breaking changes |
| **Database per Service** | 6 discrete MySQL databases (`profile_db`, `product_db`, `cart_db`, `order_db`, `wallet_db`, `admin_db`) | Loose coupling; zero cross-service table joins; independent data evolution |
| **Inter-Service Communication** | Synchronous HTTP via `@LoadBalanced RestTemplate` | Native to specification; low operational overhead without broker infrastructure |
| **Perimeter Security** | Spring Cloud Gateway + Reactive JWT Filter | Rejects unauthorized traffic at the border before it reaches internal service instances |
| **Token Lifecycle** | Short-lived JWT (15 min) + Opaque Hashed Refresh Token (7 days) in HttpOnly Cookie | Eliminates XSS token theft via localStorage; automated token rotation with reuse detection |
| **Stateful Transactions** | Saga Compensating Transactions over REST | Guarantees eventual consistency during multi-service checkout workflows without 2PC |

