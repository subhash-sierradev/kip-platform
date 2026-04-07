---
description: 'Generate comprehensive unit + integration tests for KIP backend  JUnit 5, parameterized, controller slice, service, repository, all status codes, positive + negative paths.'
---

Generate a full test suite for the selected code following KIP backend standards and JUnit 5 / Spring Boot best practices. Cover every layer present in the selection.

---

## General Rules (apply to all test types)
- Test class name: `{ClassName}Test`
- Test method name pattern: `methodName_condition_expectedResult`
- Build all test entities via builder pattern: `Entity.builder().tenantId("test-tenant")...build()`
- `tenantId` must always be set to the constant `"test-tenant"` on every test entity and mock return
- Never assert only `assertNotNull`  always assert the actual value where possible
- Use `@DisplayName("plain English description")` on every test method
- Group related tests inside `@Nested` inner classes (e.g. `@Nested class WhenEntityExists`, `@Nested class WhenEntityNotFound`)
- Target: **>= 80% line coverage** per class; **100%** on tenant-isolation and security-critical paths

---

## 1. Service Layer Unit Tests
**Setup**: `@ExtendWith(MockitoExtension.class)`  `@Mock` all repositories and external clients  `@InjectMocks` the service under test

For every public service method generate:

### Positive (Happy Path)
- Valid input -> verify correct return value (assert all fields, not just id)
- Verify the correct repository/client method was called with `verify(...)`
- Verify `@Transactional` side-effects (save called once, flush not called unnecessarily)

### Negative
- Entity not found -> throws the correct **custom domain exception** (e.g. `IntegrationPersistenceException`) with meaningful message
- Wrong `tenantId` -> returns `Optional.empty()` or throws  never leaks another tenant's data
- Null required argument -> throws `IllegalArgumentException` or domain exception
- External service (Feign) throws -> service wraps and rethrows as domain exception

### Parameterized Tests (`@ParameterizedTest`)
Use `@MethodSource` or `@CsvSource` for boundary/equivalence tests:
```java
@ParameterizedTest
@CsvSource({"'', false", "'   ', false", "'valid-input', true"})
@DisplayName("isValid returns expected result for various inputs")
void isValid_variousInputs_returnsExpected(String input, boolean expected)
```

### Edge Cases
- Empty collection input -> returns empty list (no exception)
- Concurrent `getOrCreate` -> `DataIntegrityViolationException` caught, existing entity re-fetched and returned
- `@Cacheable` method called twice -> repository called only once (verify with `verify(repo, times(1))`)

---

## 2. Controller Layer Unit Tests (`@WebMvcTest`)
**Setup**: `@WebMvcTest({ControllerClass}.class)`  `@MockBean` all services  inject `MockMvc`

### HTTP Status Code Coverage  generate one test per status:

| Scenario | Expected Status |
|---|---|
| Valid request, entity created | `201 Created` + `Location` header |
| Valid request, entity returned | `200 OK` |
| Valid request, entity deleted | `204 No Content` |
| Entity not found | `404 Not Found` |
| Validation failure (blank field, null, size) | `400 Bad Request` |
| Wrong tenant / unauthorized access | `403 Forbidden` |
| Unauthenticated request | `401 Unauthorized` |
| Conflict (duplicate) | `409 Conflict` |
| Internal server error (service throws unexpected) | `500 Internal Server Error` |

### Controller Test Pattern
```java
@Test
@DisplayName("POST /api/.../resource - valid input returns 201 with location header")
void create_validRequest_returns201WithLocationHeader() throws Exception {
    mockMvc.perform(post("/api/.../resource")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest))
            .with(jwt().jwt(j -> j.claim("tenantId", "test-tenant"))))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.name").value("expected-name"));
}
```

### Request Validation Tests (`@ParameterizedTest` + `@MethodSource`)
Generate one parameterized test covering all invalid field combinations -> each must return `400 Bad Request`:
```java
@ParameterizedTest
@MethodSource("invalidRequestProvider")
@DisplayName("POST returns 400 for all invalid request variants")
void create_invalidRequest_returns400(CreateRequest invalidRequest, String expectedErrorField)
```

### Security Tests
- No JWT -> `401`
- JWT with different `tenantId` -> `403`
- JWT with correct `tenantId` -> `200`/`201`

---

## 3. Repository Layer Tests (`@DataJpaTest`)
**Setup**: `@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + Testcontainers PostgreSQL

For every custom query method (`@Query` or derived):
- **Positive**: data exists with matching `tenantId` -> returns correct result
- **Negative**: no matching `tenantId` -> returns `Optional.empty()` or empty list
- **Cross-tenant**: data exists for `tenant-A`, query with `tenant-B` -> returns nothing
- **Pagination**: `findAll(Pageable)` -> verify page size, total elements, sorted correctly

---

## 4. Integration Tests (`@SpringBootTest`)
**Setup**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`  `@ActiveProfiles("test")`  Testcontainers PostgreSQL  WireMock for external HTTP

Cover the full stack (controller -> service -> DB -> response):
- Create entity -> GET by id -> assert all fields persisted correctly
- Update entity -> GET -> assert fields updated, `updatedAt` changed
- Delete entity -> GET -> `404`
- Cross-tenant: create with `tenant-A`, fetch with `tenant-B` -> `404`
- WireMock: external API returns error -> service handles gracefully, correct error response

---

## 5. RabbitMQ / Messaging Tests
- Mock `RabbitTemplate`; verify `convertAndSend` called with correct exchange, routing key, and payload
- Test `@RabbitListener` methods directly by calling them with a test `NotificationEvent`; verify `NotificationDispatchService` interactions

---

## Coverage Checklist
After generating tests, confirm coverage of:
- [ ] All `public` methods in the selected class
- [ ] All `if`/`else` branches
- [ ] All `catch` blocks
- [ ] All custom exception types thrown
- [ ] Tenant-isolation paths (100% required)
- [ ] All HTTP status codes for controller methods
