# Q-015 Phase A Architecture — SDK-Independent Ingestion Foundation

Status: **V1**, part of the Phase A §16.5-B bundle (Architecture + ADR-023 +
Implementation Design) authorized at Q-015 Phase A's implementation-authorization gate
(addendum §17, APPROVED 2026-09-06). Authoritative over the Implementation Design
where they differ; subordinate to the parent Requirement, the Phase A addendum, and
ADR-023. Governed by the two `docs/engineering/` documents.

**Scope guard (AGENTS.md):** Phase A is SDK-independent. The event **payload is opaque
throughout** — no Manager API interface is invented and no canonical field model is
committed. MT4/MT5 native detail, if it ever appears, lives only in the Phase B
gateway adapter.

## 1. Context

The parent Q-015 is parked awaiting the MT4/MT5 SDK + x64 Windows. The Phase A
addendum carves out the SDK-independent half: the **platform-side reliability
pipeline**, the **`SERVICE`-actor ingestion authorization boundary**, and the
**durable partitioned storage skeleton** — all built against a **versioned,
payload-opaque envelope contract** and tested with synthetic inputs on Linux/macOS.

## 2. Topology (the decision; ADR-023 is authoritative)

```
(future MT4/MT5 gateway — Phase B)          Phase A builds everything below
  or (synthetic test producer — Phase A)
                 │  provisional envelope (payload opaque)
                 ▼
   POST /api/trading-data/ingest    ← Q-009 SERVICE-actor authorized
                 │                     (capability trading-data:ingest, default-deny)
                 ▼
        TradingDataIngestionService  ← reliability by envelope METADATA only
         ├─ idempotent persist ──────▶  trading_data_event  (partitioned, full retention)
         ├─ gap detection ───────────▶  trading_data_ingestion_gap (visible marker)
         └─ ordered publish ─────────▶  Kafka topic "trading-data.canonical"
                                          (key = tradingAccountId → per-account order)
```

- **Transport / auth boundary (addendum §5.3(4)):** the gateway (Phase B) or a
  synthetic producer (Phase A) delivers envelopes to a **Q-009-authorized HTTP
  ingestion endpoint** — keeping the mandated `SERVICE`-actor authorization (parent
  FR-005) in the path, rather than publishing to Kafka directly. The endpoint is the
  reliability enforcement point.
- **Kafka (first real use):** the endpoint publishes each accepted envelope to a
  single topic **keyed by `tradingAccountId`**, so Kafka's per-partition ordering
  gives the per-account ordering guarantee (addendum §5.3(1)). `spring-kafka` is
  already on the classpath.
- **Storage:** every accepted envelope is durably persisted first (see §4), so
  acceptance is atomic and replayable.

## 3. The provisional envelope (payload-opaque)

One versioned value type (`TradingDataEnvelope`) with **generic streaming metadata
only**; `payload` is an opaque byte blob Phase A never reads or describes:

`{ envelopeVersion, platform (MT4|MT5 tag), sourceServerId, sourceSequence,
   tradingAccountId, occurredAt, payload: opaque bytes }`

This is a **contract the future gateway must satisfy**, not an SDK assertion. Its
metadata drives all reliability; its payload shape is Phase B.

## 4. Reliability by metadata (addendum A2)

- **Idempotency / de-dup (§5.3(2)):** the store carries a **unique
  `(source_server_id, source_sequence)`**; a re-delivered envelope is a no-op
  (insert-ignore / on-duplicate). The endpoint returns success idempotently.
- **Gap detection, no silent loss (§5.3(3)):** the service tracks the last contiguous
  `source_sequence` per `source_server_id`; when an arriving sequence skips ahead, it
  writes a **visible `trading_data_ingestion_gap` marker** (from/to sequence) and
  emits a metric — it does **not** silently proceed as if contiguous. (Actual
  snapshot/resync is the gateway's job in Phase B; Phase A detects and surfaces the
  gap.)
- **Ordering (§5.3(1)):** Kafka partition key = `tradingAccountId`; persisted rows
  carry `source_sequence` + `occurred_at` so per-account order is reconstructable.
- **Backpressure:** persistence + publish are synchronous to the ingest response
  (accept only after durable persist + enqueue); under overload the endpoint returns a
  standard retry/throttle response rather than unbounded buffering.

## 5. Storage skeleton (addendum A3; the only new schema)

A **new Flyway migration (V9)** adds the partitioned store — the first Phase A schema
(so the migration-count test discipline applies, parent §11):

- `trading_data_event` — `id`, `envelope_version`, `platform`, `source_server_id`,
  `source_sequence`, `trading_account_id`, `occurred_at`, `received_at`, `payload`
  (`VARBINARY`/blob, **opaque**). **Full retention** (no expiry).
  - **Partitioned** by time (RANGE on `occurred_at`, e.g. monthly) — a
    field-independent strategy (§5.3(3)); Phase B may add model-driven partitions.
  - **Unique** `(source_server_id, source_sequence)` (idempotency).
  - **Index** `(trading_account_id, occurred_at)` (historical account lookup).
- `trading_data_ingestion_gap` — `id`, `source_server_id`, `from_sequence`,
  `to_sequence`, `detected_at` — the visible no-silent-loss markers.

No payload column is indexed or parsed in Phase A. No Q-008…Q-014 table is touched.

## 6. Authorization boundary (addendum A1)

- A new capability **`trading-data:ingest`** (a `Capability` constant in the
  `tradingdata` module) and a Q-009 **`SERVICE`** actor provisioned via a dedicated
  bootstrap file (e.g. `deploy/keycloak/q015-ingestion-bootstrap.json`,
  `"actorType": "SERVICE"`) — **separate from the console operator's least-privilege
  set**, which is unchanged.
- The ingestion service calls `authorizationGuard.requireAllowed(ctx,
  TRADING_DATA_INGEST)` and requires `ActorType.SERVICE` (mirroring the record
  services' `requireHuman`, inverted to `requireService`). Default-deny for anyone
  else; no caller-supplied identity beyond the Bearer JWT.

## 7. Module structure

`com.brokeros.risk.tradingdata`:
- `domain` — `TradingDataEnvelope`, `SourceSequence`, `PlatformTag`, gap value types.
- `application` — `TradingDataIngestionService`, ports (`TradingDataEventStore`,
  `TradingDataEventPublisher`, gap tracking), `TradingDataCapabilities`.
- `infrastructure` — `JdbcTradingDataEventStore`, `KafkaTradingDataEventPublisher`,
  module configuration.
- `interfaces/rest` — `TradingDataIngestionController` (`POST /api/trading-data/ingest`)
  + request/response DTOs (payload as opaque base64).
- **No `gateway` subpackage** in Phase A (that is Phase B, on x64 Windows).

## 8. Alternatives considered

- **Gateway → Kafka direct** (rejected, §5.3(4)) — bypasses the Q-009 `SERVICE`-actor
  boundary the parent mandates.
- **Reliability in a Kafka consumer instead of at ingest** (rejected for V1) —
  enforcing idempotency/gap at the authorized ingest point keeps the durable store and
  the "no silent loss" signal authoritative and simplest to test synthetically.
- **Describing any payload field now** (rejected, AGENTS.md) — payload stays opaque
  until the SDK (Phase B).
- **A configurable-expiry store** (rejected, parent §5.3(2)) — full retention,
  partitioned for scale.

## 9. Testing posture (addendum §8)

- Replay **synthetic** envelopes proving: idempotent re-delivery, gap detection +
  visible marker, per-account ordering, backpressure; `SERVICE`-actor authorization
  (default-deny otherwise); partitioned store round-trip (opaque payload in/out,
  metadata queryable, historical account/time lookup) on real MySQL. No SDK, no
  Windows, no payload field asserted.

## 10. Deliverables

The Implementation Design specifies the envelope type, endpoint, service, ports +
adapters, the V9 schema, the capability + `SERVICE`-actor bootstrap, and the tests.
ADR-023 records the decision.
