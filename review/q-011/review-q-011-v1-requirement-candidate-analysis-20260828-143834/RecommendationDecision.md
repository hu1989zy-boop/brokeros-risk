# Q-011 Recommendation Decision

## Recommended Requirement

**Q-011 — Evidence Provenance Foundation (manually authored Evidence only)**

## Why It Is Preferred

1. It is the only remaining Q-008 provider candidate whose blocking
   prerequisite is already resolved by completed work: Q-009 supplies a
   trusted actor to author Evidence, and Q-010 supplies a trusted subject
   (`TradingAccountRef`) to scope it to.
2. It follows the approved upstream-to-downstream dependency direction:
   Decision cannot exist without it (ADR-009), so it is the mandatory next
   unlock regardless of which downstream capability is prioritized after it.
3. Scoping the first increment to manually authored Evidence avoids
   inventing Trading Data ingestion, a Rule Engine, or vendor adapters —
   none of which exist and none of which this Requirement should create.
4. It reuses Q-009's `Capability`/`AuthorizationGuard` pattern and Q-010's
   proven immutable-reference/append-only-history/same-transaction-audit
   pattern instead of inventing new conventions.
5. It is independently testable: creation, provenance capture, correction
   (supersede/invalidate/withdraw per Q-008 Requirement §11), and fail-closed
   authorization all have clear, boundable outcomes without touching
   external systems.

## Why the Other Candidates Wait

- **Decision Foundation waits** because ADR-009 requires every Decision to
  be attributable to authoritative Evidence, which does not exist until this
  Requirement is implemented.
- **Action Foundation waits** because Action originates from Decision, which
  itself waits on Evidence — a two-deep dependency.
- **ActionOutcome Foundation waits** because it depends on Action, which
  depends on Decision, which depends on Evidence — a three-deep dependency —
  and additionally requires a real MT4/MT5/CRM execution/adapter boundary
  that AGENTS.md prohibits inventing without the real SDK.
- **Audit Foundation waits** because Q-008's own approved Design already
  owns its audit table in its own transaction; no concrete cross-capability
  consumer currently makes a shared Audit platform necessary.

## Important Boundary

Q-011 is a proposal, not approval, and covers only Evidence. Even after Q-011
is eventually implemented, Q-008 will still lack Decision, Action, and
ActionOutcome authorities and will still require a separate explicit
implementation-authorization decision before any Risk Case code is written.
Completing Q-011 alone does not authorize Q-008 implementation, exactly as
completing Q-010 alone did not.

## Recommended Scope Guardrail for the Full Requirement Draft

If this candidate is confirmed, the formal Q-011 Requirement should make the
manual-only source boundary an explicit, named non-goal exclusion (mirroring
how Q-008 named `MANUAL` vs `DECISION_DRIVEN` as the only two intake
sources) — not an implicit assumption. Automated Evidence sources (Rule
Engine, trading-data anomaly detection, external alerts) should be recorded
as an explicit future-Requirement extension point, the same way Q-008
recorded `TRADING_ACCOUNT` as the only approved initial subject type.
