# Product & Category API Contract

## 1. List Products (Paginated & Filtered)

- **Endpoint**: `GET /api/products`
- **Access**: Public
- **Query Parameters**:
  - `page` (optional, default `0`): Zero-based page index.
  - `size` (optional, default `10`): Items per page.
  - `sortBy` (optional, default `id`): Sort field (`price`, `name`, `createdAt`).
  - `direction` (optional, default `ASC`): `ASC` or `DESC`.
  - `categoryId` (optional): Filter products by category.
  - `minPrice` (optional): Filter products where `price >= minPrice`.
  - `maxPrice` (optional): Filter products where `price <= maxPrice`.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Products retrieved successfully",
  "data": [
    {
      "id": 1,
      "merchantId": 2,
      "categoryId": 1,
      "categoryName": "Electronics",
      "name": "Pro Wireless Noise-Cancelling Headphones",
      "sku": "ELEC-HP-001",
      "description": "Over-ear headphones with 40-hour battery life and spatial audio.",
      "price": 14999.00,
      "imageUrl": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
      "availableStock": 50,
      "active": true
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 12,
    "totalPages": 2,
    "first": true,
    "last": false
  }
}
```

---

## 2. Search Products by Keyword

- **Endpoint**: `GET /api/products/search`
- **Access**: Public
- **Query Parameters**:
  - `query` (required): Free-text keyword matching product name or description.
  - `page` (optional, default `0`): Page number.
  - `size` (optional, default `10`): Page size.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Search results retrieved",
  "data": [
    {
      "id": 1,
      "name": "Pro Wireless Noise-Cancelling Headphones",
      "price": 14999.00,
      "availableStock": 50
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 3. Get Product by ID

- **Endpoint**: `GET /api/products/{id}`
- **Access**: Public

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "merchantId": 2,
    "categoryId": 1,
    "categoryName": "Electronics",
    "name": "Pro Wireless Noise-Cancelling Headphones",
    "sku": "ELEC-HP-001",
    "description": "Over-ear headphones with 40-hour battery life and spatial audio.",
    "price": 14999.00,
    "imageUrl": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
    "availableStock": 50,
    "active": true
  }
}
```

---

## 4. Create Product (Merchant Only)

- **Endpoint**: `POST /api/products`
- **Access**: Protected (`ROLE_MERCHANT`)
- **Headers**: `Authorization: Bearer <accessToken>`

### Request Body
```json
{
  "categoryId": 1,
  "name": "Mechanical Gaming Keyboard RGB",
  "sku": "ELEC-KB-004",
  "description": "Hot-swappable linear mechanical switches with PBT keycaps.",
  "price": 6499.00,
  "imageUrl": "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500",
  "initialStock": 35
}
```

### Response `201 Created`
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 13,
    "merchantId": 2,
    "categoryId": 1,
    "name": "Mechanical Gaming Keyboard RGB",
    "sku": "ELEC-KB-004",
    "price": 6499.00,
    "availableStock": 35,
    "active": true
  }
}
```

---

## 5. Update Product (Merchant Ownership Enforced)

- **Endpoint**: `PUT /api/products/{id}`
- **Access**: Protected (`ROLE_MERCHANT`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Updates product details and stock. Checks that the authenticated merchant matches `merchant_id`. If not, returns `403 Forbidden`.

### Request Body
```json
{
  "name": "Mechanical Gaming Keyboard RGB v2",
  "description": "Updated hot-swappable switches with sound-dampening foam.",
  "price": 5999.00,
  "imageUrl": "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=500",
  "availableStock": 50,
  "active": true
}
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Product updated successfully",
  "data": {
    "id": 13,
    "price": 5999.00,
    "availableStock": 50
  }
}
```

---

## 6. Delete Product (Merchant Ownership Enforced)

- **Endpoint**: `DELETE /api/products/{id}`
- **Access**: Protected (`ROLE_MERCHANT`)

### Response `200 OK`
```json
{
  "success": true,
  "message": "Product deleted successfully"
}
```

---

## 7. Category Endpoints

- `GET /api/categories`: Public listing of all active categories.
- `POST /api/categories`: Protected (`ROLE_MERCHANT`) category creation.

---

## 8. Internal Inventory Reservation & Release APIs (Inter-Service / Saga)

These endpoints are called synchronously by `Order Service` via `RestTemplate` during checkout orchestration and compensating transaction rollbacks.

### 8.1 Reserve Product Inventory
- **Endpoint**: `POST /api/products/reserve`
- **Access**: Internal Microservice (`Order Service`) / Gateway Verified
- **Description**: Atomically transitions stock from `available_stock` to `reserved_stock` for multiple items under an optimistic lock. If any item has insufficient stock, the entire reservation is aborted.

#### Request Body
```json
{
  "orderNumber": "ORD-20261107-A91B4C",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 5,
      "quantity": 1
    }
  ]
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Inventory successfully reserved",
  "data": {
    "orderNumber": "ORD-20261107-A91B4C",
    "reservedItems": [
      {
        "productId": 1,
        "quantity": 2,
        "remainingStock": 48
      },
      {
        "productId": 5,
        "quantity": 1,
        "remainingStock": 199
      }
    ]
  }
}
```

#### Errors
- `400 Bad Request` (`INSUFFICIENT_STOCK`):
```json
{
  "timestamp": "2026-11-07T11:00:00Z",
  "status": 400,
  "error": "INSUFFICIENT_STOCK",
  "message": "Product with ID 1 only has 1 available stock, but 2 were requested",
  "path": "/api/products/reserve"
}
```

---

### 8.2 Release Product Inventory (Compensating Transaction)
- **Endpoint**: `POST /api/products/release`
- **Access**: Internal Microservice (`Order Service`) / Gateway Verified
- **Description**: Compensating endpoint. Reverts reserved stock back to `available_stock` if payment fails, order persistence encounters a database error, or an order is cancelled.

#### Request Body
```json
{
  "orderNumber": "ORD-20261107-A91B4C",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 5,
      "quantity": 1
    }
  ]
}
```

#### Response `200 OK`
```json
{
  "success": true,
  "message": "Reserved inventory released back to available stock",
  "data": {
    "orderNumber": "ORD-20261107-A91B4C",
    "releasedItems": [
      {
        "productId": 1,
        "quantity": 2,
        "availableStock": 50
      },
      {
        "productId": 5,
        "quantity": 1,
        "availableStock": 200
      }
    ]
  }
}
```
