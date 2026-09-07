# Q-015 Phase B — Canonical Kafka-Consumer Ingestion (portable) — Implementation Prompt

**CLEARED FOR USE — Product Owner approved the Q-015 Phase B (MT4) §16.5-B bundle
2026-09-07, transport V2 (Kafka-direct).** Governed by the two `docs/engineering/`
documents **and AGENTS.md**.

## What this task is (and is NOT)

The MT4 data path is: **MT4 gateway (C++, x64 Windows — a SEPARATE task, not yours)**
maps the native feed into ADR-024 **neutral canonical events (JSON)** and **produces
them to Kafka** (`trading-data.canonical`). **Your task is the portable Java half:**
consume that Kafka topic and persist to the existing Phase A partitioned store, plus the
canonical event Java model + tests. You do **not** write or build the C++ gateway, touch
the MT4 SDK, or need Windows — everything here is portable Java, buildable and testable on
Linux, driven purely by **recorded canonical JSON** messages.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md` (parent) +
   `docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md` (Phase A COMPLETE — the
   store + reliability you re-trigger).
2. `docs/adr/ADR-024-trading-data-canonical-model.md` (Accepted) — the canonical event
   schema you deserialize.
3. `docs/architecture/q-015-phase-b-mt4-ingestion-architecture.md` (V2) — esp. §3 (Phase
   A ingestion revised to a Kafka consumer) and §1/§5 (topology, work split).
4. `docs/architecture/q-015-phase-b-mt4-gateway-implementation-design.md` (V2) — context
   for the envelope + canonical event shape the gateway produces.

Reuse: the existing `com.brokeros.risk.tradingdata` module — `TradingDataIngestionService`
(idempotency `(source_server_id, source_sequence)`, gap markers, partitioned V9 store),
`TradingDataEnvelope`, `JdbcTradingDataEventStore`. `spring-kafka` is already a dependency.

## Task

1. **Canonical event Java model** (`tradingdata` domain/dto): a typed model for the
   ADR-024 canonical event (`TRADE_ACTIVITY` / `ACCOUNT_STATE` / `QUOTE`; neutral
   side/reason enums; UTC `occurredAt`; superset optional refs) — used only to **validate/
   route**; the store still persists the payload **opaquely** (the envelope's `payload`
   stays a blob — do not add payload columns).
2. **Kafka-consumer ingestion:** a `spring-kafka` `@KafkaListener` on
   `trading-data.canonical` that parses each message into the **Phase A envelope**
   (`envelopeVersion, platform, sourceServerId, sourceSequence, tradingAccountId,
   occurredAt, payload`) and drives the **existing** `TradingDataIngestionService`
   (idempotent append, visible gap markers, ordering by the partition key, store). Ordering
   = Kafka per-partition (key = `tradingAccountId`); backpressure = the consumer's natural
   pull. Malformed/undeserializable messages → a visible dead-letter/error path, never a
   silent drop.
3. **Retire/relegate the HTTP endpoint:** the Kafka consumer is the authoritative ingestion;
   keep or remove `POST /api/trading-data/ingest` per the Architecture (if kept, mark it a
   test aid). Do not change the V9 schema or the reliability rules — only the **trigger**.
4. **Tests (real-MySQL + Kafka):** recorded canonical JSON → assert idempotency (duplicate
   `(server,seq)` → no double store), visible gap marker on a sequence jump, opaque payload
   round-trip, ordering per account, malformed → dead-letter. Use an embedded/test Kafka
   (spring-kafka-test) — no live broker or gateway needed. Keep the full backend gate green.
   **Test material:** real, de-identified canonical event payloads (validated against a
   live MT4 demo server) are provided at
   `backend/src/test/resources/q015/mt4-canonical-golden.jsonl` — these are the ADR-024
   canonical events the gateway will produce; wrap each in the Phase A envelope
   (`envelopeVersion, platform, sourceServerId, sourceSequence, tradingAccountId=accountRef,
   occurredAt, payload=<the canonical JSON>`) with a synthesized `sourceSequence`, and add
   your own duplicate / sequence-gap / malformed cases for the reliability assertions.

## Hard boundaries — do not do these

- **Do NOT** write/build the C++ gateway, include any MT4 SDK header, or reference
  `mt-manager-libs/` — that is a separate Windows task. No MT4 native type appears here;
  you only handle neutral canonical JSON.
- **Payload stays opaque** in the store (blob); no payload columns; no new migration unless
  a genuinely additive index is shown necessary (call it out).
- Reuse the existing capability model; no new capability. Do not modify other Q-0xx modules.
- No secret/PII in code, logs, or fixtures. Do not stage, commit, or push. Do not modify
  any existing timestamped review package.
- On any contradiction, resolve toward the approved docs (Parent/Addendum > ADR-024 >
  Architecture V2 > Design V2) and record the assumption in `OutstandingItems.md`.

## Environment honesty

Run the backend real-MySQL gate + the Kafka-consumer tests (embedded Kafka) and report
real pass/fail/skip; confirm no V9/schema change, payload stays opaque, no SDK reference,
no other-module change. Never claim an un-run check.

## Required output

ONE new, non-overwriting, timestamped review package at
`review/q-015/review-q-015-phase-b-consumer-v<N>-implementation-<YYYYMMDD-HHMMSS>/` with
at least `Summary.md`, `ArchitectureReview.md` (against ADR-024 + Architecture V2 §3),
`DesignTraceability.md`, `ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`,
`Verification.md` (commands + real-MySQL + Kafka test results; confirm opaque payload / no
SDK / no schema change), `SecurityReview.md`, `TestInventory.txt`, `OutstandingItems.md`,
and `docs/lessons/<date>-q-015-phase-b-consumer-implementation.md`.

This package is for Claude Code's independent review — state PASS/FAIL honestly, list open
questions. Stop after the package; do not begin the MT5 gateway or the C++ gateway.
