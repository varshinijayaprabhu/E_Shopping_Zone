# Security Best Practices & Secure Coding Standards

## 1. Principles of Least Privilege

1. **Service-to-Database Isolation**: Each microservice is provisioned with its own distinct MySQL user grant restricted strictly to its own schema.
2. **Minimal Data Exposure in DTOs**: JPA entities are never exposed across the REST boundary. DTOs intentionally omit internal database primary keys where UUIDs are preferable, and never expose audit flags, hashed passwords, or internal parent tokens.
3. **Fail-Close Architecture**: When authentication or authorization evaluation encounters unexpected state (such as null claims or broken token signatures), the system immediately fails closed and denies access (`401 Unauthorized` or `403 Forbidden`).

---

## 2. Secure Web Frontend Guidelines

In alignment with mandatory frontend security rules:
- **No `dangerouslySetInnerHTML`**: All dynamic data in React is rendered using standard JSX interpolation (`{product.name}`), ensuring native browser DOM escaping.
- **Sanitized Links**: URLs displayed in `<a>` tags or `<img>` attributes are strictly validated against an approved protocol allowlist (`http:`, `https:`) to prevent `javascript:` pseudoprotocol XSS.
- **Strict In-Memory Token Management**: Access tokens are stored in React application memory via Context. At no point is an access token or refresh token stored in `window.localStorage` or `window.sessionStorage`.
- **Full Client Session Purge on Logout**: Upon logout, the client application executes a state reset, clears in-memory user objects, calls the backend logout endpoint to clear cookies, and redirects the window to `/login`.

---

## 3. Input Validation & Defense Against Injection

- **JSR-380 Bean Validation**: Every input DTO is annotated with `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, or `@Email`.
- **SQL Injection Prevention**: All persistence operations utilize Spring Data JPA repositories with parameterized queries or method name generation. No raw SQL string concatenation is permitted.
- **Safe Command / File Handling**: No dynamic shell execution or arbitrary file path manipulation is utilized.

