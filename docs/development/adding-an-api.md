# Adding a New API Endpoint Guide

## 1. Overview

Adding an API to an existing microservice requires strict adherence to the DTO pattern, validation constraints, OpenAPI documentation, and exception handling.

---

## 2. Implementation Checklist

1. **Define Request & Response DTOs**:
   - Create immutable DTO classes under `com.eshopping.<service>.<feature>.dto`.
   - Apply Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Min`).
2. **Add OpenAPI Annotations**:
   - Annotate controller method with `@Operation(summary = "...", description = "...")`.
   - Document potential responses with `@ApiResponse(responseCode = "200")`, `@ApiResponse(responseCode = "400")`.
3. **Declare Method in Domain Service**:
   - Define interface signature in `<Feature>Service`.
   - Implement business logic in `<Feature>ServiceImpl` with `@Transactional`.
4. **Implement REST Controller Method**:
   - Apply appropriate HTTP mapping (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`).
   - Use thin controller design: validate via `@Valid`, invoke service, return `ResponseEntity.ok(...)` or `ResponseEntity.status(HttpStatus.CREATED)...`.
5. **Add Global Exception Handling**:
   - If new domain exceptions are introduced, register a handler in `GlobalExceptionHandler` with typed error code.
6. **Write Unit & Integration Tests**:
   - Unit test service logic using Mockito.
   - Integration test controller routing and validation using `MockMvc`.

