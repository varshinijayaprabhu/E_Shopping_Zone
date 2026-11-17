# Architectural Decision Records (ADRs)

## ADR-001: Strict Baseline Preservation of Java 8 and Spring Boot 2.7.x
- **Status**: Accepted
- **Context**: The Capgemini case study explicitly specifies Java 8 and Spring Boot microservices. Upgrading to Java 17 or Spring Boot 3 would break compatibility with specified baseline constraints and introduce unnecessary framework migration overhead.
- **Decision**: Lock framework version to **Spring Boot 2.7.18** and **Spring Cloud 2021.0.9**. Use standard Java 8 language features and `javax.*` packages.
- **Consequence**: Full adherence to original project requirements; guaranteed compatibility with Springdoc OpenAPI 1.8.x and Netflix Eureka.

---

## ADR-002: Monorepo Repository Structure
- **Status**: Accepted
- **Context**: A distributed system with 7 microservices, a React frontend, documentation, SQL migrations, and Postman collections requires atomic tracking and unified developer onboarding.
- **Decision**: Organize the entire solution within a single repository structure (`backend/`, `frontend/`, `database/`, `docs/`, `postman/`).
- **Consequence**: Single build and commit history; eliminates version drift across microservices and frontend contracts.

---

## ADR-003: Database-Per-Service with Logical MySQL Separation
- **Status**: Accepted
- **Context**: Microservices principles mandate autonomy and independent evolvability of domain data models.
- **Decision**: Allocate five dedicated databases (`profile_db`, `product_db`, `cart_db`, `order_db`, `wallet_db`) hosted on a single local MySQL server.
- **Consequence**: Eliminates foreign-key coupling across microservices. Inter-service data requirements are satisfied through REST endpoints.

---

## ADR-004: Synchronous Inter-Service Communication via RestTemplate
- **Status**: Accepted
- **Context**: The specification mandates RestTemplate without message brokers.
- **Decision**: Use `@LoadBalanced RestTemplate` resolved dynamically by Eureka service names. Do not introduce Kafka, RabbitMQ, or gRPC.
- **Consequence**: Lower operational complexity, straightforward debugging, zero message broker infrastructure needed.

---

## ADR-005: Dual-Token Architecture with Refresh Token Rotation
- **Status**: Accepted
- **Context**: Storing JWT access tokens in browser `localStorage` exposes users to XSS token exfiltration.
- **Decision**: 
  1. Short-lived (15 min) JWT access token held in React in-memory state.
  2. Long-lived (7 day) opaque refresh token stored in MySQL as a SHA-256 hash.
  3. Refresh token delivered strictly via an `HttpOnly`, `Secure`, `SameSite=Lax` cookie.
  4. Each refresh request invalidates the old refresh token, generates a new one, and detects token replay attacks.
- **Consequence**: High security posture against XSS and token replay, seamless background session renewal.

---

## ADR-006: Distributed Checkout Coordination via Compensating Saga
- **Status**: Accepted
- **Context**: Placing an order requires coordination across Cart, Product, Wallet, and Order services without distributed 2PC locking.
- **Decision**: Implement an orchestrated Saga in `Order Service` with explicit compensating actions (`releaseStock`, `refundPayment`) on downstream failures.
- **Consequence**: Eventual consistency without blocking database distributed transactions.

