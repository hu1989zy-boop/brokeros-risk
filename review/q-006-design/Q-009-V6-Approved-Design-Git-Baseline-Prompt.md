# Codex Prompt — Q-009 Approved Design Git Baseline

You are working in the BrokerOS Risk repository.

## Objective

Establish the approved Git baseline for **Q-009 — Trusted Actor and Authorization Foundation** after the formal approval of **Q-009 Implementation Design V1**.

This task is **governance/baseline work only**.

**DO NOT implement Q-009 runtime functionality yet.**
**DO NOT start Q-008 implementation.**

## Authoritative Current State

The latest reviewed package records:

- Q-009 Requirement V1: APPROVED
- Q-009 Architecture V2: APPROVED
- ADR-011: ACCEPTED
- Q-009 Implementation Design V1: APPROVED
- Implementation Design V2 required: NO
- Q-009 Implementation Ready for Authorization: YES
- Q-009 Implementation: NOT STARTED
- Q-009 Implementation Authorized: NO
- Q-008 Implementation Authorized: NO

Latest approval review package:

`review/q-009/review-v5-implementation-design-approved-20260826-021833/`

Approved implementation design:

`docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`

## Task

Perform a pre-commit governance/baseline check and prepare the repository so the Product Owner can manually create the **Q-009 Approved Design Git Baseline**.

### 1. Re-read governance sources

Read at minimum:

- `AGENTS.md`
- `docs/requirements/Q-009-Requirement.md`
- `docs/architecture/q-009-trusted-actor-authorization-architecture.md`
- `docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`
- `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`
- `docs/lessons/2026-08-26-q-009-trusted-actor-authorization-implementation-design.md`
- latest V5 approval review package

Confirm that the repository state is consistent with the approved decisions.

### 2. Inspect Git state carefully

Run and record:

- `git status`
- `git diff --stat`
- `git diff`
- relevant untracked-file inspection
- current branch
- current HEAD

Classify changed/untracked files into:

A. Files that belong to the Q-009 approved design baseline  
B. Pre-existing unrelated/untracked artifacts that must remain untouched  
C. Any unexpected implementation/runtime change

There must be **no Q-009 implementation code** in this baseline.

### 3. Verify baseline scope

The approved design baseline may include the Q-009 governance/design artifacts necessary to preserve the approved state, including:

- Requirement approval metadata
- Architecture approval metadata
- ADR-011 accepted metadata
- Implementation Design V1
- Implementation Design approval metadata
- Q-009 design Lessons Learned
- Q-009 V4/V5 review artifacts and their transfer ZIPs, if repository governance requires review history to be committed

Do not silently include unrelated Q-007/Q-008 transfer artifacts or other historical untracked files merely because they exist.

If repository policy makes the correct inclusion set ambiguous, stop and report the ambiguity instead of guessing.

### 4. Verification

Run the verification required by repository governance for a documentation/design-only baseline.

At minimum, confirm:

- no Java/runtime implementation change;
- no POM/dependency change;
- no Flyway migration;
- no application configuration/security runtime change;
- no Docker/Kubernetes runtime change;
- no Redis/Kafka/frontend implementation change;
- Q-008 remains unauthorized;
- Q-009 implementation remains unauthorized.

Run tests only if AGENTS.md/current governance requires them for this baseline. Do not create implementation changes to make tests pass.

### 5. Create a NEW review package

Never overwrite an existing review directory.

Create a new timestamped review directory similar to:

`review/q-009/review-v6-approved-design-git-baseline-YYYYMMDD-HHMMSS/`

The package should contain, at minimum:

- `Summary.md`
- `BaselineScope.md`
- `ArchitectureReview.md`
- `DesignIntegrityCheck.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `Verification.md`
- `OutstandingItems.md`
- `GateStatus.md`
- `PhaseReviewIndex.md`

Also create a ZIP transfer package for that exact review directory.

The review must explicitly state whether the approved design is:

**READY FOR MANUAL GIT COMMIT: YES/NO**

### 6. Git safety boundary

Do **NOT** execute:

- `git add`
- `git commit`
- `git push`
- `git reset`
- `git clean`
- `git stash`

The Product Owner performs Git commit manually after Architect review.

Do not modify or delete unrelated untracked artifacts.

## Expected Gate Result

If all checks pass, the expected result is:

- Q-009 Approved Design Baseline: READY FOR MANUAL GIT COMMIT
- Q-009 Implementation: NOT STARTED
- Q-009 Implementation Authorized: NO
- Next action: Product Owner manually commits the approved Q-009 design baseline
- After that commit is confirmed, a separate explicit **Q-009 Implementation Authorization** will be issued.

Do not interpret this prompt as implementation authorization.

## Final Response

Return a concise execution receipt containing:

1. review result;
2. exact new review directory;
3. exact ZIP path;
4. files proposed for the approved design baseline;
5. verification result;
6. `READY FOR MANUAL GIT COMMIT: YES/NO`;
7. confirmation that no Git write operation was performed;
8. confirmation that Q-009 implementation was not started;
9. blockers/outstanding items, if any.

