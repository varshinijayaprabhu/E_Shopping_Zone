# Role-Based Access Control (RBAC) Specification

## 1. Roles and Permissions Matrix

E-ShoppingZone defines two distinct system roles:
1. `ROLE_CUSTOMER`: Standard shoppers.
2. `ROLE_MERCHANT`: Sellers with product catalog and inventory ownership.

| API Operation | Method | Route | `ROLE_CUSTOMER` | `ROLE_MERCHANT` | Anonymous |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **Browse / Search Products** | `GET` | `/api/products/**` | Allowed | Allowed | Allowed |
| **Browse Categories** | `GET` | `/api/categories/**` | Allowed | Allowed | Allowed |
| **User Registration / Login** | `POST` | `/api/auth/**` | Allowed | Allowed | Allowed |
| **Manage Own Profile** | `GET, PUT`| `/api/profile` | Allowed | Allowed | Denied |
| **Manage Shopping Cart** | `*` | `/api/cart/**` | Allowed | Denied | Denied |
| **Checkout & Place Orders** | `POST` | `/api/orders/checkout` | Allowed | Denied | Denied |
| **View Personal Orders** | `GET` | `/api/orders` | Allowed | Denied | Denied |
| **Manage Digital Wallet** | `*` | `/api/wallet/**` | Allowed | Denied | Denied |
| **Create Catalog Product** | `POST` | `/api/products` | Denied | Allowed | Denied |
| **Update Own Product** | `PUT` | `/api/products/{id}` | Denied | Allowed (Owner only) | Denied |
| **Delete Own Product** | `DELETE`| `/api/products/{id}` | Denied | Allowed (Owner only) | Denied |
| **View Merchant Orders** | `GET` | `/api/orders/merchant` | Denied | Allowed | Denied |

---

## 2. Resource Ownership Enforcement (IDOR Protection)

In addition to role checks (`@PreAuthorize("hasRole('MERCHANT')")`), microservices enforce granular resource ownership:

```java
@Transactional
public ProductResponseDTO updateProduct(Long productId, ProductUpdateRequestDTO request, Long merchantId) {
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
            
    if (!product.getMerchantId().equals(merchantId)) {
        throw new AccessDeniedException("Merchant " + merchantId + " is not authorized to edit product " + productId);
    }
    
    // Apply updates...
}
```
This ensures a merchant cannot tamper with, update stock for, or delete products belonging to another merchant.

