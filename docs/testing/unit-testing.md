# Unit Testing Guidelines & Patterns

## 1. Unit Testing Philosophy

Unit tests must test **business behavior** rather than trivial getter/setter mechanics or framework wiring. All external dependencies (repositories, RestTemplate clients) are mocked using Mockito.

---

## 2. Key Unit Test Scenarios per Service

### 2.1 Profile Service
- `AuthServiceTest`:
  - `registerUser_Success`: Verifies password hashing with BCrypt and role mapping.
  - `registerUser_DuplicateEmail`: Asserts `EmailAlreadyExistsException`.
  - `login_Success`: Verifies JWT and refresh token generation.
  - `login_InvalidPassword`: Asserts `BadCredentialsException`.
  - `refreshToken_Success`: Verifies token rotation (old revoked, new hash saved).
  - `refreshToken_ReplayAttack`: Asserts session revocation upon presenting already-revoked token.

### 2.2 Product Service
- `ProductServiceTest`:
  - `createProduct_Success`: Saves product with inventory record.
  - `updateProduct_MerchantOwnershipSuccess`: Updates allowed when `merchantId` matches.
  - `updateProduct_ForbiddenForNonOwner`: Asserts `AccessDeniedException` when `merchantId` differs.
  - `reserveStock_Success`: Decrements available stock and increments reserved stock.
  - `reserveStock_InsufficientStock`: Asserts `InsufficientStockException`.

### 2.3 Cart Service
- `CartServiceTest`:
  - `addItem_Success`: Fetches price from Product Service mock and computes total.
  - `addItem_ExceedsStock`: Asserts validation failure.
  - `calculateCartTotal`: Verifies monetary sum calculation with `BigDecimal`.

### 2.4 Wallet Service
- `WalletServiceTest`:
  - `deposit_Success`: Credits balance and creates `DEPOSIT` transaction record.
  - `payment_Success`: Debits balance atomically under lock.
  - `payment_InsufficientFunds`: Asserts `InsufficientFundsException`.
  - `payment_Idempotent`: Returns existing transaction without second deduction.

### 2.5 Order Service
- `OrderCheckoutSagaTest`:
  - `checkout_WalletSuccess`: Coordinates reservation, deduction, order creation, cart clear.
  - `checkout_WalletInsufficientBalance_CompensatesInventory`: Asserts inventory release call.
  - `checkout_CodSuccess`: Skips wallet deduction, sets payment to `PENDING`.

