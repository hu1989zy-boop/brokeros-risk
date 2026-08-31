# Q-011: Evidence Provenance Foundation

## Status

**V3 — APPROVED — 2026-08-28 — Product Owner**

(This table went stale once already this session — round three fixed it
without checking whether the Requirement's own body was still consistent.
It was not: Goal 5 (§4) still overclaimed `HUMAN` for every protected use
case. Round four — a fourth, independently-authored governance-consistency
pass — found this and several related leftovers across all four documents.
This document's own finding is recorded in §19; the complete four-document
record is Implementation Design §20.10.)

| Gate | Result |
| --- | --- |
| Requirement Candidate Analysis | COMPLETE — V1 |
| Requirement Review | CHANGES REQUIRED — V1; ADDRESSED — V2 (see §18) |
| Requirement Approval | APPROVED — V2 — 2026-08-28 — Product Owner |
| Requirement V3 (this correction) | **APPROVED — 2026-08-28 — Product Owner** (§4 Goal 5 fix; see §19) |
| Architecture | APPROVED V3 — 2026-08-28; **V4 APPROVED — 2026-08-28 — Product Owner** (see Architecture document) |
| ADR | ADR-013 ACCEPTED — 2026-08-28; **amendment RE-ACCEPTED — 2026-08-28 — Product Owner** (see ADR-013's own Amendment section) |
| Implementation Design | APPROVED V4 — 2026-08-28; **V5 APPROVED — 2026-08-28 — Product Owner** (see Implementation Design document) |
| Implementation | **AUTHORIZED — 2026-08-28 — Product Owner, against Requirement V3/Architecture V4/ADR-013-as-amended/Design V5** |
| Ready for Git Commit | NO — no implementation exists yet |
| Implementation Allowed | **YES — see Implementation Design §20 for the full four-round correction history and the Codex Prompt issued 2026-08-28** |

- Requirement ID: `Q-011` — a working ID only. It is the next unused sequential
  number as of 2026-08-28 and is not itself a reservation or approval.
- Requirement version: `V3 — APPROVED — 2026-08-28` (supersedes V2; the
  Goal 5 correction is the only substantive change — see §19)
- Prepared and reviewed by: Claude Code, holding both the requirement-drafting
  role and, per the Product Owner's explicit 2026-08-28 direction, the
  external Architect review role previously held by a separate ChatGPT-based
  process (see `prompts/Claude-Code-BrokerOS-Risk-Project-Handover-Prompt.md`
  and this session's transcript). Because the same party drafted and
  reviewed this document, Section 18 records the review as a self-review,
  and final approval authority remains explicitly with the Product Owner —
  it is not implied by the review passing. Codex has written no code for
  this Requirement.
- Candidate Analysis reference:
  `review/q-011/review-q-011-v1-requirement-candidate-analysis-20260828-143834/`
- Architect Review record: Section 18 of this document.
- Architecture phase: Phase 1
- Capability type: upstream, Core-Domain-adjacent reference/provenance
  authority
- Governing model: ADR-009 and the Q-007 Core Domain baseline
- Intended first consumer: approved but prerequisite-blocked Q-008 Risk Case
  (`EvidenceReferenceQuery`, Implementation Design §14)

This V2 draft was approved by the Product Owner on 2026-08-28 (explicit
chat confirmation, "批准"), covering the Requirement Gate only: scope,
Functional/Security Requirements, domain definitions, and the Section 2.1-
style resolutions in this document. Requirement approval does not authorize
Java, API, schema, migration, or any other implementation artifact — it
authorizes proceeding to the Architecture and ADR Gate.

## 1. Background

Q-007 and ADR-009 established the canonical BrokerOS Risk model:

```text
Evidence → Decision → Action → Risk Case
```

Evidence is "traceable support for or against a risk conclusion," and its
provenance must be preserved "so the conclusion can be explained." Evidence
is upstream of the Decision Core Domain; it does not depend on Decision,
Action, or Risk Case existing.

Q-008 approved a Risk Case foundation whose Implementation Design (§14)
requires a real `EvidenceReferenceQuery` provider before any Evidence
association use case can be implemented. Q-008's own Requirement (§11,
"Evidence Requirements") already specifies how Risk Case will consume
Evidence once it exists: multiple historical associations, append-only
attribution of who/when/source/reason, no editing of Evidence content by
Risk Case, and correction through new upstream Evidence plus an explicit
association disposition (`SUPERSEDED`, `INVALIDATED`, or `WITHDRAWN`) held by
the case, not by Evidence itself. This Requirement is written to be directly
compatible with that existing text and does not reinterpret it.

Q-009 has implemented the trusted Actor/authorization foundation. Q-010 has
implemented the Trading Account Reference Authority. The Q-011 Requirement
Candidate Analysis
(`review/q-011/review-q-011-v1-requirement-candidate-analysis-20260828-143834/`)
evaluated Evidence, Decision, Action, and ActionOutcome as the remaining
Q-008 provider candidates and selected Evidence because it is the only one
whose blocking prerequisite — a trusted actor and a trusted subject to scope
observations to — is now satisfied, and because Decision cannot exist
without it under ADR-009.

## 2. Existing Capability and Gap Analysis

### 2.1 Already Exists

| Capability | Repository evidence | Q-011 treatment |
| --- | --- | --- |
| Core-domain language and ownership | Q-007, ADR-009, `brokeros-risk-core-domain` skill | Reuse unchanged: Evidence remains upstream of Decision; Q-011 does not become the Core Domain |
| Trusted Actor/authorization | Q-009, ADR-011 | Reuse `ActorContext`, `Capability`, `AuthorizationGuard` unchanged; no new identity model |
| Trading Account Reference Authority | Q-010, ADR-012 | Reuse `TradingAccountRef` as the only approved Evidence subject type; no new subject model |
| Q-008 Evidence consumption contract | Q-008-Requirement.md §11, Implementation Design §14 | Design Q-011 to satisfy this text exactly; do not reopen it |
| Engineering contracts | `ApiResponse`, `GlobalExceptionHandler`, Bean Validation, Flyway, MySQL, UTC/enum/audit standards | Reuse; no duplicate foundation |

### 2.2 Need Improvement

- Evidence has a semantic definition (ADR-009) but no runtime record, ref
  format, or provider.
- No convention exists for distinguishing an Evidence record's own validity
  (was it later corrected upstream) from a consuming case's own judgment
  about that Evidence's relevance to a specific investigation.
- No convention exists for bounding Evidence content so it cannot become
  unstructured document/file storage.

### 2.3 Missing

Repository inspection found no `Evidence` package, entity, reference type,
repository, service, controller, migration, or provider anywhere in
`backend/src/main`.

### 2.4 Out of Scope

The absence of an Evidence provider does not authorize Q-011 to implement
Decision, Action, ActionOutcome, Risk Case, Rule Engine, AI, or any Trading
Data ingestion/adapter. Section 5 is authoritative.

### 2.5 Open Decisions

The exact `EvidenceRef` encoding, content length bounds, the precise Q-010
read contract Evidence recording should validate a subject against, and
ResultCode names are not decided by this Requirement and remain open for the
Architecture Gate (see Section 14).

## 3. Problem Statement

BrokerOS Risk cannot currently answer, through an application-owned,
broker-neutral, fail-closed mechanism:

- whether a piece of traceable risk-relevant information has been recorded
  at all;
- who recorded it, when, and about which recognized subject;
- whether it remains the current record or has been corrected by a later
  observation; or
- whether recording, correction, and query operations were authorized and
  attributable.

Without this boundary, a future Decision capability would have nothing
authoritative to derive from, and Q-008 could not implement
`DECISION_DRIVEN` intake or Evidence association without trusting an
unchecked string as if it were Evidence.

## 4. Goals

1. Establish one stable, opaque, broker-neutral BrokerOS Evidence reference,
   distinct from database IDs and any future vendor identifier.
2. Preserve traceable provenance — actor, time, source, subject — for every
   recorded Evidence item, so a later Decision can be explained.
3. Support correction without loss of history: a corrected observation
   becomes new Evidence; the record it corrects remains permanently
   queryable and marked superseded.
4. Provide a narrow, two-tier read contract: a bounded existence/provenance
   check for consumers such as Q-008, and a separate, independently
   authorized full-detail read for direct Evidence review.
5. Apply Q-009 trusted ActorContext and capability authorization to every
   protected use case. Additionally require `ActorType.HUMAN` for the two
   authoring use cases (recording and correction) only; the two read use
   cases (existence/provenance and full-detail) require authorization but
   no additional actor-type restriction (V3 fix, fourth correction round:
   this Goal previously read as if `HUMAN` were required for every
   protected use case, contradicting `Q011-FR-005`'s correct scoping and
   Design §11.4; see the Requirement's own governance history in §18/§19).
6. Fail closed on unknown, ambiguous, or unavailable authority outcomes.
7. Preserve broker/CRM/platform neutrality and the Phase 1 modular-monolith
   constraint.
8. Remain exactly compatible with the Evidence semantics Q-008 already
   specified, without reopening ADR-009 or Q-008.

## 5. Scope and Non-Goals

### 5.1 In Scope

- semantic ownership of `EvidenceRef` and its minimal bounded content shape;
- exactly one approved Evidence source for this Foundation: `MANUAL`
  (human-recorded observation);
- exactly one approved Evidence subject type: `TRADING_ACCOUNT`, validated
  through the Q-010 authority;
- immutability of recorded Evidence and a controlled, auditable correction
  operation that creates new Evidence and marks the corrected record
  superseded;
- the two-tier read-contract design described in Goal 4;
- Q-009 ActorContext, exact capability checks, `HUMAN`-only recording,
  default deny, and attributable change requirements;
- application-owned MySQL/Flyway, transaction, concurrency, and immutable
  history analysis for a later approved implementation;
- failure semantics for not found, conflict, and authority unavailability;
  and
- required ADR analysis for identity ownership, immutability/correction
  model, and consumer boundary.

### 5.2 Non-Goals

Q-011 does not authorize or define:

- Decision, Action, ActionOutcome, Risk Case, Rule Engine, Account Control,
  or external Execution implementation;
- any Evidence source other than `MANUAL` — automated detection, Rule Engine
  hits, trading-data anomaly feeds, or external alerts require a separate
  approved extension Requirement;
- any Evidence subject type other than `TRADING_ACCOUNT`;
- file, blob, document, or KYC-document storage; Evidence content is bounded
  structured text only;
- an Evidence category/taxonomy, confidence score, or severity rating —
  those remain Decision-owned concerns per ADR-009;
- multi-subject Evidence, cross-account aggregate Evidence, or Evidence
  merge/deduplication;
- Risk Case's own association-disposition bookkeeping (`SUPERSEDED` /
  `INVALIDATED` / `WITHDRAWN` at the case-association level) — that remains
  owned by Q-008 exactly as already specified in Q-008-Requirement.md §11;
  Q-011 owns only Evidence's own record-level status;
- `SERVICE`-actor-authored Evidence;
- Kafka topic, Redis key, search/reporting platform, or full-text index over
  Evidence content;
- legal hold, retention duration, or redaction workflow beyond "no physical
  deletion," mirroring the exact deferral Q-008 §18 already recorded;
- evidence polarity (whether a record supports or refutes a conclusion) or
  any other interpretive classification — that judgment remains
  Decision-owned per ADR-009; Evidence records the observation only; and
- MT4/MT5/CRM adapter behavior of any kind.

## 6. Domain Definitions

### 6.1 Evidence

Unchanged from ADR-009: traceable information supporting or refuting a risk
conclusion, with preserved source provenance. Q-011 does not redefine this
term; it implements a provider for it.

### 6.2 Evidence Source

**Evidence Source** identifies the mechanism that produced an Evidence
record. This Foundation defines exactly one value, `MANUAL` — an
authenticated `HUMAN` actor records a direct observation or suspicion. This
mirrors Q-008's own `MANUAL` vs. `DECISION_DRIVEN` intake-source pattern:
naming the approved value now does not foreclose adding
`RULE_ENGINE_DETECTED` or similar values later through a separate
Requirement.

### 6.3 Evidence Status and Correction

Evidence is immutable once recorded. **Correction** is a named operation
that creates a new Evidence record carrying a reference to the record it
corrects, carries the same subject reference as the record it corrects, and
requires a mandatory correction reason distinct from its own observation
text. It sets the corrected record's status to `SUPERSEDED`. `ACTIVE`
is the default status for a record with no correction. A correction shall
never change the subject a corrected record concerns — an Evidence record
about one Trading Account cannot be "corrected" into a record about a
different one; that would be a new, unrelated Evidence record instead.
Status describes
Evidence's own validity as a source-of-truth record; it is distinct from,
and does not replace, a consuming Risk Case's own association-level
disposition (Q-008-Requirement.md §11). Superseded Evidence is never deleted
and remains permanently queryable so a later reviewer can explain why an
earlier Decision changed — this restates Q-008-Requirement.md §11's own
"invalid Evidence remains part of the historical basis" requirement at the
Evidence layer itself.

### 6.4 Provenance

**Provenance** is the bounded set of facts needed to explain an Evidence
record without exposing its full content to every caller: subject
reference, source, recording actor reference, recorded-at UTC time, status,
and — if superseded — a pointer to its replacement.

### 6.5 Subject Reference

Evidence references exactly one primary typed subject, reusing Q-008's
typed-subject pattern. The only approved type for this Foundation is
`TRADING_ACCOUNT`, validated through the Q-010 authority. Additional subject
types require an approved extension, exactly mirroring Q008-FR-003.

## 7. Functional Requirements

- **Q011-FR-001:** The system shall represent Evidence with one stable,
  opaque, immutable BrokerOS reference (`EvidenceRef`), separate from the
  internal database primary key.
- **Q011-FR-002:** Every Evidence record shall reference exactly one
  `TRADING_ACCOUNT` subject, validated as recognized by the approved Q-010
  authority. Recognition for this purpose means the reference exists in the
  Q-010 authority; it is a lower bar than, and must not be conflated with,
  Q-010's stricter "eligible for a new Risk Case association" check. An
  unrecognized subject reference shall be rejected and shall not create
  Evidence.
- **Q011-FR-003:** Evidence content shall be a bounded, structured record
  containing a mandatory observation/summary text, source, subject
  reference, recording `ActorRef`, and UTC recorded-at time. Evidence shall
  not store binary attachments, files, or documents.
- **Q011-FR-004:** The Foundation shall support exactly one Evidence source
  value, `MANUAL`. Additional values require an approved extension
  Requirement.
- **Q011-FR-005:** Only an authenticated actor of type `HUMAN` may record or
  correct Evidence. A `SERVICE` actor shall not record Evidence in this
  Foundation.
- **Q011-FR-006:** Evidence shall be immutable after recording; no named
  operation shall edit existing Evidence content.
- **Q011-FR-007:** A correction shall create a new Evidence record
  referencing the `EvidenceRef` it corrects, carrying the identical subject
  reference as the record it corrects, and carrying a mandatory correction
  reason. It shall set the corrected record's status to `SUPERSEDED` in the
  same transaction. Both records remain permanently queryable. A correction
  request whose subject reference differs from the record it targets shall
  be rejected as invalid rather than silently accepted.
- **Q011-FR-008:** One Evidence record may be superseded by at most one
  replacement. The system shall reject a second correction targeting an
  already-superseded record rather than creating a branching or ambiguous
  supersession chain.
- **Q011-FR-009:** Recording and correction shall be idempotent for an exact
  repeated request and shall conflict on materially different repeated
  content.
- **Q011-FR-010:** The Foundation shall expose two distinct protected read
  contracts:
  1. a narrow existence/provenance contract, for consumers such as Q-008,
     that distinguishes recognized, not-found, and authority-unavailable
     outcomes and returns only bounded provenance (Section 6.4) without the
     observation text; and
  2. a full-detail contract, independently protected by its own capability
     check, that returns the complete Evidence record including
     observation text for direct Evidence review.

  Authorization for contract (1) does not imply authorization for contract
  (2).
- **Q011-FR-011:** Missing authentication, missing capability, unknown
  reference, ambiguous state, and dependency/database unavailability shall
  fail closed and shall not create, disclose, or mutate an Evidence record.
- **Q011-FR-012:** Recording and correction shall retain the trusted actor,
  UTC time, operation, immutable target, subject reference, reason (mandatory
  for correction), and before/after state in the same application-owned
  transaction as the change.
- **Q011-FR-013:** The Foundation shall create no Kafka business topic,
  Redis business key, file/blob storage, or permissive/always-true
  production provider.
- **Q011-FR-014:** A full-detail read (Q011-FR-010, contract 2) of Evidence
  observation text is itself an auditable access event. It shall capture the
  accessing actor and UTC time, mirroring the read-audit pattern Q-008
  Implementation Design §9.5 already established for sensitive Risk Case
  content (`RISK_CASE_VIEWED`). If access-audit persistence fails, no
  sensitive content shall be returned.

## 8. Security Requirements

- **Q011-SR-001:** Caller-supplied actor ID, username, or subject-owner
  claim shall not establish recording authority. `ActorRef` shall come only
  from the Q-009 trusted `ActorContext`.
- **Q011-SR-002:** Protected operations shall use exactly the capabilities
  `evidence:read`, `evidence:record`, and `evidence:correct`. An
  authenticated actor has no implicit access.
- **Q011-SR-003:** Query and error behavior shall not disclose whether
  Evidence exists for a subject to an unauthorized caller.
- **Q011-SR-004:** Observation text may contain sensitive investigation
  content. Access shall be controlled and auditable, and the Foundation
  shall not log full observation text at INFO level or above; only bounded
  safe correlation may be logged.
- **Q011-SR-005:** The Foundation shall not accept `SERVICE`-actor-authored
  Evidence (Q011-FR-005). A future Requirement must explicitly approve an
  automated-source security model before that restriction is relaxed.
- **Q011-SR-006:** Audit before/after state shall reference `EvidenceRef`
  and bounded metadata only; full observation text shall not be persisted
  in Audit JSON, mirroring the pattern already approved in Q-008
  Implementation Design §8.2.

## 9. Data Integrity and Provenance Requirements

- Future application-owned schema changes use new immutable Flyway
  migrations, `snake_case`, `BIGINT id`, and a separate immutable business
  reference for `EvidenceRef`. No table, column, migration number, or exact
  reference encoding is approved by this Requirement; a
  `ev-<uuid4>`-style prefixed reference, consistent with the `ta-`/`aas-`
  convention Q-010 established, is a reasonable Architecture starting point
  but is not selected here.
- All persisted timestamps use UTC.
- Evidence is never physically deleted. Status transitions only from
  `ACTIVE` to `SUPERSEDED`, once, per record.
- Correction and idempotent-retry behavior require optimistic concurrency
  or an equivalently explicit conflict strategy; concurrent corrections of
  the same record must not both succeed (Q011-FR-008).
- `EvidenceRef` and the recording/correction operation identity are unique
  and enforced by database constraint, not application-only logic.
- Referential integrity to the subject is to the Q-010 `TradingAccountRef`
  authority only; Evidence never stores or references a raw vendor account
  identifier.

## 10. Acceptance Criteria

### 10.1 Requirement Gate

1. The Existing Capability/Gap Analysis is based on repository evidence and
   does not claim an Evidence provider already exists.
2. This Requirement is exactly compatible with Q-008-Requirement.md §11 and
   does not reinterpret or narrow it.
3. Evidence Source is limited to `MANUAL`; Evidence Subject is limited to
   `TRADING_ACCOUNT`; both are explicit non-goals to extend without a
   separate Requirement.
4. The distinction between Evidence's own record-level status and Risk
   Case's association-level disposition is explicit and does not duplicate
   Q-008's ownership.
5. The two-tier read-contract design (Q011-FR-010) is explicit.
6. ADR need is recorded as **YES** (Section 14); no ADR is created or
   accepted by this Requirement.
7. No implementation, dependency, migration, API, commit, or push occurs
   during this gate.

### 10.2 Future Behavior Gate After Separate Authorization

8. Recording, correction, and both read contracts have testable outcomes
   for valid, invalid, unauthorized, not-found, and conflicting inputs.
9. `SERVICE` actors cannot record or correct Evidence.
10. Superseded Evidence remains queryable and is never deleted.
11. A subject reference unrecognized by the Q-010 authority cannot produce
    Evidence.
12. Every recording/correction creates its audit record in the same
    application-owned transaction as the change, and every correction
    record carries a mandatory reason.
13. A correction whose subject reference differs from the record it targets
    is rejected; no correction can change the subject an Evidence record
    concerns.
14. A full-detail read of observation text creates its own access-audit
    record before content is returned.
15. Full Maven, MySQL/Flyway, security, and static verification gates pass
    with no mandatory test skipped.

## 11. Technical Constraints

- Preserve Java 21, Spring Boot 3.x, Maven, MySQL, Flyway, and the single
  Phase 1 modular-monolith deployable.
- Keep the capability broker-, CRM-, and platform-neutral.
- Reuse `ApiResponse`, `GlobalExceptionHandler`, Bean Validation, and
  existing observability/configuration foundations; create no parallel
  contract.
- Do not use money or trading-value fields; not applicable to this
  Foundation.
- Keep API DTOs, domain references, persistence records, and any future
  adapter types separate. No entity may be exposed through an API.
- Do not add a dependency, migration, source, configuration, endpoint,
  topic, key, or deployment object before the corresponding gates approve
  it.
- Unlike Q-010, this Foundation's protected operations are expected to be
  exposed through a normal authenticated HTTP surface (Evidence recording is
  a routine operational action by authorized staff, not a low-frequency,
  externally attested registration event like Q-010's account mapping).
  Exact endpoint design remains an Implementation Design decision.

## 12. Dependencies

- Q-001 through Q-006 engineering foundations — implemented and reused.
- Q-007 / ADR-009 — authoritative Evidence definition and Core Domain
  ownership direction.
- Q-008 / ADR-010 — approved consumer need (`EvidenceReferenceQuery`);
  remains unimplemented and blocked on more than this Requirement alone.
- Q-009 / ADR-011 — implemented trusted Actor/authorization foundation,
  reused unchanged.
- Q-010 / ADR-012 — implemented Trading Account Reference Authority, reused
  as the only approved Evidence subject validator.

Q-011 must not absorb the still-missing Decision, Action, or ActionOutcome
providers. **Completing Q-011 alone does not authorize Q-008
implementation** — Decision and Action providers remain separately required,
exactly as Q-010's completion alone did not authorize it either.

## 13. Verification Plan

### Requirement Review phase

- verify the committed Q-009/Q-010 baseline and current Git state;
- compare this Requirement against Q-007/ADR-009, Q-008-Requirement.md §11,
  and the Q-011 Candidate Analysis for consistency;
- verify Section 5 resolves source, subject, and status scope without
  reopening Q-008;
- run whitespace, scope, secret, and Git checks; and
- record Maven/runtime checks honestly as baseline-only if executed.

### Future approved implementation

- domain tests for `EvidenceRef` invariants, immutability, and single-level
  supersession (Q011-FR-008);
- application tests for authorization-before-access, `HUMAN`-only
  recording, idempotency, and fail-closed subject validation;
- disposable MySQL tests for uniqueness, concurrency, and constraint
  enforcement;
- security tests for spoofing, actor-type restriction, existence
  non-disclosure, and content non-leakage into logs/audit; and
- full Maven, Flyway, static, and Review gates with no mandatory skip.

## 14. Risks and Architecture Inputs

1. **Subject-validation contract choice and capability coupling:** Q-010
   currently publishes one consumer method,
   `validateForNewRiskCaseAssociation(ActorContext, TradingAccountRef)`,
   whose `ELIGIBLE_FOR_NEW_ASSOCIATION`/`RECOGNIZED_NOT_ELIGIBLE`/
   `NOT_RECOGNIZED` semantics are scoped to *new Risk Case association
   eligibility*, and whose signature requires the **caller's own
   `ActorContext`** to hold Q-010's `trading-account-reference:read`
   capability. Two problems follow if Q-011 reuses it unchanged: (a) Evidence
   may legitimately need to reference a `RECOGNIZED_NOT_ELIGIBLE`
   (inactive/historical) account, which this contract was not designed to
   confirm as acceptable; and (b) every `HUMAN` actor authorized to record
   Evidence would also need to be separately granted a Trading Account
   Reference Authority capability just to satisfy subject validation — an
   awkward cross-module authorization coupling that does not belong in an
   Evidence-recording analyst's grant set. Architecture must decide between:
   Q-010 publishing a second, broader "recognized" contract callable under
   Q-011's own service-level authorization rather than the recording actor's
   context; or another mechanism that avoids granting Trading-Account-module
   capabilities to Evidence-recording actors. This Requirement does not
   resolve that choice.
2. Exact `EvidenceRef` format and validation.
3. Exact bounded length for observation/summary text and correction reason.
4. Exact ResultCode names for not-found, conflict, and
   authority-unavailable outcomes.
5. Additive MySQL schema, indexes, constraints, and query plans for the
   supersession chain.
6. ADR content for Evidence identity ownership, immutability/correction
   model, and the two-tier consumer boundary.

(Whether correction requires a mandatory reason was resolved during Requirement
Review V2: it does — see Q011-FR-007/FR-012 — and is no longer an open
question.)

(Item 1's subject-validation choice was resolved at Architecture: Q-010's
existing three-outcome contract is reused unchanged, called with the
recording actor's own `ActorContext`; Q-011 simply accepts two of the three
outcomes (`ELIGIBLE_FOR_NEW_ASSOCIATION` and `RECOGNIZED_NOT_ELIGIBLE`)
instead of one, matching `Q011-FR-002`'s "recognized" bar exactly. No new
Q-010 contract, no service-level indirection. The capability-coupling
question in (b) was resolved separately: the two-capability grant is
accepted as normal least-privilege scoping, not solved by new
infrastructure. See Architecture §9 and this document's §18/§19.)

Architecture must return **CHANGES REQUIRED** rather than weaken Section 5
or 7-8. If subject validation cannot be resolved without weakening Q-010's
fail-closed guarantees, implementation remains blocked.

## 15. Deliverables

(V3 fix, sixth correction round: this section was written at V1/V2 drafting
time and listed "Java, API, migration..." etc. as "Not authorized now" and
Architecture/ADR/Design/implementation as future items "only after separate
authorization." That was accurate then. It went stale exactly the same way
§19 and ADR-013's introduction did in round five — a present-tense claim
embedded in a section nobody revisited when the document's overall gate
advanced through V2→V3 and Architecture/ADR/Design/implementation were all
separately approved and authorized. Rewritten below as a historical
snapshot of what was true at drafting time, with the current state stated
in present tense and pointed at §17, the sole current authority. See
`docs/lessons/2026-08-28-q-011-fifth-preimplementation-governance-blocker-correction.md`
for the reusable lesson this repeats.)

### Deliverables produced and approved to date (current as of 2026-08-28)

- this Requirement, now V3 — APPROVED (§17/§19);
- the Requirement Candidate Analysis Review Package
  (`review/q-011/review-q-011-v1-requirement-candidate-analysis-20260828-143834/`);
- independent Requirement Architect Review and approval recording (§18);
- Architecture V4 and ADR-013 (accepted, then amended and re-accepted) —
  APPROVED;
- Implementation Design V5 — APPROVED;
- a fresh, explicit implementation authorization — GRANTED.

Not yet produced: any Java, API, entity, repository, service, controller,
migration, table, endpoint, Kafka/Redis, adapter, test, or commit/push for
Q-011. Authorization for all of these exists (§17); their absence reflects
only that implementation has not yet been executed, not that it remains
unauthorized. §17 (Current Gate) is the sole current authority for this
document's gate status; if this section and §17 ever appear to disagree
again, §17 governs.

### Historical snapshot, at V1/V2 drafting time (superseded — do not treat as current)

At the time this Requirement was first drafted and through its V2 approval,
only the Requirement itself and its Candidate Analysis package existed;
Architecture, ADR, Implementation Design, and implementation were all
future items requiring separate authorization, and no Java, API, migration,
table, endpoint, Kafka/Redis, adapter, commit, or push was authorized. That
snapshot is preserved here as history and is no longer the current state —
see the subsection above.

## 16. Review Checklist

- [x] Repository evidence supports "no Evidence provider exists."
- [x] Exactly compatible with Q-008-Requirement.md §11; no reinterpretation.
- [x] Evidence Source limited to `MANUAL`; Subject limited to
      `TRADING_ACCOUNT`; both named as explicit extension points.
- [x] Evidence record-level status kept distinct from Risk Case
      association-level disposition.
- [x] Two-tier read-contract design stated explicitly.
- [x] `HUMAN`-only recording and `SERVICE` exclusion stated explicitly.
- [x] ADR Required YES recorded; no ADR created.
- [x] Non-goals prevent Decision/Action/ActionOutcome/Risk Case expansion.
- [x] Open Architecture questions (Section 14) stated rather than silently
      resolved.
- [x] Correction preserves the corrected record's subject reference and
      carries a mandatory reason (V2 fix; see Section 18).
- [x] Full-detail reads of sensitive content are themselves audited (V2 fix).
- [x] Independent Requirement Architect Review performed — **V2, see
      Section 18**.
- [x] Product Owner Requirement approval — **APPROVED, 2026-08-28**.

## 17. Current Gate

Q-011 Requirement Candidate Analysis: **COMPLETE — V1**

Q-011 Requirement Architect Review: **CHANGES REQUIRED — V1, then
addressed — V2, performed by Claude Code per the Product Owner's
2026-08-28 direction to hold the external Architect review role. Self-review
by the drafting party; recorded here as a disclosed limitation.**

Q-011 Requirement status: **V3 — APPROVED — 2026-08-28 — Product Owner**
(supersedes V2; Goal 5 correction only, see §19)

Q-011 Implementation Allowed: **YES — explicit Product Owner authorization
received 2026-08-28, granted together with approval of all four candidate
documents. Four rounds of governance correction occurred first, all
before any code was written. Rounds one through three fixed Design/
Architecture defects (six, then five, then four-plus-one) and resolved one
genuine Requirement-vs-Architecture contradiction (the subject bar — the
Requirement's "recognized" bar was confirmed correct, Architecture/Design
were corrected to match). Round four was an independently-authored,
explicitly mechanical consistency pass that found the fix had not fully
propagated: the Requirement's own Goal 5 still overclaimed `HUMAN` for
every protected use case (contradicting its own `Q011-FR-005`); ADR-013 —
accepted before the subject-bar correction — had never been amended and
still stated the old, wrong "eligible" bar throughout; and several
Architecture/Design sections still referenced stale version numbers or
listed inactive-subject Evidence as future scope after it had already
been brought in-scope. See Implementation Design §20.10 for the complete
finding-by-finding record across all four documents; this Requirement's
own share is §19.**

This round's fixes are approved as **Requirement V3, Architecture V4,
ADR-013's amendment (re-accepted), and Implementation Design V5** — all
four together, 2026-08-28. See:
`docs/architecture/q-011-evidence-provenance-foundation-architecture.md`,
`docs/adr/ADR-013-evidence-provenance-foundation.md`, and
`docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`.

Next gate: Codex executes the resume Prompt issued 2026-08-28, built
strictly from Implementation Design §11.1/§11.4, then Claude Code performs
an independent implementation review of the resulting non-overwriting
review package before the Product Owner considers any commit.

Q-011 Implementation: **SUBMITTED — 2026-08-30 — Codex**, review package
`review/q-011/review-q-011-v16-implementation-20260830-161236/`. Codex
self-reported 14 of 15 Acceptance Criteria PASS; AC15 FAIL due to a
pre-existing, unrelated `Q009MySqlIntegrationTests` stale migration-count
assertion (Codex correctly did not modify Q-009 to fix it, honoring the
task's hard boundary).

Q-011 Implementation Independent Review: **PASS (no Q-011 defect found) —
2026-08-30/31 — Claude Code, holding the external Architect role per the
Product Owner's 2026-08-27 direction.** Package
`review/q-011/review-q-011-v17-claude-code-independent-review-20260830-163904/`.
Verified by direct code/DDL inspection against Design V5 §11.1/§11.4/§8.5/§13
(not by accepting Codex's self-report), **and** by independently executing
the full Maven test suite (Java 21 + MySQL 8.4.11, disposable Docker
containers) rather than only re-reading Codex's reported results: 124
tests, 2 failures, 0 errors. Both failures are pre-existing and outside
Q-011's authorized change boundary:
- AC15 / `Q009MySqlIntegrationTests` — confirmed genuine, matches Codex's
  own report.
- A second, previously unreported finding:
  `Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay`
  fails in this Linux/Docker environment (did not fail on Codex's macOS
  host) due to a real, pre-existing timestamp-precision inconsistency in
  Q010's bootstrap replay path — see
  `docs/lessons/2026-08-31-q-010-bootstrap-replay-timestamp-precision.md`.
  Confirmed unrelated to Q-011 (no Q-011 file touches this code path) and
  reproducible (not a one-off flake).

Q-011 Implementation: **APPROVED — 2026-08-31 — Product Owner**, based on
the v16 Codex package and the v17 Claude Code independent review above.
See `docs/lessons/2026-08-31-q-011-implementation-approved.md`.

Q-011 AC15 Fix: **AUTHORIZED — 2026-08-31 — Product Owner**, narrowly
scoped to `Q009MySqlIntegrationTests.java` only (replace the hard-coded
expected migration count with one computed from `Flyway.info().pending()`
so it does not go stale again). Not yet executed.

Q-011 Ready for Git Commit: **NOT YET** — pending the AC15 fix, a final
closure review package, and separate explicit Product Owner authorization
to stage/commit.

Q-011 Git Commit / Push: **NOT PERFORMED.**

Next gate: Codex executes the narrowly-scoped AC15 fix per the resume
prompt in `prompts/`, re-runs the full real-MySQL gate, and produces a
non-overwriting closure review package. Claude Code then performs an
independent review of that package before the Product Owner considers any
commit. The separate Q010 bootstrap timestamp-precision finding is
deferred — tracked as a lesson, not authorized for a code fix at this
gate.

## 18. Requirement Architect Review Record (V1 → V2)

Performed 2026-08-28 by Claude Code, acting as the external Architect role
per the Product Owner's explicit direction that this role transfers from a
prior ChatGPT-based process. This is a self-review of a document the same
party drafted; it is not an independent third-party review, and per the
governance rule stated in
`prompts/Claude-Code-BrokerOS-Risk-Project-Handover-Prompt.md` §1
("do not self-approve your own architecture/design merely because you
authored it"), it does **not** constitute Requirement approval. It is an
adversarial-style critique intended to surface defects before the Product
Owner decides.

**Verdict on V1: CHANGES REQUIRED.** Findings:

1. **(Fixed)** V1's `Q011-FR-007` did not require a correction to preserve
   the corrected record's subject reference. As drafted, a correction could
   have silently re-pointed an Evidence record at a different Trading
   Account — an integrity defect. V2 requires the identical subject
   reference and rejects mismatched corrections.
2. **(Fixed)** V1 left "whether correction requires a reason" as an open
   Architecture question, but AGENTS.md's Audit standard and the Q-008/Q-010
   precedent both treat reason as a mandatory field for any lifecycle-
   changing operation. V2 makes correction reason mandatory
   (`Q011-FR-007`, `Q011-FR-012`).
3. **(Fixed)** V1's `Q011-FR-012` omitted "reason" from the set of fields an
   audit record must retain, inconsistent with AGENTS.md's own audit
   standard ("who, when, what, target, before/after, **reason**, source").
   V2 corrects this.
4. **(Fixed)** V1 defined a full-detail read contract for sensitive
   observation text but did not require that read itself to be audited,
   despite Q-008 Implementation Design §9.5 already establishing exactly
   this pattern (`RISK_CASE_VIEWED`) for equivalent sensitive content in
   this same repository. V2 adds `Q011-FR-014`.
5. **(Fixed)** V1's Section 14 named the subject-validation contract
   mismatch but understated it: reusing Q-010's
   `validateForNewRiskCaseAssociation` unchanged would require every
   Evidence-recording `HUMAN` actor to also hold a Trading-Account-module
   capability, an authorization-coupling defect beyond the semantic
   mismatch already noted. V2 sharpens this into an explicit two-part
   Architecture question.
6. **(Fixed, minor)** V1's Non-Goals did not exclude evidence
   polarity/support-vs-refute classification, leaving it ambiguous whether
   that was in scope. V2 adds it as an explicit non-goal.
7. **(Fixed, minor)** V1's `Q011-FR-002` did not clarify that "recognized"
   is a lower bar than Q-010's "eligible for new association," risking an
   implementer defaulting to the stricter check. V2 clarifies.

No finding required reopening Section 5 scope boundaries (source =
`MANUAL` only, subject = `TRADING_ACCOUNT` only) or the two-tier
read-contract design; those held up under adversarial review.

**Verdict on V2: no further defect found in this pass.** This does not
substitute for Product Owner approval or, if the Product Owner later wants
one, a genuinely independent second opinion.

(That verdict was incomplete: a fourth, later governance-consistency round
found that Goal 5 — the Requirement's own goals section, not one of the
seven findings listed above — still overclaimed `HUMAN` for every
protected use case. See §19.)

## 19. Requirement Correction Record (V2 → V3, Fourth Governance Round)

Performed 2026-08-28 following an explicitly mechanical, pre-specified
governance-consistency task (`prompts/Q-011-V11-Fourth-Governance-Consistency-Correction-Prompt.md`)
that independently identified leftovers from the third correction round
across all four governing documents, not just this one. This document's
share of that finding:

**Finding:** Goal 5 (§4) read: "Apply Q-009 trusted ActorContext and
capability authorization to every protected use case, restricted to
`HUMAN` actors for this Foundation." Read literally, this requires `HUMAN`
for every protected use case, including the two read use cases. That
directly contradicts this same document's own `Q011-FR-005` ("Only an
authenticated actor of type `HUMAN` may record or correct Evidence" —
explicitly scoped to two use cases, not four) and the approved Architecture/
Design (§11.4), which correctly restrict `HUMAN` to recording and
correction only. This was present from V1 and was never caught by the V1→V2
self-review (§18) because that review checked internal consistency of the
sections it read closely, not a full cross-reference of every Goal against
every FR.

**Fixed:** Goal 5 rewritten to state the `HUMAN` restriction applies only
to the two authoring use cases, matching `Q011-FR-005` exactly.

**Not reopened:** no other Requirement content changed. The "recognized"
subject bar (`Q011-FR-002`), the two-tier read-contract design, and every
other approved FR/SR/AC are unchanged.

**Historical drafting status:** V3 was produced as a candidate and was not
self-approved by its author (Claude Code) — that would have violated the
same non-self-approval discipline this correction record itself relies on.
**It was subsequently APPROVED by the Product Owner on 2026-08-28**,
together with Architecture V4, ADR-013's amendment, and Implementation
Design V5 (fifth governance round; see
`docs/lessons/2026-08-28-q-011-fourth-governance-consistency-correction.md`
and the review package it references for why this sentence needed
correcting — round three's own lesson about status going stale applied
here too). §17/Status is the sole authoritative current gate; if this
sentence and §17 ever appear to disagree again, §17 governs.
