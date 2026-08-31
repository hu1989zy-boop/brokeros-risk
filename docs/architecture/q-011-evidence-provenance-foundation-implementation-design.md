# Q-011 Evidence Provenance Foundation Implementation Design

## Document Status

- Requirement: Q-011 — V3, APPROVED — 2026-08-28 — Product Owner (Goal 5
  fix, round four — see Requirement's own Document Status)
- Architecture: Q-011 Architecture — V4, APPROVED — 2026-08-28 — Product
  Owner (§23 fixes, round four) — see
  `q-011-evidence-provenance-foundation-architecture.md` Document Status
- ADR: ADR-013 — accepted (original) 2026-08-28; **amendment RE-ACCEPTED —
  2026-08-28 — Product Owner** (round four — see ADR-013's own Amendment
  section)
- Implementation Design version: **V5 — APPROVED — 2026-08-28 — Product
  Owner** (V1 → V2 round 1 → V3 round 2 → V4 approved/authorized round
  3, §20.3 → this V5 fixed leftovers in §1.1/§20.9/§21 found by an
  independent fourth governance-consistency round, §20.10, and is now
  approved)
- Status: **APPROVED**
- Prepared by: Claude Code, holding the external Architect review role by
  explicit Product Owner direction; self-review, not independent (see
  Q-011-Evidence-Provenance-Foundation.md §18).
- Implementation: **AUTHORIZED — 2026-08-28 — Product Owner, against V5**
- Implementation Allowed: **YES — see the Codex resume Prompt issued
  2026-08-28, built strictly from §11.1/§11.4**

This document converts the approved Q-011 Architecture into concrete future
implementation contracts. **Four rounds of halting on real defects have now
occurred, three by Codex and this fourth by an independently authored,
explicitly mechanical governance-consistency task** —
`prompts/Q-011-V11-Fourth-Governance-Consistency-Correction-Prompt.md` —
run after round three's fixes were approved and authorized, and finding
that they had not fully propagated: this document's own §1.1 still named
stale version numbers, §20.9 still listed an item that had already been
brought in scope, and §21 contained two directly contradictory "Next gate"
statements. Separately and more seriously, **ADR-013 — accepted before
round three's subject-bar fix — was never amended afterward and still
stated the old, incorrect bar throughout**, which this round corrects via
an ADR amendment. Full finding-by-finding record: §20.10. No prior round's
substantive decisions were reopened. **The Product Owner approved all four
documents' candidates together, and separately authorized implementation,
on 2026-08-28.** This document itself creates no Java, Flyway migration,
table, endpoint, configuration, deployment behavior, staging, commit, or
push — implementation proceeds only through the explicit Codex Prompt
issued the same date.

## 1. Authority, Scope, and Non-Goals

### 1.1 Governing authority and document priority (V3 addition)

Strict priority order, highest first, for resolving any apparent conflict
anywhere in this task's document set:

1. `AGENTS.md` and development standards;
2. the approved Q-011 Requirement (see its own Document Status for the
   current version — do not hard-code a version number here, which has
   gone stale in every prior round);
3. the approved Q-011 Architecture (see its own Document Status);
4. accepted ADR-013 (see its own Status/Amendment header);
5. accepted ADR-002, ADR-009, ADR-010, ADR-011, and ADR-012;
6. this Implementation Design (see its own Document Status); and
7. any Codex execution Prompt issued against this Design.

**A Codex execution Prompt is the lowest-priority artifact in this list. It
summarizes and directs; it never overrides, reinterprets, or silently
redefines anything above it.** If a future Prompt's wording and this
Design's content ever appear to disagree, this Design governs, and the
Prompt is wrong and must be corrected before Codex proceeds — exactly the
situation that produced this V3 revision, where an earlier Prompt's prose
summary of the execution order did not match this Design's actual §11.1.

If implementation evidence contradicts this design without changing an
approved invariant, the design must be repaired and reviewed. Identity
ownership, content immutability, single-level supersession, subject-
reference stability across correction, mandatory correction reason,
`HUMAN`-only recording, the two-tier read-contract design, and the decision
to reuse Q-010 unchanged cannot be changed inside implementation.

### 1.2 In scope

- the Q-011 Evidence domain values and status model;
- a minimal current-state table plus append-only operation history and
  access log;
- protected HTTP recording and correction endpoints;
- protected HTTP full-detail read with mandatory access-audit;
- the in-process, protected Q-008 existence/provenance contract;
- exact idempotency, uniqueness, concurrency, and transaction behavior;
- Q-009 capability integration, including the `trading-account-reference:read`
  grant Evidence-*recording* actors must also hold (correcting actors do
  not — correction never calls Q-010, §9; V3 fix, another instance of the
  same stale "recording/correcting" pattern found across §20.1 round two);
- a future additive Flyway `V4` plan and Spring JDBC adapters; and
- safe ResultCodes, exceptions, logs, metrics, and tests.

### 1.3 Explicit non-goals

This design excludes:

- Q-008 Risk Case implementation or changes to its approved design;
- Decision, Action, ActionOutcome, Rule Engine, Account Control, or external
  execution behavior;
- any Evidence source other than `MANUAL`, or any `SERVICE`-actor authoring
  path;
- any change to Q-009 or Q-010's already-shipped code, schema, or
  capability catalog;
- file/document/blob attachment, evidence category/severity/confidence
  fields;
- Redis keys/cache, Kafka topics/events, or another persistence system;
- a general Audit query API spanning Q-009/Q-010/Q-011 uniformly; and
- any implementation before separate Design approval and explicit
  implementation authorization.

## 2. Design Outcome Summary

| Area | Concrete design decision |
| --- | --- |
| Module | `com.brokeros.risk.evidence` in the existing deployable |
| Persistence | four additive MySQL tables through future Flyway V4 |
| Reference | exact `ev-<lowercase UUIDv4>` value, length 39 |
| Content | `observation_text` (≤4,000 bytes UTF-8), `correction_reason` (≤1,000 bytes UTF-8) |
| Status | `ACTIVE`, `SUPERSEDED`; at most one correction per record via nullable-unique constraint |
| Subject validation | Q-010's existing `validateForNewRiskCaseAssociation`, called with the recording actor's own `ActorContext`; correction never calls Q-010 (Architecture §9, §V3 fix — see §20.1) |
| Operation model | one client-supplied UUIDv4 `operationId` per request is the idempotency key |
| Fingerprint | SHA-256 of a fixed length-prefixed typed field sequence, same technique as Q-010 |
| Transactions | one local MySQL transaction per record/correct result; one short dedicated (not database-read-only) transaction per full-detail read, since it writes the access-log row |
| Concurrency | unique constraints and compare-and-set are final arbiters; no distributed lock |
| Exposure | protected authenticated HTTP endpoints (record, correct, full-detail read); Q-008 contract remains in-process |
| Dependencies | none beyond the committed Java/Spring JDBC/MySQL/Flyway/Micrometer stack |
| Messaging/cache | none |

## 3. Module and Package Placement

```text
com.brokeros.risk.evidence
├── domain
│   ├── EvidenceRef
│   ├── ObservationText
│   ├── CorrectionReason
│   ├── EvidenceSource
│   ├── EvidenceStatus
│   ├── EvidenceRecord
│   ├── EvidenceOperationId / EvidenceOperationType / EvidenceOperationOutcome
│   └── EvidenceProvenanceView
├── application
│   ├── EvidenceCapabilities
│   ├── EvidenceRecordingService
│   ├── EvidenceCorrectionService
│   ├── EvidenceProvenanceQueryService
│   ├── EvidenceDetailReadService
│   ├── EvidenceFingerprintFactory
│   └── command/query/result records and expected BusinessExceptions
├── application.port
│   ├── EvidenceQueryPort
│   ├── EvidenceMutationPort
│   ├── EvidenceAccessLogPort
│   └── EvidenceRefGenerator
├── infrastructure.persistence
│   ├── JdbcEvidenceQueryAdapter
│   ├── JdbcEvidenceMutationAdapter
│   ├── JdbcEvidenceAccessLogAdapter
│   ├── JdbcEvidenceRowMappers
│   └── MySqlEvidenceConstraintClassifier
├── infrastructure.configuration
│   └── EvidenceModuleConfiguration
└── interfaces.rest
    ├── EvidenceController
    ├── RecordEvidenceRequest / CorrectEvidenceRequest
    └── EvidenceDetailResponse / EvidenceRecordedResponse
```

- `domain` uses only JDK types and owns identity/status invariants.
- `application` coordinates use cases, Q-009 authorization, fingerprinting,
  Q-010 subject validation, and typed ports; it imports no JDBC, Servlet,
  Spring Security, or persistence record.
- `interfaces.rest` is the only input adapter. Controllers translate HTTP,
  apply Bean Validation, call exactly one application service, and return
  `ApiResponse`, per AGENTS.md.
- `infrastructure.persistence` owns SQL, transaction templates, constraint
  classification, and row mapping.
- no JPA annotation or persistence field enters a domain value.
- no `common`, `utils`, `manager`, generic CRUD service, or dumping-ground
  package is added.

## 4. Domain Types and Invariants

### 4.1 Identity and content values

| Type | Responsibility and exact rules | Equality / exposure |
| --- | --- | --- |
| `EvidenceRef` | immutable String; exactly `ev-` plus canonical lowercase UUIDv4; length 39; generator uses `UUID.randomUUID()` behind a port | full exact value; may cross the Q-008 boundary as an opaque reference |
| `ObservationText` | immutable String; 1–4,000 UTF-8 bytes; rejects NUL/control characters; no trim/normalization | full value exposed only by the full-detail contract; never in the narrow provenance view |
| `CorrectionReason` | immutable String; 1–1,000 UTF-8 bytes; same character rules as `ObservationText` | never exposed to Q-008; retained in operation history only |
| `EvidenceSource` | enum; exactly one value, `MANUAL`, in this Foundation | exposed in the provenance view |
| `EvidenceStatus` | enum; exactly `ACTIVE`, `SUPERSEDED` | exposed in the provenance view |

Reference/content constructors reject null, blank, oversize, and malformed
input at construction time — invalid values cannot be represented, not just
rejected by a later check.

### 4.2 EvidenceRecord

`EvidenceRecord` (application-level current-state view) contains:

- `EvidenceRef`;
- `TradingAccountRef` subject (Q-010 type, reused unchanged);
- `EvidenceSource`;
- `ObservationText`;
- `EvidenceStatus`;
- recording `ActorRef` and UTC `recordedAt`;
- optional `supersedesRef` (`EvidenceRef` this record corrects, if any); and
- optional `supersededByRef` (`EvidenceRef` that corrected this record, if
  any).

The type exposes no setters. The only two possible transitions are: (a)
creation (status `ACTIVE`, both pointer fields determined at insert time and
never `null → non-null` for `supersedesRef` after creation), and (b) exactly
one status change `ACTIVE → SUPERSEDED` plus population of
`supersededByRef`, performed only by the correction use case targeting this
exact record. There is no third state and no reversal.

### 4.3 Operation and provenance values

- `EvidenceOperationId` is an exact canonical lowercase UUIDv4 without a
  prefix, client-supplied per request, and is the single idempotency key.
- `EvidenceOperationType` is `RECORD` or `CORRECT`.
- `EvidenceOperationOutcome` is exactly `CREATED` or `CORRECTED`. There is
  no separate `UNCHANGED` value (V2 fix; see Section 20.1, item 4): unlike
  Q-010, Evidence has no "same business fact registered under a new
  operation ID" scenario — a genuinely new operation ID always produces a
  genuinely new record. An exact replay (same operation ID, same
  fingerprint) is not a distinct outcome; it returns whichever outcome
  (`CREATED` or `CORRECTED`) was durably stored the first time.
- `EvidenceProvenanceView` (the narrow Q-008-facing type) contains:
  `EvidenceRef`, recognized/not-found outcome, and — when recognized —
  subject reference, source, status, `recordedAt`, and `supersededByRef`
  (nullable). It never contains `ObservationText` or `CorrectionReason`.

## 5. Q-009 Authorization Integration

### 5.1 Capability catalog

`EvidenceCapabilities` owns exactly three constants using the committed
Q-009 `Capability` type:

```text
RECORD  = evidence:record
CORRECT = evidence:correct
READ    = evidence:read
```

| Use case | Required Q-011 capability | Additional required capability |
| --- | --- | --- |
| record Evidence | `RECORD` | `trading-account-reference:read` (Q-010, for subject validation) |
| correct Evidence | `CORRECT` | none (subject match is a local invariant, not a fresh Q-010 call — Section 9) |
| existence/provenance check (Q-008/future consumers) | `READ` | none |
| full-detail read | `READ` | none |

Every service receives the caller's existing Q-009 `ActorContext` and calls
the committed `AuthorizationGuard.requireAllowed` before any Q-011
repository access. Recording additionally requires
`ActorContext.actorType() == ActorType.HUMAN`, checked in the application
layer immediately after authorization and before any content validation —
this is a Q-011 domain invariant, not a new Q-009 concept, and a `SERVICE`
actor is rejected even if it somehow held `evidence:record`.

Recording then reuses the same `ActorContext` to call Q-010's
`validateForNewRiskCaseAssociation`, which independently requires
`trading-account-reference:read` on that same actor. No new Q-011 SERVICE
identity, `ServiceActorContextFactory` usage, or Q-009 composition-root
change is introduced — unlike Q-010, which needed a purpose-specific
service descriptor for its non-web provisioner, Q-011 has no non-web
command and performs every use case under the calling human's own trusted
context.

### 5.2 No purpose-specific service actor

Q-011 introduces no new Q-009 `RegisteredServiceDescriptor`. Every
protected Q-011 use case — recording, correction, and both reads — is
triggered through the normal HTTP/in-process authentication boundary, not
by a deployment-invoked non-web command; there is no Q-011 equivalent of
Q-010's non-web provisioner. **This is a statement about how the actor
reaches Q-011, not about which actor type is required — `HUMAN` is
required only for recording and correction (Requirement `Q011-FR-005`;
Section 11.4), never for either read use case (V3 fix: this paragraph
previously read as if `HUMAN` applied everywhere, contradicting §11.4;
see §20.1, third correction round).** This is a deliberate, simpler
integration than Q-010's, made possible because Evidence recording is a
normal application action rather than an externally attested provisioning
event (Architecture §8).

## 6. Application Use Cases

Every mutation receives an `AuthorizedMutationContext` (reused type
convention from Q-010, distinct instance) built after Q-009 authorization
and, for recording, after Q-010 subject validation. It contains operation
ID, fingerprint, trusted `ActorContext`, allow decision, and server UTC
time. Caller-supplied actor or time fields do not exist.

| Use case/service | Typed input | Order and transaction | Output / failures / history |
| --- | --- | --- | --- |
| record Evidence | operationId, subjectRef, observationText | RECORD authorization → HUMAN check → **exact-replay check (return immediately if matched)** → content validation → Q-010 subject validation (recognized; `RECOGNIZED_NOT_ELIGIBLE` accepted, V3 fix) → mutation port transaction | generated `EvidenceRef`, original `CREATED` result on replay; one history row |
| correct Evidence | operationId, target EvidenceRef, correctionReason, new observationText | CORRECT authorization → HUMAN check → **exact-replay check (return immediately if matched, without checking target status)** → content validation → load target, verify `ACTIVE` → mutation port transaction | new `EvidenceRef`, original `CORRECTED` result on replay; target `SUPERSEDED`, two-row atomic update, one history row |
| provenance check (Q-008) | ActorContext + EvidenceRef | READ authorization → read-only query | bounded `EvidenceProvenanceView`; no mutation/history |
| full-detail read | ActorContext + EvidenceRef | READ authorization → read-only query → access-log insert | complete record incl. observation text; access-log row committed before return |

This table restates Section 11.1/12.1/12.2's ordering at a glance; those
sections are authoritative if this summary and the detailed steps ever
appear to diverge again. **Authorization and the `HUMAN`-actor-type check
are never skipped, not even for an exact replay** — only content
validation, the Q-010 call, and the target-status check are skipped on
replay (see the boldfaced note in Section 11.1).

### 6.1 Recording rules

- The exact-replay check (Section 11.1) runs immediately after
  authorization and the `HUMAN` check, before anything else. On a match it
  returns the stored result — including the original `CREATED` outcome —
  without re-running any rule below.
- For a genuinely new operation ID: subject must be recognized by Q-010 —
  either `ELIGIBLE_FOR_NEW_ASSOCIATION` or `RECOGNIZED_NOT_ELIGIBLE` (e.g.
  an inactive/retired account) is accepted. Only `NOT_RECOGNIZED` rejects
  the request without creating Evidence. This matches Requirement
  `Q011-FR-002` and Architecture §9 exactly (V3 fix — V1/V2 incorrectly
  required the stricter `ELIGIBLE_FOR_NEW_ASSOCIATION` bar; see §20.1,
  third correction round).
- A different fingerprint under the same operation ID conflicts.
- Recording never accepts a proposed `EvidenceRef`.

### 6.2 Correction rules

- The exact-replay check runs immediately after authorization and the
  `HUMAN` check, before anything else, and — critically — before the
  target-status check. On a match it returns the stored result, including
  the original `CORRECTED` outcome, **without checking whether the target
  is currently `ACTIVE`**, because it is expected to already be
  `SUPERSEDED` after the first successful correction.
- For a genuinely new operation ID: the target record must exist and be
  `ACTIVE`. A target that is already `SUPERSEDED`, or does not exist, is
  rejected — never silently accepted as a new unrelated record.
- The request's subject reference is not a separate input field; the new
  record's subject is copied from the target record, making a
  subject-mismatch structurally impossible rather than merely validated
  (stronger than Requirement §7's minimum, and simpler to implement
  correctly).
- Correction does not re-call Q-010, ever, for a new operation or a replay.
  The target's subject was already validated as recognized when it was
  originally recorded (directly, or transitively through the record it
  itself corrects); Section 9 explains why re-validation is unnecessary.
- A different
  fingerprint under the same operation ID conflicts.

## 7. Application Ports and Ownership

### 7.1 Query port

```text
interface EvidenceQueryPort {
    Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id);
    Optional<EvidenceRecordState> findByRef(EvidenceRef ref);
}
```

Returns immutable application persistence views only — never `ResultSet`,
internal IDs, or JDBC types. Implementations reject cardinality greater than
one even though unique constraints should prevent it.

### 7.2 Mutation port

```text
interface EvidenceMutationPort {
    EvidenceRecordingResult record(RecordEvidenceSpec spec,
                                    AuthorizedMutationContext context);
    EvidenceCorrectionResult correct(CorrectEvidenceSpec spec,
                                      AuthorizedMutationContext context);
}
```

Each method is one complete unit of work. The JDBC adapter owns the local
transaction and must write current state, final operation outcome, and
exactly one history row atomically. `correct` additionally performs the
target's compare-and-set to `SUPERSEDED` plus `supersededByRef` population
inside the same transaction as the new record's insert.

### 7.3 Access log port

```text
interface EvidenceAccessLogPort {
    void recordFullDetailAccess(EvidenceRef ref, ActorRef accessor, Instant occurredAt);
}
```

A separate short transaction from recording/correction; it never
participates in a mutation's transaction and never blocks a concurrent
mutation of the same record.

### 7.4 Generator port

`EvidenceRefGenerator` returns `EvidenceRef` values. The production adapter
uses JDK `UUID.randomUUID()`; tests use deterministic sequences to force
collision behavior. The mutation adapter makes the database unique
constraint the final authority and permits at most three generated-ref
attempts per request, each a fresh full transaction with the same operation
ID and fingerprint. Exhaustion fails closed.

## 8. Concrete Persistence Model

The future migration creates exactly four application-owned InnoDB tables.
All timestamps are server-derived UTC `DATETIME(6)`. All internal primary
keys are `BIGINT AUTO_INCREMENT` named `id` and never cross application
boundaries. No table has cascade delete, delete use case, money, or
customer data.

### 8.1 `evidence_record`

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key, internal only |
| `evidence_ref` | `CHAR(39)` ASCII `ascii_bin` | not null | unique canonical `ev-<UUIDv4>` |
| `subject_ref` | `CHAR(39)` ASCII `ascii_bin` | not null | Q-010 `TradingAccountRef`, no local FK (Architecture §7) |
| `source` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `MANUAL` check, extensible |
| `observation_text` | `VARBINARY(4000)` | not null | exact UTF-8 bytes, immutable |
| `status` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `ACTIVE`/`SUPERSEDED` check |
| `supersedes_id` | `BIGINT` | nullable | self-FK, delete restricted; set only at insert |
| `superseded_by_id` | `BIGINT` | nullable | self-FK, delete restricted; set exactly once by correction |
| `recorded_by_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 UUIDv4 ActorRef |
| `recorded_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes:

- PK `pk_evidence_record(id)`;
- unique `uk_evidence_record_ref(evidence_ref)`;
- unique `uk_evidence_record_supersedes(supersedes_id)` — MySQL permits
  multiple `NULL`s in a unique index, so this enforces "at most one
  correction per target" only for non-null values, exactly the invariant
  Q011-FR-008 requires;
- self-FK `fk_evidence_record_supersedes` and
  `fk_evidence_record_superseded_by` to `evidence_record.id`, both
  `ON DELETE RESTRICT`;
- checks for ref/actor UUIDv4 shape, `source`, `status`, and
  `OCTET_LENGTH(observation_text) BETWEEN 1 AND 4000`; and
- index `idx_evidence_record_subject(subject_ref)` for future subject-scoped
  queries.

### 8.2 `evidence_operation`

Durable idempotency outcome and replay source, mirroring Q-010's
`trading_account_authority_operation` table shape.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `operation_id` | `CHAR(36)` ASCII `ascii_bin` | not null | globally unique UUIDv4 idempotency key |
| `operation_type` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `RECORD`/`CORRECT` |
| `semantic_fingerprint` | `BINARY(32)` | not null | SHA-256 typed payload fingerprint |
| `evidence_id` | `BIGINT` | not null | FK to the record whose `status` this operation reflects (V3 fix — previously the ambiguous "resulting/target record"): for `RECORD`, the newly created record (only one exists); for `CORRECT`, the **target** record being superseded, not the new replacement — this is what makes `before_status`/`after_status` (§8.3) a meaningful transition rather than a trivially-always-`ACTIVE` value |
| `outcome` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `CREATED`/`CORRECTED` (no `UNCHANGED`; V2 fix, Section 20.1 item 4) |
| `occurred_at` | `DATETIME(6)` | not null | server UTC |

Constraints/indexes: unique `uk_evidence_operation_id(operation_id)`; FK
`evidence_id` → `evidence_record.id` delete restricted; checks for
`operation_id` UUIDv4 shape and `operation_type`/`outcome` codes (V3 fix:
the `operation_id` shape check and the FK's exact target were not
previously spelled out here; see §8.5).

### 8.3 `evidence_operation_history`

Append-only application history, not the general Audit module, mirroring
Q-010's `trading_account_authority_history` shape.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key/order tiebreaker |
| `operation_row_id` | `BIGINT` | not null | unique FK to operation, delete restricted |
| `operation_type` | `VARCHAR(16)` ASCII `ascii_bin` | not null | denormalized copy of `evidence_operation.operation_type` at insert time, needed because a same-table `CHECK` cannot reference another table (V2 fix; see Section 20.1, item 1) |
| `actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 actor |
| `capability` | `VARCHAR(127)` ASCII `ascii_bin` | not null | exact evaluated capability |
| `reason` | `VARBINARY(1000)` | nullable | `NULL` for `RECORD`, 1–1000 bytes for `CORRECT` — both directions enforced by the bidirectional `CHECK` below (V3 fix; see Section 20.1, second correction round: the V2 text claimed the `RECORD → NULL` direction and the byte-length floor were "checked" when the V2 `CHECK` clause did not actually enforce either) |
| `before_status` | `VARCHAR(16)` ASCII `ascii_bin` | nullable | `NULL` for `RECORD` (the record did not exist before); `'ACTIVE'` for `CORRECT` (the target's status immediately before this operation — it can only have been `ACTIVE`, since Section 11.1 requires that check before a new correction proceeds) |
| `after_status` | `VARCHAR(16)` ASCII `ascii_bin` | not null | `'ACTIVE'` for `RECORD` (the new record's initial status); `'SUPERSEDED'` for `CORRECT` (the target's resulting status) — V3 fix: both columns previously had no stated allowed-value relationship to `operation_type`, and `evidence_id`'s ambiguous "resulting/target" wording (§8.2) left it unclear which record's status these columns even described |
| `occurred_at` | `DATETIME(6)` | not null | same operation UTC time |

Constraints/indexes: unique `uk_evidence_history_operation(operation_row_id)`;
FK `operation_row_id` → `evidence_operation.id` delete restricted; index
`idx_evidence_history_time(occurred_at, id)`. The mutation adapter writes
`operation_type` into both `evidence_operation`
and `evidence_operation_history` from the same in-memory value in one
transaction, so the two copies cannot diverge. Four same-table checks (V3
fix; the two `before_status`/`after_status` checks are new this round —
see Section 20.1, third correction round):

```sql
CHECK (operation_type IN ('RECORD', 'CORRECT')),
CHECK (
    (operation_type = 'RECORD' AND before_status IS NULL)
    OR
    (operation_type = 'CORRECT' AND before_status = 'ACTIVE')
),
CHECK (
    (operation_type = 'RECORD' AND after_status = 'ACTIVE')
    OR
    (operation_type = 'CORRECT' AND after_status = 'SUPERSEDED')
),
CHECK (
    (operation_type = 'RECORD' AND reason IS NULL)
    OR
    (operation_type = 'CORRECT'
        AND reason IS NOT NULL
        AND OCTET_LENGTH(reason) BETWEEN 1 AND 1000)
)
```

### 8.4 `evidence_access_log`

Append-only, satisfying Q011-FR-014. Not joined into any mutation
transaction.

| Column | Type | Null/default | Purpose / constraint |
| --- | --- | --- | --- |
| `id` | `BIGINT` | not null, auto | primary key |
| `evidence_id` | `BIGINT` | not null | FK to record, delete restricted |
| `accessing_actor_ref` | `CHAR(36)` ASCII `ascii_bin` | not null | trusted Q-009 actor |
| `accessed_at` | `DATETIME(6)` | not null | UTC |

Constraints/indexes: FK `evidence_id` → `evidence_record.id` delete
restricted; index `idx_evidence_access_log_record(evidence_id,
accessed_at)`.

No table has an update or delete API beyond the two status-related columns
on `evidence_record`, updated only by the correction transaction.

### 8.5 Constraint-to-test traceability

Every database-enforced invariant named in §8.1–§8.4, with the exact
mechanism and the test that must prove it (§16). This table exists so a
column's prose description can never again claim an enforcement the DDL
does not actually provide.

(V3 fix, third correction round: this table's V2/V3-draft version claimed
to cover "every database-enforced invariant" but actually omitted the
`source`/`status`/`operation_type` enum checks, every UUIDv4-shape check,
and the `evidence_operation → evidence_record` FK, and it mis-attributed
the correction-reason byte bound to `evidence_record` when it is actually
enforced on `evidence_operation_history`. Codex caught this by checking
the table against §8.1–§8.4 row by row — exactly the mechanical check this
table's own stated purpose required but its first drafts had not actually
performed. This version was rebuilt by walking every column of every table
in §8.1–§8.4 in order, not by recalling what the table already said.)

**`evidence_record` (§8.1):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `evidence_ref` globally unique | `UNIQUE (evidence_ref)` | §16.4 unique-index test |
| `evidence_ref` canonical `ev-<UUIDv4>` shape | `CHECK` on ref format | §16.1 boundary/format test |
| `recorded_by_actor_ref` canonical UUIDv4 shape | `CHECK` on ref format | §16.1 boundary/format test |
| `source` is an allowed value (`MANUAL` in this Foundation) | `CHECK (source IN ('MANUAL'))` | §16.1 enum test |
| `status` is an allowed value | `CHECK (status IN ('ACTIVE', 'SUPERSEDED'))` | §16.1 enum test |
| `observation_text` is 1–4,000 bytes | `CHECK (OCTET_LENGTH(observation_text) BETWEEN 1 AND 4000)` | §16.1 boundary test |
| At most one correction per target | `UNIQUE (supersedes_id)` (nullable-unique) | §16.4/§16.5 concurrency test |
| `supersedes_id`/`superseded_by_id` reference only existing records, never dangle | self-FK `fk_evidence_record_supersedes`, `fk_evidence_record_superseded_by` → `evidence_record.id`, both `ON DELETE RESTRICT` | §16.4 FK-restrict test |

**`evidence_operation` (§8.2):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| `operation_id` globally unique (idempotency key) | `UNIQUE (operation_id)` | §16.4 unique-index test |
| `operation_id` canonical UUIDv4 shape | `CHECK` on ref format | §16.1 boundary/format test |
| `operation_type` is an allowed value | `CHECK (operation_type IN ('RECORD', 'CORRECT'))` | §16.1 enum test |
| `outcome` is exactly `CREATED`/`CORRECTED`, never `UNCHANGED` | `CHECK (outcome IN ('CREATED', 'CORRECTED'))` | §16.1 enum test |
| `evidence_id` references only an existing record | FK `evidence_id` → `evidence_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

**`evidence_operation_history` (§8.3):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| One history row per operation | `UNIQUE (operation_row_id)` | §16.4 unique-index test |
| `operation_row_id` references only an existing operation | FK `operation_row_id` → `evidence_operation.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |
| `operation_type` is an allowed value | `CHECK (operation_type IN ('RECORD', 'CORRECT'))` | §16.1 enum test |
| `operation_type` matches the parent operation row | denormalized column, written from the same in-memory value in the same transaction | §16.2 application test asserting the two values are always equal on insert |
| `before_status` is `NULL` for `RECORD`, `'ACTIVE'` for `CORRECT` | bidirectional `CHECK` quoted in §8.3 | §16.1/§16.4: assert a `RECORD` row with non-null `before_status` and a `CORRECT` row with non-`ACTIVE` `before_status` are both rejected |
| `after_status` is `'ACTIVE'` for `RECORD`, `'SUPERSEDED'` for `CORRECT` | bidirectional `CHECK` quoted in §8.3 | §16.1/§16.4: assert a `RECORD` row with non-`ACTIVE` `after_status` and a `CORRECT` row with non-`SUPERSEDED` `after_status` are both rejected |
| `reason` is `NULL` for `RECORD`, 1–1,000 bytes for `CORRECT` — **both directions, plus the byte floor** | the bidirectional `CHECK` quoted in §8.3 | §16.1/§16.4: assert a `RECORD` row with non-null `reason`, a `CORRECT` row with null `reason`, and a `CORRECT` row with zero-length (non-null) `reason` are all rejected |

**`evidence_access_log` (§8.4):**

| Invariant | Mechanism | Proving test |
| --- | --- | --- |
| `id` primary key | `PRIMARY KEY (id)` | §16.4 migration test |
| Access-log entries cannot outlive their record | FK `evidence_id` → `evidence_record.id`, `ON DELETE RESTRICT` | §16.4 FK-restrict test |

## 9. Why Correction Does Not Re-Call Q-010

Architecture §9 established that recording validates the subject against
Q-010's recognition check (V3 fix: "recognized," not "eligible" — see §9
there and §20.1 third correction round). Correction (Section 6.2) copies
the target's subject rather than accepting a new one, so a correction can
only ever concern a subject that was recognized when some ancestor record
in its supersession chain was originally recorded. Q-010 references are
never un-recognized once registered — only `MISSING`/temporarily-
`UNAVAILABLE` states change over time, not a settled recognition fact — so
there is no scenario where re-checking recognition at correction time could
even discover something new. Design therefore fixes: correction validates
only structural invariants (target exists, target is `ACTIVE`, reason
present, content bounds), never calls Q-010 at all.

## 10. HTTP API Contract

### 10.1 Endpoints

| Method | Path | Capability | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/evidence` | `evidence:record` | record new Evidence |
| `POST` | `/api/evidence/{evidenceRef}/corrections` | `evidence:correct` | correct an existing record |
| `GET` | `/api/evidence/{evidenceRef}` | `evidence:read` | full-detail read |

Exact path/verb choices are illustrative of the approved shape; final
naming may be adjusted during implementation without reopening this design
provided the capability/use-case mapping is preserved.

### 10.2 Request/response shape

```text
RecordEvidenceRequest {
  operationId: UUID string (required)
  subjectRef: string, "ta-..." (required)
  observationText: string, 1-4000 UTF-8 bytes (required)
}

CorrectEvidenceRequest {
  operationId: UUID string (required)
  correctionReason: string, 1-1000 UTF-8 bytes (required)
  observationText: string, 1-4000 UTF-8 bytes (required)
}

EvidenceDetailResponse {
  evidenceRef, subjectRef, source, status, observationText,
  recordedByActorRef, recordedAt, supersedesRef?, supersededByRef?
}
```

No request DTO accepts an actor field, `EvidenceRef` (for recording),
status, or timestamp — all server-derived. Bean Validation (`@NotBlank`,
`@Size`, `@Pattern` for `operationId`) applies at the controller boundary
per AGENTS.md; domain constructors re-validate regardless, since controllers
never bypass domain invariants.

### 10.3 Q-008 in-process contract

```text
EvidenceProvenanceView confirmProvenance(
    ActorContext actorContext,
    EvidenceRef evidenceRef)
```

An application interface/service owned by Q-011, not a REST endpoint,
mirroring exactly how Q-010 exposes `validateForNewRiskCaseAssociation`.
Q-008 passes its own operation's trusted `ActorContext`; Q-011 independently
requires `evidence:read` before lookup.

## 11. Idempotency, Duplicate, and Retry Design

### 11.1 Ordering principle — authoritative for the whole document (V3 fix;
see Section 20.1, second correction round)

This is the single authoritative statement of execution order for both
recording and correction. Every other section that summarizes or restates
this order (Section 6's table, Sections 12.1/12.2, and Architecture §14)
must match it exactly; if any of them ever appears to diverge, this section
governs and the other must be corrected, not the reverse.

**Exact steps, both recording and correction:**

1. acquire the trusted `ActorContext`;
2. `AuthorizationGuard.requireAllowed` for the exact use case's capability
   (`evidence:record` or `evidence:correct`);
3. require `ActorContext.actorType() == ActorType.HUMAN`;
4. compute the fingerprint from the request's raw semantic fields (Section
   11.2);
5. query `evidence_operation` by `operationId`:
   - same fingerprint found → return the stored result immediately
     (original `CREATED` or `CORRECTED` outcome verbatim) and stop here;
   - different fingerprint found → `EVIDENCE_IDEMPOTENCY_CONFLICT` and stop
     here;
   - not found → continue;
6. (new operation only) validate content bounds;
7. (new `RECORD` only) call Q-010's `validateForNewRiskCaseAssociation`
   using this same `ActorContext`;
8. (new `CORRECT` only) load the target record and verify it is `ACTIVE`;
   `CORRECT` never calls Q-010, for a new operation or a replay;
9. begin the local transaction, recheck `operationId` inside it to close
   the race window, then perform the insert/compare-and-set, the
   `evidence_operation` row, and the `evidence_operation_history` row, and
   commit.

**Steps 1–3 (`ActorContext`, authorization, `HUMAN` type) are never
skipped, including for an exact replay.** Only steps 6–8 (content
validation, the Q-010 call, and the target-status check) are skipped when
step 5 finds an exact replay. This distinction matters for a real security
property: a `SERVICE` actor that somehow held `evidence:record` or
`evidence:correct` must never be able to retrieve a replay result, even of
an operation a `HUMAN` actor legitimately created — so identity/authorization
checks must run before the replay lookup, not after it.

This ordering exists specifically so that a successful operation can always
be safely replayed later regardless of subsequent state drift:

- a correction's target is `SUPERSEDED` immediately after that correction
  succeeds — replaying the same request must not fail an "is target
  ACTIVE" check that was only ever meant to gate a *new* correction;
- Q-010 can become temporarily unavailable after a successful recording —
  replaying the same request must not depend on Q-010 being reachable,
  because nothing about the already-committed fact changed. (Recognition
  itself does not need this protection the way eligibility once seemed to:
  a Q-010 reference is never un-recognized once registered, so recognition
  cannot "drift" the way this bullet originally worried about — but
  temporary unavailability alone is sufficient reason to keep the
  replay check ahead of the Q-010 call.)

### 11.2 Fingerprint construction

`EvidenceFingerprintFactory` does not hash raw JSON. It serializes a fixed
ordered field sequence with the same length-prefixed technique Q-010 uses
(Implementation Design §12 there):

- **record:** domain separator `brokeros-risk:q011:record-fingerprint:v1`,
  subject ref, observation text bytes.
- **correct:** domain separator `brokeros-risk:q011:correct-fingerprint:v1`,
  target `EvidenceRef`, correction reason bytes, new observation text bytes.

### 11.3 Outcomes

| Case | Exact outcome |
| --- | --- |
| same operationId + same fingerprint (record) | return the durably stored result verbatim, including its original `CREATED` outcome; no new row, no Q-010 call |
| same operationId + different fingerprint | `EVIDENCE_IDEMPOTENCY_CONFLICT`; no mutation |
| same operationId + same fingerprint (correct) | return the durably stored result verbatim, including its original `CORRECTED` outcome; no second correction, no target-status check |
| correction targets an already-`SUPERSEDED` record, new operationId | `EVIDENCE_ALREADY_SUPERSEDED`; no mutation |
| lost response after commit | exact replay returns stored result |
| failure before commit | no state/operation/history survives; exact retry may execute |

There is no `UNCHANGED` outcome (Section 4.3, V3 fix — removed in the first
correction round and re-confirmed absent in this second round). Exact
replay is not a third outcome distinct from `CREATED`/`CORRECTED`; it is
the same originally stored outcome returned again. No logical conflict is
automatically retried, matching Q-010's rule verbatim.

### 11.4 Canonical execution-order table (all four use cases)

This table is the single reference for "what runs, in what order, for
which use case." It is generated directly from Section 11.1 and must not
be restated with different content anywhere else in this document, the
Architecture document, or any Codex Prompt; every other place that
describes ordering (Section 6's summary table, Sections 12.1/12.2,
Architecture §14) is a restatement of this table and must match it exactly.

| Step | Record | Correct | Provenance read (Q-008) | Full-detail read |
| --- | --- | --- | --- | --- |
| 1. Acquire `ActorContext` | always | always | always | always |
| 2. Authorize capability | `evidence:record` | `evidence:correct` | `evidence:read` | `evidence:read` |
| 3. Require `ActorType = HUMAN` | always, never skipped | always, never skipped | not required (any authorized actor type) | not required (any authorized actor type) |
| 4. Compute fingerprint | from subject ref + observation text | from target ref + reason + observation text | n/a — no idempotency key | n/a — no idempotency key |
| 5. Query `operationId` for exact replay | always, before content/Q-010 | always, before target-status check | n/a | n/a |
| 6. Validate content bounds | new operation only | new operation only | n/a | n/a |
| 7. Call Q-010 (`validateForNewRiskCaseAssociation`) | new operation only, this actor's own `ActorContext` | **never**, not even for a new operation | never | never |
| 8. Load/verify target record | n/a (creates new) | new operation only; verify `ACTIVE` | load by `EvidenceRef`; recognized/not-found only | load by `EvidenceRef`; recognized/not-found only |
| 9. Begin transaction, recheck `operationId`, mutate, commit | insert `evidence_record`(`ACTIVE`) + `evidence_operation` + `evidence_operation_history` | insert new `evidence_record`(`ACTIVE`) + update target(`SUPERSEDED`) + `evidence_operation` + `evidence_operation_history` | none (read-only) | insert `evidence_access_log` row in its own short, **not** database-read-only transaction, then return content |

"n/a" means the step does not apply to that use case at all, distinct from
"skipped on replay," which applies only to steps 6–8 for `Record`/`Correct`
when step 5 finds an exact match.

## 12. Transaction and Concurrency Design

### 12.1 Recording transaction

Per Section 11.1, the idempotency check precedes all other work:

1. authorization (`evidence:record`) and `HUMAN` check occur before any
   Q-011 or Q-010 access;
2. query operation ID; an exact completed replay (same fingerprint of the
   *request as submitted*, computed from raw input before further
   validation) returns the stored result immediately — skip to step 9. A
   different fingerprint under the same operation ID conflicts immediately.
   Otherwise continue;
3. content validation (bounds/characters);
4. Q-010 subject validation using the recording actor's `ActorContext`;
5. begin one local InnoDB transaction;
6. recheck operation ID inside the transaction (closes the race window
   between step 2 and step 5);
7. generate `EvidenceRef`; insert `evidence_record` row (`ACTIVE`);
8. insert `evidence_operation` and `evidence_operation_history` rows
   (`outcome = CREATED`); and
9. commit; then build/emit the response (the stored result on replay, or
   the newly committed result otherwise).

The fingerprint is computed from the request payload alone (Section 11.2),
so step 2's replay check never needs a database join beyond
`evidence_operation` and never needs the target record, Q-010, or content
re-validation to execute.

### 12.2 Correction transaction

Same idempotency-first ordering as Section 12.1, substituting Q-010
revalidation with the structural checks from Section 9:

1. authorization (`evidence:correct`) and `HUMAN` check;
2. query operation ID; an exact completed replay returns the stored result
   immediately — skip to step 8, and in particular do **not** re-check the
   target's current status, which is expected to already be `SUPERSEDED`
   after the first successful correction. A different fingerprint under the
   same operation ID conflicts immediately. Otherwise continue;
3. content validation (bounds/characters on the new observation text and
   the correction reason);
4. begin one local InnoDB transaction;
5. load and lock the target row; verify status `ACTIVE` — this check
   applies only to a genuinely new correction, never to a replay;
6. compare-and-set target `status = SUPERSEDED`,
   `superseded_by_id = <new row's future id>` — implemented as insert new
   row first (obtaining its generated `id`), then update the target, both
   inside the same transaction, so the FK is always valid;
7. insert `evidence_operation` and `evidence_operation_history` rows
   (`outcome = CORRECTED`); and
8. commit; then build/emit the response (the stored result on replay, or
   the newly committed result otherwise).

### 12.3 Races

- Concurrent corrections targeting the same not-yet-superseded record race
  on the target's compare-and-set (`WHERE id = ? AND status = 'ACTIVE'`);
  exactly one succeeds. The loser rolls back, then re-reads the target: if
  its own operation ID committed concurrently, replay rules apply;
  otherwise it returns `EVIDENCE_ALREADY_SUPERSEDED`.
- Generated-`EvidenceRef` collisions are identified only by the named
  unique constraint and MySQL error 1062; a full rolled-back transaction may
  retry with a new generated ref up to three attempts.
- `MySqlEvidenceConstraintClassifier` inspects the root `SQLException`,
  SQLState, and exact named constraint. Unknown duplicate/integrity errors
  fail as authority unavailable rather than being misclassified as
  compatible replay — the same discipline `MySqlAuthorityConstraintClassifier`
  already established for Q-010.

### 12.4 History and access-log failure

An `evidence_operation`/`evidence_operation_history` insert failure during
recording/correction marks the transaction rollback-only — tests force this
and prove the record row is absent/unchanged afterward. An
`evidence_access_log` insert failure during a full-detail read fails that
read closed (Section 6, Architecture §11.2) but never affects a concurrent
mutation transaction — they are always separate transactions.

## 13. Error, ResultCode, and Exception Model

| Condition | ResultCode | HTTP |
| --- | --- | ---: |
| invalid `EvidenceRef` / malformed request | `EVIDENCE_REQUEST_INVALID` | 400 |
| invalid observation text / correction reason bounds | `EVIDENCE_CONTENT_INVALID` | 400 |
| subject `NOT_RECOGNIZED` by Q-010 (V3 fix: `RECOGNIZED_NOT_ELIGIBLE` is accepted, not an error) | `EVIDENCE_SUBJECT_NOT_RECOGNIZED` | 422 |
| Q-010 dependency unavailable during recording | `EVIDENCE_SUBJECT_AUTHORITY_UNAVAILABLE` | 503 |
| exact replay | stored success result | 200-equivalent |
| same operationId, different fingerprint | `EVIDENCE_IDEMPOTENCY_CONFLICT` | 409 |
| correction target not found | `EVIDENCE_NOT_FOUND` | 404 |
| correction target already `SUPERSEDED` | `EVIDENCE_ALREADY_SUPERSEDED` | 409 |
| Q-009 actor/capability denial | existing `ACTOR_ACCESS_DENIED`/`AUTHORIZATION_DENIED` | 403 |
| actor type is not `HUMAN` | `EVIDENCE_ACTOR_TYPE_NOT_PERMITTED` | 403 |
| Q-009 authority unavailable | existing `SECURITY_DEPENDENCY_UNAVAILABLE` | 503 |
| MySQL unavailable / unknown integrity / ref-retry exhausted | `EVIDENCE_AUTHORITY_UNAVAILABLE` | 503 |
| history or access-log insert failure | rollback/deny, then `EVIDENCE_AUTHORITY_UNAVAILABLE` | 503 |

No error includes observation text, correction reason, subject ref detail
beyond the identifier itself, SQL, or stack trace.

## 14. Logging, Sensitive Data, and Observability

### 14.1 Logging rules

Allowed: event name, operation type, outcome, safe ResultCode, operation
ID, generated/target `EvidenceRef`, request/trace ID, UTC duration/time.

Never log: `observation_text`, `correction_reason`, or any excerpt/hash of
either that could reconstruct content; `subject_ref` beyond the identifier
already safe to log per Q-010's own convention; actor claims beyond
`ActorRef`.

### 14.2 Metrics

Reuse the existing Micrometer/Actuator platform:

- `brokeros.risk.evidence.operations` tagged by operation and outcome;
- `brokeros.risk.evidence.conflicts` tagged by safe category;
- `brokeros.risk.evidence.authorization.denied` tagged by capability;
- `brokeros.risk.evidence.access.reads` tagged by outcome; and
- `brokeros.risk.evidence.duration` tagged by operation.

No metric tag contains refs, actors, or content.

## 15. Security Design Review

| Threat | Required implementation control |
| --- | --- |
| unauthorized recording/correction | Q-009 authorization before any Q-011 access; `HUMAN`-only domain check |
| `SERVICE` actor posing as automated source | explicit `ActorType` check independent of capability grant |
| subject-swap via correction | subject copied from target, never accepted as a correction input (Section 6.2) |
| branching correction chain | nullable-unique `supersedes_id` constraint |
| existence probing | authorization before lookup on both read contracts |
| sensitive content leakage via logs/metrics | Section 14 bounds; `VARBINARY` storage avoids implicit charset logging surprises |
| undetected bulk content reads | every full-detail read commits an access-log row before content returns |
| replay/idempotency abuse | operation ID + fingerprint; changed replay conflicts |
| direct DB bypass/corruption | no delete API/SQL; restrict FKs; runtime fails closed on inconsistent cardinality |

Implementation review must statically prove `domain`/`application` contain
no Spring Security, Servlet, JDBC, vendor, Redis, or Kafka imports and no
actor/header bypass strings.

## 16. Test Design

### 16.1 Domain unit tests

- `EvidenceRef`/content value parsing and boundary rejection (0, 4000,
  4001 bytes; 0, 1000, 1001 bytes; control/NUL characters);
- status transition invariants: only `ACTIVE → SUPERSEDED`, exactly once;
- fingerprint golden vectors and one-field change sensitivity.

### 16.2 Application-service tests

Use fake ports as focused test doubles only:

- every use case invokes `AuthorizationGuard` before any Q-011 port;
- recording additionally verifies `ActorType == HUMAN` before content
  validation and before any Q-010 call;
- Q-010 validation is invoked with the recording actor's own `ActorContext`
  (not a fabricated/service one) — verified by mock argument capture;
- only `NOT_RECOGNIZED` rejects recording without creating Evidence;
  `RECOGNIZED_NOT_ELIGIBLE` is accepted (V3 fix — test must assert this
  explicitly, since V1/V2 incorrectly required rejecting it);
- correction with a not-found or already-`SUPERSEDED` target is rejected;
- correction never calls the Q-010 port (Section 9) — verified by
  zero-interaction assertion;
- exact replay, conflicting replay, and idempotency outcomes for both
  record and correct; and
- full-detail read commits the access-log write before returning content;
  a forced access-log failure returns no content.

### 16.3 Q-009/Q-010 integration tests

- denial/unavailability of `evidence:*` capabilities yields zero Q-011 data
  access;
- denial/unavailability of `trading-account-reference:read` during
  recording yields zero Evidence creation;
- `SERVICE`-actor context with `evidence:record` granted is still rejected
  by the `HUMAN`-only check.

### 16.4 Real MySQL 8.4 migration/persistence tests

Disposable MySQL 8.4.11 through mandatory test datasource inputs, no
mandatory gate skipped. Verify:

- clean `V1→V2→V3→V4` and existing-baseline upgrade, Flyway
  validate/restart/checksum;
- exactly four new tables, no data seed/destructive DDL;
- every row of §8.5's constraint-to-test table, table by table: primary
  keys; `evidence_ref`/`recorded_by_actor_ref`/`operation_id` UUIDv4-shape
  checks; `evidence_record.source` and `evidence_record.status` enum
  checks; `evidence_operation.operation_type` and `outcome` enum checks;
  `evidence_operation_history.operation_type` enum check; content
  byte-length checks on both `observation_text` (1–4,000) and
  `evidence_operation_history.reason` (1–1,000, `CORRECT` only) — not
  conflated with each other or attributed to the wrong table (V3 fix);
- nullable-unique `supersedes_id` behavior: two different records may each
  have `supersedes_id IS NULL`; two records cannot share the same non-null
  `supersedes_id`;
- self-FK restrict-delete behavior;
- both directions of the bidirectional `reason` check (§8.3, §8.5, V3 fix):
  a `RECORD` history row with a non-null `reason` is rejected, a `CORRECT`
  history row with a null `reason` is rejected, and a `CORRECT` history row
  with a zero-length (non-null) `reason` is rejected;
- the `outcome` check accepts only `CREATED`/`CORRECTED` and rejects
  `UNCHANGED` and any other value; and
- query plans use the unique/index paths and do not full-scan.

### 16.5 Transaction and concurrency tests

- two independent transactions correct the same target after a barrier:
  exactly one commit, the database unique constraint (not a precheck)
  elects the winner;
- concurrent same-operationId/same-fingerprint requests return one commit
  plus one replay;
- forced generated-ref collision retries at most three times and never
  overwrites;
- install a disposable test-only trigger that fails
  `evidence_operation_history` insert, then prove the record/operation rows
  roll back; remove the trigger afterward; and
- a forced `evidence_access_log` failure during a full-detail read returns
  no content and does not affect a concurrent, unrelated recording
  transaction.

### 16.6 Q-008 consumer/security tests

- recognized/not-found outcomes only; response type contains no
  `observation_text` or `correction_reason` field by static/reflective
  inspection;
- unauthorized/missing/revoked `evidence:read` calls no Q-011 query; and
- Q-008 test code has no Q-011 repository/table import.

### 16.7 Regression and architecture tests

- existing Q-009/Q-010 tests continue passing unchanged;
- package dependency test prohibits infrastructure/framework imports from
  `com.brokeros.risk.evidence.domain`/`application`;
- static scan proves no delete SQL, migration edits to `V1`–`V3`, permissive
  provider, or raw content logging; and
- Maven dependency tree remains unchanged.

## 17. Flyway and Rollout Plan

```text
V4__create_evidence_provenance_foundation.sql
```

One coherent forward-only additive migration after committed `V1`–`V3`. It
creates the four tables in dependency order: `evidence_record`,
`evidence_operation`, `evidence_operation_history`, and
`evidence_access_log`. It inserts no data and does not edit `V1`–`V3`.

Future rollout after all separate approvals:

1. apply/validate `V4` on disposable MySQL 8.4.11 and then the target
   database;
2. grant `evidence:record`, `evidence:correct`, `evidence:read`, and
   `trading-account-reference:read` to the intended operator role(s)
   through existing Q-009 provisioning;
3. enable the protected HTTP endpoints; and
4. enable the Q-008 consumer contract only when Q-008 is separately
   authorized to call it.

## 18. Recommended Future Implementation Sequence

Only after Design approval and explicit implementation authorization:

1. add Q-011 ResultCodes, domain values, status transitions, fingerprint,
   and unit tests;
2. add future Flyway `V4` plus static and disposable MySQL migration tests;
3. implement query/mutation/access-log ports and JDBC transaction adapters;
4. implement recording service, Q-010 integration, and idempotency/
   concurrency/history tests;
5. implement correction service and CAS/rollback tests;
6. implement the HTTP controller and DTOs;
7. implement the Q-008 provenance facade and contract tests without wiring
   or modifying Q-008;
8. run complete Maven, MySQL 8.4, Flyway, security, static, Compose,
   Kustomize, and concurrency gates with zero mandatory skip; and
9. update Skills/Lessons from actual verified implementation and create the
   final Review Package.

This ordering is a design, not authorization and not a commit plan.

## 19. Requirement and Acceptance Traceability

| Requirement | Design coverage |
| --- | --- |
| Q011-FR-001 | Sections 4.1, 7.4, 8.1 |
| Q011-FR-002 | Sections 6.1, 9, 12.1 |
| Q011-FR-003 | Sections 4.1, 8.1, 10.2 |
| Q011-FR-004 | Section 4.1 (`EvidenceSource` fixed to `MANUAL`) |
| Q011-FR-005 | Sections 5.1, 15 |
| Q011-FR-006 | Section 8.1 (no update path beyond status/pointer) |
| Q011-FR-007 | Sections 6.2, 9, 12.2 |
| Q011-FR-008 | Sections 8.1, 12.3 |
| Q011-FR-009 | Sections 11, 12 |
| Q011-FR-010 | Sections 10.3, 6 |
| Q011-FR-011 | Sections 13, 16.2 |
| Q011-FR-012 | Sections 8.3, 12 |
| Q011-FR-013 | Sections 8, 17 (no Kafka/Redis/permissive provider) |
| Q011-FR-014 | Sections 7.3, 10.1, 12.4 |

All fifteen Q-011 Acceptance Criteria have a concrete design home: approved
governance remains separate; the reference cannot derive from external
data; correction integrity is a structural, not just validated, property;
Q-009/Q-010 protect and are reused unchanged; history and access are
atomic/self-auditing; future schema is additive; and runtime verification
remains mandatory rather than claimed here.

## 20. Design Gaps and Outstanding Decisions

### 20.1 Design gaps found and fixed (V1 → V2)

V1 claimed no design gaps. That claim was wrong. Codex began implementing
V1 exactly as instructed, read all four governing documents, and halted
before writing any code because it found six concrete, real contradictions
V1's self-review had not caught. This section records them honestly rather
than re-asserting "none."

1. **Cross-table `CHECK` constraint could not be expressed.** V1's §8.3
   required `evidence_operation_history` to enforce "reason mandatory for
   `CORRECT`" by checking `evidence_operation.operation_type`, but MySQL
   `CHECK` constraints cannot reference another table. **Fixed** by
   denormalizing `operation_type` onto `evidence_operation_history` itself
   (§8.3), written from the same in-memory value as the operation row in
   the same transaction, enabling a same-table check.
2. **Correction's replay ordering contradicted itself.** V1's §12.2 checked
   "target status is `ACTIVE`" before handling idempotent replay, but a
   target is `SUPERSEDED` immediately after its first successful
   correction — so replaying that exact same request would have been
   incorrectly rejected. **Fixed** by making operation-ID/fingerprint replay
   detection the first check for both use cases (new §11.1), explicitly
   bypassing the target-status check on replay (§12.2, revised).
3. **Recording revalidated Q-010 before checking for replay.** V1's §12.1
   called Q-010 (step 3) before the operation-ID replay check (step 4),
   so a legitimate replay would re-run Q-010 validation and could fail if
   the subject later became ineligible or Q-010 was temporarily
   unavailable — even though the original request had already succeeded.
   **Fixed** by the same §11.1 ordering principle: replay check first,
   Q-010 call only for genuinely new operation IDs (§12.1, revised).
4. **`EvidenceOperationOutcome.UNCHANGED` was self-contradictory and
   unused.** V1's §4.3 defined exact replay as producing `UNCHANGED`; V1's
   §11 (now §11.3) required returning the original `CREATED`/`CORRECTED`
   result instead; and no described scenario ever actually produced
   `UNCHANGED` — it was carried over from Q-010's design out of habit
   without checking whether Evidence has an equivalent scenario (it does
   not: every genuinely new operation ID always produces a genuinely new
   record). **Fixed** by removing `UNCHANGED` entirely; exact replay
   returns the originally stored `CREATED`/`CORRECTED` outcome verbatim.
5. **The full-detail-read transaction was mislabeled "read-only."** V1's
   Architecture §13 called it a "read-only transaction" while requiring it
   to write the access-log row. **Fixed** in Architecture §13 and this
   Design's §2: it is a short, dedicated transaction, explicitly not
   database-level read-only.
6. **Table count was internally inconsistent.** V1's §2 summary said
   "three" tables; §8.1–§8.4 and the Codex Prompt itself correctly
   specified four. **Fixed**: every reference now says four
   (`evidence_record`, `evidence_operation`, `evidence_operation_history`,
   `evidence_access_log`).

Ownership, content immutability, the status model's two values, the
subject-validation mechanism, the V1 eligibility scope, and the two-tier
read-contract boundary were unchanged by the V1→V2 round. The V2→V3 round
below did touch other sections — the round-1 fix was applied only to the
specific sections Codex had cited, not swept across every place that
restated the same rules, which is exactly what caused round 2.

### 20.2 Design gaps found and fixed (V2 → V3, second round)

Codex resumed against the approved, authorized V2, read all four governing
documents plus the resume Prompt in full, and halted a second time before
writing any code — reporting five further real inconsistencies. Four were
places elsewhere in V2 that still described the pre-fix behavior from round
one (this session's round-1 correction pass patched only the sections Codex
had specifically cited); the fifth was a genuinely new gap in the history
table's `CHECK` constraint.

1. **Section 6's use-case table and rules still described the V1 ordering
   and still showed `UNCHANGED`.** §6's table (`record`/`correct` rows) and
   §6.1/§6.2's bullets restated content-validation-then-Q-010-then-replay
   ordering and `CREATED`/`UNCHANGED`, `CORRECTED`/`UNCHANGED` outcomes —
   all fixed in §11 during round one, but §6 was never updated to match.
   **Fixed**: §6's table and bullets now match §11.1 exactly, and both
   sections state that if they ever appear to diverge again, §11.1 governs.
2. **The exact ordering of authorization/`HUMAN`-check relative to the
   replay check was never stated explicitly, and a resume Prompt guessed
   wrong.** §11.1 (round one) said the replay check runs "before content
   validation, the Q-010 call, and any target-status check" but never said
   where authorization and the `HUMAN`-type check fit — they were already
   correctly placed first in §12.1/§12.2's numbered steps, but nothing
   forced a reader (including the Prompt-writer) to notice that placement
   was deliberate rather than incidental. **Fixed**: §11.1 is rewritten as
   the single authoritative, explicitly numbered statement — authorization
   and the `HUMAN` check are step 1–3, always run, never skipped, even on
   replay; only content validation, the Q-010 call, and the target-status
   check (steps 6–8) are skipped on an exact replay match — with an
   explicit security rationale (a `SERVICE` actor must never retrieve a
   replay result). §11.4 adds a canonical execution-order table covering
   all four use cases so this cannot be independently restated incorrectly
   again.
3. **Architecture §14 still had the entire V1 ordering.** The round-one fix
   touched Implementation Design §11/§12 and Architecture §13's "read-only"
   wording, but never touched Architecture §14, which still listed content
   validation and the Q-010 call before the replay check. **Fixed**:
   Architecture §14 rewritten to match §11.1/§11.4 exactly, with an inline
   note explaining the correction (Architecture is now V2; see that
   document's own Document Status).
4. **Design §2's summary line, and Architecture §9's prose, both still
   implied correction calls Q-010.** §2 said Q-010 is "called with the
   recording/correcting actor's own `ActorContext`"; Architecture §9 said
   "correction reuses the same validation using the correcting actor's own
   context." Both contradicted §5.1/§6.2/§9(Implementation Design)'s
   correct statement that correction never calls Q-010. **Fixed**: §2 now
   says "recording actor's own `ActorContext`... correction never calls
   Q-010"; Architecture §9's paragraph is rewritten to state plainly that
   correction never calls Q-010, full stop, not merely "does not need to."
5. **The history table's `reason` constraint was under-specified and only
   partially enforced.** V2's `CHECK` enforced `CORRECT → reason NOT NULL`
   but not `RECORD → reason IS NULL`, and did not reject a zero-length
   (empty-but-non-null) `CORRECT` reason, even though the column's own
   prose description claimed both were "checked." **Fixed**: §8.3's check
   is now a single bidirectional expression enforcing both directions plus
   the 1–1000 byte floor/ceiling for `CORRECT`; §8.5 adds a full
   constraint-to-test traceability table so no column description can again
   claim an enforcement the DDL does not provide; §16.4 lists both
   directions as required test assertions.

### 20.3 Design gaps found and fixed (V3 → V4, third round)

Codex resumed against approved, authorized V3 and produced a formal
blocker report (`review/q-011/q-011-v3-implementation-blocker-report-20260828-191130.md`)
before writing any code, this time in writing rather than only in chat —
finding four further real defects, plus the Product Owner independently
asked this audit to verify `before_status`/`after_status` consistency,
which surfaced a fifth.

1. **Requirement `Q011-FR-002` and Architecture/Design directly
   contradicted each other on the subject bar.** The approved Requirement
   V2 states Evidence needs only a Q-010-*recognized* subject — explicitly
   a lower bar than "eligible for a new Risk Case association." Architecture
   §9 and Design §6.1 nonetheless required the stricter
   `ELIGIBLE_FOR_NEW_ASSOCIATION` bar, rejecting `RECOGNIZED_NOT_ELIGIBLE`
   subjects (e.g. inactive/retired accounts) — a silent narrowing of an
   approved Requirement, which AGENTS.md's Requirements Discipline and this
   Design's own §1.1 priority order both prohibit. This was not a wording
   slip; it required a Product Owner decision, since it changes what
   Evidence recording actually permits. **The Product Owner confirmed on
   2026-08-28: the Requirement's "recognized" bar stands.** Architecture §2
   and §9, and Design §1.3/§6.1/§9/§11.1/§11.4/§13/§16.2, are corrected to
   accept both `ELIGIBLE_FOR_NEW_ASSOCIATION` and `RECOGNIZED_NOT_ELIGIBLE`;
   only `NOT_RECOGNIZED` rejects. The ResultCode is renamed
   `EVIDENCE_SUBJECT_NOT_RECOGNIZED`.
2. **Design §5.2 overclaimed that every use case requires `HUMAN`.** It said
   "every protected Q-011 use case is triggered by an authenticated
   `HUMAN`," contradicting §11.4, which correctly requires `HUMAN` only for
   Record/Correct (matching Requirement `Q011-FR-005` exactly) and not for
   either read use case. **Fixed**: §5.2 rewritten to state it is describing
   the authentication *boundary* (HTTP/in-process, not a non-web command),
   not the required actor *type*, and to say explicitly that `HUMAN` applies
   only to Record/Correct.
3. **§21's Architecture status line was not updated when Architecture V2 was
   approved.** The Architecture document's own Document Status and
   Architecture Gate correctly said `APPROVED`, but this Design's §21 still
   said "Architecture: V2 — pending Product Owner approval" — a leftover
   from an earlier edit that replaced only the bullets below it. **Fixed**:
   both documents' gate sections are now kept in the same edit whenever
   either changes.
4. **§8.5 was not actually exhaustive despite claiming to be.** Walking
   §8.1–§8.4 column by column (rather than recalling what §8.5 already said)
   found it was missing: the `source` and `status` enum checks on
   `evidence_record`; UUIDv4-shape checks on `evidence_ref`,
   `recorded_by_actor_ref`, and `operation_id`; the `operation_type` enum
   check on both `evidence_operation` and `evidence_operation_history`; and
   explicit FK-target rows for `evidence_operation → evidence_record` and
   `evidence_operation_history → evidence_operation`. It also mis-attributed
   the 1–1,000 byte correction-reason bound to `evidence_record` (that bound
   belongs to `evidence_operation_history.reason`; `evidence_record` only
   has the 1–4,000 byte `observation_text` bound). **Fixed**: §8.5 rebuilt
   as four separate per-table lists, one row per actual constraint, cross-
   checked against §8.1–§8.4 directly.
5. **(Found by this audit, not by Codex) `evidence_id`'s target was
   ambiguous, and `before_status`/`after_status` had no stated relationship
   to `operation_type`.** §8.2 described `evidence_operation.evidence_id` as
   "FK to resulting/target record" without saying which one for `CORRECT`
   (the new record, or the one being superseded?) — and without resolving
   that, `before_status`/`after_status` (§8.3) could not be verified as
   correct or even meaningful. **Fixed**: `evidence_id` now explicitly names
   the target (superseded) record for `CORRECT` and the new record for
   `RECORD`, making `before_status`/`after_status` a real transition
   (`NULL→ACTIVE` for `RECORD`, `ACTIVE→SUPERSEDED` for `CORRECT`), enforced
   by two new bidirectional `CHECK` clauses and traced in §8.5.

### 20.4 Reusable lesson, round three: a claim of completeness needs a mechanical check, not a confident sentence

Round two's own reusable lesson (§20.5, formerly §20.4) already said "grep,
not memory." This round's §8.5 defect shows that lesson was not fully
internalized: §8.5 was written as a table that *read* as complete without
actually being checked column-by-column against §8.1–§8.4. Writing "this
table exists so no gap can happen again" is not the same as verifying no
gap currently exists. The fix applied here — read every column of every
table in source order and produce one traceability row per constraint,
rather than transcribing remembered highlights — is the only way a
completeness claim is actually true rather than merely confident-sounding.

Separately: a Requirement/Architecture contradiction (finding 1) is a
different class of problem from a wording or completeness defect (findings
2–5). The first changes what the system is allowed to do and needs a
Product Owner decision; the rest are corrections a reviewer can make and
report. Conflating "found a contradiction" with "therefore I should decide
it" would have been its own defect — this round asked before touching
scope, and fixed everything else without asking.

### 20.5 Reusable lesson, round one (unchanged from V2)

A self-review that reads a design and checks it for stated internal
consistency is not the same test as actually trying to build it. All six
V1 defects were exactly the kind of thing that only surfaces when someone
works through concrete execution order, constraint scope, and replay
semantics field by field — which is what Codex did by attempting
implementation, and what the earlier self-review pass did not do. Treat an
implementer halting with a precise, cited objection as a successful use of
the process, not a failure to work around.

### 20.6 Reusable lesson, round two: fix the rule everywhere it is stated, not just where it was cited

Round one's mistake was treating each Codex-cited line as an isolated typo
rather than searching the whole document set for every other place the same
rule was restated. A design that states one rule in five places (a summary
table, detailed steps, a security-integration table, an Architecture
mirror, and a resume Prompt) needs a fix applied to all five, verified by
an explicit text search, not just to the one place a reviewer happened to
cite. This document now designates §11.1/§11.4 as the single authoritative
statement precisely so this class of drift has one place to fix next time,
and Section 20.5 below records a full-text scan proving the current state
is actually consistent rather than asserting it.

### 20.7 Full-text consistency scan (V3, retained; V4 scan follows in the change summary delivered separately)

Performed against this document and the Architecture document immediately
before submitting V3 for approval. Commands and results:

- `grep -n "UNCHANGED"` — the only remaining matches are inside this
  §20 history (describing what was removed) and the explicit "there is no
  `UNCHANGED` outcome" statements in §4.3/§11.3; no table, DDL, or ordering
  description contains it as a live value.
- `grep -n "correcting actor.*Q-010\|correct.*call.*Q-010\|Q-010.*correct"` —
  every remaining match states correction does **not** call Q-010; none
  imply that it does.
- Ordering description search across §2, §6, §11.1, §11.4, §12.1, §12.2,
  and Architecture §9/§14 — all now state: authorization + `HUMAN` check
  first (never skipped) → replay check → (new operations only) content
  validation → (new `RECORD` only) Q-010 call → (new `CORRECT` only)
  target-`ACTIVE` check → transaction/commit. No section states a different
  order.
- `grep -n "read-only transaction"` — the only match is Architecture §13's
  negative statement ("...is **not** a database-level read-only
  transaction...", explaining what it is not and why); no remaining text
  asserts the full-detail-read transaction positively as read-only. The
  Implementation Design's execution-order table (§11.4) separately and
  correctly marks the Provenance-read use case as genuinely read-only (it
  performs zero writes) — that is a different use case from the
  full-detail read and is not the defect that was fixed.
- `grep -n "three.*table\|table.*three"` — zero matches; every reference to
  table count says four.

### 20.8 Deployment inputs, not design gaps

- actual capability grants for the intended Evidence-recording operator
  role(s); and
- target-environment database credentials/change window.

### 20.9 Future Requirement scope

(V5 fix, fourth correction round: this list previously still named
"inactive-subject Evidence" as future scope, contradicting the round-three
fix that made it in-scope now — see §20.4/round three above. Removed.)

Automated sources, additional subject types,
polarity/severity classification, attachments, and retention/redaction
policy remain future Requirements (Architecture §22).

### 20.10 Fourth governance-consistency round (all four documents)

Performed 2026-08-28 against an explicit, pre-written, mechanical
governance-consistency task
(`prompts/Q-011-V11-Fourth-Governance-Consistency-Correction-Prompt.md`)
that independently specified required `rg` searches and a required
per-document review, rather than relying on recollection of what round
three had already fixed. Every predicted finding was verified against the
actual file content before being treated as real. Findings, by document:

**Requirement** (→ V3 candidate): Goal 5 still overclaimed `HUMAN` for
every protected use case, contradicting its own `Q011-FR-005`. Present
since V1; never caught because the V1→V2 review (§18) checked internal
consistency of the sections it read closely, not a full Goal-vs-FR
cross-reference. Fixed; see Requirement §19.

**ADR-013** (→ amendment, pending re-acceptance): the most serious gap.
ADR-013 was accepted 2026-08-28 *before* Architecture's round-three
subject-bar fix, and was never revisited afterward — it still required
`ELIGIBLE_FOR_NEW_ASSOCIATION` only, throughout its Decision, Alternatives,
Consequences, and Deferred Decisions sections, directly contradicting the
now-corrected Architecture and this Design. An **accepted ADR does not
become correct automatically when its Requirement/Architecture inputs are
later corrected — it must be explicitly amended and re-accepted.** This is
the clearest instance in this task's history of a fix not propagating to
every document that stated the rule, because ADR-013 was not touched at
all during round three (round three's own scan covered only Architecture
and this Design). Also fixed: ADR-013's Consumer Boundary section had
conflated "Q-008 cannot use the full-detail contract" (a real,
consumer-specific limitation) with "no automated actor can use the
full-detail contract" (never true — only `HUMAN` is restricted, and only
for recording/correction). See ADR-013's own Amendment section for the
complete correction; it requires separate Product Owner re-acceptance and
is not self-accepted here.

**Architecture** (→ V4 candidate): §23 (Required Architecture Review
Answers) item 15 still listed "inactive-subject Evidence" as future scope
after §22 and §9 had already brought it in scope — a restatement §22's own
round-three fix did not reach. Item 17 made a point-in-time "Implementation
authorized: No" claim that had already gone stale in both directions
across rounds three and four; reworded as an evergreen principle that
points to §24 for the actual current state instead of asserting a snapshot
that will drift again.

**This document** (→ V5 candidate): §1.1's priority-order list named
specific version numbers ("Architecture, currently V2," "this Design,
currently V3") that were already wrong by the time this round started;
fixed by removing hard-coded version numbers from that list entirely and
pointing to each document's own Document Status instead, so this exact
defect class cannot recur here. §20.9 still listed inactive-subject
Evidence as future scope, the same leftover as Architecture §23 item 15.
§21 contained two separate, contradictory "Next gate" paragraphs — one
telling Codex to proceed immediately, the other saying approval was still
pending — direct, undeniable evidence of an incomplete status update
(exactly the failure mode named in round two's own lesson, §20.6/20.4).

**Not reopened:** the execution ordering in §11.1/§11.4 did not change —
authorization and the `HUMAN` check still precede the replay check and are
never skipped; `RECORD` replay still never re-calls Q-010; `CORRECT`
replay still skips the target-status check; `CORRECT` still never calls
Q-010 at all; both read use cases still require no `HUMAN`; the full-detail
read is still a short, dedicated, non-database-read-only transaction. No
new implementation-authorization decision is made by this round.

**Reusable lesson:** four rounds in, the recurring root cause is the same
each time — a correction is applied to the specific location a reviewer
(self, Codex, or an external task) pointed at, without a systematic search
for every other place the same fact is restated, including in sibling
documents. A subject-bar decision lives in a Requirement FR, an Architecture
decision section, a Design rule, *and* an accepted ADR — fixing three of
four and treating the task as done is how this kept recurring. The
practical fix going forward: after any substantive decision correction,
search across **all** governing documents for the terms involved, not just
the one being edited.

## 21. Design Gate

- Requirement: **V3 — APPROVED — 2026-08-28 — Product Owner** (Goal 5
  fix, round four; see Requirement §19/§20)
- Architecture: **V4 — APPROVED — 2026-08-28 — Product Owner** (§23 items
  15/17 fixes, round four; see Architecture's own Document Status)
- ADR-013: accepted (original) 2026-08-28; **amendment RE-ACCEPTED —
  2026-08-28 — Product Owner** (subject-bar and consumer-boundary
  correction, round four; see ADR-013's own Amendment section)
- Implementation Design version: **V5 — APPROVED — 2026-08-28 — Product
  Owner** (V1→V2→V3→V4 history above/§20.1–§20.3; this V5 fixed §1.1's
  stale version references and §20.9's stale future-scope item, round
  four, §20.10 below)
- Implementation: **AUTHORIZED — 2026-08-28 — Product Owner, against
  Requirement V3/Architecture V4/ADR-013-as-amended/this Design V5**
- Implementation Allowed: **YES**

Next gate: Codex executes the resume Prompt issued 2026-08-28, built
strictly from §11.1/§11.4 of this document, then Claude Code performs an
independent implementation review of the resulting non-overwriting review
package before the Product Owner considers any commit.
