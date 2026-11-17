# Order & Checkout State-Machine Saga Service API Specification

The Order Microservice (`backend/order-service`) runs on port `8084` and orchestrates multi-service checkout workflows using a distributed State-Machine Saga pattern with forward and compensating transactions across Cart Service (`8083`), Product Service (`8082`), and Wallet Service (`8085`).

---

## 1. Checkout State-Machine Saga

- **Endpoint**: `POST /api/orders/checkout`
- **Access**: Protected (`ROLE_CUSTOMER` or authenticated user)
- **Headers**:
  - `Authorization: Bearer <accessToken>`
  - `Content-Type: application/json`

### Request Body
```json
{
  "idempotencyKey": "IDEMP-20261107-A91B4C",
  "paymentMethod": "WALLET",
  "shippingName": "Alice Customer",
  "shippingPhone": "+91-9876543210",
  "shippingAddress": "123 Tech Park, Bellandur, Bengaluru 560103"
}
```

### Validation Rules
- `idempotencyKey`: Mandatory, max 100 characters. Replayed identical requests return the existing confirmed order without re-charging or re-reserving. Different payloads or different customer IDs with the same key are rejected with `400 Bad Request`.
- `paymentMethod`: Mandatory enum (`WALLET` or `CASH_ON_DELIVERY`).
- `shippingName`: Mandatory, max 100 characters.
- `shippingPhone`: Mandatory, max 25 characters.
- `shippingAddress`: Mandatory, max 500 characters.

### Saga Execution Sequence
1. **Idempotency Gate**: Check `OrderRepository.findByIdempotencyKey(key)`. If found with identical payload, return existing order immediately.
2. **Cart Inspection**: Call `GET http://cart-service/api/cart`. Verify non-empty items and live stock sufficiency.
3. **Product Snapshotting**: Query `Product Service` for product names, current unit prices, and merchant ownership (`merchantId`).
4. **Step A (Inventory Reservation)**: Call `POST http://product-service/api/products/reserve` with service headers (`X-Internal-Secret`). If stock is insufficient, abort saga with `409 Insufficient Stock`.
5. **Step B (Payment Processing)**:
   - If `WALLET`: Call `POST http://wallet-service/api/wallet/pay` under pessimistic lock.
     - *If payment fails*: Immediately trigger compensation `POST http://product-service/api/products/release` and throw `PaymentFailedException`.
     - *If payment times out*: Query `GET http://wallet-service/api/wallet/payment/order/{orderNumber}` to reconcile before failing or releasing stock.
   - If `CASH_ON_DELIVERY`: Set `paymentStatus = COD_PENDING`.
6. **Step C (Order Persistence)**: Atomically insert `Order`, immutable line item snapshots (`order_items`), and `PaymentRecord` to `order_db`.
7. **Step D (Cart Clearance)**: Call `DELETE http://cart-service/api/cart` to clean up active customer cart.
8. **Finalization**: Transition `sagaState` to `COMPLETED` and return `OrderDTO`.

### Success Response `201 Created`
```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "id": 1,
    "orderNumber": "ORD-79BF9096E694",
    "customerId": 1,
    "orderStatus": "PLACED",
    "sagaState": "COMPLETED",
    "subtotalAmount": 1200.00,
    "shippingFee": 0.00,
    "totalAmount": 1200.00,
    "shippingName": "Alice Customer",
    "shippingPhone": "+91-9876543210",
    "shippingAddress": "123 Tech Park, Bellandur, Bengaluru 560103",
    "createdAt": "2026-11-07T14:41:35",
    "updatedAt": "2026-11-07T14:41:35",
    "items": [
      {
        "id": 1,
        "productId": 10,
        "productNameSnapshot": "Wireless Headphones",
        "unitPriceSnapshot": 600.00,
        "quantity": 2,
        "subtotal": 1200.00,
        "merchantId": 2
      }
    ],
    "payment": {
      "id": 1,
      "paymentReference": "PAY-A98B7C6D5E4F",
      "paymentMethod": "WALLET",
      "paymentStatus": "PAID",
      "amount": 1200.00,
      "createdAt": "2026-11-07T14:41:35"
    }
  },
  "timestamp": "2026-11-07T14:41:35"
}
```

---

## 2. Customer Order History

- **Endpoint**: `GET /api/orders`
- **Access**: Protected (`ROLE_CUSTOMER` or authenticated user)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Query Parameters**:
  - `page` (default `0`)
  - `size` (default `10`)

### Success Response `200 OK`
```json
{
  "success": true,
  "message": "Customer orders retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "orderNumber": "ORD-INIT-001",
        "customerId": 1,
        "orderStatus": "DELIVERED",
        "sagaState": "COMPLETED",
        "totalAmount": 14999.00,
        "items": [...]
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

---

## 3. Order Details Lookup (Anti-IDOR Protection)

- **Endpoint**: `GET /api/orders/{id}`
- **Access**: Protected (`ROLE_CUSTOMER` owner)
- **Headers**: `Authorization: Bearer <accessToken>`

### Security Enforcement
- Resolves authenticated customer ID from JWT / gateway headers.
- Queries `orderRepository.findByIdAndCustomerId(id, customerId)`.
- If an unauthenticated user or different customer attempts access, returns `404 Not Found` (never leaks order existence to unauthorized callers).

---

## 4. Merchant Order Items Visibility

- **Endpoint**: `GET /api/orders/merchant`
- **Access**: Protected (`ROLE_MERCHANT` only)
- **Headers**: `Authorization: Bearer <merchantToken>`
- **Query Parameters**: `page=0&size=10`

### Security Enforcement
- Automatically scopes results to products where `merchant_id == authenticatedUserId`.
- Callers with `ROLE_CUSTOMER` are rejected with `403 Forbidden`.
- Merchants only see their line items and recipient shipping details, preventing cross-merchant catalog data leaks.

---

## 5. Order Cancellation Saga (Compensating Transactions)

- **Endpoint**: `POST /api/orders/{id}/cancel`
- **Access**: Protected (`ROLE_CUSTOMER` owner)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Request Body** (optional):
```json
{
  "reason": "Customer requested cancellation"
}
```

### Compensating Workflow
1. Verifies order ownership and cancellable status (`PLACED`, `PENDING`, `CONFIRMED`). Orders already `SHIPPED` or `DELIVERED` cannot be cancelled.
2. If payment was `WALLET` and status was `PAID`, calls `POST http://wallet-service/api/wallet/refund` with `X-Internal-Secret` to restore wallet balance.
3. Calls `POST http://product-service/api/products/release` with `X-Internal-Secret` to restore reserved product stock.
4. Marks order status `CANCELLED` and payment status `REFUNDED`.

---

## 6. Public / Authenticated Order Status Lookup

- **Endpoint**: `GET /api/orders/status/{orderNumber}`
- **Access**: Public / Authenticated

---

## 7. Internal Saga Reconciliation

- **Endpoint**: `POST /api/orders/reconcile`
- **Access**: Internal Services only
- **Headers**: `X-Internal-Secret: ESHOPPING_INTERNAL_SECRET_2026`
- **Description**: Inspects stuck orders in intermediate saga states (`STOCK_RESERVED`, `COMPENSATING_STOCK`), verifies payment status with Wallet Service, and drives orders forward to completion or backward to compensation.
