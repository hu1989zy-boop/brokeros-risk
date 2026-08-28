# Q-010 Requirement Candidate Analysis

## Decision Method

Candidates were ranked by dependency direction, smallest coherent authority,
reuse of Q-001–Q-009, absence of speculative infrastructure, clear acceptance
criteria, and progress toward Q-008/Core Domain capability without violating
ADR-009 or ADR-010.

## Candidate 1 — Trading Account Reference Authority Foundation

- **Problem solved:** no trusted broker-neutral authority can recognize the
  typed `TRADING_ACCOUNT` subject required by Q-008.
- **Why now:** Q-009 now supplies trusted actors/authorization; this provider
  is independent of missing Evidence/Decision/Action providers.
- **Prerequisites:** Q-001–Q-006 foundations, Q-007 ownership baseline, Q-009
  security; a real controlled initial registration/source decision.
- **Q-001–Q-009 dependencies:** reuses MySQL/Flyway, standards, verification,
  correlation, configuration, ADR-002 isolation, ADR-009, and Q-009.
- **Architecture impact:** new upstream supporting capability and narrow
  read-only published query; no new deployable.
- **ADR analysis:** likely YES for business identity, authority ownership,
  mapping, and external-system boundary.
- **Database impact:** likely additive application-owned registry, mapping, and
  history tables after approval; no external DB access.
- **Security/audit:** Q-009 exact capabilities, no existence disclosure,
  durable actor/source/before/after/reason history.
- **Expected size:** Medium.
- **Risks:** becoming account master data; unsafe account-number uniqueness;
  missing broker/tenant/source namespace; fake registration authority.
- **Enables afterward:** safe Q-008 primary-subject validation, subject-scoped
  Evidence, later Decision context, and multi-broker adapter mapping.

## Candidate 2 — Risk Evidence Provenance Foundation

- **Problem solved:** no authoritative immutable Evidence record/provider.
- **Why now:** Evidence is the explainability basis and an explicit Q-008
  provider prerequisite.
- **Prerequisites:** source taxonomy/provenance, stable subject identity where
  Evidence is account-scoped, Q-009 security.
- **Q-001–Q-009 dependencies:** ADR-009 Core Domain rules, MySQL/Flyway,
  auditability, correlation, Q-009.
- **Architecture impact:** new upstream/Core Domain-owned evidence boundary,
  source adapters, immutable provenance and query contract.
- **ADR analysis:** likely YES because Evidence ownership/provenance is a
  durable Core Domain boundary.
- **Database impact:** likely additive immutable evidence/provenance schema.
- **Security/audit:** sensitive Evidence access, trusted creator/mechanism,
  correction/supersession history, source disclosure controls.
- **Expected size:** Medium–Large.
- **Risks:** inventing generic evidence without an actual observation/source;
  mixing documents, raw trading data, alerts, and decisions; vendor coupling.
- **Enables afterward:** explainable Decision foundation, decision-driven case
  intake, later Rule Engine input.

## Candidate 3 — Explainable Decision Record Foundation

- **Problem solved:** Decision is the approved Core Domain but has no runtime
  record/provider.
- **Why now:** it is the highest-value domain boundary and a Q-008 prerequisite.
- **Prerequisites:** authoritative Evidence/provenance and stable subject
  context; decision semantics/rationale/versioning.
- **Q-001–Q-009 dependencies:** ADR-009, Q-009 authorization, existing
  persistence/verification foundations.
- **Architecture impact:** first executable Core Domain capability; rule/manual
  decision-source boundary; Decision query contract.
- **ADR analysis:** YES.
- **Database impact:** additive Decision/Evidence-reference/history schema.
- **Security/audit:** protected decision creation/read, actor or mechanism
  provenance, immutable rationale and evidence linkage.
- **Expected size:** Large.
- **Risks:** bypassing missing Evidence authority; prematurely designing Rule
  Engine, scoring/confidence, or Actions; oversized Core Domain service.
- **Enables afterward:** Action intent, decision-driven Risk Case, and later
  versioned Rule Engine.

## Candidate 4 — Audit Record Foundation

- **Problem solved:** standards and Q-008 require durable critical-change
  attribution, but no shared Audit persistence/query capability exists.
- **Why now:** Q-009 supplies trusted actor inputs and Q-008 has explicit
  same-transaction audit needs.
- **Prerequisites:** concrete initial event/mutation consumers, ownership,
  sensitivity, retention, access, and same-transaction contract.
- **Q-001–Q-009 dependencies:** Phase 0.6 audit standards, Q-008/ADR-010,
  Q-009 ActorContext and authorization provenance.
- **Architecture impact:** cross-capability Audit boundary and query policy;
  must remain inside the modular monolith and avoid a generic event platform.
- **ADR analysis:** likely YES.
- **Database impact:** additive append-only audit schema and indexes after
  retention/query decisions.
- **Security/audit:** highly sensitive history, separate read capability,
  anti-tamper/retention/redaction policy, no credential/payload leakage.
- **Expected size:** Medium–Large.
- **Risks:** designing a universal audit platform before real consumers;
  unbounded payloads/indexes; conflicting retention/regulatory assumptions.
- **Enables afterward:** consistent case/security audit query and additional
  critical business mutations.

## Selection

Candidate 1 is selected. It is the only candidate that can establish a real
Q-008 prerequisite without first requiring another missing business provider.
It remains useful independently as the identity anchor for future Evidence and
Decision while retaining a narrow, testable scope.
