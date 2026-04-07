---
description: 'Harden Playwright selectors in e2e tests using stable locators, role/label strategies, and maintainable page-object patterns.'
---

Refactor selected E2E tests/page objects for stable selectors.

## Rules
- Prefer role/label/test-id based selectors over CSS structure selectors.
- Keep selector logic in page-object classes under `e2e/pages/`.
- Minimize duplicated locators.
- Preserve existing test intent and assertions.

## Output
- Updated selector strategy per file
- Before/after examples for unstable selectors
- Follow-up recommendation for adding explicit test ids where needed
