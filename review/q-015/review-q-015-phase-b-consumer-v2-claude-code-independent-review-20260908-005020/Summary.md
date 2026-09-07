# Q-015 Phase B (portable consumer) — Claude Code Independent Review (v2)

- Requirement: Q-015 Phase B — the portable **Kafka-consumer ingestion** half (transport
  V2), per ADR-024 + Architecture V2 §3.
- Reviewed: Codex v1 delivery (`review-q-015-phase-b-consumer-v1-implementation-20260908-001952`)
- Baseline: `ab82faa`
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-08
- **Gate Decision: PASS** (I concur with Codex's *PASS WITH CONDITIONS*; the conditions
  are real, correctly characterized, and appropriately deferred — none is a defect in the
  delivered portable consumer).

## Verdict

The V2 Kafka-consumer ingestion, delivered cleanly and honestly. A `@KafkaListener` on
`trading-data.canonical` drives the **existing** Phase A reliability + partitioned store
(idempotency, gap markers) with **no republish** to the input topic; malformed input goes
to a **visible dead-letter** with the original bytes; the record offset advances only after
the local transaction commits (or confirmed quarantine), so infra failure stops
consumption and replays. The payload is stored **opaquely** (original UTF-8 bytes sliced,
not reserialized). I independently reproduced the full backend gate and code-reviewed the
boundary + reliability.

## Independently reproduced

| Check | Result |
| --- | --- |
| Backend full real-MySQL + embedded-Kafka gate (Docker MySQL 8.4 + Maven 21) | **BUILD SUCCESS — 356 tests, 0 failures, 0 errors, 0 skipped** |
| Q-015 consumer test classes | `Q015CanonicalKafkaMySqlTests` **4/4** (embedded-Kafka + real-MySQL: golden events→store, duplicate/gap/account-order, malformed→quarantine+continue, DB-failure→rollback+stop+replay, dead-letter-publish-failure→no offset advance+replay); `CanonicalTradingDataMessageReaderTests` **7/7**; `TradingDataKafkaConfigurationTests` **1/1** |
| Schema | **V9 unchanged** (9 migrations, no V10); JDBC store + envelope untouched |
| Boundary | **no SDK/MT4 reference**; payload stored opaquely (original bytes sliced); **no new capability**; changes confined to `tradingdata` + docs + `pom.xml` (only `spring-kafka-test`, test scope) + test props |

## Code review (correct)

- **Listener** (`CanonicalTradingDataKafkaListener.consume(ConsumerRecord<String,byte[]>)`):
  `InvalidCanonicalMessageException` → `deadLetters.publish(record)` + metric + return
  (quarantine, not silent drop); success → `service.ingestFromKafka(envelope)`, ack only
  after commit; any `RuntimeException` → a **sanitized** `TradingDataAuthorityUnavailableException`
  (no SQL/parser/payload diagnostics) → the stopping error handler halts consumption, offset
  not advanced → replayable.
- **No republish:** `ingestFromKafka` calls `persist(envelope, false)`; the `publish` flag
  reuses the exact Phase A idempotency/gap/store transaction while skipping the re-produce
  (the event is already on Kafka). Minimal, correct refactor of the trigger only.
- **Opaque payload:** `CanonicalTradingDataMessageReader` slices the **original** payload
  object bytes (preserving decimals/whitespace/unknown extensions) for storage; the typed
  `CanonicalTradingDataEvent` is used only to validate/route (neutral kinds, refs, side/
  reason, UTC time, key). No payload columns, no monetary coercion.
- **Config/docs:** the new Kafka consumer group / listener auto-startup / HTTP-test-aid
  flag are documented in `docs/configuration/README.md` (the project's config-doc
  discipline); a reusable note was added to `docs/skills/development-standards.md`. Both
  additive and on-topic.

## Conditions (I concur — deferred, not defects)

Codex honestly surfaced two that are **real gaps in the V2 gateway/envelope design** (mine),
exposed by this implementation — valuable independent-implementation findings:

- **Global sequence vs account partitions.** The per-server `(server,seq)` gap detection
  (`MAX(sequence)`) assumes serialized delivery, but Kafka partitions by `accountRef`, so a
  server's events span partitions → concurrent/out-of-order consumption can make the gap
  logic false/incomplete. V1 (single consumer thread; tests serialize cross-account
  delivery) is fine; **cursor ownership/reconciliation must be settled before concurrent
  production.**
- **Reconnect epoch not encoded.** Architecture V2 requires a new epoch on reconnect but the
  envelope has **no epoch field**, so a same-server sequence reset can collide with old rows.
  **The gateway/envelope must add a durable epoch/discontinuity representation** before live
  reconnect acceptance.

Other conditions (all appropriately deferred): raw `QUOTE` needs an `accountRef` (rejected
visibly, not invented); the golden fixtures omit `currency` (the gateway must supply it —
the typed model is a routing projection, not a financial validator); production broker
security/topology/at-least-once recovery. None blocks the portable consumer.

## Acceptance — reviewer view

| Item | Result |
| --- | --- |
| Kafka-consumer ingestion (idempotency/gap/store, no republish) | **PASS** |
| Malformed → visible dead-letter; infra failure → replay | **PASS** |
| Opaque payload (original bytes) | **PASS** |
| Canonical Java model (routing projection) | **PASS** |
| No SDK / no schema change / no new capability / confined scope | **PASS** — reproduced |
| Backend gate (real-MySQL + embedded Kafka) | **PASS** — 356/0/0 |

## Recommendation

**Accept Q-015 Phase B portable consumer V1.** The V2 Kafka-consumer ingestion is correct,
boundary-clean, and independently verified (356 backend incl. embedded-Kafka + real-MySQL
consumer tests). The reviewer changed no code. The two design-gap conditions
(global-sequence/account-partition gap detection; envelope reconnect epoch) are real and
feed back into the **MT4 gateway + envelope** work (the Windows line), not the consumer.
The C++ gateway, MT5, and Flink remain the deferred halves.
