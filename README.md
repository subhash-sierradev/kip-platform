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
| **Build Tools**      | Gradle 9.4.1, Vite 7.3.0                         |
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
- **Gradle 9.4.1** (single build tool — all versions centralised in `api/gradle.properties`)
- Azure Key Vault for secrets management
- REST APIs, scheduling, data extraction, processing, publishing
- Security with multi-tenant support
- Fat JAR deployment via `bootJar` (Gradle)

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
│   ├── gradle.properties                     # All versions centralised here
│   ├── gradlew / gradlew.bat                 # Gradle wrapper
│   ├── integration-execution-contract/       # Shared DTOs (lightweight, no tests)
│   │   ├── build.gradle
│   │   ├── checkstyle.xml                    # Checkstyle config — module root (authoritative)
│   │   └── checkstyle/
│   │       └── checkstyle-suppressions.xml   # Gradle configDirectory (${config_loc})
│   ├── integration-management-service/       # Config API (8085, fat JAR)
│   │   ├── build.gradle
│   │   ├── checkstyle.xml
│   │   └── checkstyle/
│   │       └── checkstyle-suppressions.xml
│   └── integration-execution-service/        # Processing Engine (8081, fat JAR)
│       ├── build.gradle
│       ├── checkstyle.xml
│       └── checkstyle/
│           └── checkstyle-suppressions.xml
│
├── web/                                      # Vue.js 3.5.25 + TypeScript 5.7.2
│   ├── src/
│   │   ├── components/
│   │   ├── api/
│   │   ├── store/
│   │   └── composables/
│   └── vite.config.ts
│
├── e2e/                                      # Playwright E2E testing suite
│   ├── tests/
│   └── pages/
│
├── packages/
│   └── prompt-library/
└── README.md
```

> **Checkstyle layout**: `checkstyle.xml` at each module root is read by Gradle via `configFile`.
> `checkstyle/` is Gradle's `configDirectory` for `${config_loc}` resolution (Gradle 9 forbids pointing
> `configDirectory` at the module root when `build/` output is present). `checkstyle-suppressions.xml`
> lives only inside `checkstyle/` — edit it directly there.

**Detailed documentation**: See `api/README.md`, `web/README.md`, and `e2e/README.md`

---

## Quick Start

### Prerequisites

- **Java 25** or higher
- **Gradle 9.4.1** (or use the included `gradlew` wrapper — no local install needed)
- **Node.js** (for frontend)
- **PostgreSQL 18+**

### Backend (Gradle)

```powershell
cd api

# Build all modules (produces fat JARs in build/libs/)
./gradlew build -x test

# Run management service (port 8085)
./gradlew :integration-management-service:bootRun

# Run execution service (port 8081)
./gradlew :integration-execution-service:bootRun

# Run fat JARs directly
./gradlew bootJar -x test
java -jar integration-management-service/build/libs/integration-management-service-*.jar
java -jar integration-execution-service/build/libs/integration-execution-service-*.jar
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

```powershell
cd api

# Build all modules (fat JARs)
./gradlew build -x test

# Run tests with JaCoCo coverage report
./gradlew test jacocoTestReport

# Enforce 80% coverage threshold
./gradlew check

# Checkstyle — main + test sources
./gradlew checkstyleMain checkstyleTest

# Full build including tests and quality checks
./gradlew build

# Database migration — IMS only (needs running PostgreSQL)
./gradlew :integration-management-service:flywayMigrate
./gradlew :integration-management-service:flywayInfo
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
