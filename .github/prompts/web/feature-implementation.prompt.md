---
description: 'Implement Vue 3 feature work in web with strict OpenAPI alignment, Composition API, Pinia, DevExtreme patterns, testing, and validation expectations.'

---

Implement the selected frontend feature in `web/` using existing project patterns, generated API contracts, and the established Vue 3 architecture.

## Goal
Deliver production-ready frontend feature work that fits the existing KIP web application without inventing new patterns, bypassing the API contract, or duplicating logic that already exists elsewhere in the repo.

## General Rules
- Use Vue 3 Composition API with `<script setup lang="ts">`.
- Keep TypeScript strict; avoid `any`, loose casts, and untyped event payloads.
- Prefer extending existing components, composables, stores, routes, and utility patterns over creating parallel implementations.
- Follow existing DevExtreme patterns for grids, forms, popups, toolbars, validation, and data-heavy views.
- Keep changes focused on the requested feature; do not refactor unrelated areas unless required to complete the work safely.

## Investigate Before Coding
Before writing code, inspect the existing implementation surface that is closest to the requested feature.

Check for reusable code in:
- `web/src/components/`
- `web/src/composables/`
- `web/src/store/`
- `web/src/router/`
- `web/src/api/services/`
- `web/src/tests/`

Prefer reuse in this order:
1. Existing component or page pattern
2. Existing composable
3. Existing Pinia store
4. Existing generated API service and OpenAPI model
5. New file only if no suitable extension point exists

If you create something new, explain why reuse was not sufficient.

## Must Follow
- No direct `axios` or `fetch` calls; use generated clients from `web/src/api/services/` or existing hand-crafted service wrappers already used by the app.
- Do not invent frontend-only request or response fields that are not present in the OpenAPI-generated models.
- Keep API models authoritative; if the feature needs data the contract does not provide, call that out instead of silently diverging.
- Use Pinia for state that must survive across components, routes, or repeated interactions.
- Keep component state local when it does not need to be shared.
- Add or update tests for new logic, user-visible behavior, and edge cases.
- Keep lint, type-check, and tests green.

## Implementation Guidance

### 1. Components and Pages
- Keep components focused on presentation and orchestration, not API plumbing spread across the template.
- Match existing page composition patterns in the nearest feature area before introducing a new layout structure.
- Use semantic markup, accessible labels, keyboard-reachable controls, and clear loading or empty states.
- For tables, forms, dialogs, and management screens, align with the existing DevExtreme usage already present in `web/src/components/`.
- Avoid large monolithic components when logic can be moved into an existing or new composable.

### 2. Composables
- Reuse an existing composable if the new behavior is a variant of current logic.
- Create a new composable only when the logic is stateful, reusable, and not tied to a single component rendering concern.
- Keep composable APIs explicit: typed inputs, typed return values, clear loading and error state handling.
- Encapsulate side effects, request coordination, and derived state in the composable rather than scattering them across multiple components.

### 3. Pinia Stores
- Extend an existing store if the feature belongs to the same domain.
- Create a new store only for truly shared feature state, not as a default place for every new variable.
- Keep store responsibilities narrow: state, getters, and actions for one domain boundary.
- Avoid duplicating server data in multiple stores unless there is a clear synchronization reason.

### 4. API Integration
- Use existing generated services and models from `web/src/api/` as the source of truth.
- Reuse established request and response mapping patterns already used by nearby features.
- Handle loading, success, empty, and failure states explicitly.
- If an endpoint or model is missing, state the backend contract gap instead of hardcoding temporary shape mismatches.
- Preserve tenant-aware and authenticated request flows already configured in the app.

### 5. Routing, Forms, and User Flow
- Follow the existing router structure and naming patterns when adding or changing routes.
- Keep forms typed and validate user input with the same patterns already used in related screens.
- Support edit, create, cancel, retry, and error-recovery flows when they are part of the feature.
- Preserve expected navigation behavior, breadcrumbs, and tab state where applicable.

### 6. Error Handling and UX States
- Provide clear user-facing feedback for failed requests, validation issues, and empty results.
- Reuse existing toast, dialog, and confirmation patterns where available.
- Do not swallow API errors or hide failed background actions.
- Ensure loading indicators and disabled states prevent duplicate submission or conflicting actions.

## Testing Expectations
- Add or update Vitest tests for new behavior.
- Cover composable logic, component behavior, emitted events, and important edge cases.
- Prefer behavior-focused assertions over implementation-detail assertions.
- Mock API service calls consistently with existing test patterns.
- If the feature changes a workflow that already has E2E coverage, note whether the relevant Playwright tests should also be updated.

At minimum, cover:
- Happy path user behavior
- Validation or guardrail behavior
- Error and empty states
- State transitions caused by async requests
- Any new conditional rendering branches introduced by the feature

## Deliverables
Return work in this format:

1. **Implementation summary**
	- Files changed
	- Main feature behavior added or updated

2. **Reuse and contract notes**
	- Components, composables, stores, and API services reused or extended
	- OpenAPI models and endpoints involved
	- Any assumptions or contract gaps found

3. **Testing**
	- Tests added or updated
	- Important cases covered

4. **Validation**
	- Commands run
	- Pass/fail status
	- Any blockers that prevented full validation

## Validation
- `npm run lint`
- `npm run type-check`
- `npm run test:run`
- `npm run test:coverage`
