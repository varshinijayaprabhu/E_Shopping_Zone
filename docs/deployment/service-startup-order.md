# Service Startup Order & Dependency Sequencing

## 1. Prescribed Startup Sequence

Because downstream services register with Eureka and the API Gateway queries Eureka for dynamic routes, services must be launched in the exact sequence outlined below:

```mermaid
flowchart TD
    DB[("Step 0: MySQL Server (Port 3306)<br/>Create schemas & apply seeds")] --> S1
    S1["Step 1: Eureka Discovery Server (Port 8761)<br/>Wait 10-15s until registry is active"] --> S2
    S2["Step 2: API Gateway (Port 8080)<br/>Edge perimeter routing & reactive security"] --> S3
    S3["Step 3: Core Domain Services<br/>Profile Service (Port 8081)<br/>Product Service (Port 8082)<br/>Cart Service (Port 8083)<br/>Wallet Service (Port 8085)<br/>Order Service (Port 8084)"] --> S4
    S4["Step 4: React UI Single Page App (Port 5173)<br/>Vite dev server"]
```

---

## 2. Health Verification Checklist

1. **Verify MySQL**: Ensure port `3306` is open and databases `profile_db`, `product_db`, `cart_db`, `order_db`, and `wallet_db` exist.
2. **Verify Eureka Dashboard**: Open `http://localhost:8761`. Verify that `API-GATEWAY`, `PROFILE-SERVICE`, `PRODUCT-SERVICE`, `CART-SERVICE`, `ORDER-SERVICE`, and `WALLET-SERVICE` appear with status `UP (1)`.
3. **Verify Gateway Routing**: Invoke `http://localhost:8080/api/products` and confirm public product listing is returned.
4. **Verify React UI**: Open `http://localhost:5173` and confirm the homepage renders catalog products cleanly.

