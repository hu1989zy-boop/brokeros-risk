# Q-014 Architecture + ADR-016 + Implementation Design — Bundle Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12 and `docs/engineering/Architecture-and-Design-Decision-Principles.md`
§16.5-B (connected-chain drafting after Requirement approval).

## Task ID

Q-014 — Action Outcome Provenance Foundation.

## Stage

Architecture Analysis + ADR Decision + Implementation Design, drafted as
one connected chain under §16.5-B and self-reviewed together. Presented as
a single bundle at the implementation-authorization gate, which remains the
Product Owner's.

## Scope Reviewed

- `docs/architecture/q-014-action-outcome-provenance-foundation-architecture.md` V1
- `docs/adr/ADR-016-action-outcome-provenance-foundation.md`
- `docs/architecture/q-014-action-outcome-provenance-foundation-implementation-design.md` V1

Checked for consistency with approved Requirement V1 (including its three
confirmed §5.3 answers), internal consistency across the three documents,
and against the actual committed Q-009/Q-013 code.

## Files Inspected

- Q-014 Requirement V1 (all 8 FRs cross-checked into Design §19).
- ADR-009 (Action/Execution boundary), ADR-014 (Decision — the correct
  structural analogue), ADR-015 (Action — the cross-module no-FK pattern
  and the contrast on the status column).
- Committed `V5`/`V6` migrations and `DecisionRecord`/`ActionRecord` shape
  (column types/lengths, to derive `aoc-`/`CHAR(40)` by arithmetic).
- `docs/lessons/2026-08-31-q011-migration-count-test-fix.md` (Design §16.4
  bakes in the fixed-baseline-plus-dynamic-increment test discipline so
  this module's own migration test cannot become a fourth occurrence).

## Verification Executed

Not applicable — no code exists yet. `GitStatus.txt`/`GitDiffStat.txt`
confirm the only working-tree changes are the Q-014 documents and packages.

## Requirement / Architecture / Design compliance

- Every Architecture §2 decision is traced into a concrete Design section.
- Every Requirement FR (Q014-FR-001…008) maps to a Design section (§19),
  and each cited FR number was verified to exist in the approved
  Requirement.
- ADR-016 introduces no decision beyond what Architecture V1 made; it
  records them durably and its "Alternatives Considered" each trace to a
  Requirement/Architecture decision already taken.

## Findings

The one structural judgement worth calling out (and verified, not assumed):
**ActionOutcome follows Decision's shape (immutable, no status column, no
correction, no history table), NOT Action's.** Action carries a `PROPOSED`
status column because it has a real proposed→approved lifecycle; an outcome
*fact* has no lifecycle to transition. This is exactly the "carried over by
analogy — verify it actually applies" check flagged as necessary during the
Q-013 self-review: here the analogy to Decision (not to the immediately
preceding module, Action) is the correct one, and the documents state why.

Other verified points:
- `aoc-`/`CHAR(40)` computed from the actual prefix length
  (`len("aoc-")=4 + 36 = 40`), not copied.
- Three tables, no join table (single reference, confirmed cardinality),
  no history table (immutable) — a real simplification traced to the
  requirement, not an omission.
- Many-to-one with **no** uniqueness on `action_ref` is reflected in
  §8.1 and explicitly test-covered in §16.4 (recording the same
  `action_ref` twice must succeed) — matching the confirmed §5.3(3).
- The deferred result taxonomy is recorded as known debt with an
  additive-nullable-column migration path (ADR-016, Architecture §22),
  not Unknown debt.
- Design §16.4 explicitly requires the dynamic-count test discipline, so
  Q-014's own migration test is designed from the start not to repeat the
  hard-coded-count bug class.

No inconsistency found across the three documents.

## Remaining Risks

- Exact next Flyway migration version number deliberately left to
  implementation time (Design §17/§20).
- If a future Requirement adds a result taxonomy or real execution, it
  must not retrofit ActionOutcome without its own Requirement/ADR
  (Architecture §22, ADR-016 Deferred Decisions).

## Out-of-Scope Issues

None beyond the Non-Goals already scoped out (execution, adapter, taxonomy,
Q-008 code).

## Recommendation

Present the bundle to the Product Owner at the implementation-authorization
gate. My self-review is PASS on all three documents; the Product Owner's
review + implementation authorization is the next and only remaining gate
before Codex implements.

## Gate Decision

**PASS** (self-review only — the Product Owner's bundle acceptance and
implementation authorization remain outstanding).
