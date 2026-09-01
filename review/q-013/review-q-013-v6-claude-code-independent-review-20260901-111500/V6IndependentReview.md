# Q-013 Independent Implementation Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§8–§12.

## Task ID

Q-013 — Action Provenance Foundation.

## Stage

Independent Review (of Codex's implementation, delivered as
`review/q-013/review-q-013-v5-implementation-20260831-235338.zip`).

## Scope Reviewed

The full Q-013 implementation diff (`com.brokeros.risk.action` module,
`V6__create_action_provenance_foundation.sql`, `ResultCode.java`,
`scripts/verify-static.sh`) and Codex's own package claims, checked
against actual source and independently executed tests — not accepted on
report authority alone (protocol §8).

## Files Inspected (direct reading)

- `ActionRecordingService.java` — full read. Traced execution against
  Implementation Design §11.1: authorize → `requireHuman` → fingerprint
  from **raw** fields (before domain parsing) → `queryPort.findOperation`
  replay check (returns immediately on match, before content validation
  or the Q-012 call) → content validation (parse `IntentText`, parse
  **single** `DecisionRef` — not a set) → Q-012 `confirmProvenance`
  (rejects only `NOT_FOUND` → `ACTION_DECISION_NOT_RECOGNIZED`; maps
  unavailability) → build context → `mutationPort.record`. Exact match,
  no deviation.
- `V6__create_action_provenance_foundation.sql` — full read. Three tables
  (`action_record`, `action_operation`, `action_access_log`); every
  constraint in Design §8.4 present; `decision_ref CHAR(40)` regex-checked
  with **no cross-module FK**; `status CHECK IN ('PROPOSED')` kept as a
  real, single-value, extensible-by-relaxation column (not omitted);
  `source CHECK IN ('MANUAL')`; `OCTET_LENGTH(intent_text) BETWEEN 1 AND
  4000`; three intra-module FKs (`action_operation.action_id`,
  `action_access_log.action_id` → `action_record.id`, `ON DELETE
  RESTRICT`); no join table, no history table. Correct.
- `ActionProvenanceView.java` — full read. **No `intentText` field at the
  type level** (structural guarantee); compact constructor enforces
  `RECOGNIZED`-complete / `NOT_FOUND`-null. Correct.
- `ActionController.java` — full read. Exactly two routes (`POST
  /api/actions`, `GET /api/actions/{actionRef}`), no `PATCH`/`PUT`,
  `@ConditionalOnWebApplication(SERVLET)`, actor only from
  `ActorContextProvider.currentContext()`. Correct.
- `git diff` on `ResultCode.java` (eight additive `ACTION_*` codes) and
  `scripts/verify-static.sh` (migration count 6, V6 shape checks,
  Q-013 dynamic-count enforcement) — additive-only, matching Codex's
  `GitDiffStat.txt`.

## Verification Executed

Independently re-ran the full repository-wide real-MySQL gate myself in a
disposable Docker/Java 21/MySQL 8.4 Linux environment (fresh
container/network), all five datasource families
(`Q009`/`Q010`/`Q011`/`Q012`/`Q013`) enabled:

```
Tests run: 204, Failures: 1, Errors: 0, Skipped: 0

Q012MySqlMigrationTests.migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart
expected: 1
 but was: 2
```

Exactly matches Codex's own reported result (same test, same numbers),
now confirmed in a second, independent environment. All eight Q-013
Action test classes passed cleanly (`ActionArchitectureTests` 4,
`ActionApplicationTests` 8, `ActionMetricsTests` 1,
`Q013SecurityMySqlIntegrationTests` 5, `Q013MySqlPersistenceTests` 6,
`Q013MySqlMigrationTests` 6, `ActionDomainTests` 5,
`ActionRestContractTests` 4 = 39), matching Codex's "22 non-database + 17
database = 39" claim exactly.

Docker resources (`brokeros-q013-verify-mysql`,
`brokeros-q013-verify-net`) removed after the run.

## Requirement Status

Q-013 Requirement V1 — all 9 `Q013-FR-XXX` items independently confirmed
implemented as specified. No missing requirement, no unrequested
addition, no vendor-operation vocabulary found.

## Architecture Status

Architecture V1 / ADR-015 — independently confirmed compliant: single
`DecisionRef` (no set/join table), no cross-module FK, no eligibility
service, `status` an extensible-but-`PROPOSED`-only enum, module
boundaries match §3.

## Design Compliance Status

Implementation Design V1 — independently confirmed compliant, in
particular §11.1's execution order (traced directly in source) and §8.4's
constraint-to-test mapping (every row present in the DDL).

## Test Status

204 tests, 1 failure, 0 errors, 0 skipped — independently reproduced,
identical to Codex's report. Q-013's own 39 tests: 39/39 pass.

## Findings

**No defect found in Q-013's own implementation.** Both the code review
and the independently executed tests support this.

**The one failing test, `Q012MySqlMigrationTests`, is the SAME
already-precedented bug class, now occurring for the THIRD time — and it
is exactly the latent follow-up this reviewer explicitly predicted during
Q-012's own review.** During Q-013's implementation, Codex added
migration V6, which made `Q012MySqlMigrationTests`'s hard-coded
"expected 1 post-V4 migration" assertion stale (V5+V6 = 2, not 1). This
same bug class was already found and fixed twice — Q-009's AC15 test and
Q-011's migration test — each time by deriving the expected count
dynamically from `flyway.info().pending().length`. Critically, this exact
occurrence was **forecast, not a surprise**:
`review/q-012/review-q-012-v6-claude-code-independent-review-20260831-224500/V7ClosureIndependentReview.md`
recorded that `Q012MySqlMigrationTests` "will fail the same way the moment
any future migration ... is added," and recommended it as a tracked
follow-up. Q-013's V6 is that future migration. `git diff` confirms Codex
did not touch the Q-012 file, correctly honoring Q-013's hard boundary.

## Remaining Risks

- This is now the **third** occurrence of the identical hard-coded
  -migration-count pattern across three different test files. Fixing only
  the one currently-failing instance (as done twice before) leaves any
  other file still using the `.target("N")`-then-hard-coded-count pattern
  latent, to break at the next module. A one-time repository-wide sweep
  for this exact pattern across all existing test files — converting each
  to the dynamic-count form — would end the recurrence permanently. This
  is a separate, broader task from the immediate one-file fix and is
  recommended below.

## Out-of-Scope Issues

None beyond what Q-013's Requirement/Architecture/Design already scope out
(Q-008, ActionOutcome, Execution, Account Control, approval workflow, Rule
Engine).

## Recommendation

1. **Accept Q-013's implementation.** It is a faithful, correct,
   independently-verified realization of Implementation Design V1 — no
   defect found in code review or in independently executed tests.
2. **Separately authorize the same narrow fix** already used twice (for
   Q-009's AC15 and Q-011's migration test), applied this time to
   `Q012MySqlMigrationTests.java` only, to unblock the all-module gate.
3. **Additionally, consider a one-time repository-wide sweep** for the
   remaining hard-coded-migration-count test pattern, as a separate task,
   so this stops recurring with every new module — this is the third
   occurrence and the pattern is now clearly systemic, not incidental.

## Gate Decision

**BLOCKED** — matching Codex's own honest self-assessment. Q-013's
implementation itself is sound (would independently warrant PASS on its
own merits), but the mandatory all-module real-MySQL regression gate is
not green, and per protocol §10 ("never weaken tests simply to obtain
PASS") that failure must be resolved through the same narrow,
separately-authorized fix used twice before — not silently worked around.
The Product Owner's decision on that fix (and, optionally, the broader
sweep) is the next step.
