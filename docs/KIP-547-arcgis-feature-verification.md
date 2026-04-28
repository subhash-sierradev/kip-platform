# KIP-547 — ArcGIS Feature Service Verification (Temporary Admin Page)

> **Jira**: [KIP-547](https://kaseware.atlassian.net/browse/KIP-547)
> **Branch**: `feature/KIP-547-arcgis-postman-verification`
> **Base**: `release/042026`
> **Status**: In Progress
> **Purpose**: Temporary verification tool — **must be removed after verification is complete**

---

## Summary

Build a temporary, read-only admin page to display and search records from a given ArcGIS feature service. The page allows querying records by `OBJECTID` or `external_location_id` to verify that data is being pushed into ArcGIS correctly.

ArcGIS credentials are passed through Spring Boot `application.yml` configuration (env vars). Data is retrieved using pagination (1,000 records per request). The entire implementation is isolated in its own packages and all touch points are marked with `// TODO KIP-547 REMOVE` for safe, traceable cleanup.

---

## Scope

### What This IS

- A read-only admin page (role: `app_admin` only)
- Server-side pagination: 1,000 records per page via ArcGIS `resultOffset` + `resultRecordCount`
- Search/filter by `OBJECTID` or `external_location_id`
- Credentials sourced from `application.yml` (environment variables)
- Isolated — all new code is in dedicated packages, nothing shared with existing production flows

### What This IS NOT

- Not production infrastructure — this is a temporary verification/testing tool
- No writes to ArcGIS
- No Azure Key Vault integration
- No audit logging
- No notifications
- No OpenAPI spec update

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Frontend (Vue.js)                                       │
│  web/src/components/admin/arcgisverification/            │
│    ArcGISVerificationPage.vue                            │
│  web/src/composables/useArcGISVerification.ts            │
└─────────────────────┬───────────────────────────────────┘
                      │  GET /api/management/arcgis/verification/features
                      │  ?offset=0&objectId=&locationId=
┌─────────────────────▼───────────────────────────────────┐
│  Backend (IMS — integration-management-service)          │
│  com.integration.management.arcgisverification/          │
│    ArcGISVerificationController  (port 8085)             │
│    ArcGISVerificationService                             │
│    model/ArcGISVerificationPageResponse                  │
│  com.integration.management.config.properties/           │
│    ArcGISVerificationProperties                          │
└─────────────────────┬───────────────────────────────────┘
                      │  HTTPS — OAuth2 token + query
┌─────────────────────▼───────────────────────────────────┐
│  ArcGIS Feature Service                                  │
│  {featureServiceUrl}/0/query                             │
└─────────────────────────────────────────────────────────┘
```

---

## Implementation Plan

### Phase 1 — Backend

#### Step 1 — Configuration Properties

**New file**: `api/integration-management-service/src/main/java/com/integration/management/config/properties/ArcGISVerificationProperties.java`

- `@ConfigurationProperties(prefix = "arcgis.verification")`
- Fields: `featureServiceUrl`, `clientId`, `clientSecret`, `tokenUrl`

**New file**: `api/integration-management-service/src/main/java/com/integration/management/config/ArcGISVerificationConfig.java`

- `@Configuration` + `@EnableConfigurationProperties(ArcGISVerificationProperties.class)`

#### Step 2 — Response DTO

**New file**: `api/integration-management-service/src/main/java/com/integration/management/arcgisverification/model/ArcGISVerificationPageResponse.java`

```java
// Fields:
List<Map<String, Object>> features  // attributes from ArcGIS response
int fetchedCount                    // number of records in this page
int offset                          // offset used for this request
boolean exceededTransferLimit       // from ArcGIS response flag
```

#### Step 3 — Service

**New file**: `api/integration-management-service/src/main/java/com/integration/management/arcgisverification/ArcGISVerificationService.java`

- Uses `java.net.http.HttpClient` (Java 25 stdlib — no new dependencies)
- `getToken()` — POST `client_credentials` grant to `tokenUrl`; volatile in-memory TTL cache
- `queryFeatures(int offset, String objectIdFilter, String locationIdFilter)`:
  - `where` clause logic:
    - objectId provided → `OBJECTID = {n}`
    - locationId provided → `external_location_id = '{v}'`
    - neither → `1=1`
  - ArcGIS query URL:
    ```
    {featureServiceUrl}/0/query
      ?f=json
      &where={whereClause}
      &outFields=*
      &returnGeometry=false
      &orderByFields=OBJECTID
      &resultOffset={offset}
      &resultRecordCount=1000
      &token={token}
    ```
  - Parse `features[].attributes` → `List<Map<String, Object>>`
  - Return `ArcGISVerificationPageResponse`

#### Step 4 — Controller

**New file**: `api/integration-management-service/src/main/java/com/integration/management/arcgisverification/ArcGISVerificationController.java`

```
GET /api/management/arcgis/verification/features
  ?offset=0        (default: 0)
  &objectId=       (optional)
  &locationId=     (optional)

→ 200 ArcGISVerificationPageResponse
→ 503 if featureServiceUrl is not configured
```

- `@PreAuthorize("hasRole('app_admin')")` — app admin only
- All class/method comments marked `// TODO KIP-547 REMOVE`

#### Step 5 — application.yml

**Modify**: `api/integration-management-service/src/main/resources/application.yml`

```yaml
# TODO KIP-547 REMOVE — temporary ArcGIS feature service verification config
arcgis:
  verification:
    feature-service-url: ${ARCGIS_VERIFICATION_URL:}
    client-id: ${ARCGIS_VERIFICATION_CLIENT_ID:}
    client-secret: ${ARCGIS_VERIFICATION_CLIENT_SECRET:}
    token-url: ${ARCGIS_VERIFICATION_TOKEN_URL:}
```

#### Step 6 — Unit Tests

**New file**: `...arcgisverification/ArcGISVerificationServiceTest.java`

- Test `where` clause construction for all three cases
- Test token fetch and caching
- Test response parsing from mock ArcGIS JSON

**New file**: `...arcgisverification/ArcGISVerificationControllerTest.java`

- MockMvc tests: 200 with data, 503 when URL not configured

---

### Phase 2 — Frontend

#### Step 7 — Composable

**New file**: `web/src/composables/useArcGISVerification.ts`

State:
| Name | Type | Description |
|------|------|-------------|
| `records` | `ref<Record<string, unknown>[]>` | Loaded records |
| `loading` | `ref<boolean>` | Request in-flight |
| `error` | `ref<string \| null>` | Error message |
| `hasMore` | `ref<boolean>` | `exceededTransferLimit` from API |
| `currentOffset` | `ref<number>` | Current pagination offset |

Method: `fetchRecords(offset, objectId?, locationId?)` — calls `GET /management/arcgis/verification/features` via `request` from `src/api/core/request`

#### Step 8 — Page Component

**New file**: `web/src/components/admin/arcgisverification/ArcGISVerificationPage.vue`

- Two `DxTextBox` inputs: **Object ID** and **Location ID**
- **Search** `DxButton` → `fetchRecords(0, objectId, locationId)` (replaces records)
- **Load Next Page** `DxButton` (visible only when `hasMore === true`) → `fetchRecords(currentOffset + 1000, objectId, locationId)` (appends records)
- `GenericDataGrid` with dynamic columns derived from first record's keys; `OBJECTID` and `external_location_id` pinned first
- Loading indicator while `loading === true`
- Error alert when `error !== null`

#### Step 9 — Routes

**Modify**: `web/src/router/routes.ts`

```ts
// TODO KIP-547 REMOVE
arcgisVerification: '/admin/arcgis-verification',
```

**Modify**: `web/src/router/index.ts`

```ts
// TODO KIP-547 REMOVE
{
  path: ROUTES.arcgisVerification,
  component: () => import('@/components/admin/arcgisverification/ArcGISVerificationPage.vue'),
  beforeEnter: roleGuard(['app_admin']),
},
```

#### Step 10 — Navigation

**Modify**: `web/src/components/layout/AppShell.vue`

```ts
// TODO KIP-547 REMOVE
{ label: 'ArcGIS Verify', route: '/admin/arcgis-verification', icon: 'search' },
```

Added to the Admin section children array.

#### Step 11 — Frontend Tests

**New file**: `web/src/tests/composables/useArcGISVerification.spec.ts`

- Mock `request`, test `fetchRecords`, pagination offset logic, error handling

**New file**: `web/src/tests/components/ArcGISVerificationPage.spec.ts`

- Render component, simulate search input + button click, assert grid receives records

---

## Environment Variables

Set these in your local dev environment or deployment config:

| Variable                            | Description                                          | Example                                            |
| ----------------------------------- | ---------------------------------------------------- | -------------------------------------------------- |
| `ARCGIS_VERIFICATION_URL`           | ArcGIS Feature Service base URL (without `/0/query`) | `https://services.arcgis.com/.../FeatureServer`    |
| `ARCGIS_VERIFICATION_CLIENT_ID`     | OAuth2 client ID                                     | `abc123`                                           |
| `ARCGIS_VERIFICATION_CLIENT_SECRET` | OAuth2 client secret                                 | `secret456`                                        |
| `ARCGIS_VERIFICATION_TOKEN_URL`     | ArcGIS OAuth2 token endpoint                         | `https://www.arcgis.com/sharing/rest/oauth2/token` |

If `ARCGIS_VERIFICATION_URL` is blank (default), the controller returns `503 Service Unavailable`.

---

## New Files Checklist

| File                                                                     | Type | Notes                      |
| ------------------------------------------------------------------------ | ---- | -------------------------- |
| `api/.../config/properties/ArcGISVerificationProperties.java`            | New  | Config binding             |
| `api/.../config/ArcGISVerificationConfig.java`                           | New  | Spring config registration |
| `api/.../arcgisverification/model/ArcGISVerificationPageResponse.java`   | New  | Response DTO               |
| `api/.../arcgisverification/ArcGISVerificationService.java`              | New  | Business logic             |
| `api/.../arcgisverification/ArcGISVerificationController.java`           | New  | REST endpoint              |
| `api/.../arcgisverification/ArcGISVerificationServiceTest.java`          | New  | Unit tests                 |
| `api/.../arcgisverification/ArcGISVerificationControllerTest.java`       | New  | Controller tests           |
| `web/src/composables/useArcGISVerification.ts`                           | New  | Vue composable             |
| `web/src/components/admin/arcgisverification/ArcGISVerificationPage.vue` | New  | UI page                    |
| `web/src/tests/composables/useArcGISVerification.spec.ts`                | New  | Frontend tests             |
| `web/src/tests/components/ArcGISVerificationPage.spec.ts`                | New  | Frontend tests             |

## Modified Files Checklist

| File                                                                    | Change                                         |
| ----------------------------------------------------------------------- | ---------------------------------------------- |
| `api/integration-management-service/src/main/resources/application.yml` | Add `arcgis.verification.*` config block       |
| `web/src/router/routes.ts`                                              | Add `arcgisVerification` route constant        |
| `web/src/router/index.ts`                                               | Add lazy route with `roleGuard(['app_admin'])` |
| `web/src/components/layout/AppShell.vue`                                | Add nav item to Admin section                  |

---

## Quality Gates

```powershell
# Backend (run from api/)
./gradlew :integration-management-service:checkstyleMain checkstyleTest  # 0 violations
./gradlew :integration-management-service:test                           # all pass
./gradlew :integration-management-service:jacocoTestReport               # >= 80%

# Frontend (run from web/)
npm run lint          # 0 warnings
npm run type-check    # 0 errors
npm run test:run      # all pass
npm run test:coverage # >= 80%
```

---

## Manual Verification Steps

1. Set the four environment variables above for your target ArcGIS service
2. Start IMS: `./gradlew :integration-management-service:bootRun`
3. Navigate to `/admin/arcgis-verification` as a user with `app_admin` role
4. Verify the grid loads the first 1,000 records
5. Enter a known `OBJECTID` in the Object ID field → click Search → confirm single matching record appears
6. Enter a known `external_location_id` in the Location ID field → click Search → confirm filtered result
7. Click **Load Next Page** → confirm additional 1,000 records append to the grid
8. Compare record values with Postman queries against the same ArcGIS feature service

---

## How to Remove This Feature (KIP-547 Cleanup)

Search the codebase for `// TODO KIP-547 REMOVE` and `# TODO KIP-547 REMOVE` to find all touch points, then:

1. **Delete** all files in the table above under "New Files Checklist"
2. **Revert** all four modified files (remove the marked lines only)
3. Remove the env var entries from any deployment configuration
4. Verify: `./gradlew clean check` + `npm run lint && npm run type-check && npm run test:run`

---

## Key Design Decisions

| Decision                                               | Rationale                                                                                |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| Credentials in `application.yml` (not Azure Key Vault) | Simpler for a temporary tool; no secret rotation needed during short verification window |
| `java.net.http.HttpClient` (stdlib)                    | Zero new dependencies; fully contained in `arcgisverification/` package                  |
| `app_admin` role only                                  | Most restrictive available role; prevents tenant users from accessing test tooling       |
| Server-side `where` clause filter                      | Avoids fetching large datasets before filtering; ArcGIS handles efficiently              |
| Read-only scope                                        | No writes, no audit log, no notifications — verification only                            |
| No OpenAPI spec update                                 | Follows hand-crafted `request()` pattern used by `UserNotificationService`               |

---

## References

- [ArcGIS Feature Service Query REST API](https://developers.arcgis.com/rest/services-reference/enterprise/query-feature-service-layer/)
- [ArcGIS OAuth2 Token](https://developers.arcgis.com/rest/users-groups-and-items/authentication/)
- Existing ArcGIS client patterns: `api/integration-execution-service/src/main/java/com/integration/execution/client/ArcGISApiClient.java`
- Field constants: `api/integration-execution-service/src/main/java/com/integration/execution/constants/ArcGisConstants.java`
