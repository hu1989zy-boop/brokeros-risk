# Q-018 Implementation Review Summary

- Task: Q-018 — Association Management (Group C)
- Lifecycle stage: Implementation + Implementation Verification
- Authorized scope: six Group C association operations, Option A existing-reference previews,
  on-case pickers, AssociationsPanel, and the approved bootstrap capability grant
- Baseline: `7d886c9` on `main` (one pre-existing commit ahead of `origin/main`)
- Package generated: 2026-09-03 16:41 +08:00
- Gate Decision: **BLOCKED**

The authorized frontend implementation is complete within the permitted file
boundary and is ready for Claude Code's independent review. The six operations
reuse the Q-017 descriptor registry and `useCaseAction` runner, external inputs
use a debounced typed preview flow, decision/action on-case options are derived
from loaded detail/history, and the detail page renders a deliberately labelled
bounded association view. No Java, backend test, Flyway migration, SQL, endpoint,
or dependency was changed.

The final locally executable verification passed: strict TypeScript, the
production build, static verification, and **148/148 Vitest tests**. The live
Q-018 Playwright spec was discovered and reported **1 skipped** because live
credentials and seeded inputs were unavailable.

The gate is BLOCKED for two contract defects that cannot be repaired within the
authorized zero-backend scope:

1. Approved Q-018 documents require `ev-/dc-/ac-/ao-`, while the committed
   backend accepts `ev-/dec-/act-/aoc-`. Per the implementation prompt's authority
   rule, the Q-018 client implements the approved short prefixes. Decision,
   action, and outcome preview/association cannot therefore work against the
   current backend.
2. Risk Case detail/history does not expose an evidence association's
   `associationEventRef`, replacement evidence ref, or an outcome ref. The
   disposition target cannot be selected or preview-confirmed as designed, and
   history cannot support a complete current-association projection.

## Delivered scope

- Six typed Group C request/response paths with exact body construction and no
  actor identity.
- One reference-preview repository over the four existing authenticated
  `GET /{ref}` endpoints, with strict small response parsers.
- Debounced `ReferenceInput` states for format, loading, confirmed preview, 404,
  403, mismatched response, and generic failure; external-ref submission remains
  disabled until preview confirmation.
- Detail/history-derived decision and action pickers, plus a guarded manual UUID
  fallback for the unavailable evidence association event ID.
- `AssociationsPanel` with six Group C action entry points and an explicit warning
  that non-decision association data is reconstructed from the loaded history
  page, not an authoritative current projection.
- Operator bootstrap grants for `risk-case:associate`, `evidence:read`,
  `decision:read`, `action:read`, and `action-outcome:read`.
- A 45-test Q-018 Vitest/RTL/MSW addition and a secret-artifact-disabled live
  Playwright association slice.
- Q-018 Lessons Learned and this new non-overwriting review package.

## Acceptance criteria

| AC | Result | Evidence / blocker |
| --- | --- | --- |
| 1 | **BLOCKED** | All six operations pass MSW-backed component flows, but live execution was unavailable and the approved `dc-/ac-/ao-` refs are rejected by the current backend contract. |
| 2 | **FAIL** | External preview behavior passes tests and decision/action targets are selectable from displayed state. The evidence disposition target event ID is absent from detail/history and has no preview endpoint, so it falls back to warned manual UUID input rather than the approved on-case picker + preview. |
| 3 | **PASS** | Six conflict cases reload version 7→8, preserve input, and retry with expectedVersion 8 through the shared runner. |
| 4 | **BLOCKED** | The live decision→select→action sequence was not executed and cannot use the approved short prefixes against the committed backend. |
| 5 | **PASS** | Backend/Flyway task diff is empty, all exact-body tests exclude `actorRef`, and the only runtime change outside `frontend/` is the approved bootstrap grant. |
| 6 | **FAIL** | Typecheck/build and 148 tests pass; the required live Playwright slice reported 1 skipped rather than passing. |

## Scope boundary result

- No backend Java, backend test, Flyway migration, SQL, persistence, Kafka,
  Redis, adapter, Kubernetes, or endpoint change.
- No evidence/decision/action creation, Group E case creation, JWT claim parsing,
  capability probe, actor-body field, or client-owned business rule.
- No dependency manifest or lockfile change.
- No staging, commit, push, or modification of an existing timestamped review
  directory/archive. Two pre-existing Q-016/Q-017 untracked ZIPs were preserved.

## Technical recommendation

Amend the Q-018 reference contract to the already-established backend prefixes
`ev-/dec-/act-/aoc-`; changing persisted foundational identifiers would create
greater compatibility and data-integrity risk. Separately authorize an additive,
bounded authoritative association projection (including evidence event IDs,
effective replacement refs, action/outcome refs) or revise FR-02/FR-07. Only
after those governance changes should the frontend contract and live test be
updated and rerun.

## Exit condition

Stop at the Q-018 implementation-verification handoff. This package is evidence
for independent review; it does not approve Q-018 or authorize another
Requirement.
