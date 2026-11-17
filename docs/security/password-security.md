# Password Security & Credentials Policy

## 1. Password Hashing Algorithm

E-ShoppingZone enforces password hashing via **BCrypt** with an adaptive work factor of 10 (`BCryptPasswordEncoder(10)`).

### Security Characteristics:
- **Salt Generation**: Automatically generates a unique cryptographically random 128-bit salt for every password hashed.
- **Timing Safe**: Password verification runs in constant time relative to password length, eliminating timing side-channel attacks.
- **Work Factor (Cost 10)**: Strikes an optimal balance between server latency (~80ms per hash on modern CPUs) and resistance to GPU-accelerated brute-force cracking.

---

## 2. Password Strength Policy

Passwords submitted during registration (`POST /api/auth/register`) or profile password reset must satisfy the following validation rules:
- **Minimum Length**: 8 characters.
- **Maximum Length**: 64 characters (prevents denial-of-service via oversized hashing buffers).
- **Complexity Requirements**:
  - At least 1 uppercase letter (`A-Z`).
  - At least 1 lowercase letter (`a-z`).
  - At least 1 numeric digit (`0-9`).
  - At least 1 special character (`!@#$%^&*()_+-=[]{}|;:,.<>?`).

---

## 3. Storage and Logging Hygiene

1. **Zero Plaintext Storage**: Plaintext passwords exist in memory only during initial authentication or hashing; they are never persisted or stored in cache.
2. **Log Sanitization**: Spring Boot Logback filters redact or suppress any request payload containing `password`, `accessToken`, or `refreshToken`.
3. **No Credential Echoing**: Authentication API responses never return password hashes or raw passwords in response payloads.

