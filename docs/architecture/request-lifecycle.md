# Request Lifecycle Specification

## 1. Request Ingress & Gateway Flow

When an HTTP request originates from the React frontend or Postman client, it travels through a deterministic lifecycle across perimeter filters, edge routing, downstream security filters, controllers, service beans, repositories, and persistence databases.

```mermaid
flowchart TD
    Client(["Client (Browser / Postman)"])
    
    subgraph GatewayBoundary ["API Gateway (Port 8080)"]
        CorsFilter["1. CorsWebFilter<br/>Validate origin & allowed headers"]
        TraceFilter["2. CorrelationIdFilter<br/>Generate or preserve X-Correlation-Id"]
        AuthFilter{"3. JwtAuthenticationFilter<br/>Is route public?"}
        TokenValidation["3a. Validate JWT Signature & Expiry<br/>Extract claims into request headers"]
        RejectAuth["3b. Return 401 Unauthorized JSON"]
        RouteLocator["4. RouteLocator<br/>Match path prefix & resolve Eureka instance"]
    end

    subgraph ServiceBoundary ["Target Microservice (e.g. Order Service 8084)"]
        DownstreamSecurity["5. Internal Security Filter<br/>Verify X-User-Id / JWT Header"]
        Controller["6. RestController<br/>Validate request body DTO (@Valid)"]
        ServiceLayer["7. Domain Service<br/>Execute business logic & call other services"]
        RepoLayer["8. Spring Data JPA Repository<br/>Execute query via Hibernate"]
        Database[("9. MySQL Database")]
        ResponseMapper["10. ModelMapper / DTO Assembler<br/>Transform Entity to ResponseDTO"]
    end

    Client --> CorsFilter
    CorsFilter --> TraceFilter
    TraceFilter --> AuthFilter
    AuthFilter -- "Public (/api/products/**)" --> RouteLocator
    AuthFilter -- "Protected (/api/orders/**)" --> TokenValidation
    TokenValidation -- "Invalid / Expired" --> RejectAuth
    RejectAuth --> Client
    TokenValidation -- "Valid Token" --> RouteLocator
    RouteLocator --> DownstreamSecurity
    DownstreamSecurity --> Controller
    Controller --> ServiceLayer
    ServiceLayer --> RepoLayer
    RepoLayer --> Database
    Database --> RepoLayer
    RepoLayer --> ServiceLayer
    ServiceLayer --> ResponseMapper
    ResponseMapper --> Controller
    Controller --> Client
```

---

## 2. Step-by-Step Execution Lifecycle

### Phase 1: Client Submission
- The React application sends an asynchronous request (via `axios` or `fetch`) targeting `http://localhost:8080/api/...`.
- If an access token exists in application memory, it is appended to the `Authorization` header (`Bearer eyJ...`).
- Cross-origin credentials (`withCredentials: true`) are enabled so that refresh token cookies are included.

### Phase 2: Edge Interception (Spring Cloud Gateway)
1. **CORS Evaluation**: Inflight preflight `OPTIONS` requests are validated against allowed origins (`http://localhost:5173`) and returned with required headers (`Access-Control-Allow-Credentials: true`).
2. **Correlation ID Injection**: A unique UUID is assigned to `X-Correlation-Id` if not already present.
3. **Route & Security Classification**:
   - Public paths (e.g., `/api/auth/login`, `/api/auth/register`, `/api/products/**`) pass directly without token verification.
   - Protected paths (e.g., `/api/cart/**`, `/api/orders/**`, `/api/wallet/**`) undergo reactive JWT signature verification using the shared HMAC secret.
   - If verification succeeds, user metadata is injected into internal forwarding headers:
     - `X-User-Id: 42`
     - `X-User-Email: user@example.com`
     - `X-User-Role: ROLE_CUSTOMER`
4. **Dynamic Routing**: Route predicate forwards the request to an instance resolved from Eureka (`lb://ORDER-SERVICE`).

### Phase 3: Service-Level Ingestion
1. **Context Initialization**: `UserContextFilter` reads internal headers (`X-User-Id`, `X-User-Role`) and establishes a `SecurityContext` in `ThreadLocal` storage.
2. **Controller Validation**: `@Valid` enforces JSR-380 constraints (e.g., `@NotNull`, `@Min`, `@Size`). Validation violations trigger `MethodArgumentNotValidException`, handled centrally by `@RestControllerAdvice`.
3. **Business Execution**: Domain services execute transactional state changes. Inter-service REST dependencies are called via `@LoadBalanced RestTemplate`.
4. **DTO Transformation**: Entities are mapped to immutable response DTOs, ensuring no internal database entities leak to the caller.
5. **Standardized Response**: The client receives a clean JSON payload with accurate HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `500 Internal Error`).

