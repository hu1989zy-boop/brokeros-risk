# ADR-023: Q-015 Phase A — Payload-Opaque SDK-Independent Ingestion

- Status: **Accepted** — 2026-09-06 (Product Owner, as part of the Q-015 Phase A
  §16.5-B bundle at the implementation-authorization gate).
- Scope: **Phase A only** (SDK-independent). Builds toward, but does not decide, the
  canonical field model or the MT4/MT5 gateway interfaces — those are Phase B,
  blocked on the real SDK (parent Q-015 §17; AGENTS.md L76).
- Context docs: `docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md`;
  `docs/architecture/q-015-phase-a-ingestion-architecture.md`.

## Context

Q-015 is parked because the canonical Trading Data field model and the MT4/MT5
gateway interfaces cannot be designed without the real SDK. To make SDK-independent
progress, Phase A must deliver the reliability pipeline, the trusted-actor ingestion
boundary, and the durable partitioned store — without describing any native field.
The design question: how do we build and fully test the ingestion foundation now
without inventing a Manager API interface or committing the canonical model?

## Decision

Build the platform-side ingestion against a **versioned, payload-opaque envelope
contract**, enforcing all reliability from **envelope metadata only**, behind a
Q-009 `SERVICE`-actor HTTP boundary, into a partitioned full-retention store and
Kafka.

1. **Payload-opaque envelope.** `TradingDataEnvelope { envelopeVersion, platform,
   sourceServerId, sourceSequence, tradingAccountId, occurredAt, payload: opaque
   bytes }`. Phase A never reads, indexes, or describes `payload`. The metadata is a
   **contract the future gateway must satisfy**, not an SDK assertion.
2. **Q-009-authorized HTTP ingestion boundary.** `POST /api/trading-data/ingest`
   authorizes as a `SERVICE` actor holding a new `trading-data:ingest` capability
   (default-deny), provisioned by a **separate** bootstrap file — the console
   operator's least-privilege set is untouched. The endpoint is the reliability
   enforcement point (chosen over gateway→Kafka-direct to keep the mandated Q-009
   boundary in the path).
3. **Reliability by metadata.** Idempotency/de-dup via a unique
   `(source_server_id, source_sequence)`; gap detection by per-server sequence
   contiguity, writing a **visible gap marker** (no silent loss) rather than skipping;
   per-account ordering via Kafka partition key = `tradingAccountId` plus persisted
   `source_sequence`/`occurred_at`; backpressure by synchronous accept-after-persist
   with a throttle response under overload.
4. **Partitioned full-retention store (new migration V9).** `trading_data_event`
   (opaque `payload` blob + queryable metadata), RANGE-partitioned by time
   (field-independent), unique on `(source_server_id, source_sequence)`, indexed on
   `(trading_account_id, occurred_at)`; plus `trading_data_ingestion_gap`. No expiry.
5. **Versioning.** `envelopeVersion` (and a store column) so Phase B evolves the shape
   and migrates without breaking Phase A data.
6. **Kafka first use.** One topic `trading-data.canonical`, keyed by
   `tradingAccountId`; `spring-kafka` (already present) is the producer.

## Consequences

**Positive**
- Real, testable progress on the reliability/auth/storage foundation now, with no SDK
  and no Windows — all verifiable with synthetic envelopes on Linux/macOS.
- AGENTS.md respected: no invented Manager API interface, no committed canonical field
  model, payload opaque; MT4/MT5 detail stays out entirely until the Phase B adapter.
- The `SERVICE`-actor boundary and the "no silent loss" posture (the parent's hardest
  reliability requirements) are established and tested early.

**Negative / trade-offs (bounded, accepted — addendum §6)**
- The envelope's ordering / de-dup / partition **keys** are informed by the eventual
  model, so Phase B may adjust them. Bounded by keeping the payload opaque, the
  envelope versioned, and all logic metadata-driven — a thin, migratable layer.
- Phase A stores opaque payloads that only become queryable-by-content in Phase B
  (acceptable — historical *account/time* lookup works now; content lookup is a Phase
  B concern).

**Neutral**
- A new schema (V9) is added — the first Phase A migration; the dynamic
  migration-count test discipline applies.

## Alternatives rejected

- **Wait for the SDK to design everything** — forgoes de-risking the reliability core
  now; the PO chose phased progress (addendum route 1).
- **Invent/guess a canonical field model or SDK struct** — violates AGENTS.md L76/L98.
- **Gateway publishes to Kafka directly** — bypasses the Q-009 `SERVICE`-actor boundary.
- **Configurable-expiry storage** — parent §5.3(2) mandates full retention, partitioned.

## Compliance / verification

- Synthetic-envelope replay tests (idempotency, gap marker, ordering, backpressure);
  `SERVICE`-actor default-deny authorization; real-MySQL partitioned-store round-trip
  (opaque payload, metadata queryable); no payload field asserted; no Q-008…Q-014
  change; ingestion read-only.
