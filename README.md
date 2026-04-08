# Kaseware Integration Platform

Enterprise-grade multi-tenant data integration platform with automated scheduling, monitoring, and modular architecture.

---

## Quick Overview

KIP Backend automates data synchronization workflows:

- **Scheduled Jobs**: Periodic batch sync (e.g., Kaseware → ArcGIS)
- **Real-time Webhooks**: Instant event processing (e.g., Jira → Kaseware)
- **Geographic Intelligence**: Case data visualization on ArcGIS maps
- **Issue Tracking**: Bidirectional Jira integration
- **Multi-tenant**: Secure tenant isolation with Azure Key Vault

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Frontend (Vue.js) + External API Clients               │
└──────────────────┬──────────────────────────────────────┘
                   │
                   │ OIDC Login / REST + JWT
                   ▼
         ┌─────────────────────┐
         │  Keycloak (OAuth2)  │
         │  JWT Token Provider │
         └───────┬─────────┬───┘
                 │         │
        Validate │         │ Validate
          Token  │         │  Token
                 ▼         ▼
┌──────────────────────────────────────────────┐         ┌──────────────────┐
│  integration-management-service              │───────▶│  PostgreSQL      │
│  • REST APIs for CRUD operations             │         │  (multi-tenant)  │
│  • Quartz job scheduling & execution         │  DB     └──────────────────┘
│  • @PublishNotification AOP (SSE events)     │  Access
└──────┬──────────────────────────────────-──-─┘
       │                                ▲     │
       │ Publishes             Listens  |     │ REST + JWT (metadata)
       ▼                       (status) |     │
┌──────────────────────────────────────────┐  │
│  RabbitMQ (Message Broker)               │  │
│  • notification.exchange (Topic)         │  │
│  • arcgis.exchange (Direct)              │  │
│  • jira.exchange (Direct)                │  │
│  • confluence.exchange (Direct)          │  │
└────┬───────────────────────────────────▲─┘  │
     │                                   │    │
     │ Consumes job commands   Publishes │    │
     ▼                         (status)  │    ▼
┌────────────────────────────────────────┴──────────────────┐   ┌─────────────────┐
│  integration-execution-service                            │   │ Azure Key Vault │
│  • Webhook event processing                               │◀─┤  (Credentials)  |
│  • Data extraction, transformation, publishing            │   │ Retrieve/Store  │
│  • External API clients (Jira/ArcGIS/Kaseware/Confluence) │   └─────────────────┘
│  • Extractor → Processor → Publisher pipeline             │
│  • REST endpoint for metadata (validates JWT from IMS)    │
└──────┬────────────────────────────────────────────────────┘
       │
       │ External API Calls
       ▼
    ┌────────┬────────────┬──────────────┬
    ▼        ▼            ▼              ▼
  Jira    Kaseware      ArcGIS      Confluence
```

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

| Component            | Technology                                       |
| -------------------- | ------------------------------------------------ |
| **Language**         | Java 25, TypeScript 5.7.2                        |
| **Frameworks**       | Spring Boot 4.0.4, Vue.js 3.5.25                 |
| **Build Tools**      | Gradle 9.4.1 (primary) · Maven 3.8+ (dual-build), Vite 7.3.0 |
| **Database**         | PostgreSQL 42.7.7 with JPA/Hibernate 7.1.8.Final |
| **Authentication**   | Keycloak 26.2.0                                  |
| **UI Framework**     | DevExtreme 25.2.3                                |
| **State Management** | Pinia 3.0.4                                      |
| **Testing**          | Vitest 4.0.9, Vue Testing Library 8.1.0          |

### Component Details

**Frontend:**

- Vue.js 3.5.25, TypeScript 5.7.2, Vite 7.3.0
- DevExtreme 25.2.3 UI components with responsive design
- Pinia 3.0.4 state management with persistence
- Vue Router 4.6.3, Keycloak 26.2.0 for authentication
- @vueuse/core 14.1.0 composable utilities
- Testing: Vitest 4.0.9, Vue Testing Library 8.1.0, 80% coverage target

**Backend:**

- Spring Boot 4.0.4, Java 25
- Multi-module build: `integration-execution-contract`, `integration-management-service`, `integration-execution-service`
- **Gradle 9.4.1** (primary build tool) + **Maven 3.8+** (dual-build — both fully supported)
- Azure Key Vault for secrets management
- REST APIs, scheduling, data extraction, processing, publishing
- Security with multi-tenant support
- Fat JAR deployment via `bootJar` (Gradle) / `spring-boot:repackage` (Maven)

**Database & Infrastructure:**

- PostgreSQL 42.7.7 with JPA/Hibernate 7.1.8.Final (multi-tenant)
- Quartz Scheduler for job management
- Docker support (compose configuration available)
- Checkstyle for code quality enforcement

---

## Project Structure

```
kip-platform/
├── api/                                      # Spring Boot 4.0.4 + Java 25 multi-module
│   ├── settings.gradle                       # Gradle root — submodule declarations
│   ├── build.gradle                          # Gradle root — shared Java toolchain config
│   ├── gradle.properties                     # Gradle — all versions centralised
│   ├── gradlew / gradlew.bat                 # Gradle wrapper (primary build)
│   ├── pom.xml                               # Maven root (dual-build — aggregator only, unchanged)
│   ├── integration-execution-contract/       # Shared DTOs (lightweight, no tests)
│   │   ├── build.gradle                      # Gradle build
│   │   ├── pom.xml                           # Maven build (unchanged)
│   │   ├── checkstyle.xml                    # Checkstyle config — authoritative, read by BOTH Maven & Gradle
│   │   ├── checkstyle-suppressions.xml       # Suppressions — authoritative, read by Maven
│   │   └── checkstyle/                       # Gradle configDirectory (copies — Gradle 9 cannot use module root as configDirectory)
│   ├── integration-management-service/       # Config API (8085, fat JAR)
│   │   ├── build.gradle                      # Gradle build (Spring Boot + JaCoCo + Flyway)
│   │   ├── pom.xml                           # Maven build (unchanged)
│   │   ├── checkstyle.xml                    # Checkstyle config — authoritative, read by BOTH Maven & Gradle
│   │   ├── checkstyle-suppressions.xml       # Suppressions — authoritative, read by Maven
│   │   └── checkstyle/                       # Gradle configDirectory (copies)
│   └── integration-execution-service/        # Processing Engine (8081, fat JAR)
│       ├── build.gradle                      # Gradle build (Spring Boot + JaCoCo)
│       ├── pom.xml                           # Maven build (unchanged)
│       ├── checkstyle.xml                    # Checkstyle config — authoritative, read by BOTH Maven & Gradle
│       ├── checkstyle-suppressions.xml       # Suppressions — authoritative, read by Maven
│       └── checkstyle/                       # Gradle configDirectory (copies)
│
├── web/                                      # Vue.js 3.5.25 + TypeScript 5.7.2
│   ├── src/                                  # Application source code
│   │   ├── components/                       # Feature-based Vue components
│   │   ├── api/                              # Auto-generated OpenAPI client
│   │   ├── store/                            # Pinia state management
│   │   └── composables/                      # Reusable Vue logic
│   └── vite.config.ts                        # Build configuration (80% coverage)
│
├── e2e/                                      # Playwright E2E testing suite
│   ├── tests/                                # Test specifications
│   └── pages/                                # Page Object Model
│
├── packages/                                 # Shared libraries and templates
│   └── prompt-library/                       # AI prompt library
└── README.md                                 # This file
```

> **Checkstyle config layout**: Each module root is the **authoritative source** — `checkstyle.xml` is read by both Maven and Gradle; `checkstyle-suppressions.xml` is read by Maven. Each module's `checkstyle/` subdirectory is Gradle's `configDirectory` and contains copies of both files. Gradle 9 forbids pointing `configDirectory` at the module root (it contains `build/`). Always update module-root files first, then sync the `checkstyle/` copies.

**Detailed documentation**: See `api/README.md`, `web/README.md`, and `e2e/README.md`

---

## Quick Start

### Prerequisites

- **Java 25** or higher
- **Gradle 9.4.1** (primary) **or Maven 3.6+** (dual-build — both supported)
- **Node.js** (for frontend)
- **PostgreSQL 18+**

### Backend — Gradle (primary)

```powershell
cd api

# Build all modules (produces fat JARs in build/libs/)
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

### Backend — Maven (dual-build fallback)

```powershell
cd api
mvn clean install
# Run management service
cd integration-management-service
mvn spring-boot:run
# Run execution service
cd ../integration-execution-service
mvn spring-boot:run
```

### Frontend (Vue.js 3.5.25 + Vite)

```powershell
cd web
npm install
npm run dev
```

**Development URL:** `http://localhost:8084`

### E2E Tests (Playwright)

```powershell
cd e2e
npm install
npm run sanity
```

---

## Core Features

- **Multi-tenant architecture** with isolated configurations and data
- **Modular backend** (integration-execution-contract, integration-management-service, integration-execution-service)
- **REST APIs** for integration, scheduling, data extraction, processing, publishing
- **Quartz-based job scheduling** for robust data processing, automated scheduling, and comprehensive cron support
- **Jira Webhook Integration** with complete webhook lifecycle management
- **Modern Vue.js UI** with DevExtreme components and TypeScript
- **Pinia state management** with reactive stores and persistence
- **Keycloak authentication** for secure multi-tenant access
- **Comprehensive testing**: Frontend 80% coverage (Vitest), Backend 80% coverage (JUnit)
- **Code quality enforcement**: ESLint (frontend), Checkstyle (backend)
- **PostgreSQL integration** with JPA/Hibernate for relational data storage

---

## Development & Testing

### Backend Commands

#### Gradle (primary)

```powershell
cd api

# Build all modules (fat JARs)
./gradlew build -x test

# Run tests with JaCoCo coverage report
./gradlew test jacocoTestReport

# Enforce 80% coverage threshold (equivalent to mvn verify)
./gradlew check

# Checkstyle — main + test sources
./gradlew checkstyleMain checkstyleTest

# Full build including tests and quality checks
./gradlew build

# Database migration — IMS only (needs running PostgreSQL)
./gradlew :integration-management-service:flywayMigrate
./gradlew :integration-management-service:flywayInfo
```

#### Maven (dual-build fallback)

```powershell
cd api
mvn clean install

# Run tests with coverage (80% minimum)
mvn test
mvn jacoco:report

# Code quality checks
mvn checkstyle:check

# Full verification
mvn clean verify

# Database Migration (Flyway - run from specific service)
cd integration-management-service
mvn flyway:migrate
mvn --% -Dflyway.cleanDisabled=false flyway:clean flyway:migrate
```

### Frontend Commands

```powershell
cd web

# Development server
npm run dev

# Build for production
npm run build

# Build and deploy via HTTP
npm run build:deploy:http

# Validate and fix code issues
npm run validate:fix

# Run tests
npm run test

# Run tests with coverage and generate detailed coverage charts
npm run test:coverage

# Generate API client
npm run generate:api
```

### E2E Commands

```powershell
cd e2e

# List discovered tests (dry run)
npx playwright test --list

# Run sanity and regression suites
npm run sanity
npm run regression
npm run regression:arcgis
npm run regression:jira

# Open Playwright report
npm run report
```

**Coverage Reports**: After running `npm run test:coverage`, open `coverage/index.html` in your browser to view detailed file-wise coverage charts and reports.
