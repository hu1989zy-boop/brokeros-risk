# Q-014 Requirement V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12 and `docs/engineering/Architecture-and-Design-Decision-Principles.md`.

## Task ID

Q-014 — Action Outcome Provenance Foundation (new task; the final
Core-Domain-chain prerequisite for Q-008 Risk Case).

## Stage

Requirement. Self-review by Claude Code (external Architect role). Per
protocol §3, work stops here pending the Product Owner's Gate Decision.

## Scope Reviewed

`docs/requirements/Q-014-Action-Outcome-Provenance-Foundation.md` V1,
checked for internal consistency and against the actual approved upstream
sources it cites.

## Files Inspected

- `docs/adr/ADR-009-brokeros-risk-core-domain-model.md` (the Action/
  Execution boundary this Requirement is built to respect).
- `docs/requirements/Q-008-Requirement.md` §13 (Action Requirements — what
  Q-008 needs from an outcome reference and what it explicitly defers) and
  §26 (Implementation Gate — confirms ActionOutcome is the last of the
  three provider prerequisites).
- `docs/requirements/Q-013-Action-Provenance-Foundation.md` (structural
  precedent, and the source of the narrow provenance-read contract Q-014's
  validation depends on).
- `AGENTS.md` (the "never invent Manager API without real SDK" constraint
  that forces the human-recorded-fact scope).

## Verification Executed

Not applicable — no code exists yet. `GitStatus.txt`/`GitDiffStat.txt`
confirm the only change is the one new Requirement file.

## Requirement Status

V1, drafted, self-reviewed, **not yet approved**.

## Findings

This is the most business-scope-laden Requirement in the chain so far,
because ActionOutcome sits exactly on the Action/Execution boundary that
ADR-009 draws. Per Decision Authority §16.2, the genuine WHAT questions
are surfaced explicitly in §5.3 with firm recommendations rather than
silently assumed:

1. **Human-recorded outcome fact, not real execution** — strongly
   constrained (no SDK, Execution outside Core Domain, established MANUAL
   -only pattern); recommended firmly, but the product intent is the
   Product Owner's to confirm.
2. **Free-text outcome, no result taxonomy now** — mirrors Decision's own
   deferred outcome taxonomy (Q-008 §12); recommended, with a clean
   additive-nullable-column migration path recorded as *known* deferred
   debt (§20 of the Principles doc — no Unknown debt).
3. **Many-to-one ActionOutcome→Action, no one-per-Action constraint** —
   extensibility-first (a constraint can be added later but not cheaply
   removed once relied upon); recommended.

A deliberate modelling choice, verified against precedent: ActionOutcome
follows **Decision's** shape (immutable fact, no status column, no
correction), not Action's (which had a `PROPOSED` status column because
Action genuinely has a proposed→approved lifecycle). An outcome fact has
no lifecycle to transition, so adding a status column would be speculative
— consistent with §11 (Simplicity) and §20 (deferred debt has a migration
path). This distinction (why Q-014 has no status column while Q-013 does)
is the kind of "carried over by analogy — verify it actually applies"
check the Q-013 self-review flagged as necessary; here it was applied and
the analogy to Decision (not Action) is the correct one.

No citation error found (Q-008 §13 and §26 verified against the live file
before citing).

## Remaining Risks

- If any §5.3 answer is overridden, §7/§10 change materially — flagged in
  §16's checklist so it is not missed at the next gate.
- `aoc-`/`CHAR(40)` length stated as an Architecture-stage confirmation,
  not asserted final.

## Out-of-Scope Issues

None beyond the Non-Goals in §5.2.

## Recommendation

Present to the Product Owner for a Gate Decision, with explicit attention
to §5.3's three questions. My recommendation on all three is to confirm as
drafted; none blocks a firm recommendation, but all three are genuinely
the Product Owner's WHAT to confirm.

## Gate Decision

**PASS** (self-review only — the Product Owner's Gate Decision remains
outstanding).
