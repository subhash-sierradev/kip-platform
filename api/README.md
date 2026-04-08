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
| **Build Tool**   | Gradle 9.4.1                                         |
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
- Gradle 9.4.1 (or use the included `gradlew` wrapper — no local install needed)
- PostgreSQL 14+
- RabbitMQ 3.x
- Azure subscription (or use dev fallback)

### Build & Run

```powershell
# Build all modules (produces fat JARs)
cd api
./gradlew build -x test

# Run management service (port 8085)
./gradlew :integration-management-service:bootRun

# Run execution service (port 8081 — separate terminal)
./gradlew :integration-execution-service:bootRun

# Run fat JARs directly
./gradlew bootJar -x test
java -jar integration-management-service/build/libs/integration-management-service-*.jar
java -jar integration-execution-service/build/libs/integration-execution-service-*.jar
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

```powershell
cd api

# Run all tests
./gradlew test

# Run tests + generate JaCoCo HTML coverage report
./gradlew test jacocoTestReport

# Enforce 80% coverage threshold
./gradlew check

# Run checkstyle on main sources
./gradlew checkstyleMain checkstyleTest

# Run specific test class
./gradlew :integration-management-service:test --tests "com.integration.management.*JiraApiClientTest"

# View JaCoCo coverage report
# integration-management-service/build/reports/jacoco/test/html/index.html
```

**Current Coverage**: IMS ~39% (Target: 80%) · IES: 0% (Target: 80%)

---

## Project Structure

```
api/
├── settings.gradle                      # Gradle root — declares all submodules
├── build.gradle                         # Gradle root — shared Java toolchain config
├── gradle.properties                    # All version strings centralised here
├── gradlew / gradlew.bat                # Gradle wrapper scripts
├── gradle/wrapper/                      # Gradle wrapper JAR + properties
│
├── integration-execution-contract/      # Shared DTOs (lightweight, no tests)
│   ├── build.gradle                     # Gradle build (java-library + checkstyle)
│   ├── checkstyle.xml                   # Checkstyle config — module root (authoritative)
│   └── checkstyle/                      # Gradle configDirectory
│       └── checkstyle-suppressions.xml  # Suppression rules (${config_loc} reference)
│
├── integration-management-service/      # Config API (8085, fat JAR)
│   ├── build.gradle                     # Gradle build (Spring Boot + JaCoCo + Flyway)
│   ├── checkstyle.xml                   # Checkstyle config — module root (authoritative)
│   └── checkstyle/                      # Gradle configDirectory
│       └── checkstyle-suppressions.xml  # Suppression rules (${config_loc} reference)
│
└── integration-execution-service/       # Processing Engine (8081, fat JAR)
    ├── build.gradle                     # Gradle build (Spring Boot + JaCoCo)
    ├── checkstyle.xml                   # Checkstyle config — module root (authoritative)
    └── checkstyle/                      # Gradle configDirectory
        └── checkstyle-suppressions.xml  # Suppression rules (${config_loc} reference)
```

> **Checkstyle layout**: `checkstyle.xml` lives at the module root and is read by Gradle via `configFile`.
> `checkstyle/` is Gradle's `configDirectory` — it cannot point to the module root (Gradle 9 forbids it
> when `build/` output is present), so a dedicated subdirectory holds `checkstyle-suppressions.xml`.
> When editing suppression rules, update `checkstyle/checkstyle-suppressions.xml` directly.

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

**Recent Updates**:

- **Gradle-only build**: Maven removed; all versions centralised in `api/gradle.properties`
- **Notification System**: Full async notification pipeline via RabbitMQ + SSE — rules, templates, recipient policies, per-user delivery, real-time browser push
- Fat JAR deployment configured
- Contract module optimized (lightweight)
- Multi-environment configurations
- Code coverage improvement (39% → 80% target)

### Code Standards

1. **IDE Formatting**: Use .editorconfig (automatic in IntelliJ/VS Code/Eclipse)
2. **Checkstyle**: Run `./gradlew checkstyleMain checkstyleTest` — reads `checkstyle.xml` from module root, suppressions from `checkstyle/`
3. **Testing**: Write tests first (TDD preferred), maintain >80% coverage
4. **Database**: Always use Flyway migrations for schema changes
5. **Contract Module**: Coordinate changes — shared by both services

### Developer Workflow

```powershell
cd api

# 1. Validate checkstyle
./gradlew checkstyleMain checkstyleTest

# 2. Run tests with coverage report
./gradlew test jacocoTestReport

# 3. Enforce 80% coverage threshold
./gradlew check

# 4. Build all modules (fat JARs)
./gradlew build -x test

# 5. Database migration (IMS only)
./gradlew :integration-management-service:flywayMigrate
```

**Last Updated**: April 8, 2026

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

