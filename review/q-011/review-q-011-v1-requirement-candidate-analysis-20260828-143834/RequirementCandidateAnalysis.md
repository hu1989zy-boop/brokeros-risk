# Q-011 Requirement Candidate Analysis

## Decision Method

Candidates were ranked by dependency direction, smallest coherent authority,
reuse of Q-001–Q-010, absence of speculative infrastructure, clear acceptance
criteria, and progress toward Q-008/Core Domain capability without violating
ADR-009 or ADR-010. This repeats the method used in the Q-010 Requirement
Candidate Analysis (`review/q-010/review-q-010-v1-requirement-candidate-analysis-20260826-154640/RequirementCandidateAnalysis.md`)
with the repository state as it stands after Q-009 and Q-010 completion.

## Candidate 1 — Evidence Provenance Foundation

- **Problem solved:** no authoritative immutable Evidence record/provider.
  Q-008 Implementation Design §14 requires a real `EvidenceReferenceQuery`
  provider; none exists.
- **Why now (changed from the Q-010 V1 analysis):** the Q-010 V1 analysis
  deferred this candidate "until account/source identity and a concrete
  initial evidence source can be defined without a generic evidence bucket."
  Account/source identity is now resolved: Q-010 supplies a trusted,
  broker-neutral `TradingAccountRef` that Evidence can scope to. The
  remaining condition — a concrete initial evidence source — is resolvable
  now by scoping the Foundation to **manually authored, human-observed
  Evidence only**: an authenticated `HUMAN` actor (Q-009) records a
  traceable observation about a recognized `TRADING_ACCOUNT` (Q-010).
  Automated sources (trading-data anomaly feeds, Rule Engine hits, external
  alerts) all require infrastructure that does not exist yet (trading-data
  ingestion, Rule Engine, adapters) and are explicitly deferred, mirroring
  how Q-008 itself separated `MANUAL` from `DECISION_DRIVEN` intake instead
  of requiring both on day one.
- **Prerequisites:** Q-009 trusted ActorContext/Capability (implemented),
  Q-010 TradingAccountRef eligibility (implemented) as an optional
  subject-scoping reference. No additional missing provider is required for
  the manual-only scope.
- **Q-001–Q-010 dependencies:** ADR-009 Core Domain ownership rules, Q-009
  `AuthorizationGuard`/`Capability` pattern, Q-010
  `TradingAccountReferenceEligibilityService` pattern (reused as a design
  precedent, not a hard dependency — Evidence may exist for a subject
  without requiring case-eligibility), MySQL/Flyway, append-only
  history/audit conventions already proven by Q-009/Q-010.
- **Architecture impact:** new upstream/Core-Domain-adjacent Evidence
  boundary; narrow write (record) and narrow read
  (`EvidenceReferenceQuery`) contracts; immutable provenance with
  correction-by-supersession rather than overwrite.
- **ADR analysis:** likely YES — durable Core Domain boundary, new business
  identity, immutability/correction policy, and cross-capability consumer
  contract (Q-008) all meet the repository's ADR threshold.
- **Database impact:** likely additive immutable evidence + provenance
  schema, append-only correction/supersession history. Q-008 Requirement
  §11 already anticipates `SUPERSEDED` / `INVALIDATED` / `WITHDRAWN`
  disposition on the Risk Case side; Evidence Foundation must supply the
  matching upstream record those dispositions point at.
- **Security/audit:** sensitive evidence content access control, trusted
  creator attribution via ActorRef, no vendor payload or CRM/MT4/MT5 schema
  because the initial scope is manual-only.
- **Expected size:** Medium.
- **Risks:** scope creep into a generic "evidence bucket" if automated
  sources are not explicitly excluded from this Foundation; conflating a
  manual observation with a formal Decision; treating Evidence as free-form
  document storage rather than a structured, provenance-bearing record.
- **Enables afterward:** Decision Foundation (has a real Evidence authority
  to derive from), later Rule Engine as an additional Evidence source
  without redesigning Evidence ownership, and Q-008 `DECISION_DRIVEN` intake
  once Decision also exists.

## Candidate 2 — Explainable Decision Record Foundation

- **Problem solved:** Decision is the approved Core Domain but has no
  runtime record/provider.
- **Why it still waits:** ADR-009 requires every Decision to be
  attributable to authoritative Evidence. Evidence has no provider yet
  (Candidate 1). This is an unchanged hard sequential dependency from the
  Q-010 V1 analysis, not a resolvable-now condition.
- **Prerequisites:** Evidence Foundation must exist first.

## Candidate 3 — Action Intent Foundation

- **Problem solved:** Action is business response intent originating from a
  Decision; no runtime record/provider exists.
- **Why it still waits:** Action originates from Decision (ADR-009), which
  itself originates from Evidence. This is a two-deep unresolved
  dependency chain.

## Candidate 4 — ActionOutcome Foundation

- **Problem solved:** the outcome/attempt record for an executed Action; no
  runtime record/provider exists.
- **Why it still waits:** three-deep dependency (Action → Decision →
  Evidence), and it additionally requires a real execution/adapter boundary
  (MT4/MT5 Manager API or equivalent). AGENTS.md prohibits inventing
  Manager API interfaces without the real SDK, so this candidate cannot
  even be scoped narrowly the way Evidence can. It remains the furthest-out
  candidate.

## Candidate 5 — Audit Foundation (carried from the Q-010 V1 analysis)

- **Re-check relevance:** the Q-010 V1 analysis listed a shared Audit
  Foundation as Candidate 4. Since then, Q-008's approved Implementation
  Design V4 (§8.2, §9) already scopes an Audit table that Q-008 owns
  directly inside its own transaction, not an external prerequisite Q-008
  is blocked on. Q-009 and Q-010 have independently proven the same
  same-transaction history/audit pattern without a shared Audit module.
  A cross-capability Audit query/retention platform may still be valuable
  later, but it is **not on Q-008's critical path** the way Evidence,
  Decision, Action, and ActionOutcome are. Deprioritized, not selected.

## Selection

Candidate 1 (Evidence Provenance Foundation), scoped to manually authored
Evidence only, is selected. It is the only candidate whose blocking
prerequisite is now resolved by completed work (Q-009 and Q-010), and it is
the required unlock for Decision, which is in turn required for Action and
ActionOutcome. Selecting Decision, Action, or ActionOutcome now would violate
ADR-009's Evidence-attributability rule or require inventing a vendor
execution boundary without a real SDK.
