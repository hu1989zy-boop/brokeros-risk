# AI Engineering Execution Protocol

**Adopted:** 2026-08-31, by the Product Owner, as a standing governance
document for this repository. Applies to every AI agent (Claude Code,
Codex, or any other) performing engineering work on BrokerOS Risk, for
every task, not only the session in which it was adopted.

## 1. Core Principle

This project MUST NOT be executed as one large autonomous task.
Claude must operate as a stage-bounded engineering agent.
For every task:

- Work on ONE engineering stage at a time.
- Do NOT continue automatically into the next stage.
- Do NOT assume that passing tests means the task is complete.
- Do NOT silently modify architecture, requirements, ADRs, or approved
  design.
- Stop when the current stage exit criteria are satisfied.
- Wait for explicit approval before entering the next stage.

The primary objective is NOT speed.
The primary objectives are:

1. correctness
2. traceability
3. architectural consistency
4. reviewability
5. reproducibility

## 2. Mandatory Workflow

Every engineering task must follow this lifecycle:

```
Requirement
↓
Requirement Review Gate
↓
Architecture Analysis
↓
Architecture Review Gate
↓
ADR Decision
↓
ADR Approval Gate
↓
Implementation Design
↓
Design Review Gate
↓
Design Approval
↓
Implementation
↓
Implementation Verification
↓
Independent Review
↓
Final Closure
↓
Git Commit
```

Claude MUST NOT skip stages.
Claude MUST NOT combine multiple major stages into a single autonomous
execution unless explicitly instructed.

## 3. Stage Boundary Rule

Before starting work, Claude must explicitly determine:

- Current task ID
- Current lifecycle stage
- Approved inputs
- Expected outputs
- Allowed actions
- Forbidden actions
- Exit criteria

If any of these are unclear, Claude must STOP and report the ambiguity.
Claude must never infer approval.

## 4. Context Control

Before performing design or review work, Claude must reload the
authoritative project context from the repository.

Priority of authority:

1. Approved Requirement
2. Approved ADR
3. Approved Architecture
4. Approved Implementation Design
5. Project engineering rules
6. Existing implementation
7. Tests
8. Historical review artifacts
9. Conversation context

Conversation history is NOT an authoritative engineering source.
If conversation instructions conflict with approved repository artifacts,
Claude must report the conflict instead of silently choosing one.

## 5. Context Drift Prevention

For long-running tasks, Claude must periodically re-anchor itself to:

- current task
- current stage
- approved requirements
- relevant ADRs
- approved design
- remaining exit criteria

If the work begins to expand into unrelated problems, Claude must classify
them as:

- blocker
- prerequisite
- follow-up
- unrelated issue

Claude must NOT silently expand the scope.

## 6. Scope Expansion Rule

When an unexpected issue is discovered:
DO NOT automatically fix it.
First determine whether it is:

A. Required to complete the current stage
B. A prerequisite requiring separate approval
C. Technical debt
D. Unrelated

For B/C/D:
Record the issue and continue only if the current task remains valid.
Never turn a local task into a repository-wide refactor without explicit
approval.

## 7. Architecture Protection

Claude must treat approved architecture and ADRs as constraints.
During implementation review:
Claude must verify:

- implementation matches approved design
- module boundaries are respected
- dependency direction is correct
- domain logic has not leaked into infrastructure
- no undocumented architectural decision was introduced
- no approved design decision was silently changed

If implementation requires changing architecture:
STOP.
Do not modify the architecture automatically.
Create an Architecture Deviation finding and request a new architecture
decision.

## 8. Independent Review Principle

Claude must NOT treat:

- successful compilation
- passing unit tests
- passing integration tests
- successful Docker startup

as proof that implementation is correct.

Review must independently evaluate:

```
Requirement → Design → Implementation → Runtime behavior
```

Tests are evidence, not authority.

## 9. Review Checklist

Every implementation review must check at minimum:

**Requirement Compliance**
- Are all acceptance criteria implemented?
- Is anything missing?
- Was anything implemented that was not requested?

**Architecture Compliance**
- Does implementation follow approved architecture?
- Does implementation comply with ADRs?
- Were new architectural decisions introduced?

**Design Compliance**
- Does implementation match approved implementation design?
- Are deviations documented?

**Code Quality**
- correctness
- maintainability
- naming
- boundaries
- error handling
- transaction handling
- concurrency concerns
- idempotency where applicable

**Data**
- schema compatibility
- migration safety
- backward compatibility
- MySQL compatibility
- data integrity

**Tests**
- happy path
- boundary cases
- failure cases
- regression risks

**Operational Safety**
- startup
- configuration
- logging
- observability
- rollback implications

**Git**
- unexpected files
- unrelated modifications
- generated artifacts
- suspicious deletions

## 10. Error Propagation Prevention

If an error occurs:
Do NOT repeatedly modify surrounding systems merely to make a test pass.
Instead:

1. identify root cause
2. identify affected layer
3. compare against requirement/design/ADR
4. determine whether code or test is wrong
5. apply the smallest valid correction
6. rerun relevant verification

Never weaken tests simply to obtain PASS.

## 11. Completion Rule

Claude must never report "Task completed" only because commands succeeded.
Completion requires evidence that all stage exit criteria have been
satisfied.

The final report must contain:

- Task ID
- Stage
- Scope reviewed
- Files inspected
- Verification executed
- Requirement status
- Architecture status
- Design compliance status
- Test status
- Findings
- Remaining risks
- Out-of-scope issues
- Recommendation
- Gate decision

## 12. Gate Decision

Every stage must end with exactly one decision:

- PASS
- PASS WITH CONDITIONS
- BLOCKED
- FAIL

Claude must explain the evidence behind the decision.
Claude MUST NOT proceed to the next lifecycle stage automatically.

## 13. Token / Context Budget Protection

If the task becomes too large to reliably review within the current
context: STOP. Do not continue with degraded reasoning.

Report: **CONTEXT LIMIT RISK**

Then provide:

- completed scope
- remaining scope
- current findings
- unresolved questions
- recommended continuation point

A controlled stop is considered better than an unreliable completion.

## 14. Git Safety

Claude must NOT commit or push unless explicitly instructed.

Before recommending a commit, verify:

- working tree changes
- unexpected files
- required review artifacts
- required documentation
- test evidence
- unresolved blockers

Only then may Claude recommend: **READY FOR GIT COMMIT**

## 15. Final Principle

When choosing between "finish more work" and "preserve correctness and
traceability," always choose correctness and traceability.

Small verified steps are preferred over large autonomous execution.

## 16. Governance-Document Status Consistency (mandatory)

A recurring real defect (Q-011, and again Q-014): when a governance
document's gate status is advanced (draft → approved → accepted →
authorized), the status is updated in one location but a mirror location
in the same document — or a sibling document — is left stating the old
status, so the document contradicts itself and an implementer (Codex)
correctly halts. Writing a passive "be thorough" lesson did not stop the
recurrence. These two rules are therefore mandatory and mechanical.

### 16.1 Single live-status source

Each governance document (Requirement, Architecture, ADR, Implementation
Design) declares its **live** gate status in exactly ONE section — its
Gate / Current-Gate section (Requirement §17, Architecture §24,
Implementation Design §21, or an ADR's Status line + Approval Boundary).
Every other mention of status in that same document — including the
top-of-file Document Status / Status header — must **point to that one
section as authoritative and must not independently restate the live
PASS / APPROVED / ACCEPTED / AUTHORIZED / PENDING value.** A header may
carry a one-line current summary only if it explicitly defers ("see §N,
authoritative if this disagrees"); it must never be a second
independently-maintained copy of the value. Single source → drift is
structurally impossible.

### 16.2 Pre-completion status sweep

Before declaring any gate-status change complete (approval, acceptance,
authorization, or a correction of one), run a literal search of the
changed document — and every sibling governance document that references
it — for the **prior** status token (e.g. `DRAFT`, `PENDING`, `NOT
AUTHORIZED`, `not yet approved`, `Proposed`, `Allowed: **NO`) and confirm
zero stale live occurrences remain. This is a verification step, not a
judgement call: the search must actually be run, and its clean result is
part of the evidence that the status change is complete. A single §N Gate
section may also accumulate contradictory lines across successive edits
(an old `Allowed: NO` beside a new `Allowed: YES`); the sweep must catch
and collapse these into one coherent progression too.

These two rules bind Claude Code directly and are not optional politeness;
skipping the §16.2 sweep is the specific omission that caused Codex to
halt on Q-014.
