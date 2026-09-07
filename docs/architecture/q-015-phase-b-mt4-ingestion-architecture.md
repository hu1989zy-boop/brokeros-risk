# Q-015 Phase B (MT4) Architecture — MT4 Gateway Ingestion

Status: **V1**, part of the Phase B (MT4) §16.5-B bundle (Architecture + ADR-024 +
MT4-gateway Implementation Design) authorized 2026-09-07. Authoritative over the
Implementation Design where they differ; subordinate to the parent Requirement, the
Phase A addendum, and ADR-024. Governed by the two `docs/engineering/` documents **and
AGENTS.md**.

Prerequisites now satisfied: the real MT4 Manager API SDK is in hand
(`mt-manager-libs/`, gitignored), and connectivity + a live sample capture were proven
on x64 Windows. Phase B is therefore unblocked (parent §17); this bundle scopes the
**neutral model (now)** + the **MT4 gateway (first)**; the MT5 gateway follows its own
intake.

## 1. Topology

```
 MT4 Manager server (broker)
   │  pumping (native callbacks, Manager API)      [x64 Windows]
   ▼
 MT4 Gateway  (new C++ component, hosts mtmanapi64.dll)
   ├─ Connect(server) + Login(manager) + PumpingSwitchEx(callback)
   ├─ map native TradeRecord / bid-ask / account  ->  NEUTRAL canonical event (ADR-024)
   ├─ normalize: time -> UTC (+server offset), volume -> lots, side/reason -> neutral enums
   ├─ synthesize sourceSequence (per connection; MT4 gives none)
   ├─ wrap in the Phase A payload-opaque envelope (payload = canonical event JSON)
   ▼  HTTPS POST  (Bearer JWT: Q-009 SERVICE actor, trading-data:ingest)
 Phase A ingestion endpoint  POST /api/trading-data/ingest   [existing, Linux/Docker]
   └─ idempotency / gap markers / ordering / backpressure -> Kafka + partitioned store
```

The gateway is the **only** component that touches the MT4 SDK (AGENTS.md L75). It is a
network client of the MT4 server and an authorized client of the Phase A endpoint. The
platform side (Phase A) is **unchanged** — it still receives an opaque payload.

## 2. What the gateway does (from the real capture)

- **Connect/login/pumping** via `CManagerFactory` → `CManagerInterface`:
  `Connect` → `Login(managerLogin, password)` → `PumpingSwitchEx(OnPump, 0, ctx)`.
- **Initial snapshot** on subscribe (the capture showed a burst: `START_PUMPING`,
  `UPDATE_SYMBOLS/GROUPS/USERS/ONLINE/TRADES/...`) → emit the current open trades +
  account state as `lifecycleHint=STATE`/`OPEN` observations (a clean baseline).
- **Steady state**:
  - `UPDATE_TRADES(type, TradeRecord*)` → one `TRADE_ACTIVITY` event; `type`
    `TRANS_ADD/UPDATE/DELETE` → `lifecycleHint OPEN/MODIFY/CLOSE`.
  - `UPDATE_BIDASK` → `QUOTE` events feeding the **rolling per-instrument tick buffer**;
    on an order open/close, emit that instrument's `[event−30s, event+30s]` window
    (parent §5.3(1) markout). (Markout may be a second gateway iteration — see §6.)
  - account/margin updates → `ACCOUNT_STATE`.
  - `PING` → liveness only (not emitted).
- **Sequence synthesis (key finding):** the pump callback carries no native sequence, so
  the gateway assigns a **monotonic `sourceSequence` per connection** (a counter,
  reset+resnapshot on reconnect). This is exactly the contract Phase A's envelope
  requires (addendum §5.3(2)); MT4 cannot supply it, so the gateway is the source of
  truth for ordering/dedup.
- **Reconnect / no silent loss:** on disconnect the gateway reconnects and MT4 re-sends
  a full snapshot; the gateway starts a **new sequence epoch** and emits a boundary so
  the Phase A gap detector sees the discontinuity (visible, never silent — parent
  §5.3(3)). Re-snapshot is the resync.
- **Read-only:** the gateway only reads/subscribes; it never calls any MT4 write/trade
  operation (parent FR-008).

## 3. Delivery to Phase A

- The gateway POSTs each canonical event as a Phase A envelope to
  `POST /api/trading-data/ingest`, authenticating as the **Q-015 `SERVICE` actor**
  (`trading-data:ingest`) — the same bootstrap Phase A already defined. Backpressure
  (HTTP 429/503) → the gateway retries with bounded backoff, preserving order per
  account (it does not advance its send cursor past an un-acked event).
- The gateway holds a **small local outbound buffer** so a brief Phase A hiccup does not
  drop MT4 events; if the buffer is exhausted it fails visibly (no silent loss) rather
  than discarding.

## 4. Deployment & secrets

- The gateway runs on the **x64 Windows** host next to `mtmanapi64.dll`. It is a
  separate deployable from the Java platform (which stays on Linux/Docker).
- **Secrets** (MT4 manager login/password, the SERVICE-actor token/credentials, the
  Phase A endpoint URL) come from **environment/secure config, never from code or the
  repo** (Principles §4; parent §8). No manager credential or account-holder PII is
  logged or placed on the stream beyond the neutral canonical fields.

## 5. Source layout

- The gateway **source** lives in the repo under `gateway/mt4/` (our code, committed);
  it `#include`s `MT4ManagerAPI.h` via an include path to the **gitignored**
  `mt-manager-libs/` (the licensed SDK stays local, never committed). A build script
  documents the local SDK path + the x64/static-runtime flags proven by the probe.
- No change to the Java modules; Phase A is consumed over HTTP only.

## 6. Scope of this bundle (MT4 first)

- **In:** the neutral model (ADR-024, both platforms) + the **MT4 gateway** delivering
  `TRADE_ACTIVITY` + `ACCOUNT_STATE` (and `QUOTE`/markout at least buffered) to Phase A,
  with sequence synthesis, snapshot/reconnect resync, read-only, SERVICE-actor auth.
- **Deferred:** the **MT5 gateway** (its own intake + design, reusing this neutral
  model); full markout windowing may be a second MT4 iteration if the first lands the
  core trade/account stream (called out in the Implementation Design); position
  reconstruction / Evidence formation (parent §5.2, future).

## 7. Testing posture

- **Gateway mapping** unit-tested against the **captured golden records** (the real
  TradeRecord sample) — time→UTC with the measured +offset, volume→lots, side/reason
  enums, lifecycleHint from trans-type — no live server needed for the mapping tests.
- **Live gateway integration** on x64 Windows against the demo MT4 server (connect →
  pumping → a handful of neutral events POSTed to a Phase A test endpoint) — the real
  end-to-end slice.
- Phase A already has its own reliability/store tests (unchanged).

## 8. Deliverables

ADR-024 (the model). This Architecture. The MT4-gateway Implementation Design (the
build spec). The MT5 gateway is a later bundle.
