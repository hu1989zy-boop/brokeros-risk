# Q-004 Patch-01 Review

## Status

PATCH IMPLEMENTED — LIVE CI RERUN REQUIRED

The script defect is fixed and all locally executable checks pass. This host
has no Docker daemon, so the real GitHub Actions infrastructure step must run
again before Q-004 runtime verification can be marked PASS.

## Root Cause

GitHub Actions run
[`31512713728`](https://github.com/hu1989zy-boop/brokeros-risk/actions/runs/31512713728)
failed only in `Docker and infrastructure verification`; all preceding Maven,
static, and Kustomize steps passed.

The old script checked `/api/health` with:

```sh
grep -q '"success":true'
```

BrokerOS Risk does not expose a `success` boolean. The approved `ApiResponse`
contract returns `"code":"SUCCESS"`, with health state under
`"data":{"status":"UP"}`. The request therefore succeeded, but `grep -q`
returned status 1. Because that pipeline was unguarded under `set -e`, the
script exited without naming the failed assertion.

The EXIT trap then printed the last 200 lines from every service. Kafka's normal
`Kafka Server started` line appeared in those diagnostics even though Kafka was
not the cause. The log ordering made a backend response-contract assertion look
like a Kafka startup failure.

## Fix

Only `scripts/verify-infrastructure.sh` changed.

- The application health assertion now requires both `"code":"SUCCESS"` and
  `"status":"UP"`, matching the existing API contract and tests.
- HTTP responses are captured before inspection, avoiding an opaque pipeline
  failure.
- Each preflight, Compose, service-health, MySQL/Flyway, Redis, Kafka, backend,
  log-scan, cleanup, and overall stage emits explicit INFO/PASS/FAIL output.
- Commands and assertions that can return non-zero are handled in explicit
  conditionals with stage-specific failure messages.
- Fatal-pattern `grep` distinguishes the expected no-match status 1 from a real
  grep execution error.
- Cleanup disables fail-fast behavior, reports diagnostic/cleanup failures,
  preserves the original verification status when both verification and
  cleanup fail, and fails independently when verification passed but cleanup
  did not.
- Signal traps now convert HUP/INT/TERM into explicit non-zero exit statuses and
  let the EXIT cleanup preserve them.

## Scope and Architecture Impact

- Business code: no change.
- API contract: no change; the script was aligned to the existing contract.
- Database/Flyway migrations: no change.
- Kafka topics and Redis keys: no change.
- Docker Compose and Kubernetes manifests: no change.
- Architecture and accepted ADRs: no change; no new ADR is required.
- Risk Case, Rule Engine, Account Control, Audit, adapters, MT4/MT5, CRM, and
  external integrations: no impact.

## Verification

Executed locally:

```bash
sh -n scripts/verify-infrastructure.sh
sh scripts/verify-static.sh 8bf42bc
git diff --check
cd backend && mvn test
cd backend && mvn package
```

All commands passed. Each Maven lifecycle reported 12 tests with 0 failures,
0 errors, and 0 skipped; packaging completed successfully.

A temporary `/private/tmp` Docker command stub exercised the script without
changing the repository or creating infrastructure:

- Full success path: every stage, cleanup, and overall verification emitted
  PASS and the script exited 0.
- Wrong `/api/health` contract: emitted `FAIL [backend-health]` and exited 1.
- Wrong contract plus cleanup failure: retained the original exit status 1 and
  reported the cleanup failure separately.
- Successful verification plus cleanup failure: exited 1 instead of reporting
  an overall PASS.

The existing backend test verifies the authoritative API shape:

```text
$.code = SUCCESS
$.data.status = UP
```

Real Docker/MySQL/Flyway/Redis/Kafka execution is not claimed by this local
review. Push the patch and require a successful GitHub Actions rerun for that
evidence.

## Development Standards Compliance

- AGENTS.md: preflight documents and applicable skills were read; the patch is
  restricted to the requested verification concern.
- Architecture: the modular monolith and infrastructure layout are unchanged.
- ADR: ADR-006's repository-owned blocking verification remains intact; no
  durable decision changed.
- API: no API implementation changed; verification now matches `ApiResponse`.
- Database: the sole V1 migration is unchanged and no business DDL was added.
- Security: credentials remain ephemeral and are never printed; diagnostic
  output remains scoped to the isolated Compose project.
- Auditability: named stages and preserved exit status improve CI evidence; no
  critical business action is involved.
- Skills: `development-standards.md` and `ci-integration-verification.md` were
  applied. No skill update is needed because explicit failure handling and
  cleanup-status preservation are already documented there.

## Outstanding Item

Run the existing GitHub Actions workflow with this patch. Do not mark Q-004
runtime verification PASS solely from the local command-stub tests.
