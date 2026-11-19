# Role-Based Access Control (RBAC) Specification

## 1. Roles and Permissions Matrix

E-ShoppingZone defines three distinct system roles:
1. `ROLE_CUSTOMER`: Standard shoppers with personal cart, orders, and wallet privileges.
2. `ROLE_MERCHANT`: Sellers with product catalog and inventory management ownership.
3. `ROLE_ADMIN`: Platform operators with platform KPIs, seller approvals/bans, dispute arbitration, and audit privileges.

| API Operation | Method | Route | `ROLE_CUSTOMER` | `ROLE_MERCHANT` | `ROLE_ADMIN` | Anonymous |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| **Browse / Search Products** | `GET` | `/api/products/**` | Allowed | Allowed | Allowed | Allowed |
| **Browse Categories** | `GET` | `/api/categories/**` | Allowed | Allowed | Allowed | Allowed |
| **User Registration / Login** | `POST` | `/api/auth/**` | Allowed | Allowed | Allowed | Allowed |
| **Manage Own Profile** | `GET, PUT`| `/api/profile` | Allowed | Allowed | Allowed | Denied |
| **Manage Shopping Cart** | `*` | `/api/cart/**` | Allowed | Denied | Denied | Denied |
| **Checkout & Place Orders** | `POST` | `/api/orders/checkout` | Allowed | Denied | Denied | Denied |
| **View Personal Orders** | `GET` | `/api/orders` | Allowed | Denied | Denied | Denied |
| **Manage Digital Wallet** | `*` | `/api/wallet/**` | Allowed | Denied | Denied | Denied |
| **Create Catalog Product** | `POST` | `/api/products` | Denied | Allowed | Denied | Denied |
| **Update Own Product** | `PUT` | `/api/products/{id}` | Denied | Allowed (Owner only) | Denied | Denied |
| **Delete Own Product** | `DELETE`| `/api/products/{id}` | Denied | Allowed (Owner only) | Denied | Denied |
| **View Merchant Orders** | `GET` | `/api/orders/merchant` | Denied | Allowed | Denied | Denied |
| **Platform Metrics / Big Numbers** | `GET` | `/api/admin/metrics` | Denied | Denied | Allowed | Denied |
| **Approve / Ban Merchant** | `PUT` | `/api/admin/merchants/{id}/status` | Denied | Denied | Allowed | Denied |
| **Raise Order Dispute** | `POST` | `/api/disputes/create` | Allowed | Denied | Denied | Denied |
| **Respond to Dispute (Accept/Reject)** | `POST` | `/api/disputes/{id}/merchant-response` | Denied | Allowed | Denied | Denied |
| **Admin Dispute Refund Override** | `POST` | `/api/admin/disputes/{id}/resolve` | Denied | Denied | Allowed | Denied |
| **View Admin Audit Logs** | `GET` | `/api/admin/audit-logs` | Denied | Denied | Allowed | Denied |

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

