# Cart API Contract

## 1. Retrieve Current Customer Cart

- **Endpoint**: `GET /api/cart`
- **Access**: Protected (`ROLE_CUSTOMER`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Fetches the customer's active cart. For every cart item, Cart Service queries Product Service in real time to fetch authoritative prices, names, images, and verifies stock availability.

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "cartId": 1,
    "customerId": 1,
    "items": [
      {
        "itemId": 10,
        "productId": 1,
        "productName": "Pro Wireless Noise-Cancelling Headphones",
        "productSku": "ELEC-HP-001",
        "unitPrice": 14999.00,
        "imageUrl": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500",
        "quantity": 2,
        "itemSubtotal": 29998.00,
        "inStock": true,
        "availableStock": 50
      }
    ],
    "totalItems": 2,
    "cartSubtotal": 29998.00
  }
}
```

---

## 2. Add Item to Cart

- **Endpoint**: `POST /api/cart/items`
- **Access**: Protected (`ROLE_CUSTOMER`)
- **Description**: Adds a product to the cart or increments quantity if already present.

### Request Body
```json
{
  "productId": 1,
  "quantity": 1
}
```
*Validation Rules*: `productId` must be positive; `quantity` must be between 1 and 99.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Item added to cart",
  "data": {
    "cartId": 1,
    "items": [ ... ],
    "cartSubtotal": 29998.00
  }
}
```

### Errors
- `400 Bad Request` (`INSUFFICIENT_STOCK`): Requested quantity exceeds available product stock.
- `404 Not Found` (`PRODUCT_NOT_FOUND`): Product ID does not exist or is inactive.

---

## 3. Update Cart Item Quantity

- **Endpoint**: `PUT /api/cart/items/{itemId}`
- **Access**: Protected (`ROLE_CUSTOMER`)

### Request Body
```json
{
  "quantity": 3
}
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Cart item quantity updated",
  "data": { ... }
}
```

---

## 4. Remove Item from Cart

- **Endpoint**: `DELETE /api/cart/items/{itemId}`
- **Access**: Protected (`ROLE_CUSTOMER`)

### Response `200 OK`
```json
{
  "success": true,
  "message": "Item removed from cart",
  "data": { ... }
}
```

---

## 5. Clear Entire Cart

- **Endpoint**: `DELETE /api/cart`
- **Access**: Protected (`ROLE_CUSTOMER`)

### Response `204 No Content`
*(Empty response body)*

