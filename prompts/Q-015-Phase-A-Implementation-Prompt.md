# Q-015 Phase A (SDK-Independent Ingestion Foundation) — Implementation Prompt

**CLEARED FOR USE — Product Owner approved Q-015 Phase A V1 and authorized the §16.5-B
bundle 2026-09-06.** As one §16.5-B bundle at the implementation-authorization gate,
the Architecture V1, ADR-023 (Accepted), and Implementation Design V1 are accepted
with implementation authorized. Recorded in
`docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md` §17 and each bundle
document's status. Governed by the two `docs/engineering/` documents **and
`AGENTS.md`** — read them first.

## Absolute boundary (AGENTS.md — do not cross)

**This is Phase A: SDK-INDEPENDENT. The event `payload` is OPAQUE bytes end to end.**
Do **not** invent any MT4/MT5 Manager API interface, do **not** describe or assume any
native field, do **not** commit a canonical field model, and do **not** create any
`gateway` package or native adapter. If you feel you need a payload field to proceed,
you are over the line — keep it opaque and stop. (AGENTS.md L75/L76/L98; parent Q-015
§17; addendum §7.)

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md` (APPROVED; §5.3 all
   CONFIRMED; §17) — and its parent `Q-015-Trading-Data-Ingestion-Foundation.md` for
   context (parent is parked for Phase B).
2. `docs/adr/ADR-023-phase-a-payload-opaque-ingestion.md` (Accepted).
3. `docs/architecture/q-015-phase-a-ingestion-architecture.md` (V1).
4. `docs/architecture/q-015-phase-a-ingestion-implementation-design.md` (V1) — the
   authoritative build spec (domain §1; endpoint §2; service §3; adapters §4; schema
   §5; bootstrap §6; tests §7).

Reuse the patterns of: the Q-011 Evidence module (record service authorize→validate→
persist; `requireHuman`), `JdbcAuthorizationAdapter` / `AuthorizationGuard`, a
`Jdbc…QueryAdapter`, a `…Controller` + request DTO, and `SecurityBootstrapCommand`
(which already supports `"actorType": "SERVICE"`). `spring-kafka` is already a
dependency.

## The confirmed shape — summary; the four documents are authoritative

- **New module** `com.brokeros.risk.tradingdata` (domain / application /
  infrastructure / interfaces) — **no `gateway` subpackage**.
- **Ingestion endpoint** `POST /api/trading-data/ingest`, authorized as a Q-009
  **`SERVICE`** actor holding a **new** `trading-data:ingest` capability (default-deny;
  `requireService`), provisioned via a **new** `deploy/keycloak/q015-ingestion-
  bootstrap.json` (`actorType: SERVICE`) — **do not modify** `q016-security-
  bootstrap.json`.
- **Provisional envelope** `{ envelopeVersion, platform (MT4|MT5 tag), sourceServerId,
  sourceSequence, tradingAccountId, occurredAt, payload (opaque bytes / base64) }` —
  metadata validated, **payload never parsed**.
- **Reliability by metadata:** idempotent de-dup via unique `(source_server_id,
  source_sequence)`; gap detection writing a **visible** `trading_data_ingestion_gap`
  marker (no silent loss); per-account ordering via Kafka key = `tradingAccountId`;
  backpressure via synchronous accept-after-persist with a throttle response.
- **Storage (new migration V9):** partitioned full-retention `trading_data_event`
  (opaque payload + queryable metadata) + `trading_data_ingestion_gap`. Follow Design
  §5 on the MySQL partition/unique-key interaction — and if partitioning forces an
  awkward unique constraint, you MAY ship V1 non-partitioned with the plain
  `UNIQUE (source_server_id, source_sequence)` **and call it out** in `Verification.md`
  (idempotency correctness is the invariant; partitioning is the target).
- **Kafka** topic `trading-data.canonical`, key `tradingAccountId` (first real use).
- **A5 research note** `docs/2026-09-06-mt4-mt5-neutrality-pre-sdk-notes.md`: compare
  MT4 (order-as-position) vs MT5 (deal/position/order) semantics **from PUBLIC
  MetaTrader documentation only**, list candidate canonical-model neutrality options
  and open questions to resolve **against the SDK**. It must **invent no SDK interface,
  assert no Manager API operation, and commit no field-level schema** — clearly marked
  as pre-SDK research for the Phase B Architecture.

## Task

Implement Q-015 Phase A exactly as specified in the Implementation Design, and only
that: the `tradingdata` domain + ingestion endpoint + service (reliability) + ports +
JDBC/Kafka adapters + the V9 schema + the `trading-data:ingest` capability + the
`SERVICE`-actor bootstrap file + module wiring; the tests (application + real-MySQL
partitioned-store round-trip + contract); and the A5 research note. Update the dynamic
migration-count assertion for V9.

## Hard boundaries — do not do these

- **Payload opaque; SDK-independent** (see the absolute boundary above). No native
  field, no invented Manager API interface, no canonical field model, no `gateway`.
- **Read-only ingest:** never call out to any MT4/MT5/broker system.
- **No new capability beyond `trading-data:ingest`**; do **not** modify the operator
  bootstrap or any Q-008…Q-014 module; reuse Q-009 unchanged.
- No PII/credential/opaque-payload-content in logs or fixtures.
- Do not stage, commit, or push. Do not modify any existing timestamped review package.
- On any contradiction, resolve toward the approved documents (Addendum > ADR-023 >
  Architecture > Design) and record the assumption in `OutstandingItems.md`.

## Environment honesty (important)

Run the backend real-MySQL gate for the new endpoint/store (the project's disposable
MySQL pattern) and the full repository gate — report real pass/fail/skip counts, mount
the **full repo** for the Docker run, and confirm the diff is the new `tradingdata`
module + V9 + bootstrap only (no Q-008…Q-014 change). Kafka: if a broker is not
available in the test environment, the publisher must be tested with a test double /
embedded broker or the send mocked — say exactly what ran in `Verification.md`; never
claim a check passed that did not run. Run frontend gates only if you touch frontend
(you should not — Phase A is backend-only).

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-015/review-q-015-phase-a-v<N>-implementation-<YYYYMMDD-HHMMSS>/` with at
least: `Summary.md`, `ArchitectureReview.md` (against ADR-023 + Design),
`DesignTraceability.md` (map each addendum item / Design section to code/test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact
commands, tool availability, pass/fail/skip — honest; backend real-MySQL results,
Kafka test approach, migration-count update; **confirm payload stays opaque, no SDK
interface, no canonical model, no Q-008…Q-014 change, operator bootstrap untouched**),
`SecurityReview.md` (`SERVICE`-actor default-deny; separate ingestion bootstrap;
read-only; no payload content logged), `TestInventory.txt`, `OutstandingItems.md`. Add
`docs/lessons/<date>-q-015-phase-a-implementation.md`.

This package is for Claude Code's independent implementation review — not your own
sign-off. State PASS/FAIL against each addendum acceptance item honestly; list every
open question and assumption. Stop after producing the review package; do not begin
Phase B or any other Requirement.
