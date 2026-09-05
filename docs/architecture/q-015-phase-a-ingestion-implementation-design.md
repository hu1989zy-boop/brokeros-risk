# Q-015 Phase A Implementation Design — SDK-Independent Ingestion Foundation

Status: **V1**, part of the Phase A §16.5-B bundle. The authoritative build spec;
subordinate to the parent Requirement, the Phase A addendum, ADR-023, and the
Architecture. SDK-independent; **payload opaque throughout**.

## 0. Ground rules

- SDK-independent: no MT4/MT5 native type, no invented Manager API interface, no
  canonical field model. `payload` is opaque bytes end to end.
- Additive **new module** `com.brokeros.risk.tradingdata`; reuse Q-009 unchanged; do
  not modify any Q-008…Q-014 file. Ingestion is **read-only** into the platform (it
  never calls out to any broker).
- One new Flyway migration (V9) for the store; update the dynamic migration-count
  assertions (project test discipline).

## 1. Domain (`tradingdata/domain`)

- `TradingDataEnvelope` (record): `envelopeVersion:int`, `platform:PlatformTag`,
  `sourceServerId:String`, `sourceSequence:long`, `tradingAccountId:String`,
  `occurredAt:Instant`, `payload:byte[]` (opaque). Validates: version ≥ 1;
  `sourceSequence` ≥ 0; non-blank server/account (bounded length, charset like the
  other refs); `payload` length within a sane cap (e.g. ≤ 64 KiB) — but its **bytes
  are never inspected**.
- `PlatformTag` enum: `MT4`, `MT5` (a provenance label only — not a structure).
- `SourceSequence` / small value types as needed. No native concept is modeled.

## 2. Ingestion endpoint (`tradingdata/interfaces/rest`)

- `TradingDataIngestionController` — `@PostMapping("/api/trading-data/ingest")`,
  `@ConditionalOnWebApplication(SERVLET)`. Body `IngestTradingDataRequest`:
  `{ envelopeVersion, platform (MT4|MT5), sourceServerId, sourceSequence,
     tradingAccountId, occurredAt (ISO-8601), payloadBase64 }` — `payloadBase64` is
  decoded to opaque bytes and never parsed. Bean-validation on the metadata only.
- Delegates to `ingestionService.ingest(actorContextProvider.currentContext(),
  command)`; returns the standard `ApiResponse` wrapping
  `{ outcome: ACCEPTED|DUPLICATE, gapDetected: bool }`. (Batch endpoint optional;
  V1 may accept one envelope per call.)

## 3. Ingestion service (`tradingdata/application`)

`TradingDataIngestionService.ingest(ActorContext, IngestCommand)`:

1. **Authorize:** `authorizationGuard.requireAllowed(ctx,
   TradingDataCapabilities.INGEST)` (on denial, `recordAuthorizationDenied` + rethrow),
   then `requireService(ctx)` — throw a `TRADING_DATA_ACTOR_TYPE_NOT_PERMITTED`-style
   `ResultCode` if `actorType() != SERVICE` (mirrors the record services' `requireHuman`).
2. **Build + validate** the `TradingDataEnvelope` (metadata only; malformed →
   a `TRADING_DATA_REQUEST_INVALID` `ResultCode`).
3. **Idempotent persist:** `eventStore.append(envelope)` — insert into
   `trading_data_event`; a unique `(source_server_id, source_sequence)` violation is
   caught and mapped to **DUPLICATE** (idempotent no-op, still success).
4. **Gap detection:** `eventStore.lastContiguousSequence(sourceServerId)`; if
   `sourceSequence > last + 1`, `eventStore.recordGap(sourceServerId, last+1,
   sourceSequence-1)` and set `gapDetected=true` + a metric — never silently proceed.
   (First-ever event for a server is not a gap.)
5. **Publish:** on a newly-accepted (non-duplicate) event,
   `eventPublisher.publish(envelope)` — Kafka, key = `tradingAccountId`.
6. **Backpressure:** persist+publish are synchronous to the response; the publisher
   uses a bounded send with a timeout, surfaced as a throttle/unavailable `ResultCode`
   rather than unbounded buffering.

Ports (interfaces in `application/port`): `TradingDataEventStore`
(`append`, `lastContiguousSequence`, `recordGap`), `TradingDataEventPublisher`
(`publish`). `TradingDataCapabilities.INGEST = new Capability("trading-data:ingest")`.

## 4. Infrastructure (`tradingdata/infrastructure`)

- `JdbcTradingDataEventStore` — `INSERT ... ON DUPLICATE KEY`/insert-ignore for
  idempotency; `SELECT` for last-contiguous-sequence; `INSERT` gap markers. Reads/
  writes only the two new tables. Opaque `payload` bound as bytes.
- `KafkaTradingDataEventPublisher` — `KafkaTemplate<String, byte[]>` (or a small
  envelope serialization that still treats payload as opaque bytes), topic
  `trading-data.canonical`, key = `tradingAccountId`. `spring-kafka` is already a
  dependency.
- `TradingDataModuleConfiguration` — wires the service, ports, guard, clock, metrics;
  Kafka producer config (bootstrap servers already provided via env in compose).

## 5. Schema — `V9__create_trading_data_ingestion_foundation.sql`

- `trading_data_event`: `id BIGINT AUTO_INCREMENT`, `envelope_version SMALLINT`,
  `platform VARCHAR(8)` (`MT4`/`MT5`), `source_server_id VARCHAR(128)`,
  `source_sequence BIGINT`, `trading_account_id VARCHAR(128)`, `occurred_at
  DATETIME(6)`, `received_at DATETIME(6)`, `payload VARBINARY(65535)` (opaque).
  - `UNIQUE (source_server_id, source_sequence)`; `INDEX (trading_account_id,
    occurred_at)`; CHECKs (`platform IN ('MT4','MT5')`, `envelope_version >= 1`,
    `source_sequence >= 0`, payload length ≥ 1).
  - **RANGE partition** on a time expression of `occurred_at` (e.g.
    `TO_DAYS`/`YEAR*100+MONTH`), monthly, with a catch-all `MAXVALUE` partition. (Note:
    MySQL requires every UNIQUE/PK to include the partition column — so the primary key
    is `(id, occurred_at)` and the uniqueness is `(source_server_id, source_sequence,
    occurred_at)`; keep idempotency correct by treating `(source_server_id,
    source_sequence)` as logically unique within retained windows. If partitioning
    complicates the unique constraint unacceptably, the implementer may, and should
    call out in `Verification.md`, ship V1 **non-partitioned with the plain UNIQUE**
    and a documented follow-up to partition — the addendum's partitioned-store intent
    is the target, correctness of idempotency is the invariant.)
- `trading_data_ingestion_gap`: `id`, `source_server_id VARCHAR(128)`, `from_sequence
  BIGINT`, `to_sequence BIGINT`, `detected_at DATETIME(6)`; `INDEX (source_server_id,
  detected_at)`.
- Full retention (no expiry/TTL job). No FK to any Q-008…Q-014 table.

## 6. Authorization bootstrap

- `TradingDataCapabilities.INGEST = "trading-data:ingest"`.
- A new `deploy/keycloak/q015-ingestion-bootstrap.json`: one actor,
  `"actorType": "SERVICE"`, its principal(s), capabilities `["trading-data:ingest"]`.
  **Do not touch** `q016-security-bootstrap.json` (operator stays least-privilege).
- `requireService(ActorContext)` in the service (symmetric to `requireHuman`).

## 7. Tests (`tradingdata` + real-MySQL)

- **Application/unit:** ingest happy path (ACCEPTED + published); duplicate
  `(server,seq)` → DUPLICATE, not re-published; gap (seq jumps) → gap marker +
  `gapDetected`; ordering (publisher key = accountId); non-`SERVICE` actor → denied;
  missing capability → `AUTHORIZATION_DENIED`; malformed metadata → request-invalid;
  **payload bytes never inspected** (a test passes arbitrary bytes and asserts they
  round-trip unread).
- **Real-MySQL (`Q015…MySqlTests`):** partitioned-store round-trip — persist a batch
  of synthetic envelopes, assert metadata queryable + opaque payload returned intact +
  historical `(account, time-range)` lookup + the unique/idempotency constraint +
  the gap marker table; `SHOW CREATE TABLE` (or information_schema) confirms
  partitioning if shipped. Update the migration-count assertion to include V9.
- **Contract:** the `IngestTradingDataRequest`/envelope schema is the seam — test its
  shape + `envelopeVersion`.
- No SDK, no Windows, no payload field asserted; no credential/PII in fixtures.

## 8. Traceability

| Addendum item | Code |
| --- | --- |
| A1 auth boundary | §2 endpoint + §3.1 authorize/requireService + §6 capability/bootstrap |
| A2 reliability | §3.3–3.6 idempotency/gap/order/backpressure + §4 adapters |
| A3 storage | §5 V9 partitioned store |
| A4 replay harness | §7 synthetic-envelope tests |
| Envelope contract §5 | §1 domain + §2 DTO (payload opaque, versioned) |

## 9. Out of scope (Phase B / parent)

Canonical field model + ADR; MT4/MT5 gateway interfaces + native adapters;
native→canonical translation; markout tick-window capture; x64 Windows integration;
automatic Evidence creation; account control/execution; rule engine; any Q-008…Q-014
change. **A5 pre-SDK research note** is a separate `docs/` note, not code.
