---
description: 'OWASP Top 10 security scan tailored for KIP backend (Spring Boot, multi-tenant, PostgreSQL, Azure Key Vault).'
---

Perform a security scan of the selected code. Map each finding to its OWASP Top 10 category with severity: 🔴 Critical / 🟡 Medium / 🟢 Minor.

## A01 – Broken Access Control
- [ ] All endpoints secured by OAuth2 / Keycloak JWT — no unauthenticated access
- [ ] `tenantId` always from JWT claims — never from user-supplied input (IDOR prevention)
- [ ] Entity ownership validated before read/update/delete (cross-tenant access impossible)

## A02 – Cryptographic Failures
- [ ] No credentials, API keys, or tokens in source code, logs, or API responses
- [ ] All sensitive config uses Azure Key Vault in prod (`azure.keyvault.enabled: true`)
- [ ] No plain-text passwords stored anywhere

## A03 – Injection
- [ ] No SQL string concatenation — JPQL / Criteria API / named params only
- [ ] No `@Query` with unescaped user-controlled input
- [ ] Feign client base URLs sourced from config — not constructed from user input (SSRF prevention)

## A04 – Insecure Design
- [ ] Multi-tenant isolation enforced at service layer, not just in the UI
- [ ] Public-facing webhooks (Jira) protected by Resilience4j rate limiter or circuit breaker

## A05 – Security Misconfiguration
- [ ] `ddl-auto: validate` in prod — never `create`, `create-drop`, or `update`
- [ ] CORS `allowedOriginPatterns` restricted in non-dev profiles (not `*`)
- [ ] Actuator endpoints secured or disabled in prod

## A06 – Vulnerable Components
- [ ] Dependency versions match those in `copilot-instructions.md` (PostgreSQL 42.7.7, Hibernate 7.1.8, Flyway 11.20.0, etc.)
- [ ] No SNAPSHOT or RC dependencies in production builds

## A07 – Identification & Authentication Failures
- [ ] JWT validation fully delegated to Spring Security OAuth2 resource server — no custom parsing
- [ ] Token expiry and signature enforced by Keycloak configuration

## A08 – Software & Data Integrity Failures
- [ ] Existing Flyway migration files never modified after deployment — always add a new `V{n}__` file
- [ ] RabbitMQ messages validated (structure + tenant context) before processing

## A09 – Security Logging & Monitoring Failures
- [ ] Auth failures and access denials logged at `WARN` level with enough context to investigate
- [ ] No sensitive data (tokens, passwords, PII) appears in any log statement

## A10 – SSRF
- [ ] External API URLs (Jira, ArcGIS, Kaseware) read from `application.yml` / Key Vault — not from request params
- [ ] No dynamic URL construction from user-supplied input in Feign clients or `RestTemplate` calls
