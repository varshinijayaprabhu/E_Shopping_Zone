# Coding Conventions & Engineering Guidelines

## 1. Backend Architecture Conventions (Java 8 / Spring Boot)

1. **Layered Architecture & Separation of Concerns**:
   - `controller`: Thin REST controllers responsible for endpoint exposure, input validation via `@Valid`, and routing. No business logic.
   - `service`: Domain services holding business algorithms, transactions (`@Transactional`), and cross-service calls.
   - `repository`: Spring Data JPA interfaces. No direct database queries in controllers or services.
   - `dto`: Request and response data transfer objects. Entities are never returned directly from controllers.
   - `mapper`: Mapping logic transforming Entities to DTOs and vice-versa.
   - `exception`: Centralized exceptions handled via `@RestControllerAdvice`.
2. **Dependency Injection**:
   - Always use **constructor-based dependency injection**. Avoid field injection (`@Autowired` on private fields) to facilitate isolated unit testing.
   ```java
   @Service
   public class ProductServiceImpl implements ProductService {
       private final ProductRepository productRepository;
       private final CategoryRepository categoryRepository;

       public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
           this.productRepository = productRepository;
           this.categoryRepository = categoryRepository;
       }
   }
   ```
3. **Java 8 Compatibility Rules**:
   - Use `java.time` (e.g. `Instant`, `LocalDateTime`) instead of legacy `java.util.Date`.
   - Use `javax.validation.*`, `javax.persistence.*` (Jakarta EE 9+ packages like `jakarta.*` are prohibited under Spring Boot 2.7.x).
   - Java records and text blocks are prohibited; use standard classes or Lombok if desired, or standard POJOs with getters/setters/constructors.
4. **Monetary Precision**:
   - Always use `java.math.BigDecimal` for prices, balances, and totals. Never use `float` or `double`.

---

## 2. Frontend Architecture Conventions (React + TypeScript)

1. **Feature-Based Modular Organization**:
   - Features (`auth`, `products`, `cart`, `orders`, `wallet`, `profile`, `merchant`) encapsulate their components, hooks, and types.
2. **Type Safety**:
   - All API payloads, component props, and context states must have explicit TypeScript interfaces. Avoid `any`.
3. **API Centralization**:
   - All Axios/fetch calls reside inside `src/services/` or `src/features/*/api.ts`. Components never execute raw HTTP requests directly.
4. **Security & Sanitization**:
   - Never use `dangerouslySetInnerHTML`. Rely on React standard JSX bindings.

