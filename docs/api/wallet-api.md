# Wallet API Contract

> [!NOTE]
> **Case Study Alignment & Additional Production Features**:
> - **Primary Specification Mandate**: The original Capgemini case study specifies that users can *"make payment through various modes like wallet payment and cash on delivery"* and *"view their previous orders and transactions"*.
> - **Deposit & Top-up**: In the baseline case study, demo wallets are pre-funded (e.g. ₹10,000 in seed data). In this architecture, wallet deposit (`POST /api/wallet/deposit`) is provided as an additional production feature simulating instantaneous top-up via a mock payment gateway/UPI/card.
> - **Bank Withdrawal**: Wallet withdrawal (`POST /api/wallet/withdraw`) is marked as an **Extended Architectural Feature**, enabling full lifecycle simulation of a real-world digital wallet.

---

## 1. Get Wallet Balance

- **Endpoint**: `GET /api/wallet/balance`
- **Access**: Protected (`ROLE_CUSTOMER`)
- **Headers**: `Authorization: Bearer <accessToken>`
- **Description**: Returns the customer's current balance and status. Auto-creates a new zero-balance wallet if one does not exist yet.

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "walletId": 1,
    "customerId": 1,
    "currentBalance": 10000.00,
    "currency": "INR",
    "status": "ACTIVE"
  }
}
```

---

## 2. Deposit Funds into Wallet (Simulated Top-up Feature)

- **Endpoint**: `POST /api/wallet/deposit`
- **Access**: Protected (`ROLE_CUSTOMER`)
- **Description**: Credits the user's digital wallet balance. Simulates immediate success from a mock payment gateway/UPI provider.

### Request Body
```json
{
  "amount": 2500.00,
  "idempotencyKey": "dep-99887766-5544-3322-1100-aabbccddeeff",
  "fundingSource": "SIMULATED_UPI",
  "description": "UPI Top-up via demo UPI"
}
```
*Validation Rules*: `amount` must be greater than `0.00` and up to `100000.00`.

### Response `200 OK`
```json
{
  "success": true,
  "message": "Deposit successful",
  "data": {
    "transactionReference": "TXN-DEP-20261107-101",
    "transactionType": "DEPOSIT",
    "amount": 2500.00,
    "balanceBefore": 10000.00,
    "balanceAfter": 12500.00,
    "status": "SUCCESS",
    "createdAt": "2026-11-07T11:00:00Z"
  }
}
```

---

## 3. Withdraw Funds from Wallet (Extended Production Feature)

- **Endpoint**: `POST /api/wallet/withdraw`
- **Access**: Protected (`ROLE_CUSTOMER`)
- **Description**: Debits funds from the digital wallet to a simulated external bank account.

### Request Body
```json
{
  "amount": 1000.00,
  "idempotencyKey": "wth-11223344-5566-7788-9900-ffeeddccbbaa",
  "bankAccountNumber": "987654321098",
  "ifscCode": "HDFC0001234"
}
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Withdrawal processed",
  "data": {
    "transactionReference": "TXN-WTH-20261107-202",
    "amount": 1000.00,
    "balanceBefore": 12500.00,
    "balanceAfter": 11500.00,
    "status": "SUCCESS"
  }
}
```

### Errors
- `400 Bad Request` (`INSUFFICIENT_FUNDS`): Requested withdrawal amount exceeds available balance.

---

## 4. Payment Deduction (Invoked during Checkout)

- **Endpoint**: `POST /api/wallet/payment`
- **Access**: Protected (`ROLE_CUSTOMER` or Internal Gateway/Order Service token)
- **Description**: Idempotently debits wallet balance under a pessimistic row lock (`SELECT ... FOR UPDATE`).

### Request Body
```json
{
  "amount": 14999.00,
  "orderReference": "ORD-20261107-A91B4C",
  "idempotencyKey": "pay-ord-20261107-a91b4c"
}
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Payment deducted successfully",
  "data": {
    "transactionReference": "TXN-PAY-20261107-303",
    "amount": 14999.00,
    "status": "SUCCESS"
  }
}
```

---

## 5. Refund (Compensating Transaction)

- **Endpoint**: `POST /api/wallet/refund`
- **Access**: Protected / Internal Service
- **Description**: Re-credits funds back to the customer's wallet if order processing fails or an order is cancelled.

### Request Body
```json
{
  "paymentTransactionReference": "TXN-PAY-20261107-303",
  "amount": 14999.00,
  "reason": "ORDER_CANCELLED",
  "idempotencyKey": "ref-ord-20261107-a91b4c"
}
```

### Response `200 OK`
```json
{
  "success": true,
  "message": "Refund credited successfully",
  "data": {
    "transactionReference": "TXN-REF-20261107-404",
    "amount": 14999.00,
    "status": "SUCCESS"
  }
}
```

---

## 6. Query Payment Status by Order Reference (Crash Reconciliation)

- **Endpoint**: `GET /api/wallet/payment/order/{orderReference}`
- **Access**: Internal Microservice / Protected
- **Description**: Used by Order Service during crash recovery and retry to check if a payment deduction actually succeeded before taking compensating actions.

### Response `200 OK`
```json
{
  "success": true,
  "data": {
    "transactionReference": "TXN-PAY-20261107-303",
    "orderReference": "ORD-20261107-A91B4C",
    "amount": 14999.00,
    "status": "SUCCESS",
    "createdAt": "2026-11-07T10:45:00Z"
  }
}
```

---

## 7. List Wallet Transactions (Transaction Ledger)

- **Endpoint**: `GET /api/wallet/transactions`
- **Access**: Protected (`ROLE_CUSTOMER`)
- **Query Parameters**: `page` (default `0`), `size` (default `10`), `type` (optional filter).

### Response `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "transactionReference": "TXN-PAY-20261107-303",
      "transactionType": "PAYMENT",
      "amount": 14999.00,
      "balanceBefore": 15000.00,
      "balanceAfter": 1.00,
      "status": "SUCCESS",
      "description": "Payment for order ORD-20261107-A91B4C",
      "createdAt": "2026-11-07T10:45:00Z"
    }
  ],
  "pagination": { ... }
}
```
