# Q-015 Phase A Architecture Review

## Governing decisions inspected

- `docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md`.
- `docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md` V1, approved for
  implementation on 2026-09-06.
- `docs/architecture/q-015-phase-a-ingestion-architecture.md` V1.
- `docs/adr/ADR-023-phase-a-payload-opaque-ingestion.md` (Accepted).
- `docs/architecture/q-015-phase-a-ingestion-implementation-design.md` V1.
- `prompts/Q-015-Phase-A-Implementation-Prompt.md` (cleared).
- Root `AGENTS.md`, both engineering authority documents, applicable repository
  standards/skills, and recent Lessons Learned.

## Architecture findings

The change remains one Spring Boot modular monolith. The new module separates its
domain envelope, application use case and ports, infrastructure adapters, and REST
DTO/controller. Its core has no MT4/MT5 Manager API, gateway, native adapter,
canonical trading field, or external database dependency. Platform tags are
metadata only and payload bytes remain opaque.

The ingestion sequence is authorization, actor-type enforcement, envelope
validation, prior sequence lookup, idempotent insert, optional visible gap marker,
then bounded synchronous Kafka publication. New rows and gap markers use the local
JDBC transaction; a publication failure rolls them back. A duplicate returns a
successful `DUPLICATE` outcome and is not published again. Kafka topic and key are
exactly `trading-data.canonical` and `tradingAccountId`, as decided by ADR-023.

V9 creates only the two approved application tables. The event table uses `BLOB`
rather than `VARBINARY(65535)` because both are permitted opaque binary storage and
`BLOB` avoids the InnoDB inline row-size problem at the exact 65,535-byte bound.
The schema retains all rows, enforces the plain global source/sequence uniqueness,
and indexes account/time lookup.

V1 is not time-partitioned. This is the explicit Implementation Design/prompt
fallback: MySQL requires every unique key on a partitioned table to contain the
partition expression, which would make `(source_server_id, source_sequence)` only
logically rather than globally unique. The implementation prioritizes data
integrity/idempotency and records partitioning as a condition for a later approved
design.

## Residual architecture risks

- MySQL and Kafka are not one atomic resource. The transaction prevents a Kafka
  failure from leaving an unpublishable persisted duplicate, but a Kafka
  acknowledgement followed by database commit failure can still create an orphan
  publication and a retry publication. No outbox/third table was invented because
  Phase A did not authorize one.
- `lastContiguousSequence` is implemented as the highest stored sequence. This is
  sufficient for the sequential synthetic pipeline and persistent gap signal, but
  requests for the same server are not serialized. Concurrent arrival can produce
  overlapping/incomplete observations until a future cursor/reconciliation design.
- No live broker test exercised topic ACLs, replication, producer durability, or
  cluster backpressure. The Kafka adapter contract was tested through a bounded
  sender seam as the cleared prompt permits when a broker is unavailable.
- Durable gap signalling is present; actual snapshot/resync remains explicitly in
  the Phase B gateway.

## Development Standards Compliance

### AGENTS.md compliance

The inspected delta is limited to the Q-015 module, V9, Q-015 bootstrap, shared
result-code and migration/static-gate maintenance, tests, A5 research, Lessons
Learned, and this package. No Q-008 through Q-014 module or schema was edited;
`deploy/keycloak/q016-security-bootstrap.json` has an empty diff. No existing
timestamped package was modified and no Git stage/commit/push operation occurred.

### Architecture compliance

The modular-monolith boundary is preserved. Controllers translate HTTP, validate,
call one use-case service, and return `ApiResponse`; business flow stays in the
application service; JDBC and Kafka remain adapters behind application ports. The
product remains broker/CRM neutral, reads or writes no external database, and makes
no claim about an unavailable Manager SDK.

### ADR compliance

Accepted ADR-023 is followed for the provisional envelope, metadata-only
reliability keys, Q-009 `SERVICE` boundary, full retention, account/time index,
visible gap marker, Kafka topic/key, and Phase B exclusions. The accepted Design's
non-partitioned fallback is used to protect the ADR's idempotency invariant. No new
ADR is warranted because no decision outside the accepted bundle was introduced.

### API standard compliance

The additive route is `POST /api/trading-data/ingest`; request DTO fields use
Jakarta validation including required numeric fields and an encoded-payload size
bound. The response uses `ApiResponse` and exposes only `outcome` and
`gapDetected`. Module business failures use stable `ResultCode` values and
`BusinessException` subclasses routed through `GlobalExceptionHandler`; stack
traces and raw infrastructure errors are not exposed.

### Database standard compliance

The only schema change is immutable forward-only Flyway V9. It creates two
`snake_case` InnoDB tables with `BIGINT id`, UTC-compatible `DATETIME(6)` values,
checks, a global unique key, and the approved history index. It has no destructive
DDL/DML, data movement, default rewrite, foreign key to older modules, payload
index, TTL, or external-schema access. Real MySQL verified clean migration,
V8-to-V9 pending count, idempotent rerun, checks, bytes, lookup index, uniqueness,
and rollback. The missing partition is explicit, not misreported.

### Security standard compliance

Q-009 authorization is enforced before actor-type/use-case work. Only a `SERVICE`
actor with `trading-data:ingest` can proceed; capability denial retains the generic
`AUTHORIZATION_DENIED` contract. The dedicated bootstrap grants one capability to
one service and does not expand the operator actor. Identifiers and payload sizes
are bounded; payload bytes, credentials, bearer headers, and customer content are
not logged or placed in review evidence.

### Auditability compliance

Each accepted envelope persists its version, platform tag, source server and
sequence, account, occurrence time, receive time, and exact payload. Detected
missing ranges are durable with source and detection time. This is the approved
ingestion provenance; no speculative Audit module was added. Authorization-denied,
outcome, gap, backpressure, and duration metrics provide operational visibility.

### Skill compliance

`docs/skills/development-standards.md` and the repository's established
authorization/data practices were applied. A reusable-skill evaluation is recorded
in the new Q-015 lesson; no additional repository skill was justified. The personal
`brokeros-review-package` skill governs this non-overwriting package, sensitive-data
scan, archive validation, checksum, and cleanup handoff.

## Review conclusion

No hidden architecture or standards violation was found. The implementation-stage
recommendation is **PASS WITH CONDITIONS** for the four residual items above.
Independent reviewer acceptance and Product Owner gate advancement remain pending;
Phase B must not start from this package alone.
