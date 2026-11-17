# E-ShoppingZone — Master Services & Platform Startup Guide

[![Java 8](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2021.0.9-blue.svg)](https://spring.io/projects/spring-cloud)
[![React](https://img.shields.io/badge/React-18.3.1-cyan.svg)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-blue.svg)](https://www.mysql.com/)

This guide provides exact, copy-paste instructions to launch the entire **E-ShoppingZone** ecosystem on a local Windows machine — including the **MySQL databases, 7 backend microservices, Swagger documentation, and React web application**.

---

## 1. Quick Reference: Access URLs & Swagger Matrix

Once started, every application, dashboard, and interactive Swagger UI is available at these endpoints:

| Component | Port | Browser URL | Description |
| :--- | :---: | :--- | :--- |
| 🛍️ **React Web Storefront** | `5173` | [http://localhost:5173](http://localhost:5173) | Main user interface (Customers & Merchants) |
| 🌐 **Eureka Service Registry** | `8761` | [http://localhost:8761](http://localhost:8761) | Dashboard showing all 6 registered microservices |
| 🚪 **Spring Cloud API Gateway** | `8080` | [http://localhost:8080](http://localhost:8080) | Edge entry point routing all API traffic |
| 📄 **Profile Service Swagger** | `8081` | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) | Auth, Profiles, Token Rotation APIs |
| 📄 **Product Service Swagger** | `8082` | [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) | Catalog, Categories, Stock Reservation APIs |
| 📄 **Cart Service Swagger** | `8083` | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) | Customer Cart, Line Items, Live Price APIs |
| 📄 **Order Service Swagger** | `8084` | [http://localhost:8084/swagger-ui/index.html](http://localhost:8084/swagger-ui/index.html) | Checkout Saga, History, Snapshots, COD APIs |
| 📄 **Wallet Service Swagger** | `8085` | [http://localhost:8085/swagger-ui/index.html](http://localhost:8085/swagger-ui/index.html) | Digital Ledger, Deductions, Refunds APIs |

---

## 2. Step-by-Step Instructions

### Step 1: Ensure MySQL is Running & Initialized
The platform uses **MySQL 8.x** running locally on port `3306`.

1. Ensure your local MySQL service is running:
   ```powershell
   Get-Service -Name MySQL* | Start-Service
   ```

2. Execute the master database provisioning script (run once):
   ```powershell
   # From project root: c:\Users\vivek\Desktop\E Shopping Zone
   Get-Content scripts\init-databases.sql | mysql -u root -p
   ```
   *(Enter your MySQL root password when prompted)*

   This automatically creates and initializes all 5 schemas:
   - `profile_db`: Users, roles, refresh tokens.
   - `product_db`: 4 categories, 12 products, inventory stock.
   - `cart_db`: Customer carts and line items.
   - `wallet_db`: Digital wallets and immutable ledger with seed balances.
   - `order_db`: Orders, snapshots, payment records.

---

### Step 2: Start All Services with One Command (Recommended)

Run the master automated startup script in PowerShell from the project root:

```powershell
.\start-all-services.ps1
```

> [!TIP]
> **Frontend Development Mode**:  
> To launch the React frontend with Vite live hot-reloading (HMR), pass `-FrontendDevMode`:
> ```powershell
> .\start-all-services.ps1 -FrontendDevMode
> ```

#### What this script does automatically:
1. Verifies that MySQL is reachable on port `3306`.
2. Checks all ports to avoid duplicate instances.
3. Launches services in strict dependency sequence:
   - **Discovery Server** (`:8761`) — waits for health check `UP`.
   - **Profile Service** (`:8081`) — waits for health check `UP`.
   - **Product Service** (`:8082`) — waits for health check `UP`.
   - **Cart Service** (`:8083`) — waits for health check `UP`.
   - **Wallet Service** (`:8085`) — waits for health check `UP`.
   - **Order Service** (`:8084`) — waits for health check `UP`.
   - **API Gateway** (`:8080`) — waits for health check `UP`.
4. Waits for Eureka registry synchronization across all services.
5. Launches the **React 18 Frontend** on port `5173`.
6. Saves tracked process IDs to `.running-processes.json` and outputs logs to `logs/`.

---

### Step 3: Stop All Services Cleanly

When you are done testing, gracefully shut down all services with one command:

```powershell
.\stop-all-services.ps1
```

#### What this script does:
- Gracefully terminates all Java and Node.js process IDs tracked in `.running-processes.json`.
- Sweeps ports `8761`, `8080-8085`, and `5173` to ensure no orphaned background processes remain bound.
- Cleans up lock files.

---

## 3. Pre-Seeded Demonstration Accounts

You can immediately sign in at [http://localhost:5173](http://localhost:5173) with either account:

| Account Role | Email Address | Password | Initial State / Purpose |
| :--- | :--- | :--- | :--- |
| 👤 **Customer** | `customer@eshopping.com` | `Pass1234!` | Pre-funded with **₹15,000.00** wallet balance; ready to add items to cart and execute checkout. |
| 🏪 **Merchant** | `merchant@eshopping.com` | `Pass1234!` | Registered merchant; owner of 12 catalog products; access to merchant dashboard and stock controls. |

*To onboard a new merchant via the registration form, use the administrative secret code:*  
**`ESHOPPING_MERCHANT_INVITE_2026`**

---

## 4. Alternative: Manual Startup (Separate Terminal Windows)

If you prefer to see live logging in individual PowerShell windows, open 8 separate terminals and run each service in the following exact sequence:

### Terminal 1: Discovery Server (Port 8761)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\discovery-server"
mvn spring-boot:run
```
*(Wait until console shows `Started DiscoveryServerApplication` on port 8761)*

### Terminal 2: Profile Microservice (Port 8081)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\profile-service"
mvn spring-boot:run
```

### Terminal 3: Product Microservice (Port 8082)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\product-service"
mvn spring-boot:run
```

### Terminal 4: Cart Microservice (Port 8083)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\cart-service"
mvn spring-boot:run
```

### Terminal 5: Wallet Microservice (Port 8085)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\wallet-service"
mvn spring-boot:run
```

### Terminal 6: Order Microservice (Port 8084)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\order-service"
mvn spring-boot:run
```

### Terminal 7: API Gateway (Port 8080)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\backend\api-gateway"
mvn spring-boot:run
```

### Terminal 8: React Frontend (Port 5173)
```powershell
cd "c:\Users\vivek\Desktop\E Shopping Zone\frontend"
npm run dev
```

---

## 5. Log Files & Monitoring

When using `.\start-all-services.ps1`, real-time logs are redirected to the `logs/` directory:

```
c:\Users\vivek\Desktop\E Shopping Zone\logs\
├── discovery_server-stdout.log / discovery_server-stderr.log
├── profile_service-stdout.log  / profile_service-stderr.log
├── product_service-stdout.log  / product_service-stderr.log
├── cart_service-stdout.log     / cart_service-stderr.log
├── wallet_service-stdout.log   / wallet_service-stderr.log
├── order_service-stdout.log    / order_service-stderr.log
├── api_gateway-stdout.log      / api_gateway-stderr.log
└── frontend-stdout.log         / frontend-stderr.log
```

To tail logs in real time in PowerShell:
```powershell
Get-Content logs\order_service-stdout.log -Wait -Tail 20
```

---

## 6. Troubleshooting

### Port Conflict (`Address already in use: bind`)
If any port (`8761`, `8080-8085`, `5173`) is already bound by a previous background process:
```powershell
.\stop-all-services.ps1
```
*This forcefully sweeps and kills any orphaned processes on those ports.*

### MySQL Authentication Failure (`Access denied for user 'root'@'localhost'`)
If Profile Service or another service fails with `SQLException: Access denied for user 'root'@'localhost' (using password: YES)`:
- Check lines 14–17 of your `.env` file:
  - If your local MySQL has **no password**, set `MYSQL_PASSWORD=` and `DB_PASSWORD=` (leave blank after `=`).
  - If your local MySQL uses a **custom password**, set `MYSQL_PASSWORD=your_actual_password` and `DB_PASSWORD=your_actual_password`.
- Re-run `.\start-all-services.ps1`.

### Gateway 503 Service Unavailable (`No servers available for service`)
If you access an endpoint via API Gateway (`:8080`) immediately after startup and see `Whitelabel Error Page: 503 Service Unavailable`:
- This occurs when the Gateway's initial 30-second Eureka registry fetch cycle has not yet completed.
- Wait 5–10 seconds and refresh the browser (F5).
- Verify all services report `UP` on [http://localhost:8761](http://localhost:8761).

### GitHub OAuth "Invalid Redirect URI"
If GitHub displays `The redirect_uri is not associated with this application`:
- Open **[GitHub Developer Settings → OAuth Apps → E-ShoppingZone](https://github.com/settings/developers)**.
- Ensure the **Redirect URIs** list contains all 3 supported variants:
  - `http://localhost:8080/login/oauth2/code/github`
  - `http://localhost:8081/login/oauth2/code/github`
  - `http://<your-lan-ip>:8081/login/oauth2/code/github`
- Ensure the browser initiates OAuth on the same domain as the callback to avoid browser cross-origin cookie boundaries.

### CSRF Violation on Token Refresh
If `/api/auth/refresh` or `/api/auth/logout` returns `403 CSRF_VIOLATION`:
- `AntiCsrfFilter` mandates the `X-Requested-With: XMLHttpRequest` header on all cookie-based protected paths.
- `axiosClient.ts` automatically attaches this header to all HTTP requests and auto-refresh calls.

### Rebuilding Jars After Code Changes
If you modify Java source code, rebuild the artifacts before starting:
```powershell
cd backend
mvn clean package -DskipTests
cd ..
```

