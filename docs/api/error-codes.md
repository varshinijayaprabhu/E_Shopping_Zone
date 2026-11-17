# Global Error Codes & Status Codes Reference

## 1. HTTP Status Code Conventions

| HTTP Status | Standard Meaning in E-ShoppingZone | Typical Usage Scenario |
| :--- | :--- | :--- |
| `200 OK` | Request completed successfully | Fetch queries, updates, successful actions |
| `201 Created` | New resource created successfully | Registration, product creation, checkout |
| `204 No Content` | Action completed, empty body | Cart clearance |
| `400 Bad Request` | Malformed request or domain rule failure | Stock deficiency, insufficient funds, invalid inputs |
| `401 Unauthorized` | Missing, invalid, or expired credentials | Expired JWT, wrong password, replay attack |
| `403 Forbidden` | Authenticated, but lacks authority | Customer calling merchant API, merchant updating other's product |
| `404 Not Found` | Target resource does not exist | Invalid product ID, cart not found |
| `409 Conflict` | Unique constraint violation | Email already registered, duplicate SKU |
| `500 Internal Error` | Unexpected server error | Database down, uncaught runtime exception |
| `503 Service Unavailable` | Downstream microservice offline | Product service unreachable during cart add |
| `504 Gateway Timeout` | Downstream service read timeout | Network stall exceeded 5000ms |

---

## 2. Domain-Specific Business Error Codes

| Error Code | HTTP Status | Description |
| :--- | :--- | :--- |
| `VALIDATION_FAILED` | 400 | One or more DTO fields failed `@Valid` constraints |
| `EMAIL_ALREADY_EXISTS` | 409 | Attempting to register an email already in `profile_db` |
| `INVALID_CREDENTIALS` | 401 | Incorrect email or password during login |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh token passed expiry timestamp |
| `REFRESH_TOKEN_REUSED` | 401 | Reuse of a previously rotated token; all sessions revoked |
| `ACCESS_DENIED` | 403 | Role-based permission check failed |
| `PRODUCT_NOT_FOUND` | 404 | Product with requested ID does not exist |
| `INSUFFICIENT_STOCK` | 400 | Requested quantity exceeds `available_stock` |
| `MERCHANT_UNAUTHORIZED` | 403 | Merchant attempted to edit a product owned by another merchant |
| `EMPTY_CART` | 400 | Cannot checkout an empty shopping cart |
| `INSUFFICIENT_FUNDS` | 400 | Wallet balance is insufficient to complete the transaction |
| `ORDER_NOT_FOUND` | 404 | Order with requested ID does not exist |
| `ORDER_CANNOT_BE_CANCELLED` | 400 | Order has already been shipped or delivered |
| `DOWNSTREAM_SERVICE_ERROR` | 503 | Inter-service communication failed |
| `IDEMPOTENT_CONFLICT` | 409 | Duplicate idempotent request currently executing |

