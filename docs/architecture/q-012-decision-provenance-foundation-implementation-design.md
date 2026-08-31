# Q-012 Decision Provenance Foundation Implementation Design

## Document Status

- Requirement: Q-012 — V1, APPROVED — 2026-08-31 — Product Owner
- Architecture: Q-012 — V1, APPROVED — 2026-08-31 — Product Owner (see
  that document's own Document Status for its current version — do not
  hard-code a version number here)
- ADR: ADR-014 — **Accepted — 2026-08-31 — Product Owner**
- Implementation Design submission: **V1 — DRAFT, not yet approved**
- Prepared by: Claude Code, holding the external Architect review role by
  explicit Product Owner direction. Self-review artifact, not an
  independent one — disclosed for the same reason every prior design in
  this repository discloses it.
- Implementation: NOT AUTHORIZED.

This document turns approved Architecture V1 and accepted ADR-014 into
exact Java/SQL/HTTP/transaction/test mechanics, without reopening any
Requirement, Architecture, or ADR decision. Approval creates no code —
implementation authorization remains a separate, later decision, per
`docs/engineering/AI-Engineering-Execution-Protocol.md` §2/§3/§12.

## 1. Authority, Scope, and Non-Goals

Authoritative, in order: Q-012 Requirement V1, Q-012 Architecture V1,
ADR-014, this repository's development standards, and the actual
committed Q-009/Q-010/Q-011 code (not their design documents) for every
reused contract.

This design does not reopen: Decision immutability (no correction/
supersede); `MANUAL`-only source; `HUMAN`-only recording; the two-tier
read split; the absence of an eligibility service; or the no-hard-FK
cross-module reference pattern. Those are Requirement/Architecture/ADR
decisions.

Non-goals, restated from Requirement §5.2 so this document is
self-contained for an implementer: no Rule Engine, no `AUTOMATED` source,
no outcome taxonomy/confidence/severity, no correction, no Q-008 code, no
Action/ActionOutcome/Alert/Rule Hit concept, no delete use case.

## 2. Design Outcome Summary

| Area | Design decision |
| --- | --- |
| Package | `com.brokeros.risk.decision`, mirroring `evidence`'s internal layout exactly (domain / application / application.port / infrastructure.persistence / infrastructure.configuration / infrastructure.observability / interfaces.rest) |
| Migration | next additive version after the highest currently committed (confirm exact number at implementation time — do not assume V5, since other work may land first) |
| Tables | `decision_record`, `decision_evidence_reference`, `decision_operation`, `decision_access_log` — four tables, no `*_history` table (§8) |
| HTTP surface | `POST /api/decisions` (record), `GET /api/decisions/{decisionRef}` (full-detail read) — two routes, not three (no correction route) |
| In-process contract | `confirmProvenance(ActorContext, DecisionRef)` → `DecisionProvenanceView`, not HTTP-exposed |
| Capabilities | `decision:record` (HUMAN), `decision:read` (any ActorType) |
| ResultCodes | ten new codes (§13) |

## 3. Module and Package Placement

```
com.brokeros.risk.decision
├── domain
│   ├── DecisionRef, TradingAccountRef (reused from tradingaccount package — see below), EvidenceRef (reused from evidence package)
│   ├── ConclusionText (value object, mirrors ObservationText)
│   ├── DecisionSource (enum: MANUAL)
│   ├── DecisionOperationId, DecisionSemanticFingerprint
│   ├── DecisionProvenanceView (narrow read type)
│   └── DecisionRecord, CompletedDecisionOperation
├── application
│   ├── DecisionRecordingService
│   ├── DecisionProvenanceQueryService (narrow read)
│   ├── DecisionDetailReadService (full-detail read)
│   ├── AuthorizedMutationContext, AuthorizedMutationFactory (decision-scoped; not a reuse of tradingaccount's class of the same name — same pattern, separate type, matching how evidence has its own AuthorizedMutationContext rather than importing tradingaccount's)
│   └── DecisionCapabilities (capability string constants)
├── application.port
│   ├── DecisionMutationPort, DecisionQueryPort, DecisionAccessLogPort
├── infrastructure.persistence
│   ├── JdbcDecisionMutationAdapter, JdbcDecisionQueryAdapter, JdbcDecisionAccessLogAdapter
├── infrastructure.configuration
│   └── DecisionModuleConfiguration
├── infrastructure.observability
│   └── DecisionMetrics
└── interfaces.rest
    └── DecisionController, request/response DTOs
```

`DecisionRecord` importing `TradingAccountRef` from
`com.brokeros.risk.tradingaccount.domain` and `EvidenceRef` from
`com.brokeros.risk.evidence.domain` is a read-only type dependency in the
same direction Q-011 already established toward Q-010 (`EvidenceRecord`
imports `TradingAccountRef`) — not a new coupling pattern.

## 4. Domain Types and Invariants

- `DecisionRef` — value object, `dec-<canonical-lowercase-UUIDv4>`,
  parses/validates exactly like `EvidenceRef`.
- `ConclusionText` — value object, UTF-8 byte length 1–4,000 (same bound
  as `ObservationText`), rejects blank-after-trim, NUL, and control
  characters, matching Q-011's exact validation approach.
- `DecisionSource` — enum, exactly one value: `MANUAL`.
- `DecisionRecord` — immutable record: `decisionRef`, `subjectRef`
  (`TradingAccountRef`), `evidenceRefs` (`Set<EvidenceRef>`, non-empty),
  `conclusionText`, `source` (`MANUAL`), `recordedByActorRef`,
  `recordedAt`. No status field — a `DecisionRecord` either exists or it
  does not; there is no third state.
- `DecisionProvenanceView` — the narrow read type. Compact constructor
  invariant mirrors `EvidenceProvenanceView` exactly: a `RECOGNIZED`
  outcome requires all of `subjectRef`/`evidenceRefs`/
  `recordedByActorRef`/`recordedAt` non-null; a `NOT_FOUND` outcome
  requires all of them null. **No `conclusionText` field exists on this
  type at all** — the same structural (not conventional) guarantee
  Q-011's equivalent type provides.
- `CompletedDecisionOperation` — `operationId`, `decisionRef`, `outcome`
  (`CREATED` — the only value; kept as an explicit field rather than a
  boolean for consistency with Q-011's shape, at negligible cost), the
  full `DecisionRecord`.

## 5. Q-009 Authorization Integration

Identical integration to Q-011 §5: `AuthorizationGuard.requireAllowed`
before any use case body executes. `DecisionCapabilities` defines exactly
`decision:record` and `decision:read` as `Capability` constants,
registered the same way `EvidenceCapabilities`/`TradingAccountCapabilities`
are — no Q-009 file changes.

## 6. Application Use Cases

### 6.1 `DecisionRecordingService.record(ActorContext, RecordDecisionSpec)`

The single mutating use case. Exact ordering (§11).

### 6.2 `DecisionProvenanceQueryService.confirmProvenance(ActorContext, DecisionRef)`

Narrow read: authorize (`decision:read`) → lookup → return
`DecisionProvenanceView` (`recognized()` or `notFound()` factory,
mirroring `EvidenceProvenanceView`). No `HUMAN` check.

### 6.3 `DecisionDetailReadService.readDetail(ActorContext, DecisionRef)`

Full-detail read: authorize (`decision:read`) → lookup; if found, commit
an access-log row (dedicated `REQUIRES_NEW`-style transaction) **before**
returning the full `DecisionRecord` including `conclusionText`; a failed
access-log write propagates and no content is returned. No `HUMAN` check.
Identical shape to `EvidenceDetailReadService`.

## 7. Application Ports and Ownership

- `DecisionMutationPort.record(RecordDecisionSpec, AuthorizedMutationContext)`
  → `CompletedDecisionOperation`. One method — there is no
  `correct(...)` sibling.
- `DecisionQueryPort.findByRef(DecisionRef)` → `Optional<DecisionRecord>`;
  `DecisionQueryPort.findOperation(operationId)` → the replay lookup.
- `DecisionAccessLogPort.recordFullDetailAccess(DecisionRef, ActorRef,
  Instant)`.

## 8. Concrete Persistence Model

Four application-owned InnoDB tables, additive migration, next unused
version number (confirm at implementation time). All timestamps
server-derived UTC `DATETIME(6)`. All internal primary keys `BIGINT
AUTO_INCREMENT id`, never exposed. No cascade delete, no delete use case,
no money/customer data.

### 8.1 `decision_record`

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key, internal only |
| `decision_ref` | `CHAR(40)` ASCII `ascii_bin` | not null | unique canonical `dec-<UUIDv4>` |
| `subject_ref` | `CHAR(39)` ASCII `ascii_bin` | not null | Q-010 `TradingAccountRef`, no local FK (Architecture §8) |
| `source` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `MANUAL` check, extensible |
| `conclusion_text` | `VARBINARY(4000)` | not null | exact UTF-8 bytes, immutable |
| `recorded_by_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 UUIDv4 ActorRef |
| `recorded_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes: PK `pk_decision_record(id)`; unique
`uk_decision_record_ref(decision_ref)`; checks for `decision_ref` shape
(`dec-` + UUIDv4), `recorded_by_actor_ref` UUIDv4 shape, `source IN
('MANUAL')`, `OCTET_LENGTH(conclusion_text) BETWEEN 1 AND 4000`; index
`idx_decision_record_subject(subject_ref)`.

No status column and no self-referencing FK exist on this table — unlike
`evidence_record`, there is nothing to supersede.

### 8.2 `decision_evidence_reference`

The one-to-many Decision-to-Evidence join table (Requirement
`Q012-FR-004`).

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `decision_id` | `BIGINT` | not null | FK to `decision_record.id`, intra-module |
| `evidence_ref` | `CHAR(39)` ASCII `ascii_bin` | not null | Q-011 `EvidenceRef`, no cross-module FK (Architecture §8/ADR-014) |
| `created_at` | `DATETIME(6)` | not null | same instant as the owning `decision_record.recorded_at` |

Constraints/indexes: PK `pk_decision_evidence_reference(id)`; FK
`fk_decision_evidence_reference_decision(decision_id)` →
`decision_record.id`, `ON DELETE RESTRICT`; unique
`uk_decision_evidence_reference(decision_id, evidence_ref)` — the same
`EvidenceRef` cannot be listed twice under one Decision; check on
`evidence_ref` shape (`ev-` + UUIDv4); index
`idx_decision_evidence_reference_decision(decision_id)`.

### 8.3 `decision_operation`

Durable idempotency outcome and replay source, mirroring
`evidence_operation`'s shape.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | globally unique UUIDv4 idempotency key |
| `operation_type` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `RECORD` — the only value; kept as an explicit column for shape-consistency with Q-010/Q-011's ledger tables and negligible-cost future extensibility, not because more than one value exists today |
| `semantic_fingerprint` | `BINARY(32)` | not null | SHA-256 over subject + sorted de-duplicated evidence-ref set + conclusion text |
| `decision_id` | `BIGINT` | not null | FK to `decision_record.id` — the record this operation produced. Unlike `evidence_operation.evidence_id`, there is no target-vs-resulting ambiguity to resolve here: `RECORD` is the only operation type, so this column always means "the record this operation created" |
| `outcome` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `CREATED` — the only value |
| `occurred_at` | `DATETIME(6)` | not null | server UTC |

Constraints/indexes: unique `uk_decision_operation_id(operation_id)`; FK
`fk_decision_operation_record(decision_id)` → `decision_record.id`, `ON
DELETE RESTRICT`; checks for `operation_id` UUIDv4 shape,
`operation_type IN ('RECORD')`, `outcome IN ('CREATED')`.

### 8.4 `decision_access_log`

Append-only, satisfying `Q012-FR-008`. Not joined into the recording
transaction.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `decision_id` | `BIGINT` | not null | FK to `decision_record.id`, delete restricted |
| `accessing_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 actor |
| `accessed_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes: FK `fk_decision_access_log_record(decision_id)` →
`decision_record.id`, `ON DELETE RESTRICT`; index
`idx_decision_access_log_record(decision_id, accessed_at)`.

No table in this schema has any update or delete API. No
`decision_operation_history` table exists (ADR-014, "Alternative 5" —
there is no correction to have history of).

### 8.5 Constraint-to-test traceability

Built by walking every column of every table in §8.1–§8.4 in order, per
the exact lesson Q-011's own governance history recorded about this
table's purpose (Q-011 Implementation Design §8.5).

**`decision_record` (§8.1):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `decision_ref` globally unique | `UNIQUE (decision_ref)` | §16.4 unique-index test |
| `decision_ref` canonical `dec-<UUIDv4>` shape | `CHECK` on ref format | §16.1 boundary/format test |
| `recorded_by_actor_ref` canonical UUIDv4 shape | `CHECK` on ref format | §16.1 boundary/format test |
| `source` is an allowed value (`MANUAL`) | `CHECK (source IN ('MANUAL'))` | §16.1 enum test |
| `conclusion_text` is 1–4,000 bytes | `CHECK (OCTET_LENGTH(conclusion_text) BETWEEN 1 AND 4000)` | §16.1 boundary test |

**`decision_evidence_reference` (§8.2):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `decision_id` references only an existing Decision | FK `fk_decision_evidence_reference_decision` → `decision_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |
| Same `EvidenceRef` cannot be listed twice under one Decision | `UNIQUE (decision_id, evidence_ref)` | §16.2 application test + §16.4 unique-index test |
| `evidence_ref` canonical `ev-<UUIDv4>` shape | `CHECK` on ref format | §16.1 boundary/format test |

**`decision_operation` (§8.3):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `operation_id` globally unique (idempotency key) | `UNIQUE (operation_id)` | §16.4 unique-index test |
| `operation_id` canonical UUIDv4 shape | `CHECK` on ref format | §16.1 boundary/format test |
| `operation_type` is exactly `RECORD` | `CHECK (operation_type IN ('RECORD'))` | §16.1 enum test |
| `outcome` is exactly `CREATED` | `CHECK (outcome IN ('CREATED'))` | §16.1 enum test |
| `decision_id` references only an existing record | FK `fk_decision_operation_record` → `decision_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

**`decision_access_log` (§8.4):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| Access-log entries cannot outlive their Decision | FK `fk_decision_access_log_record` → `decision_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

## 9. Why Recording Validates Both Q-010 and Q-011, and Why There Is No Correction Path to Avoid Re-Calling Them

Q-011 has a dedicated section explaining why *correction* must not re-call
Q-010 (only recording does). Q-012 has no correction path at all, so this
class of question does not arise — every Decision, having exactly one
possible mutation (creation), always validates its subject via Q-010 and
its evidence via Q-011 exactly once, on the only path that exists. There
is no second, cheaper path that must deliberately avoid re-validating.

## 10. HTTP API Contract

- `POST /api/decisions` — record. Request DTO: `subjectRef` (string),
  `evidenceRefs` (non-empty string array), `conclusionText` (string),
  `operationId` (string, client-supplied idempotency key — matches
  Q-011's pattern of accepting a client-generated operation id, not a
  server-generated one, so a client can safely retry). Actor from
  `ActorContextProvider.currentContext()` only — never from the request
  body. No `decisionRef`, `recordedByActorRef`, `recordedAt`, or
  `source` field is accepted from the client (`source` is always
  `MANUAL` for this Requirement; accepting it as input would let a
  client claim `AUTOMATED` before that value is ever authorized).
  Response: `ApiResponse<DecisionRecordedResponse>` (`decisionRef`,
  `subjectRef`, `evidenceRefs`, `recordedByActorRef`, `recordedAt`,
  `outcome`) — never `conclusionText` in the *recording* response either,
  matching the narrow-disclosure default; a caller who needs the
  conclusion text calls the full-detail read separately, so that access
  is always logged.
- `GET /api/decisions/{decisionRef}` — full-detail read. Response
  includes `conclusionText`. 404-shaped `DECISION_NOT_FOUND` on a
  not-found ref.
- No `PATCH`/`PUT` route exists.
- `confirmProvenance` is never exposed over HTTP (Requirement
  `Q012-FR-009`).

Bean Validation at the controller boundary (`@Valid @RequestBody`),
matching Q-011's pattern; domain-level `ConclusionText`/`EvidenceRef`
parsing is the authoritative validation, HTTP-layer annotations are a
fast-fail convenience only.

## 11. Idempotency, Duplicate, and Retry Design

### 11.1 Canonical execution order (single authoritative statement)

This is the one place this design states the recording order; every other
section must defer to it, not restate it independently — the exact
discipline Q-011's Implementation Design §11.1/§11.4 established after
several governance rounds proved how easily restatements drift.

1. `authorizationGuard.requireAllowed(actorContext, DecisionCapabilities.RECORD)`.
2. `requireHuman(actorContext)`.
3. Compute `semanticFingerprint` from the **raw** request fields (subject
   string, evidence-ref strings, conclusion string) — before any parsing
   into domain value objects, matching Q-011's exact reasoning (a replay
   must be detected even if the raw content would fail domain-level
   parsing, so a client retrying a byte-identical request always gets the
   original outcome or a clean conflict, never a parsing exception on
   the second attempt).
4. `queryPort.findOperation(operationId)` — the replay check. If found:
   compare `semanticFingerprint`; identical → return the original
   `CompletedDecisionOperation` (`replay()`), no new write; different →
   reject `DECISION_IDEMPOTENCY_CONFLICT`. **This step precedes every
   step below** — content validation and both external calls never run
   on a replay.
5. (New operation only) Content validation: parse `ConclusionText`; parse
   and de-duplicate the evidence-ref set; reject an empty set with
   `DECISION_CONTENT_INVALID` before any external call.
6. `eligibilityService.validateForNewRiskCaseAssociation(actorContext,
   subjectRef)` (Q-010, actor's own context) — reject only
   `NOT_RECOGNIZED` with `DECISION_SUBJECT_NOT_RECOGNIZED`; map an
   unavailability exception to `DECISION_SUBJECT_AUTHORITY_UNAVAILABLE`.
7. For each distinct evidence reference (sorted, de-duplicated):
   `evidenceQueryService.confirmProvenance(actorContext, evidenceRef)`
   (Q-011, actor's own context) — reject a `NOT_FOUND` outcome with
   `DECISION_EVIDENCE_NOT_RECOGNIZED`; map an unavailability exception to
   `DECISION_EVIDENCE_AUTHORITY_UNAVAILABLE`. Accept `RECOGNIZED`
   regardless of the underlying Evidence's `ACTIVE`/`SUPERSEDED` status.
8. Build `AuthorizedMutationContext` (fingerprint, actorContext,
   authorizationDecision, capability, `clock.instant()` — reusing the
   single shared `Clock` bean, already microsecond-truncated at its
   source per the shared-Clock fix landed for Q-009/Q-010/Q-011).
9. `mutationPort.record(new RecordDecisionSpec(operationId, subjectRef,
   evidenceRefSet, conclusionText), context)` — inserts `decision_record`,
   all `decision_evidence_reference` rows, and the `decision_operation`
   row atomically.

### 11.2 Generated-reference collision handling

`decisionRef` generation retries on the named unique-constraint violation
only, at most three attempts, matching Q-010/Q-011's exact pattern — never
overwrites, never retries on an unrelated constraint violation.

## 12. Transaction and Concurrency Design

- Recording: one transaction, per §11.1 step 9.
- Two concurrent requests with the same `operationId` and same
  fingerprint: the database unique constraint on `decision_operation
  .operation_id` elects exactly one committer; the other observes the
  committed row on retry and replays. No precheck-then-insert race.
- Full-detail read's access-log write: a short, dedicated transaction
  isolated from the read and from any concurrent, unrelated recording
  transaction — mirrors Q-011 exactly.
- There is no concurrent-correction scenario to design for (ADR-014,
  "Consequences") — an entire class of test and code Q-011 needed
  (electing a winner between two competing corrections of the same
  target) does not exist for Q-012.

## 13. Error, ResultCode, and Exception Model

Ten new `ResultCode` values, additive only, no change to existing
entries:

| ResultCode | Meaning |
| --- | --- |
| `DECISION_REQUEST_INVALID` | malformed request shape (Bean Validation failure) |
| `DECISION_CONTENT_INVALID` | blank/oversized conclusion, or empty/malformed evidence-ref set |
| `DECISION_SUBJECT_NOT_RECOGNIZED` | Q-010 `NOT_RECOGNIZED` |
| `DECISION_SUBJECT_AUTHORITY_UNAVAILABLE` | Q-010 call failed |
| `DECISION_EVIDENCE_NOT_RECOGNIZED` | Q-011 `NOT_FOUND` for a referenced `EvidenceRef` |
| `DECISION_EVIDENCE_AUTHORITY_UNAVAILABLE` | Q-011 call failed |
| `DECISION_IDEMPOTENCY_CONFLICT` | same `operationId`, different fingerprint |
| `DECISION_NOT_FOUND` | full-detail read on an unknown `DecisionRef` |
| `DECISION_ACTOR_TYPE_NOT_PERMITTED` | non-`HUMAN` actor attempted to record |
| `DECISION_AUTHORITY_UNAVAILABLE` | unclassified persistence/database failure (fail-closed default) |

All mapped through the existing shared `GlobalExceptionHandler`; no SQL or
stack trace in any API response.

## 14. Logging, Sensitive Data, and Observability

No `conclusionText`, `subjectRef`, `evidenceRefs`, or actor identity in
any log line or metric tag. `DecisionMetrics` uses only bounded
operation/outcome/capability/ResultCode tags, matching
`EvidenceMetrics` exactly.

## 15. Security Design Review

Identical shape and reasoning to Q-011 §15/ADR-013's Security
Implications, restated for Decision's two capabilities and the absence of
a correction path (which removes an entire class of Q-011's security
surface: subject-substitution-during-correction cannot occur here because
there is no correction).

## 16. Test Design

### 16.1 Domain unit tests

- `DecisionRef`/`ConclusionText` boundary rejection (0, 4000, 4001 bytes;
  blank-after-trim; control/NUL characters);
- fingerprint golden vectors and one-field change sensitivity (subject,
  each evidence ref, conclusion text each independently change the
  fingerprint).

### 16.2 Application-service tests

- every use case invokes `AuthorizationGuard` before any Decision port;
- recording additionally verifies `ActorType == HUMAN` before content
  validation and before any Q-010/Q-011 call;
- Q-010 and Q-011 validation are invoked with the recording actor's own
  `ActorContext` (mock argument capture);
- only Q-010 `NOT_RECOGNIZED` rejects recording; `RECOGNIZED_NOT_ELIGIBLE`
  is accepted (explicit assertion, matching the exact bar Q011-FR-003
  requires and Q-011's own tests already proved for Evidence);
- only Q-011 `NOT_FOUND` rejects recording; both `ACTIVE` and
  `SUPERSEDED` referenced Evidence are accepted (explicit assertion for
  each);
- an empty evidence-ref set is rejected before any Q-010/Q-011 call
  (zero-interaction assertion on both mocks);
- a duplicate evidence-ref within one request is de-duplicated, not
  rejected and not double-validated (single-interaction assertion per
  distinct ref);
- exact replay and conflicting replay outcomes;
- full-detail read commits the access-log write before returning content;
  a forced access-log failure returns no content.

### 16.3 Q-009/Q-010/Q-011 integration tests

- denial/unavailability of `decision:*` capabilities yields zero Decision
  data access;
- denial/unavailability of `trading-account-reference:read` during
  recording yields zero Decision creation;
- denial/unavailability of `evidence:read` during recording yields zero
  Decision creation;
- `SERVICE`-actor context with `decision:record` granted is still
  rejected by the `HUMAN`-only check.

### 16.4 Real MySQL 8.4 migration/persistence tests

Disposable MySQL 8.4.11, mandatory test datasource inputs, no mandatory
gate skipped. Verify:

- clean upgrade from the current baseline through the new migration,
  Flyway validate/restart/checksum;
- exactly four new tables, no data seed, no destructive DDL;
- every row of §8.5's table, table by table;
- `decision_evidence_reference` uniqueness: the same
  (`decision_id`, `evidence_ref`) pair cannot be inserted twice;
- both FK-restrict behaviors (`decision_evidence_reference.decision_id`,
  `decision_operation.decision_id`, `decision_access_log.decision_id` →
  `decision_record.id`);
- query plans use unique/index paths, no full scans.

### 16.5 Transaction and concurrency tests

- concurrent same-`operationId`/same-fingerprint requests: exactly one
  commit, one replay;
- forced generated-`decisionRef` collision retries at most three times,
  never overwrites;
- install a disposable test-only trigger that fails
  `decision_evidence_reference` insert, then prove `decision_record` and
  `decision_operation` roll back together; remove the trigger afterward;
- a forced `decision_access_log` failure during a full-detail read
  returns no content and does not affect a concurrent, unrelated
  recording transaction.

### 16.6 Q-008 consumer/security tests

- `DecisionProvenanceView` contains no `conclusionText` field by
  static/reflective inspection;
- unauthorized/missing/revoked `decision:read` calls no Decision query;
- no Q-012 repository/table import exists anywhere Q-008 will eventually
  live (there is no Q-008 code yet to test this against, so this is a
  forward-looking assertion recorded here for the future Q-008
  implementer, mirroring how Q-011 recorded the same expectation before
  Q-008 existed).

### 16.7 Regression and architecture tests

- existing Q-009/Q-010/Q-011 tests continue passing unchanged;
- package dependency test prohibits infrastructure/framework imports from
  `com.brokeros.risk.decision.domain`/`application`;
- static scan proves no delete SQL, no edits to any existing migration,
  no permissive provider, no raw content logging;
- Maven dependency tree remains unchanged.

## 17. Flyway and Rollout Plan

Single additive migration, next unused version number confirmed at
implementation time (do not assume a specific number now — the same
lesson Q-011's own §1.1 correction recorded about not hard-coding
sibling-document version numbers applies equally to migration version
numbers guessed before implementation). No existing migration modified.

## 18. Recommended Future Implementation Sequence

1. Domain types (§4).
2. Migration + §8.5 constraint tests, verified against real MySQL before
   any application code is written (matching Q-011's own proven
   sequence).
3. Ports and JDBC adapters.
4. Application services, in the exact §11.1 order.
5. REST controller and DTOs.
6. Full test suite (§16), including the mandatory real-MySQL gate with
   Q-009/Q-010/Q-011/Q-012 all enabled.

## 19. Requirement and Acceptance Traceability

| Requirement/AC | Design section |
| --- | --- |
| Q012-FR-001–002 | §6.1, §11.1, §12 |
| Q012-FR-003 | §11.1 step 6 |
| Q012-FR-004 | §11.1 steps 5/7, §8.2 |
| Q012-FR-005 | §11.1 step 4, §12 |
| Q012-FR-006 | §8 (no status/history table), §9 |
| Q012-FR-007 | §4 (`DecisionProvenanceView`), §6.2 |
| Q012-FR-008 | §6.3, §12 |
| Q012-FR-009 | §10 |
| AC 1–11 (Requirement §10) | §4, §8, §9, §11, §16 collectively |

## 20. Design Gaps and Outstanding Decisions

None identified that require a Product Owner decision before
implementation — every open question Architecture §21/§22 deferred to
this stage has been resolved above (migration version number is the one
genuinely implementation-time fact, correctly left unresolved here rather
than guessed).

## 21. Design Gate

- Implementation Design submission complete: YES (V1)
- Implementation Design V1 approved: **YES — 2026-08-31 — Product Owner.**
  Gate Decision: **PASS.**
- Implementation: **AUTHORIZED — 2026-08-31 — Product Owner**, against
  Requirement V1 / Architecture V1 / ADR-014 (Accepted) / this
  Implementation Design V1.
- Implementation Allowed: **YES**

Next gate: Codex executes the implementation Prompt built strictly from
this document's §11.1/§8.5, then Claude Code performs an independent
implementation review (including independently executed tests, per this
project's established practice) before any commit.
