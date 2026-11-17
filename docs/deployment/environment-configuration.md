# Environment Configuration Reference

## 1. Local Development (`application-dev.yml`)

By default, services activate the `dev` profile with verbose SQL logging and fast reconnects:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    com.eshopping: DEBUG
    org.springframework.security: INFO
```

---

## 2. Production Hardening (`application-prod.yml`)

For production environments:
1. `spring.jpa.show-sql: false` (Prevents leaking SQL parameters to logs).
2. `server.ssl.enabled: true` (Enforces TLS 1.3 encryption in transit).
3. `eshopping.security.cookie.secure: true` (Enforces HTTPS on refresh token cookies).
4. `management.endpoints.web.exposure.include: health,info,metrics` (Restricts Actuator endpoints).

