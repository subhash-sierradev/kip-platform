# Backend Instructions - Kaseware Integration Platform

## Multi-Module Architecture

### Module Overview

```
api/
├── integration-execution-contract/      # Shared DTOs and contracts
├── integration-management-service/      # Configuration API (Port 8085)
└── integration-execution-service/       # Processing Engine (Port 8081)
```

### Module Responsibilities

#### integration-execution-contract

- **Purpose**: Lightweight contract module with shared DTOs
- **Dependencies**: Minimal (jakarta.validation-api, jackson-annotations, lombok, hibernate-validator)
- **No Tests**: This module contains only DTOs, no business logic
- **No Fat JAR**: Library module, not executable
- **Checkstyle**: Minimal rules (formatting + naming only, 75 lines)

#### integration-management-service

- **Purpose**: User-facing REST APIs for configuration and monitoring
- **Port**: 8085
- **Deployment**: Fat JAR with repackage goal
- **Main Class**: `com.integration.management.IntegrationManagementServiceApplication`
- **Responsibilities**:
  - CRUD operations for integrations, schedules, master data
  - Quartz job scheduling and execution (`ArcGISIntegrationJob`, `ArcGISScheduleService`, `QuartzJobReloaderService`, `CronScheduleService`)
  - Feign clients to communicate with execution service
  - Health checks and monitoring endpoints
  - User profile and tenant management

#### integration-execution-service

- **Purpose**: Background processing engine for data integration
- **Port**: 8081
- **Deployment**: Fat JAR with repackage goal
- **Main Class**: `com.integration.execution.IntegrationExecutionServiceApplication`
- **Responsibilities**:
  - Webhook event processing
  - Data extraction, transformation, publishing
  - External API integration (Jira, ArcGIS, Kaseware)
  - Vault lifecycle management (Azure Key Vault)

---

## Development Standards

### Spring Boot Configuration

#### Fat JAR Build (Management & Execution Services)

```kotlin
// build.gradle.kts
springBoot {
    mainClass.set("com.integration.[service].Application")
}

tasks.bootJar {
    archiveClassifier.set("")
}
```

#### Multi-Environment Profiles

- `application.yml` - Base configuration
- `application-dev.yml` - Development (ddl-auto: none)
- `application-sandbox.yml` - Sandbox (ddl-auto: none, debug logging)
- `application-prod.yml` - Production (ddl-auto: validate)

**Profile Activation**:

```bash
./gradlew :integration-management-service:bootRun --args='--spring.profiles.active=sandbox'
java -jar service.jar --spring.profiles.active=prod
```

### Code Organization

#### Package Structure

```
com.integration.[service]/
├── config/              # Spring configuration classes
├── controller/          # REST endpoints (@RestController)
├── service/             # Business logic (@Service)
├── job/                 # Quartz Job implementations (management service only)
├── extractor/           # Data extraction (execution service only)
├── processor/           # Data transformation (execution service only)
├── publisher/           # Data publishing (execution service only)
├── entity/              # JPA entities (@Entity)
├── repository/          # Data access (@Repository)
├── exception/           # Custom exceptions
└── [Service]Application.java  # Main class
```

#### Contract Module Structure

```
com.integration.contract/
├── dto/                 # Data Transfer Objects
│   ├── request/         # API request DTOs
│   └── response/        # API response DTOs
└── enums/               # Shared enumerations
```

### Entity Design Patterns

#### Base Entity

All entities extend `BaseEntity`:

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    protected String tenantId;
    protected String createdBy;
    protected LocalDateTime createdAt;
    protected String updatedBy;
    protected LocalDateTime updatedAt;

    @Version
    protected Long version;
}
```

#### Multi-Tenancy

- All entities include `tenantId` field
- Automatic tenant filtering in repositories
- Tenant isolation enforced at service layer

### Service Layer Patterns

#### BaseEntityService

Common CRUD operations with lifecycle hooks:

```java
public abstract class BaseEntityService<T extends BaseEntity, ID> {
    protected abstract void initializeForCreate(T entity);
    protected abstract void prepareForUpdate(T entity, T existing);

    public T create(T entity) {
        initializeForCreate(entity);
        return repository.save(entity);
    }
}
```

#### Service Conventions

- Constructor injection with `@RequiredArgsConstructor`
- `@Transactional` for write operations
- Synchronized methods for concurrent creation (tenant/user profiles)
- Exception handling with custom domain exceptions

### Constants Management & Naming Conventions

#### Constants Organization Rules

**Shared Constants (2+ usages across classes):**

Centralize in constants classes:

- `IntegrationManagementConstants.java` — IMS shared constants
- `IntegrationExecutionConstants.java` — IES shared constants (when needed)
- `QueueNames.java` (in contract module) — RabbitMQ exchanges/queues/routing keys

**Single-Use Constants:**

Define locally in the class where needed:

```java
@Service
public class MyService {
    private static final int DEFAULT_PAGE_SIZE = 20;  // Only used here
    private static final String CACHE_NAME = "myServiceCache";  // Only used here
}
```

**Constants Structure:**

Group logically with clear section headers:

```java
public final class IntegrationManagementConstants {

    // ======================================================
    // Integration Type Constants
    // ======================================================
    public static final String INTEGRATION_TYPE_ARCGIS = "ARCGIS";
    public static final String INTEGRATION_TYPE_CONFLUENCE = "CONFLUENCE";

    // ===============================================
    // Site Configuration Keys for Initial Sync
    // ===============================================
    public static final String CONFIG_KEY_ARCGIS_INITIAL_SYNC_START = "ARCGIS_INITIAL_SYNC_START_TIMESTAMP";
    public static final String CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START = "CONFLUENCE_INITIAL_SYNC_START_TIMESTAMP";

    // Nested classes for domain grouping
    public static final class Security {
        public static final String GLOBAL = "GLOBAL";
        public static final String SYSTEM_USER = "system";
        private Security() {}
    }

    private IntegrationManagementConstants() {}
}
```

**Static Imports:**

Prefer static imports for frequently used constants to improve readability:

```java
import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS;
import static com.integration.management.constants.IntegrationManagementConstants.CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;

public class ArcGISIntegrationJob {
    public void execute() {
        String type = INTEGRATION_TYPE_ARCGIS;  // Clean, no class prefix
        String config = CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;
    }
}
```

#### String/Number Literals Rule

**NEVER use raw string or number literals for business-significant values.**

❌ **Bad Examples:**

```java
if (integrationType.equals("ARCGIS")) { }           // Magic string
int maxRetries = 3;                                  // Magic number
String config = "ARCGIS_INITIAL_SYNC_START_TIMESTAMP";  // Magic string
```

✅ **Good Examples:**

```java
if (integrationType.equals(INTEGRATION_TYPE_ARCGIS)) { }
int maxRetries = MAX_RETRY_ATTEMPTS;
String config = CONFIG_KEY_ARCGIS_INITIAL_SYNC_START;
```

**Exceptions (allowed raw literals):**

- Common numbers: `0`, `1`, `-1`, `100` (for percentages)
- Empty strings: `""` (but prefer `StringUtils.EMPTY` for clarity)
- Array indices: `array[0]`
- Collection sizes: `new ArrayList<>(10)`

#### Meaningful Naming Conventions

**Class Names:**

- Clear domain intent: `IntegrationJobExecutionService`, `ExecutionWindowResolverService`
- NO generic suffixes without domain context: ❌ `DataService`, ❌ `HelperService`
- ✅ `ArcGISIntegrationService`, ✅ `ConfluenceScheduleService`

**Method Names:**

- Verb-first, descriptive action:
  - ✅ `findLatestRetriableConfluenceExecution`
  - ✅ `createConfluenceRetryExecution`
  - ❌ `getExecution` (too vague)
  - ❌ `processData` (too generic)
- Boolean methods: `isEnabled`, `hasActiveSchedule`, `canRetry`
- Validation: `validateSourceEndpoint`, `validateFieldMappings`

**Variable Names:**

- Full words, NO abbreviations:
  - ✅ `integrationId`, `executionWindow`, `scheduledFireTime`
  - ❌ `intId`, `execWin`, `schTime`
- Exceptions: Standard Java abbreviations (`id`, `url`, `dto`, `uri`)

**Constant Names:**

- Descriptive, context-specific:
  - ✅ `INTEGRATION_TYPE_ARCGIS`, `CONFIG_KEY_CONFLUENCE_INITIAL_SYNC_START`
  - ✅ `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT_SECONDS`
  - ❌ `TYPE`, `CONFIG`, `MAX`, `DEFAULT` (too vague without context)
- Prefix with domain when needed: `ARCGIS_`, `CONFLUENCE_`, `NOTIFICATION_`

**Package Names:**

- Feature-based, single-word lowercase (or multi-word when necessary):
  - ✅ `service`, `controller`, `repository`, `scheduler`, `notification`
  - ❌ `utils`, `helpers`, `common` (avoid generic utility packages)

#### Code Review Enforcement

**GitHub Copilot Reviews MUST flag:**

1. **Magic Strings/Numbers**: Any non-constant business value
   - Example: `"ARCGIS"` in switch/if statements → suggest `INTEGRATION_TYPE_ARCGIS`
2. **Generic Names**: Classes/methods with vague or abbreviated names
   - Example: `DataService` → suggest domain-specific name
3. **Duplicate Constants**: Same literal value in 2+ classes
   - Example: `"ARCGIS"` in multiple files → suggest shared constant
4. **Missing Static Imports**: Constants used with full class prefix
   - Example: `IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS` → suggest static import

**Review Template:**

```markdown
❌ **Constants Violation**
Line 45: Magic string "ARCGIS" should be constant `INTEGRATION_TYPE_ARCGIS`

**Fix:**

1. Import: `import static com.integration.management.constants.IntegrationManagementConstants.INTEGRATION_TYPE_ARCGIS;`
2. Replace: `"ARCGIS"` → `INTEGRATION_TYPE_ARCGIS`
```

### Security Configuration

#### CORS Setup

```java
@Configuration
public class SecurityConfig {
    // Null-safe CORS handling with default values
    private final SecurityProperties securityProperties;

    // Default arrays in SecurityProperties.Cors:
    // allowedOriginPatterns = new String[]{"*"}
    // allowedMethods = new String[]{"GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"}
}
```

#### OAuth2 Resource Server

- Keycloak JWT validation
- Tenant extraction from JWT claims
- Multi-tenant context propagation

### Database Patterns

#### JPA/Hibernate

- PostgreSQL 42.7.7
- Hibernate 7.1.8.Final
- Schema management: `ddl-auto: none` (dev/sandbox), `validate` (prod)
- Flyway 11.20.0 migrations in `src/main/resources/db/migration/`

#### Race Condition Handling

```java
@Service
public class TenantProfileService {
    @Transactional
    public synchronized TenantProfile getOrCreateTenantProfile(String tenantId) {
        try {
            return repository.save(new TenantProfile(tenantId));
        } catch (DataIntegrityViolationException e) {
            return repository.findByTenantId(tenantId).orElseThrow();
        }
    }
}
```

#### Caching Strategy

- Caffeine cache with 24-hour expiry
- Cache tenant profiles and user profiles
- `@Cacheable` annotations with proper cache keys

### Master Data Configuration

#### Location

Each service has its own master data:

- `integration-management-service/src/main/resources/master-data/`
- `integration-execution-service/src/main/resources/master-data/`

#### Files

- `credential-types.json` - Supported credential types
- `frequency-patterns.json` - Schedule frequency patterns
- `integration-types.json` - Integration type definitions
- `site-config.json` - Site-specific configuration

### Job Scheduling (Management Service)

#### Quartz Configuration

- Clustered mode for high availability
- JDBC job store in PostgreSQL
- All Quartz classes live in `integration-management-service` under `job/` and `service/`:
  - `ArcGISIntegrationJob` — Quartz `Job` implementation (`@DisallowConcurrentExecution`)
  - `ArcGISScheduleService` — schedules, updates, and deletes Quartz triggers
  - `QuartzJobReloaderService` — reloads persisted jobs on startup
  - `CronScheduleService` — cron expression building and validation

#### Job Processing Pipeline

1. **Extractor** - Fetch data from source system
2. **Processor** - Transform data (JOLT, custom logic)
3. **Publisher** - Send data to target system

#### Registry Pattern

```java
@Component
public class JobProcessorRegistry {
    public JobProcessor getProcessor(String integrationType) {
        // Factory pattern for integration type selection
    }
}
```

### Notification System (RabbitMQ + SSE)

#### RabbitMQ Topology

- All constants in `integration-execution-contract/.../queue/QueueNames.java` (shared by both services)
- `integration.notification.exchange` — `TopicExchange` (durable); routing key `notification.event`
- `integration.notification.ims` — durable queue declared **in IMS only**; IES publishes only (no binding)
- IMS `RabbitMQConfig` declares all exchanges, queues, and bindings
- IES `NotificationExchangeConfig` declares the topic exchange only (publish-only side)

#### AOP Publishing Pattern (`@PublishNotification`)

```java
// Provider strategy: pre-fetches entity data BEFORE proceed() — use for delete/toggle
@PublishNotification(
    metadataProvider = "arcGISNotificationMetadataProvider",
    entityId = "#integrationId"
)
public void deleteIntegration(String integrationId) { ... }

// Inline SpEL strategy: evaluated AFTER proceed() using #result
@PublishNotification(metadata = "{'integrationName': #result.name}")
public ArcGISIntegration createIntegration(...) { ... }
```

Three `NotificationMetadataProvider` beans: `arcGISNotificationMetadataProvider`, `jiraWebhookNotificationMetadataProvider`, `siteConfigNotificationMetadataProvider`.

#### Dispatch Flow

`NotificationListener` (@RabbitListener on `integration.notification.ims`) → `NotificationDispatchService`:

1. Look up enabled `NotificationRule` for `(eventKey, tenantId)`
2. Resolve recipients from `NotificationRecipientPolicy` (`ALL_USERS` / `ADMINS_ONLY` / `SELECTED_USERS`)
3. Render `NotificationTemplate` using `{{placeholder}}` substitution from `metadata` map
4. Persist `AppNotification` per target user
5. `SseEmitterRegistry.send(userId, payload)` — fans out to all open tabs

#### SSE Infrastructure

- Endpoint: `GET /api/management/notifications/stream` → `text/event-stream`
- `SseEmitterRegistry`: `ConcurrentHashMap<userId, CopyOnWriteArrayList<SseEmitter>>`
- Emitter timeout: **30 minutes**
- `@Scheduled(fixedDelay = 25_000)` heartbeat keeps connections alive and prunes dead emitters
- Sends comment-only `heartbeat` events; real notifications use event name `"notification"`

#### REST Endpoints (`/api/management/notifications/`)

```
GET    /                    paginated notification list
GET    /count               unread count
PATCH  /read                mark list of IDs as read
GET    /stream              SSE stream (text/event-stream)
GET    /events/             event catalog (22 event types)
GET|POST /rules/            per-tenant rules
DELETE /rules/{id}
PATCH  /rules/{id}/toggle
POST   /rules/batch         batch-create all remaining events
GET|POST /templates/        per-tenant message templates
GET|POST|DELETE /policies/  recipient policies per rule
```

#### Entities (`notifications` DB schema)

`AppNotification` · `NotificationRule` · `NotificationEventCatalog` · `NotificationTemplate` · `NotificationRecipientPolicy` · `NotificationRecipientUser` · `NotificationEventLog`

#### Enums

`NotificationType` (22 values) · `NotificationSeverity` (INFO/WARNING/ERROR/SUCCESS) · `NotificationEntityType` · `RecipientType` (ALL_USERS/ADMINS_ONLY/SELECTED_USERS)

#### Contract (`integration-execution-contract`)

- `NotificationEvent` — RabbitMQ message DTO: `eventKey`, `tenantId`, `triggeredByUserId`, `metadata`
- `QueueNames` — all exchange, queue, and routing key constants

#### Custom Exceptions

- `IntegrationPersistenceException` - Database errors
- `FrequencyTypeNotFoundException` - Schedule errors
- `CredentialNotFoundException` - Authentication errors
- Extend `RuntimeException` for unchecked exceptions

#### Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IntegrationPersistenceException.class)
    public ResponseEntity<ErrorResponse> handlePersistence(Exception e) {
        // Standardized error responses
    }
}
```

### Testing Standards

#### Unit Tests

- JUnit 5 with `@ExtendWith(MockitoExtension.class)`
- Mock dependencies with `@Mock`, inject with `@InjectMocks`
- Test single units of functionality
- 80% minimum coverage

#### Integration Tests

- `@SpringBootTest` with `@ActiveProfiles("test")`
- Testcontainers for PostgreSQL
- Full Spring context for end-to-end testing

#### Test Naming

```java
// Pattern: methodName_condition_expectedResult
void createIntegration_validInput_returnsCreatedIntegration()
void getIntegration_nonExistent_throwsNotFoundException()
```

### Code Quality

#### Checkstyle

- Full rules for services (181 lines)
- Minimal rules for contract module (75 lines - formatting + naming only)
- Line length: 120 characters
- Run: `./gradlew checkstyleMain checkstyleTest`

#### EditorConfig

If an `.editorconfig` is present, use these settings:

- Java: 4 spaces, max line 120
- YAML: 2 spaces
- UTF-8, LF line endings

### Build & Deployment

#### Gradle Commands

```bash
# Build all modules (order handled automatically by Gradle)
./gradlew clean build

# Build specific module
./gradlew :integration-execution-contract:build

# Run management service (port 8085)
./gradlew :integration-management-service:bootRun

# Run execution service (port 8081)
./gradlew :integration-execution-service:bootRun

# Fat JAR deployment
./gradlew clean bootJar
java -jar integration-management-service/build/libs/integration-management-service-<version>.jar
java -jar integration-execution-service/build/libs/integration-execution-service-<version>.jar

# Tests with coverage
./gradlew test jacocoTestReport

# Full verification
./gradlew clean check
```

#### Version Management

Project versions are managed in `gradle.properties` (`projectVersion` and `contractVersion`).
The version catalog (`gradle/libs.versions.toml`) manages all dependency versions centrally.

#### Deployment Checklist

- ✅ Both services build as fat JARs
- ✅ Checkstyle passes (0 violations)
- ✅ All tests pass
- ✅ Coverage ≥80%
- ✅ Environment variables configured
- ✅ Database migrations applied
- ✅ Parent version managed in gradle.properties (never bump root independently)

---

## Common Issues & Solutions

### Issue: "Invalid enum value: target values must be absolute"

**Cause**: Using `HttpStatus` class with `@Enumerated` annotation  
**Workaround**: Set `ddl-auto: none` instead of `validate`  
**Proper Fix**: Change field type from `HttpStatus` to `String`  
**Status**: Technical debt - marked with TODO comments in `application-sandbox.yml`

### Issue: Duplicate key violations during concurrent creation

**Cause**: Race condition with batch inserts and unique constraints  
**Solution**: Synchronized methods + `DataIntegrityViolationException` handling + double-check pattern

### Issue: CORS NullPointerException

**Cause**: `SecurityProperties.Cors` arrays can be null when not in YAML  
**Solution**: Null-safe checks + default array values in `@ConfigurationProperties`

### Issue: Fat JAR not created

**Cause**: Missing `bootJar` task or incorrect `archiveClassifier`  
**Solution**: Ensure `springBoot { mainClass.set(...) }` and `tasks.bootJar { archiveClassifier.set("") }` in `build.gradle.kts`

---

## Technical Debt

### Current Items

1. **HttpStatus Enum Issue**: JiraWebhookEvent uses `HttpStatus` with `@Enumerated` - fails schema validation
2. **Test Coverage**: Currently 39%, target 80%
3. **Code Documentation**: Add JavaDoc for public APIs
4. **Error Messages**: Standardize error response formats

### Future Improvements

- Circuit breaker pattern for external API calls
- Distributed tracing (OpenTelemetry)
- API rate limiting
- Webhook retry mechanism with exponential backoff

---

## References

- **Root README**: `README.md` - Quick start and overview
- **Root Instructions**: `.github/copilot-instructions.md` - Cross-cutting concerns and quality gates
- **Canonical Prompts**: `.github/prompts/api/` - source of truth for backend Copilot prompts
- **Legacy Prompt Location**: `api/.github/prompts/` - compatibility only; prefer canonical root prompts

### Key Dependency Versions

| Dependency               | Version                                            |
| ------------------------ | -------------------------------------------------- |
| Spring Boot              | 4.0.4                                              |
| Spring Cloud (OpenFeign) | 2025.1.1                                           |
| Java                     | 25                                                 |
| PostgreSQL driver        | 42.7.7                                             |
| Hibernate                | 7.1.8.Final                                        |
| Flyway                   | 11.20.0                                            |
| Lombok                   | 1.18.42                                            |
| MapStruct                | 1.6.3                                              |
| Caffeine                 | 3.2.3                                              |
| Azure Key Vault          | 4.10.4                                             |
| Azure Identity           | 1.18.1                                             |
| Resilience4j             | 2.3.0                                              |
| Spring Retry             | 2.0.12                                             |
| JOLT                     | 0.1.8                                              |
| WireMock                 | 3.13.1                                             |
| Testcontainers           | 1.20.4                                             |
| Spring AMQP (RabbitMQ)   | via `spring-boot-starter-amqp` (Spring Boot 4.0.4) |

### Exception Class Note

The 11 exception classes (`IntegrationBaseException`, `IntegrationApiException`, `IntegrationExecutionException`, `IntegrationNotFoundException`, `IntegrationNameAlreadyExistsException`, `IntegrationPersistenceException`, `SchedulingException`, `FieldMappingProcessingException`, `EndpointValidationException`, `CacheNotFoundException`, `AzureKeyVaultException`) are **currently duplicated** in both `integration-management-service/exception/` and `integration-execution-service/exception/`. The planned consolidation is to move them into `integration-execution-contract` — see `REFACTORING-PLAN.md` for details.
