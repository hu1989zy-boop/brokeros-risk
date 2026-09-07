# Q-015 Phase B portable canonical consumer

Authority: Q-015 parent §17 Transport V2, ADR-024 and Phase B Architecture V2 §3.
This document records the implementation and its operating limits; it does not
advance a gate or authorize gateway, schema, analytics or production deployment work.

## Wire and storage boundary

The input is `trading-data.canonical`, with a UTF-8 string key equal to
`tradingAccountId` and JSON object value:

```json
{
  "envelopeVersion": 1,
  "platform": "MT4",
  "sourceServerId": "synthetic-server",
  "sourceSequence": 1,
  "tradingAccountId": "synthetic-account",
  "occurredAt": "2026-09-07T00:00:00Z",
  "payload": {
    "canonicalVersion": 1,
    "platform": "MT4",
    "sourceServerId": "synthetic-server",
    "eventKind": "QUOTE",
    "lifecycleHint": "STATE",
    "accountRef": "synthetic-account",
    "symbol": "EURUSD",
    "bid": 1.10001,
    "ask": 1.10002,
    "occurredAt": "2026-09-07T00:00:00Z"
  }
}
```

The canonical model is an immutable **routing projection**: kind, lifecycle,
provenance, UTC time, string references, neutral side/reason and their raw codes.
It does not assert financial completeness, calculate money, reconstruct positions
or normalize source values. Other ADR-024 fields and extensions remain in the
original JSON. The 12 provided golden records omit account currency; no currency
is inferred or invented. Their amounts must not be used as validated money until
the gateway supplies authoritative currency and a separately reviewed financial
consumer validates it. QUOTE without an account key cannot satisfy the approved
Phase A envelope and is quarantined; gateway account assignment remains a contract
question, not a reason to invent a symbol-based key here.

The reader rejects malformed JSON, duplicate keys, trailing values, unsupported
versions, invalid enums/coercions, missing routing fields, mismatched envelope /
canonical provenance or key, and non-UTC timestamps. Source time, if present,
requires its offset. `OTHER` side/reason requires its raw code. The parser limits
nesting to 32, numeric tokens to 128 characters, total wire bytes to 73,727 and
payload bytes to the existing 65,535 maximum. It slices the original payload
object bytes, preserving decimal lexemes, Unicode, whitespace and unknown fields.
The JDBC adapter and reliability service never deserialize payloads.

V9 and all existing migrations are unchanged. The actual V9 store is the
**accepted non-partitioned Phase A fallback**, with a global unique `(server,seq)`
key and account/time index; this implementation does not claim physical partitioning.

## Delivery and failure handling

- One synchronous listener thread, record acknowledgements, synchronous offset
  commits, auto-commit disabled, bounded pulls of at most 100 records. The local
  transaction commits the event and gap marker before its Kafka offset advances.
- The Kafka entry uses the existing service without invoking its producer or its
  HTTP actor guard. Topic ACLs / SASL or mTLS are the authenticated boundary.
- Delivery is at-least-once. A crash after database commit but before offset commit
  replays the record into the existing unique key, producing a duplicate no-op.
- Invalid records publish original value/key to
  `brokeros.risk.tradingdata.ingestion-rejected`. Headers contain rejection version
  `1`, stable `TRADING_DATA_REQUEST_INVALID`, and original topic/partition/offset.
  Caller-supplied headers and parser/SQL/producer exception diagnostics are not copied.
  The topic is a quarantine transport, not an application entity or a new business
  capability. The original record is retained verbatim, including tombstones.
- Successful quarantine is acknowledged before advancing the input offset. A
  crash between these steps can duplicate a dead letter; identify it by original
  topic/partition/offset. Quarantine consumers must handle null values and must not
  use compaction to discard rejected observations.
- Database, quarantine-publish, or unexpected listener failures stop the listener
  with the failing input offset uncommitted. No catch-all retry or exhaustion skip
  exists. The producer allows three protocol-level retries for retriable failures,
  `acks=all`, idempotence, one in-flight request, 5-second max block/request and
  15-second delivery timeout. Synchronous send confirmation waits at most 5 seconds.
- Metrics: existing operations/gaps/duration plus
  `brokeros.risk.tradingdata.kafka.rejected` and `.failed`. Error logs expose only
  partition and offset. Default producer payload/error logging is disabled.

After a stop, investigate the fixed-coordinate error/failed counter and database
or broker availability. Repair the cause, then restart the application with the
**same consumer group**. Do not reset offsets past the failure. Retain input data
long enough for this recovery. A stopped listener does not make the existing HTTP
health endpoint fail; operators must monitor the failure counter and consumer lag.
This work adds no Actuator endpoint or automatic production recovery policy.

## Source ordering and reconnect limitations

Kafka preserves order within a partition. The producer must consistently key by
account and preserve ordering before publication; explicit mispartitioning and
cross-partition order are not repairable by this reader. Avoid repartitioning a
live stream without a separately reviewed migration.

The unchanged Phase A gap algorithm looks at `MAX(source_sequence)` per server.
It is not a global sequencer. Even with one listener thread, multiple account
partitions can be polled in a different order from a server's global sequence;
multiple application instances add concurrent cursor ownership. Consequently
same-server multi-partition production can produce false/incomplete gap markers.
The tests serialize cross-account fixture arrival and separately verify a burst
within one account. They do not prove global cross-partition gap correctness.
Before production concurrency, architecture must settle sequence ownership,
late arrivals and gap reconciliation (an existing Phase A accepted condition).

A reconnect that resets sequence for the same `sourceServerId` collides with the
unchanged idempotency key. V2 describes epochs but does not specify their wire
encoding or durable uniqueness. No epoch field, fake account, marker event kind,
or new key was invented here. Gateway integration must resolve this contract
before live reconnect is accepted.

## Provisioning and security

Pre-provision both topics; this application declares no `NewTopic` and consumer
auto-creation is disabled. Configure broker-side auto-creation off. The quarantine
topic must allow the input record size, preserve rejected bytes (non-compacted
retention), and have appropriately reviewed replication/min-ISR and recovery
retention. This task tested a disposable embedded broker, not production topology.

The gateway principal may produce only to canonical input. The application
principal may consume canonical input with its stable group and produce to the
quarantine topic. Quarantine read access is restricted to incident/recovery
operators. Kafka authentication, TLS certificates, SASL credentials and ACLs are
external deployment controls; native Spring Kafka properties are retained by the
module factories. Never put credentials in source control, logs or Review files.
Rejected messages can contain untrusted sensitive content; the quarantine must
have at least the input topic's access protection. Record coordinates, not bodies,
in operational alerts.

## HTTP compatibility and configuration

`POST /api/trading-data/ingest` is a deprecated, **disabled-by-default test aid**.
An explicit `brokeros.risk.tradingdata.http-test-aid-enabled=true` exposes its
original authenticated SERVICE/capability API and opaque `payloadBase64` contract.
It still uses the Phase A store+publish behavior; do not enable it in production.
Its legacy `payloadBase64` Kafka messages are intentionally not the new canonical
wire contract and will be quarantined if consumed by this listener. Use isolated
test infrastructure for that legacy path. Q-009 and its capabilities are unchanged.

Native `spring.kafka.listener.auto-startup` controls listener startup (framework
default true in every packaged profile; test-classpath properties false). Embedded integration tests
explicitly start the listener. Native consumer group/bootstrap/security settings
remain externally configured; restart is required for configuration changes.
Safe acknowledgement, serialization, concurrency and producer delivery settings
are fixed by this adapter and take precedence over incompatible native overrides.
Rollback needs no database migration: stop this listener and deploy the prior
application version if needed. The old HTTP producer expects a different wire
contract, so coordinate gateway/consumer activation explicitly during rollback.
