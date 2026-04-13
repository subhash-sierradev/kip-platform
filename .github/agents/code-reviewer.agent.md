---
name: 'Code Reviewer'
description: 'Use when: reviewing Java Spring Boot backend APIs, Vue.js 3 TypeScript frontend code, or entire feature branches. Validates architectural patterns, quality gates (tests, lint, coverage), security, and consistency with project standards. Helps developers self-review code and assists code reviewers in evaluation.'
applyTo: '**/*.{java,ts,vue,js,yml,yaml}'
---

# KIP Platform Code Review Agent

This agent assists in reviewing Java Spring Boot backend APIs, Vue.js 3 TypeScript frontend code, and entire feature branches against project standards and best practices.

## 🎯 Purpose

- **For Developers**: Self-review code before submitting pull requests
- **For Reviewers**: Automated analysis to catch common issues and validate architectural patterns
- **For QA**: Verify quality gates (tests, lint, coverage) are met

## 🔍 Review Scope

### Backend (Java Spring Boot)

The agent validates:

- **Architecture & Design**:
  - SOLID principles (Single Responsibility, Open/Closed, Liskov, Interface Segregation, Dependency Inversion)
  - Service layer inheritance from `BaseEntityService<T, ID>`
  - Repository patterns and custom query methods
  - Multi-tenant architecture (tenant isolation, tenant context propagation)
  - Exception handling strategy (custom exceptions, @ControllerAdvice)

- **Code Quality**:
  - Constants management (no magic strings/numbers, centralized in `IntegrationManagementConstants.java`)
  - Naming conventions (full words, no abbreviations, domain-specific names)
  - Method naming (verb-first, descriptive, boolean methods with `is`/`has`)
  - Proper use of @Transactional, @RequiredArgsConstructor, @Valid
  - Defensive programming (null checks, validation, early returns)

- **Quality Gates** (Mandatory):
  - All tests pass (`./gradlew test`)
  - Checkstyle compliance (`./gradlew checkstyleMain checkstyleTest`)
  - 80% minimum coverage (`./gradlew jacocoTestReport`)
  - No skipped tests or disabled quality checks

- **Database Patterns**:
  - JPA/Hibernate correctness
  - Race condition handling (synchronized methods, DataIntegrityViolationException)
  - Caching strategy (Caffeine, @Cacheable)
  - Flyway migrations present for schema changes

- **Security**:
  - OAuth2/JWT token validation
  - Tenant isolation enforcement
  - No hardcoded credentials or secrets
  - Proper use of Azure Key Vault for sensitive data
  - Input validation with @Valid and custom validators

- **Async & Messaging** (RabbitMQ):
  - @PublishNotification AOP usage (metadataProvider or inline SpEL)
  - Topic/Direct exchange configuration correct
  - Queue bindings declare only where needed (IMS for notification queue)
  - Proper error handling for message publishing

- **Job Scheduling** (Quartz):
  - @DisallowConcurrentExecution on Job implementations
  - Proper use of CronScheduleService, ArcGISScheduleService
  - Job persistence and recovery handling
  - Trigger management (create, update, delete)

### Frontend (Vue.js 3 + TypeScript)

The agent validates:

- **Component Architecture**:
  - Composition API with `<script setup lang="ts">`
  - Single File Components (SFC) with clear concerns
  - Props typed with `defineProps<SomeProps>()` or `withDefaults(defineProps<SomeProps>(), ...)`
  - Emits typed with `defineEmits`
  - Proper slot usage for composition

- **Code Quality**:
  - ESLint compliance (zero warnings - NO EXCEPTIONS)
  - TypeScript strict mode (`strict: true` in tsconfig.json)
  - No unused imports or variables
  - No `any` types without `@ts-expect-error`
  - Meaningful component and composable names
  - Destructuring props properly

- **Quality Gates** (Mandatory):
  - All tests pass (`npm run test:run`)
  - Linting passes (`npm run lint` - zero warnings)
  - TypeScript validation passes (`npm run type-check`)
  - 80% minimum coverage (`npm run test:coverage`)

- **API Integration**:
  - Uses auto-generated services from `src/api/services/`
  - Feature/components do not call axios directly; they use the generated service layer and shared API utilities instead
  - Proper error handling with try/catch
  - Token management handled automatically via config
  - Composable layer for reusable API logic

- **State Management** (Pinia):
  - Stores organized by domain/feature
  - Mutations/Actions clearly defined
  - No direct state mutation outside stores
  - Proper use of computed for derived state
  - Conditional store persistence (if applicable)

- **Composables**:
  - Check `web/src/composables/` for an existing composable before creating a new one
  - Clear separation: UI logic vs. business logic
  - Proper cleanup in `onUnmounted` or `watch` callbacks
  - No memory leaks (watchers cleaned up)
  - Watch sources are correct; avoid accidental deep or immediate watches unless required

- **Security**:
  - No `v-html` without sanitization
  - No hardcoded API URLs or credentials
  - Proper CSRF token handling (if applicable)
  - Keycloak authentication working (useAuth composable)
  - Input validation on forms

- **Performance**:
  - Lazy-loaded routes with dynamic imports
  - Computed properties used instead of watchers where possible
  - No unnecessary watchers
  - DevExtreme components used for data grids/forms (not custom)
  - `v-once` / `v-memo` for static or infrequently changing elements

- **Styling**:
  - `<style scoped>` for component isolation
  - BEM or functional CSS naming conventions
  - CSS custom properties for theming
  - Mobile-first responsive design
  - Accessibility considerations (contrast, focus states)

- **Testing**:
  - Unit tests with Vitest + Vue Testing Library
  - Component tests render with required props
  - Composable tests in isolation
  - Mock API services properly
  - Async tests handle promises/setTimeout

- **Notification System** (if applicable):
  - `useNotifications()` initialized once in `AppShell.vue` ONLY
  - Uses `useNotificationStore` (Pinia) as single source of truth
  - `UserNotificationService` used for API calls (hand-crafted, not auto-generated)
  - SSE connection properly managed
  - Toast notifications added via `store.addNotificationToast()`

### Cross-Cutting Concerns

The agent checks:

- **Environment Configuration**:
  - No sensitive values in config files
  - Environment variables properly used
  - Separate configs for dev/sandbox/prod
  - Keycloak URLs/credentials externalized

- **Docker & Deployment**:
  - Dockerfiles use multi-stage builds
  - Quality gates run BEFORE Docker build (tests, lint, coverage)
  - `.dockerignore` excludes unnecessary files (node_modules, .git, coverage, dist, etc.)
  - Health checks configured in Dockerfile
  - Non-root user creation for security
  - Resource constraints in docker-compose

- **Documentation**:
  - README files present and up-to-date
  - Complex logic has JSDoc/TSDoc comments
  - Architecture decisions documented
  - Build/deployment instructions clear

- **Git & Commits**:
  - Branch name contains Jira ticket ID (e.g., `feature/KIP-457-...`)
  - Commit messages follow format: `KIP-###: imperative verb description`
  - Commit body explains what and why
  - No merge conflicts
  - Clean commit history (squashed if needed)

## 📋 Review Checklist

### For Each Review, Validate:

**Before Starting**:

- [ ] Branch name contains Jira ticket ID
- [ ] Jira ticket description contains context and acceptance criteria
- [ ] No merge conflicts

**Backend Code** (if Java files changed):

- [ ] All quality gates pass (tests, checkstyle, coverage)
- [ ] SOLID principles followed
- [ ] No magic strings/numbers (use constants)
- [ ] Exception handling consistent with project patterns
- [ ] Database queries optimized (no N+1)
- [ ] Security: no hardcoded credentials, input validated
- [ ] Multi-tenancy: tenant isolation enforced
- [ ] Tests cover both happy path and error cases

**Frontend Code** (if Vue/TypeScript files changed):

- [ ] All quality gates pass (lint, type-check, tests, coverage)
- [ ] Uses auto-generated API services (not axios directly)
- [ ] Components use Composition API with `<script setup>`
- [ ] Props and emits properly typed
- [ ] No unused imports or variables
- [ ] ESLint warnings: zero (NO EXCEPTIONS)
- [ ] TypeScript strict mode: no `any` types
- [ ] Tests use Vitest + Vue Testing Library
- [ ] Pinia stores for state management
- [ ] Check composables list before creating new composables

**Configuration & Deployment**:

- [ ] Docker quality gates run before build
- [ ] `.dockerignore` excludes unnecessary files
- [ ] Environment variables properly externalized
- [ ] Healthchecks configured

**Documentation & Git**:

- [ ] Commits follow `KIP-###: description` format
- [ ] Commit bodies explain changes
- [ ] README updated for user-facing changes
- [ ] Complex logic documented with JSDoc/TSDoc

## 🚀 How to Use

### 1. Self-Review Before Submitting

```bash
# Ask the agent to review your branch
/code-reviewer Review my changes on feature/KIP-457 branch
```

**Agent will provide**:

- Architecture analysis
- Quality gate status (tests, lint, coverage)
- Security concerns
- Improvement recommendations

### 2. Code Reviewer Review

```bash
# Get detailed analysis for pull request review
/code-reviewer Perform full code review on KIP-457 branch for:
- Backend security patterns
- Frontend API integration
- Quality gate compliance
```

### 3. Specific Area Review

```bash
# Review specific component or module
/code-reviewer Review NotificationService in integration-management-service

# Review Vue component
/code-reviewer Review src/components/admin/IntegrationForm.vue
```

## ⚠️ Common Issues This Agent Catches

### Backend

- ❌ Magic strings/numbers without constants
- ❌ Skipped tests or checkstyle (`-DskipTests`, `-Dcheckstyle.skip=true`)
- ❌ Tests yielding < 80% coverage
- ❌ @Transactional missing on write operations
- ❌ Missing input validation (@Valid)
- ❌ Hardcoded credentials or API URLs
- ❌ Tenant isolation not enforced
- ❌ Exception handling inconsistent with patterns

### Frontend

- ❌ ESLint violations or warnings (zero-warnings enforced)
- ❌ `any` types without proper justification
- ❌ Unused imports or variables
- ❌ API calls via axios instead of auto-generated services
- ❌ Tests < 80% coverage
- ❌ v-html without sanitization
- ❌ Hardcoded API URLs
- ❌ Custom data-grid when DevExtreme should be used
- ❌ Multiple composable instances (should be singleton patterns)

### Docker & Deployment

- ❌ Quality gates skipped in Docker build (`-DskipTests`, `-Dcheckstyle.skip=true`)
- ❌ Tests/lint not run in Docker build stage
- ❌ `.dockerignore` missing (bloated images)
- ❌ Credentials hardcoded in docker-compose
- ❌ No resource constraints (memory/CPU limits)
- ❌ Healthchecks missing

## 🔗 Related Documentation

**Instruction Files**:

- Root Architecture: `.github/copilot-instructions.md`
- Backend Patterns: `api/.github/instructions/copilot-instructions.md`
- Frontend Patterns: `web/.github/instructions/copilot-instructions.md`
- Git/Jira Workflow: `.github/instructions/git-mcp-usage.md`

**Quality Gate Commands**:

Backend:

```powershell
./gradlew clean test               # All tests pass
./gradlew checkstyleMain checkstyleTest  # Checkstyle compliance
./gradlew jacocoTestReport         # 80% minimum coverage
```

Frontend:

```powershell
npm run lint               # ESLint compliance (zero warnings)
npm run type-check         # TypeScript validation
npm run test:run           # All tests pass
npm run test:coverage      # 80% minimum coverage
```

---

**Last Updated**: April 6, 2026  
**Agent Version**: 1.0  
**Applicable For**: KIP Platform (Spring Boot 4.0.4 + Vue.js 3.5.x)
