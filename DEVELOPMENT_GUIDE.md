# E-ShoppingZone Developer Guide

## 1. Welcome to E-ShoppingZone

This guide provides developers with the essential guidelines, development environment instructions, conventions, and operational workflows needed to develop, run, and debug the E-ShoppingZone platform.

---

## 2. Technology Stack & Framework Baseline

Every backend microservice runs strictly on:
- **Java**: `1.8` (Java 8).
- **Spring Boot**: `2.7.18` (Latest 2.7.x release).
- **Spring Cloud**: `2021.0.9` (Compatible with Spring Boot 2.7.x).
- **Spring Security**: `5.7.12`.
- **Spring Data JPA**: `2.7.18` / Hibernate `5.6.15.Final`.
- **Springdoc OpenAPI**: `1.8.0` (Do NOT use 2.x).
- **JJWT**: `0.11.5` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
- **Database**: MySQL `8.0.x`.
- **Frontend**: React 18, TypeScript 5, Vite, Axios, TailwindCSS / CSS Modules, React Router v6.

---

## 3. Directory Layout

```
c:/Users/vivek/Desktop/E Shopping Zone/
├── backend/
│   ├── pom.xml
│   ├── discovery-server/      # Netflix Eureka Registry (:8761)
│   ├── api-gateway/           # Spring Cloud Gateway (:8080)
│   ├── profile-service/       # Auth, Profile, RTR, OAuth (:8081)
│   ├── product-service/       # Catalog, Categories, Stock (:8082)
│   ├── cart-service/          # Shopping Cart & Live Prices (:8083)
│   ├── order-service/         # Checkout Saga & Orders (:8084)
│   └── wallet-service/        # Digital Wallet & Ledger (:8085)
├── frontend/                  # React 18.3.1 + TypeScript + Vite 5 (:5173)
├── scripts/
│   └── init-databases.sql     # Master MySQL initialization script (5 schemas + seed)
├── postman/                   # Postman collection & environment (37 automated tests)
├── logs/                      # Service runtime logs
├── start-all-services.ps1     # Automated startup script
├── stop-all-services.ps1      # Automated shutdown & port-sweep script
├── verify-full-system-e2e.ps1 # 8-phase E2E integration test script
├── STARTUP_GUIDE.md           # Master startup & Swagger guide
├── SETUP_GUIDE.md             # Local Windows installation & setup guide
├── TESTING_GUIDE.md           # Testing strategy & test runner guide
├── API_DOCUMENTATION.md       # Complete REST API reference
├── ARCHITECTURE.md            # C4 diagrams & Saga state machine
├── DATABASE.md                # Schemas, dictionaries & ER diagrams
├── SECURITY.md                # Security architecture & CSRF/IDOR defenses
├── PROJECT_STATUS.md          # 100% Phase completion & acceptance matrix
├── CHANGELOG.md               # Version history
└── README.md                  # Landing page
```

---

## 4. Local Execution Reference

### Step 1: Start MySQL Server & Initialize Schemas
Ensure MySQL is running on port `3306`. Run the master database setup script:
```powershell
Get-Content scripts\init-databases.sql | mysql -u root -p
```

### Step 2: Build All Backend Services (Java 8 Baseline)
```powershell
cd backend
mvn clean package -DskipTests
cd ..
```

### Step 3: Launch Everything with One Command
```powershell
.\start-all-services.ps1
```
*(Or run `.\start-all-services.ps1 -FrontendDevMode` for hot-reload dev mode).*

### Step 4: Stop All Services Cleanly
```powershell
.\stop-all-services.ps1
```

For complete details on accessing Swagger UIs, demo credentials, and manual terminal options, see **[STARTUP_GUIDE.md](STARTUP_GUIDE.md)**.

