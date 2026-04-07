# Integration Platform Frontend

Enterprise-grade Vue 3 + TypeScript UI for the Kaseware Integration Platform — multi-tenant Jira webhook management, ArcGIS integration orchestration, and real-time job monitoring.

---

## Tech Stack

| Component | Technology |
|---|---|
| **Framework** | Vue.js 3.5.25 + TypeScript 5.7.2 |
| **Build Tool** | Vite 7.3.0 |
| **UI Components** | DevExtreme 25.2.3 |
| **State Management** | Pinia 3.0.4 |
| **Routing** | Vue Router 4.6.3 |
| **Authentication** | Keycloak 26.2.0 (fully integrated) |
| **HTTP Client** | Axios 1.13.2 |
| **Utilities** | @vueuse/core 14.1.0 |
| **Icons** | lucide-vue-next 0.553.0 |
| **Scheduling** | cron-validator 1.4.0 |
| **Export** | XLSX export (devextreme-exceljs-fork), file-saver 2.0.5 |
| **Testing** | Vitest 4.0.9, @vue/test-utils 2.4.6, Vue Testing Library 8.1.0 |
| **Linting** | ESLint 9.39.3, @typescript-eslint 8.49.0 |

---

## Environment Configuration

Configure API and auth settings in `.env`:

```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_KEYCLOAK_URL=http://localhost:8180
VITE_KEYCLOAK_REALM=integration
VITE_KEYCLOAK_CLIENT_ID=integration-frontend-vue
```

---

## Scripts

```powershell
npm install
npm run dev                    # Dev server on http://localhost:8084
npm run build                  # vue-tsc + test:run + vite build
npm run lint                   # ESLint (max 0 warnings enforced)
npm run lint:fix               # ESLint with auto-fix
npm run type-check             # vue-tsc --noEmit --skipLibCheck
npm run test:run               # Vitest (run mode, no watch)
npm run test:coverage          # Vitest + v8 coverage report
npm run test:coverage:enforce  # Coverage threshold gate
npm run generate:api           # Regenerate OpenAPI client from openapi.yaml
npm run validate:fix           # lint:fix + type-check
```

---

## Quality Gates

All must pass before merge:

```powershell
npm run lint                   # ESLint — zero warnings allowed
npm run type-check             # TypeScript strict validation
npm run test:run               # All tests pass
npm run test:coverage          # 80% minimum on lines, branches, functions, statements
```

---

## NPM Deprecation Warnings

`npm install` is currently clean for deprecation warnings in this project.

Resolved in this branch:

- Removed `exceljs` usage from ArcGIS job metadata export and migrated to `devextreme-exceljs-fork` for XLSX output.
- This removed the prior deprecated transitive chain (`glob@7`, `inflight`, `fstream`, `rimraf@2`).
- Upgraded transitive `glob` resolution from `10.5.0` to `13.0.6` via dependency override.

Important notes:

- These warnings are upstream package-maintainer deprecation notices.
- They are separate from security advisories.
- Security status remains verified with:

```powershell
npm audit --package-lock-only
npm audit --package-lock-only --omit=dev
```

Both commands are expected to report `0 vulnerabilities` before merge.

Upgrade policy:

- Prefer direct upstream upgrades only.
- Avoid forced major transitive overrides unless separately validated for runtime compatibility.
- If deprecation warnings reappear, prefer upstream direct upgrades; use targeted overrides only with full validation.

---

## Coverage

Coverage thresholds are enforced via `vitest.config.ts` (v8 provider) and `coverage.config.js`.

| Metric | Target | Current |
|---|---|---|
| **Lines** | 80% | ~88.7% ✅ |
| **Statements** | 80% | ~87.5% ✅ |
| **Functions** | 80% | ~84.4% ✅ |
| **Branches** | 80% | ~79.8% ⚠️ |

After running `npm run test:coverage`, open `coverage/index.html` for file-level detail.

> **Note**: `src/tests/unit/ArcGISWizard.FieldMappingStep.spec.ts` is temporarily excluded in `vitest.config.ts` pending a fix for the ArcGIS wizard field mapping spec.

---

## Project Structure

```
src/
├── api/                    # Auto-generated OpenAPI client (axios-based)
│   ├── core/               # Request infrastructure
│   ├── models/             # Generated TypeScript models
│   ├── services/           # Generated service classes
│   │   ├── ArcGISIntegrationService.ts
│   │   ├── CredentialTypeService.ts
│   │   ├── IntegrationConnectionService.ts
│   │   ├── JiraIntegrationService.ts
│   │   ├── JiraWebhookService.ts
│   │   ├── KwIntegrationService.ts
│   │   └── SettingsService.ts
│   └── OpenAPI.ts          # Base URL + auth token config
│
├── assets/                 # Static assets
├── components/
│   ├── admin/              # Admin screens: arcgisconnections, jiraconnections, audit, cache, siteconfig, notifications/
│   ├── common/             # Shared reusable components (ActionMenu, AppModal, GenericDataGrid, StatusBadge, etc.)
│   ├── home/               # HomePage.vue
│   ├── inbound/            # IntegrationsPage.vue (webhook dashboard)
│   ├── layout/             # AppShell, SideNav, TopBar + sidebar composables
│   └── outbound/
│       ├── arcgisintegration/  # ArcGIS: card, page, details tabs, wizard steps
│       └── jirawebhooks/       # Jira webhook: dashboard, create, details, common dialogs
│
├── composables/            # 39 reusable Vue composables:
│   │                       # useApiData, useArcGISConnectionsAdmin, useArcgisFeatures,
│   │                       # useArcGISIntegrationActions, useArcGISJobHistory, useArcGISWizardState,
│   │                       # useAuditLogs, useAuth, useAuthInfo, useAutoLogout,
│   │                       # useCharacterCounter, useConfigureOpenAPI, useConfirmationDialog,
│   │                       # useConnectionTest, useConnectionValidation, useCredentialTypes,
│   │                       # useCron, useDashboardStats, useExistingConnections, useGlobalLoading,
│   │                       # useIntegrationNameValidation, useJiraConnections, useJiraSprints,
│   │                       # useJiraTeams, useJiraWebhookConnection, useJiraWebhookNameValidation,
│   │                       # useJiraWebhookPrefill, useJiraWebhookSubmit, useJiraWebhookWizardState,
│   │                       # useListRouteSync, useNotificationAdmin, useNotificationRulesToggle,
│   │                       # useNotifications, useServiceConnectionsAdmin, useSourceFields,
│   │                       # useTooltip, useTroubleshootDialog, useWebhookActions, useWebhookHistory,
│   │                       # useWebhookHistoryData, useWebhookHistoryPagination
│
├── config/                 # Keycloak config, OpenAPI token setup
├── router/                 # Vue Router 4 routes with lazy loading
├── store/                  # Pinia stores (persistent state)
├── strategies/             # Auth strategy pattern
├── styles/                 # Global CSS / utility classes
├── tests/                  # Unit and integration test files
├── theme/                  # CSS custom properties, design tokens
├── types/                  # TypeScript interfaces and type aliases
└── utils/                  # Shared utility functions
```

---

## Notification System

Real-time in-app notifications delivered via Server-Sent Events (SSE) backed by RabbitMQ async processing on the backend.

### Key Files

| File | Purpose |
|---|---|
| `src/composables/useNotifications.ts` | SSE lifecycle: connect, exponential backoff reconnect (2s → 30s), heartbeat handling |
| `src/store/notification.ts` | Pinia store: `unreadCount`, `notifications[]`, `notificationToasts[]`, pagination, optimistic mark-read |
| `src/api/services/UserNotificationService.ts` | Hand-crafted service (not auto-generated): `getNotifications()`, `getUnreadCount()`, `markAsRead()` |
| `src/components/layout/NotificationBell.vue` | Lucide `Bell` SVG with amber badge overflowing top-right (`top: -6px; right: -6px`) |
| `src/components/layout/NotificationPanel.vue` | Dropdown: recent notifications, mark-as-read, "View all" link |
| `src/components/notifications/NotificationsAllPage.vue` | Full-page list at `/notifications` |
| `src/components/admin/notifications/NotificationsPage.vue` | Admin UI: Event Catalog, Notification Rules, Templates tabs |

### SSE Connection (`useNotifications`)
- Initialized once in `AppShell.vue` on mount
- Stream URL: `{API_BASE}/management/notifications/stream`
- Authentication: uses the standard OAuth2/JWT flow (e.g., `Authorization: Bearer <token>` header managed by Keycloak/axios) — no tokens in the URL
- SSE event name: `"notification"` — payload is `AppNotificationResponse` JSON
- `CONNECTED` event triggers initial fetch of count + notifications
- Exponential backoff reconnect on error: 2s → 4s → 8s → 16s → 30s (capped)

### Notification Store (`useNotificationStore`)
- `unreadCount` — drives bell badge
- `notifications[]` — in-memory page 0; deduplication on incoming SSE
- `notificationToasts[]` — auto-dismissed after 6s
- `loadMoreNotifications()` — infinite-scroll pagination
- `markReadLocal()` / `markAllReadLocal()` — optimistic local mutations before API call

### Admin Panel (`/admin/notifications`)
Three tabs managed by `useNotificationAdmin` composable:
1. **Event Catalog** — 22 platform-wide event types (read-only grid, no tooltips)
2. **Notification Rules** — per-tenant rules: create, delete, toggle enabled/disabled, batch-create all remaining events
3. **Templates** — per-tenant `{{placeholder}}`-based title + message templates

---

## Authentication

Keycloak 26.2.0 is fully integrated:

- Configuration in `src/config/keycloak.ts`
- Token management in `src/config/configureOpenAPI.ts` — injects JWT into all API calls automatically
- `useAuth` / `useAuthInfo` composables for reactive user and tenant state
- `useAutoLogout` handles session expiry
- Multi-tenant: tenant ID extracted from JWT claims

---

## API Client

Generated from `openapi.yaml` (management service OpenAPI spec):

```powershell
npm run generate:api   # regenerates src/api/ from openapi.yaml
```

Use the generated service classes in composables — do not call `axios` directly.

---

## State Management

Pinia stores in `src/store/`:

- Reactive, typed state with `defineStore`
- Persistent stores for user/tenant context
- Actions for async API calls; composables wrap store access

---

## Layout

- Dark top bar + sidebar (`TopBar.vue`, `SideNav.vue`, `AppShell.vue`)
- Feature sidebar sub-menus defined in `sidebarMenuDescriptions.ts`
- Light content panels with subtle border/shadow
- `StatusBadge`, `StatusChipForDataTable` for entity state display
- `ToastContainer.vue` for centralized toast notifications

---

## Testing Patterns

- **Component tests**: `mount` with Vitest + Vue Testing Library; test user interactions
- **Composable tests**: isolated with `vi.mock` for service layer
- **Store tests**: Pinia with `createPinia()` in test setup
- **API mocks**: `vi.mock` the generated service classes
- Environment: jsdom, globals enabled, isolation per file

---

## License

Copyright © 2026 Kaseware. All rights reserved.

