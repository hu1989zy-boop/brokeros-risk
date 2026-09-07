# Q-015 Phase B (MT4) Implementation Design — MT4 Gateway

Status: **V2 (2026-09-07)**, part of the Phase B (MT4) §16.5-B bundle. The authoritative
build spec for the **MT4 gateway**; subordinate to the parent Requirement, the Phase A
addendum, ADR-024 (the canonical model), and the Phase B Architecture (V2). Vendor detail
stays in this gateway only (AGENTS.md L75).

**V2:** the gateway **produces canonical JSON directly to Kafka** (librdkafka) instead of
HTTP-POSTing to a platform endpoint; it emits **raw `QUOTE` ticks** (markout windowing is
a downstream Flink job, not the gateway); the mapper is written as **portable C++** (POD
input) so its golden tests run off-Windows. Phase A ingests by **consuming Kafka** (a
separate Java task).

## 0. Ground rules

- New component `gateway/mt4/` (C++17, x64 Windows). It `#include`s `MT4ManagerAPI.h`
  from the **gitignored** `mt-manager-libs/Include` (licensed SDK stays local); links
  nothing from the SDK (the API loads `mtmanapi64.dll` at runtime via `CManagerFactory`).
- **Read/ingest only.** Never call any MT4 write/trade/admin operation. Only
  `Connect/Login/PumpingSwitchEx/TradesRequest/ServerTime/Disconnect/Release` + reads.
- No MT4 native type crosses the Kafka boundary — only ADR-024 canonical events (JSON).
- No secret/PII in code, logs, or the repo. Credentials via env/secure config.
- Do **not** modify the Java modules from the gateway. The gateway only **produces to
  Kafka**; the Phase A Kafka-consumer ingestion (separate Java task) reads that topic.

## 1. Component structure (`gateway/mt4/`)

- `main.cpp` — arg/env parse, lifecycle, reconnect loop.
- `Mt4Session.{h,cpp}` — wraps `CManagerFactory`/`CManagerInterface`: connect, login,
  pumping subscription, snapshot pull, disconnect; owns the pump callback.
- **`CanonicalMapper.{h,cpp}` — PORTABLE C++** (compiles off-Windows): pure functions
  mapping a **plain POD** mirror of the native fields → ADR-024 canonical events, no I/O,
  no SDK header. `TradeRecord-POD → TRADE_ACTIVITY`; account → `ACCOUNT_STATE`; bid/ask →
  raw `QUOTE`. This is the Type-1 correctness core; its golden tests run on Linux/CI.
- `Envelope.{h,cpp}` — builds the envelope JSON (`envelopeVersion`, `platform`,
  `sourceServerId`, `sourceSequence`, `tradingAccountId`, `occurredAt`, `payload`) — the
  Kafka message value. Portable.
- `KafkaProducer.{h,cpp}` — **librdkafka** producer: `acks=all` + idempotent producer,
  topic `trading-data.canonical`, key = `accountRef`; unrecoverable produce failure → the
  gateway fails visibly (no silent loss). Portable (librdkafka builds on Linux+Windows).
- `SequenceSource.{h,cpp}` — monotonic per-connection counter + epoch on reconnect. Portable.
- **`Mt4Session.{h,cpp}` — the ONLY Windows-native glue:** `CManagerFactory`/pumping;
  reads real `TradeRecord`/quote/account and fills the POD for the mapper.
- `Config.{h,cpp}` — env/secure-config loader (MT4 server + manager login/password; Kafka
  brokers + credentials/ACL; `SOURCE_SERVER_ID`; server UTC offset override).
- `build.bat` (Windows, x64, `/MT`, `/I <mt-manager-libs>\Include`, `ws2_32.lib` +
  librdkafka) and a portable build (CMake) for the mapper/envelope/producer + tests.
- `tests/` — mapper golden unit tests (portable), run off-Windows.

## 2. Session + pumping (from the real API)

- `CManagerFactory factory; factory.WinsockStartup(); man = factory.Create(ManAPIVersion);`
- `man->Connect(server)`; `man->Login(managerLogin, password)`; check `== RET_OK`.
- **Server UTC offset:** compute `offset = ServerTime() − <real UTC now>` at login (the
  probe measured ~+3 h); store it; allow a config override. Used by the mapper for
  time→UTC.
- `man->PumpingSwitchEx(&OnPump, 0, this)`. The callback `void __stdcall OnPump(int
  code,int type,void* data,void* param)` runs on the API thread; it hands work to a
  queue consumed by the sender thread (do minimal work in the callback).
- **Snapshot:** on `PUMP_START_PUMPING`, pull `TradesRequest(&total)` and the user/
  account list → emit baseline `TRADE_ACTIVITY(lifecycleHint=STATE)` + `ACCOUNT_STATE`.
- **Events:**
  - `PUMP_UPDATE_TRADES` → the changed `TradeRecord` (from `data`, type `TRANS_ADD=0 /
    TRANS_DELETE=1 / TRANS_UPDATE=2`) → `TRADE_ACTIVITY` with `lifecycleHint`
    `OPEN/CLOSE/MODIFY`. (If a given build delivers only the code without a usable
    record pointer, re-pull via `TradesRequest`/`TradesGetByLogin` and diff — decide at
    implementation against the real callback payload; **do not guess** the `data`
    layout — verify it live and record the finding.)
  - `PUMP_UPDATE_BIDASK` → **raw `QUOTE`** events (no buffering/windowing in the gateway;
    markout is a downstream Flink job).
  - `PUMP_UPDATE_USERS` / margin events → `ACCOUNT_STATE`.
  - `PUMP_PING` → liveness timestamp only.
- **Reconnect:** on `PUMP_STOP_PUMPING` / connection loss, reconnect; on re-login start a
  **new sequence epoch**, re-snapshot, and emit an epoch-boundary marker so Phase A sees
  a visible discontinuity (no silent loss).

## 3. Canonical mapping (ADR-024) — MT4 specifics

`CanonicalMapper::fromTradeRecord(const TradeRecord&, TransType, offset) -> CanonicalEvent`:

- identity: `platform=MT4`, `orderRef=positionRef=to_string(order)`, `dealRef` absent,
  `accountRef=to_string(login)`.
- instrument: `symbol` (trim to the 12-char field), `digits`.
- side: `cmd` → neutral enum (`OP_BUY→BUY`, `OP_SELL→SELL`, limits/stops mapped,
  `OP_BALANCE→BALANCE`, `OP_CREDIT→CREDIT`); retain `sourceSideCode=cmd`.
- volume: `volumeLots = volume / 100.0`; `sourceVolume=volume`,
  `sourceVolumeUnit=MT4_CENTILOT`.
- prices: `openPrice`, `closePrice`, `sl`, `tp` (as-is, with `digits`).
- money: `profit`, `commission`, `swap=storage`, `taxes`; account `currency` from the
  account/group (Implementation notes where MT4 exposes it).
- reason: `reason` → neutral provenance enum; retain raw.
- time: `occurredAt = toUtc(open_time or timestamp, offset)`; retain `sourceTime` +
  `serverUtcOffsetSeconds`. Use `timestamp` (last-change) for MODIFY/CLOSE, `open_time`
  for OPEN — decide per `lifecycleHint`; document the choice.
- `lifecycleHint` from `TransType`.
- `canonicalVersion=1`.

`ACCOUNT_STATE`: `accountRef`, `balance`, `equity`, `credit`, `margin`, `currency`,
`occurredAt` (UTC). `QUOTE`: `symbol`, `bid`, `ask`, `occurredAt` (UTC).

## 4. Envelope + Kafka delivery (V2)

- `Envelope::build(event, seq)`: `envelopeVersion=<Phase A current>`, `platform=MT4`,
  `sourceServerId=<config>`, `sourceSequence=seq`, `tradingAccountId=event.accountRef`,
  `occurredAt=event.occurredAt` (UTC), `payload=<canonical event JSON>` — serialized as
  one Kafka message value (JSON).
- `KafkaProducer::produce(envelopeJson, key=accountRef)`: librdkafka, topic
  `trading-data.canonical`, **idempotent producer + `acks=all`** so Kafka guarantees
  per-partition order + no silent duplicate/loss; a delivery-report error that cannot be
  recovered → the gateway stops and exits **visibly** (parent §5.3(3)), never silently
  dropping. Ordering per account is Kafka's per-partition guarantee (key = accountRef).
- No HTTP, no Bearer token in the gateway. The producer authenticates to Kafka via
  **SASL/mTLS** using credentials from secure config (Architecture §3, §6).

## 5. Config (env / secure store)

`MT4_SERVER`, `MT4_MANAGER_LOGIN`, `MT4_MANAGER_PASSWORD`, `KAFKA_BROKERS`,
`KAFKA_TOPIC` (default `trading-data.canonical`), Kafka SASL/mTLS credentials/certs,
`SOURCE_SERVER_ID`, `MT4_SERVER_UTC_OFFSET` (optional override). All secret values must
be absent from the repo (env/secure store only).

## 6. Tests

- **Mapping unit tests (no server):** feed the **captured golden `TradeRecord`s** (from
  the intake) → assert the canonical output: `volumeLots` (79→0.79, 100→1.00),
  side/reason enums, `occurredAt` = UTC after applying the measured offset,
  `lifecycleHint` per trans-type, refs, money. These are the core correctness tests.
- **Envelope tests:** the envelope JSON matches the Phase A envelope shape; payload is the
  canonical JSON; `sourceSequence` monotonic; epoch resets on reconnect. Portable.
- **Live integration (x64 Windows, demo server):** connect → pumping → N neutral events
  **produced to Kafka** (the compose Kafka or a local broker) → assert consumed; a forced
  reconnect produces a visible epoch boundary. Report honestly what ran (server/broker
  availability) — never claim an un-run live check.

## 7. Out of scope (MT4 bundle)

- The **MT5 gateway** (later, own intake/design; reuses ADR-024).
- **Markout** windowing = a downstream **Flink** job (parent §5.2). The gateway emits
  **raw `QUOTE` ticks**; it does not buffer or window them.
- Position reconstruction / Evidence formation (parent §5.2, future Flink). Any MT4
  write operation. The gateway does not touch the Java modules (it only produces to Kafka).

## 8. Traceability

| Parent/ADR item | Gateway element |
| --- | --- |
| Neutral model (ADR-024) | `CanonicalMapper` (portable POD → canonical) |
| FR-004 reliability / envelope | `SequenceSource` + `Envelope` + `KafkaProducer` (idempotent, acks=all) + Phase A Kafka consumer |
| §5.3(3) no silent loss | reconnect epoch boundary + idempotent producer + visible produce-failure exit |
| FR-005 trusted source | Kafka SASL/mTLS + topic ACLs on `trading-data.canonical` |
| §5.3(1) markout | raw `QUOTE` on Kafka → **Flink** computes the ±30s window (downstream) |
| FR-008 read-only | no MT4 write calls anywhere |
| AGENTS.md L75 | vendor detail only in `gateway/mt4/` (Windows glue); canonical on Kafka |
