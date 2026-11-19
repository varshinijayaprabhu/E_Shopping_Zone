# Admin Governance & Dispute Management API Contract

> [!NOTE]
> **Domain Overview & Production Governance**:
> - **Platform Governance**: The `admin-service` microservice provides centralized administrative control over marketplace operations, platform-wide KPIs, seller verification, and dispute mediation.
> - **Decoupled Architecture**: All administrative routes are mapped through the API Gateway under `/api/admin/**` (restricted strictly to `ROLE_ADMIN`) and `/api/disputes/**` (accessible to authenticated `ROLE_CUSTOMER`, `ROLE_MERCHANT`, and `ROLE_ADMIN`).
> - **SLA Enforcement**: Customer disputes feature an automated 2-minute merchant SLA countdown timer. If unhandled within 2 minutes, tickets auto-escalate to `ESCALATED_TO_ADMIN` via a background scheduler.

---

## 1. Platform Metrics & Big Numbers

- **Endpoint**: `GET /api/admin/metrics`
- **Access**: Protected (`ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Aggregates platform-wide executive metrics via internal REST API composition with `profile-service` and `order-service`.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Dashboard metrics retrieved successfully",
  "data": {
    "totalOrders": 150,
    "totalRevenue": 450000.00,
    "totalUsers": 500,
    "activeCustomers": 480,
    "registeredMerchants": 20,
    "pendingMerchants": 2,
    "pendingDisputes": 3,
    "escalatedDisputes": 1,
    "resolvedDisputes": 12
  }
}
```

---

## 2. List Registered Merchants

- **Endpoint**: `GET /api/admin/merchants`
- **Access**: Protected (`ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Returns all merchant accounts with registration timestamp, address details, and current approval/ban status.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Merchants retrieved successfully",
  "data": [
    {
      "id": 2,
      "email": "merchant@eshopping.com",
      "fullName": "Bob Merchant",
      "phoneNumber": "+91-9876543211",
      "enabled": true,
      "createdAt": "2026-11-15T10:00:00"
    }
  ]
}
```

---

## 3. Approve, Suspend, or Ban Merchant

- **Endpoint**: `PUT /api/admin/merchants/{id}/status`
- **Access**: Protected (`ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Toggles merchant account active (`enabled: true`) or suspended/frozen (`enabled: false`) status. Automatically creates an immutable entry in `admin_audit_logs`.

### Request Body
```json
{
  "enabled": false,
  "reason": "Repeated merchant SLA expiration and high dispute failure rate"
}
```
*Validation Rules*: `enabled` is mandatory (boolean); `reason` must not be blank (max 500 characters).

### Response `200 OK`
```json
{
  "success": true,
  "message": "Merchant suspended successfully",
  "data": null
}
```

---

## 4. Dispute Tickets Queue

- **Endpoint**: `GET /api/admin/disputes`
- **Access**: Protected (`ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Query Parameters**:
  - `status` *(Optional)*: `PENDING_MERCHANT`, `MERCHANT_ACCEPTED`, `MERCHANT_REJECTED`, `ESCALATED_TO_ADMIN`, `REFUNDED_BY_ADMIN`, `RESOLVED_NO_REFUND`
  - `page` *(Default: 0)*: Zero-based page index
  - `size` *(Default: 10, Max: 50)*: Number of items per page

### Response `200 OK`
```json
{
  "success": true,
  "message": "Disputes retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "orderId": 101,
        "customerId": 1,
        "merchantId": 2,
        "issueReason": "Damaged product received",
        "customerComment": "Headphones arrived with broken hinge and torn packaging.",
        "merchantResponse": null,
        "adminNotes": null,
        "refundAmount": 14999.00,
        "status": "ESCALATED_TO_ADMIN",
        "merchantDeadline": "2026-11-18T14:32:00",
        "remainingSeconds": 0,
        "escalatedAt": "2026-11-18T14:32:01",
        "resolvedAt": null,
        "createdAt": "2026-11-18T14:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

## 5. Administrative Dispute Resolution & Refund Override

- **Endpoint**: `POST /api/admin/disputes/{id}/resolve`
- **Access**: Protected (`ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Administrator manually adjudicates an escalated dispute. When `action: "REFUND"`, invokes internal Saga compensation on `order-service` to refund customer wallet and unreserve/restock items, transitioning status to `REFUNDED_BY_ADMIN`. When `action: "NO_REFUND"`, permanently closes the dispute without financial deduction.

### Request Body
```json
{
  "action": "REFUND",
  "adminNotes": "Approved full refund based on customer photo evidence verifying transit damage."
}
```
*Validation Rules*: `action` must be `REFUND` or `NO_REFUND`; `adminNotes` must not be blank.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Dispute resolved successfully",
  "data": {
    "id": 1,
    "orderId": 101,
    "status": "REFUNDED_BY_ADMIN",
    "refundAmount": 14999.00,
    "adminNotes": "Approved full refund based on customer photo evidence verifying transit damage.",
    "resolvedAt": "2026-11-18T14:35:10"
  }
}
```

---

## 6. Admin Audit Log History

- **Endpoint**: `GET /api/admin/audit-logs`
- **Access**: Protected (`ROLE_ADMIN`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Query Parameters**: `page` *(Default: 0)*, `size` *(Default: 10, Max: 50)*
- **Description**: Returns immutable chronological records of all administrative actions (merchant status changes, dispute overrides).

### Response `200 OK`
```json
{
  "success": true,
  "message": "Audit logs retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "adminId": 4,
        "adminEmail": "admin@eshopping.com",
        "actionType": "DISPUTE_RESOLUTION_OVERRIDE",
        "targetEntityType": "DISPUTE",
        "targetEntityId": 1,
        "reason": "Customer photo verified damage",
        "metadata": "{\"action\":\"REFUND\",\"amount\":14999.00}",
        "ipAddress": "127.0.0.1",
        "createdAt": "2026-11-18T14:35:10"
      }
    ],
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 7. Customer Raise Order Dispute Ticket

- **Endpoint**: `POST /api/disputes/create`
- **Access**: Protected (`ROLE_CUSTOMER` or authenticated user)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Initiates a dispute against an existing order. Automatically initializes a 2-minute merchant SLA countdown timer (`merchantDeadline = createdAt + 2 minutes`).

### Request Body
```json
{
  "orderId": 101,
  "merchantId": 2,
  "issueReason": "Damaged product received",
  "customerComment": "Headphones package arrived dented and product has visible scratches.",
  "refundAmount": 14999.00
}
```
*Validation Rules*: `orderId`, `merchantId`, and `issueReason` are mandatory; `refundAmount` must be $\ge 0.00$.

### Response `201 Created`
```json
{
  "success": true,
  "message": "Dispute raised successfully. Merchant has 2 minutes to respond.",
  "data": {
    "id": 1,
    "orderId": 101,
    "customerId": 1,
    "merchantId": 2,
    "issueReason": "Damaged product received",
    "customerComment": "Headphones package arrived dented and product has visible scratches.",
    "refundAmount": 14999.00,
    "status": "PENDING_MERCHANT",
    "merchantDeadline": "2026-11-18T14:32:00",
    "remainingSeconds": 120,
    "createdAt": "2026-11-18T14:30:00"
  }
}
```

---

## 8. Get My Disputes

- **Endpoint**: `GET /api/disputes/my`
- **Access**: Protected (Authenticated user)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Dynamically resolves the caller's role. If caller has `ROLE_MERCHANT`, returns all disputes filed against their products. If caller has `ROLE_CUSTOMER`, returns all disputes they created.

### Response `200 OK`
```json
{
  "success": true,
  "message": "My disputes retrieved successfully",
  "data": [
    {
      "id": 1,
      "orderId": 101,
      "status": "PENDING_MERCHANT",
      "issueReason": "Damaged product received",
      "refundAmount": 14999.00,
      "remainingSeconds": 85,
      "createdAt": "2026-11-18T14:30:00"
    }
  ]
}
```

---

## 9. Merchant Dispute Response

- **Endpoint**: `POST /api/disputes/{id}/merchant-response`
- **Access**: Protected (`ROLE_MERCHANT`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Merchant responds to an active dispute ticket before SLA expiration.
  - If `action: "ACCEPT"`: Auto-triggers customer refund via order service Saga; ticket transitions to `MERCHANT_ACCEPTED`.
  - If `action: "REJECT"`: Ticket immediately escalates to `ESCALATED_TO_ADMIN` for administrative arbitration.

### Request Body
```json
{
  "action": "ACCEPT",
  "responseComment": "We apologize for the damaged package. Full refund approved."
}
```
*Validation Rules*: `action` must be `ACCEPT` or `REJECT`; `responseComment` must not be blank.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Dispute response submitted successfully",
  "data": {
    "id": 1,
    "orderId": 101,
    "status": "MERCHANT_ACCEPTED",
    "merchantResponse": "We apologize for the damaged package. Full refund approved.",
    "resolvedAt": "2026-11-18T14:31:15"
  }
}
```

---

## 10. Get Dispute Ticket Details

- **Endpoint**: `GET /api/disputes/{id}`
- **Access**: Protected (Authenticated user)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Retrieves full dispute ticket details, including live remaining SLA seconds (`remainingSeconds`) calculated dynamically against server clock.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Dispute details retrieved successfully",
  "data": {
    "id": 1,
    "orderId": 101,
    "customerId": 1,
    "merchantId": 2,
    "issueReason": "Damaged product received",
    "customerComment": "Headphones package arrived dented and product has visible scratches.",
    "merchantResponse": null,
    "adminNotes": null,
    "refundAmount": 14999.00,
    "status": "PENDING_MERCHANT",
    "merchantDeadline": "2026-11-18T14:32:00",
    "remainingSeconds": 75,
    "createdAt": "2026-11-18T14:30:00"
  }
}
```
