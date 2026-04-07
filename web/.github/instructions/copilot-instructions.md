---
description: 'VueJS 3 development standards and best practices with Composition API and TypeScript'
applyTo: '**/*.vue, **/*.ts, **/*.js, **/*.scss'
---

# VueJS 3 Development Instructions

Instructions for building high-quality VueJS 3 applications with the Composition API, TypeScript, and modern best practices.

## Project Context
- Vue 3.5.29 with Composition API (`<script setup>` syntax) as default
- TypeScript 5.7.2 — strict mode enabled
- Single File Components (`.vue`) with `<script setup lang="ts">`
- Build tool: Vite 7.3.0 with `@vitejs/plugin-vue` 6.0.1
- UI components: DevExtreme 25.2.3 — use for all data grids, forms, popups, toolbars
- State management: Pinia 3.0.4 with persistent stores
- Routing: Vue Router 4.6.3 with lazy-loaded routes
- Authentication: Keycloak 26.2.0 (fully integrated — token auto-injected into all API calls)
- HTTP client: Axios 1.13.2 via auto-generated OpenAPI client in `src/api/`
- Utilities: @vueuse/core 14.1.0, lucide-vue-next 0.553.0, cron-validator 1.4.0
- Export: exceljs 4.4.0, file-saver 2.0.5
- Testing: Vitest 4.0.9 + @vue/test-utils 2.4.6 + Vue Testing Library 8.1.0 (jsdom environment)
- Linting: ESLint 9.17.0 with @typescript-eslint 8.49.0 — zero warnings enforced
- E2E: Playwright (see `e2e/`)
- Official Vue style guide and best practices

## Development Standards

### Architecture
- Favor the Composition API (`setup` functions and composables) over the Options API
- Organize components and composables by feature or domain for scalability
- Separate UI-focused components (presentational) from logic-focused components (containers)
- Extract reusable logic into composable functions in a `composables/` directory
- Structure store modules (Pinia) by domain, with clearly defined actions, state, and getters

### TypeScript Integration
- Enable `strict` mode in `tsconfig.json` for maximum type safety
- Use `defineComponent` or `<script setup lang="ts">` with `defineProps` and `defineEmits`
- Leverage `PropType<T>` for typed props and default values
- Use interfaces or type aliases for complex prop and state shapes
- Define types for event handlers, refs, and `useRoute`/`useRouter` hooks
- Implement generic components and composables where applicable

### Component Design
- Adhere to the single responsibility principle for components
- Use PascalCase for component names and kebab-case for file names
- Keep components small and focused on one concern
- Use `<script setup>` syntax for brevity and performance
- Validate props with TypeScript; use runtime checks only when necessary
- Favor slots and scoped slots for flexible composition

### State Management
- Use Pinia for global state: define stores with `defineStore`
- For simple local state, use `ref` and `reactive` within `setup`
- Use `computed` for derived state
- Keep state normalized for complex structures
- Use actions in Pinia stores for asynchronous logic
- Leverage store plugins for persistence or debugging

### Composition API Patterns
- Create reusable composables for shared logic, e.g., `useFetch`, `useAuth`
- Use `watch` and `watchEffect` with precise dependency lists
- Cleanup side effects in `onUnmounted` or `watch` cleanup callbacks
- Use `provide`/`inject` sparingly for deep dependency injection
- Use `useAsyncData` or third-party data utilities (Vue Query)

### Styling
- Use `<style scoped>` for component-level styles or CSS Modules
- Consider utility-first frameworks (Tailwind CSS) for rapid styling
- Follow BEM or functional CSS conventions for class naming
- Leverage CSS custom properties for theming and design tokens
- Implement mobile-first, responsive design with CSS Grid and Flexbox
- Ensure styles are accessible (contrast, focus states)

### Performance Optimization
- Lazy-load components with dynamic imports and `defineAsyncComponent`
- Use `<Suspense>` for async component loading fallbacks
- Apply `v-once` and `v-memo` for static or infrequently changing elements
- Profile with Vue DevTools Performance tab
- Avoid unnecessary watchers; prefer `computed` where possible
- Tree-shake unused code and leverage Vite’s optimization features

### Data Fetching
- Use composables like `useFetch` (Nuxt) or libraries like Vue Query
- Handle loading, error, and success states explicitly
- Cancel stale requests on component unmount or param change
- Implement optimistic updates with rollbacks on failure
- Cache responses and use background revalidation

### Error Handling
- Use global error handler (`app.config.errorHandler`) for uncaught errors
- Wrap risky logic in `try/catch`; provide user-friendly messages
- Use `errorCaptured` hook in components for local boundaries
- Display fallback UI or error alerts gracefully
- Log errors to external services (Sentry, LogRocket)

### Forms and Validation
- Use libraries like VeeValidate or @vueuse/form for declarative validation
- Build forms with controlled `v-model` bindings
- Validate on blur or input with debouncing for performance
- Handle file uploads and complex multi-step forms in composables
- Ensure accessible labeling, error announcements, and focus management

### Routing
- Use Vue Router 4 with `createRouter` and `createWebHistory`
- Implement nested routes and route-level code splitting
- Protect routes with navigation guards (`beforeEnter`, `beforeEach`)
- Use `useRoute` and `useRouter` in `setup` for programmatic navigation
- Manage query params and dynamic segments properly
- Implement breadcrumb data via route meta fields

### Testing
- Write unit tests with Vue Test Utils and Vitest (NOT Jest — this project uses Vitest 4.0.9)
- Focus on behavior, not implementation details
- Use `mount` and `shallowMount` for component isolation
- Mock global plugins (router, Pinia) as needed
- Add end-to-end tests with Playwright (see `e2e/`)
- Test accessibility using axe-core integration
- **Coverage targets**: 80% lines, statements, functions, branches (enforced via `vitest.config.ts`)

### Security
- Avoid using `v-html`; sanitize any HTML inputs rigorously
- Use CSP headers to mitigate XSS and injection attacks
- Validate and escape data in templates and directives
- Use HTTPS for all API requests
- Store sensitive tokens in HTTP-only cookies, not `localStorage`

### Accessibility
- Use semantic HTML elements and ARIA attributes
- Manage focus for modals and dynamic content
- Provide keyboard navigation for interactive components
- Add meaningful `alt` text for images and icons
- Ensure color contrast meets WCAG AA standards

## Implementation Process
1. Plan component and composable architecture
2. Initialize Vite project with Vue 3 and TypeScript
3. Define Pinia stores and composables
4. Create core UI components and layout
5. Integrate routing and navigation
6. Implement data fetching and state logic
7. Build forms with validation and error states
8. Add global error handling and fallback UIs
9. Add unit and E2E tests
10. Optimize performance and bundle size
11. Ensure accessibility compliance
12. Document components, composables, and stores

## Additional Guidelines
- Follow Vue’s official style guide (vuejs.org/style-guide)
- Use ESLint (with `plugin:vue/vue3-recommended`) and Prettier for code consistency
- Write meaningful commit messages and maintain clean git history
- Keep dependencies up to date and audit for vulnerabilities
- Document complex logic with JSDoc/TSDoc
- Use Vue DevTools for debugging and profiling

## Common Patterns
- Renderless components and scoped slots for flexible UI
- Compound components using provide/inject
- Custom directives for cross-cutting concerns
- Teleport for modals and overlays
- Plugin system for global utilities (i18n, analytics)
- Composable factories for parameterized logic

---

## Kaseware-Specific Patterns

### Prompt Governance
- Canonical runnable prompts for frontend work live in `.github/prompts/web/`.
- E2E prompts live in `.github/prompts/e2e/`.
- `packages/prompt-library/` contains mirrored documentation and examples.

### API Layer
- Always use services from `src/api/services/` (auto-generated from `openapi.yaml`) — never call axios directly
- Token management is handled automatically in `src/config/configureOpenAPI.ts`
- To regenerate the client: `npm run generate:api`

### Authentication
- Keycloak config: `src/config/keycloak.ts`
- Use `useAuth` / `useAuthInfo` composables for reactive user/tenant state
- Tenant ID is extracted from JWT claims automatically

### Composables (42 available in `src/composables/`)
`useApiData` · `useArcGISConnectionsAdmin` · `useArcgisFeatures` · `useArcGISIntegrationActions` · `useArcGISJobHistory` · `useArcGISWizardState` · `useAuditLogs` · `useAuth` · `useAuthInfo` · `useAutoLogout` · `useCharacterCounter` · `useConfigureOpenAPI` · `useConfirmationDialog` · `useConnectionTest` · `useConnectionValidation` · `useCredentialTypes` · `useCron` · `useDashboardStats` · `useExistingConnections` · `useGlobalLoading` · `useIntegrationNameValidation` · `useJiraConnections` · `useJiraSprints` · `useJiraTeams` · `useJiraWebhookConnection` · `useJiraWebhookNameValidation` · `useJiraWebhookPrefill` · `useJiraWebhookSubmit` · `useJiraWebhookWizardState` · `useListRouteSync` · `useNotificationAdmin` · `useNotificationRulesToggle` · `useNotifications` · `useServiceConnectionsAdmin` · `useSourceFields` · `useTooltip` · `useTroubleshootDialog` · `useWebhookActions` · `useWebhookHistory` · `useWebhookHistoryData` · `useWebhookHistoryPagination`

**Before creating a new composable**, check this list — the needed logic may already exist.


> `ArcGISWizard.FieldMappingStep.spec.ts` is temporarily excluded in `vitest.config.ts`.

### Quality Gate Commands
```powershell
npm run lint                   # ESLint — zero warnings allowed
npm run type-check             # vue-tsc strict validation
npm run test:run               # All tests pass
npm run test:coverage          # Coverage report
```

### Notification System
- SSE connection managed by `useNotifications()` — initialized **once** in `AppShell.vue`; never instantiate elsewhere
- `useNotificationStore` (`src/store/notification.ts`) is the single source of truth: `unreadCount`, `notifications[]`, `notificationToasts[]`
- `UserNotificationService` (`src/api/services/UserNotificationService.ts`) is **hand-crafted** (not auto-generated) — use it directly for notification API calls; do not call axios
- Bell icon uses `lucide-vue-next` `Bell` SVG (not `dx-icon-bell`); badge overflows top-right at `top: -6px; right: -6px`
- Admin notification management uses `useNotificationAdmin` (rules, templates, policies, event catalog) and `useNotificationRulesToggle` (toggle enable/disable with confirmation dialog)
- Toast notifications are added via `store.addNotificationToast()` and auto-dismissed after 6 seconds
- `NotificationsPage.vue` has three tabs: **Event Catalog** (read-only, no tooltips), **Notification Rules** (CRUD + toggle + batch), **Templates** (`{{placeholder}}`-based)
- SSE reconnects with exponential backoff: 2s → 4s → 8s → 16s → 30s (capped) — do not modify backoff logic without updating `useNotifications.ts`