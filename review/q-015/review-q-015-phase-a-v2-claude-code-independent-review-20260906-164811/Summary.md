# Q-015 Phase A — Claude Code Independent Implementation Review (v2)

- Requirement: Q-015 Trading Data Ingestion Foundation — **Phase A** (SDK-Independent
  Addendum), V1
- Reviewed: Codex v1 delivery (`review-q-015-phase-a-v1-implementation-20260906-013112`)
- Baseline: `a783e48` (Q-015 Phase A addendum + §16.5-B bundle)
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-06
- **Gate Decision: PASS** (I concur with Codex's *PASS WITH CONDITIONS*; the four
  conditions are real, correctly characterized, and appropriately deferred — none is a
  defect in the authorized Phase A scope)

## Verdict

The SDK-independent ingestion foundation, delivered cleanly and — most importantly —
**within the AGENTS.md boundary, which is not just met but automatically guarded**. A
new `com.brokeros.risk.tradingdata` module accepts a versioned, **payload-opaque**
envelope over a Q-009 `SERVICE`-actor-authorized endpoint, enforces reliability from
envelope metadata only, persists to a full-retention store, and publishes to Kafka
keyed by account. I independently reproduced the backend real-MySQL gate and
code-reviewed the boundary, the authorization, and the reliability/dual-write handling.

## Independently reproduced

| Check | Result |
| --- | --- |
| Backend full real-MySQL gate (Docker MySQL 8.4 + Maven 21, full repo mounted) | **BUILD SUCCESS — 342 tests, 0 failures, 0 errors, 0 skipped** |
| Q-015 test classes (8) | **24/24** — `Q015MySqlTests` **5/5** (opaque round-trip, idempotency, gap, history, migration), plus domain/application/REST/messaging/architecture/bootstrap |
| Flyway | **V9 applied (9 migrations)**; migration-count assertion updated |
| SDK-independence boundary | **confirmed** — no `gateway` package, no MT4/MT5 native symbol, payload `byte[]`/`BLOB` never parsed (tested with non-UTF-8 bytes `FF 00 C3 28`); operator bootstrap untouched; `ResultCode` purely additive (4 new codes); no Q-008…Q-014 change |

## Code review (correct)

- **A1 — authorization boundary.** `POST /api/trading-data/ingest` →
  `TradingDataIngestionService`: `requireAllowed(TradingDataCapabilities.INGEST)` then
  `requireService` (`ActorType.SERVICE` → else `TRADING_DATA_ACTOR_TYPE_NOT_PERMITTED`),
  both before any persistence; default-deny. New `trading-data:ingest` capability + a
  **separate** `q015-ingestion-bootstrap.json` (`actorType: SERVICE`); the operator
  bootstrap is unchanged.
- **A2 — reliability by metadata.** Idempotency via a unique
  `(source_server_id, source_sequence)` (re-append → `DUPLICATE`, not re-published);
  gap detection (`seq − lastContiguous > 1`) writes a **visible** `trading_data_ingestion_gap`
  marker; per-account ordering via Kafka key = `tradingAccountId`; backpressure via a
  bounded 5 s send → `TRADING_DATA_BACKPRESSURE` (429/503). **Dual-write handled the
  right way:** `publish` runs **inside** the DB transaction, so a publish failure rolls
  back the event/gap rows — the design errs toward duplicate delivery (at-least-once)
  rather than silent loss, honouring §5.3(3).
- **A3 — storage.** V9 `trading_data_event` (opaque `BLOB` payload + queryable
  metadata, `UNIQUE (source_server_id, source_sequence)`, `INDEX (trading_account_id,
  occurred_at)`, no FK, no TTL) + `trading_data_ingestion_gap`. **Non-partitioned V1**
  — the *explicitly cleared* Design §5 fallback, taken because MySQL requires the
  partition column in every unique key, which would weaken the global idempotency key.
  Correct call; partitioning stays the documented target.
- **A4 — tests.** Synthetic-envelope coverage of opaque round-trip, dedup/no-republish,
  visible gap, per-account key, backpressure, `SERVICE` default-deny, and **publish
  rollback** — no SDK/Windows/live broker.
- **A5 — research note.** Public-doc-only; explicitly defines no canonical schema and
  claims no Manager API operation; lists SDK questions for Phase B. Rule-respecting.
- **Bonus:** `verify-static.sh` gained guards that **reject** `manager api|native
  adapter|order_ticket|deal_ticket|position_ticket` and non-additive DDL in V9 — the
  payload-opaque/SDK-independent boundary is now enforced in CI, plus
  `TradingDataArchitectureTests`.

## Conditions (I concur — deferred, not defects)

1. **Partitioning target** — V1 non-partitioned (cleared fallback); a future design
   must reconcile partition pruning with the global idempotency key.
2. **MySQL/Kafka atomicity** — at-least-once by design; do not infer exactly-once. An
   outbox / transactional producer / reconciliation is future authorized work.
3. **Concurrent per-server sequence ownership** — V1 assumes a single server's calls
   are serialized; define same-server concurrency/late-arrival/gap-overlap semantics
   before concurrent production ingress.
4. **Kafka deployment validation** — topic partitions, replication/min-ISR, producer
   idempotence/acks, ACL/TLS, max message size, real-broker backpressure — deployment
   time.

These are the right things for a foundation phase to defer; each is Phase-B / operational
and none blocks acceptance of the authorized Phase A scope.

## Acceptance — reviewer view

| Item | Result |
| --- | --- |
| A1 SERVICE-actor authorized ingest | **PASS** |
| A2 reliability by metadata | **PASS** (conditions 1–4 deferred) |
| A3 full-retention store (cleared non-partitioned V1) | **PASS** |
| A4 synthetic replay harness | **PASS** |
| A5 pre-SDK research note | **PASS** |
| Boundary: payload opaque, no SDK interface, no canonical model, no gateway, no Q-008…Q-014 change, Q-009 reused, read-only | **PASS** — reproduced + CI-guarded |
| Backend gate | **PASS** — 342/0/0, V9 |

## Recommendation

**Accept Q-015 Phase A V1.** The SDK-independent ingestion foundation is correct,
boundary-clean (and now boundary-guarded in CI), and independently verified (342
backend + 24 Q-015 + V9). The four PASS-WITH-CONDITIONS items are real, honest, and
correctly deferred to Phase B / operational hardening. The reviewer changed no code.
Phase B (canonical model + gateways + x64 Windows) remains parked on the parent §17.
