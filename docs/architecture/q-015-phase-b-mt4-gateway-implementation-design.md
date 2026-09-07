# Q-015 Phase B (MT4) Implementation Design — MT4 Gateway

Status: **V1**, part of the Phase B (MT4) §16.5-B bundle. The authoritative build spec
for the **MT4 gateway**; subordinate to the parent Requirement, the Phase A addendum,
ADR-024 (the canonical model), and the Phase B Architecture. Vendor detail stays in
this gateway only (AGENTS.md L75).

## 0. Ground rules

- New component `gateway/mt4/` (C++17, x64 Windows). It `#include`s `MT4ManagerAPI.h`
  from the **gitignored** `mt-manager-libs/Include` (licensed SDK stays local); links
  nothing from the SDK (the API loads `mtmanapi64.dll` at runtime via `CManagerFactory`).
- **Read/ingest only.** Never call any MT4 write/trade/admin operation. Only
  `Connect/Login/PumpingSwitchEx/TradesRequest/ServerTime/Disconnect/Release` + reads.
- No MT4 native type crosses the HTTP boundary — only ADR-024 canonical events (JSON).
- No secret/PII in code, logs, or the repo. Credentials + endpoint via env/secure config.
- Do **not** modify the Java modules or Phase A. The gateway is an HTTP client of the
  existing `POST /api/trading-data/ingest`.

## 1. Component structure (`gateway/mt4/`)

- `main.cpp` — arg/env parse, lifecycle, reconnect loop.
- `Mt4Session.{h,cpp}` — wraps `CManagerFactory`/`CManagerInterface`: connect, login,
  pumping subscription, snapshot pull, disconnect; owns the pump callback.
- `CanonicalMapper.{h,cpp}` — pure functions mapping native → ADR-024 canonical events
  (no I/O; unit-tested). `TradeRecord → TRADE_ACTIVITY`; user/account → `ACCOUNT_STATE`;
  bid/ask → `QUOTE`.
- `Envelope.{h,cpp}` — builds the Phase A envelope JSON (`envelopeVersion`, `platform`,
  `sourceServerId`, `sourceSequence`, `tradingAccountId`, `occurredAt`, `payload`).
- `PhaseAClient.{h,cpp}` — HTTPS POST to the ingestion endpoint with the Bearer token;
  bounded retry/backoff on 429/503; a small ordered outbound buffer.
- `SequenceSource.{h,cpp}` — monotonic per-connection counter + epoch on reconnect.
- `Config.{h,cpp}` — env/secure-config loader (server, manager login, password, token,
  endpoint URL, server UTC offset override).
- `build.bat` — x64, `/MT`, `/I <mt-manager-libs>\Include`, `ws2_32.lib` + the HTTP/TLS
  lib (WinHTTP, built into Windows — no third-party). Documents the local SDK path.
- `tests/` — mapping unit tests against captured golden records.

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
  - `PUMP_UPDATE_BIDASK` → `QUOTE` into the rolling per-instrument tick buffer.
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

## 4. Envelope + delivery

- `Envelope::build(event, seq)`: `envelopeVersion=<Phase A current>`, `platform=MT4`,
  `sourceServerId=<server id/config>`, `sourceSequence=seq`,
  `tradingAccountId=event.accountRef`, `occurredAt=event.occurredAt`,
  `payload=<canonical event JSON>` (base64 per the Phase A request DTO).
- `PhaseAClient::post(envelopeJson)`: WinHTTP HTTPS POST to
  `POST /api/trading-data/ingest`, header `Authorization: Bearer <SERVICE token>`;
  success → advance cursor; `429/503` → bounded exponential backoff, **do not advance**
  (preserve per-account order); persistent failure past the buffer → exit visibly.
- The SERVICE token is obtained per the deployment's OIDC/`SERVICE`-actor mechanism
  (config); the gateway never embeds it.

## 5. Config (env / secure store)

`MT4_SERVER`, `MT4_MANAGER_LOGIN`, `MT4_MANAGER_PASSWORD`, `PHASE_A_URL`,
`PHASE_A_TOKEN` (or a token-acquisition config), `MT4_SERVER_UTC_OFFSET` (optional
override), `SOURCE_SERVER_ID`. All required-secret values must be absent from the repo.

## 6. Tests

- **Mapping unit tests (no server):** feed the **captured golden `TradeRecord`s** (from
  the intake) → assert the canonical output: `volumeLots` (79→0.79, 100→1.00),
  side/reason enums, `occurredAt` = UTC after applying the measured offset,
  `lifecycleHint` per trans-type, refs, money. These are the core correctness tests.
- **Envelope tests:** the envelope JSON matches the Phase A request DTO; payload is the
  canonical JSON; `sourceSequence` monotonic; epoch resets on reconnect.
- **Live integration (x64 Windows, demo server):** connect → pumping → N neutral events
  POSTed to a Phase A test endpoint (or a local stub) → assert accepted; a forced
  reconnect produces a visible epoch boundary. Report honestly what ran (server
  availability) — never claim an un-run live check.

## 7. Out of scope (MT4 bundle)

- The **MT5 gateway** (later, own intake/design; reuses ADR-024).
- Full **markout window emission** may be a second MT4 iteration if the first lands the
  trade/account stream cleanly (buffer the ticks now; window emission called out in
  `Verification.md` if deferred).
- Position reconstruction / Evidence formation (parent §5.2, future). Any change to
  Phase A or the Java modules. Any MT4 write operation.

## 8. Traceability

| Parent/ADR item | Gateway element |
| --- | --- |
| Neutral model (ADR-024) | `CanonicalMapper` |
| FR-004 reliability / envelope | `SequenceSource` + `Envelope` + `PhaseAClient` + Phase A |
| §5.3(3) no silent loss | reconnect epoch boundary + bounded buffer + visible failure |
| FR-005 SERVICE auth | `PhaseAClient` Bearer (Q-009 SERVICE, trading-data:ingest) |
| §5.3(1) markout | `QUOTE` rolling buffer (window emission per §7) |
| FR-008 read-only | no MT4 write calls anywhere |
| AGENTS.md L75 | vendor detail only in `gateway/mt4/` |
