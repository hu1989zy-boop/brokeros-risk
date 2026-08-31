# Q-010: Trading Account Reference Authority Foundation

## Status

**Approved — V8 Final Closure PASS / Awaiting Architect Final Review**

| Gate | Result |
| --- | --- |
| Requirement Candidate Analysis | COMPLETE — V1 |
| Requirement Review | PASS — V2 |
| Requirement Approval | APPROVED — 2026-08-26 |
| Architecture | APPROVED — V1 — 2026-08-27 |
| ADR | ADR-012 ACCEPTED — 2026-08-27 |
| Implementation Design | APPROVED — V1 — EXTERNAL ARCHITECT — 2026-08-27 |
| Implementation | APPROVED — V7 — EXTERNAL ARCHITECT — 2026-08-27 |
| Verification | PASS — V8 — 2026-08-28 |
| Final Closure | PASS / CLOSED — V8 — 2026-08-28 |
| Ready for Git Commit | YES — closure assessment; final Architect review required |
| Implementation Allowed | YES — explicit V7 authorization executed |

- Requirement ID: `Q-010`
- Requirement version: `V1 — APPROVED`
- Architect Requirement Review: `PASS / APPROVED — 2026-08-26`
- Architecture phase: Phase 1
- Capability type: upstream supporting reference authority
- Governing model: ADR-009 and the Q-007 Core Domain baseline
- Intended first consumer: approved but prerequisite-blocked Q-008 Risk Case
- Architecture submission:
  `docs/architecture/q-010-trading-account-reference-authority-architecture.md`
- Implementation Design submission:
  `docs/architecture/q-010-trading-account-reference-authority-implementation-design.md`
- Accepted ADR:
  `docs/adr/ADR-012-trading-account-reference-authority-foundation.md`

This Requirement V1 was approved through the authorized Q-010 Architect
Requirement Review on 2026-08-26. Approval covers only the Requirement boundary
and the resolutions below. The external Architect approved Q-010 Architecture
V1 and approved ADR-012 for acceptance recording on 2026-08-27; Q-010 V4
records those decisions. Q-010 V5 submitted Implementation Design V1, and the
external Architect approved that Design on 2026-08-27. Q-010 V6 records the
approval; Codex does not self-approve. The explicit V7 authorization was then
implemented, and the external Architect approved that exact V7 implementation
on 2026-08-27. V8 records the external decision and the verification-backed
Final Closure. Git commit and push remain separate and were not performed.

## 1. Background

Q-007 established the canonical BrokerOS Risk model:

```text
Evidence → Decision → Action → Risk Case
```

Trading Data remains an upstream supporting context. Q-008 then approved
`TRADING_ACCOUNT` as the only initial Risk Case subject type, but its
implementation remains blocked because no authoritative Trading Account,
Evidence, Decision, Action, or ActionOutcome reference provider exists. Q-009
has now implemented the separate trusted Actor/authorization prerequisite.

The repository contains no Trading Account module, business table, stable
BrokerOS Trading Account reference, external-account mapping authority, or
read-only provider that Q-008 or a future Evidence/Decision capability can
trust. A caller-supplied account number or opaque string is not an authority,
and a vendor/CRM database identifier cannot become the BrokerOS domain model.

## 2. Problem Statement

BrokerOS Risk cannot currently answer, through an application-owned,
broker-neutral, fail-closed mechanism:

- whether a Trading Account reference is recognized;
- which stable BrokerOS reference represents an external account identity;
- which source namespace and provenance established that mapping;
- whether the reference remains valid for a proposed new association while
  preserving historical resolvability; or
- whether registration, mapping, lifecycle, and query operations were
  authorized and attributable.

Without this boundary, future Risk Case, Evidence, Decision, or Account Control
work could trust arbitrary strings, couple to MT4/MT5/CRM schemas, confuse one
broker/server account with another, silently reuse reassigned account numbers,
or erase historical identity after an account becomes inactive.

### 2.1 Approved Requirement Gate Resolutions

The Q-010 Requirement Review resolves the following boundary decisions. Later
Architecture and Implementation Design may choose representations and
mechanisms only within these constraints.

1. **Authority scope and source namespace:** every external account identity
   is exactly the tuple `AccountAuthorityScopeRef + SourceNamespace +
   ExternalAccountKey`. `AccountAuthorityScopeRef` is an opaque BrokerOS-owned
   reference to the broker/deployment authority scope; it is not a Customer,
   Broker, Tenant, or Organization master record. `SourceNamespace` identifies
   one governed external source instance and must distinguish platform/source,
   server, and environment where those dimensions can collide. A raw login,
   account number, vendor database ID, or source name alone is never
   authoritative.
2. **Uniqueness and cardinality:** the complete tuple is unique and maps to one
   and only one `TradingAccountRef`. In the Foundation, one
   `TradingAccountRef` has exactly one authoritative external-identity tuple.
   Aliasing, merges, one-to-many mappings, cross-source migration, and
   reassignment to another `TradingAccountRef` are excluded and require a
   later approved Requirement.
3. **Lifecycle and history:** an active reference is eligible for new
   associations. A deactivated or retired reference remains resolvable for
   historical associations but is not eligible for new ones. References and
   external-identity tuples are never physically deleted, silently reused, or
   reassigned. Reactivation, if later supported, may restore only the same
   immutable mapping through an explicit authorized operation with durable
   provenance.
4. **Initial registration authority:** the Foundation uses controlled,
   application-owned, non-web provisioning as its only registration and
   lifecycle-change authority. A deployment-designated, pre-provisioned Q-009
   actor with the exact Q-010 capability submits a complete manifest backed by
   a broker/source-owner-approved record and a bounded provenance reference.
   Runtime consumers, HTTP request fields, unknown authenticated principals,
   and discovery from an external database cannot auto-register an account.
5. **Consumer boundary and disclosure:** Q-008 may use only a protected,
   read-only application contract that validates a supplied
   `TradingAccountRef` and returns recognized/eligible state plus bounded
   authority version/provenance. It receives no external account key, source
   payload, customer data, or vendor DTO. External-key resolution, if needed by
   a future source adapter, is a separate protected use case and is not exposed
   to Q-008 or as a public lookup API.
6. **Security and capabilities:** Q-010 owns the exact capabilities
   `trading-account-reference:read`,
   `trading-account-reference:register`, and
   `trading-account-reference:change-lifecycle`. Every protected read and
   mutation obtains the actor from Q-009 `ActorContext`, authorizes before data
   access, and defaults to deny. No role, actor header, source field, or account
   ownership assertion grants access.
7. **Auditability:** registration and lifecycle changes retain the trusted
   actor, UTC time, operation, immutable target, authority/source provenance,
   reason, before/after state, and version in the same application-owned
   transaction as the change. This Requirement does not create a general Audit
   module.
8. **Failure semantics:** unauthenticated, forbidden, and authority-unavailable
   outcomes follow Q-009 and disclose no account existence. An authorized
   unknown reference returns not found; an inactive reference returns
   recognized but not eligible for new use; conflict or ambiguous durable
   state returns a safe integrity conflict and never selects a winner;
   database/authority unavailability returns dependency unavailable and never
   creates, mutates, or returns a stale success.
9. **Q-008 governance record:** the pre-approval header and Section 17 in the
   Q-008 Implementation Design are intentionally retained submission-time
   history. The later immutable Q-008 Architect Approval record explicitly
   classifies them this way and is the authoritative current Design Gate.
   Q-008 needs no metadata repair and remains unimplemented and blocked by its
   remaining prerequisites.

## 3. Goals

1. Establish one stable, opaque, broker-neutral BrokerOS Trading Account
   reference distinct from database IDs and external account identifiers.
2. Establish an authoritative mapping boundary from scoped external account
   identity to the stable BrokerOS reference.
3. Provide a narrow read-only reference-resolution contract for approved
   consumers such as Q-008.
4. Preserve source provenance, lifecycle history, and historical
   resolvability without importing trading or customer data.
5. Apply Q-009 trusted ActorContext and capability authorization to protected
   registration, lifecycle, mapping, and query use cases.
6. Fail closed on unknown, ambiguous, conflicting, inactive-for-new-use, or
   unavailable authority outcomes.
7. Preserve broker, CRM, and trading-platform neutrality and Phase 1 modular-
   monolith constraints.

## 4. In Scope

- semantic ownership of `TradingAccountRef` and scoped external account keys;
- stable internal reference generation/validation requirements;
- source-namespace and account-identity mapping requirements;
- controlled registration/import/provisioning boundary and provenance;
- exact duplicate, collision, reassignment, disable/retire, and restoration
  behavior constrained by Section 2.1 and detailed at Architecture;
- read-only recognition/resolution for application-owned consumers;
- safe bounded metadata sufficient to identify provenance and reference state;
- Q-009 ActorContext, exact capability checks, default deny, and attributable
  change requirements;
- application-owned MySQL/Flyway, transaction, concurrency, and immutable
  lifecycle-history analysis for a later approved implementation;
- failure semantics for not found, conflict, ambiguity, and authority
  unavailability; and
- required ADR analysis for identity ownership, mapping, lifecycle, consumer,
  and external-system boundaries.

## 5. Non-Goals

Q-010 does not authorize or define:

- a full Trading Account master-data, customer, KYC, CRM, broker, tenant, or
  organization-management platform;
- balances, equity, margin, leverage, credit, currency conversions, orders,
  deals, positions, transactions, prices, ticks, or other trading data;
- Evidence, Decision, Rule Engine, Action, ActionOutcome, Risk Case, Account
  Control, external Execution, or Audit platform implementation;
- MT4/MT5 Manager API, CRM API/database, bridge, LP, dealer plugin, or vendor
  SDK behavior;
- direct reads from or writes to an external-system database;
- automatic discovery/synchronization, polling, streaming, CDC, Kafka topics,
  Redis keys, cache, Flink, Python, ML, or AI;
- account-control commands such as disable trading, leverage change, trade
  close, restriction, or liquidation;
- a public administration UI, search/reporting platform, bulk workflow, or
  generalized universal Entity/Subject framework;
- Q-008 implementation authorization or a reduced Q-008 implementation; or
  any production implementation before separate Architecture, ADR,
  Implementation Design, and authorization gates.

## 6. Functional Requirements

- **Q010-FR-001:** The system shall represent a Trading Account with one stable,
  opaque BrokerOS reference that is separate from the internal database primary
  key and every external account identifier.
- **Q010-FR-002:** An external account identity shall be the complete approved
  tuple `AccountAuthorityScopeRef + SourceNamespace + ExternalAccountKey`.
  Missing tuple elements or a raw login/account number shall not be
  authoritative.
- **Q010-FR-003:** The system shall map one authoritative scoped external
  identity to exactly one BrokerOS Trading Account reference. The Foundation
  shall reject aliases, merges, one-to-many mappings, ambiguity, and
  reassignment.
- **Q010-FR-004:** Registration or import shall be explicit, attributable,
  idempotent for an exact request, and conflict on materially different state.
  It shall occur only through the approved non-web provisioning authority.
  Unknown references shall not be created just because an application or HTTP
  request presents them.
- **Q010-FR-005:** A read-only reference query shall distinguish recognized,
  not-found, ambiguous/conflicting, not-eligible-for-new-use, and authority-
  unavailable outcomes without returning vendor DTOs or sensitive account data.
- **Q010-FR-006:** A reference that has historical associations shall remain
  resolvable for history after lifecycle change. Normal operations shall not
  physically delete or silently reuse the stable BrokerOS reference.
- **Q010-FR-007:** Lifecycle and mapping changes shall occur through named,
  concurrency-safe operations with actor, time, reason, source, before/after,
  and version provenance.
- **Q010-FR-008:** Protected reads and every mutation shall obtain ActorRef from
  Q-009 trusted ActorContext and require an exact Q-010-owned capability before
  accessing or changing authority data.
- **Q010-FR-009:** Missing authentication, missing capability, unknown source,
  ambiguity, mapping conflict, and dependency/database unavailability shall
  fail closed and shall not create, disclose, or mutate an account reference.
- **Q010-FR-010:** The authority shall expose only the minimum safe metadata
  needed by an approved consumer to validate the reference, its provenance,
  and its eligibility for that use. It shall not expose credentials, customer
  identity, balances, positions, or vendor payloads.
- **Q010-FR-011:** External protocol, source-system, and vendor identifiers
  shall remain behind adapters/mappers and shall not leak into downstream
  Risk Case or Core Domain types.
- **Q010-FR-012:** The initial implementation, if later approved, shall create
  no Kafka business topic or Redis business key and shall use no permissive,
  fake, always-true, or unchecked production provider.

## 7. Acceptance Criteria

1. Q-010 Requirement V1 and the Section 2.1 resolutions are recorded as
   approved through the Q-010 V2 Requirement Architect Review.
2. Architecture translates the approved authority owner, non-web provisioning,
   identity tuple, one-to-one cardinality, lifecycle/history, failure, and
   consumer constraints into a conforming technical design without reopening
   them.
3. ADR determination is **Required: YES** for the new business identity,
   authority ownership, mapping, lifecycle, and external-system boundary; no
   implementation begins before the ADR is accepted.
4. A stable BrokerOS Trading Account reference cannot be derived from, or
   replaced by, a raw vendor account number or database ID.
5. Exact scoped external-identity uniqueness, duplicate registration,
   reassignment, concurrency, and lifecycle conflicts have testable outcomes.
6. Q-008 and future consumers can ask only a narrow read-only question and
   cannot create, mutate, or bypass the account authority.
7. Q-009 authentication, ActorContext, capability checks, default deny, safe
   failures, and trusted attribution protect every approved use case.
8. Material changes retain durable who/when/what/before/after/reason/source
   evidence and do not rely on Kafka-only or best-effort audit.
9. Any future schema is additive, Flyway-owned, application-owned, and verified
   against disposable MySQL 8.4; no external database is read or modified.
10. No trading data, financial calculation, customer data, Risk Case, Rule
    Engine, Account Control, vendor adapter, Kafka, Redis, or prohibited
    technology is introduced under this boundary.
11. Full Maven, MySQL/Flyway, security, static, Docker/Kubernetes, and Review
    gates pass for any later implementation with no mandatory test skipped.
12. Requirement, Architecture, ADR, Implementation Design, implementation
    authorization, implementation, runtime verification, final Architect
    review, staging, and commit remain separate recorded gates.

## 8. Technical Constraints

- Preserve Java 21, Spring Boot 3.x, Maven, MySQL, Flyway, and the single Phase
  1 modular-monolith deployable.
- Keep the capability broker-, CRM-, platform-, and vendor-neutral.
- Use adapters for external account sources; do not invent an MT4/MT5/CRM SDK
  contract without an actual supported integration Requirement and SDK/API.
- Use application-owned MySQL as durable state only if Architecture approves
  BrokerOS ownership of the reference registry.
- Use `snake_case`, `BIGINT id`, immutable business reference, UTC timestamps,
  stable readable codes, optimistic versions, and explicit uniqueness/
  referential constraints in any later schema.
- Do not use money or trading-value fields in this foundation. If introduced by
  another Requirement, use `BigDecimal`/`DECIMAL` plus explicit currency and
  approved precision rules.
- Keep API DTOs, domain references, persistence records, and external adapter
  types separate. No entity may be exposed through an API.
- Do not add a dependency, migration, source, configuration, endpoint, topic,
  key, or deployment object before the corresponding gates approve it.

## 9. Security and Audit Constraints

- Caller-supplied ActorRef, username, role, account-owner claim, account
  reference, Request ID, or Trace ID cannot establish identity or permission.
- Protected operations use the Q-009 trusted context and one exact capability;
  an authenticated actor has no implicit access.
- Query/error behavior must not disclose whether a sensitive account exists to
  an unauthorized caller.
- Do not log credentials, tokens, full external identity keys, customer data,
  or vendor payloads. Bounded safe correlation may be retained separately.
- Every registration, mapping, lifecycle, restore, and conflict override (if
  any is later approved) requires durable trusted actor/source attribution.
- Retention, legal hold, exceptional redaction, and a general Audit query API
  remain separate Requirements; history required for reference integrity may
  not be silently deleted.

## 10. Data and Schema Considerations

A future Architecture/Implementation Design must design a minimal
application-owned registry, the one-to-one scoped external mapping, and
append-only lifecycle/provenance history that conform to Section 2.1. It must
decide:

- the internal business-reference format;
- representation and canonicalization of the approved authority-scope,
  source-namespace, and external-key tuple;
- how constraints enforce the approved one-to-one, no-reassignment rule;
- lifecycle code names and operations while preserving the approved active/
  historical eligibility semantics;
- historical resolvability after deactivation/retirement;
- case-sensitive/binary collation requirements;
- optimistic locking, idempotency, indexes, and duplicate handling; and
- atomic change-history/audit consistency.

No table, column, migration number, lifecycle code name, or identifier encoding
is approved by this Requirement.

## 11. Dependencies

- Q-001 through Q-006 engineering foundations — implemented and reused.
- Q-007 / ADR-009 — authoritative upstream/Core Domain/downstream ownership.
- Q-008 / ADR-010 — approved consumer need; remains unimplemented and blocked.
- Q-009 / ADR-011 — implemented trusted Actor/authorization foundation.
- ADR-002 — external-system isolation and no direct external database writes.

Q-010 must not absorb the missing Evidence, Decision, Action, or ActionOutcome
providers. Completing Q-010 alone does not authorize Q-008 implementation.

## 12. Verification Plan

### Requirement Review phase

- verify the committed Q-009 baseline and current Git state;
- compare Q-007/Q-008/Q-009 requirements, ADRs, Skills, Lessons, closure
  Reviews, and outstanding prerequisites;
- verify Section 2.1 resolves every mandatory Requirement boundary decision;
- verify approval records only the Requirement Gate and preserves Architecture,
  ADR, Implementation Design, and implementation restrictions;
- run whitespace, scope, secret, ZIP, and Git checks; and
- record Maven/runtime checks honestly as baseline-only if executed.

### Future approved implementation

- domain tests for reference/key invariants and lifecycle/history;
- application tests for authorization-before-access, idempotency, conflicts,
  attribution, and fail-closed authority behavior;
- disposable MySQL 8.4 tests for migration, uniqueness, collations,
  constraints, optimistic concurrency, query plans, and history;
- security tests for spoofing, unauthorized existence disclosure, capability
  denial, dependency failure, and safe logs/errors;
- adapter contract tests only for an actual approved source; and
- full Maven, Flyway, static, Compose, Kustomize, and Review gates with no
  mandatory skip.

## 13. Risks and Architecture Inputs

The Requirement Gate resolved authority scope, uniqueness, cardinality,
lifecycle eligibility, controlled non-web provisioning, consumer disclosure,
security, audit, and failure semantics in Section 2.1. Architecture must now
resolve only the conforming technical detail, including:

1. opaque formats and validation for `TradingAccountRef`,
   `AccountAuthorityScopeRef`, and `SourceNamespace`;
2. how deployment governance bootstraps approved authority scopes and validates
   the broker/source-owner provenance reference without storing a credential or
   vendor payload;
3. exact manifest schema, fingerprint/idempotency behavior, concurrency,
   transaction, and conflict diagnostics for non-web provisioning;
4. minimal lifecycle code names and whether reactivation is included in the
   first implementation or remains disabled;
5. exact protected internal query contracts and bounded authority
   version/provenance fields;
6. additive MySQL schema, binary/case normalization, indexes, constraints,
   history, retention, and query plans; and
7. ADR content for business identity ownership, source-of-truth, mapping,
   lifecycle, external isolation, and published consumer boundary.

Architecture must return **CHANGES REQUIRED** rather than weaken Section 2.1.
If no controlled provisioning authority or broker/source-owner-approved record
can be established for a target deployment, implementation remains blocked.

## 14. Deliverables

### Approved Requirement deliverables

- this approved Requirement V1;
- Q-010 V1 capability/gap and candidate recommendation Review Package; and
- the independent Q-010 V2 Requirement Architect Review Package and ZIP.

### Architecture deliverables completed

- approved Q-010 Architecture V1 and accepted ADR-012.

### Future deliverables only after separate authorization

- separately approved Implementation Design;
- implementation only after explicit authorization;
- tests, Skill/Lessons updates from actual work, runtime verification, and
  final Review Package.

## 15. Current Gate

Q-010 Requirement status: **APPROVED — V1**

Q-010 Architecture: **APPROVED — V1 — 2026-08-27**

Q-010 ADR: **ADR-012 ACCEPTED — 2026-08-27**

Q-010 Implementation Design: **APPROVED — V1 — EXTERNAL ARCHITECT —
2026-08-27**

Q-010 Design Approval Recording: **V6 — REVIEWED / APPROVED**

Q-010 Implementation: **APPROVED — V7 — EXTERNAL ARCHITECT — 2026-08-27**

Q-010 Implementation Allowed: **YES — EXPLICIT V7 AUTHORIZATION EXECUTED**

Q-010 Verification: **PASS — V8 — 2026-08-28**

Q-010 Final Closure: **PASS / CLOSED — V8 — 2026-08-28**

Q-010 Ready for Git Commit: **YES — CLOSURE ASSESSMENT ONLY**

Q-010 Git Commit / Push: **NOT PERFORMED**

Next gate: **Independent Architect Final Review of the V8 Final Closure
package. Only after that review may the Product Owner manually commit.**

### Post-Closure Fix: Shared Clock Microsecond-Precision (2026-08-31)

After V8 closure, independent Q-011 review (Claude Code, holding the
external Architect role) discovered a pre-existing Q-010 defect during
cross-environment test execution: `Q010BootstrapMySqlIntegrationTests`'s
idempotent-replay assertion could fail on hosts whose JVM clock exposes
genuine sub-microsecond precision, because the single shared `Clock` bean
(`SecurityModuleConfiguration.securityClock()`) minted instants at full
clock precision while the `DATETIME(6)` columns that persist them cap at
microseconds. See
`docs/lessons/2026-08-31-q-010-bootstrap-replay-timestamp-precision.md`.

Q-010 Timestamp-Precision Fix: **AUTHORIZED — 2026-08-31 — Product Owner**,
scoped to the single shared `Clock` bean only (`Clock.tick(Clock.systemUTC(),
Duration.ofNanos(1000))`), so Q-009 and Q-011 are corrected identically at
the same source.

Q-010 Timestamp-Precision Fix — Independent Review: **PASS — 2026-08-31 —
Claude Code.** Verified the diff directly (`SecurityModuleConfiguration.java`
only, 2 insertions/1 deletion) and independently re-executed the full
124-test real-MySQL gate in the Linux/Docker environment that had twice
reproduced the original failure deterministically: 0 failures, 0 errors.
Package: `review/q-010/review-q-010-v9-shared-clock-precision-fix-20260831-141025/`
(+ `ClaudeCodeIndependentReview.md`).

Q-010 Timestamp-Precision Fix: **ACCEPTED — 2026-08-31 — Product Owner.**

Q-010 Git Commit / Push (post-fix): **STILL NOT PERFORMED** — accepting this
fix does not itself authorize staging, committing, or pushing; that remains
a separate, explicit decision.
