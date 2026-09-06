# Q-015 Phase A Implementation Review Summary

## Review boundary

- Requirement: Q-015 Trading Data Ingestion Foundation, Phase A SDK-Independent
  Addendum V1.
- Lifecycle stage: Phase A implementation and implementation verification only.
- Authorized inputs: approved Q-015 parent Requirement and Phase A Addendum,
  Architecture V1, accepted ADR-023, Implementation Design V1, and the cleared
  implementation prompt.
- Intended consumer: Claude Code's independent implementation review.
- This package does not accept its own delivery, advance Phase B, stage, commit, or
  push any change.

## Outcome

The Phase A SDK-independent ingestion foundation is implemented. A new
`com.brokeros.risk.tradingdata` module accepts a versioned metadata envelope with
opaque bytes, authorizes the Q-009 `SERVICE` actor capability, applies metadata-only
de-duplication and gap signalling, persists the full envelope, and publishes newly
accepted envelopes synchronously to Kafka using `tradingAccountId` as the key.

Implementation-stage Gate Decision recommendation: **PASS WITH CONDITIONS**.

The conditions are visible design/operational follow-ups, not hidden acceptance:
V1 uses the explicitly authorized non-partitioned storage fallback to preserve the
global source-sequence unique key; MySQL and Kafka do not share an atomic commit;
same-server concurrent sequence handling is not serialized; and an actual
snapshot/resync agent remains Phase B gateway work. Independent review and the
Product Owner gate remain required.

## Addendum acceptance items

1. **A1 — PASS.** `POST /api/trading-data/ingest` authorizes
   `trading-data:ingest` through Q-009 before use, requires `ActorType.SERVICE`, and
   has a separate one-service/one-capability bootstrap. The operator bootstrap is
   unchanged.
2. **A2 — PASS WITH CONDITIONS.** Global `(sourceServerId, sourceSequence)`
   de-duplication, visible durable gap markers, the per-account Kafka key, bounded
   five-second send wait, and 429/503 failure mapping are implemented. Publication
   failure rolls back the local event/gap transaction. Cross-resource atomicity and
   concurrent per-server cursor ownership remain open; actual resync is Phase B as
   the Architecture specifies.
3. **A3 — PASS USING THE CLEARED V1 FALLBACK.** V9 provides full-retention metadata
   plus an opaque `BLOB`, a plain global unique key, and the account/time history
   index. It is deliberately non-partitioned because MySQL requires partition
   columns in every unique key. The cleared Design and prompt permit this exact V1
   fallback when partitioning would weaken idempotency; time partitioning remains a
   documented target condition.
4. **A4 — PASS.** Synthetic tests cover neutral arbitrary-byte envelopes,
   de-duplication/no republish, visible gap signalling, per-account Kafka key,
   bounded backpressure, authorization/default denial, database rollback, and
   V8-to-V9 migration/round-trip behavior without SDK, Windows, or a live Kafka
   broker.
5. **A5 — PASS.** The public-document research note compares only high-level MT4
   and MT5 semantics, identifies candidate neutrality approaches and SDK questions,
   and expressly disclaims ADR, canonical-field, and SDK-interface authority.

## Delivered areas

- Backend: 22 production Java files under `tradingdata`, four stable result codes,
  one authorized REST entry, JDBC/Kafka/metrics adapters, and module wiring.
- Data: additive V9 with `trading_data_event` and
  `trading_data_ingestion_gap`; no DML, expiry, external FK, or existing-table
  change.
- Authorization: `trading-data:ingest` and a separate Q-015 `SERVICE` bootstrap.
- Verification: eight Q-015 test classes (24 tests) plus one shared Flyway contract
  assertion; real MySQL V1-to-V9 coverage and a full repository gate.
- Documentation: A5 research note, Q-015 Lessons Learned, and this timestamped
  review package.

## Verification headline

- Focused Q-015/Flyway contract selection: 24 tests passed.
- Final full-backend real-MySQL-enabled gate: 342 tests, 0 failures, 0 errors,
  0 skipped.
- Q-015 real-MySQL class within the full gate: 5/5 passed.
- Static migration/boundary verification and `git diff --check`: passed.
- Frontend: not run because Phase A made no frontend change, as directed.

See `Verification.md`, `DesignTraceability.md`, `SecurityReview.md`, and
`OutstandingItems.md` for exact evidence and review attention items.
