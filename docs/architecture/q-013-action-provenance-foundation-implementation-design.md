# Q-013 Action Provenance Foundation Implementation Design

## Document Status

- Requirement: Q-013 — V1, APPROVED — 2026-08-31 — Product Owner
- Architecture: Q-013 — V1, APPROVED — 2026-09-01 — Product Owner (see
  that document's own Document Status for its current version)
- ADR: ADR-015 — **Accepted — 2026-09-01 — Product Owner**
- Implementation Design submission: **V1 — DRAFT, not yet approved**
- Prepared by: Claude Code, holding the external Architect review role by
  explicit Product Owner direction. Self-review artifact, not an
  independent one.
- Implementation: NOT AUTHORIZED.

This document turns approved Architecture V1 and accepted ADR-015 into
exact Java/SQL/HTTP/transaction/test mechanics, without reopening any
Requirement, Architecture, or ADR decision. Approval creates no code —
implementation authorization remains a separate, later decision, per
`docs/engineering/AI-Engineering-Execution-Protocol.md` §2/§3/§12.

## 1. Authority, Scope, and Non-Goals

Authoritative, in order: Q-013 Requirement V1, Q-013 Architecture V1,
ADR-015, this repository's development standards, and the actual
committed Q-009/Q-010/Q-011/Q-012 code for every reused contract.

This design does not reopen: exactly one originating `DecisionRef` (not a
set); `MANUAL`-only source; `PROPOSED`-only status with no transition;
Action immutability; the two-tier narrow/full-detail read split;
`HUMAN`-only recording; the absence of an eligibility service; or the
no-hard-FK cross-module reference pattern.

Non-goals, restated so an implementer does not need to re-derive them
from the Requirement: no approval-workflow transition, no ActionOutcome,
no Execution/Account Control adapter, no vendor-specific operation
taxonomy, no correction/deletion use case, no Q-008 code.

## 2. Design Outcome Summary

| Area | Design decision |
| --- | --- |
| Package | `com.brokeros.risk.action`, mirroring `decision`'s internal layout |
| Migration | next additive version after the highest currently committed (confirm exact number at implementation time) |
| Tables | `action_record`, `action_operation`, `action_access_log` — three tables, no join table (Action-to-Decision is one-to-one, unlike Decision-to-Evidence) |
| HTTP surface | `POST /api/actions` (record), `GET /api/actions/{actionRef}` (full-detail read) |
| In-process contract | `confirmProvenance(ActorContext, ActionRef)` → `ActionProvenanceView`, not HTTP-exposed |
| Capabilities | `action:record` (HUMAN), `action:read` (any ActorType) |
| ResultCodes | eight new codes (§13) |

## 3. Module and Package Placement

```
com.brokeros.risk.action
├── domain
│   ├── ActionRef
│   ├── IntentText (value object, mirrors ConclusionText)
│   ├── ActionSource (enum: MANUAL)
│   ├── ActionStatus (enum: PROPOSED)
│   ├── ActionOperationId, ActionSemanticFingerprint
│   ├── ActionProvenanceView (narrow read type)
│   └── ActionRecord, CompletedActionOperation
├── application
│   ├── ActionRecordingService
│   ├── ActionProvenanceQueryService (narrow read)
│   ├── ActionDetailReadService (full-detail read)
│   ├── AuthorizedMutationContext, AuthorizedMutationFactory (action-scoped, its own type — not a reuse of decision's/tradingaccount's same-named classes, matching the established per-module pattern)
│   └── ActionCapabilities (capability string constants)
├── application.port
│   ├── ActionMutationPort, ActionQueryPort, ActionAccessLogPort
├── infrastructure.persistence
│   ├── JdbcActionMutationAdapter, JdbcActionQueryAdapter, JdbcActionAccessLogAdapter
├── infrastructure.configuration
│   └── ActionModuleConfiguration
├── infrastructure.observability
│   └── ActionMetrics
└── interfaces.rest
    └── ActionController, request/response DTOs
```

`ActionRecord` importing `DecisionRef` from
`com.brokeros.risk.decision.domain` is a read-only type dependency in the
same direction Q-012 already established toward Q-011 (`DecisionRecord`
imports `EvidenceRef`).

## 4. Domain Types and Invariants

- `ActionRef` — value object, `act-<canonical-lowercase-UUIDv4>`, parses
  exactly like `DecisionRef`/`EvidenceRef`.
- `IntentText` — value object, UTF-8 byte length 1–4,000, rejects
  blank-after-trim, NUL, and control characters, matching
  `ConclusionText`'s exact validation approach.
- `ActionSource` — enum, exactly one value: `MANUAL`.
- `ActionStatus` — enum, exactly one value: `PROPOSED`.
- `ActionRecord` — immutable record: `actionRef`, `decisionRef` (single
  `DecisionRef`, not a set), `intentText`, `status` (`PROPOSED`),
  `source` (`MANUAL`), `recordedByActorRef`, `recordedAt`.
- `ActionProvenanceView` — the narrow read type. Compact constructor
  invariant mirrors `DecisionProvenanceView` exactly: a `RECOGNIZED`
  outcome requires `decisionRef`/`status`/`recordedByActorRef`/
  `recordedAt` all non-null; a `NOT_FOUND` outcome requires them all
  null. **No `intentText` field exists on this type at all.**
- `CompletedActionOperation` — `operationId`, `actionRef`, `outcome`
  (`CREATED`), the full `ActionRecord`.

## 5. Q-009 Authorization Integration

Identical integration to Q-011/Q-012 §5: `AuthorizationGuard.requireAllowed`
before any use case body executes. `ActionCapabilities` defines exactly
`action:record` and `action:read` — no Q-009 file changes.

## 6. Application Use Cases

### 6.1 `ActionRecordingService.record(ActorContext, RecordActionSpec)`

The single mutating use case. Exact ordering (§11).

### 6.2 `ActionProvenanceQueryService.confirmProvenance(ActorContext, ActionRef)`

Narrow read: authorize (`action:read`) → lookup → return
`ActionProvenanceView`. No `HUMAN` check.

### 6.3 `ActionDetailReadService.readDetail(ActorContext, ActionRef)`

Full-detail read: authorize (`action:read`) → lookup; if found, commit an
access-log row (`PROPAGATION_REQUIRES_NEW`, non-read-only transaction)
**before** returning the full `ActionRecord` including `intentText`; a
failed access-log write propagates and no content is returned. No
`HUMAN` check. Identical shape to `DecisionDetailReadService`.

## 7. Application Ports and Ownership

- `ActionMutationPort.record(RecordActionSpec, AuthorizedMutationContext)`
  → `CompletedActionOperation`. One method.
- `ActionQueryPort.findByRef(ActionRef)` → `Optional<ActionRecord>`;
  `ActionQueryPort.findOperation(operationId)` → the replay lookup.
- `ActionAccessLogPort.recordFullDetailAccess(ActionRef, ActorRef,
  Instant)`.

## 8. Concrete Persistence Model

Three application-owned InnoDB tables, additive migration, next unused
version number (confirm at implementation time). All timestamps
server-derived UTC `DATETIME(6)`. All internal primary keys `BIGINT
AUTO_INCREMENT id`, never exposed. No cascade delete, no delete use case.

### 8.1 `action_record`

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key, internal only |
| `action_ref` | `CHAR(40)` ASCII `ascii_bin` | not null | unique canonical `act-<UUIDv4>` |
| `decision_ref` | `CHAR(40)` ASCII `ascii_bin` | not null | Q-012 `DecisionRef`, no local FK (Architecture §7/ADR-015) |
| `source` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `MANUAL` check, extensible |
| `status` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `PROPOSED` check — single-value now, extensible by relaxation (Requirement §5.3) |
| `intent_text` | `VARBINARY(4000)` | not null | exact UTF-8 bytes, immutable |
| `recorded_by_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 UUIDv4 ActorRef |
| `recorded_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes: PK `pk_action_record(id)`; unique
`uk_action_record_ref(action_ref)`; checks for `action_ref` shape (`act-`
+ UUIDv4), `decision_ref` shape (`dec-` + UUIDv4),
`recorded_by_actor_ref` UUIDv4 shape, `source IN ('MANUAL')`, `status IN
('PROPOSED')`, `OCTET_LENGTH(intent_text) BETWEEN 1 AND 4000`; index
`idx_action_record_decision(decision_ref)`.

No self-referencing FK and no status-transition columns exist on this
table — there is nothing to supersede or transition.

### 8.2 `action_operation`

Durable idempotency outcome and replay source, mirroring
`decision_operation`'s shape.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | globally unique UUIDv4 idempotency key |
| `operation_type` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `RECORD` — the only value, kept for shape-consistency with Q-010–Q-012's ledger tables |
| `semantic_fingerprint` | `BINARY(32)` | not null | SHA-256 over the raw originating-Decision reference and intent-text strings |
| `action_id` | `BIGINT` | not null | FK to `action_record.id` — the record this operation produced |
| `outcome` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `CREATED` — the only value |
| `occurred_at` | `DATETIME(6)` | not null | server UTC |

Constraints/indexes: unique `uk_action_operation_id(operation_id)`; FK
`fk_action_operation_record(action_id)` → `action_record.id`, `ON DELETE
RESTRICT`; checks for `operation_id` UUIDv4 shape, `operation_type IN
('RECORD')`, `outcome IN ('CREATED')`.

### 8.3 `action_access_log`

Append-only, satisfying `Q013-FR-008`. Not joined into the recording
transaction.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `action_id` | `BIGINT` | not null | FK to `action_record.id`, delete restricted |
| `accessing_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 actor |
| `accessed_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes: FK `fk_action_access_log_record(action_id)` →
`action_record.id`, `ON DELETE RESTRICT`; index
`idx_action_access_log_record(action_id, accessed_at)`.

No table in this schema has any update or delete API. No join table and
no `*_history` table exist (Architecture §7, ADR-015).

### 8.4 Constraint-to-test traceability

Built by walking every column of every table in §8.1–§8.3 in order.

**`action_record` (§8.1):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `action_ref` globally unique | `UNIQUE (action_ref)` | §16.4 unique-index test |
| `action_ref` canonical `act-<UUIDv4>` shape | `CHECK` on ref format | §16.1 boundary/format test |
| `decision_ref` canonical `dec-<UUIDv4>` shape | `CHECK` on ref format | §16.1 boundary/format test |
| `recorded_by_actor_ref` canonical UUIDv4 shape | `CHECK` on ref format | §16.1 boundary/format test |
| `source` is an allowed value (`MANUAL`) | `CHECK (source IN ('MANUAL'))` | §16.1 enum test |
| `status` is an allowed value (`PROPOSED`) | `CHECK (status IN ('PROPOSED'))` | §16.1 enum test |
| `intent_text` is 1–4,000 bytes | `CHECK (OCTET_LENGTH(intent_text) BETWEEN 1 AND 4000)` | §16.1 boundary test |

**`action_operation` (§8.2):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `operation_id` globally unique (idempotency key) | `UNIQUE (operation_id)` | §16.4 unique-index test |
| `operation_id` canonical UUIDv4 shape | `CHECK` on ref format | §16.1 boundary/format test |
| `operation_type` is exactly `RECORD` | `CHECK (operation_type IN ('RECORD'))` | §16.1 enum test |
| `outcome` is exactly `CREATED` | `CHECK (outcome IN ('CREATED'))` | §16.1 enum test |
| `action_id` references only an existing record | FK `fk_action_operation_record` → `action_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

**`action_access_log` (§8.3):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| Access-log entries cannot outlive their Action | FK `fk_action_access_log_record` → `action_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

## 9. Why Action Validates Its Originating Decision Exactly Once, and Trusts It Transitively

Recording validates the originating `DecisionRef` via Q-012's narrow
provenance contract, but does not re-validate Decision's own subject or
evidentiary basis — those were already validated when the Decision was
recorded (Q-012's own §11.1), and Q-012's `DecisionRecord` is immutable,
so nothing about that validation can have changed since. Re-validating
transitively would duplicate work Q-012 already guarantees and would
create a hidden dependency on Q-010/Q-011 that this Requirement never
asked for.

## 10. HTTP API Contract

- `POST /api/actions` — record. Request DTO: `decisionRef` (string,
  singular — not an array), `intentText` (string), `operationId` (string,
  client-supplied idempotency key). Actor from
  `ActorContextProvider.currentContext()` only. No `actionRef`,
  `recordedByActorRef`, `recordedAt`, `source`, or `status` field is
  accepted from the client. Response:
  `ApiResponse<ActionRecordedResponse>` (`actionRef`, `decisionRef`,
  `status`, `recordedByActorRef`, `recordedAt`, `outcome`) — never
  `intentText`, matching the narrow-disclosure default even in the
  recording response.
- `GET /api/actions/{actionRef}` — full-detail read. Response includes
  `intentText`. 404-shaped `ACTION_NOT_FOUND` on a not-found ref.
- No `PATCH`/`PUT` route exists.
- `confirmProvenance` is never exposed over HTTP.

## 11. Idempotency, Duplicate, and Retry Design

### 11.1 Canonical execution order (single authoritative statement)

1. `authorizationGuard.requireAllowed(actorContext, ActionCapabilities.RECORD)`.
2. `requireHuman(actorContext)`.
3. Compute `semanticFingerprint` from the **raw** request fields
   (`decisionRef` string, `intentText` string) — before domain parsing.
4. `queryPort.findOperation(operationId)` — the replay check. Identical
   fingerprint → return the original `CompletedActionOperation`
   (`replay()`), no new write; different → reject
   `ACTION_IDEMPOTENCY_CONFLICT`. **This step precedes every step below.**
5. (New operation only) Content validation: parse `IntentText`; parse
   `DecisionRef`.
6. `decisionQueryService.confirmProvenance(actorContext, decisionRef)`
   (Q-012, actor's own context) — reject a `NOT_FOUND` outcome with
   `ACTION_DECISION_NOT_RECOGNIZED`; map an unavailability exception to
   `ACTION_DECISION_AUTHORITY_UNAVAILABLE`.
7. Build `AuthorizedMutationContext` (fingerprint, actorContext,
   authorizationDecision, capability, `clock.instant()` — reusing the
   single shared, already microsecond-truncated `Clock` bean).
8. `mutationPort.record(new RecordActionSpec(operationId, decisionRef,
   intentText), context)` — inserts `action_record` and the
   `action_operation` row atomically.

### 11.2 Generated-reference collision handling

`actionRef` generation retries on the named unique-constraint violation
only, at most three attempts, matching Q-010–Q-012's exact pattern.

## 12. Transaction and Concurrency Design

- Recording: one transaction, per §11.1 step 8.
- Two concurrent requests with the same `operationId` and same
  fingerprint: the database unique constraint on
  `action_operation.operation_id` elects exactly one committer; the other
  replays on retry.
- Full-detail read's access-log write: a short, dedicated transaction
  isolated from the read and from any concurrent, unrelated recording
  transaction.
- No concurrent-correction scenario exists (no correction to have one) —
  the same simplification Q-012 already realized relative to Q-011.

## 13. Error, ResultCode, and Exception Model

Eight new `ResultCode` values, additive only:

| ResultCode | Meaning |
| --- | --- |
| `ACTION_REQUEST_INVALID` | malformed request shape |
| `ACTION_CONTENT_INVALID` | blank/oversized intent text, or malformed `DecisionRef` |
| `ACTION_DECISION_NOT_RECOGNIZED` | Q-012 `NOT_FOUND` for the originating Decision |
| `ACTION_DECISION_AUTHORITY_UNAVAILABLE` | Q-012 call failed |
| `ACTION_IDEMPOTENCY_CONFLICT` | same `operationId`, different fingerprint |
| `ACTION_NOT_FOUND` | full-detail read on an unknown `ActionRef` |
| `ACTION_ACTOR_TYPE_NOT_PERMITTED` | non-`HUMAN` actor attempted to record |
| `ACTION_AUTHORITY_UNAVAILABLE` | unclassified persistence/database failure |

All mapped through the existing shared `GlobalExceptionHandler`.

## 14. Logging, Sensitive Data, and Observability

No `intentText`, `decisionRef`, or actor identity in any log line or
metric tag. `ActionMetrics` uses only bounded operation/outcome/
capability/ResultCode tags.

## 15. Security Design Review

Identical shape and reasoning to Q-011/Q-012 §15. No correction-specific
security surface exists (nothing to correct).

## 16. Test Design

### 16.1 Domain unit tests

- `ActionRef`/`IntentText` boundary rejection (0, 4000, 4001 bytes;
  blank-after-trim; control/NUL characters);
- fingerprint golden vectors and one-field change sensitivity
  (`decisionRef`, `intentText` each independently change the fingerprint).

### 16.2 Application-service tests

- every use case invokes `AuthorizationGuard` before any Action port;
- recording additionally verifies `ActorType == HUMAN` before content
  validation and before the Q-012 call;
- Q-012 validation is invoked with the recording actor's own
  `ActorContext` (mock argument capture);
- only Q-012 `NOT_FOUND` rejects recording;
- exact replay and conflicting replay outcomes;
- full-detail read commits the access-log write before returning content;
  a forced access-log failure returns no content.

### 16.3 Q-009/Q-012 integration tests

- denial/unavailability of `action:*` capabilities yields zero Action
  data access;
- denial/unavailability of `decision:read` during recording yields zero
  Action creation;
- `SERVICE`-actor context with `action:record` granted is still rejected
  by the `HUMAN`-only check.

### 16.4 Real MySQL 8.4 migration/persistence tests

Disposable MySQL 8.4.11, mandatory test datasource inputs, no mandatory
gate skipped. Verify:

- clean upgrade from the current baseline through the new migration,
  Flyway validate/restart/checksum;
- exactly three new tables, no data seed, no destructive DDL;
- every row of §8.4's table, table by table;
- FK-restrict behavior on both `action_operation.action_id` and
  `action_access_log.action_id`;
- query plans use unique/index paths, no full scans;
- **the migration-count assertion in this suite's own test class is
  computed dynamically via `flyway.info().pending().length`, not
  hard-coded** — per the lesson recorded in
  `docs/lessons/2026-08-31-q011-migration-count-test-fix.md` and the
  proactive check Requirement §13 requires, so this module's own test
  does not become a third occurrence of that bug class.

### 16.5 Transaction and concurrency tests

- concurrent same-`operationId`/same-fingerprint requests: exactly one
  commit, one replay;
- forced generated-`actionRef` collision retries at most three times,
  never overwrites;
- a forced `action_access_log` failure during a full-detail read returns
  no content and does not affect a concurrent, unrelated recording
  transaction.

### 16.6 Q-008 consumer/security tests

- `ActionProvenanceView` contains no `intentText` field by
  static/reflective inspection;
- unauthorized/missing/revoked `action:read` calls no Action query;
- no Q-013 repository/table import exists anywhere Q-008 will eventually
  live.

### 16.7 Regression and architecture tests

- existing Q-009/Q-010/Q-011/Q-012 tests continue passing unchanged;
- package dependency test prohibits infrastructure/framework imports from
  `com.brokeros.risk.action.domain`/`application`;
- static scan proves no delete SQL, no edits to any existing migration,
  no permissive provider, no raw content logging, and no vendor-specific
  operation vocabulary anywhere in the module;
- Maven dependency tree remains unchanged.

## 17. Flyway and Rollout Plan

Single additive migration, next unused version number confirmed at
implementation time. No existing migration modified.

## 18. Recommended Future Implementation Sequence

1. Domain types (§4).
2. Migration + §8.4 constraint tests, verified against real MySQL before
   any application code is written.
3. Ports and JDBC adapters.
4. Application services, in the exact §11.1 order.
5. REST controller and DTOs.
6. Full test suite (§16), including the mandatory real-MySQL gate with
   Q-009/Q-010/Q-011/Q-012/Q-013 all enabled.

## 19. Requirement and Acceptance Traceability

| Requirement/AC | Design section |
| --- | --- |
| Q013-FR-001–002 | §6.1, §11.1, §12 |
| Q013-FR-003 | §11.1 step 6 |
| Q013-FR-004 | §11.1 step 4, §12 |
| Q013-FR-005 | §8.1 (`status` single-value, extensible `CHECK`) |
| Q013-FR-006 | §8 (no history/correction table), §9 |
| Q013-FR-007 | §4 (`ActionProvenanceView`), §6.2 |
| Q013-FR-008 | §6.3, §12 |
| Q013-FR-009 | §4, §6 (free text only, no operation taxonomy) |
| AC 1–12 (Requirement §10) | §4, §8, §9, §11, §16 collectively |

## 20. Design Gaps and Outstanding Decisions

None identified that require a Product Owner decision before
implementation — every open question Architecture §21/§22 deferred to
this stage has been resolved above.

## 21. Design Gate

- Implementation Design submission complete: YES (V1)
- Implementation Design V1 approved: **YES — 2026-09-01 — Product Owner.**
  Gate Decision: **PASS.**
- Implementation: **AUTHORIZED — 2026-09-01 — Product Owner**, against
  Requirement V1 / Architecture V1 / ADR-015 (Accepted) / this
  Implementation Design V1.
- Implementation Allowed: **YES**

Next gate: Codex executes the implementation Prompt built strictly from
this document's §11.1/§8.4, then Claude Code performs an independent
implementation review (including independently executed tests) before any
commit.
