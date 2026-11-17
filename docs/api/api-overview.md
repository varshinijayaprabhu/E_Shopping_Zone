# API Architecture Overview & Contract Guidelines

## 1. Gateway Routing Table

All external clients (React SPA, mobile apps, third-party consumers, Postman) communicate exclusively through the API Gateway at `http://localhost:8080`. The Gateway routes requests dynamically to registered downstream Eureka instances.

| External Path Pattern | Target Microservice | Eureka Service ID | Auth Requirement |
| :--- | :--- | :--- | :--- |
| `/api/auth/**` | Profile Service | `PROFILE-SERVICE` | Public (Login, Register, Refresh) / Protected (Logout) |
| `/api/profile/**` | Profile Service | `PROFILE-SERVICE` | Protected (`ROLE_CUSTOMER`, `ROLE_MERCHANT`) |
| `/api/products/**` | Product Service | `PRODUCT-SERVICE` | Public (Read/Search) / Protected (`ROLE_MERCHANT` for CRUD) |
| `/api/categories/**` | Product Service | `PRODUCT-SERVICE` | Public (Read) / Protected (`ROLE_MERCHANT` for Create) |
| `/api/cart/**` | Cart Service | `CART-SERVICE` | Protected (`ROLE_CUSTOMER`) |
| `/api/orders/**` | Order Service | `ORDER-SERVICE` | Protected (`ROLE_CUSTOMER` for checkout/history, `ROLE_MERCHANT` for orders) |
| `/api/wallet/**` | Wallet Service | `WALLET-SERVICE` | Protected (`ROLE_CUSTOMER`) |

---

## 2. Global Request & Response Standards

### 2.1 Content-Type & Character Encoding
All requests and responses use `application/json;charset=UTF-8`.

### 2.2 Standard Success Response Envelope
For non-paginated object responses:
```json
{
  "success": true,
  "message": "Resource retrieved successfully",
  "data": { ... }
}
```

For paginated listing responses:
```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": [ ... ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 48,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

### 2.3 Standard Error Response Envelope
```json
{
  "timestamp": "2026-11-07T10:15:30Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Product with ID 100 was not found",
  "path": "/api/products/100",
  "correlationId": "f78d91b4-2b6e-4402-985b-a81d4a9ec045"
}
```

### 2.4 Standard Validation Error Envelope
```json
{
  "timestamp": "2026-11-07T10:15:30Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Invalid input arguments provided",
  "path": "/api/auth/register",
  "fieldErrors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Email must be a valid email address format"
    },
    {
      "field": "password",
      "rejectedValue": "123",
      "message": "Password must be at least 8 characters long"
    }
  ],
  "correlationId": "a189f213-909d-4762-b118-20da5943ebc1"
}
```

