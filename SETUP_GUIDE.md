G# E-ShoppingZone Local Windows Setup & Operations Guide

[![Java 8](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-blue.svg)](https://www.mysql.com/)
[![React](https://img.shields.io/badge/React-18.3.1-cyan.svg)](https://react.dev/)

This guide provides step-by-step instructions for installing, configuring, initializing, booting, and verifying the complete **E-ShoppingZone** microservices suite on a native **Microsoft Windows** workstation.

> [!IMPORTANT]
> **Zero Containerization Policy**: In accordance with the Capgemini Case Study specification and project requirements, this application runs entirely bare-metal on local Windows services (Java 8 JVM, native MySQL 8.4, and Node.js Vite server). **Docker and Kubernetes are strictly not required and not used.**

---

## 1. System Prerequisites

Ensure the following runtimes and tools are installed and present in your system `PATH`:

| Tool / Runtime | Required Version | Verification Command | Purpose |
| :--- | :---: | :--- | :--- |
| **Java JDK** | `8` (or 17 targeting 8) | `java -version` | Compiles and executes 8 Spring Boot microservices |
| **Apache Maven** | `3.6.0+` | `mvn -v` | Monorepo reactor build tool |
| **MySQL Server** | `8.0+` or `8.4+` | `mysql --version` | Relational database engine (listening on port `3306`) |
| **Node.js** | `18.x` or `20.x` | `node -v` | Frontend build and development server |
| **npm** | `9.x` or `10.x` | `npm -v` | Frontend package manager |
| **PowerShell** | `5.1` or `7.x` | `$PSVersionTable.PSVersion` | Service lifecycle scripts and test runners |

---

## 2. Database Initialization

The platform uses a **Database-Per-Service** architecture with 6 distinct physical schemas on MySQL:
1. `profile_db`
2. `product_db`
3. `cart_db`
4. `order_db`
5. `wallet_db`
6. `admin_db`

### 2.1 One-Command Database Setup
The master initialization script located at `scripts/init-databases.sql` creates all six databases, establishes all required tables, foreign keys, indexes, and populates initial seed data (4 product categories, 28 catalog products with stock, and demo user accounts).

Execute the script in PowerShell:
```powershell
# Using MySQL CLI directly
Get-Content scripts\init-databases.sql | mysql -u root -p
```
*(When prompted, enter your MySQL root password. If your root user has no password, omit `-p` or press Enter).*

> [!TIP]
> If you already have existing databases initialized and only want to add the 16 newest products (IDs 13–28) without dropping or re-creating tables, execute the delta script:
> ```powershell
> Get-Content scripts\add-more-products.sql | mysql -u root -p
> ```

Alternatively, you can open `scripts/init-databases.sql` in **MySQL Workbench** or **DBeaver** and execute the entire script.

---

## 3. Environment & Secrets Configuration

All sensitive configuration parameters are externalized and support override via environment variables.

### 3.1 Template `.env.example`
A complete template is provided at `.env.example` in the workspace root. Create a local `.env` or set system environment variables:

```bash
# ==============================================================================
# Database Credentials
# ==============================================================================
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root

# ==============================================================================
# Security & JWT Secrets
# ==============================================================================
# 512-bit HMAC SHA-512 signing secret (minimum 64 characters)
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION_MS=900000

# Refresh token expiration (7 days = 604800 seconds)
REFRESH_TOKEN_EXPIRATION_SEC=604800

# Administrative Merchant registration invite code
ESHOPPING_MERCHANT_INVITE_CODE=ESHOPPING_MERCHANT_INVITE_2026

# Internal Saga inter-service authentication secret
ESHOPPING_INTERNAL_SERVICE_SECRET=ESHOPPING_INTERNAL_SERVICE_SECRET_2026

# ==============================================================================
# GitHub OAuth 2.0 (Optional for social login)
# ==============================================================================
GITHUB_CLIENT_ID=your_github_client_id_here
GITHUB_CLIENT_SECRET=your_github_client_secret_here
GITHUB_REDIRECT_URI=http://localhost:8080/login/oauth2/code/github
OAUTH2_REDIRECT_URI=http://localhost:5173/oauth2/redirect
```

### 3.2 GitHub OAuth 2.0 Social Login Setup

To enable one-click customer sign-in with GitHub:

1. Navigate to **[GitHub Developer Settings → OAuth Apps → New OAuth App](https://github.com/settings/applications/new)**.
2. Fill in the application registration form:
   | Field | Recommended Value | Description |
   | :--- | :--- | :--- |
   | **Application name** | `E-ShoppingZone` | Application name displayed on consent screen. |
   | **Homepage URL** | `http://localhost:5173` | The web frontend root address. |
   | **Application description** | `E-Commerce platform with instant checkout & digital wallet.` | Description displayed to users. |
   | **Authorization callback URL** | `http://localhost:8080/login/oauth2/code/github` | The standard Spring Security OAuth callback endpoint routed through API Gateway. |
   | **Redirect URIs** *(Additional)* | `http://localhost:8081/login/oauth2/code/github`<br>`http://<your-lan-ip>:8081/login/oauth2/code/github` | Registering direct service and LAN IP variants ensures multi-origin resilience. |
   | **Checkboxes** | ❌ Leave all unchecked | Wildcard matching, Device Flow, and token expiration are not required for web SPA. |
3. Click **Register application**.
4. Copy the **Client ID**. Click **Generate a new client secret** and copy the **Client Secret**.
5. Save both into your root `.env` file:
   ```env
   GITHUB_CLIENT_ID=your_copied_client_id
   GITHUB_CLIENT_SECRET=your_copied_client_secret
   ```
6. **Authentication Flow Architecture**:
   - The user clicks **"Continue with GitHub"** at `http://localhost:5173/login`.
   - The browser redirects to `/oauth2/authorization/github` on `profile-service`.
   - Upon user consent, GitHub redirects to the callback endpoint with an authorization code.
   - Spring Security auto-provisions or retrieves the customer profile, generates JWT access & refresh tokens, and stores a secure, one-time exchange code in `OAuth2ExchangeCodeStore`.
   - The user is redirected to `http://localhost:5173/oauth2/redirect?code=<one-time-code>`.
   - `OAuthRedirectHandler.tsx` sends `POST /api/auth/oauth2/exchange`, receives the user profile and JWT access token, saves the session into `AuthContext` and `localStorage`, and instantly directs the user to the home shopping catalog logged in.

---

## 4. Building the Project

### 4.1 Compile Backend Microservices
From the project root directory, run the Maven reactor build:
```powershell
cd backend
mvn clean package -DskipTests
cd ..
```
*Expected Result*: Build completes with `BUILD SUCCESS` across all 8 modules (`eshopping-parent`, `discovery-server`, `api-gateway`, `profile-service`, `product-service`, `cart-service`, `order-service`, `wallet-service`).

### 4.2 Verify Java 8 Bytecode Compliance
Verify that compiled `.class` files strictly comply with Java 8 bytecode specification (`major version: 52`):
```powershell
javap -v backend\order-service\target\classes\com\eshopping\order\OrderServiceApplication.class | Select-String "major version"
```
*Expected Output*: `major version: 52`

### 4.3 Install Frontend Dependencies
```powershell
cd frontend
npm install
npm run build
cd ..
```
*Expected Result*: Vite production bundle compiles cleanly into `frontend/dist/`.

---

## 5. Starting All Services

### 5.1 Automated Startup (Recommended)
An enterprise-grade startup orchestration script is provided: `start-all-services.ps1`.

Run the startup script in PowerShell:
```powershell
.\start-all-services.ps1
```

#### What `start-all-services.ps1` Does:
1. **Validates MySQL**: Probes `127.0.0.1:3306` to ensure the database engine is accessible.
2. **Port Conflict Check**: Checks all 8 application ports (`8761`, `8080`, `8081`, `8082`, `8083`, `8084`, `8085`, `5173`) and warns if any are already bound.
3. **Sequenced Boot**:
   - Boots `discovery-server` (Port `8761`) & polls `/actuator/health` until UP.
   - Boots `profile-service` (Port `8081`) & polls `/actuator/health` until UP.
   - Boots `product-service` (Port `8082`) & polls `/actuator/health` until UP.
   - Boots `cart-service` (Port `8083`) & polls `/actuator/health` until UP.
   - Boots `wallet-service` (Port `8085`) & polls `/actuator/health` until UP.
   - Boots `order-service` (Port `8084`) & polls `/actuator/health` until UP.
   - Boots `api-gateway` (Port `8080`) & polls `/actuator/health` until UP.
   - Boots React Frontend (Port `5173`) via `npm run dev`.
4. **Log Output**: Directs individual service logs to the root directory (`*_service-stdout.log` and `*_service-stderr.log`).
5. **PID Tracking**: Writes tracked process IDs to `.running-processes.json` for deterministic, clean termination.

---

### 5.2 Manual Startup Sequence (Alternative)
If you prefer running each service in a separate visible terminal window, open 8 PowerShell consoles and run in this strict sequence:

1. **Discovery Server** (Terminal 1):
   ```powershell
   cd backend\discovery-server
   mvn spring-boot:run
   ```
   *Wait until Eureka console shows port 8761 active.*

2. **Profile Service** (Terminal 2):
   ```powershell
   cd backend\profile-service
   mvn spring-boot:run
   ```

3. **Product Service** (Terminal 3):
   ```powershell
   cd backend\product-service
   mvn spring-boot:run
   ```

4. **Cart Service** (Terminal 4):
   ```powershell
   cd backend\cart-service
   mvn spring-boot:run
   ```

5. **Wallet Service** (Terminal 5):
   ```powershell
   cd backend\wallet-service
   mvn spring-boot:run
   ```

6. **Order Service** (Terminal 6):
   ```powershell
   cd backend\order-service
   mvn spring-boot:run
   ```

7. **API Gateway** (Terminal 7):
   ```powershell
   cd backend\api-gateway
   mvn spring-boot:run
   ```

8. **React Frontend** (Terminal 8):
   ```powershell
   cd frontend
   npm run dev
   ```

---

## 6. Accessing the Platform

Once all services are running:

| Component | URL | Expected Display |
| :--- | :--- | :--- |
| **React Web App** | [http://localhost:5173](http://localhost:5173) | E-ShoppingZone storefront & navigation |
| **Eureka Registry** | [http://localhost:8761](http://localhost:8761) | Dashboard showing 6 registered instances |
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | Edge entry point |
| **Profile Swagger** | [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html) | OpenAPI documentation for Auth & Profiles |
| **Product Swagger** | [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html) | OpenAPI documentation for Products & Stock |
| **Cart Swagger** | [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html) | OpenAPI documentation for Shopping Cart |
| **Order Swagger** | [http://localhost:8084/swagger-ui/index.html](http://localhost:8084/swagger-ui/index.html) | OpenAPI documentation for Orders & Saga |
| **Wallet Swagger** | [http://localhost:8085/swagger-ui/index.html](http://localhost:8085/swagger-ui/index.html) | OpenAPI documentation for Ledger & Wallet |

---

## 7. Pre-Configured Demo Accounts

The database initialization script seeds three ready-to-use user accounts:

| Role | Email Address | Password | Initial State / Capabilities |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@eshopping.com` | `Pass1234!` | Pre-funded digital wallet with **₹100,000.00**; ready for instant checkout. |
| **Merchant** | `merchant@eshopping.com` | `Pass1234!` | Registered merchant; owner of the 12 seeded catalog products across 4 categories. |
| **Customer (Zero Balance)** | `poor@eshopping.com` | `Pass1234!` | Pre-registered customer with **₹0.00** wallet balance; used to verify Saga payment failure rollback. |

---

## 8. Stopping All Services

### 8.1 Automated Shutdown (Recommended)
An enterprise-grade shutdown script is provided: `stop-all-services.ps1`.

Run the shutdown script in PowerShell:
```powershell
.\stop-all-services.ps1
```

#### What `stop-all-services.ps1` Does:
1. Reads `.running-processes.json` and terminates all tracked Java and Node.js process IDs gracefully.
2. Performs a thorough port sweep across ports `8761`, `8080`, `8081`, `8082`, `8083`, `8084`, `8085`, and `5173`.
3. If any orphaned processes remain bound to those ports, it forcefully terminates them and releases the socket handles.
4. Cleans up `.running-processes.json`.

---

## 9. Troubleshooting & Common Issues

### 9.1 Port Conflicts (`BindException`)
If a service fails to start because a port is already in use:
```powershell
# Identify process listening on port 8080
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object OwningProcess
# Terminate the offending process
Stop-Process -Id <PID> -Force
```
Or simply run `.\stop-all-services.ps1` to sweep all 8 platform ports automatically.

### 9.2 Eureka Service Registration Delays
During initial boot, downstream services may take up to 5-10 seconds to appear in the Eureka registry and for the Spring Cloud LoadBalancer cache to synchronize. The platform is pre-configured with aggressive 3-second cache TTLs (`spring.cloud.loadbalancer.cache.ttl: 3s`) to minimize cold-start latency.

### 9.3 MySQL Connection Refused
Ensure the MySQL Windows service is running:
```powershell
Get-Service -Name MySQL* | Start-Service
```
Verify that MySQL is listening on port `3306`:
```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 3306
```

