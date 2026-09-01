# Q-014 Action Outcome Provenance Foundation Implementation Design

## Document Status

- Requirement: Q-014 — V1, APPROVED — 2026-09-01 — Product Owner
- Architecture: Q-014 — V1 (see its own Document Status)
- ADR: **ADR-016 — Accepted — 2026-09-01 — Product Owner**
- Implementation Design submission: **V1 — APPROVED — 2026-09-01 —
  Product Owner** (accepted as one bundle with Architecture V1 and ADR-016
  at the implementation-authorization gate, per Decision Authority §16.5-B;
  see §21)
- Prepared by: Claude Code, external Architect role. Part of the connected
  Architecture → ADR → Implementation Design bundle accepted at the
  implementation-authorization gate (Decision Authority §16.5-B).
- Implementation: **AUTHORIZED — 2026-09-01 — Product Owner** (see §21).

## 1. Authority, Scope, and Non-Goals

Authoritative, in order: Q-014 Requirement V1, Q-014 Architecture V1,
ADR-016, development standards, and the actual committed Q-009/Q-013 code.

Does not reopen: human-recorded outcome-fact scope; `MANUAL`-only source;
free-text outcome, no result taxonomy; immutable, no correction; many-to
-one, no one-per-Action constraint; two-tier read; `HUMAN`-only recording;
no eligibility service; no cross-module hard FK; no status column.

Non-goals: real execution / Account Control adapter, execution attempt/
retry, result taxonomy, correction/deletion, Q-008 code, Rule Engine.

## 2. Design Outcome Summary

| Area | Decision |
| --- | --- |
| Package | `com.brokeros.risk.actionoutcome`, mirroring `decision`'s layout |
| Migration | next additive version after the highest committed (confirm at implementation time — do not assume V7) |
| Tables | `action_outcome_record`, `action_outcome_operation`, `action_outcome_access_log` — three, no join table, no history table |
| HTTP surface | `POST /api/action-outcomes`, `GET /api/action-outcomes/{actionOutcomeRef}` |
| In-process contract | `confirmProvenance(ActorContext, ActionOutcomeRef)` → `ActionOutcomeProvenanceView`, not HTTP-exposed |
| Capabilities | `action-outcome:record` (HUMAN), `action-outcome:read` (any ActorType) |
| ResultCodes | eight new codes (§13) |

## 3. Module and Package Placement

```
com.brokeros.risk.actionoutcome
├── domain
│   ├── ActionOutcomeRef
│   ├── OutcomeText (value object, mirrors ConclusionText/IntentText)
│   ├── ActionOutcomeSource (enum: MANUAL)
│   ├── ActionOutcomeOperationId, ActionOutcomeSemanticFingerprint
│   ├── ActionOutcomeProvenanceView (narrow read type)
│   └── ActionOutcomeRecord, CompletedActionOutcomeOperation
├── application
│   ├── ActionOutcomeRecordingService
│   ├── ActionOutcomeProvenanceQueryService (narrow read)
│   ├── ActionOutcomeDetailReadService (full-detail read)
│   ├── AuthorizedMutationContext, AuthorizedMutationFactory (module-scoped, own type)
│   └── ActionOutcomeCapabilities
├── application.port
│   ├── ActionOutcomeMutationPort, ActionOutcomeQueryPort, ActionOutcomeAccessLogPort
├── infrastructure.persistence
│   ├── JdbcActionOutcomeMutationAdapter, JdbcActionOutcomeQueryAdapter, JdbcActionOutcomeAccessLogAdapter
├── infrastructure.configuration
│   └── ActionOutcomeModuleConfiguration
├── infrastructure.observability
│   └── ActionOutcomeMetrics
└── interfaces.rest
    └── ActionOutcomeController, request/response DTOs
```

`ActionOutcomeRecord` importing `ActionRef` from
`com.brokeros.risk.action.domain` is the same read-only, downstream-toward
-upstream type dependency Q-013 established toward Q-012.

## 4. Domain Types and Invariants

- `ActionOutcomeRef` — `aoc-<canonical-lowercase-UUIDv4>`, parses like
  `ActionRef`/`DecisionRef`.
- `OutcomeText` — UTF-8 byte length 1–4,000, rejects blank-after-trim, NUL,
  control chars, matching `ConclusionText`'s approach.
- `ActionOutcomeSource` — enum, one value: `MANUAL`.
- `ActionOutcomeRecord` — immutable record: `actionOutcomeRef`, `actionRef`
  (single), `outcomeText`, `source` (`MANUAL`), `recordedByActorRef`,
  `recordedAt`. **No status field.**
- `ActionOutcomeProvenanceView` — narrow read type. Compact-constructor
  invariant mirrors `DecisionProvenanceView`/`ActionProvenanceView`:
  `RECOGNIZED` requires `actionRef`/`recordedByActorRef`/`recordedAt` all
  non-null; `NOT_FOUND` requires them all null. **No `outcomeText` field.**
- `CompletedActionOutcomeOperation` — `operationId`, `actionOutcomeRef`,
  `outcome` (`CREATED`), the full `ActionOutcomeRecord`.

## 5. Q-009 Authorization Integration

`AuthorizationGuard.requireAllowed` before any use case body.
`ActionOutcomeCapabilities` defines exactly `action-outcome:record` and
`action-outcome:read`. No Q-009 file changes.

## 6. Application Use Cases

- **6.1 `ActionOutcomeRecordingService.record(ActorContext, RecordActionOutcomeSpec)`**
  — the single mutating use case (§11 order).
- **6.2 `ActionOutcomeProvenanceQueryService.confirmProvenance(ActorContext, ActionOutcomeRef)`**
  — narrow read: authorize (`action-outcome:read`) → lookup → return
  `ActionOutcomeProvenanceView`. No `HUMAN` check.
- **6.3 `ActionOutcomeDetailReadService.readDetail(ActorContext, ActionOutcomeRef)`**
  — full-detail: authorize → lookup; if found, commit an access-log row
  (`REQUIRES_NEW`, non-read-only) **before** returning the full record
  including `outcomeText`; a failed access-log write propagates and returns
  no content. No `HUMAN` check.

## 7. Application Ports and Ownership

- `ActionOutcomeMutationPort.record(RecordActionOutcomeSpec, AuthorizedMutationContext)`
  → `CompletedActionOutcomeOperation`. One method.
- `ActionOutcomeQueryPort.findByRef(ActionOutcomeRef)` →
  `Optional<ActionOutcomeRecord>`; `.findOperation(operationId)` → replay.
- `ActionOutcomeAccessLogPort.recordFullDetailAccess(ActionOutcomeRef, ActorRef, Instant)`.

## 8. Concrete Persistence Model

Three InnoDB tables, additive migration, next unused version (confirm at
implementation time). Timestamps server UTC `DATETIME(6)`; PKs `BIGINT
AUTO_INCREMENT id`, never exposed; no cascade/delete.

### 8.1 `action_outcome_record`

| Column | Type | Null | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | PK, internal |
| `action_outcome_ref` | `CHAR(40)` ASCII `ascii_bin` | not null | unique canonical `aoc-<UUIDv4>` |
| `action_ref` | `CHAR(40)` ASCII `ascii_bin` | not null | Q-013 `ActionRef`, no local FK |
| `source` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `MANUAL` check |
| `outcome_text` | `VARBINARY(4000)` | not null | exact UTF-8 bytes, immutable |
| `recorded_by_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 UUIDv4 ActorRef |
| `recorded_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes: PK `pk_action_outcome_record(id)`; unique
`uk_action_outcome_record_ref(action_outcome_ref)`; checks for
`action_outcome_ref` shape (`aoc-`+UUIDv4), `action_ref` shape
(`act-`+UUIDv4), `recorded_by_actor_ref` UUIDv4 shape, `source IN
('MANUAL')`, `OCTET_LENGTH(outcome_text) BETWEEN 1 AND 4000`; index
`idx_action_outcome_record_action(action_ref)`.

No status column, no self-FK. **No uniqueness on `action_ref`** (many-to
-one, Requirement §5.3(3)).

### 8.2 `action_outcome_operation`

| Column | Type | Null | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | PK |
| `operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | globally unique UUIDv4 idempotency key |
| `operation_type` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `RECORD` — only value |
| `semantic_fingerprint` | `BINARY(32)` | not null | SHA-256 over raw `action_ref` + outcome-text strings |
| `action_outcome_id` | `BIGINT` | not null | FK → `action_outcome_record.id` |
| `outcome` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `CREATED` — only value |
| `occurred_at` | `DATETIME(6)` | not null | server UTC |

Constraints: unique `uk_action_outcome_operation_id(operation_id)`; FK
`fk_action_outcome_operation_record(action_outcome_id)` →
`action_outcome_record.id`, `ON DELETE RESTRICT`; checks for `operation_id`
UUIDv4 shape, `operation_type IN ('RECORD')`, `outcome IN ('CREATED')`.

### 8.3 `action_outcome_access_log`

| Column | Type | Null | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | PK |
| `action_outcome_id` | `BIGINT` | not null | FK → record, delete restricted |
| `accessing_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 actor |
| `accessed_at` | `DATETIME(6)` | not null | UTC |

Constraints: FK `fk_action_outcome_access_log_record(action_outcome_id)` →
`action_outcome_record.id`, `ON DELETE RESTRICT`; index
`idx_action_outcome_access_log_record(action_outcome_id, accessed_at)`.

### 8.4 Constraint-to-test traceability

Built by walking every column of §8.1–§8.3 in order.

**`action_outcome_record`:**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `action_outcome_ref` globally unique | `UNIQUE (action_outcome_ref)` | §16.4 unique-index test |
| `action_outcome_ref` canonical `aoc-<UUIDv4>` | `CHECK` | §16.1 boundary/format test |
| `action_ref` canonical `act-<UUIDv4>` | `CHECK` | §16.1 boundary/format test |
| `recorded_by_actor_ref` canonical UUIDv4 | `CHECK` | §16.1 boundary/format test |
| `source` allowed value | `CHECK (source IN ('MANUAL'))` | §16.1 enum test |
| `outcome_text` 1–4,000 bytes | `CHECK (OCTET_LENGTH(outcome_text) BETWEEN 1 AND 4000)` | §16.1 boundary test |

**`action_outcome_operation`:**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `operation_id` globally unique | `UNIQUE (operation_id)` | §16.4 unique-index test |
| `operation_id` canonical UUIDv4 | `CHECK` | §16.1 boundary/format test |
| `operation_type` exactly `RECORD` | `CHECK (operation_type IN ('RECORD'))` | §16.1 enum test |
| `outcome` exactly `CREATED` | `CHECK (outcome IN ('CREATED'))` | §16.1 enum test |
| `action_outcome_id` references existing record | FK, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

**`action_outcome_access_log`:**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| access rows cannot outlive their record | FK, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

## 9. Why ActionOutcome Trusts the Action Transitively

Recording validates the pertaining `ActionRef` via Q-013's narrow
contract, but does not re-validate the Action's own originating Decision —
Q-013 validated it at Action-recording time and Action is immutable, so
nothing can have changed. Re-validating transitively would duplicate work
and create an unrequested dependency on Q-012.

## 10. HTTP API Contract

- `POST /api/action-outcomes` — record. Request DTO: `actionRef` (string,
  singular), `outcomeText` (string), `operationId` (string, client-supplied
  idempotency key). Actor from `ActorContextProvider.currentContext()`
  only. No `actionOutcomeRef`/`recordedByActorRef`/`recordedAt`/`source`
  accepted from the client. Response:
  `ApiResponse<ActionOutcomeRecordedResponse>` (`actionOutcomeRef`,
  `actionRef`, `recordedByActorRef`, `recordedAt`, `outcome`) — never
  `outcomeText`.
- `GET /api/action-outcomes/{actionOutcomeRef}` — full-detail; response
  includes `outcomeText`; 404-shaped `ACTION_OUTCOME_NOT_FOUND` on unknown
  ref.
- No `PATCH`/`PUT`. `confirmProvenance` never on HTTP.

Bean Validation at the controller boundary; domain-level parsing is
authoritative.

## 11. Idempotency, Duplicate, and Retry Design

### 11.1 Canonical execution order (single authoritative statement)

1. `authorizationGuard.requireAllowed(actorContext, ActionOutcomeCapabilities.RECORD)`.
2. `requireHuman(actorContext)`.
3. Compute `semanticFingerprint` from the **raw** request fields
   (`actionRef` string, `outcomeText` string) — before domain parsing.
4. `queryPort.findOperation(operationId)` — replay check. Identical
   fingerprint → return the original `CompletedActionOutcomeOperation`,
   no new write; different → `ACTION_OUTCOME_IDEMPOTENCY_CONFLICT`. **This
   step precedes every step below.**
5. (New operation only) Content validation: parse `OutcomeText`; parse the
   single `ActionRef`.
6. `actionQueryService.confirmProvenance(actorContext, actionRef)` (Q-013,
   actor's own context) — reject `NOT_FOUND` with
   `ACTION_OUTCOME_ACTION_NOT_RECOGNIZED`; map unavailability to
   `ACTION_OUTCOME_ACTION_AUTHORITY_UNAVAILABLE`.
7. Build `AuthorizedMutationContext` (fingerprint, actorContext,
   authorizationDecision, capability, `clock.instant()` — shared, already
   microsecond-truncated `Clock` bean).
8. `mutationPort.record(new RecordActionOutcomeSpec(operationId, actionRef,
   outcomeText), context)` — inserts `action_outcome_record` and
   `action_outcome_operation` atomically.

### 11.2 Generated-reference collision handling

`actionOutcomeRef` generation retries on the named unique-constraint
violation only, at most three attempts, never overwrites — matching
Q-010…Q-013.

## 12. Transaction and Concurrency Design

Recording: one transaction (§11.1 step 8). Concurrent same-`operationId`/
same-fingerprint: the unique constraint on
`action_outcome_operation.operation_id` elects one committer; the other
replays. Full-detail read's access-log write is a short `REQUIRES_NEW`
transaction isolated from the read and from any concurrent unrelated
recording. No concurrent-correction scenario (no correction).

## 13. Error, ResultCode, and Exception Model

Eight new `ResultCode` values, additive only:

| ResultCode | Meaning |
| --- | --- |
| `ACTION_OUTCOME_REQUEST_INVALID` | malformed request shape |
| `ACTION_OUTCOME_CONTENT_INVALID` | blank/oversized outcome text, or malformed `ActionRef` |
| `ACTION_OUTCOME_ACTION_NOT_RECOGNIZED` | Q-013 `NOT_FOUND` for the pertaining Action |
| `ACTION_OUTCOME_ACTION_AUTHORITY_UNAVAILABLE` | Q-013 call failed |
| `ACTION_OUTCOME_IDEMPOTENCY_CONFLICT` | same `operationId`, different fingerprint |
| `ACTION_OUTCOME_NOT_FOUND` | full-detail read on an unknown `ActionOutcomeRef` |
| `ACTION_OUTCOME_ACTOR_TYPE_NOT_PERMITTED` | non-`HUMAN` actor attempted to record |
| `ACTION_OUTCOME_AUTHORITY_UNAVAILABLE` | unclassified persistence/database failure |

All mapped through the existing shared `GlobalExceptionHandler`.

## 14. Logging, Sensitive Data, and Observability

No `outcomeText`, `actionRef`, or actor identity in any log line or metric
tag. `ActionOutcomeMetrics` uses only bounded operation/outcome/capability/
ResultCode tags.

## 15. Security Design Review

Identical shape/reasoning to Q-012/Q-013 §15. No correction-specific
surface (nothing to correct).

## 16. Test Design

### 16.1 Domain unit tests

- `ActionOutcomeRef`/`OutcomeText` boundary rejection (0, 4000, 4001 bytes;
  blank-after-trim; control/NUL);
- fingerprint golden vectors and one-field change sensitivity (`actionRef`,
  `outcomeText` each independently change the fingerprint).

### 16.2 Application-service tests

- every use case invokes `AuthorizationGuard` before any port;
- recording verifies `ActorType == HUMAN` before content validation and
  before the Q-013 call;
- Q-013 validation invoked with the recording actor's own `ActorContext`
  (mock capture);
- only Q-013 `NOT_FOUND` rejects recording;
- exact replay and conflicting replay outcomes;
- full-detail read commits the access-log write before returning content;
  a forced access-log failure returns no content.

### 16.3 Q-009/Q-013 integration tests

- denial/unavailability of `action-outcome:*` yields zero data access;
- denial/unavailability of `action:read` during recording yields zero
  ActionOutcome creation;
- `SERVICE`-actor with `action-outcome:record` granted is still rejected by
  the `HUMAN`-only check.

### 16.4 Real MySQL 8.4 migration/persistence tests

Disposable MySQL 8.4.11, mandatory datasource inputs, no mandatory gate
skipped. Verify: clean upgrade from the current baseline through the new
migration, Flyway validate/restart/checksum; exactly three new tables, no
seed, no destructive DDL; every §8.4 row; FK-restrict on both
`action_outcome_operation.action_outcome_id` and
`action_outcome_access_log.action_outcome_id`; that recording the **same**
`action_ref` twice (different outcome refs) succeeds (many-to-one, no
uniqueness); query plans use unique/index paths, no full scans; **the
migration-count assertion is fixed-baseline hard-coded for the target-N
transition but dynamic (`flyway.info().pending().length`) for the
unrestricted post-baseline migrate — per
`docs/lessons/2026-08-31-q011-migration-count-test-fix.md`.**

### 16.5 Transaction and concurrency tests

- concurrent same-`operationId`/same-fingerprint: exactly one commit, one
  replay;
- forced generated-`actionOutcomeRef` collision retries ≤3, never
  overwrites;
- a forced `action_outcome_access_log` failure during a full-detail read
  returns no content and does not affect a concurrent unrelated recording.

### 16.6 Q-008 consumer/security tests

- `ActionOutcomeProvenanceView` has no `outcomeText` field by static/
  reflective inspection;
- unauthorized/missing/revoked `action-outcome:read` calls no query;
- no Q-014 repository/table import exists anywhere Q-008 will live.

### 16.7 Regression and architecture tests

- existing Q-009…Q-013 tests pass unchanged;
- package dependency test prohibits infrastructure/framework imports from
  `com.brokeros.risk.actionoutcome.domain`/`application`;
- static scan proves no delete SQL, no edits to existing migrations, no
  permissive provider, no raw content logging, and **no execution/adapter/
  MT4/MT5/result-taxonomy vocabulary** anywhere in the module;
- Maven dependency tree unchanged.

## 17. Flyway and Rollout Plan

Single additive migration, next unused version confirmed at implementation
time. No existing migration modified.

## 18. Recommended Future Implementation Sequence

1. Domain types (§4). 2. Migration + §8.4 constraint tests, verified
against real MySQL first. 3. Ports and JDBC adapters. 4. Application
services in §11.1 order. 5. REST controller and DTOs. 6. Full test suite
(§16), including the mandatory real-MySQL gate with Q-009…Q-014 enabled.

## 19. Requirement and Acceptance Traceability

| Requirement/AC | Design section |
| --- | --- |
| Q014-FR-001–002 | §6.1, §11.1, §12 |
| Q014-FR-003 | §11.1 step 6 |
| Q014-FR-004 | §11.1 step 4, §12 |
| Q014-FR-005 | §8 (no history table), §9 |
| Q014-FR-006 | §4 (`ActionOutcomeProvenanceView`), §6.2 |
| Q014-FR-007 | §6.3, §13 |
| Q014-FR-008 | §4, §6, §16.7 (free text; no taxonomy/adapter) |
| AC 1–10 (Requirement §10) | §4, §8, §9, §11, §16 collectively |

## 20. Design Gaps and Outstanding Decisions

None requiring a Product Owner decision before implementation — every open
question Architecture §21/§22 deferred to this stage is resolved above;
the exact migration version number is the sole implementation-time fact.

## 21. Design Gate

- Implementation Design submission complete: YES (V1)
- Implementation Design V1 approved: **YES — 2026-09-01 — Product Owner**
  (bundle, with Architecture V1 and ADR-016-as-accepted)
- Implementation: **AUTHORIZED — 2026-09-01 — Product Owner**
- Implementation Allowed: **YES**

Next gate: Codex executes the implementation Prompt built strictly from
§11.1/§8.4, then Claude Code performs an independent implementation review
(including independently executed tests) before any commit.
