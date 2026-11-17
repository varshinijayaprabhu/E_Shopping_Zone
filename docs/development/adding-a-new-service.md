# Adding a New Microservice Guide

## 1. Overview

Follow these standardized steps when introducing a new domain service into the E-ShoppingZone ecosystem.

---

## 2. Step-by-Step Procedure

1. **Create Module Directory**:
   Create a new folder under `backend/<new-service-name>`.
2. **Register in Parent POM**:
   Add `<module><new-service-name></module>` in `backend/pom.xml`.
3. **Configure Dependencies in Child `pom.xml`**:
   Inherit from the root parent POM. Include core starters:
   - `spring-boot-starter-web`
   - `spring-boot-starter-data-jpa`
   - `spring-cloud-starter-netflix-eureka-client`
   - `spring-boot-starter-validation`
   - `springdoc-openapi-ui` (version `1.8.0`)
   - `mysql-connector-java`
4. **Define Application Class & Eureka Client**:
   Annotate main class with `@SpringBootApplication` and `@EnableEurekaClient`.
5. **Configure `application.yml`**:
   - Assign dedicated server port.
   - Configure Eureka client service URL: `http://localhost:8761/eureka/`.
   - Configure isolated datasource for `<service>_db`.
6. **Register Gateway Route**:
   In `api-gateway/src/main/resources/application.yml`, register new route predicate:
   ```yaml
   - id: new-service
     uri: lb://NEW-SERVICE
     predicates:
       - Path=/api/new-path/**
   ```
7. **Create MySQL Schema**:
   Add new schema definition script in `database/schema/`.
8. **Document Swagger & OpenAPI**:
   Expose Swagger UI at `http://localhost:<port>/swagger-ui/index.html`.

