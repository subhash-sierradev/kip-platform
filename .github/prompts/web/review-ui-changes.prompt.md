---
description: 'Review web UI changes for architecture, type safety, accessibility, performance, testing, and OpenAPI contract alignment with actionable findings by severity.'
---

Review selected `web/` changes and report concrete findings that matter for correctness, maintainability, accessibility, performance, and contract alignment.

## Goal
Identify meaningful frontend issues before merge, with findings ordered by severity and tied to the actual KIP web patterns used in `web/`.

## Review Approach
Inspect the changed code in the context of nearby existing implementations before judging the design.

Compare the changes against patterns already established in:
- `web/src/components/`
- `web/src/composables/`
- `web/src/store/`
- `web/src/router/`
- `web/src/api/services/`
- `web/src/tests/`

Focus on root-cause issues, not cosmetic preferences.

## Review Priorities

### 1. Architecture and Boundaries
- Are component responsibilities clear, or is logic spread across templates, pages, composables, and stores in a way that increases maintenance cost?
- Does the change reuse existing page, form, dialog, grid, and feature patterns instead of introducing parallel structures?
- Is shared state kept in the correct place: local component state when isolated, Pinia only when cross-component or cross-route state is required?
- Are new composables or stores justified, or does the change duplicate existing functionality?

### 2. TypeScript and Vue Patterns
- Check for `any`, unsafe casts, loosely typed refs, and untyped emitted events.
- Check that Composition API usage is clear and consistent with nearby code.
- Flag state or watcher logic that is fragile, implicit, or difficult to reason about.
- Flag large components that should push reusable stateful logic into composables.

### 3. API and OpenAPI Contract Alignment
- Confirm all API access goes through generated services in `web/src/api/` or existing approved service wrappers already used by the app.
- Flag any direct `axios` or `fetch` usage.
- Flag frontend-only request or response fields that are not represented in OpenAPI-generated models.
- Check that loading, empty, success, and error states are handled for async API work.
- Call out backend contract gaps explicitly when the UI appears to rely on data the API does not supply.

### 4. DevExtreme and UI Consistency
- Check that grids, forms, popups, toolbars, and validation patterns align with existing DevExtreme usage in the repo.
- Flag inconsistent interaction patterns, layout structures, or custom widgets that duplicate existing components.
- Check whether the UI preserves expected create, edit, cancel, retry, and confirmation flows.

### 5. Accessibility and UX
- Check semantic structure, labeling, keyboard access, focus handling, and screen-reader-friendly states.
- Flag missing empty states, vague error states, inaccessible icon-only actions, or broken tab order.
- Check whether loading and disabled states prevent duplicate actions and communicate progress clearly.

### 6. Performance and Reactivity
- Flag unnecessary watchers, derived state that should be computed, and reactive scope that is broader than needed.
- Check list rendering, conditional rendering, and repeated expensive computations.
- Flag route or feature code that should be lazy-loaded but is unnecessarily eager.

### 7. Testing and Regression Risk
- Check whether new logic and changed behavior are covered by Vitest tests.
- Flag missing coverage for happy path, edge cases, validation, async state transitions, and error handling.
- If the change affects a user flow likely covered by Playwright, note whether E2E updates are missing.
- Call out cases where the lack of tests makes the change hard to trust.

## Findings Rules
- Report findings first, ordered by severity.
- Only raise an issue if it is concrete, defensible, and likely to matter in production or maintenance.
- Include file-level references and explain the specific risk or regression.
- Suggest the direction of a fix when it is clear.
- If there are no findings, say so explicitly and mention any residual testing or validation gaps.

## Output Format
- `🔴 Critical`: must fix before merge
- `🟡 Medium`: should fix
- `🟢 Minor`: optional improvement

For each finding, include:
- Severity
- File and affected area
- What is wrong
- Why it matters
- Concrete fix direction

After findings, optionally include:
- Open questions or assumptions
- Residual risks or testing gaps
- Very short change summary

## Validation Notes
When relevant, mention whether review confidence depends on these checks:
- `npm run lint`
- `npm run type-check`
- `npm run test:run`
- `npm run test:coverage`
