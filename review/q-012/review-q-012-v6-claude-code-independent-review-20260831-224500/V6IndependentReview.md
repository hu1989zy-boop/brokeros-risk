# Q-012 Independent Implementation Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§8–§12.

## Task ID

Q-012 — Decision Provenance Foundation.

## Stage

Independent Review (of Codex's implementation, delivered as
`review/q-012/review-q-012-v5-implementation-20260831-214033.zip`).

## Scope Reviewed

The full Q-012 implementation diff (`com.brokeros.risk.decision` module,
`V5__create_decision_provenance_foundation.sql`, `ResultCode.java`,
`scripts/verify-static.sh`, `docs/skills/development-standards.md`) and
Codex's own review package's claims, checked against the actual source
and independently executed tests — not accepted on report authority
alone, per protocol §8 ("tests are evidence, not authority").

## Files Inspected (direct reading)

- `DecisionRecordingService.java` — full read. Traced execution line by
  line against Implementation Design §11.1's nine-step canonical order:
  authorize → `HUMAN` check → fingerprint from **raw** fields (before
  domain parsing) → `queryPort.findOperation` replay check (returns
  immediately on match, before content validation or any Q-010/Q-011
  call) → content validation (parse conclusion, de-duplicate/sort
  evidence refs via `TreeSet`, parse subject) → Q-010 validation
  (rejects only `NOT_RECOGNIZED`) → Q-011 validation per distinct
  evidence ref (rejects only `NOT_FOUND`) → build
  `AuthorizedMutationContext` → `mutationPort.record`. Exact match, no
  deviation.
- `DecisionFingerprintFactory.java` — full read. Confirmed the raw
  evidence-ref strings are canonicalized (sorted + de-duplicated via
  `TreeSet`) **before** hashing, so client-submitted order/duplicates
  never change the fingerprint — correctly matching the idempotency
  design intent.
- `V5__create_decision_provenance_foundation.sql` — full read. Every
  column, type, collation, and constraint checked against Design
  §8.1–§8.4 table by table: `decision_ref CHAR(40)` (correct length for
  the 4-character `dec-` prefix, not copied from the 3-character
  `ta-`/`ev-` precedent), `subject_ref`/`evidence_ref CHAR(39)` with no
  cross-module foreign key (matching ADR-014), exactly three intra
  -module foreign keys (`decision_evidence_reference.decision_id`,
  `decision_operation.decision_id`, `decision_access_log.decision_id`,
  all → `decision_record.id`, `ON DELETE RESTRICT`), single-value
  `source`/`operation_type`/`outcome` `CHECK`s, `OCTET_LENGTH` bound on
  `conclusion_text`, and the `decision_evidence_reference` uniqueness
  constraint. Exactly four tables, no fifth history table. Confirmed
  present and correct.
- `DecisionProvenanceView.java` — full read. Confirmed **no
  `conclusionText` field exists at the type level** (structural
  guarantee, not convention), and the compact constructor enforces
  `RECOGNIZED` requires complete metadata / `NOT_FOUND` requires none.
- `DecisionController.java` — full read. Confirmed exactly two routes
  (`POST /api/decisions`, `GET /api/decisions/{decisionRef}`), no
  `PATCH`/`PUT`, `@ConditionalOnWebApplication(type = SERVLET)` present
  (avoiding the regression class Q-011 had to fix against Q-010's
  non-Web bootstrap), actor identity taken only from
  `ActorContextProvider.currentContext()`.
- `DecisionDetailReadService.java` and
  `JdbcDecisionAccessLogAdapter.java` — full read. Confirmed the
  access-log write happens after lookup succeeds but before the record
  is returned, in a `PROPAGATION_REQUIRES_NEW`, non-read-only
  transaction — a failed write propagates and no content is returned.
- `git diff` on `ResultCode.java`, `scripts/verify-static.sh`,
  `docs/skills/development-standards.md` — confirmed additive-only,
  matching Codex's own `GitDiffStat.txt`.

## Verification Executed

Independently re-ran the full repository-wide real-MySQL gate myself, in
a disposable Docker/Java 21/MySQL 8.4 Linux environment (fresh
container/network, not reused state), with
`Q009_MYSQL_TEST_URL`/`Q010_MYSQL_TEST_URL`/`Q011_MYSQL_TEST_URL`/
`Q012_MYSQL_TEST_URL` all set:

```
Tests run: 165, Failures: 1, Errors: 0, Skipped: 0

Q011MySqlMigrationTests.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart:45
expected: 1
 but was: 2
```

This exactly matches Codex's own reported result (same test, same
numbers), now confirmed in a second, independent environment. All of
Q-012's own tests passed cleanly in this run too: `DecisionApplicationTests`
(9), `DecisionMetricsTests` (1), `Q012SecurityMySqlIntegrationTests` (6),
`Q012MySqlMigrationTests` (7), `Q012MySqlPersistenceTests` (6),
`DecisionDomainTests` (5), `DecisionArchitectureTests` (3),
`DecisionRestContractTests` (4) — 41 tests, matching Codex's "22
non-database + 19 database" claim exactly.

Docker resources (`brokeros-q012-verify-mysql` container,
`brokeros-q012-verify-net` network) were removed after this run.

## Requirement Status

Q-012 Requirement V1 — all 9 `Q012-FR-XXX` items independently confirmed
implemented as specified (see Files Inspected above). No missing
requirement, no unrequested addition found.

## Architecture Status

Architecture V1 / ADR-014 — implementation independently confirmed
compliant: no cross-module foreign key, no eligibility service, no
correction/status column, module boundaries match §3.

## Design Compliance Status

Implementation Design V1 — independently confirmed compliant, in
particular §11.1's execution order (traced directly in source, not
inferred from tests) and §8.5's constraint-to-test mapping (every row
present in the migration DDL).

## Test Status

165 tests, 1 failure, 0 errors, 0 skipped — independently reproduced,
identical to Codex's own report. Q-012's own 41 tests: 41/41 pass.

## Findings

**No defect found in Q-012's own implementation.** Both the code review
and the independently executed tests support this.

**The one failing test, `Q011MySqlMigrationTests
.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart`, is a
genuine, pre-existing, Q-012-unrelated defect — the same class of bug
already found and fixed once before in this project**, on 2026-08-31, for
`Q009MySqlIntegrationTests` (AC15 in the Q-011 governance chain). That
earlier fix replaced a hard-coded "expected 1 more migration" assertion
with a dynamically derived count (`flyway.info().pending().length`), but
was scoped narrowly to the one file failing at the time
(`Q009MySqlIntegrationTests.java`) — it did not touch
`Q011MySqlMigrationTests.java`, which has the identical hard-coded-count
pattern (`.target("3")` baseline, then asserting exactly `1` further
migration). That assertion was already stale the moment Q-012 added V5;
it is now off by exactly the number of migrations added since V3 (V4 and
V5 = 2, not 1). `git diff --stat` confirms Codex did not touch this Q-011
file, correctly honoring the Q-012 Prompt's hard boundary against
modifying any Q-011 file.

This is not a new discovery of a new bug — it is a second, entirely
predictable occurrence of a bug pattern this project already diagnosed
and already has an approved fix pattern for.

## Remaining Risks

- Every future additive migration will trigger this same class of
  failure in any test that still hard-codes a post-baseline migration
  count. A repository-wide sweep for this pattern (not just Q-011's one
  instance) may be worth considering at some point, though that is a
  separate, broader decision from fixing the one currently-failing
  instance.

## Out-of-Scope Issues

None beyond what Q-012's own Requirement/Architecture/Design already
scope out (Q-008, Action, ActionOutcome, Rule Engine, correction,
eligibility service).

## Recommendation

1. **Accept Q-012's implementation.** It is a faithful, correct,
   independently-verified realization of Implementation Design V1 — no
   defect found in code review or in independently executed tests.
2. **Separately authorize the same narrow fix pattern** already used for
   AC15, applied this time to `Q011MySqlMigrationTests.java` only:
   replace the hard-coded expected-migration-count literal with a
   dynamically derived value from Flyway's own pending-migration
   metadata, so it does not go stale with the next migration either. This
   is unrelated to Q-012's own correctness and should not block Q-012's
   closure any more than AC15 blocked Q-011's.

## Gate Decision

**BLOCKED** — matching Codex's own honest self-assessment. Q-012's
implementation itself is sound (would independently warrant PASS on its
own merits), but the mandatory all-module real-MySQL regression gate
(Design §16.7/§18, Requirement AC 11) is not green, and per protocol §10
("never weaken tests simply to obtain PASS"), that failure must be
resolved through the same narrow, separately-authorized fix used for
AC15 — not silently worked around, and not waved through as if it did
not exist. The Product Owner's decision on whether/how to authorize that
fix is the next step, not Claude Code's to make unilaterally.
