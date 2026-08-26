# Codex Prompt — Q-009 V8 Mandatory Runtime Verification

## Objective

Continue **Q-009 — Trusted Actor and Authorization Foundation** from the existing V7 implementation state.

This task is **verification and evidence completion only** unless a verified defect requires the smallest possible Q-009-scoped fix.

The V7 review must NOT be treated as approved.

Current gate state from V7:

- Requirement Conformance: PASS
- Architecture Conformance: PASS
- ADR-011 Conformance: PASS
- Maven verification: BUILD SUCCESS
- Tests: 58 total, 0 failures, 0 errors, 1 skipped
- Security Review: FAIL because mandatory runtime evidence is incomplete
- Verification: FAIL
- Q-009 Implementation Complete: NO
- Ready for Architect Implementation Review: NO
- Ready for Git Commit: NO
- Q-008 Implementation Authorized: NO

The purpose of V8 is to obtain the missing mandatory runtime evidence honestly and reproducibly.

---

## 1. Re-establish the baseline first

Before changing anything, read:

- `AGENTS.md`
- `docs/requirements/Q-009-Requirement.md`
- `docs/architecture/q-009-trusted-actor-authorization-architecture.md`
- `docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`
- `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`
- latest Q-009 V7 implementation review package
- relevant Q-009 implementation and test files

Record:

- current branch
- current HEAD
- `git status`
- `git diff --stat`
- existing Q-009 implementation changes
- unrelated/pre-existing untracked files

Do not overwrite, delete, stage, commit, reset, clean, or stash anything.

Confirm that V7 implementation changes are still present and that no unexpected unrelated runtime change has appeared.

If the baseline cannot be understood safely, STOP and report the blocker.

---

## 2. Mandatory environment capability check

Inspect the actual execution environment before attempting verification.

Check and record at minimum:

- Java version
- Maven version
- Docker availability/version
- Docker daemon accessibility
- Docker Compose availability/version
- `kubectl` availability/version
- Kustomize support (`kubectl kustomize`) or repository-approved equivalent
- ports/services relevant to the project's MySQL/Redis/Kafka verification
- whether required project scripts already exist

Do not assume a command is unavailable merely because one invocation fails. Distinguish:

- binary missing;
- daemon/service unavailable;
- permission failure;
- PATH issue;
- configuration/context issue;
- repository script issue.

Do not install system software, change host configuration, or use privileged/destructive commands unless existing repository governance explicitly permits it.

---

## 3. Mandatory MySQL 8.4 runtime verification

The highest-priority missing evidence is the real MySQL integration verification for Q-009.

The authoritative Actor Mapping / Capability Grant persistence path must be verified against **actual MySQL 8.4**, not H2 or mocks.

Prefer the repository's existing Docker Compose / test infrastructure.

If Docker is available:

1. Start only the required disposable/project-approved infrastructure using existing repository definitions.
2. Confirm the MySQL container/image/version is actually MySQL 8.4 as required by the approved design/governance.
3. Wait for readiness using the repository-approved health/readiness mechanism.
4. Run the Q-009 MySQL integration test(s), including `Q009MySqlIntegrationTests` or their current exact equivalent.
5. Confirm the test is **executed**, not skipped.
6. Capture:
   - command;
   - MySQL version;
   - test count;
   - pass/fail;
   - relevant Flyway result;
   - relevant schema/migration evidence.
7. Tear down only disposable infrastructure that this verification task itself started, and only using repository-approved commands.

Do NOT delete persistent developer data or unrelated containers/volumes.

If the test remains skipped, investigate the documented activation mechanism and execute it correctly. Do not alter the test merely to remove the skip unless the test configuration itself is demonstrably defective and a minimal Q-009-scoped fix is justified.

If actual MySQL 8.4 cannot be executed in this environment, record the exact blocker and keep the gate FAIL.

---

## 4. Docker Compose infrastructure verification

Using the repository's existing verification procedure, validate the infrastructure required by current governance.

At minimum verify the applicable project baseline services, such as:

- MySQL
- Flyway/migrations
- Redis
- Kafka

Use `AGENTS.md`, repository scripts, and existing review conventions as the source of truth.

Capture commands and results.

Do not redesign Docker Compose and do not upgrade/change infrastructure versions merely to obtain PASS.

If a real defect in the Q-009 change breaks the existing infrastructure verification, make only the smallest approved-scope correction, explain it, and rerun all affected verification.

---

## 5. Kubernetes / Kustomize verification

Validate the repository's Kubernetes manifests according to existing governance.

At minimum render/validate:

- base
- test
- prod

Prefer the repository's established commands.

If supported, use `kubectl kustomize` or the repository-approved equivalent.

This is a render/configuration verification unless governance explicitly requires a live Kubernetes cluster. Do not deploy to a real cluster merely for this review.

Record the exact commands and results.

If `kubectl`/Kustomize is genuinely unavailable and no repository-approved alternative exists, do not fake PASS. Record the blocker and keep the relevant gate FAIL.

---

## 6. Full regression verification

After runtime/environment checks, rerun the complete required Maven verification.

Confirm:

- compilation succeeds;
- all ordinary tests pass;
- Q-009 integration tests required by governance actually execute;
- no mandatory Q-009 test is skipped;
- existing tests were not weakened/deleted;
- no Q-008 functionality was introduced.

Report exact test counts, including skipped tests.

A skipped test is acceptable only if it is unrelated to a mandatory Q-009 acceptance gate and repository governance allows it. Explain every skip.

---

## 7. Security verification

Re-evaluate the V7 Security Review using the new runtime evidence.

Explicitly verify:

1. trusted actor data cannot be injected from arbitrary caller-controlled request data;
2. authorization uses trusted server-side context;
3. missing/invalid actor state fails closed as approved;
4. missing capability is denied;
5. no implicit wildcard/admin capability exists;
6. Actor Mapping / Capability Grant persistence behaves correctly on actual MySQL 8.4;
7. duplicate/uniqueness/constraint behavior required by the design works on actual MySQL;
8. request/thread context cannot leak privilege across requests/tests;
9. error handling does not expose sensitive authorization internals;
10. Q-008 remains outside implementation scope.

Do not mark Security Review PASS unless all mandatory evidence is present.

---

## 8. Scope discipline

This is not an architecture redesign task.

Do NOT:

- expand Q-009 scope;
- implement Q-008;
- introduce speculative security features;
- refactor unrelated modules;
- change ADR-011;
- weaken tests or gates;
- substitute H2/mock success for mandatory MySQL 8.4 evidence;
- claim Docker/Kubernetes verification without executing it;
- mark an unavailable verification as PASS.

If the approved design is found to be incorrect or impossible to implement safely, STOP and document the architecture conflict for Architect review.

---

## 9. Create a NEW V8 review package

Never overwrite V7 or any prior review.

Create a timestamped directory similar to:

`review/q-009/review-v8-mandatory-runtime-verification-YYYYMMDD-HHMMSS/`

Include at minimum:

- `Summary.md`
- `EnvironmentCapabilities.md`
- `MySql84Verification.md`
- `DockerComposeVerification.md`
- `KustomizeVerification.md`
- `RegressionVerification.md`
- `SecurityReview.md`
- `ArchitectureConformance.md`
- `GitStatus.txt`
- `GitDiffStat.txt`
- `ChangedFiles.txt`
- `OutstandingItems.md`
- `GateStatus.md`
- `PhaseReviewIndex.md`

Include raw/concise command evidence where useful, without exposing passwords, tokens, credentials, or other secrets.

Also create a ZIP transfer package for the exact V8 review directory.

---

## 10. V8 gate rules

Only if **all mandatory verification succeeds**, V8 may state:

- Requirement Conformance: PASS
- Architecture Conformance: PASS
- ADR-011 Conformance: PASS
- MySQL 8.4 Runtime Verification: PASS
- Docker Compose Verification: PASS
- Kustomize Verification: PASS
- Security Review: PASS
- Verification: PASS
- Q-009 Implementation Complete: YES
- Ready for Architect Implementation Review: YES
- Ready for Git Commit: NO

Even on full PASS:

**Ready for Git Commit must remain NO.**

Architect implementation approval is still required before the Product Owner is told to commit.

If any mandatory environment check cannot be performed, state precisely:

- what was unavailable;
- what was attempted;
- why it could not be completed;
- what evidence remains missing;

and keep:

- Verification: FAIL
- Q-009 Implementation Complete: NO
- Ready for Architect Implementation Review: NO
- Ready for Git Commit: NO

Do not manufacture a green gate.

---

## 11. Git safety boundary

Do NOT execute:

- `git add`
- `git commit`
- `git push`
- `git reset`
- `git clean`
- `git stash`

The Product Owner controls Git commits after Architect approval.

---

## Final response

Return a concise execution receipt containing:

1. V8 result;
2. exact review directory;
3. exact ZIP path;
4. environment capability result;
5. actual MySQL 8.4 verification result;
6. Docker Compose verification result;
7. Kustomize base/test/prod verification result;
8. Maven/regression result and exact test counts;
9. Security Review result;
10. any code/config changes made to correct verified defects;
11. `Q-009 Implementation Complete: YES/NO`;
12. `Ready for Architect Implementation Review: YES/NO`;
13. `Ready for Git Commit: NO`;
14. confirmation that Q-008 was not implemented;
15. confirmation that no Git write operation was performed;
16. exact blockers/outstanding items if any.
