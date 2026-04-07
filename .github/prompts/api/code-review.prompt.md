---
description: 'Full KIP backend code review — conventions, tenancy, security, DB, messaging, and tests.'
---

Review the selected code against KIP backend standards. Group all findings by severity:
🔴 Critical (must fix before merge) / 🟡 Medium (should fix) / 🟢 Minor (nice to fix)

## 1. Project Conventions
- [ ] Constructor injection (`@RequiredArgsConstructor`) — no `@Autowired` field injection
- [ ] `@Transactional` on all write operations (create, update, delete)
- [ ] Custom domain exceptions used — not raw `RuntimeException` or `Exception`
- [ ] Controllers return contract DTOs from `integration-execution-contract`, never raw JPA entities
- [ ] Line length ≤ 120 chars; `PascalCase` classes; `camelCase` methods/vars; `SCREAMING_SNAKE_CASE` constants
- [ ] MapStruct `@Mapper` used for DTO ↔ entity conversion — no manual mapping in controllers/services

## 2. Multi-Tenancy
- [ ] All entity reads/writes filter by `tenantId`
- [ ] `tenantId` sourced from JWT claims only — never from request body or query params
- [ ] New entities extend `BaseEntity` (id, tenantId, createdBy, createdAt, updatedBy, updatedAt, version)

## 3. Security
- [ ] No SQL string concatenation — JPQL / Spring Data named params only
- [ ] No credentials, keys, or tokens in logs or API responses
- [ ] Request DTOs annotated with `@NotNull`/`@NotBlank`/`@Size`; controllers use `@Valid`
- [ ] No hardcoded secrets or URIs — sensitive config in Azure Key Vault or env vars only

## 4. Error Handling
- [ ] No exceptions swallowed silently (`catch (Exception e) {}`)
- [ ] No `try/catch` in controllers — `GlobalExceptionHandler` (`@ControllerAdvice`) handles all REST errors
- [ ] Error responses use the standardized `ErrorResponse` DTO

## 5. Database & JPA
- [ ] No N+1 queries — `@EntityGraph` or `JOIN FETCH` used where lazy collections are accessed
- [ ] Schema changes use a new Flyway migration (`V{n}__description.sql`) — never `ddl-auto: create/update`
- [ ] Concurrent create uses `DataIntegrityViolationException` catch + re-fetch pattern
- [ ] `@Version` present on entities requiring optimistic locking

## 6. Performance
- [ ] `@Cacheable` used for frequently-read, rarely-changed data (tenant/user profiles, master data)
- [ ] Paginated queries (`Pageable` / `Page<T>`) for list endpoints — no unbounded `findAll()`
- [ ] Feign client calls wrapped with Resilience4j circuit breaker or retry

## 7. Testing
- [ ] Unit test present for each new public method; named `methodName_condition_expectedResult`
- [ ] All external dependencies mocked (`@Mock` / `@InjectMocks`)
- [ ] Integration tests use `@SpringBootTest` + Testcontainers, not mocked repositories

## 8. RabbitMQ / Messaging
- [ ] Queue/exchange names from `QueueNames` constants — no hardcoded strings
- [ ] `@PublishNotification` used on service methods; no ad-hoc `rabbitTemplate.convertAndSend` in business logic
- [ ] IES only publishes to notification exchange — never declares IMS-owned queues or bindings
