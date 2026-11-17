# Authentication API Contract

## 1. Register Account

- **Endpoint**: `POST /api/auth/register`
- **Access**: Public
- **Description**: Registers a new customer account. Public registration strictly provisions `ROLE_CUSTOMER`. Users cannot self-assign `ROLE_MERCHANT`. Prevents duplicate email registration and hashes password with BCrypt.

> [!IMPORTANT]
> **Controlled Merchant Provisioning**:
> Public self-registration for `ROLE_MERCHANT` is forbidden to prevent unauthorized merchant privilege escalation. Merchant accounts are provisioned via:
> 1. Dedicated invite-token endpoint: `POST /api/auth/register-merchant` (requires valid administrative `merchantInviteCode`).
> 2. System database seeding for demo merchant accounts (`merchant@eshopping.com`).

### Request Body (`POST /api/auth/register` - Customer)
```json
{
  "email": "jane.customer@example.com",
  "password": "Password@123",
  "fullName": "Jane Customer",
  "phoneNumber": "+91-9876543210"
}
```
*Validation Rules*:
- `email`: Non-blank, valid email regex.
- `password`: Non-blank, min 8 chars, max 64 chars, containing at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special char.
- `fullName`: Non-blank, min 2, max 100 chars.
- *Note*: Any submitted `role` field in public registration is ignored; the server unconditionally enforces `ROLE_CUSTOMER`.

### Response `201 Created`
```json
{
  "success": true,
  "message": "Customer registered successfully",
  "data": {
    "userId": 4,
    "email": "jane.customer@example.com",
    "fullName": "Jane Customer",
    "roles": ["ROLE_CUSTOMER"]
  }
}
```

---

## 2. Register Merchant Account (Controlled Provisioning)

- **Endpoint**: `POST /api/auth/register-merchant`
- **Access**: Controlled (Requires valid administrative invitation secret)
- **Description**: Registers a verified merchant account.

### Request Body
```json
{
  "email": "merchant.seller@example.com",
  "password": "Password@123",
  "fullName": "Seller Enterprise",
  "phoneNumber": "+91-9876543219",
  "merchantInviteCode": "ESHOPPING_MERCHANT_INVITE_2026"
}
```

### Response `201 Created`
```json
{
  "success": true,
  "message": "Merchant registered and provisioned successfully",
  "data": {
    "userId": 5,
    "email": "merchant.seller@example.com",
    "fullName": "Seller Enterprise",
    "roles": ["ROLE_MERCHANT"]
  }
}
```

### Errors
- `400 Bad Request` (`INVALID_INVITE_CODE`): Merchant invite code is missing, invalid, or expired.
- `409 Conflict` (`EMAIL_ALREADY_EXISTS`): User with email is already registered.

---

## 3. Login (Local Credentials)

- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Description**: Authenticates credentials, generates short-lived JWT access token, creates secure opaque refresh token in DB, and returns refresh token via `Set-Cookie`.

### Request Body
```json
{
  "email": "customer@eshopping.com",
  "password": "Password@123"
}
```

### Response `200 OK`
- **Headers**:
  - `Set-Cookie: eshopping_refresh_token=1a89c9f0...; Path=/api/auth; HttpOnly; SameSite=Lax; Max-Age=604800`
- **Payload**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 1,
      "email": "customer@eshopping.com",
      "fullName": "Alice Customer",
      "roles": ["ROLE_CUSTOMER"]
    }
  }
}
```

### Errors
- `401 Unauthorized` (`INVALID_CREDENTIALS`): Bad email or password.
- `403 Forbidden` (`ACCOUNT_DISABLED`): User account is deactivated.

---

## 4. Refresh Access Token (Token Rotation with CSRF Protection)

- **Endpoint**: `POST /api/auth/refresh`
- **Access**: Public (Requires valid `eshopping_refresh_token` cookie + Anti-CSRF Header)
- **Description**: Validates refresh token hash, invalidates used token, issues a new refresh token (RTR), and issues a new access token.

### Request Headers
- `Cookie: eshopping_refresh_token=1a89c9f0...`
- `X-Requested-With: XMLHttpRequest` *(Mandatory Anti-CSRF Header)*
- `Origin: http://localhost:5173` *(Strictly validated against allowed origins)*

### Response `200 OK`
- **Headers**:
  - `Set-Cookie: eshopping_refresh_token=new_token_hash_here...; Path=/api/auth; HttpOnly; SameSite=Lax; Max-Age=604800`
- **Payload**:
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

### Errors
- `403 Forbidden` (`CSRF_VIOLATION`): Missing `X-Requested-With` header or disallowed Origin/Referer.
- `401 Unauthorized` (`REFRESH_TOKEN_EXPIRED`): Expired refresh token.
- `401 Unauthorized` (`REFRESH_TOKEN_REUSED`): Detected replay attack; all user sessions revoked.

---

## 5. Logout (Current Session)

- **Endpoint**: `POST /api/auth/logout`
- **Access**: Authenticated
- **Headers**: `X-Requested-With: XMLHttpRequest`
- **Description**: Revokes the specific refresh token and clears the `eshopping_refresh_token` cookie.

### Response `200 OK`
- **Headers**:
  - `Set-Cookie: eshopping_refresh_token=; Path=/api/auth; Max-Age=0`
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

## 6. Logout All (All Sessions)

- **Endpoint**: `POST /api/auth/logout-all`
- **Access**: Authenticated (`ROLE_CUSTOMER`, `ROLE_MERCHANT`)
- **Description**: Revokes every active refresh token belonging to the authenticated user ID across all browsers/devices.

### Response `200 OK`
```json
{
  "success": true,
  "message": "All user sessions invalidated successfully"
}
```
