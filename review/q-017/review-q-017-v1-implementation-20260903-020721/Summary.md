# Q-017 Implementation Review Summary

- Task: Q-017 — Risk Console Case Lifecycle Operations
- Lifecycle stage: Implementation + Implementation Verification
- Authorized scope: V1 Groups A+B+D only
- Baseline: `7a36bc7` (`docs: Q-017 Risk Console case lifecycle operations — approved + §16.5-B bundle authorized`)
- Package generated: 2026-09-03 02:07 +08:00
- Gate Decision: **PASS WITH CONDITIONS**

The authorized frontend implementation is ready for Claude Code's independent
implementation review. The eleven operations are implemented through a
declarative registry and one TanStack Query runner, the detail page exposes only
status-valid actions, terminal operations use a distinct confirmation step, and
version conflicts reload the authoritative case while preserving form input for
retry. The Q-016 API/authentication client is reused unchanged.

The final Node verification passed: strict TypeScript and the production build
passed, and Vitest/RTL/MSW passed **103/103** tests. Playwright discovered both
live specs and reported **2 skipped** because live credentials and seeded case
inputs were not supplied. Therefore this package does not claim live lifecycle
success or Q-017 acceptance.

## Delivered scope

- Typed requests and response parsing for all approved Q-017 operations.
- Eleven descriptors: assign, change priority, three review transitions,
  resolve, close, cancel, resume, reopen, and note correction.
- Generic action form/confirmation UI and status-filtered action bar.
- Shared mutation behavior for pending state, typed errors, success
  invalidation, and conflict reload/input preservation/retry.
- Notes panel that binds correction to the selected note reference without
  inventing unavailable note-content reads.
- Exact development operator capability set `{read,note,assign,review,resolve,
  close,cancel,reopen}` plus aligned launcher documentation.
- Parameterized 11-operation RTL/MSW matrix and live Playwright lifecycle spec.
- Q-017 Lessons Learned and this timestamped review package.

## Acceptance criteria

| AC | Result | Evidence / outstanding condition |
| --- | --- | --- |
| 1 | **FAIL — live evidence outstanding** | All 11 UI/repository operations pass MSW-backed end-to-end component tests and refresh detail/history. No operation was executed against a live backend in this environment. |
| 2 | **PASS** | Resolve/close/cancel cannot reach the request submission until required fields validate and the separate confirmation step is accepted. |
| 3 | **PASS** | Each of 11 parameterized conflict cases reloads version 7→8, preserves input, retries, and sends version 8. |
| 4 | **PASS** | Status map tests cover all six states; 11 typed `403` paths pass; backend remains authoritative. |
| 5 | **PASS** | No actor identity is body-supplied; `backend/` and Flyway task diffs are empty; no C/E implementation exists. |
| 6 | **FAIL — live Playwright skipped** | Typecheck/build and 103 frontend tests pass. Q-017 live Playwright was discovered but skipped for missing `E2E_OPERATOR_PASSWORD` and `E2E_Q017_CASE_NUMBER`. |

## Scope boundary result

- No Java, backend test, Flyway migration, database, Kafka, Redis, adapter,
  Kubernetes, or production endpoint change.
- No Group C association commands and no Group E case creation.
- No JWT claim parsing, capability probe, client-side authorization decision,
  or actor field in command bodies.
- No staging, commit, push, or modification of an existing timestamped review
  package. The pre-existing untracked Q-016 ZIP was preserved.

## Exit condition

Stop at the Q-017 implementation-verification handoff. Claude Code should
independently review code and evidence, then run the live lifecycle slice when
the required local-only inputs are available. This package does not approve the
implementation or authorize a subsequent Requirement.
