# ADR-024: Trading Data Canonical (Broker-Neutral) Model — Q-015 Phase B

- Status: **Accepted** — 2026-09-07 (Product Owner, as part of the Q-015 Phase B §16.5-B bundle).
- Scope: the broker-neutral **canonical event model** that MT4/MT5 gateways produce and
  platform-side consumers read. This is parent Q-015 §14's "single hardest, Type-1
  decision." Designed against the **real SDKs now in hand** (MT4 `MT4ManagerAPI.h`
  build 1260 + a live MT4 capture; MT5 `MT5APIManager.h` + `Bases/MT5APIDeal.h` etc.).
- Context docs: parent `docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md`;
  Phase A addendum + ADR-023 (the payload-opaque envelope that carries this model);
  `docs/architecture/q-015-phase-b-mt4-ingestion-architecture.md`.
- Neutrality obligation (AGENTS.md L75): MT4/MT5 vendor detail lives only in the
  gateway adapters; no MT4/MT5-specific structure reaches a consumer — only a
  `platform` provenance tag.

## Context — the two real models (from the SDKs)

| Aspect | **MT4** (`TradeRecord`, order-centric) | **MT5** (`IMTDeal`/Position/Order) |
| --- | --- | --- |
| Core unit | **Order == position** in one mutable `TradeRecord` | **Deal** (immutable execution) + separate Position + Order |
| Mutability | Mutable: real-time `UPDATE_TRADES` with `TRANS_ADD/UPDATE/DELETE` | Deals immutable; positions/orders mutate |
| Identity | `order` (int32), `login` (int32) | `Deal`/`Order`/`PositionID` (UINT64), `Login` (UINT64) |
| Side/op | `cmd`: `OP_BUY=0..OP_CREDIT=7` (8) | `Action`: `DEAL_BUY=0..` (21, incl. charge/dividend/tax/...) |
| Open/close | one record, `open_*`/`close_*` fields | `Entry`: `IN/OUT/INOUT/OUT_BY` per deal |
| Reason | `reason`: client/expert/dealer/signal/gateway/mobile/web/api (8) | `EnDealReason`: 20 (adds SL/TP/SO/rollover/settlement/...) |
| Volume | `volume` = 1/100 lot (int) | `Volume`/`VolumeExt` (UINT64, contract-scaled) |
| Time | `open_time`/`close_time`/`timestamp` = **server-local** (capture proved ~UTC+3, NOT UTC) | `Time` INT64 (server time) |
| Real-time | pumping callback `(code,type,data)` — **NO native per-event sequence** | pumping — same: no client-visible global sequence |

## Decision

Define **Trading Data** as an append-only stream of **immutable, broker-neutral
observation events**, each `platform`-tagged, carrying a **neutral superset** of
fields that both platforms map into without either leaking. The canonical event is
the **payload** the gateway serializes into the Phase A payload-opaque envelope; Phase
A stores/forwards it opaquely; consumers parse it.

### 1. Neutral event kinds

- **`TRADE_ACTIVITY`** — a trade/deal observation. MT4 `TradeRecord` (ADD/UPDATE/DELETE)
  and MT5 `IMTDeal` both map here.
- **`ACCOUNT_STATE`** — balance / equity / credit / margin (parent §5.3(1)).
- **`QUOTE`** — bid/ask tick (MT4 `UPDATE_BIDASK` / MT5 ticks), emitted **raw**; the ±30 s
  markout window is computed **downstream by a future Flink job** (a windowed `QUOTE`↔
  `TRADE_ACTIVITY` join), not by the gateway or this model.

Each event also carries a **`lifecycleHint`** (`OPEN | MODIFY | CLOSE | EXECUTE |
STATE`) derived neutrally (MT4 `TRANS_ADD→OPEN`, `TRANS_UPDATE→MODIFY`,
`TRANS_DELETE→CLOSE`; MT5 deal → `EXECUTE` with `Entry` retained). The stream is
append-only: MT4 mutations become successive immutable observations — this is how the
model neutralizes the MT4-mutable vs MT5-immutable difference (both become observation
streams; position reconstruction is a **future** consumer concern, not Q-015).

### 2. Neutral identity (superset; each platform fills what it has)

`accountRef`, `orderRef`, `dealRef?`, `positionRef?` — all **strings** (hold int32 and
UINT64), each tagged by `platform`. MT4 fills `orderRef==positionRef` (= order ticket)
and no `dealRef`; MT5 fills `dealRef` + `orderRef` + `positionRef` distinctly. Absent
refs are omitted, never faked.

### 3. Normalization rules (the core neutral decisions)

- **Time → UTC.** Every event's `occurredAt` is **UTC**. Because MT4 (and MT5) emit
  server-local time, the gateway records the **server UTC offset** (measured by
  comparing `ServerTime()` to real UTC — the capture measured MT4 at +3 h) and
  normalizes; it also retains `sourceTime` (raw) + `serverUtcOffsetSeconds`. **No event
  carries an un-normalized time without its offset.**
- **Volume → lots (decimal).** Canonical `volumeLots` (decimal) + `sourceVolume` (raw)
  + `sourceVolumeUnit` (`MT4_CENTILOT` | `MT5_*`). MT4: `volume/100`. MT5: divide by the
  symbol's contract/volume factor (pinned in the Implementation Design from the MT5
  symbol config). Retain raw so nothing is lost.
- **Side/operation → one neutral enum** covering trade sides (`BUY, SELL, BUY_LIMIT,
  SELL_LIMIT, BUY_STOP, SELL_STOP`) and non-trade operations (`BALANCE, CREDIT, CHARGE,
  COMMISSION, BONUS, DIVIDEND, TAX, CORRECTION, INTEREST, AGENT, SO_COMPENSATION,
  OTHER`). MT4 `cmd` (8) and MT5 `Action` (21) both map in; MT5's extras land on the
  superset; anything unmapped → `OTHER` **+ retain the raw code** (`sourceSideCode`).
- **Reason/provenance → one neutral enum** (`CLIENT, EXPERT, DEALER, SL, TP, STOP_OUT,
  ROLLOVER, GATEWAY, SIGNAL, MOBILE, WEB, API, SETTLEMENT, TRANSFER, MIGRATION,
  EXTERNAL, OTHER`), union of MT4 `reason` (8) and MT5 `EnDealReason` (20); retain raw.
- **Money** (`profit, commission, swap, fee, taxes`) — decimals in the account's
  deposit currency; retain the account `currency`.
- **Instrument** — `symbol` string + `digits` (price precision); MT5 wide-strings are
  decoded to UTF-8.

### 4. Provenance & envelope binding

Each canonical event carries `platform` (`MT4`|`MT5`), `sourceServerId` (the broker
server identity), and maps onto the Phase A envelope: envelope `platform`,
`sourceServerId`, `tradingAccountId` (= neutral `accountRef`), `occurredAt` (UTC),
`sourceSequence` (**synthesized by the gateway** — neither platform provides one), and
`payload` = the serialized canonical event (JSON in V1). Phase A treats the payload as
opaque; **this ADR is the schema for what is inside it.**

### 5. Serialization

V1: **JSON** (human-inspectable, schema-versioned via `canonicalVersion`). A compact
binary encoding is a possible later optimization; not V1.

## Consequences

**Positive**
- Genuinely neutral: MT4 order-as-position and MT5 deal/position/order both map to a
  common observation stream, tagged, without leaking either (parent §14 satisfied).
- Append-only immutable observations align with Phase A's store and defer position
  reconstruction (and markout) to a future consumer — a **Flink** job on the canonical
  Kafka stream (not Q-015's job).
- Time-as-UTC-with-offset and raw retention prevent the classic MT timezone bug (the
  capture proved MT4 ≠ UTC) and keep the model lossless/auditable.
- The MT5 side of the model is designed now (from real MT5 headers) even though the MT5
  gateway is built later — the model won't need re-cutting for MT5.

**Negative / trade-offs**
- A neutral superset means some fields are platform-specific-absent (optional) — the
  consumer must tolerate absence. Documented per field.
- JSON payload is larger than binary; acceptable for V1 (Phase A store is opaque bytes;
  size cap already 64 KiB).
- Volume/contract scaling for MT5 depends on symbol config — an MT5-gateway concern,
  pinned when the MT5 gateway is built.

## Alternatives rejected

- **Adopt MT5's deal/position/order model as canonical** — leaks MT5 onto MT4; MT4 has
  no deals. Rejected (non-neutral).
- **Adopt MT4's order-as-position as canonical** — cannot represent MT5 deals/entries;
  loses MT5 richness. Rejected.
- **Reconstruct positions in the gateway** — that is analysis, not ingestion (parent
  §5.2 defers it); keep Q-015 to raw neutral observations.
- **Emit un-normalized (server-local) time** — reintroduces the timezone bug the
  capture exposed. Rejected.

## Compliance / verification

- Neutrality: no MT4/MT5 native type in the canonical event; only a `platform` tag +
  optional refs. Enforced by review + the payload-opaque Phase A boundary.
- The MT4 gateway's mapping is tested with the real captured records (golden fixtures);
  time-offset normalization and volume-to-lots are unit-tested against the capture.
