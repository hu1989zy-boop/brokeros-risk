# Q-008 Risk Case Foundation — Provider Binding Addendum (Implementation Design V4 → V5)

## Status

- Requirement: Q-008 — APPROVED (Requirement, Architecture, ADR-010, and
  Implementation Design V4 all previously approved, 2026-08-25/28).
- This addendum: **V5 addendum — its live status is recorded in §5 (Gate),
  authoritative if this header ever appears to disagree.** (Approved and in
  force as of the 2026-09-02 implementation authorization.)
- Prepared by: Claude Code, external Architect role, under Decision
  Authority §16.1/§16.5-B, after the five provider prerequisites shipped
  (Q-010 Trading Account, Q-011 Evidence, Q-012 Decision, Q-013 Action,
  Q-014 ActionOutcome) and the Q-009 authorization provider shipped.
- Purpose: bind Q-008 Implementation Design §14's five abstract read-only
  reference-provider ports to the concrete shipped services, as the single
  authoritative binding, so implementation targets an unambiguous mapping
  and never guesses. This addendum does **not** change any approved V4
  decision; it makes the deferred provider binding concrete.

This addendum is authoritative for the provider binding; Implementation
Design V4 remains authoritative for everything else. The Q-008 §26 / V4
§17 implementation blockers ("providers absent", "auth provider absent")
are resolved by the bindings below.

## 1. Port → concrete provider binding

Q-008 defines its own read-only port interfaces (hexagonal), each
implemented by a thin adapter in `infrastructure` that delegates to the
shipped in-process service, called with the caller's own `ActorContext`.
No shipped provider is modified; no vendor DTO enters Q-008's schema.

| Q-008 port (Design §14) | Concrete service (com.brokeros.risk.*) | Method | Returns |
| --- | --- | --- | --- |
| `TradingAccountReferenceQuery` | `tradingaccount.application.TradingAccountReferenceEligibilityService` | `validateForNewRiskCaseAssociation(ActorContext, TradingAccountRef)` | `TradingAccountReferenceEligibility` (tri-state) |
| `EvidenceReferenceQuery` | `evidence.application.EvidenceProvenanceQueryService` | `confirmProvenance(ActorContext, EvidenceRef)` | `EvidenceProvenanceView` |
| `DecisionReferenceQuery` | `decision.application.DecisionProvenanceQueryService` | `confirmProvenance(ActorContext, DecisionRef)` | `DecisionProvenanceView` (carries `evidenceRefs`) |
| `ActionReferenceQuery` | `action.application.ActionProvenanceQueryService` | `confirmProvenance(ActorContext, ActionRef)` | `ActionProvenanceView` (carries originating `decisionRef`) |
| `ActionOutcomeReferenceQuery` | `actionoutcome.application.ActionOutcomeProvenanceQueryService` | `confirmProvenance(ActorContext, ActionOutcomeRef)` | `ActionOutcomeProvenanceView` (carries pertaining `actionRef`) |
| Authenticated actor + authorization | `security.*` | `ActorContext` + `AuthorizationGuard.requireAllowed(ActorContext, Capability)` | `AuthorizationDecision` (default-deny) |

Q-008's reference value objects (`EvidenceRef`/`DecisionRef`/`ActionRef`/
`ActionOutcomeRef`/`TradingAccountSubjectRef`) reuse the shipped domain
`*Ref` types (or wrap their canonical string forms `ev-`/`dec-`/`act-`/
`aoc-`/`ta-`); Q-008 never accepts an unchecked string as proof a
reference exists — every reference is confirmed through the port above
before it is associated (Design §8.3 / §14).

## 2. Recognition mapping per port

- **Evidence / Decision / Action / ActionOutcome:** accept a `RECOGNIZED`
  provenance outcome; reject `NOT_FOUND` with the corresponding Q-008
  "reference not found" ResultCode; map a provider-unavailability
  exception to `RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE` (fail-closed,
  Design §11.4). Evidence in `SUPERSEDED` status and Decisions/Actions/
  Outcomes are all still "recognized" for association/history purposes —
  Q-008 records provenance, it does not re-judge upstream lifecycle.
- **Trading Account subject:** see §3 — one business decision is
  outstanding.

## 3. RESOLVED business decision (Product Owner, 2026-09-02) — subject eligibility bar on case creation

**DECISION: STRICT bar — confirmed by the Product Owner on 2026-09-02
("严格档").** Risk Case creation / subject association requires the Q-010
subject to be `ELIGIBLE_FOR_NEW_ASSOCIATION`; a `RECOGNIZED_NOT_ELIGIBLE`
subject is rejected with `RISK_CASE_SUBJECT_NOT_ELIGIBLE` (a case cannot be
opened on an account not eligible for a new risk-case association), and
`NOT_RECOGNIZED` is rejected as not-found. The
`ELIGIBLE_FOR_NEW_ASSOCIATION` state exists precisely to gate new
risk-case association, so it is the correct bar for Q-008's own subject.
This intentionally differs from the looser "recognized" bar the Product
Owner confirmed for Evidence (Q-011) and Decision (Q-012), because those
are records *about* a subject whereas Q-008 creates the case association
the state was designed to gate. The analysis that led here is retained
below for the record.

### Analysis (retained)

Q-008 Design §14 says the subject must be "recognized." Q-010's contract,
however, returns a tri-state whose top value —
`ELIGIBLE_FOR_NEW_ASSOCIATION` — was named specifically for the risk-case
-association use case, i.e. Q-008. The two do not unambiguously agree on
which bar applies when a Risk Case is **created** for a subject:

- **Strict (recommended):** case creation requires
  `ELIGIBLE_FOR_NEW_ASSOCIATION`; a `RECOGNIZED_NOT_ELIGIBLE` subject is
  rejected (a case cannot be opened on an account not eligible for a new
  association). Rationale: the state exists precisely to gate new risk
  -case association; using the looser bar would make that state
  meaningless for its own designed purpose.
- **Loose:** case creation accepts any recognized subject
  (`ELIGIBLE_FOR_NEW_ASSOCIATION` **or** `RECOGNIZED_NOT_ELIGIBLE`),
  matching the "recognized" bar the Product Owner confirmed for Evidence
  (Q-011) and Decision (Q-012). Rationale: consistency with the rest of
  the chain; Evidence/Decision are records *about* a subject and accept
  the looser bar.

This differs from the Q-011/Q-012 precedent because those are records
*about* a subject, whereas Q-008 creates the actual case association the
`ELIGIBLE_FOR_NEW_ASSOCIATION` state was designed to gate. This is a
risk/business policy decision (Decision Authority §16.2) and must be
confirmed by the Product Owner before implementation. **Claude Code
recommends the strict bar.**

(Note: the bar applies to *case creation / subject association*. It does
not affect the Evidence/Decision/Action/Outcome reference bindings in
§1–§2.)

### ResultCode consequence of the strict bar

The strict bar distinguishes two subject rejections that the original
Design §11.4 code set did not separate:

- `NOT_RECOGNIZED` (subject reference not recognized at all) →
  `RISK_CASE_REFERENCE_NOT_FOUND` (422), the existing §11.4 code.
- `RECOGNIZED_NOT_ELIGIBLE` (subject exists but is not eligible for a new
  risk-case association) → **one new code, `RISK_CASE_SUBJECT_NOT_ELIGIBLE`
  (422)**.

This is the only ResultCode added beyond Design §11.4's original set, and
it is added under §11.4's own stated principle ("may add only codes that
map to a real designed failure") — the strict-bar rejection is a real,
newly-distinguished designed failure. Distinguishing it from
"reference not found" is required for diagnosability (Principles §7):
an operator must not see "reference not found" for an account that plainly
exists but is ineligible. This is Claude Code's error-taxonomy decision
(Decision Authority §16.1) flowing directly from the Product Owner's
strict-bar business decision.

## 4. Relational-invariant field usage (confirmation)

Q-008's approved invariants are checkable precisely because the shipped
narrow views carry the linking fields:

- `associateAction` (Design §5.1) requires the Action originate from a
  Decision already associated to the case → the adapter reads
  `ActionProvenanceView.decisionRef` and Q-008 checks it against the
  case's associated Decisions.
- `recordActionOutcomeReference` (Design §5.1) requires the outcome
  pertain to an associated Action → the adapter reads
  `ActionOutcomeProvenanceView.actionRef` and Q-008 checks it against the
  case's associated Actions.

No additional provider capability is required.

## 5. Gate

- Provider binding: **defined and complete** (§1–§2, §4).
- §3 subject-eligibility bar: **RESOLVED — STRICT (`ELIGIBLE_FOR_NEW_ASSOCIATION`)
  — 2026-09-02 — Product Owner.**
- Q-008 V4 implementation blockers (providers + auth): **resolved by §1**.
- This binding addendum (V5): **APPROVED / in force — 2026-09-02 — Product
  Owner** (accepted together with the implementation authorization).
- Implementation: **AUTHORIZED — 2026-09-02 — Product Owner.** The
  authoritative implementation inputs are Requirement / Architecture /
  ADR-010 / Implementation Design V4 / this V5 binding addendum.
