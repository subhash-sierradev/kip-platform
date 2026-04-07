---
description: 'Triage flaky Playwright tests in e2e by isolating nondeterminism, timing, selector stability, and data-seeding dependencies.'
---

Investigate flaky E2E tests and provide deterministic fixes.

## Investigation Checklist
- Repro pattern: frequency, suite, browser, environment
- Selector stability: avoid brittle nth-child/text-only selectors
- Wait strategy: replace sleeps with explicit state-based waits
- Data setup: confirm seed/setup dependencies are deterministic
- Network/retry behavior: identify transient backend dependencies

## Output
1. Root-cause hypothesis with evidence.
2. Minimal code-level fix per flaky test.
3. Optional hardening steps (timeouts, retries, fixtures) with rationale.
