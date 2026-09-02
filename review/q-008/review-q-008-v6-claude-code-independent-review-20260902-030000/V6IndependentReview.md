# Q-008 Risk Case Foundation — Independent Implementation Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§8–§12 and `docs/engineering/Architecture-and-Design-Decision-Principles.md`.
Performed autonomously while the Product Owner was away, so the verdict is
ready on their return.

## Task ID / Stage

Q-008 — Risk Case Foundation. **Independent Review** of Codex's
implementation (`review/q-008/review-q-008-v5-implementation-20260902-024550.zip`),
which was produced autonomously under the Product Owner's directive to
complete in one run without check-ins.

## Verification Executed (independent, not report-only)

Independently re-ran the full repository-wide real-MySQL gate in a
disposable Docker / Java 21 / MySQL 8.4 environment (fresh container/
network), all seven datasource families (`Q009`…`Q014` + `Q008`) enabled:

```
Tests run: 300, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Exactly matches Codex's reported 300/0/0, now reproduced in a second
environment. All 54 Q-008 test classes passed (`RiskCaseDomainTests` 15,
`RiskCaseApplicationTests` 7, `Q008MySqlMigrationTests` 7,
`Q008MySqlPersistenceTests` 12, `RiskCaseReferenceAdapterTests` 3,
`RiskCaseArchitectureTests` 5, `RiskCaseRestContractTests` 4,
`RiskCaseMetricsTests` 1). Docker resources removed after the run.

## Files Inspected (direct code reading, not report-only)

Q-008 is 98 main Java files across `riskcase` + the minimal `audit`
module — far larger than the provenance foundations. I focused the code
review on the correctness-critical elements per §18:

- **`RiskCase` aggregate root** — `status`/`version` are private; there is
  **no public `setStatus`/setter**; every change is a named operation
  returning an immutable record; `version = Math.addExact(version, 1)`
  (overflow-guarded) and `expectedVersion != version → VERSION_CONFLICT`.
  Matches Design §4/§5. ✔
- **`TradingAccountReferenceAdapter`** — implements the **STRICT** subject
  bar exactly: `NOT_RECOGNIZED → RISK_CASE_REFERENCE_NOT_FOUND`,
  `RECOGNIZED_NOT_ELIGIBLE → RISK_CASE_SUBJECT_NOT_ELIGIBLE` (the new V5
  code), `ELIGIBLE_FOR_NEW_ASSOCIATION` passes, unavailable →
  `RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE` (fail-closed). Matches the V5
  addendum §3 decision. ✔
- **`ActionReferenceAdapter`** — returns `RecognizedAction(actionRef,
  decisionRef)`, exposing the originating `decisionRef`; and
  `RiskCaseAssociationService.associateAction` checks that originating
  Decision is associated to the case, else `RISK_CASE_INVARIANT_VIOLATION`.
  This is the relational invariant from Design §5.1, made checkable by the
  narrow contract's linking field. ✔ (Same shape confirmed for the
  action-outcome→action relation.)
- **`V8__create_risk_case_foundation.sql`** — 13 additive tables;
  **no cross-module foreign key** (grep for any FK not referencing a
  `risk_case` table is empty); root `version BIGINT`; create idempotency
  `UNIQUE (created_by_ref, creation_idempotency_key_hash)` + request-hash;
  `uq_risk_case_decision_ref UNIQUE (decision_ref)` (at most one primary
  case per Decision); `uq_risk_case_resolution_cycle UNIQUE (case_id,
  cycle_no)` (one resolution per cycle). Matches Design §8/§13. ✔
- **Transaction/CAS/audit** — `JdbcRiskCaseRepository.updateRoot` uses
  `UPDATE ... WHERE id=? AND version=?` and the command service requires
  exactly one updated row; history + one Audit Record commit in the same
  transaction; the query service appends `RISK_CASE_VIEWED` before
  returning content. Matches Design §9. ✔
- **Deterministic history ordering** — the pagination cursor is
  `(case_version, event_rank, row_id)`, never wall-clock. Matches
  Design §7.4. ✔
- **Test-ownership discipline (this session's two lessons, required in the
  prompt):** `Q008MySqlMigrationTests` uses a fixed-baseline `target("7")`
  + a **dynamic** `flyway.info().pending().length` for the post-baseline
  count (not hard-coded); no broad-prefix ownership assertion
  (`LIKE 'risk_case_%'` / `startsWith("RISK_CASE_")`) exists. Both lessons
  honored from the start. ✔

## Change scope

Confirmed via `git diff --stat`: the only modified existing files are
`ResultCode.java` (+18, the nine Q-008 codes incl.
`RISK_CASE_SUBJECT_NOT_ELIGIBLE`), `scripts/verify-static.sh` (Q-008 static
checks), `docs/skills/development-standards.md` (the reusable aggregate/
history/audit-atomicity rule), plus the two governance-doc header fixes I
made (below). All new code is the untracked `riskcase` + `audit` modules,
V8, tests, the lesson, and this review. No Q-009…Q-014 source/test file and
no existing migration (V1–V7) was modified.

## Codex's documented conditions/assumptions — assessed

1. **Assignee active-state lookup (assumption).** The design requires
   authenticated assignee references, but Q-009 provides no
   actor-directory lookup to confirm an arbitrary assignee ref is a
   currently-active actor, and Design §12.2 explicitly defers users/roles/
   IAM. Codex validates the assignee is a canonical Q-009 `ActorRef` shape
   and preserves assigner provenance — the maximum honest validation
   available. **Confirmed the intended reading, not a defect:** it matches
   the design's own deferral. A future actor-directory provider (if ever
   required) is a separate Requirement, not something Q-008 may invent.
2. **Stale governance status mirrors (pre-existing).** The V4 design
   header and the Requirement's top Status header still said
   "Implementation Allowed: NO" from the August drafting era. This is the
   §16 status-drift class, pre-existing in Q-008's own docs; Codex
   correctly did not rewrite governance during implementation. **FIXED by
   Claude Code** during this review: both headers now defer to §26 (the
   single live-status source) and reflect AUTHORIZED/DELIVERED.
3. **Stale infrastructure verifier (pre-existing, out of scope).**
   `scripts/verify-infrastructure.sh` is still Q-004-specific — it
   hard-codes V1–V3 and a seven-table schema and binds fixed host ports —
   so it cannot validate the current V8 schema and failed on an occupied
   host port 6379. This is a **production script (not a test file)**, so it
   is outside the §16.5-A test-maintenance delegation; it is a pre-existing
   staleness unrelated to Q-008's correctness and needs a separately
   authorized maintenance fix. Flagged for the Product Owner; not fixed
   here.
4. **Flyway 8.4 warning (benign, pre-existing).** Known since Q-009; all
   migration/persistence tests passed on MySQL 8.4.11.

## Findings

**No defect found in Q-008's implementation.** The approved V4 design was
independently reviewed for soundness earlier (readiness package) and found
sound; this review confirms the implementation faithfully realizes it and
the V5 provider binding, with all correctness-critical elements verified in
code and all 300 tests passing in an independent environment. Codex's four
disclosures are legitimate governance/tooling/boundary items, not
implementation defects; the two governance-mirror items are now fixed.

## Remaining Risks / Out-of-Scope

- `scripts/verify-infrastructure.sh` staleness (condition 3) — a separate,
  small, Product-Owner-authorized maintenance task; does not affect Q-008
  correctness or the green test gate.
- Assignee active-state validation is shape-only by design; a future
  actor-directory Requirement would be needed to strengthen it.

## Recommendation

**Accept Q-008's implementation.** It completes the Evidence → Decision →
Action → ActionOutcome → **Risk Case** core-domain chain: Q-008 is the
aggregate root that associates the four provenance references under a
lifecycle, with immutable history, resolution cycles, optimistic-version
concurrency, and atomic audit. Separately, consider authorizing the small
`verify-infrastructure.sh` maintenance fix.

## Gate Decision

**PASS** — implementation defect-free, faithful to the approved design +
V5 binding, all 300 tests independently green. This is my independent
-review verdict, not Product Owner acceptance; acceptance and any commit
remain the Product Owner's decisions. Nothing was staged, committed, or
pushed.
