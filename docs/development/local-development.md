# Local Development Workflow

## 1. Local Development Topology

During active development on a single machine:
- All 7 backend Spring Boot processes run as independent JVM processes.
- Eureka Discovery Server tracks localhost registrations.
- React Vite dev server runs with Hot Module Replacement (HMR).

---

## 2. Port Allocation Reference

```
+----------------------------------------------------------------+
| E-ShoppingZone Local Port Map                                  |
+----------------------+-------+---------------------------------+
| Service              | Port  | Role / Endpoint                 |
+----------------------+-------+---------------------------------+
| Discovery Server     | 8761  | Eureka Dashboard / Registry     |
| API Gateway          | 8080  | Single Client Ingress / Routing |
| Profile Service      | 8081  | Auth, Profiles, GitHub OAuth    |
| Product Service      | 8082  | Catalog, Categories, Inventory  |
| Cart Service         | 8083  | Shopping Cart Operations        |
| Order Service        | 8084  | Checkout & Saga Orchestration   |
| Wallet Service       | 8085  | Balance Ledger & Deductions     |
| React Frontend (UI)  | 5173  | Customer & Merchant Web Portal  |
| MySQL Database       | 3306  | Relational Persistence Engine   |
+----------------------+-------+---------------------------------+
```

---

## 3. Running Services Individually from CLI

Each microservice can be started independently via Maven:
```bash
# Terminal 1: Discovery Server
cd backend/discovery-server
mvn spring-boot:run

# Terminal 2: API Gateway
cd backend/api-gateway
mvn spring-boot:run

# Terminal 3: Profile Service
cd backend/profile-service
mvn spring-boot:run

# Terminal 4: Product Service
cd backend/product-service
mvn spring-boot:run

# Terminal 5: Cart Service
cd backend/cart-service
mvn spring-boot:run

# Terminal 6: Wallet Service
cd backend/wallet-service
mvn spring-boot:run

# Terminal 7: Order Service
cd backend/order-service
mvn spring-boot:run

# Terminal 8: React UI
cd frontend/e-shopping-ui
npm run dev
```

