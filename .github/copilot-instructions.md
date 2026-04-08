# Copilot Instructions for Kaseware Integration Platform

## 🔥 CRITICAL: ALWAYS LOAD ALL INSTRUCTION FILES FIRST

**Before generating ANY code, you MUST load and store these instructions in local memory:**

1. **Root Instructions** (this file): `.github/copilot-instructions.md`
2. **Backend Instructions**: `api/.github/instructions/copilot-instructions.md`
3. **Frontend Vue.js Instructions**: `web/.github/instructions/copilot-instructions.md`
4. **Git & MCP Tools Usage**: `.github/instructions/git-mcp-usage.md` (for Jira tickets, commits, workflows)

**Memory Loading Protocol:**

- Read ALL instruction files completely before starting any work (root + backend + frontend)
- Store guidelines, patterns, and standards in local memory
- Apply all established conventions consistently
- Never deviate from the documented patterns

**Git & MCP Tools**: When working with Jira tickets, Git commits, or MCP tool workflows, refer to `.github/instructions/git-mcp-usage.md` for:

- Single-step Jira ticket creation with complete descriptions
- Autonomous commit workflow (extract ticket ID, generate message, commit)
- Quality gates integration (pre-commit validation)
- Performance optimization (parallel operations, minimize round trips)
- Anti-patterns to avoid (blank tickets, interactive commits, repeated tool searches)

## 🚨 NON-NEGOTIABLE QUALITY GATES

**Every piece of generated code MUST pass these checks:**

### Backend Quality Requirements (MANDATORY)

```powershell
# These commands MUST pass before code completion
./gradlew checkstyleMain checkstyleTest   # Checkstyle compliance - NO EXCEPTIONS
./gradlew test                            # All tests pass - NO EXCEPTIONS
./gradlew test jacocoTestReport           # 80% minimum coverage report - NO EXCEPTIONS
./gradlew check                           # Enforces 80% threshold
```

### Frontend Quality Requirements (MANDATORY)

```powershell
# These commands MUST pass before code completion
npm run lint                # ESLint rules - NO EXCEPTIONS
npm run type-check          # TypeScript validation - NO EXCEPTIONS
npm run test:run            # All tests pass - NO EXCEPTIONS
npm run test:coverage       # 80% minimum coverage - NO EXCEPTIONS
```

### Code Generation Rules (ENFORCE ALWAYS)

- **Vue.js/TypeScript**: Follow ESLint rules exactly, use DevExtreme patterns, implement proper TypeScript typing with Composition API
- **Java/Spring Boot**: Follow Checkstyle rules exactly, use SOLID principles, maintain established patterns
- **Test Coverage**: Backend 80% minimum, Frontend 80% minimum - NO COMPROMISES
- **Quality Checks**: All lint and test commands must pass - NO BYPASSING

## Project Overview

Enterprise-grade multi-tenant data integration platform with automated scheduling, monitoring, and modular architecture. Built with Spring Boot 4.0.4 backend and Vue.js 3.5.25 + TypeScript frontend.

**Core Features**:

- **Jira Webhook Integration**: Complete webhook management system with field mapping, connection management, trigger history, and audit logging
- **ArcGIS Integration**: Scheduled batch sync of Kaseware case data to ArcGIS feature layers
- **Notification System**: Async RabbitMQ-driven notifications with per-tenant rules, templates, recipient policies, and real-time SSE browser push
- **Multi-tenant Architecture**: Isolated configurations and data per tenant
- **Real-time Processing**: Quartz-based job scheduling for robust data processing
- **Authentication & Authorization**: Keycloak-based OAuth2/JWT security
- **Secrets Management**: Azure Key Vault integration for secure credential storage

**Active Components** (focus areas):

- **kip-backend**: Multi-module project with **Gradle 9.4.1** — modules: integration-execution-contract, integration-management-service, integration-execution-service
- **web**: Vue.js 3.5.25 + Vite 7.3.0 + DevExtreme 25.2.3 + Pinia 3.0.4

**Temporary Components** (ignore - will be removed):

- `source-endpoint-secured-app/` and `target-endpoint-graphql-app/` - temporary samples

## Development Philosophy & Principles

### SOLID Principles Implementation

- **Single Responsibility**: Services handle one domain (IntegrationService, MasterDataService, CronScheduleService). Controllers focus solely on HTTP concerns
- **Open/Closed**: Extensible via interfaces (BaseEntityService, JobProcessorRegistry, EndpointFieldExtractorRegistry). New integration types added without modifying existing code
- **Liskov Substitution**: All services extend BaseEntityService consistently. Repository abstractions allow seamless implementation switching
- **Interface Segregation**: Separate interfaces for different concerns (CrudRepository, custom query methods). Frontend composables serve single purposes (useAuth, useJiraConnections)
- **Dependency Inversion**: Constructor injection everywhere (@RequiredArgsConstructor). Services depend on abstractions, not implementations

### Maintainability & Extensibility Standards

- **Immutable DTOs**: Use @Builder, @Data, @AllArgsConstructor for consistent data transfer objects
- **Defensive Programming**: Null checks, validation at service boundaries, early returns for invalid states
- **Fail-Fast Design**: Input validation with @Valid, custom exceptions for domain errors (IntegrationPersistenceException, FrequencyTypeNotFoundException)
- **Clear Naming**: Method names indicate intent (validateSourceEndpoint, buildCron, getLastExecutionDetails). No abbreviations or cryptic naming
- **Consistent Error Handling**: @ControllerAdvice for centralized exception handling, standardized error responses

## Architecture & Data Flow

### Backend (Spring Boot 4.0.4 + Java 25)

- **Entry Points**:
  - Management Service: `IntegrationManagementServiceApplication.java` in `integration-management-service/`
  - Execution Service: `IntegrationExecutionServiceApplication.java` in `integration-execution-service/`
- **Multi-Module Structure**: Parent POM manages `integration-execution-contract`, `integration-management-service`, `integration-execution-service`
- **Data Flow**: Controllers → Services (IMS) → Quartz Jobs (IMS) → Feign → Processors/Publishers (IES)
- **Async Messaging**: RabbitMQ (Spring AMQP) — `integration.notification.exchange` (TopicExchange) for notification events; `integration.arcgis.exchange` and `integration.jira.exchange` (DirectExchange) for job commands/results
- **Notification Flow**: `@PublishNotification` AOP → `NotificationEventPublisher` → RabbitMQ → `NotificationListener` → `NotificationDispatchService` → `SseEmitterRegistry` → browser SSE
- **Deployment**: Fat JAR with repackage configuration for both services
- **Database**: PostgreSQL 42.7.7 with JPA/Hibernate 7.1.8.Final
- **Integration**: HTTP clients and REST APIs for external connectivity
- **Security**: OAuth2/JWT resource server with Keycloak integration
- **Scheduling**: Quartz Scheduler in `integration-management-service` — `ArcGISIntegrationJob`, `ArcGISScheduleService`, `QuartzJobReloaderService`, `CronScheduleService`

### Frontend (Vue.js 3.5.25 + TypeScript 5.7.2)

- **Entry Point**: `src/main.ts` → `src/App.vue`
- **State Management**: Pinia 3.0.4 with persistent stores
- **UI Framework**: DevExtreme 25.2.3 with responsive components
- **API Layer**: Auto-generated OpenAPI client with service composables
- **Build Tool**: Vite 7.3.0 with TypeScript, ESLint 9.17.0
- **Authentication**: Keycloak 26.2.0 with token-based multi-tenant support
- **Testing**: Vitest 4.0.9 with Vue Testing Library 8.1.0, 80% coverage target

## Developer Workflows

### Backend Commands (Gradle)

```powershell
# Build all modules (fat JARs in build/libs/)
./gradlew build -x test

# Run management service (port 8085)
./gradlew :integration-management-service:bootRun

# Run execution service (port 8081)
./gradlew :integration-execution-service:bootRun

# Run tests with coverage report
./gradlew test jacocoTestReport

# Enforce 80% coverage threshold
./gradlew check

# Checkstyle on main + test sources
./gradlew checkstyleMain checkstyleTest

# Full build (tests + coverage + quality)
./gradlew build

# Database migration — IMS only
./gradlew :integration-management-service:flywayMigrate
```


### Frontend Commands (npm)

```powershell
# Development server (port 8084)
npm run dev

# Production build with TypeScript compilation
npm run build

# Testing with Vitest (80% coverage target)
npm run test
npm run test:coverage
npm run test:ui

# Code quality
npm run lint
npm run lint:fix
npm run type-check

# Generate API client from OpenAPI spec
npm run generate:api
```

## Key Architectural Patterns

### Backend Conventions

**See `api/.github/instructions/copilot-instructions.md` for detailed backend patterns.**

Key principles:

- **Multi-Tenant Security**: OAuth2/JWT with tenant isolation throughout the application
- **Code Organization**: Feature-based packages (controller/, service/, scheduler/, notification/, etc.)
- **Data Processing Pipeline**: IMS schedules and triggers jobs (Quartz); IES runs Extractor → Processor → Publisher pattern
- **Notification Pipeline**: AOP `@PublishNotification` on IMS/IES service methods → RabbitMQ topic exchange → `NotificationListener` → `NotificationDispatchService` → SSE fan-out to browser
- **Service Layer Inheritance**: BaseEntityService<T, ID> provides common CRUD operations
- **Registry Pattern**: JobProcessorRegistry, EndpointFieldExtractorRegistry for plugin-like extensibility

### Frontend Conventions

- **Component Organization**: Feature-based in `src/components/` (admin/, home/, inbound/, outbound/, layout/, common/)
- **API Integration**: Use auto-generated services from `src/api/services/` with Vue composables
- **State Management**: Pinia stores in `src/store/` with persistent state management
- **Authentication Flow**: Keycloak configuration in `src/config/` with automatic token management
- **Type Safety**: Comprehensive TypeScript throughout with strict configuration
- **Composable Pattern**: Feature-specific composables (useAuth, useJiraConnections, useWebhookActions) for logic reuse
- **Component Composition**: Vue 3 Composition API with `<script setup>` syntax
- **Error Handling Strategy**: Centralized error handling via toast notifications and error boundaries
- **Jira Webhook Pattern**: Complete webhook lifecycle management with DevExtreme components

## Critical Integration Points

### Backend External Dependencies

**See `api/.github/instructions/copilot-instructions.md` for detailed integration patterns.**

- PostgreSQL (multi-tenant), Keycloak (OAuth2/JWT), Quartz Scheduler, Azure Key Vault

### Frontend API Communication

- **Primary API Layer**: Auto-generated OpenAPI client in `src/api/` with services (IntegrationsService, MasterDataService, SchedulerService, SettingsService)
- **State Management**: Pinia stores for reactive state management
- **Authentication**: Token management via `src/config/configureOpenAPI.ts`
- **Cross-Component**: Pinia stores and Vue provide/inject for state sharing

## Essential File Locations

### Backend Key Files

**See `api/.github/instructions/copilot-instructions.md` for complete backend structure.**

- `api/settings.gradle` + `api/build.gradle` + `api/gradle.properties` - Gradle root configuration
- `api/integration-management-service/` - REST API service (port 8085)
- `api/integration-execution-service/` - Processing engine (port 8081)
- `api/integration-execution-contract/` - Shared DTOs and contracts
- **Checkstyle**: `checkstyle.xml` at each module root is read by Gradle via `configFile`. `checkstyle/checkstyle-suppressions.xml` is Gradle's `configDirectory` content for `${config_loc}`. Edit suppressions directly in `checkstyle/checkstyle-suppressions.xml`.

### Frontend Key Files

- `web/package.json`: Dependencies and build scripts
- `web/src/store/`: Pinia store setup and modules
- `web/src/api/`: Auto-generated OpenAPI client (preferred)
- `web/src/components/`: Feature-based Vue components (admin/, home/, layout/, common/)
- `web/src/composables/`: Vue composables for reusable logic (useAuth, useJiraConnections, useWebhookActions)
- `web/src/types/`: TypeScript type definitions
- `web/vite.config.ts`: Build configuration with test setup and 80% coverage threshold
- `web/src/config/keycloak.ts`: Authentication configuration

## Development Best Practices

### Code Quality & Testing

- **Backend**: JaCoCo 80% minimum coverage, Checkstyle enforcement, JUnit 5 + Mockito + Testcontainers
- **Frontend**: Vitest 80% coverage target, ESLint with TypeScript rules, Vue Testing Library for Vue components
- **Always check existing code** before creating new classes, methods, or components - prefer extending/reusing
- **Minimal comments** - file/symbol names should clearly indicate purpose
- **Feature-based organization** - group related functionality together

### Constants & Naming Standards

**See `api/.github/instructions/copilot-instructions.md` (Constants Management & Naming Conventions section) for comprehensive rules.**

**Key Principles (Backend):**

- **No Magic Strings/Numbers**: All business-significant literals must be constants
- **Shared Constants**: 2+ usages → centralize in `IntegrationManagementConstants.java` or `QueueNames.java`
- **Single-Use Constants**: Define locally in the class where needed
- **Meaningful Names**: Full words, NO abbreviations (`integrationId` ✅, `intId` ❌)
- **Static Imports**: Prefer for frequently used constants to improve readability
- **Code Review Enforcement**: GitHub Copilot must flag magic literals and generic names

**Examples:**

```java
// ❌ Bad
if (type.equals("ARCGIS")) { }
String cfg = "ARCGIS_INITIAL_SYNC_START_TIMESTAMP";

// ✅ Good
import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS;
import static com.integration.management.constants.IntegrationManagementConstants.CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;

if (type.equals(INTEGRATION_TYPE_ARCGIS)) { }
String cfg = CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;
```

**Frontend (TypeScript):**

- Use `const` for immutable values
- UPPER_SNAKE_CASE for true constants, camelCase for configuration objects
- Centralize API routes, feature flags, and magic values in dedicated constants files

### Testing Patterns & Standards

**Backend Testing (JUnit 5 + Mockito):**

- See `api/.github/instructions/copilot-instructions.md` for detailed backend testing standards
- 80% minimum coverage with JUnit 5, Mockito, Testcontainers

**Frontend Testing (Vitest + Vue Testing Library):**

- **Component Tests**: Render Vue components with required props, test user interactions
- **Composable Tests**: Test Vue composables in isolation with Vue Test Utils
- **Integration Tests**: Test component trees with Pinia store and router context
- **API Integration**: Mock service calls with consistent error handling patterns
- **Coverage Requirements**: 80% threshold for branches, functions, lines, statements

### Code Architecture Principles

**Backend Design Patterns:**

- See `api/.github/instructions/copilot-instructions.md` for detailed backend patterns
- Repository, Factory, Builder, Template Method, Strategy, Observer patterns

**Frontend Design Patterns:**

- **Composition API**: Vue 3 Composition API with `<script setup>` syntax for reactive logic
- **Composables**: Encapsulate stateful logic (useAuth, useJiraConnections, useWebhookActions)
- **Component Composition**: Single File Components with clear separation of concerns
- **Reactive Patterns**: Vue reactivity system for data binding and state management
- **Provider Pattern**: Vue provide/inject for dependency injection and shared state

### API Development Workflow

- **Backend**: Update OpenAPI spec (`openapi.yaml`) → Generate frontend client → Implement backend endpoint
- **Frontend**: Use auto-generated services from `src/api/services/` with Vue composables for state management
- **Authentication**: Leverage existing OAuth2/JWT integration - tokens managed automatically

### Error Handling & Validation

**Backend Error Strategy:**

- See `api/.github/instructions/copilot-instructions.md` for detailed error handling patterns
- Custom exceptions, @ControllerAdvice, Jakarta validation

**Frontend Error Strategy:**

- **API Error Handling**: Consistent error types from OpenAPI client
- **User Feedback**: Toast notifications for user-facing errors
- **Error Boundaries**: Vue error handlers for component failure isolation
- **Form Validation**: Real-time validation with clear error messages

### Performance & Scalability

**Backend Optimization:**

- See `api/.github/instructions/copilot-instructions.md` for detailed optimization strategies
- Caching, database optimization, async processing, connection pooling

**Frontend Optimization:**

- **Code Splitting**: Route-based lazy loading with Vue dynamic imports
- **Reactivity Optimization**: Computed properties, watch functions for efficient updates
- **Bundle Optimization**: Vite configuration for optimal bundle sizes
- **State Normalization**: Pinia stores for efficient state updates

---

## 📝 INSTRUCTION FILES REFERENCE

**Always load all instruction files before any code generation:**

- **Root Instructions** (this file): `.github/copilot-instructions.md` - Architecture, project overview, SOLID principles, quality gates
- **Backend Instructions**: `api/.github/instructions/copilot-instructions.md` - Multi-module structure, Spring Boot patterns, database, deployment
- **Frontend Vue.js Instructions**: `web/.github/instructions/copilot-instructions.md` - Vue.js/TypeScript patterns

## 🧭 PROMPT GOVERNANCE

- Canonical runnable prompts live under `.github/prompts/` with domain folders: `api/`, `web/`, `e2e/`, `cross-cutting/`.
- `packages/prompt-library/` is a documentation mirror and onboarding library, not the execution source of truth.
- Legacy module-local prompt locations (for example `api/.github/prompts/`) are temporary compatibility copies and should point to canonical root prompts.

**Additional Backend Docs**: See `api/README.md` for detailed documentation

## 🎯 MEMORY CHECKPOINT

Before generating code, confirm you have loaded and stored:

- ✅ Project architecture & SOLID principles
- ✅ Multi-module structure: integration-execution-contract (DTOs), integration-management-service (API), integration-execution-service (processing)
- ✅ Backend Java/Spring Boot 4.0.4/PostgreSQL patterns & quality gates
- ✅ Frontend Vue.js/TypeScript patterns & quality gates
- ✅ Test coverage requirements (Backend 80%, Frontend 80%)
- ✅ Code quality requirements (Checkstyle, ESLint)
- ✅ Fat JAR deployment pattern with ports 8085 (management) and 8081 (execution)
- ✅ RabbitMQ notification pipeline: `@PublishNotification` AOP → TopicExchange → `NotificationListener` → `NotificationDispatchService` → SSE
- ✅ Notification frontend: `useNotifications` (SSE), `useNotificationStore` (Pinia), `UserNotificationService` (hand-crafted API), `NotificationBell` (Lucide SVG)

_Focus on `api/`, `web/`, and `e2e/` components. Ignore source-endpoint-secured-app and target-endpoint-graphql-app as they are temporary and will be removed._

```

```
