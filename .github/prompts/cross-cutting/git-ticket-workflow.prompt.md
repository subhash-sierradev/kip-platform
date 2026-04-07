---
description: 'Execute ticket-based Git workflow tasks on demand across the monorepo: create branch, commit changes, push branch, and create PR to a base branch with ticket in title. Optimized for minimal interaction with auto-push by default.'
---

Run selected Git workflow tasks only. This prompt is task-driven so each step can be executed independently as development progresses.

**Key optimization**: When `commit-changes` is executed, changes are automatically pushed unless `noPush=true` is specified. This reduces manual steps and streamlines the workflow.

## Inputs

- `task` (required): one or more comma-separated values from `create-branch`, `commit-changes`, `push-branch`, `create-pr`
- `branch` (required for `create-branch` and `create-pr`; optional for `commit-changes`): target branch name
- `baseBranch` (required for `create-branch` and `create-pr`): base branch to sync from before branch creation and to target for PR
- `ticket` (required for `create-pr`; optional for `commit-changes`): ticket number in `KIP-123` format
- `summary` (required for `create-pr`): concise imperative summary for PR title
- `prBody` (optional for `create-pr`): PR body content
- `draft` (optional for `create-pr`): `true` or `false` (default `false`)
- `files` (required for `commit-changes`): files or paths to stage, for example `.` or `src/ tests/`
- `commitSummary` (required for `commit-changes`): concise imperative summary used in commit subject
- `commitWhy` (required for `commit-changes`): required WHY-focused commit body with business context
- `dryRun` (optional for `commit-changes`): `true` previews commit message and stops (default `false`)
- `noPush` (optional for `commit-changes`): `true` disables auto-push after commit (default `false`)

## Behavior Contract

- Execute only the selected `task` values.
- If multiple tasks are provided, run in this order:

1. `create-branch`
2. `commit-changes`
3. `push-branch`
4. `create-pr`

- Fail fast if required inputs for a selected task are missing.
- Never force-push unless explicitly requested.
- Never run destructive commands such as `git reset --hard` or `git checkout --`.

## Validation Rules

- `task` must contain only supported values.
- `baseBranch` is required when `create-branch` or `create-pr` is selected.
- `ticket` must match `^KIP-[0-9]+$` when `create-pr` is selected.
- When `commit-changes` is selected, derive `ticket` from the current branch name if not provided.
- `commitWhy` must not be empty when `commit-changes` is selected.
- `branch` must follow `<prefix>/KIP-<number>-<meaningful-description>`.
- Allowed prefixes: `feature`, `fix`, `bug`, `defect`, `patch`, `major`, `chore`, `refactor`.
- `<meaningful-description>` must be lowercase kebab-case and descriptive (for example, `add-monitoring-dashboard`).
- Reject vague descriptions such as `update`, `work`, `temp`, `test`, and `misc`.
- If branch prefix has a common typo, suggest the corrected branch and stop before creating it.
- **Task type inference**: Extract prefix from branch and automatically infer task type using the Task Type Mapping table. Report inferred type in output.

Common typo mapping:

1. `feture` -> `feature`
2. `featre` -> `feature`
3. `htofix` -> `hotfix`
4. `hotfx` -> `hotfix`
5. `bugfix` -> `fix`
6. `defetc` -> `defect`

## Task Type Mapping

Automatically infer task type from branch prefix. This enables validation and categorization without explicit user input.

| Branch Prefix       | Task Type         | Description                              |
| ------------------- | ----------------- | ---------------------------------------- |
| `feature`, `feat`   | `feature`         | New functionality                        |
| `fix`               | `fix`             | Bug fix                                  |
| `bug`               | `bug`             | Bug reports and remediation              |
| `hotfix`            | `hotfix`          | Critical production fix                  |
| `patch`             | `patch`           | Minor patch release                      |
| `major`, `breaking` | `breaking-change` | Breaking changes or major version bump   |
| `chore`             | `chore`           | Maintenance and tooling                  |
| `docs`              | `documentation`   | Documentation updates                    |
| `refactor`          | `refactor`        | Code refactoring without behavior change |

**Inference Rule**: Extract the prefix (text before first `/` in branch name) and map to task type using the table above. If prefix has a typo, suggest correction before proceeding.

## Task: create-branch

Run only when `task` includes `create-branch`.

**Optimization**: Combines fetch and checkout operations to minimize git commands and reduce interaction time.

Steps:

1. Validate `branch` format and meaning before any git operation.
2. Validate `baseBranch` is provided.
3. If the branch has a prefix typo, return the corrected branch suggestion and stop.
4. Extract branch prefix and infer task type using the Task Type Mapping table.
5. Combine fetch and branch creation in optimized sequence:
   - If local branch exists, check it out directly
   - If not, fetch and create in single operation: `git fetch origin <baseBranch> && git checkout -b <branch> origin/<baseBranch>`
6. Verify `origin/<baseBranch>` exists after operation; fail fast with remediation if not.
7. Report resulting branch name, inferred task type, and branch source ref (`origin/<baseBranch>`).

Optimized command:

```bash
# Combined fetch and branch creation (single operation)
git fetch origin <baseBranch> && git checkout -b <branch> origin/<baseBranch>
```

## Task: push-branch

Run only when `task` includes `push-branch`.

Steps:

1. Confirm current branch.
2. Push with upstream tracking using `git push -u origin <branch>`.
3. Report remote tracking status.

## Task: commit-changes

Run only when `task` includes `commit-changes`.

**Auto-push behavior**: After successful commit, automatically push changes to remote unless `noPush=true` is specified. This streamlines the workflow by eliminating the need for separate `push-branch` task in typical scenarios.

Steps:

1. Resolve the effective branch from `branch` input or current checkout.
2. Resolve `ticket` from input or extract first `KIP-[0-9]+` token from branch.
3. Fail fast if no valid ticket can be resolved.
4. Stage changes and commit in a single optimized flow using combined commands.
5. Build commit message using this format:

   Subject: `<ticket>: <commitSummary>`

   Body: `<commitWhy>`

6. Enforce subject target length of 9-10 words total including ticket.
7. If `dryRun=true`, show staged files and the full commit message, then stop.
8. Execute commit using combined add+commit command.
9. If `noPush=false` (default), automatically push with upstream tracking.
10. Return only completion plus hash in format `Committed all files <hash>`.

Optimized command sequence:

```bash
# Single-step add and commit
git add <files> && git commit -m "<ticket>: <commitSummary>" -m "<commitWhy>"

# Auto-push (unless noPush=true)
git push -u origin HEAD
```

**Note**: `push-branch` task is still available for explicit control but is typically unnecessary when using `commit-changes` with default auto-push behavior.

## Task: create-pr

Run only when `task` includes `create-pr`.

Steps:

1. Ensure `gh` CLI is installed and authenticated (`gh auth status`).
2. Ensure current branch matches `branch`; switch if needed.
3. Extract branch prefix and infer task type using the Task Type Mapping table.
4. Push branch if upstream is missing.
5. Build PR title as `<ticket>: <summary>`.
6. Create PR to `baseBranch` from `branch`.
7. Use draft mode when `draft=true`.
8. Report inferred task type in completion output.

Command shape:

```bash
gh pr create --base <baseBranch> --head <branch> --title "<ticket>: <summary>" [--body "<prBody>"] [--draft]
```

If `gh` is unavailable or unauthenticated:

1. Stop before PR creation.
2. Return exact remediation steps (`gh auth login`) and the exact `gh pr create` command to run.

## Output Format

Return a concise execution report:

1. `Tasks requested`
2. `Task type inferred` (when `create-branch` or `create-pr` executes; e.g., "Task type: feature")
3. `Base sync status` (when `create-branch` executes, include fetched ref and source ref)
4. `Commands executed`
5. `Resulting branch/remote state`
6. `Commit hash` (when committed)
7. `PR URL` (when created)
8. `Completion line` (`Committed all files <hash>`) when `commit-changes` executes
9. `Blockers and next action` (if any)

## Example Invocations

1. **Branch only** (optimized):
   - `task=create-branch branch=feature/KIP-437-add-monitoring-dashboard baseBranch=release/0.0.3`
   - Creates branch in single fetch+checkout operation

2. **Commit with auto-push** (most common - optimized):
   - `task=commit-changes files=. commitSummary=add monitoring dashboard commitWhy=Adds monitoring dashboard capabilities to improve visibility and speed up operational triage.`
   - Commits AND pushes automatically in one step

3. **Commit without push** (when auto-push not wanted):
   - `task=commit-changes files=. commitSummary=add monitoring dashboard commitWhy=Adds monitoring dashboard capabilities to improve visibility and speed up operational triage. noPush=true`
   - Commits only, skips automatic push

4. **Push only** (rarely needed now):
   - `task=push-branch branch=feature/KIP-437-add-monitoring-dashboard`
   - Manual push (typically unnecessary with auto-push behavior)

5. **PR only**:
   - `task=create-pr branch=feature/KIP-437-add-monitoring-dashboard baseBranch=release/0.0.3 ticket=KIP-437 summary=add monitoring dashboard`

6. **Full workflow** (optimized - no explicit push needed):
   - `task=create-branch,commit-changes,create-pr branch=feature/KIP-437-add-monitoring-dashboard baseBranch=release/0.0.3 files=. commitSummary=add monitoring dashboard commitWhy=Adds monitoring dashboard capabilities to improve visibility and speed up operational triage. summary=add monitoring dashboard`
   - Note: `push-branch` removed from task list - auto-push handles it
