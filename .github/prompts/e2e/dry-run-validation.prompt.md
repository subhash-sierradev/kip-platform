---
description: 'Validate Playwright suite health in e2e using dry-run/list mode and identify immediate breakpoints before execution.'
---

Validate E2E test readiness for this repository using the `e2e/` project.

## Tasks
1. Run prompt-guided dry-run checks:
- `cd e2e`
- `npm ci`
- `npx playwright test --list`
2. Report parse/configuration issues immediately.
3. Flag missing env/config assumptions and unstable test grouping.

## Output
- Status: pass/fail for dry-run
- First blocking error (if any)
- Recommended smallest fix
- Optional follow-up checks (`sanity`, `regression`)
