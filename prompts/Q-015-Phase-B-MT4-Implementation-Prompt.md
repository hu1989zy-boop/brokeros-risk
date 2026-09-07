# Q-015 Phase B (MT4 Gateway) — Implementation Prompt

**CLEARED FOR USE — Product Owner approved the Q-015 Phase B (MT4) §16.5-B bundle
2026-09-07** (neutral canonical model + MT4 gateway first). As one §16.5-B bundle, the
Architecture V1, ADR-024 (Accepted), and the MT4-gateway Implementation Design V1 are
accepted with implementation authorized. Governed by the two `docs/engineering/`
documents **and AGENTS.md** — read them first. Prerequisites are satisfied: the real
MT4 SDK is in hand and connectivity + a live capture were proven on x64 Windows.

## Absolute boundaries (AGENTS.md)

- **Vendor detail lives ONLY in `gateway/mt4/`.** No MT4 native type, struct, or ID
  crosses the HTTP boundary — only ADR-024 canonical events (JSON). No MT4/MT5 type
  enters the Java modules or Phase A.
- **Read/ingest ONLY.** The gateway may only connect, log in, subscribe (pumping), read
  trades/account/quotes, and disconnect. It must **never** call any MT4 write/trade/
  admin operation.
- **Do not guess the SDK.** Use the real `MT4ManagerAPI.h` (in the gitignored
  `mt-manager-libs/Include`). Where the live callback's `data` layout for
  `PUMP_UPDATE_TRADES` is uncertain, **verify it live and record the finding** — do not
  invent it. (AGENTS.md L76/L173.)
- **No secret/PII** in code, logs, fixtures, or the repo. The licensed SDK stays under
  the gitignored `mt-manager-libs/` and is never committed.
- **Do not modify** the Java modules or Phase A; the gateway is only an HTTP client of
  the existing `POST /api/trading-data/ingest`.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md` (parent, APPROVED) +
   `docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md` (Phase A COMPLETE — the
   envelope + endpoint you deliver into).
2. `docs/adr/ADR-024-trading-data-canonical-model.md` (Accepted) — the neutral model.
3. `docs/architecture/q-015-phase-b-mt4-ingestion-architecture.md` (V1).
4. `docs/architecture/q-015-phase-b-mt4-gateway-implementation-design.md` (V1) — the
   authoritative build spec (structure §1; session/pumping §2; mapping §3; envelope §4;
   config §5; tests §6).

Reference reality: the real header `mt-manager-libs/Include/MT4ManagerAPI.h`
(`CManagerFactory`/`CManagerInterface`, `TradeRecord`, `PUMP_*`, `TRANS_*`, `OP_*`,
`TR_REASON_*`), Phase A's `IngestTradingDataRequest` DTO + `POST /api/trading-data/ingest`,
and the intake capture findings (MT4 time = server-local ~UTC+3; volume = 1/100 lot;
UPDATE_TRADES type = ADD/DELETE/UPDATE; no native sequence → the gateway synthesizes it;
reconnect re-snapshots).

## Task

Build `gateway/mt4/` exactly as the Implementation Design specifies, and only that: the
session (connect/login/pumping via `CManagerFactory`), the pure `CanonicalMapper`
(native → ADR-024 events), `SequenceSource` (synthesized monotonic per-connection
sequence with reconnect epochs), `Envelope` (Phase A envelope JSON, payload = canonical
JSON), `PhaseAClient` (WinHTTP HTTPS POST, SERVICE-actor Bearer, bounded retry on
429/503, ordered outbound buffer, visible failure on exhaustion), config from
env/secure store, the x64/`/MT` `build.bat`, and the mapping unit tests against the
captured golden records. Deliver `TRADE_ACTIVITY` + `ACCOUNT_STATE`; buffer `QUOTE`
ticks (markout window emission may be a called-out second iteration).

## Hard boundaries — do not do these

- No MT4 write/admin/trade operation; read-only.
- No MT4 native type past the HTTP boundary; no change to the Java modules or Phase A;
  no new capability (reuse the Phase A `trading-data:ingest` SERVICE actor).
- No secret/PII/credential in code, logs, fixtures, or the repo; never commit the SDK.
- Do not stage, commit, or push. Do not modify any existing timestamped review package.
- On any contradiction, resolve toward the approved docs (Parent/Addendum > ADR-024 >
  Architecture > Design) and record the assumption in `OutstandingItems.md`.

## Environment honesty (important)

- **Mapping unit tests** (no live server) are the core deliverable and MUST run: feed
  the captured golden `TradeRecord`s and assert the canonical output (volume→lots,
  time→UTC with the measured offset, side/reason enums, lifecycleHint, refs, money).
- **Live integration** (connect → pumping → events POSTed to a Phase A test endpoint,
  plus a forced reconnect → visible epoch boundary) runs on the x64 Windows host if
  available. If the demo server / Phase A endpoint is not reachable in your environment,
  say so explicitly in `Verification.md` and deliver the code + mapping tests; **never
  claim a live check that did not run.**
- Building the C++ gateway needs the x64 toolchain + the local SDK path; report exactly
  what compiled/ran.

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-015/review-q-015-phase-b-mt4-v<N>-implementation-<YYYYMMDD-HHMMSS>/` with at
least: `Summary.md`, `ArchitectureReview.md` (against ADR-024 + Design),
`DesignTraceability.md` (map each Design section / parent FR to code/test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact
commands, tool availability, pass/fail/skip — honest; mapping-test results + live-slice
status; **confirm read-only, no native type past the boundary, no Java/Phase A change,
SDK not committed**), `SecurityReview.md` (read-only; secrets/PII handling; SERVICE-actor
auth), `TestInventory.txt`, `OutstandingItems.md` (incl. any live-verified `data`-layout
finding for `PUMP_UPDATE_TRADES`, and whether markout emission is deferred). Add
`docs/lessons/<date>-q-015-phase-b-mt4-implementation.md`.

This package is for Claude Code's independent implementation review — not your own
sign-off. State PASS/FAIL against each Design section honestly; list every open
question and assumption. Stop after the review package; do not begin the MT5 gateway.
