# Q-014 Independent Implementation Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§8–§12 and `docs/engineering/Architecture-and-Design-Decision-Principles.md`.

## Task ID

Q-014 — Action Outcome Provenance Foundation.

## Stage

Independent Review of Codex's implementation
(`review/q-014/review-q-014-v3-implementation-20260902-013321.zip`), plus
a §16.5-A cross-module test-maintenance fix applied and verified by Claude
Code.

## Scope Reviewed

The full Q-014 implementation diff (`com.brokeros.risk.actionoutcome`
module, `V7__create_action_outcome_provenance_foundation.sql`,
`ResultCode.java`, `scripts/verify-static.sh`,
`docs/skills/development-standards.md`), Codex's package claims, and the
three failing Q-013 tests — checked against actual source and
independently executed tests (protocol §8: tests are evidence, not
authority).

## Files Inspected (direct reading)

- `ActionOutcomeRecordingService.java` — full read. §11.1 order exact:
  authorize → `requireHuman` → fingerprint from raw fields → replay check
  (before content validation and the Q-013 call) → content validation
  (parse `OutcomeText`, parse single `ActionRef`) → Q-013
  `confirmProvenance` (rejects only `NOT_FOUND`) → build context → record.
  Correct.
- `V7__create_action_outcome_provenance_foundation.sql` — full read. Three
  tables; **no status column** (correctly follows Decision's shape, not
  Action's); no join table; no `*_history` table; `action_ref` is a
  regex-checked `CHAR(40)` with **no cross-module FK** and **no uniqueness
  constraint** (many-to-one, per Requirement §5.3(3)); only intra-module
  FKs `ON DELETE RESTRICT`; `source IN ('MANUAL')`; byte-bound
  `outcome_text`. Correct.
- Codex's `ActionOutcomeProvenanceView`/controller (per package + spot
  check) — narrow view has no `outcomeText`; two routes only. Correct.

**No defect found in Q-014's own implementation.** Codex's own 42 Q-014
tests pass, and the code matches Implementation Design V1 exactly.

## Verification Executed

Independently re-ran the full Q-009…Q-014 real-MySQL gate in a disposable
Docker / Java 21 / MySQL 8.4 environment.

- Before my fix (reproducing Codex's report): 246 tests, **3 failures**,
  0 errors — `Q013MySqlMigrationTests.migrationUpgradesV5…`,
  `Q013MySqlMigrationTests.metadataContainsEvery…`,
  `ActionRestContractTests.resultCodesExposeExactlyTheApprovedActionHttpContract`.
- After my fix: **246 tests, 0 failures, 0 errors, 0 skipped. BUILD
  SUCCESS.**

Docker resources removed after the run.

## Findings

### 1. Q-014 implementation: no defect.

Confirmed by code review and by its 42 own tests.

### 2. The 3 failures were a Q-013 test-scoping defect — a NEW second class of cross-module test-ownership fragility.

All three failing Q-013 tests used **over-broad prefix ownership** that
wrongly assumed the `action` prefix is exclusively Q-013's:

- `Q013MySqlMigrationTests.actionTables()` and `foreignKeys()` filtered on
  `table_name LIKE 'action_%'`, which also matches Q-014's legitimate
  `action_outcome_record`/`action_outcome_operation`/`action_outcome_access_log`
  tables and their FKs.
- `ActionRestContractTests` filtered result codes on
  `startsWith("ACTION_")`, which also matches Q-014's `ACTION_OUTCOME_*`
  codes.

Both Q-013 and Q-014 are individually correct; the collision is purely
that `action_outcome` legitimately nests under the `action` prefix. This
is the same *class* of latent-assumption bug as the hard-coded
migration-count one (a test hard-coding an assumption that becomes false
when an approved sibling module is added), but a different assumption
(prefix exclusivity).

### 3. Fix applied under Decision Authority §16.5-A (pure test-maintenance, zero business impact, cross-module).

Three assertions in two Q-013 test files, made precise (coverage
unchanged — Q-013 still asserts it owns exactly its own objects; it just
stops over-claiming Q-014's):

- `Q013MySqlMigrationTests.java` `actionTables()` and `foreignKeys()`:
  added `AND table_name NOT LIKE 'action_outcome_%'`.
- `ActionRestContractTests.java`: result-code filter now
  `startsWith("ACTION_") && !startsWith("ACTION_OUTCOME_")`.

`git diff --stat`: 2 files, 4 insertions / 1 deletion. Only `src/test/**`;
no production code, no migration, no other module touched.

### 4. Repository-wide sweep (the systemic picture).

Scanned every test for the broad-prefix-ownership pattern:

| Location | Pattern | Currently colliding? |
| --- | --- | --- |
| `Q013MySqlMigrationTests` | `LIKE 'action_%'` | YES (with `action_outcome_`) — fixed |
| `ActionRestContractTests` | `startsWith("ACTION_")` | YES (with `ACTION_OUTCOME_`) — fixed |
| `Q011MySqlMigrationTests` | `LIKE 'evidence_%'` | No — no sibling nests under `evidence_` yet |
| `Q012MySqlMigrationTests` | `LIKE 'decision_%'` | No — no sibling nests under `decision_` yet |
| `DecisionRestContractTests` | `startsWith("DECISION_")` | No — no `DECISION_X_` sibling yet |
| `ActionOutcomeRestContractTests` (Q-014's own) | `startsWith("ACTION_OUTCOME_")` | No — correctly scoped to the leaf |

The `evidence_%`/`decision_%`/`DECISION_` ones are **latent, not
currently stale** (no sibling nests under them today). Per the
migration-count precedent — do not speculatively churn passing tests that
a naive sweep could break — they were left unchanged.

## Remaining Risks

- This over-broad-prefix-ownership pattern will recur whenever a future
  module nests under an existing module's prefix (e.g. a hypothetical
  `evidence_x` or `decision_x`). See the Recommendation.

## Out-of-Scope Issues

None beyond Q-014's own Non-Goals (no execution, adapter, result taxonomy,
Q-008 code).

## Recommendation

1. **Accept Q-014's implementation.** Defect-free by code review and by
   independently executed tests; the full Q-009…Q-014 gate is green after
   the §16.5-A test fix.
2. **Adopt a convention (systemic):** cross-module "exactly these"
   ownership assertions (tables, FKs, result codes) must scope to the
   module's **exact own object names**, or explicitly exclude sibling
   namespaces — never a bare shared prefix (`LIKE 'x_%'` / `startsWith
   ("X_")`). This is the second distinct class of cross-module
   test-ownership fragility (after hard-coded migration counts); a
   convention prevents a third. Whether to proactively harden the latent
   `evidence_%`/`decision_%`/`DECISION_` assertions now, or catch them
   when a sibling actually appears, is a Product Owner call — I recommend
   catching them when relevant (avoid churning passing tests), backed by
   the convention for all new tests.

## Gate Decision

**PASS** — Q-014's implementation is defect-free and the full Q-009…Q-014
gate is independently green. This is my independent-review verdict, not a
Product Owner acceptance; acceptance and any commit remain the Product
Owner's decisions. The §16.5-A test fix is in the working tree and is not
committed until the Product Owner triggers a commit gate.
