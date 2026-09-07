# Design traceability

| Approved input / requested behavior | Implementation | Verification |
| --- | --- | --- |
| ADR-024 neutral model; task 1 | CanonicalTradingDataEvent immutable routing projection: three kinds, five lifecycle hints, superset side/reason codes, optional string refs, Instant time | CanonicalTradingDataMessageReaderTests: golden records, synthetic MT5 account/quote, optional refs, OTHER/raw codes |
| ADR-024 provenance/envelope binding | CanonicalTradingDataMessageReader checks exact key/account/platform/server/UTC time equivalence and V1 versions | Mismatch, non-UTC, coercion, invalid enum, duplicate/trailing JSON tests |
| Phase A envelope / unchanged opaque payload | Original nested object bytes are sliced and passed to existing TradingDataEnvelope and JDBC BLOB store | All 12 recorded payloads byte-equal in real MySQL; Unicode/whitespace/long decimal extensions and byte-limit unit cases |
| Architecture V2 §3 authoritative trigger | CanonicalTradingDataKafkaListener on trading-data.canonical, factory from native KafkaProperties, service.ingestFromKafka | Real embedded broker + MySQL test; fake guard/publisher port forbids accidental HTTP auth or re-publication |
| FR-004 idempotency | Existing UNIQUE(server,seq) and shared append transaction | Replay all 12 records, 12 duplicate outcomes, no duplicate rows |
| FR-004 visible gaps | Existing MAX(sequence) lookup and transactional gap insert unchanged | Sequence jump 12→20 yields 13–19; failed gap insert rolls back event; replay produces 2–3 gap |
| Ordering and pull/backpressure | Account key checked; synchronous single listener; RECORD sync commits; max 100 per poll | Same-account 20/21/22 burst observed in insertion-ID order; offset inspection after success/failure |
| Malformed → visible dead letter | Quarantine publisher retains original value/key and safe broker coordinates | Real DLT byte/header checks; no forwarding caller header; consumption continues |
| No silent loss on infrastructure failure | Listener stops with failing offset uncommitted; no catch-all retry | MySQL-trigger and DLT-send failure injection; restart replays original offset successfully |
| V2 HTTP may be kept as test aid | Conditional disabled default; OpenAPI deprecated; original SERVICE/capability checks retained | Default route bean absent; existing Q-015 REST/application/MySQL tests pass |
| V2 Kafka-native trust | Native security/SASL/SSL map retained; scoped producer/consumer settings | Factory configuration test with synthetic secret; no production TLS/ACL claim |
| Hard boundaries | No migration, gateway, SDK or other business-module changes | Source manifest, Git scope and unchanged-file checks; static and architecture tests |

## Fixture and acceptance boundary

The unchanged mt4-canonical-golden.jsonl supplies 12 de-identified MT4 snapshot
TRADE_ACTIVITY observations from the provided capture. This task trusts the supplied
provenance and does not claim to have repeated the live capture. Sequences, envelopes,
duplicates and gaps are synthesized exactly as requested. Synthetic MT5 account/quote
cases prove neutral routing only; no native mapping or live MT5 test is claimed.

The model is a validation/routing projection, not a complete payload schema or a
financial calculation model. Amounts/currency and other unneeded content remain
opaque. Unknown optional fields are retained rather than silently dropped.

The cross-account golden replay waits for each prior append because Phase A only
accepted serialized server ingress. The same-account burst proves partition order;
it does not establish global source-sequence order across multiple Kafka partitions.
