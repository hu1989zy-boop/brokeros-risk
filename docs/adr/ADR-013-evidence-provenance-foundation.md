# ADR-013: Evidence Provenance Foundation

- Status: **Accepted** (original decision, 2026-08-28) — **Amendment
  RE-ACCEPTED — 2026-08-28 — Product Owner.** The original acceptance and
  this amendment's re-acceptance are both explicit Product Owner decisions,
  recorded separately per this document's own governance discipline. This
  repository has no prior ADR-amendment precedent; this record establishes
  one: an amendment is drafted, marked pending, and only governs once
  separately re-accepted — never inherited from the original acceptance.
- Date: 2026-08-28 (original); amendment drafted and re-accepted 2026-08-28
- Approval origin: explicit Product Owner decision, 2026-08-28 (chat
  confirmation, "批准"), covering both the original content and this
  amendment. Prepared by Claude Code, holding the external Architect
  review role by explicit Product Owner direction; this was a self-review
  artifact (see
  `docs/requirements/Q-011-Evidence-Provenance-Foundation.md` §18), not an
  independent one — disclosed here for the same reason.
- Requirement: Q-011 — Evidence Provenance Foundation V3, APPROVED —
  2026-08-28 — Product Owner
- Architecture: `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
  (see its own Document Status for the current version — do not hard-code a
  version number here, which is what caused part of this amendment)
- Depends on: ADR-009, ADR-010, ADR-011, ADR-012
- Supersedes: None

This ADR's original content was accepted by explicit Product Owner decision
on 2026-08-28, consistent with every prior ADR in this repository requiring
a decision from someone other than the document's drafter, disclosed here
as a self-review rather than an independent one. **That original content
contained a defect: it required Evidence's subject to be
`ELIGIBLE_FOR_NEW_ASSOCIATION`, which directly contradicts the separately
approved Requirement `Q011-FR-002` (subject need only be Q-010-"recognized",
a strictly lower bar). This was never corrected here even after Architecture
and Implementation Design were corrected in a later round — an accepted ADR
does not become correct automatically when its Requirement/Architecture
inputs are later fixed; it must be explicitly amended.** See "Amendment"
below for the complete correction. That amendment was drafted pending
re-acceptance and was **subsequently RE-ACCEPTED by the Product Owner on
2026-08-28** (fifth governance round) — the current governing status is
Accepted as amended; see this document's own Status line above and
"Approval Boundary" below, which are authoritative if this sentence ever
appears to disagree with them again.

## Context

Q-008 requires an authoritative Evidence existence/provenance provider, but
the repository has no Evidence identity, content model, or query contract.
Q-007/ADR-009 define Evidence semantically (traceable support for a risk
conclusion, with preserved provenance) but authorize no runtime
implementation. Q-009 has implemented trusted Actor/authorization. Q-010 has
implemented a Trading Account reference authority that Evidence can be
scoped to.

Q-011 Requirement V2 (approved 2026-08-28) resolves the business boundary:
Evidence Source limited to `MANUAL`, Evidence Subject limited to
`TRADING_ACCOUNT`, recording/correction limited to `HUMAN` actors,
immutable content with reasoned subject-preserving correction, and a
two-tier read-contract design. Architecture must decide identity/content
representation, how Evidence validates its subject against Q-010 without
reopening Q-010's already-shipped code, durable storage, and the exact
consumer boundary.

## Decision

### Bounded capability and identity ownership

Create a logical Evidence Provenance Foundation capability inside the
existing Phase 1 modular monolith. It owns stable `EvidenceRef` identity,
immutable observation content, the `ACTIVE`/`SUPERSEDED` status model, and
protected recording, correction, and read use cases.

It owns no Decision, Rule Engine, Trading Data, customer data, or Trading
Account identity, and it creates no Risk Case, Action, or ActionOutcome
behavior.

### Opaque reference

Use a server-generated prefixed canonical lowercase UUIDv4 business
reference, consistent with Q-010's established convention:

- `ev-<uuidv4>` for `EvidenceRef`.

It is separate from the internal `BIGINT id`, immutable, non-sequential, and
contains no subject/actor/time semantics.

### Content model

Evidence content is a bounded structured record: observation/summary text
(≤4,000 bytes UTF-8), source (fixed `MANUAL`), subject reference
(`TradingAccountRef`), recording `ActorRef`, and UTC recorded-at time. No
binary, file, or document storage. Content is immutable once recorded;
correction never edits it in place.

### Correction and status

Correction is a named operation that creates a new Evidence record
referencing the record it corrects, carrying the identical subject
reference, and carrying a mandatory bounded reason (≤1,000 bytes). It sets
the corrected record's status to `SUPERSEDED` atomically with the new
record's creation. One record may be superseded by at most one replacement;
a second correction of an already-superseded record is rejected. Superseded
records are never deleted and remain permanently queryable.

### Subject validation without reopening Q-010 (AMENDED)

Evidence validates its subject by calling Q-010's existing, already-shipped
`validateForNewRiskCaseAssociation(ActorContext, TradingAccountRef)`
contract unchanged, using the recording actor's own `ActorContext`. That
contract already returns one of three outcomes —
`ELIGIBLE_FOR_NEW_ASSOCIATION`, `RECOGNIZED_NOT_ELIGIBLE`, or
`NOT_RECOGNIZED`. Two consequences follow, both accepted deliberately:

1. An Evidence-recording `HUMAN` actor must also hold Q-010's
   `trading-account-reference:read` capability. This is treated as a
   normal, explicit, least-privilege grant under Q-009's capability model —
   the same pattern Q-008's own analysts will need for the identical
   reason — not a defect requiring a new cross-module contract.
2. **Q-011 accepts both `ELIGIBLE_FOR_NEW_ASSOCIATION` and
   `RECOGNIZED_NOT_ELIGIBLE` as valid Evidence subjects — only
   `NOT_RECOGNIZED` rejects the request.** This matches Requirement
   `Q011-FR-002` exactly, which requires only that the subject be
   Q-010-recognized, a strictly lower bar than "eligible for a new Risk
   Case association." Recording Evidence about an inactive/retired
   (`RECOGNIZED_NOT_ELIGIBLE`) account is therefore in scope now, not a
   future Requirement. (Original decision, superseded by this amendment:
   the original text of this ADR required `ELIGIBLE_FOR_NEW_ASSOCIATION`
   only, silently narrowing the approved Requirement — a defect, not a
   scope choice this ADR was entitled to make. Corrected here.)

This keeps Q-011 from modifying Q-010's already-approved, already-shipped
code or capability surface at all — the fix required only accepting a
second already-returned outcome, not adding a new one.

### HTTP exposure

Unlike Q-010's non-web-only registration (justified there by low-frequency,
externally attested account mapping), Evidence recording, correction, and
full-detail reading are exposed through protected authenticated HTTP
endpoints, because they are routine operational actions performed by
authorized staff. The narrow existence/provenance contract consumed by
Q-008 remains an in-process application call, mirroring exactly how Q-008
consumes Q-010's equivalent contract today.

### Durable authority and atomic history

Application-owned MySQL/InnoDB is the durable source of truth, accessed
through the existing Spring JDBC/local transaction manager and changed only
through a future additive Flyway migration. Current record state, durable
idempotency outcome, and immutable mutation history commit in one local
transaction; history failure rolls back state. A full-detail read commits
its own access-log row before returning content and fails closed if that
write fails, mirroring the `RISK_CASE_VIEWED` pattern Q-008's Implementation
Design already established for equivalent sensitive-content reads.

Database constraints must enforce unique `EvidenceRef`, unique operation
identity, and the "at most one correction per target" invariant.

Redis, Kafka, event sourcing, a second database, and read-cache authority
are not selected, for the same reasons ADR-012 already gave for Q-010.

### Consumer boundary (AMENDED)

Q-008 and any future Decision capability may call only a protected,
in-process, read-only contract that confirms an `EvidenceRef` exists and
returns bounded provenance (subject reference, source, status, recorded
time, supersession pointer). It never returns observation text. A
separately capability-gated full-detail HTTP read exists for reviewing
complete Evidence content, including observation text.

**Two distinct rules, not one, govern access — this ADR previously
conflated them:**

1. **Consumer-contract limitation (unchanged):** Q-008, and any future
   automated capability, may consume only the narrow existence/provenance
   contract. It is never given the full-detail contract, regardless of
   which actor or actor type is driving it. This is a contract-design
   limitation, not an identity check.
2. **Actor-type limitation (corrected):** the full-detail HTTP read itself
   requires only `evidence:read` under Q-009 authorization. It does not
   require `ActorType.HUMAN`. Any authorized actor — `HUMAN` or `SERVICE`
   — may call it directly. (Original decision, superseded by this
   amendment: the original text described this as "direct human review...
   not available to Q-008 or any automated consumer," which read as an
   actor-type restriction on the read itself. There is no such restriction;
   `HUMAN` is required only for recording and correction, per Requirement
   `Q011-FR-005`. What is actually restricted is which *contract* Q-008 may
   call, not which *actor type* may call the full-detail one.)

The mandatory access-audit rule is unchanged: every full-detail read
commits an access-log row, regardless of actor type, before content is
returned.

## Alternatives Considered

### Extending Q-010 with a new "recognized regardless of eligibility" contract

Rejected — and unnecessary. It would require modifying already-shipped,
already-approved Q-010 code and adding a second Q-010 consumer contract.
No extension is needed at all: Q-010's existing
`validateForNewRiskCaseAssociation` already returns
`RECOGNIZED_NOT_ELIGIBLE` as a distinct outcome; Q-011 simply treats it as
acceptable instead of rejecting it, matching Requirement `Q011-FR-002`'s
"recognized" bar with zero Q-010 changes. (Original decision, superseded by
this amendment: the original text rejected this alternative by *also*
narrowing Q-011's own scope to "currently-eligible subjects," deferring
broader support to "a future Requirement." That narrowing was itself the
defect this amendment corrects — no future Requirement is needed, because
the existing contract already carries the information Q-011 needs.)

### A dedicated Q-011 SERVICE actor calling Q-010 on the recording actor's behalf

Rejected. It would introduce new Q-009 provisioning, a new indirection
layer, and a new internal trust boundary solely to avoid granting
`trading-account-reference:read` to Evidence-recording actors — a grant
that is otherwise a normal, explicit, least-privilege capability under
Q-009's model and that Q-008's own future analysts will need for the same
reason. The complexity was not justified by a real problem.

### Evidence owning its own copy/cache of recognized Trading Account references

Rejected. It would duplicate Q-010's authority, risk staleness, and violate
the same "no cache/event-only authority" principle ADR-012 already
established, extended here to cross-module (not just cross-system)
authority.

### Free-form Evidence content (blob/document/JSON bag)

Rejected. It would make Evidence an unstructured store, defeat bounded
provenance disclosure, and reopen exactly the "generic evidence bucket" risk
the Q-011 Requirement Candidate Analysis explicitly flagged.

### Single Evidence status field shared with Risk Case association disposition

Rejected. Q-008-Requirement.md §11 already assigns association-level
disposition (`SUPERSEDED`/`INVALIDATED`/`WITHDRAWN`) to Risk Case's own
bookkeeping. Collapsing it into Evidence's own status would let a single
case's judgment silently affect every other consumer of that Evidence
record, violating ADR-009's ownership boundaries.

### Non-web-only exposure (mirroring Q-010)

Rejected. Q-010's non-web justification (low-frequency, externally
attested, high-sensitivity account mapping) does not apply to routine
Evidence recording by authorized operational staff.

## Consequences

### Positive

- BrokerOS gains a real, immutable, provenance-preserving Evidence record
  without inventing Decision, Rule Engine, or trading-data ingestion.
- Q-011 reuses Q-009 and Q-010 entirely unchanged — no already-shipped code
  is modified, and no new cross-module trust boundary is introduced.
- Correction cannot silently change what subject an Evidence record
  concerns, and cannot branch into an ambiguous chain.
- Sensitive content access is itself audited, closing a gap the Requirement
  V1 draft had left open.
- No new framework, cache, messaging topology, service, or external
  dependency is introduced.

### Costs and constraints

- Evidence-recording actors must hold both `evidence:record` and
  `trading-account-reference:read` — an explicit two-capability grant
  operators must provision correctly.
- The full-detail-read access log is an open-ended append-only growth
  concern requiring future retention analysis, same as Q-010's operation
  history.
- Automated Evidence sources (Rule Engine, trading-data anomaly detection)
  remain entirely out of scope until a separate Requirement approves them.

## Security Implications

- Authorization occurs before lookup or content disclosure in every use
  case, limiting existence and content disclosure.
- Recording and correction are restricted to `HUMAN` actors at the
  application layer using the existing `ActorType` distinction; no new
  identity concept is introduced.
- Full observation text is never logged or placed in Audit JSON beyond
  bounded metadata.
- Subject validation reuses Q-010's existing fail-closed behavior verbatim;
  Q-011 adds no new failure mode to that dependency.

## Data and Integrity Implications

- MySQL/Flyway owns additive application schema only; no external database
  is read or modified — Q-011 has no external-system dependency at all.
- Evidence content is immutable; only status and its pointer fields change,
  exactly once, only through the correction operation.
- State, idempotency outcome, and mutation history share one transaction;
  no delete or cascade delete is permitted.
- Real disposable MySQL 8.4 verification is mandatory before implementation
  completion can be claimed, matching the standard already applied to
  Q-009/Q-010.

## Operational Implications

- Deployment must grant the correct capability pairs to Evidence-recording
  operators (`evidence:record`/`evidence:correct` plus
  `trading-account-reference:read`).
- Full-detail reads are audited; operators should expect the access log to
  grow proportionally to review activity, not just mutation activity.
- Recording/correction are exposed through normal authenticated HTTP,
  unlike Q-010's deployment-controlled non-web command.

## Dependencies

Q-011 reuses Java 21, Spring Boot, Spring JDBC, the local transaction
manager, MySQL, Flyway, and the implemented Q-009 `ActorContext`/
`AuthorizationGuard`/`Capability` contracts and Q-010's existing
`validateForNewRiskCaseAssociation` contract. It depends on ADR-009 domain
ownership, ADR-010 Q-008 consumer boundaries, ADR-011 trust semantics, and
ADR-012 Trading Account authority.

No new Maven dependency, framework, vendor SDK, service, database, Kafka
topic, Redis key, deployment object, or external integration is required.

## Deferred Decisions

Implementation Design will define exact Java/SQL/endpoint/transaction/
exception/query/test mechanics without reopening this decision.

A future approved Requirement is required for: automated Evidence sources
and any `SERVICE`-actor *authoring* (recording/correction) path — this does
not affect the two read use cases, which already permit any authorized
actor type; additional Evidence subject types; polarity/confidence/severity
classification; file/document attachment; multi-subject Evidence; and
retention/redaction policy beyond "no physical deletion." (Amendment: the
prior version of this list also included "Evidence about a recognized but
currently ineligible Trading Account" — removed, since that is now in
scope; see "Subject validation" above.)

## Approval Boundary

**Original decision:** ADR-013's original content was **Accepted** through
explicit Product Owner decision dated 2026-08-28. That historical fact is
preserved and not erased by this amendment.

**Amendment (this document, same date):** the "Subject validation" and
"Consumer boundary" sections above, plus the corresponding changes to
"Alternatives Considered," "Costs and constraints," and "Deferred
Decisions," are a correction to that original content — not a new,
separate decision — made necessary because the original content
contradicted the separately approved Requirement `Q011-FR-002`. **This
amendment was RE-ACCEPTED by explicit Product Owner decision on
2026-08-28**, separate from and in addition to the original acceptance.
This ADR's formal governance state is now **Accepted (original) / Amendment
Accepted (re-acceptance, 2026-08-28)** — fully accepted as amended.

No part of this ADR, original or amended, itself authorizes implementation,
dependency, migration, endpoint, or commit — Implementation Design approval
and a separate explicit implementation authorization remain required
before any code is written. Both have been separately granted as of
2026-08-28; see Implementation Design's own Document Status.
