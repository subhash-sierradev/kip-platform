# Git & MCP Tools - Efficient Usage Guide

**Purpose**: Instructions for using Git operations and MCP tools (Jira, GitHub) efficiently in Copilot workflows.

---

## 🔧 Tool Discovery Pattern (MANDATORY)

**BLOCKING REQUIREMENT**: Before using any MCP tool, load it first:

```
tool_search_tool_regex(pattern="mcp.*jira|atlassian")
tool_search_tool_regex(pattern="git")
```

**Load once per session** - reuse throughout conversation.

---

## 📋 Jira Ticket Management (Atlassian MCP)

### Available Tools

- `mcp_com_atlassian_getMyCloudIds` - Get Atlassian cloud IDs
- `mcp_com_atlassian_getMyProjects` - List Jira projects
- `mcp_com_atlassian_createJiraIssue` - Create new tickets
- `mcp_com_atlassian_editJiraIssue` - Update existing tickets
- `mcp_com_atlassian_addJiraIssueComment` - Add comments

### Single-Step Ticket Creation (REQUIRED)

**DO**: Create ticket with complete description in one call

```javascript
mcp_com_atlassian_createJiraIssue({
  cloudId: '<your-domain>.atlassian.net',
  contentFormat: 'markdown',
  fields: {
    project: { key: 'KIP' },
    issuetype: { name: 'Bug' }, // or "Task", "Story"
    summary: 'Short 9-10 word summary',
    description: '## Problem\n\n[Full markdown content]\n\n## Solution\n\n...',
  },
});
```

**DON'T**: Create blank ticket → read back → add description as comment

### Required Fields

- `project.key`: "KIP" (Your Project Key)
- `issuetype.name`: "Bug", "Task", or "Story"
- `summary`: Maximum 9-10 words total
- `description`: Complete markdown with structured sections

### Description Template

````markdown
## Problem

[Clear statement of the issue - what's broken or missing]

## Root Cause

**File**: `path/to/file.java` (lines X-Y)

[Technical explanation with code snippets showing the issue]

```java
// Example of problematic code
public void methodWithIssue() {
    // Issue explanation
}
```
````

## Impact

- **Production Impact**: [None/Low/Medium/High]
- **Affected Components**: [List components]
- **Severity**: [P0-Critical/P1-High/P2-Medium/P3-Low]

## Recommended Solution

[Approach with rationale - why this solution over alternatives]

### Steps

1. [Specific implementation step with file references]
2. [Test requirements - unit, integration, e2e]
3. [Quality gates to verify - checkstyle, lint, coverage]

## Files Affected

- `path/to/file1.java` - [Description of changes]
- `path/to/file1.spec.ts` - [Test updates]
- `path/to/component.vue` - [UI changes if applicable]

## Rationale

- **Why this approach?**: [Technical justification]
- **Alternatives considered**: [What was rejected and why]
- **Trade-offs**: [Any compromises or future considerations]

```

### Efficient Jira Workflow

1. **Analysis Phase**
   - Use `semantic_search`, `read_file`, `grep_search` to investigate issue
   - Use `get_errors` to confirm problem
   - Document findings in session memory

2. **Single Ticket Creation**
   - Call `createJiraIssue` with complete description
   - Include all sections: Problem, Root Cause, Impact, Solution, Files, Rationale
   - NO subsequent edits or comments needed

3. **Report**
   - Share ticket URL with user immediately
   - Format: "Created [KIP-###](https://<your-domain>.atlassian.net/browse/KIP-###)"

### Anti-Patterns to Avoid

❌ **DON'T** do this:
```

1. Create ticket with blank description
2. Read ticket back to get ID
3. Edit ticket to add description
4. Add comment with more details
5. Read again to confirm

```

✅ **DO** this:
```

1. Create ticket with full description in one call
2. Report ticket URL
3. Done

````

---

## 🔀 Git Operations

### Available Tool
- `get_changed_files` - Get git diffs (staged, unstaged, merge conflicts)

### Check Changed Files Before Commit

```javascript
get_changed_files({
  repositoryPath: "c:/Users/.../kip-platform",
  sourceControlState: ["staged", "unstaged"]
})
````

**Use Cases**:

- Pre-commit validation
- Reviewing what will be committed
- Checking for unintended changes
- Merge conflict detection
- Quality gate integration

### KIP Commit Message Standard

**Format**:

```
KIP-###: imperative verb change-description

[Body explaining what changed and why - REQUIRED]
```

**Rules**:

- Subject line: Maximum 9-10 words (including ticket ID)
- Body: REQUIRED - explain what and why (blank line after subject)
- Ticket ID: Extract from branch name automatically
- Imperative mood: "add", "remove", "fix", "update" (not "adds", "added")

**Examples**:

✅ **Good**:

```
KIP-464: remove unused acquirePermit and releasePermit methods

Removes dead code with asymmetric resource handling. The decorator
pattern in executeWithRateLimit handles permit lifecycle correctly,
making manual acquire/release unnecessary.
```

❌ **Bad**:

```
KIP-464: commit
```

(No description, too short)

❌ **Bad**:

```
KIP-464: add monitoring data page builder with integration processing service execution and confluence synchronization features
```

(11+ words, too long)

### Efficient Commit Workflow (Autonomous)

**REQUIRED**: Single command, NO user interaction

1. Extract ticket ID from branch name automatically
   - Example: `feature/KIP-437-add-notifications` → `KIP-437`
2. Generate commit message from context (NO prompts)
3. Execute commit with descriptive body
4. Report completion: "Committed all files [short-hash]"
5. **NO** git status check after commit
6. **NO** asking for feedback or confirmation
7. **End conversation immediately**

### Anti-Patterns to Avoid

❌ **DON'T**:

```
1. Ask user for commit message
2. Ask what files to include
3. Confirm before committing
4. Check git status after commit
5. Ask for feedback on commit message
```

✅ **DO**:

```
1. Extract ticket ID from branch
2. Generate message automatically
3. Commit
4. Report: "Committed all files a4b2c8d"
```

---

## 🔍 GitHub Repository Search

### When to Use `github_repo`

- ✅ User explicitly asks for code from specific external GitHub repo
- ✅ Looking for examples from public projects
- ✅ Researching implementation patterns from other repos
- ❌ **DON'T USE**: For repos already open in current workspace

### Efficient Search Pattern

```javascript
github_repo({
  repo: 'owner/repository',
  query: 'specific function name class context keywords',
});
```

**Query Tips**:

- Be specific: Include function names, class names, file types
- Add domain context: "authentication handler spring boot"
- Use technical terms: "rate limiter resilience4j bulkhead"
- Avoid generic searches: "code" or "implementation"

---

## 🔄 Multi-Tool Workflows

### Workflow 1: Bug Analysis → Jira Creation

```
1. semantic_search(query="component with issue")
   → Identify relevant files

2. read_file(filePath="...", startLine, endLine)
   → Read problematic code

3. get_errors(filePaths=["..."])
   → Confirm compilation issues

4. grep_search(query="related pattern", includePattern="**/*.java")
   → Find related code

5. mcp_com_atlassian_createJiraIssue(with full description)
   → Create ticket in one call

6. Report ticket URL
   → Done
```

### Workflow 2: Feature Branch → Quality Gates → Commit

```
1. get_changed_files(sourceControlState=["unstaged", "staged"])
   → Review changes

2. get_errors(filePaths=[changed files])
   → Validate no errors

3. Run quality gates based on file types:
   - Java: ./gradlew checkstyleMain checkstyleTest test jacocoTestReport
   - TypeScript: npm run lint type-check test:run

4. Extract ticket ID from branch name
   → Parse branch: feature/KIP-437-... → KIP-437

5. Generate commit message (no user input)
   → Subject + body with rationale

6. Execute commit
   → All quality gates passed

7. Report hash only
   → "Committed all files a4b2c8d"
```

### Workflow 3: Cross-Repository Research

```
1. semantic_search(query="local implementation pattern")
   → Understand current approach

2. github_repo(repo="reference/project", query="similar pattern")
   → Find external examples

3. Compare approaches
   → Document trade-offs

4. Update session memory
   → Save findings for plan
```

---

## ⚡ Performance Optimization

### Parallel Operations

When operations are independent, execute in **parallel**:

```javascript
// ✅ Read multiple files in parallel
(read_file(file1), read_file(file2), read_file(file3));

// ✅ Check multiple tool patterns
(tool_search_tool_regex('git'), tool_search_tool_regex('jira'));

// ✅ Get errors for multiple files
get_errors([file1, file2, file3]);
```

### Minimize Round Trips

- **Batch related operations**: Read all needed files at once
- **Pre-fetch required context**: Use semantic_search before detailed reads
- **Load MCP tools once**: At session start, reuse throughout
- **Avoid read-back cycles**: Create complete tickets, no confirmation reads

### Strategic Tool Usage

1. **semantic_search** → Get overview (5-10 snippets)
2. **read_file** → Get specific implementation (targeted lines)
3. **grep_search** → Find patterns across files
4. **get_errors** → Validate before commit

---

## ✅ Quality Gates Integration

### Pre-Commit Validation

```
1. get_changed_files(repositoryPath="...", sourceControlState=["unstaged"])
   → Identify modified files

2. get_errors(filePaths=[...])
   → Validate no compilation errors

3. Run quality gates based on file types:

   Backend (Java/Spring Boot):
   - ./gradlew checkstyleMain checkstyleTest  # NO EXCEPTIONS
   - ./gradlew test                           # NO EXCEPTIONS
   - ./gradlew test jacocoTestReport          # 80% minimum

   Frontend (Vue.js/TypeScript):
   - npm run lint             # NO EXCEPTIONS
   - npm run type-check       # NO EXCEPTIONS
   - npm run test:run         # NO EXCEPTIONS
   - npm run test:coverage    # 80% minimum

4. Only commit if ALL checks pass
```

### Post-Implementation Jira Verification

Include in ticket description:

```markdown
## Verification

1. Run `./gradlew :integration-execution-service:test` - all tests pass
2. Run `./gradlew :integration-execution-service:checkstyleMain checkstyleTest` - no violations
3. Run `./gradlew :integration-execution-service:jacocoTestReport` - coverage meets 80% threshold
4. Search codebase: `grep -r "methodName" api/` - zero unexpected matches
```

---

## 🚫 Common Anti-Patterns

### ❌ Anti-Pattern 1: Multiple Small Calls

```
create blank Jira ticket
→ read ticket back to get ID
→ edit to add description
→ read again to verify
→ add comment with more details
```

**Impact**: 5+ API calls, slow, error-prone

### ✅ Efficient Pattern: Single Complete Call

```
create Jira ticket with full markdown description
→ report ticket URL
```

**Impact**: 1 API call, fast, reliable

---

### ❌ Anti-Pattern 2: Interactive Commit Process

```
ask user for commit message
→ ask what files to include
→ confirm before committing
→ run git status after commit
→ ask for feedback
```

**Impact**: Slow, requires multiple user interactions

### ✅ Efficient Pattern: Autonomous Commit

```
analyze changes from context
→ extract ticket ID from branch
→ generate complete commit message
→ commit with body
→ report hash: "Committed all files a4b2c8d"
```

**Impact**: Zero user interaction, fast

---

### ❌ Anti-Pattern 3: Repeated Tool Searches

```
Every operation:
→ search for git tools
→ search for Jira tools
→ use tool
```

**Impact**: Unnecessary latency

### ✅ Efficient Pattern: Load Once Per Session

```
Session start:
→ tool_search_tool_regex("git")
→ tool_search_tool_regex("mcp.*jira|atlassian")

Throughout session:
→ use loaded tools directly
```

**Impact**: Minimal overhead

---

### ❌ Anti-Pattern 4: Sequential File Reads

```
read file1 → wait
read file2 → wait
read file3 → wait
```

**Impact**: 3× latency

### ✅ Efficient Pattern: Parallel Reads

```
read_file(file1), read_file(file2), read_file(file3)
→ all execute in parallel
```

**Impact**: 1× latency

---

## 📊 Efficiency Checklist

Before completing any workflow, verify:

- [ ] MCP tools loaded once per session (not repeatedly)
- [ ] Jira tickets created with complete description (no blank → edit cycle)
- [ ] Git changes reviewed before commit (`get_changed_files`)
- [ ] Commit messages generated automatically (no user prompts)
- [ ] Quality gates validated pre-commit (checkstyle, lint, tests)
- [ ] Parallel operations used for independent tasks
- [ ] Tool calls minimized (batch, pre-fetch)
- [ ] KIP commit standard followed (9-10 words max, required body)
- [ ] Completion reported concisely (hash only, no status checks)
- [ ] Session memory used for complex multi-step tasks

---

## 🎯 Summary: Efficiency Rules

1. **Load MCP tools once** at session start
2. **Create complete Jira tickets** in single call (no blank → edit)
3. **Generate commit messages automatically** (no user interaction)
4. **Validate quality gates** before commit
5. **Use parallel operations** for independent tasks
6. **Minimize API calls** - batch and pre-fetch
7. **Report concisely** - URLs for tickets, hashes for commits
8. **Follow KIP standards** - commit format, coverage requirements

---

**Reference**: Load this file when Git operations, Jira ticket creation, or commit workflows are needed.

**Integration**: This file should be referenced in `.github/copilot-instructions.md` for consistent MCP tool usage across all Copilot sessions.
