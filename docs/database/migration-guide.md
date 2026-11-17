# Database Migration & Initialization Guide

## 1. Overview

Each microservice in E-ShoppingZone connects to its dedicated schema. In development environments, schemas and seed data can be initialized using native SQL scripts located in the `database/` folder or via Spring Data JPA `ddl-auto: update` mode.

---

## 2. Local MySQL Setup Instructions

### 2.1 Prerequisites
- MySQL Server 8.0+ running on port `3306`.
- Root or administrative user with privileges to create databases and grant permissions.

### 2.2 Execution of Master Init Script
Run the initialization script using the MySQL CLI:

```bash
mysql -u root -p < database/schema/init-all-databases.sql
```

Alternatively, from within the MySQL interactive shell:

```sql
source c:/Users/vivek/Desktop/E Shopping Zone/database/schema/init-all-databases.sql;
```

---

## 3. Database Credentials Configuration

Each microservice defaults to local credentials, which can be overridden using environment variables:

| Variable Name | Default Value | Description |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | MySQL Server host |
| `DB_PORT` | `3306` | MySQL Server port |
| `DB_USER` | `root` | Database username |
| `DB_PASSWORD` | `root` | Database password |

Example per-service connection URL format:
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/profile_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

## 4. Flyway Migration Strategy (Optional / Production)

For controlled production deployments, Flyway scripts follow the naming format:
`V1__initial_schema.sql`, `V2__add_indexes.sql`.
Spring Boot will automatically detect and execute migrations placed under `src/main/resources/db/migration` within each microservice module.

