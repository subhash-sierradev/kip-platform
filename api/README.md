# KIP Backend - Kaseware Integration Platform

[![Java](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue.svg)](https://www.postgresql.org/)
![KIP Backend CI](https://github.com/kaseware/kip-backend/actions/workflows/ci.yml/badge.svg)

**Multi-tenant SaaS platform for bidirectional data synchronization between Kaseware and external systems (Jira, ArcGIS).**

---

## Overview

KIP Backend automates data synchronization workflows:

- **Scheduled Jobs**: Periodic batch sync (e.g., Kaseware → ArcGIS)
- **Real-time Webhooks**: Instant event processing (e.g., Jira → Kaseware)
- **Geographic Intelligence**: Case data visualization on ArcGIS maps
- **Issue Tracking**: Bidirectional Jira integration
- **Multi-tenant**: Secure tenant isolation with Azure Key Vault

---

**Key Integration Flows:**

- **Authentication**: Frontend → Keycloak (OIDC) → JWT tokens issued and validated by IMS/IES
- **Inter-Service Auth**: IMS → IES REST calls include JWT → IES validates token with Keycloak
- **Notifications**: `@PublishNotification` (IMS) → RabbitMQ TopicExchange → `NotificationListener` (IMS) → SSE → Browser
- **Job Scheduling**: IMS (Quartz) → RabbitMQ DirectExchange → IES (consume & process) → RabbitMQ (publish status) → IMS
- **Metadata Fetch**: IMS → IES REST endpoint + JWT (fetch field schemas, endpoint metadata)
- **Data Persistence**: IMS only → PostgreSQL (multi-tenant) — IES is stateless
- **Secrets**: IMS/IES → Azure Key Vault for credentials

**Modules:**

- `integration-management-service` - User-facing configuration API (executes as fat JAR)
- `integration-execution-service` - Backend processing engine (executes as fat JAR)
- `integration-execution-contract` - Shared DTOs/models (lightweight, no tests)

---

## Tech Stack

| Component        | Technology                                           |
| ---------------- | ---------------------------------------------------- |
| **Language**     | Java 25                                              |
| **Framework**    | Spring Boot 4.0.4                                    |
| **Database**     | PostgreSQL 42.7.7 + Flyway 11.20.0                   |
| **Scheduler**    | Quartz (clustered)                                   |
| **Security**     | OAuth2 + Keycloak JWT                                |
| **Cloud**        | Azure Key Vault 4.10.4                               |
| **Transform**    | JOLT 0.1.8, MapStruct 1.6.3                          |
| **Resilience**   | Resilience4j 2.3.0, Spring Retry 2.0.12              |
| **Messaging**    | RabbitMQ (Spring AMQP)                               |
| **Spring Cloud** | 2025.1.1 (OpenFeign)                                 |
| **Testing**      | JUnit 5.10.3, Testcontainers 1.20.4, WireMock 3.13.1 |

---

## Quick Start

### Prerequisites

- Java 25 (JDK)
- PostgreSQL 14+
- RabbitMQ 3.x
- Azure subscription (or use dev fallback)

### Build & Run

```bash
# Build entire project (run from api/)
./gradlew clean build

# Build specific module
./gradlew :integration-execution-contract:build

# Run management service (port 8085)
./gradlew :integration-management-service:bootRun

# Or run fat JAR
./gradlew :integration-management-service:bootJar
java -jar integration-management-service/build/libs/integration-management-service-<version>.jar

# Run execution service (port 8081 - separate terminal)
./gradlew :integration-execution-service:bootRun

# Or run fat JAR
./gradlew :integration-execution-service:bootJar
java -jar integration-execution-service/build/libs/integration-execution-service-<version>.jar
```

### Access Services

- **Management API**: http://localhost:8085
- **Execution API**: http://localhost:8081

---

## Key Features

### 1. ArcGIS Integration (Scheduled)

- Extract Kaseware documents with location data
- Transform to GeoJSON features
- Batch publish to ArcGIS feature layers
- Configurable schedules (daily/weekly/monthly)

### 2. Jira Webhooks (Real-time)

- Receive Jira issue created/updated events
- Transform and map fields
- Push to Kaseware entities
- Automatic retry on failures

### 3. Notification System (RabbitMQ + SSE)

- AOP-driven event publishing via `@PublishNotification` on service methods
- Async delivery through RabbitMQ topic exchange (`integration.notification.exchange`)
- Per-tenant rules, templates, and recipient policies (ALL_USERS / ADMINS_ONLY / SELECTED_USERS)
- Real-time push to browser via Server-Sent Events (`/api/management/notifications/stream`)
- Multi-tab support with 30-minute emitter timeout and 25-second heartbeat
- Full audit trail via `notification_event_log` table

### 4. Multi-tenant SaaS

- Tenant-based data isolation
- Secure credential storage (Azure Key Vault)
- Per-tenant field mappings
- Audit trails for compliance

---

## Testing

```bash
# Run all tests (from api/)
./gradlew test

# Run with coverage report (80% minimum threshold enforced)
./gradlew test jacocoTestReport

# View coverage report
open integration-management-service/build/reports/jacoco/test/html/index.html

# Run checkstyle validation
./gradlew checkstyleMain checkstyleTest

# Run specific test class
./gradlew :integration-management-service:test --tests "com.integration.management.JiraApiClientTest"

# Full verification (checkstyle + tests + coverage)
./gradlew clean check
```

---

## Security & Dependency Hygiene

### 1. OWASP CVE Scanning

Scans all direct and transitive dependencies against the NIST NVD. **Fails the build** if any CVE with CVSS ≥ 7.0 is detected. The plugin is applied to every subproject so individual modules can be scanned in isolation.

```bash
# Scan all modules — aggregated report (preferred for CI)
./gradlew dependencyCheckAggregate

# Scan a single module in isolation (developer workflow)
./gradlew :integration-management-service:dependencyCheckAnalyze
./gradlew :integration-execution-service:dependencyCheckAnalyze
./gradlew :integration-execution-contract:dependencyCheckAnalyze

# HTML report locations
open integration-management-service/build/reports/dependency-check/dependency-check-report.html
open integration-execution-service/build/reports/dependency-check/dependency-check-report.html
```

**NVD API Key (recommended)** — without a key, NVD rate-limits downloads to ~10 req/min, which can cause timeouts in CI. Obtain a free key at <https://nvd.nist.gov/developers/request-an-api-key> and configure it via **one** of:

```bash
# Option A — Gradle property (local, never commit to VCS)
echo "nvd.api.key=YOUR_KEY" >> ~/.gradle/gradle.properties

# Option B — environment variable (CI/CD)
export NVD_API_KEY=YOUR_KEY
```

**Suppressing false positives** — add entries to `api/owasp-suppressions.xml` with a justification comment and expiry date. The same file is shared by all subproject scans. See the scaffold in that file for the format.

**Forced transitive upgrades** — some CVEs are remediated by overriding a transitive dependency version. All forced versions are declared in `gradle/libs.versions.toml` (e.g., `commonsLang3`, `plexusUtils`) and referenced from the root `build.gradle.kts` `resolutionStrategy` block — a single-line bump in the catalog propagates to all modules automatically.

### 2. Outdated Dependency Report (`dependencyUpdates`)

Produces **informational** HTML/JSON reports of libraries that have newer stable releases available. Does **not** fail the build. The plugin is applied per-subproject so each module's report reflects its own declared dependencies.

```bash
# Run across all modules at once (root aggregate shim)
./gradlew dependencyUpdates

# Or per-module
./gradlew :integration-management-service:dependencyUpdates
./gradlew :integration-execution-service:dependencyUpdates

# HTML report locations (one per module)
open integration-management-service/build/reports/dependencyUpdates/dependency-updates.html
open integration-execution-service/build/reports/dependencyUpdates/dependency-updates.html
open integration-execution-contract/build/reports/dependencyUpdates/dependency-updates.html
```

Non-stable versions (alpha, beta, RC, milestone) are automatically excluded — only stable upgrade candidates are surfaced.

### 3. Compiler Deprecation Flags (automatic)

`-Xlint:deprecation` and `-Xlint:unchecked` are applied globally to every `compileJava` task across all subprojects. Deprecation and unchecked-cast warnings appear in the compiler output on every `./gradlew build` — no extra command needed.

**Current Coverage**: IMS ~39% (Target: 80%) · IES: 0% (Target: 80%)

---

## Project Structure

```
kip-backend/
├── integration-execution-contract/      # Shared DTOs (lightweight, no tests)
├── integration-management-service/      # Config API (8085, fat JAR)
└── integration-execution-service/       # Processing Engine (8081, fat JAR)
```

---

## Configuration

### Database (PostgreSQL)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/kw_ip_dev_micro
    username: postgres
    password: YOUR_PASSWORD
```

### Profiles

- `dev` - Development (default)
- `sandbox` - Sandbox environment
- `prod` - Production

### Azure Key Vault (Production)

```yaml
azure:
  keyvault:
    uri: https://{vault-name}.vault.azure.net/
    enabled: true
```

### Development Fallback

Set `azure.keyvault.enabled: false` to use PostgreSQL `vault_secrets` table.

---

## Development Status

> **Note**: The root `kip-backend` parent POM is permanently fixed at `1.0.0`. Only child module versions (`integration-execution-contract`, `integration-management-service`, `integration-execution-service`) are updated during releases.

**Recent Updates**:

- **Notification System**: Full async notification pipeline via RabbitMQ + SSE — rules, templates, recipient policies, per-user delivery, real-time browser push
- Fat JAR deployment configured
- Contract module optimized (lightweight)
- EditorConfig for IDE consistency
- Multi-environment configurations
- Code coverage improvement (39% → 80% target)

### Code Standards

1. **IDE Formatting**: Use .editorconfig (automatic in IntelliJ/VS Code/Eclipse)
2. **Checkstyle**: Run `./gradlew checkstyleMain checkstyleTest` before committing
3. **Testing**: Write tests first (TDD preferred), maintain >80% coverage
4. **Database**: Always use Flyway migrations for schema changes
5. **Contract Module**: Coordinate changes - shared by both services

### Development Workflow

```bash
# 1. Format code (automatic with EditorConfig)
# 2. Validate checkstyle
./gradlew checkstyleMain checkstyleTest

# 3. Run tests with coverage
./gradlew test jacocoTestReport

# 4. Build all modules
./gradlew clean build

# 5. Test both services locally
# Terminal 1: Management Service (8085)
./gradlew :integration-management-service:bootRun

# Terminal 2: Execution Service (8081)
./gradlew :integration-execution-service:bootRun
```

**Last Updated**: March 16, 2026

### Naming Conventions (enforced by Checkstyle)

- **Classes**: `PascalCase` (e.g., `JiraWebhookService`)
- **Methods/Variables**: `camelCase` (e.g., `processWebhook`)
- **Constants**: `SCREAMING_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT`)

---

### Exchange & Queue Topology

| Constant                               | Value                                  | Type                     |
| -------------------------------------- | -------------------------------------- | ------------------------ |
| `NOTIFICATION_EXCHANGE`                | `integration.notification.exchange`    | `TopicExchange`          |
| `NOTIFICATION_QUEUE_IMS`               | `integration.notification.ims`         | Durable queue (IMS only) |
| `NOTIFICATION_ROUTING_KEY`             | `notification.event`                   | Binding/routing key      |
| `ARCGIS_EXCHANGE`                      | `integration.arcgis.exchange`          | `DirectExchange`         |
| `ARCGIS_EXECUTION_COMMAND_QUEUE`       | `integration.arcgis.execution.command` | Durable                  |
| `ARCGIS_EXECUTION_RESULT_QUEUE`        | `integration.arcgis.execution.result`  | Durable                  |
| `JIRA_WEBHOOK_EXCHANGE`                | `integration.jira.exchange`            | `DirectExchange`         |
| `JIRA_WEBHOOK_EXECUTION_COMMAND_QUEUE` | `integration.jira.execution.command`   | Durable                  |
| `JIRA_WEBHOOK_EXECUTION_RESULT_QUEUE`  | `integration.jira.execution.result`    | Durable                  |

All constants live in `integration-execution-contract/.../queue/QueueNames.java` (shared by both services).

### Notification Flow

```
Service method (@PublishNotification)
        │
        ▼
  NotificationAspect (AOP @Around)
        │  builds NotificationEvent { eventKey, tenantId, userId, metadata }
        ▼
  NotificationEventPublisher (IMS or IES)
        │  rabbitTemplate.convertAndSend("integration.notification.exchange", "notification.event", event)
        ▼
  RabbitMQ → integration.notification.ims queue
        │
        ▼
  NotificationListener (@RabbitListener — IMS only)
        │
        ▼
  NotificationDispatchService
        │  1. Lookup enabled NotificationRule for (eventKey, tenantId)
        │  2. Resolve recipients via RecipientType (ALL_USERS / ADMINS_ONLY / SELECTED_USERS)
        │  3. Render NotificationTemplate ({{placeholder}} substitution)
        │  4. Persist AppNotification per user
        │  5. SseEmitterRegistry.send(userId, payload)
        ▼
  Browser ← SSE event "notification" (GET /api/management/notifications/stream)
```

### Key Classes

| Class                                   | Module    | Role                                                                   |
| --------------------------------------- | --------- | ---------------------------------------------------------------------- |
| `@PublishNotification`                  | IMS       | Annotation — triggers async notification on method success             |
| `NotificationAspect`                    | IMS       | `@Around` AOP — pre-fetches metadata, publishes after `proceed()`      |
| `NotificationEventPublisher`            | IMS + IES | Sends `NotificationEvent` to topic exchange                            |
| `NotificationListener`                  | IMS       | `@RabbitListener` consumer on `integration.notification.ims`           |
| `NotificationDispatchService`           | IMS       | Orchestrates rule → recipient → template → persist → SSE               |
| `SseEmitterRegistry`                    | IMS       | Per-user emitter map; 30-min timeout; 25s heartbeat; multi-tab support |
| `AppNotificationController`             | IMS       | SSE stream + REST: paginated list, unread count, mark-as-read          |
| `NotificationRuleController`            | IMS       | CRUD + toggle + batch-create per-tenant rules                          |
| `NotificationTemplateController`        | IMS       | Per-tenant `{{placeholder}}` message templates                         |
| `NotificationRecipientPolicyController` | IMS       | Audience policy per rule                                               |
| `NotificationEventCatalogController`    | IMS       | Read-only catalog of 22 platform event types                           |

### Entities (`notifications` DB schema)

`AppNotification` · `NotificationRule` · `NotificationEventCatalog` · `NotificationTemplate` · `NotificationRecipientPolicy` · `NotificationRecipientUser` · `NotificationEventLog`

### Enums

`NotificationType` (22 values: `ARCGIS_INTEGRATION_*`, `JIRAWEBHOOK_INTEGRATION_*`, `SITE_CONFIG_*`, etc.) · `NotificationSeverity` (INFO / WARNING / ERROR / SUCCESS) · `NotificationEntityType` · `RecipientType` (ALL_USERS / ADMINS_ONLY / SELECTED_USERS)

### Metadata Providers (pre-fetch AOP strategy)

| Bean                                      | Resolves For                        |
| ----------------------------------------- | ----------------------------------- |
| `arcGISNotificationMetadataProvider`      | ArcGIS integration CRUD & lifecycle |
| `jiraWebhookNotificationMetadataProvider` | Jira webhook CRUD & toggle          |
| `siteConfigNotificationMetadataProvider`  | Site config changes                 |

---

## Contributing

**Standards**:

- Follow EditorConfig (automatic formatting)
- Run `./gradlew checkstyleMain checkstyleTest` before commits
- Maintain >80% test coverage
- Use Flyway for database changes

**Naming Conventions**:

- Classes: `PascalCase`
- Methods/Variables: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`

---

## License

Copyright © 2026 Kaseware. All rights reserved.

---
