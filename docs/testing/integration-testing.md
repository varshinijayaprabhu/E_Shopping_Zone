# Integration Testing Strategy

## 1. Controller & Security Integration Tests (`@WebMvcTest`)

Controller integration tests verify:
1. Serialization/Deserialization of request and response DTOs.
2. JSR-380 input validation triggers (asserting HTTP 400 with expected field error list).
3. Security authorization filter responses (asserting HTTP 401 for missing tokens and HTTP 403 for unauthorized roles).

---

## 2. Spring Boot Integration Tests (`@SpringBootTest`)

Full-context integration tests:
- Spin up an embedded test environment (H2 database in MySQL compatibility mode or local MySQL test schema).
- Verify database cascade operations and repository transactions.
- Test inter-service client error decoding via `MockRestServiceServer`.

