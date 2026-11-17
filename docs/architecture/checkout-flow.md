# Checkout & Distributed State-Machine Saga Specification

## 1. Overview & Problem Definition

In an e-commerce microservices architecture, placing an order spans across four separate services and databases:
1. **Cart Service (`cart_db`)**: Retrieves active items and verifies cart state.
2. **Product Service (`product_db`)**: Reserves product inventory to guarantee availability.
3. **Wallet Service (`wallet_db`)**: Deducts monetary funds when the user chooses `WALLET` payment mode.
4. **Order Service (`order_db`)**: Persists the order master, snapshot line items, and payment status.

### The Crash Vulnerability Problem
A naive Saga that reserves stock, deducts funds, and *then* inserts the order record is vulnerable to JVM crashes or network partition:
> *If the Order Service process crashes or loses database connectivity after the wallet deduction succeeds but before saving the order, the customer's funds would be debited without an order ever being recorded in the database.*

### The Solution: Pre-Persistence State-Machine Saga & Idempotent Reconciliation
E-ShoppingZone resolves this with a **Pre-Persistence State-Machine Saga** paired with **Idempotency** and **Automated Reconciliation**:
1. **Order Pre-Persistence**: The Order record is saved in `order_db` in `PAYMENT_PENDING` status *prior* to contacting external payment services.
2. **Idempotent Retry Resolution**: If a crash occurs and the client retries, Order Service detects the existing pending order, queries Wallet Service for payment status, and safely completes the confirmation.
3. **Automated Background Reconciliation**: A scheduled reconciler inspects stale `PAYMENT_PENDING` orders, reconciling payments or rolling back stock reservations.

---

## 2. Robust State-Machine Checkout Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Customer (UI)
    participant OrderSvc as Order Service (Orchestrator)
    participant OrderDB as order_db
    participant CartSvc as Cart Service
    participant ProductSvc as Product Service
    participant WalletSvc as Wallet Service

    Customer->>OrderSvc: POST /api/orders/checkout<br/>{paymentMethod: "WALLET"|"COD", shippingAddress, idempotencyKey}

    Note over OrderSvc, OrderDB: Step 1: Idempotency Check & Pre-Persistence
    OrderSvc->>OrderDB: Query order by idempotency_key
    alt Order already exists
        OrderSvc->>OrderSvc: Handle Idempotent Retry Flow (See Section 3)
    else New Order
        OrderSvc->>CartSvc: GET /api/cart
        CartSvc-->>OrderSvc: 200 OK (Cart items with live prices)
        Note over OrderSvc, OrderDB: Persist Order in PAYMENT_PENDING state BEFORE money changes hands!
        OrderSvc->>OrderDB: INSERT INTO orders (order_number, idempotency_key, status: 'PAYMENT_PENDING', total_amount...)<br/>INSERT INTO order_items (snapshot names, snapshot unit prices...)
    end

    Note over OrderSvc, ProductSvc: Step 2: Reserve Product Stock
    OrderSvc->>ProductSvc: POST /api/products/reserve<br/>{orderNumber, items: [{productId, quantity}]}
    alt Inventory Insufficient
        ProductSvc-->>OrderSvc: 400 Bad Request (INSUFFICIENT_STOCK)
        OrderSvc->>OrderDB: UPDATE orders SET status = 'FAILED'
        OrderSvc-->>Customer: 400 Bad Request (OUT_OF_STOCK)
    end
    ProductSvc-->>OrderSvc: 200 OK (Stock reserved)

    Note over OrderSvc, WalletSvc: Step 3: Payment Processing
    alt Payment Method == "WALLET"
        OrderSvc->>WalletSvc: POST /api/wallet/payment<br/>{amount, orderReference: orderNumber, idempotencyKey: orderNumber + "-pay"}
        alt Wallet Balance Insufficient / Payment Failed
            WalletSvc-->>OrderSvc: 400 Bad Request (INSUFFICIENT_FUNDS)
            Note over OrderSvc, ProductSvc: Compensation: Release Reserved Stock
            OrderSvc->>ProductSvc: POST /api/products/release {orderNumber, items}
            OrderSvc->>OrderDB: UPDATE orders SET status = 'FAILED', payment_status = 'FAILED'
            OrderSvc-->>Customer: 400 Bad Request (PAYMENT_FAILED_INSUFFICIENT_FUNDS)
        else Wallet Payment Success
            WalletSvc-->>OrderSvc: 200 OK (TransactionReference)
        end
    else Payment Method == "COD"
        Note over OrderSvc: Cash on Delivery selected; Payment status remains PENDING
    end

    Note over OrderSvc, OrderDB: Step 4: Confirm Order
    OrderSvc->>OrderDB: UPDATE orders SET status = 'CONFIRMED', payment_status = (WALLET ? 'SUCCESS' : 'PENDING')
    
    Note over OrderSvc, CartSvc: Step 5: Post-Order Cart Cleanup
    OrderSvc->>CartSvc: DELETE /api/cart
    CartSvc-->>OrderSvc: 204 No Content

    OrderSvc-->>Customer: 201 Created (OrderDetailsDTO: id, orderNumber, status: CONFIRMED)
```

---

## 3. Crash Recovery & Reconciliation Scenarios

### 3.1 Scenario: Order Service Crashes After Wallet Deduction
1. **The State When Crash Occurs**:
   - `order_db`: Contains Order `ORD-101` in `PAYMENT_PENDING`.
   - `product_db`: Inventory reserved for `ORD-101`.
   - `wallet_db`: Money successfully deducted under transaction `TXN-PAY-ORD-101`.
   - Client: Receives a network dropped connection / 504.
2. **Client Automatic Retry Flow**:
   - The user or React UI retries `POST /api/orders/checkout` with the same `idempotencyKey`.
   - Order Service finds existing order `ORD-101` with status `PAYMENT_PENDING`.
   - Order Service issues a reconciliation query: `GET /api/wallet/payment/order/ORD-101`.
   - Wallet Service returns `200 OK (status: SUCCESS)`.
   - Order Service marks `ORD-101` as `CONFIRMED`, triggers cart cleanup, and returns `200 OK` with the confirmed order.
   - **Result**: No duplicate deduction occurs, and the user receives their confirmed order cleanly!

### 3.2 Scenario: Background Reconciliation Worker (`OrderReconciliationScheduler`)
For orders where the user never retries:
```java
@Scheduled(fixedDelay = 60000) // Runs every 60 seconds
public void reconcilePendingOrders() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(3);
    List<Order> pendingOrders = orderRepository.findByOrderStatusAndCreatedAtBefore(OrderStatus.PAYMENT_PENDING, cutoff);

    for (Order order : pendingOrders) {
        if (PaymentMethod.WALLET.equals(order.getPaymentMethod())) {
            PaymentStatusResponseDTO paymentStatus = walletClient.queryPaymentStatus(order.getOrderNumber());
            if (paymentStatus != null && "SUCCESS".equals(paymentStatus.getStatus())) {
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                orderRepository.save(order);
                cartClient.clearCart(order.getCustomerId());
            } else {
                // Payment was never made: abort order and release stock
                productClient.releaseStock(order.getOrderNumber(), mapItems(order));
                order.setOrderStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            }
        }
    }
}
```

---

## 4. Historical Price & Product Snapshotting

To ensure order history remains immutable regardless of future merchant catalog edits:
1. `OrderItem` entities physically store:
   - `product_id`: Identifier reference.
   - `product_name_snapshot`: Exact product name at checkout time.
   - `unit_price_snapshot`: Authoritative unit price at checkout time.
   - `quantity`: Exact quantity purchased.
   - `subtotal`: `unit_price_snapshot * quantity`.
2. Total order price is mathematically computed as the sum of line item snapshots plus any applicable delivery charges, guaranteed immutable once confirmed.
