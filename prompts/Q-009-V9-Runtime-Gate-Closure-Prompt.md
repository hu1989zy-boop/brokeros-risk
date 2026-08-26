# Q-009 V9 — Mandatory Runtime Verification & Final Gate Closure

You are continuing BrokerOS Risk Q-009 after V8 Mandatory Runtime Verification.

## Confirmed prerequisite

The host runtime blocker identified in V8 has now been resolved by the operator.

Verified host evidence:
- Docker CLI: Docker 29.7.2
- Docker Compose: v5.4.0
- Docker daemon: running via Docker Desktop
- Docker context: desktop-linux
- Architecture: aarch64 / Apple Silicon
- `docker run --rm hello-world`: PASS

Do NOT reinterpret the prior V8 runtime failure as an implementation defect. Re-run the mandatory gates now that Docker is available.

## Objective

Perform Q-009 V9 Runtime Gate Closure. The purpose of this run is to obtain real runtime evidence for all previously blocked mandatory checks and determine whether Q-009 is actually ready for architect/final review and Git commit.

Do not weaken tests, bypass gates, replace required MySQL 8.4 verification with the host MySQL 5.7 instance, or change production behavior merely to obtain PASS.

## Safety constraints

1. Never run Flyway `clean()` or destructive integration tests against any pre-existing/local/shared MySQL database.
2. Use only a disposable, isolated Docker-managed MySQL 8.4 runtime for destructive database verification.
3. Do not use the host MySQL 5.7 instance for Q-009 mandatory integration verification.
4. Do not delete or overwrite previous review packages. Create a new V9 review directory/package.
5. Preserve all existing Q-008/Q-009 review history.
6. Do not perform `git commit`, `git push`, history rewrite, reset, checkout/discard of unrelated changes, or cleanup of user files.
7. Do not silently modify approved architecture/ADR/design. If implementation conflicts with approved design, stop and record the conflict.
8. Do not mark a skipped mandatory test as PASS.

## Step 1 — Baseline and environment evidence

Record at minimum:
- current branch
- current HEAD
- `git status --short`
- `git diff --stat`
- `docker --version`
- `docker compose version`
- `docker info` relevant runtime/architecture information
- relevant Q-009 requirement, approved architecture, ADR-011, approved implementation design and V8 review references

Confirm that Docker is now usable from the same execution environment in which verification will run.

## Step 2 — Inspect the existing Q-009 implementation before changing anything

Review the implementation and existing verification configuration against the approved Q-009 artifacts.

Determine whether any code/config change is genuinely required for runtime correctness. Prefer verification-only execution if the implementation already matches the approved design.

If a change is necessary:
- make only the minimum change required by the approved design;
- explain why V8 could not reveal it;
- add/update tests as appropriate;
- include the change explicitly in V9 evidence.

Do not broaden scope.

## Step 3 — Disposable MySQL 8.4 mandatory runtime verification

Provision an isolated MySQL 8.4 runtime using the repository's approved Docker/Compose mechanism where available. If a dedicated test container is required, it must be clearly disposable and isolated.

Verify the actual server version from inside the running database and capture evidence showing that it is MySQL 8.4.x.

Then execute the Q-009 mandatory MySQL integration verification, including `Q009MySqlIntegrationTests`.

Requirements:
- the mandatory integration test must actually execute;
- zero mandatory Q-009 tests may be skipped;
- Flyway migration/clean behavior must target only the disposable database;
- capture Maven test counts and PASS/FAIL evidence;
- if the test still skips, treat the gate as FAIL and diagnose the exact skip condition instead of overriding it.

## Step 4 — Docker Compose runtime verification

Run the repository's required Docker Compose verification for Q-009 using the real Docker daemon.

Validate at minimum the services and runtime conditions required by the approved Q-009 design. Capture:
- compose config validation;
- image/container startup result;
- health/readiness evidence where applicable;
- MySQL 8.4 runtime evidence;
- application/database integration evidence required by Q-009;
- clean shutdown/cleanup result.

Do not leave disposable verification containers running unnecessarily after evidence is captured.

## Step 5 — Full mandatory verification

Re-run all mandatory Q-009 verification gates required by the approved requirement/design and repository governance, including relevant Maven tests and configuration/static checks.

Do not rely solely on previous V8 results for gates affected by the runtime prerequisite. Re-run them where the newly available runtime can materially affect the conclusion.

## Step 6 — Security Review closure

Re-evaluate the Q-009 Security Review using the new real MySQL 8.4/runtime evidence.

Explicitly determine whether the V8 Security Review blocker is now closed.

Security PASS must be evidence-based. If any mandatory security condition remains unverified or failed, Q-009 must remain blocked.

## Step 7 — Final gate decision

Produce an explicit gate matrix covering at least:
- Requirement alignment
- Architecture alignment
- ADR-011 alignment
- Implementation Design alignment
- Implementation review
- Maven/unit tests
- Q009MySqlIntegrationTests actual execution
- MySQL 8.4 runtime
- Flyway/runtime database verification
- Docker Compose verification
- Security Review
- outstanding blockers
- Ready for Architect/Final Review
- Ready for Git Commit

Only set `Ready for Git Commit: YES` if every mandatory Q-009 gate is genuinely PASS and there are no unresolved blockers.

If anything fails, do not conceal or downgrade it. Record exact evidence, root cause, and the smallest recommended next action.

## Step 8 — Review package

Create a NEW immutable review package for this run, following the repository's current review conventions, with a name equivalent to:

`review/q-009/review-q-009-v9-runtime-gate-closure-<timestamp>/`

and corresponding ZIP:

`review/q-009/review-q-009-v9-runtime-gate-closure-<timestamp>.zip`

Do not overwrite V1–V8.

The package should contain the repository-standard review artifacts and enough evidence to independently audit the V9 decision, including environment/runtime verification, test results, architecture/security assessment, git status/diff evidence, outstanding items, summary, and review index as applicable under current AGENTS.md conventions.

Ensure the ZIP actually exists before finishing.

## Final response

Return a concise operator-facing summary containing:
1. V9 result: PASS / FAIL
2. MySQL 8.4 runtime: PASS / FAIL
3. `Q009MySqlIntegrationTests`: executed PASS / executed FAIL / SKIPPED
4. Docker Compose verification: PASS / FAIL
5. Security Review: PASS / FAIL
6. total Maven test result
7. whether implementation files changed during V9
8. outstanding blockers
9. `Ready for Git Commit: YES/NO`
10. exact path of the new V9 review directory
11. exact path of the new V9 ZIP

Do not commit. Stop after generating the V9 evidence/package and wait for architect review and explicit operator approval.
