# JWT Access Token & Refresh Token Specification

## 1. Access Token Specification

- **Algorithm**: HMAC-SHA256 (`HS256`).
- **Signature Secret**: Minimum 256-bit high-entropy secret passed via environment variable `JWT_SECRET`.
- **Expiration Window**: 15 minutes (`900` seconds).
- **Format**: Standard 3-part base64url encoded string (`header.payload.signature`).

### Token Payload Structure
```json
{
  "sub": "1",
  "email": "customer@eshopping.com",
  "fullName": "Alice Customer",
  "roles": ["ROLE_CUSTOMER"],
  "iss": "eshopping-zone-profile-service",
  "aud": "eshopping-zone-api-gateway",
  "iat": 1730973600,
  "exp": 1730974500
}
```

---

## 2. Refresh Token Specification

- **Entropy Source**: Cryptographically secure pseudo-random number generator (`java.security.SecureRandom`).
- **Token Format**: Base64url encoded 32-byte string (256 bits of entropy).
- **Expiration Window**: 7 days (`604800` seconds).
- **Transport**: Standard HTTP Cookie (`Set-Cookie`).
  - `Name`: `eshopping_refresh_token`
  - `HttpOnly`: `true` (Inaccessible to `document.cookie`).
  - `SameSite`: `Lax` (Protects against CSRF while permitting top-level navigation).
  - `Path`: `/api/auth` (Restricts transmission strictly to authentication renewal routes).
  - `Secure`: `false` for localhost development, `true` when deployed on HTTPS.

---

## 3. Database Persistence & Hashing

Refresh tokens are never stored in plaintext within MySQL. When a refresh token is generated:
1. `rawToken = Base64UrlEncode(SecureRandom(32 bytes))`
2. `hash = SHA256(rawToken)`
3. Save `hash`, `userId`, `expiryDate`, `revoked=false`, `parentTokenHash` into `profile_db.refresh_tokens`.
4. Transmit `rawToken` to the user via the `Set-Cookie` header.

When the user calls `/api/auth/refresh`:
1. Extract `rawToken` from cookie.
2. Compute `hash = SHA256(rawToken)`.
3. Query `profile_db.refresh_tokens` by `token_hash = hash`.

