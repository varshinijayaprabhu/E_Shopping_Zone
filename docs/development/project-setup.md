# Project Setup & Development Environment Guide

## 1. Prerequisites

Before building and running E-ShoppingZone, ensure your local development workstation satisfies the following software baseline:

| Component | Minimum Version | Verified Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Java JDK** | 1.8.0 | Java 8 (OpenJDK / Temurin 8 or 17 targeting 8) | Backend microservices compilation & runtime |
| **Apache Maven** | 3.6.3 | 3.9.x | Backend dependency management and builds |
| **Node.js** | 18.x | 20.x or 22.x | Frontend runtime environment |
| **npm** | 9.x | 10.x | Frontend package manager |
| **MySQL Server** | 8.0.x | 8.0.x | Relational persistence on port `3306` |
| **Postman** | Latest | Latest | REST API collection testing |

---

## 2. Repository Layout

```
E-SHOPPING-ZONE/
├── backend/
│   ├── pom.xml                 # Root Parent POM (Spring Boot 2.7.18, Java 8)
│   ├── discovery-server/       # Eureka Registry (Port 8761)
│   ├── api-gateway/            # Spring Cloud Gateway (Port 8080)
│   ├── profile-service/        # Auth & Profile Domain (Port 8081)
│   ├── product-service/        # Catalog & Inventory Domain (Port 8082)
│   ├── cart-service/           # Shopping Cart Domain (Port 8083)
│   ├── order-service/          # Order & Checkout Domain (Port 8084)
│   └── wallet-service/         # Digital Ledger Domain (Port 8085)
├── frontend/
│   └── e-shopping-ui/          # React 18 + TypeScript SPA (Port 5173)
├── database/
│   ├── schema/                 # DDL init scripts per service
│   ├── migrations/             # Versioned schema updates
│   └── seed/                   # Demo accounts & catalog products
├── docs/                       # Comprehensive documentation
├── postman/                    # Postman collection & environment JSON
├── README.md
├── DEVELOPMENT_GUIDE.md
├── PROJECT_STATUS.md
└── IMPLEMENTATION_ROADMAP.md
```

---

## 3. Quickstart Step-by-Step

### Step 1: Database Setup
Launch MySQL Server and execute the master initialization script:
```bash
mysql -u root -p < database/schema/init-all-databases.sql
mysql -u root -p < database/seed/seed-data.sql
```

### Step 2: Build Backend Modules
From the `backend/` directory:
```bash
cd backend
mvn clean install -DskipTests
```

### Step 3: Start Services in Prescribed Sequence
Follow the dependency startup sequence:
1. `discovery-server` (Port 8761)
2. `api-gateway` (Port 8080)
3. `profile-service` (Port 8081)
4. `product-service` (Port 8082)
5. `cart-service` (Port 8083)
6. `wallet-service` (Port 8085)
7. `order-service` (Port 8084)

### Step 4: Launch React Single-Page Application
```bash
cd frontend/e-shopping-ui
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

