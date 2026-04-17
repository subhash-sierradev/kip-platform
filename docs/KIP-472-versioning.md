# KIP-472: Cross-Component Versioning

## Overview

Each component owns its version independently. Source files are the single source of truth — no shared `version.json`. The sidebar footer shows all three component versions live.

| Component    | Source File                                 | Read by                                |
| ------------ | ------------------------------------------- | -------------------------------------- |
| **Web**      | `web/package.json` → `version`              | Vite at build time (`__APP_VERSION__`) |
| **IMS**      | `api/gradle.properties` → `imsVersion`      | Spring Boot `BuildProperties` bean     |
| **IES**      | `api/gradle.properties` → `iesVersion`      | Spring Boot `BuildProperties` bean     |
| **Contract** | `api/gradle.properties` → `contractVersion` | Gradle compile-time dep                |

---

## Release Workflow

### Step 1 — Feature branches → Release branch

Create a release branch with any name under `release/`:

```
release/022026     ✅ valid
release/1.0.0      ✅ valid
release/sprint-42  ✅ valid
```

Merge feature PRs into it. On every merge, `release-versioning.yml` automatically:

- Detects which modules changed (`web/`, `api/integration-management-service/`, etc.)
- Reads the **current version from source files** as the RC base
- Creates per-module RC git tags (counters are independent per module)

```
Current versions in source files: web=0.0.1, ims=0.0.1, ies=0.0.1

PR merges into release/022026 (web + IMS changed):
  Creates tags: web-0.0.1-rc.1   ims-0.0.1-rc.1
  IES unchanged → no tag

Next PR (IMS only):
  Creates tag:  ims-0.0.1-rc.2   (IMS counter increments independently)
  web-0.0.1-rc.1 stays            (no new web tag)
```

RC tags are **metadata only** — source files are not modified during this step.

---

### Step 2 — Release branch → `main` (Version Bump)

When the release PR is ready, **add a label** to the PR to control the bump type:

| PR Label     | Bump  | When to Use                     |
| ------------ | ----- | ------------------------------- |
| `bump:major` | Major | Breaking API changes            |
| `bump:minor` | Minor | New features                    |
| `bump:patch` | Patch | Bug fixes (default if no label) |

On merge, `release-versioning.yml` automatically:

1. Detects which modules changed in the PR
2. Reads the **current version in `main`** from source files (strips any `-rc.N` suffix)
3. Applies the bump from the PR label to each changed module **independently**
4. Writes the new version back to `package.json` / `gradle.properties` on `main`
5. Commits with `[skip ci]` then creates per-module release tags + a platform tag `vX.Y.Z`

Each module's version is bumped from **its own current version** — they are fully independent:

```
PR label: bump:patch
Modules changed: web, IMS, IES

Current versions on main:
  web/package.json          → 1.3.0
  gradle.properties (IMS)   → 1.2.3
  gradle.properties (IES)   → 1.2.3

After merge:
  web/package.json          → 1.3.1    (was 1.3.0, patch bump)
  gradle.properties (IMS)   → 1.2.4    (was 1.2.3, patch bump)
  gradle.properties (IES)   → 1.2.4    (was 1.2.3, patch bump)

Tags created: web-1.3.1   ims-1.2.4   ies-1.2.4   v1.3.1 (platform tag)
```

```
PR label: bump:minor
Modules changed: IES only (web and IMS untouched)

Current versions on main:
  web/package.json          → 1.3.1   (unchanged, no web files in this PR)
  gradle.properties (IES)   → 1.2.4

After merge:
  gradle.properties (IES)   → 1.3.0   (was 1.2.4, minor bump)

Tags created: ies-1.3.0   v1.3.0 (platform tag)
Web and IMS: no bump, no tag, no Docker push
```

---

### Hotfix (Direct to `main`)

Skip the release branch for urgent single-component fixes:

```
Branch: hotfix/KIP-500-fix  →  PR to main  →  label: bump:patch

IMS: 0.1.0 → 0.1.1   (only IMS changed)
Web, IES: unchanged
Docker push: ims-0.1.1 only
```

---

## CI/CD Workflow Summary

| Workflow                        | Trigger                     | Action                                                                |
| ------------------------------- | --------------------------- | --------------------------------------------------------------------- |
| `release-versioning.yml`        | PR merged → `main`          | Bump changed modules (label-driven), create release tags              |
| `release-versioning.yml`        | PR merged → `release/**`    | Create RC tags for changed modules (source files unchanged)           |
| `release-candidate-preview.yml` | PR opened → `release/**`    | Comment on PR showing next RC version per module                      |
| `api-deploy.yml`                | Push to `main`/`release/**` | Build & push IMS/IES Docker images (version from `gradle.properties`) |
| `web-deploy.yml`                | Push to `main`/`release/**` | Build & push Web Docker image (version from `package.json`)           |

---

## Key Design Decisions

- **Free-form release branch names** — `release/022026`, `release/1.0.0`, etc. all work
- **Bump type via PR label** — no semver in branch name needed; `bump:minor` / `bump:major` / `bump:patch`; defaults to `patch`
- **RC tags are metadata only** — source files not modified on release branch merges
- **Final bump only on main merge** — source files written once, at the point of production release
- **Fully independent per-component** — only changed modules get bumped; others unaffected
- **No `version.json`** — source files read directly by CI/CD and build tools
- **Authenticated version endpoints** — IMS `GET /api/management/version` requires JWT; IES version fetched via Feign with 24h Caffeine cache
