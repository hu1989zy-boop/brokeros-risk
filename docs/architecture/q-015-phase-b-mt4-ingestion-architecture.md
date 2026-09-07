# Q-015 Phase B (MT4) Architecture — MT4 Gateway → Kafka Ingestion

Status: **V2 (2026-09-07)**, part of the Phase B (MT4) §16.5-B bundle (Architecture +
ADR-024 + MT4-gateway Implementation Design). Authoritative over the Implementation
Design where they differ; subordinate to the parent Requirement, the Phase A addendum,
and ADR-024. Governed by the two `docs/engineering/` documents **and AGENTS.md**.

**V2 change (Product Owner, 2026-09-07):** transport is **Kafka-direct** (the gateway
publishes canonical events straight to Kafka) instead of an HTTP POST to a platform
endpoint; **markout windowing moves downstream to a future Flink job** (the gateway
emits raw ticks, not a windowed buffer); and Phase A's ingestion becomes a **Kafka
consumer → partitioned store** rather than an HTTP endpoint. Rationale: isolate the only
Windows/SDK-native piece behind Kafka so every downstream (the store, a future **Flink**
job, local macOS dev) consumes the same stream with no SDK dependency — which also lets
the non-gateway work be built and tested off-Windows. ADR-024 (the neutral model) is
unchanged.

Prerequisites satisfied: real MT4 SDK in hand, connectivity + live capture proven on x64
Windows (parent §17 — Phase B unblocked).

## 1. Topology (V2 — Kafka is the backbone)

```
 MT4 Manager server (broker)
   │  pumping (native callbacks)                                   [x64 Windows]
   ▼
 MT4 Gateway service  (C++, hosts mtmanapi64.dll — the ONLY SDK-native component)
   ├─ Connect + Login(manager) + PumpingSwitchEx
   ├─ map native TradeRecord / bid-ask / account -> NEUTRAL canonical event (ADR-024)
   ├─ normalize: time->UTC(+server offset), volume->lots, side/reason->neutral enums
   ├─ synthesize sourceSequence (per connection; MT4 gives none)
   └─ produce canonical JSON to Kafka (librdkafka)
   ▼
 Kafka topic  trading-data.canonical   (key = accountRef; the neutral, SDK-free boundary)
   ├───────────► Phase A ingestion CONSUMER (Java)  -> idempotency/gap -> partitioned store
   ├───────────► FUTURE Flink jobs  -> markout (windowed tick↔trade join), position
   │                                   reconstruction, risk metrics/alerts
   └───────────► local macOS dev / other consumers  (read canonical JSON directly)
```

The gateway is the **only** component that touches the MT4 SDK (AGENTS.md L75). Once an
event is on Kafka it is fully neutral (canonical JSON, `platform`-tagged) — no MT4 type
crosses the boundary. Every consumer (store, Flink, dev) is portable and SDK-free.

## 2. The MT4 gateway (what it does)

- **Session/pumping** via `CManagerFactory`→`CManagerInterface`:
  `Connect(server)` → `Login(managerLogin,password)` → `PumpingSwitchEx(OnPump,0,ctx)`.
- **Server UTC offset:** measure `ServerTime() − real-UTC` at login (capture: ~+3 h);
  used by the mapper for time→UTC (config override allowed).
- **Initial snapshot** on `PUMP_START_PUMPING` → emit current open trades + account as
  `lifecycleHint=STATE`/`OPEN` observations (baseline).
- **Steady state:**
  - `PUMP_UPDATE_TRADES(type, TradeRecord*)` → `TRADE_ACTIVITY` (`TRANS_ADD/UPDATE/DELETE`
    → `OPEN/MODIFY/CLOSE`).
  - `PUMP_UPDATE_BIDASK` → **raw `QUOTE`** events (no buffering/windowing in the gateway —
    markout is Flink's job downstream).
  - `PUMP_UPDATE_USERS`/margin → `ACCOUNT_STATE`. `PUMP_PING` → liveness only.
- **Sequence synthesis:** a monotonic `sourceSequence` per connection (MT4 has none);
  a new **epoch** on reconnect.
- **Kafka produce:** each canonical event → the Phase A envelope shape (JSON) → Kafka,
  key = `accountRef`. Producer set to `acks=all` + idempotent producer so a broker hiccup
  does not silently drop or duplicate; on unrecoverable produce failure the gateway fails
  visibly (no silent loss, parent §5.3(3)).
- **Reconnect / no silent loss:** reconnect → new sequence epoch → re-snapshot; emit an
  epoch-boundary marker so downstream sees a visible discontinuity.
- **Read-only:** subscribe/read only; never any MT4 write/trade operation (FR-008).

## 3. Phase A ingestion — revised to a Kafka consumer (V2)

Phase A (COMPLETE) built the reliability + partitioned store behind an HTTP endpoint.
V2 **re-triggers that same logic from a Kafka consumer** instead of the HTTP endpoint:

- A Kafka listener on `trading-data.canonical` drives the existing
  `TradingDataIngestionService` semantics: **idempotency** via the unique
  `(source_server_id, source_sequence)`; **gap detection** → visible
  `trading_data_ingestion_gap` markers; append to the partitioned `trading_data_event`
  store (payload = the canonical JSON, still stored opaquely).
- **Ordering** is Kafka's per-partition guarantee (key = `accountRef`); **backpressure**
  is the consumer's natural pull (no unbounded buffering).
- The HTTP `POST /api/trading-data/ingest` may be retired or kept as a test aid; the
  authoritative ingestion is the Kafka consumer. This is a bounded refactor of Phase A —
  the store schema (V9) and the reliability rules are **unchanged**; only the trigger
  moves (REST → Kafka listener).
- **Auth boundary:** the trust boundary becomes **Kafka-native** (SASL/mTLS + topic
  ACLs restricting who may produce to `trading-data.canonical`) rather than the Q-009
  `SERVICE`-actor HTTP check. The Q-015 `SERVICE` actor/capability remains defined but is
  no longer the ingestion gate (recorded as a Phase A revision).

## 4. Flink-readiness (future, not this bundle)

The design is deliberately Flink-friendly, so the deferred risk/analytics phase drops in
as another Kafka consumer group with no rework:

- canonical **event-time is UTC** (ADR-024) → correct Flink event-time windows/watermarks;
- **`sourceSequence`** → dedup/order in Flink;
- **append-only immutable events** → stream-native; **position reconstruction** and
  **markout** (windowed `QUOTE`↔`TRADE` join) are textbook Flink jobs (parent §5.2 defers
  them);
- **key = `accountRef`** → Flink keyed state (per-account); re-keyable to (account,symbol).
- Later niceties (not now): a schema registry + Avro/Protobuf for schema evolution; JSON
  V1 does not block Flink.

## 5. Work split (given Codex has no x64 Windows)

- **Windows gateway (C++, built/run on the x64 Windows host):** SDK session + canonical
  mapper + librdkafka producer. The only Windows/SDK-native piece; authored hand-in-hand
  and built on the Windows host. Its **canonical mapper is written as portable C++**
  (input = a plain POD mirror of the native fields, not the SDK header) so its **golden
  unit tests** (against the captured real records) compile and run off-Windows too.
- **Portable side (Java, off-Windows / Codex):** the Phase A **Kafka-consumer ingestion**
  refactor + the canonical event Java model + tests fed with **recorded canonical JSON**.
  This is the independently-reviewable, Codex-buildable half.

## 6. Deployment & secrets

- Gateway runs on x64 Windows next to `mtmanapi64.dll`; the Java platform stays on
  Linux/Docker; Kafka is the link (dev: the compose Kafka, reachable by the gateway;
  prod: a secured broker).
- MT4 manager login/password, Kafka credentials/ACL certs, `SOURCE_SERVER_ID`, the UTC
  offset override — all from **env/secure config, never code/logs/repo**. No manager
  credential or account-holder PII beyond the neutral canonical fields is logged or put
  on the stream (parent §8).

## 7. Source layout

- Gateway source in the repo under `gateway/mt4/` (our code, committed); it `#include`s
  `MT4ManagerAPI.h` via the **gitignored** `mt-manager-libs/` (licensed SDK stays local,
  never committed). librdkafka is an open-source dependency of the gateway build.
- The Kafka-consumer ingestion lives in the Java `tradingdata` module (portable).

## 8. Scope of this bundle (MT4 first)

- **In:** ADR-024 neutral model; the **MT4 gateway** (→ `TRADE_ACTIVITY` + `ACCOUNT_STATE`
  + raw `QUOTE`, Kafka-direct, sequence synthesis, snapshot/reconnect resync, read-only);
  the **Phase A Kafka-consumer ingestion** refactor.
- **Deferred:** the **MT5 gateway** (own intake; reuses ADR-024); **markout** + position
  reconstruction + risk = future **Flink** jobs (parent §5.2).

## 9. Testing posture

- **Gateway mapper** golden unit tests vs the captured real records (portable C++).
- **Kafka-consumer ingestion** tests (Java): recorded canonical JSON → idempotency/gap/
  store (real-MySQL), reusing the Phase A store gate.
- **Live slice** on x64 Windows: gateway → Kafka → consumer → store, plus a forced
  reconnect → visible epoch boundary.
