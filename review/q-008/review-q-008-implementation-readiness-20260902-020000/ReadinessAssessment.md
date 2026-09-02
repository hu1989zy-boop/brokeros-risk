# Q-008 Risk Case — Implementation Readiness Assessment

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§4 (reload authoritative context) and §18 (design-review thinking), and
`docs/engineering/Architecture-and-Design-Decision-Principles.md`.

## Task ID / Stage

Q-008 — Risk Case Foundation. Stage: **Implementation Readiness Review**
(pre-implementation). Q-008 is NOT a fresh drafting task — its Requirement,
Architecture, ADR-010, and Implementation Design V4 were approved earlier
(2026-08-25/28). Its Implementation Gate (§26) recorded `Implementation
Allowed: NO` blocked **solely** by two prerequisites. This assessment
checks whether those prerequisites are now satisfied and whether the
approved design still fits the providers that actually shipped.

## The two recorded blockers (Requirement §26 / Design §13–§14)

1. Real authoritative Trading Account / Evidence / Decision / Action /
   ActionOutcome reference providers.
2. An authenticated Actor / authorization provider.

## Blocker 1 — reference providers: SATISFIED

Q-008 Design §14 defines five abstract read-only reference-provider ports.
Each now maps cleanly onto a shipped, in-process, narrow-contract service
(verified against actual committed source):

| Q-008 abstract port (Design §14) | Shipped concrete provider (verified) | Returns |
| --- | --- | --- |
| `TradingAccountReferenceQuery` (subject recognized, no CRM/MT schema) | Q-010 `TradingAccountReferenceEligibilityService.validateForNewRiskCaseAssociation(ActorContext, TradingAccountRef)` | `TradingAccountReferenceEligibility` (tri-state) |
| `EvidenceReferenceQuery` (Evidence exists, safe provenance only) | Q-011 `EvidenceProvenanceQueryService.confirmProvenance(ActorContext, EvidenceRef)` | `EvidenceProvenanceView` (no observation text) |
| `DecisionReferenceQuery` (Decision exists, attributable to Evidence) | Q-012 `DecisionProvenanceQueryService.confirmProvenance(ActorContext, DecisionRef)` | `DecisionProvenanceView` (includes evidenceRefs) |
| `ActionReferenceQuery` (Action exists + its originating DecisionRef) | Q-013 `ActionProvenanceQueryService.confirmProvenance(ActorContext, ActionRef)` | `ActionProvenanceView` (includes decisionRef) |
| `ActionOutcomeReferenceQuery` (outcome exists, no vendor-execution interpretation) | Q-014 `ActionOutcomeProvenanceQueryService.confirmProvenance(ActorContext, ActionOutcomeRef)` | `ActionOutcomeProvenanceView` (no outcome text) |

The fit is not coincidental: each provider's narrow `confirmProvenance`
contract was deliberately built "for future Q-008 consumption," and
Q-010's method is literally named `validateForNewRiskCaseAssociation`.
Every Q-008 port need is met by the corresponding shipped view:
- `ActionReferenceQuery` needs "originating DecisionRef" → `ActionProvenanceView`
  carries `decisionRef`. ✔
- `DecisionReferenceQuery` needs "attributable to Evidence" →
  `DecisionProvenanceView` carries the `evidenceRefs` set. ✔
- `ActionOutcomeReferenceQuery` needs "no vendor-execution interpretation"
  → Q-014 is by design a human-recorded fact, not an execution record. ✔

## Blocker 2 — authenticated Actor / authorization provider: SATISFIED

Q-009 provides the real `ActorContext` and `AuthorizationGuard`
(default-deny, capability-based). Q-008's design already assumes an
`ActorContext` + authorization decision provider; Q-009 supplies exactly
that. The four provenance services are also called with the caller's own
`ActorContext`, matching Q-008's "never accept an unchecked string as
proof" rule (Design §14).

## Drift assessment

- **No contract drift.** Q-008's ports are read-only "confirm this
  reference is recognized" contracts; the shipped services are exactly
  that shape. No provider needs changing, and Q-008 needs nothing a
  shipped view does not provide.
- **Naming/binding gap only.** Q-008 named its ports `*ReferenceQuery`;
  the shipped services are `*ProvenanceQueryService.confirmProvenance` /
  the Q-010 eligibility service. Q-008's implementation defines its own
  port interfaces and provides thin adapters delegating to the concrete
  services — standard hexagonal wiring, which the design already
  anticipated (ports defined as read-only adapters). This is an
  implementation-wiring detail, not a design change.
- **ActionOutcome consistency.** Q-008 §13 treats the outcome as a
  referenced fact Risk Case does not own; Q-014 shipped ActionOutcome as
  exactly a human-recorded fact (not execution). Consistent.

## Scale note (not a blocker, flagged for awareness)

Q-008 is substantially larger than any provenance foundation: it is the
aggregate root with lifecycle states, resolution cycles, assignment,
comments, case numbers, priority, bounded history/timeline, and
associations to Evidence/Decision/Action/ActionOutcome. This is a big
single implementation. The approved design (V4) is coherent and I do not
recommend re-scoping it, but the Product Owner should be aware this is a
larger effort than Q-009…Q-014.

## Recommendation

1. **Both §26 blockers are satisfied.** Q-008 is genuinely ready to move
   from "Implementation Allowed: NO" toward implementation.
2. **One design artifact is warranted before implementation:** a concrete
   **Provider Binding Addendum (Implementation Design V4 → V5)** that
   records the exact port→service mapping above as the single
   authoritative binding, so Codex implements against an unambiguous,
   approved mapping and never guesses. This is a small, HOW-level addendum
   Claude Code can draft (Decision Authority §16.1/§16.5-B).
3. **Then implementation authorization** — the Product Owner's gate
   (spending real, larger-than-usual engineering effort).

## Design Soundness Review (§18) — done because the Product Owner delegated diligence

The Product Owner stated they trust the V4 design and will not review it
themselves. That makes Claude Code the sole reviewer, which raises rather
than lowers the diligence bar. I therefore read the correctness-critical
sections in full (§4 aggregate boundary, §5 lifecycle, §7 resolution/cycle
immutability, §8.3 reference integrity, §9 transaction/audit, §12 security,
§13 concurrency) and applied the §18 design-review checklist:

- **Failure paths (§9.3):** thorough and fail-closed — invariant failure
  writes nothing; zero-row optimistic update rolls back and returns
  conflict; case-history or Audit write failure rolls back the root; no
  caught exception continues a rollback-only transaction. ✔
- **Idempotency/duplicates (§9.4, §13):** create uses a durable
  idempotency key + request-hash on the root; decision-driven creation is
  guarded by a global unique `decision_ref`; each cycle's resolution is
  guarded by unique `(case_id, cycle_no)`. ✔
- **Concurrency (§13):** a single optimistic-version compare-and-set
  (`WHERE id=? AND version=?`) is the sole serialization point per case —
  exactly one command commits to N+1, no last-write-wins. ✔
- **Audit completeness:** every material mutation appends one Audit Record
  in the same transaction; reads append `RISK_CASE_VIEWED` before
  disclosure and fail closed if that write fails. ✔
- **Immutable history / resolution cycles (§7):** resolution records are
  immutable, one per cycle; reopen increments the cycle and clears the
  current Decision pointer while preserving all prior facts; ordering is
  deterministic by `(case_version, event-type rank, id)`, never wall-clock.
  ✔
- **Over-design:** none — Kafka/Saga/Event-Sourcing/2PC are explicitly
  rejected (§9.3); the richness is appropriate for the case-management
  aggregate root. ✔

**Relational-invariant / provider-field fit (the key drift check):** Q-008's
invariants require cross-checking relationships that only the shipped
provenance views make checkable —
- `associateAction` requires the Action "originates from an associated
  Decision": `ActionProvenanceView` returns the Action's originating
  `decisionRef`, so Q-008 can verify it is one of the case's associated
  Decisions. ✔
- `recordActionOutcomeReference` requires the outcome pertain to an
  associated Action: `ActionOutcomeProvenanceView` returns the pertaining
  `actionRef`. ✔

The narrow contracts I built for Q-011…Q-014 carry exactly the linking
fields Q-008's relational invariants need. **No design defect found; no
contract drift found.** The design is sound and ready to implement.

## Gate Decision

**PASS (readiness)** — prerequisites satisfied, approved V4 design
independently reviewed and found sound, approved design fits the shipped
providers (including the relational-invariant linking fields), one binding
addendum recommended before implementation. This is a readiness
assessment, not an implementation authorization, which remains the Product
Owner's decision.
