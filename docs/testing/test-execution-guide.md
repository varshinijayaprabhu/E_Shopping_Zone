# Test Execution & Verification Guide

## 1. Running Unit & Integration Tests via Maven

Execute tests across all backend microservice modules simultaneously from the `backend/` root directory:

```bash
cd backend
mvn test
```

To run tests for an individual microservice (e.g. `order-service`):
```bash
cd backend/order-service
mvn test
```

---

## 2. Test Execution Flags

- **Skip Tests during quick packaging**:
  ```bash
  mvn clean package -DskipTests
  ```
- **Run a specific test class**:
  ```bash
  mvn test -Dtest=OrderCheckoutSagaTest
  ```
- **Generate Surefire XML / HTML Test Reports**:
  ```bash
  mvn surefire-report:report
  ```
  Reports are generated at `target/site/surefire-report.html` within each service module.

