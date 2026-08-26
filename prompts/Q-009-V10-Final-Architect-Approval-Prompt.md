# Q-009 V10 — Final Architect Approval & Closure Recording

## Role and objective
Act as the implementation agent for the BrokerOS Risk repository.

This round is **Q-009 V10 — Final Architect Approval & Closure Recording**. Its purpose is to formally record final architect approval and close Q-009 after the successful V9 mandatory runtime verification. This is a governance/documentation/review-package task, not a new feature-development round.

## Authoritative V9 baseline
Treat V9 evidence as the preceding review baseline, but independently reconcile it against repository artifacts before approving closure.

V9 reported:
- MySQL 8.4.11 runtime: PASS
- `Q009MySqlIntegrationTests`: 1/1 PASS, 0 skipped
- Maven: 58/58 PASS, 0 skipped
- Docker Compose runtime: PASS
- MySQL / Redis / Kafka / Backend health: PASS
- Flyway V1 -> V2 and restart/idempotence: PASS
- Kustomize base / test / prod: PASS
- Security Review: PASS
- Architecture / ADR-011 / Implementation Design: PASS
- Q-008 boundary preserved
- host MySQL 5.7 not used for destructive verification
- disposable Docker resources cleaned up

V9 also changed only the integration-test assertion in `Q009MySqlIntegrationTests.java` to recognize the actual MySQL 8.4 CHECK-constraint violation response without weakening the required database constraint.

The remaining lifecycle gap is:
`Architect Final Approval Recorded: NO`

## 1. Establish repository baseline
Before changes, capture and review:
- current branch and HEAD
- `git status --short`
- `git diff`
- `git diff --stat`
- relevant Q-009 artifacts
- Q-009 review history through V9

Do not reset, stash, discard, overwrite, or silently modify unrelated user changes.

## 2. Reconcile the complete Q-009 evidence chain
Read the actual authoritative artifacts, including at minimum:
- Q-009 Requirement
- Q-009 Architecture
- ADR-011
- Q-009 Implementation Design
- approved design baseline
- Q-009 implementation
- V8 Mandatory Runtime Verification
- V9 Runtime Gate Closure
- applicable Skill, Lessons Learned, phase/review indexes and outstanding-items records

Verify this chain from contents, not filenames:

Requirement -> Architecture -> ADR -> Implementation Design -> Approved Design Baseline -> Implementation -> Mandatory Runtime Verification -> Security Review -> Final Architect Approval

If there is any material contradiction, missing mandatory evidence, unresolved blocker, unauthorized scope expansion, or security/runtime uncertainty, STOP closure and record V10 as FAIL/BLOCKED.

## 3. Record Final Architect Approval
Only if independently supported by evidence, formally record:
- Q-009 Final Status: PASS / APPROVED
- Requirement: PASS
- Architecture: PASS
- ADR-011: PASS / APPROVED
- Implementation Design: PASS / APPROVED
- Implementation: PASS
- Mandatory Runtime Verification: PASS
- MySQL 8.4 Verification: PASS
- Maven mandatory tests: PASS, zero mandatory skips
- Docker Compose Verification: PASS
- Flyway Verification: PASS
- Kustomize Verification: PASS
- Security Review: PASS
- Scope / Boundary Review: PASS
- Outstanding Blocking Items: NONE
- Architect Final Approval: APPROVED
- Ready for Git Commit: YES

Clearly distinguish implementation completion, runtime verification, architect approval recording, and Git commit.

**Do not run `git commit` or `git push`. The user performs Git commit manually after external review.**

## 4. Governance/documentation updates
Update only the existing governance/status artifacts required by repository conventions so all Q-009 records consistently reflect the final decision. This may include requirement/architecture/design status, ADR status, phase/review index, Skill, Lessons Learned, Outstanding Items, or closure records where appropriate.

Do not create redundant documents merely for completeness.

Do not modify production code unless a genuine closure-blocking defect is discovered. If such a defect exists, do not silently fix it: mark V10 FAIL/BLOCKED, document it, and do not approve Q-009.

## 5. Mandatory verification
Perform real verification proportional to final closure.

Repository integrity:
- `git status --short`
- `git diff`
- `git diff --stat`
- verify no accidental unrelated modifications

Documentation consistency:
- all final Q-009 records must agree on approval, runtime/security results, blockers, and Git readiness

Regression:
- run the repository's appropriate Maven test command
- all mandatory tests must PASS
- no mandatory Q-009 test may be skipped

If the MySQL integration test requires database reset/Flyway clean, use the already-established isolated disposable Docker MySQL 8.4 approach.

**Never use the user's existing host MySQL 5.7 for destructive verification.**
Do not weaken or skip tests to obtain PASS.

## Strict constraints
Do not:
- implement Q-008
- start Q-010 or another future requirement
- introduce unrelated refactoring
- change architecture without an approved ADR
- change dependencies without necessity/approval
- weaken tests or fabricate evidence
- run `git commit` or `git push`
- destructively reset Git state
- delete unrelated files

`Ready for Git Commit: YES` is a gate decision only; Codex must leave the commit to the user.

## 6. Required V10 review package
Create a NEW timestamped directory without overwriting V1-V9, following repository conventions, preferably:

`review/q-009/review-q-009-v10-final-architect-approval-<timestamp>/`

Include sufficient independent closure evidence, at minimum equivalents of:
1. `Summary.md`
2. `ArchitectureReview.md`
3. `Verification.md`
4. `SecurityReview.md`
5. `OutstandingItems.md`
6. `GitStatus.txt`
7. `GitDiffStat.txt`
8. `ProjectTree.txt`
9. phase/review index evidence
10. final architect approval / closure record

Follow existing filenames if repository conventions differ.

The Summary must contain a final gate matrix covering Requirement, Architecture, ADR-011, Implementation Design, Implementation, Mandatory Runtime Verification, MySQL 8.4, Maven Tests, mandatory skips, Docker Compose, Flyway, Kustomize, Security, Scope, blockers, Architect Final Approval, and Git readiness.

Only record PASS/APPROVED values actually supported by evidence.

## 7. ZIP requirement
Create a ZIP of the exact V10 review package, following repository conventions, preferably:

`review-q-009-v10-final-architect-approval-<timestamp>.zip`

Verify that the ZIP:
- exists and is readable
- contains the expected V10 evidence
- does not overwrite previous packages
- does not accidentally contain unrelated/generated large artifacts

## Required final response
Report concisely:
1. V10 result: PASS / FAIL / BLOCKED
2. Architect Final Approval: APPROVED / NOT APPROVED
3. Ready for Git Commit: YES / NO
4. Production code changed: YES / NO
5. Tests executed and results
6. Runtime/database verification status
7. Documentation updated
8. Review directory path
9. ZIP path
10. Outstanding blockers

If PASS, explicitly state:

> Q-009 has completed Final Architect Approval & Closure Recording. It is ready for external review and, after external approval, manual Git commit.

Do not claim that Q-009 has been Git committed.

## Decision principle
The goal is a trustworthy closure decision, not a forced PASS. Missing runtime evidence, skipped mandatory tests, security uncertainty, scope violations, or inconsistent documentation must result in FAIL/BLOCKED rather than approval.
