# Codex Prompt — Q-009 Implementation Authorization

You are working in the BrokerOS Risk repository.

## Objective

This prompt is the **explicit implementation authorization** for:

**Q-009 — Trusted Actor and Authorization Foundation**

The approved Q-009 design baseline has already been manually committed by the Product Owner.

You are now authorized to begin Q-009 implementation strictly within the approved Requirement, Architecture, ADR-011, and Implementation Design.

This authorization applies to **Q-009 only**.

**Q-008 remains unauthorized.**

---

## Authoritative Baseline

Re-read and treat the following as authoritative:

- `AGENTS.md`
- `docs/requirements/Q-009-Requirement.md`
- `docs/architecture/q-009-trusted-actor-authorization-architecture.md`
- `docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`
- `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`
- `docs/lessons/2026-08-26-q-009-trusted-actor-authorization-implementation-design.md`
- latest approved-design-baseline review package under `review/q-009/`

Before making changes, record:

- current branch
- current HEAD
- `git status`
- confirmation that the approved Q-009 design baseline is committed
- confirmation that the worktree does not contain unexpected unrelated changes

If the repository is not at a clean/understood baseline, stop and report the blocker.

---

## Authorization Boundary

You MAY implement only what is required by the approved Q-009 documents.

You MUST NOT:

- redesign Q-009 architecture without an explicit new architecture review;
- contradict ADR-011;
- expand scope into Q-008;
- add speculative features;
- introduce unrelated refactors;
- change business modules unrelated to Q-009;
- perform `git commit` or `git push`;
- delete or overwrite unrelated untracked files;
- silently change an approved decision because implementation is inconvenient.

If implementation reveals a genuine conflict with the approved design, **STOP** and create a review package describing the conflict instead of improvising.

---

## Implementation Requirements

Implement Q-009 exactly according to the approved Implementation Design.

At minimum, ensure the implementation provides the approved foundation for:

- trusted actor representation;
- trusted actor type/source semantics;
- authenticated/trusted actor propagation at the approved boundary;
- authorization capability representation;
- capability checks according to ADR-011;
- fail-closed behavior where the approved design requires it;
- framework/application integration boundaries defined by the implementation design;
- prevention of untrusted caller-controlled actor/capability injection;
- testability of actor and authorization behavior;
- required error/exception behavior;
- required logging/observability behavior without leaking sensitive information.

Do not infer additional runtime features beyond the approved documents.

---

## Security Constraints

Treat Q-009 as security-sensitive foundation code.

Explicitly verify:

1. Actor identity/trust information cannot be accepted directly from arbitrary external request fields unless explicitly approved.
2. Authorization decisions are based on trusted server-side context.
3. Missing/invalid trusted actor state follows the approved fail-closed semantics.
4. Capability checks are explicit and deterministic.
5. No default wildcard/admin capability is accidentally granted.
6. No privilege escalation path is introduced through mutable context, thread leakage, test helpers, headers, DTOs, or fallback behavior.
7. Context cleanup/isolation is correct where request/thread context is used.
8. Error responses do not disclose sensitive authorization internals.
9. Existing Q-007 domain foundation contracts are reused correctly where the approved design requires them.
10. Q-008 behavior is not implemented as a side effect.

---

## Tests

Add/update automated tests required by the approved Implementation Design.

Tests must cover both positive and negative paths, including relevant cases such as:

- trusted actor creation/resolution;
- valid capability authorization;
- missing capability denial;
- missing actor denial;
- invalid/untrusted actor input rejection or non-use;
- no implicit privilege escalation;
- context isolation/cleanup if applicable;
- boundary/adaptor behavior;
- regression coverage for existing functionality.

Use the exact project testing conventions from `AGENTS.md`.

Run the full required Maven verification.

At minimum, unless repository governance requires more:

- Maven tests
- compilation
- any architecture/static checks already present in the project

Do not weaken or delete existing tests simply to obtain a green build.

---

## Documentation / Governance Updates

After implementation, update only the governance artifacts required by repository rules.

Create/update the Q-009 implementation lessons learned if implementation produced relevant reusable findings.

Do not mark implementation as approved or closed yourself.

Implementation completion is **not** equivalent to Architect approval.

---

## Create a NEW Review Package

Never overwrite an existing review directory.

Create a timestamped implementation review package similar to:

`review/q-009/review-v7-implementation-YYYYMMDD-HHMMSS/`

Include at minimum:

- `Summary.md`
- `ImplementationScope.md`
- `ArchitectureConformance.md`
- `SecurityReview.md`
- `TestCoverage.md`
- `Verification.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ChangedFiles.txt`
- `OutstandingItems.md`
- `GateStatus.md`
- `PhaseReviewIndex.md`

Also create a ZIP transfer package for that exact review directory.

The review must explicitly state:

- Requirement Conformance: PASS/FAIL
- Architecture Conformance: PASS/FAIL
- ADR-011 Conformance: PASS/FAIL
- Security Review: PASS/FAIL
- Verification: PASS/FAIL
- Q-009 Implementation Complete: YES/NO
- Ready for Architect Implementation Review: YES/NO
- Ready for Git Commit: NO

`Ready for Git Commit` must remain **NO** until Architect review/approval is completed.

---

## Git Safety Boundary

Do NOT execute:

- `git add`
- `git commit`
- `git push`
- `git reset`
- `git clean`
- `git stash`

The Product Owner performs Git commit manually only after Architect approval.

---

## Expected Final State

If implementation succeeds:

- Q-009 Implementation: COMPLETE
- Q-009 Implementation Authorized: YES
- Q-009 Architect Implementation Review: PENDING
- Q-009 Ready for Git Commit: NO
- Q-008 Implementation Authorized: NO

Next step must be:

**Architect Implementation Review of Q-009**

Do not proceed to final closure or another requirement automatically.

---

## Final Response

Return a concise execution receipt containing:

1. implementation result;
2. exact new review directory;
3. exact ZIP path;
4. changed files;
5. tests/verification result;
6. architecture/ADR conformance result;
7. security review result;
8. `Ready for Architect Implementation Review: YES/NO`;
9. confirmation that no Git write operation was performed;
10. blockers/outstanding items, if any.
