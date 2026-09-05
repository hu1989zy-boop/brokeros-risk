# Q-015: Trading Data Ingestion Foundation (MT4/MT5 Pumping)

## Status

V1, by Claude Code holding the external Architect role, per
`docs/engineering/AI-Engineering-Execution-Protocol.md` and
`docs/engineering/Architecture-and-Design-Decision-Principles.md`.
**The authoritative live status is §17 (Current Gate)**, authoritative if
any summary above it disagrees (per Execution Protocol §16). This is the
Requirement stage of a new task; the three business-scope questions in
§5.3 are surfaced for explicit Product Owner confirmation (Decision
Authority §16.2) rather than silently assumed.

- Requirement ID: `Q-015`
- Architecture phase: Phase 1
- Realizes: Q-007 / ADR-009's **"Trading Data"** — the broker-neutral
  upstream supporting context that feeds the Core Domain. Q-007 named it
  and deferred its implementation; Q-015 implements it.
- Depends on: Q-009 (trusted actor/authorization — the ingestion gateway
  authenticates as a `SERVICE` actor). Feeds (does not modify): Q-011
  Evidence and the rest of the Core Domain.
- **Hard external prerequisite for the Architecture and Implementation
  stages (not this Requirement):** the real MT4 and MT5 Manager API SDKs
  and an x64 Windows test environment. Per `AGENTS.md` ("never invent
  Manager API interfaces without the real SDK"), the field-level canonical
  model and the gateway interfaces cannot be designed until the real SDK
  data structures are in hand. This Requirement defines scope and business
  boundaries only and does not invent any SDK interface.

## 1. Background

Every Core-Domain module so far (Q-009…Q-014) runs on `MANUAL`,
human-recorded input. The system therefore has no real trading data
flowing into it: Evidence, Decision, Action, and Risk Case are all fed by
hand. Q-007/ADR-009 anticipated this gap by naming **Trading Data** as the
broker-neutral upstream supporting context and the raw source from which
Evidence is later formed — while deferring its implementation.

Q-015 implements that upstream: a real-time ingestion of trading activity
from broker MT4 and MT5 servers, so the risk platform can observe actual
deals, positions, and account state as they happen. This is the first
genuinely **automated** (non-`MANUAL`, `SERVICE`-sourced) capability in
the system, and the first to use the streaming backbone (Kafka) that has
been in the Phase 1 stack, unused, since Q-001.

### 1.1 Why pumping (not REST)

MT4/MT5 Manager APIs deliver real-time trading activity through a
**pumping** model: after a Manager client subscribes, the server *pushes*
events (deals/trades, positions, orders, account/balance changes, quotes)
via callbacks over a persistent, stateful connection. A request/response
REST query cannot deliver this real-time firehose. A broker risk platform
whose core value is watching exposure as it forms therefore requires the
pumping ingestion path; REST is not a substitute for it.

## 2. Existing Capability and Gap Analysis

| Need | Existing capability | Gap |
| --- | --- | --- |
| Real trading activity flowing into the risk platform | None — all input is `MANUAL` | This is what Q-015 must deliver |
| A broker/platform-neutral representation of trading activity | Q-007 named "Trading Data" but built nothing | Q-015 must define and deliver the canonical model + ingestion |
| A trusted identity for an automated ingestion process | Q-009 `SERVICE` actor type + `AuthorizationGuard` | Reusable — the gateway authenticates as a `SERVICE` actor |
| A streaming backbone | Kafka is in the Phase 1 stack, unused | Q-015 is its first real use |
| Isolation of MT4/MT5 vendor detail from the Core Domain | ADR-009 / §15 neutrality principle; adapter pattern | Q-015 must keep native detail behind a Gateway/Adapter |
| MT4/MT5 Manager SDK + x64 Windows env | Not present | **External prerequisite the Product Owner must supply for the Architecture/Implementation stages** |

## 3. Problem Statement

The Core Domain has no real input. Without Q-015, the entire risk platform
can only be exercised by hand and cannot monitor a live broker book.

Q-015 must ingest real-time trading activity from MT4 and MT5 Manager
servers via pumping, normalize both platforms' native events into one
broker-neutral canonical Trading Data model, deliver that stream reliably
onto the platform (Kafka) for the Core Domain to consume, and keep all
MT4/MT5 vendor detail isolated behind a gateway boundary — without the
Core Domain ever seeing a platform-specific shape, and without inventing
any Manager API interface before the real SDK is available.

## 4. Goals

1. Establish **Trading Data** as a real, ingested, broker-neutral,
   real-time event stream — the Q-007 upstream supporting context — as the
   raw source that feeds the Core Domain.
2. Ingest from both MT4 and MT5 Manager servers via the pumping model, and
   **normalize both into ONE canonical, platform-neutral Trading Data
   event model** before any event reaches the platform stream. The Core
   Domain never sees an MT4- or MT5-specific structure (only a `platform`
   provenance tag).
3. Isolate each platform's native SDK inside a dedicated **Gateway**
   process behind a Port/Adapter; a failure or restart of one platform's
   gateway must not stop the other's ingestion (failure isolation /
   bulkhead).
4. Deliver canonical Trading Data events onto **Kafka** with defined
   reliability: reconnect after disconnect, gap recovery
   (snapshot + resync so a disconnect cannot silently lose events),
   idempotent/de-duplicated delivery, ordering guarantees the risk domain
   can rely on, and backpressure handling.
5. Authenticate each gateway as a trusted Q-009 `SERVICE` actor; no
   caller-supplied identity, no bypass of the authorization model.
6. Keep **Trading Data distinct from Evidence** (§5.2): ingested data is
   the raw observation stream, not automatically a risk Evidence record;
   Evidence is created *from* Trading Data by later, separately-approved
   logic or humans.
7. Reuse Q-009 unchanged; introduce no vendor SDK type into the Core
   Domain; do not modify any Q-008…Q-014 module.

## 5. Scope and Non-Goals

### 5.1 In Scope

- The **canonical broker-neutral Trading Data event model** (the central
  Type-1 decision; its field-level design is deferred to the Architecture
  stage pending the real SDK, but its *existence, purpose, and neutrality
  contract* are defined here).
- Two platform ingestion **gateways** (MT4, MT5), each maintaining a
  pumping subscription to one or more Manager servers, translating native
  callbacks into canonical Trading Data events. (Runs on x64 Windows;
  see §11.)
- The **canonical-event → Kafka** delivery pipeline, with the reliability
  properties in Goal 4.
- A trusted `SERVICE`-actor identity and authorization boundary for each
  gateway (reusing Q-009).
- **Full durable retention** of ingested canonical Trading Data in a
  **partitioned, query-optimized store** (not a single table), designed
  for historical order lookup at scale, and sufficient for gap recovery,
  replay, and audit (§5.3(2)).
- **Markout tick capture:** on each order open and close, the instrument's
  ticks over `[event − 30s, event + 30s]` (requiring a short rolling
  per-instrument tick buffer), for computing markout — not the full tick
  firehose (§5.3(1)).
- The consumer-facing canonical event contract the Core Domain (initially
  a future Evidence-formation capability) will read.
- Verification via **replayed canonical event fixtures** (so the Core
  Domain / pipeline can be tested without a live Manager), plus real
  gateway-integration tests on x64 Windows (§13).

### 5.2 Non-Goals (explicitly deferred)

- **Automatic Evidence creation.** Ingested Trading Data is the raw
  upstream source; turning it into risk `Evidence` (Q-011) is a separate,
  future, separately-approved capability (rule engine / human). Q-015
  delivers the stream and its storage, not Evidence records, and does not
  modify Q-011.
- Any **Account Control / execution** (writing back to MT4/MT5 — e.g.
  restrict trading, change leverage). That is a separate future
  capability; Q-015 is **read/ingest only**.
- A Rule Engine, risk detection logic, thresholds, scoring, or alerting.
- Inventing MT4/MT5 Manager API interfaces before the real SDK is
  available (`AGENTS.md`).
- Non-MT4/MT5 sources (other brokers, CRM, external feeds) — future
  adapters behind the same canonical contract.
- Historical bulk backfill / data-warehouse analytics, Flink/streaming
  analytics, ML — future phases.

### 5.3 Business-scope decisions — confirmed by the Product Owner, 2026-09-02

**(1) Event types in Foundation scope — CONFIRMED, with a markout tick
refinement.** The Foundation ingests: **deals/trades, positions, and
account state — balance, equity, and credit status.** In addition, and
refining the original "defer all ticks" recommendation, the Foundation
**captures ticks only in a bounded window of ±30 seconds around each order
open and close, for computing markout** (post-trade price movement, an
execution-quality/risk metric). This is a small, event-correlated tick
capture — not the full high-frequency tick firehose. Architectural
implication (flagged for the Architecture stage, §14): capturing the "30
seconds before" an order event requires the ingestion side to maintain a
short rolling per-instrument tick buffer, then, on an order open/close,
emit that instrument's ticks over `[event − 30s, event + 30s]`. The full
raw tick stream outside these windows remains out of scope.

**(2) Retention — CONFIRMED as FULL retention, with partitioned storage.**
All ingested Trading Data is retained (no expiry). It must **not** live in
a single table: the storage is partitioned/split (the Architecture decides
the partition key — e.g. by time and/or account and/or event type) so that
historical order queries remain performant at scale. So the driver is not
a configurable expiry policy but a **partitioned, query-optimized durable
store for the complete history**, explicitly designed for historical order
lookup. (This overrides the original "configurable retention duration"
recommendation toward "keep everything, designed for scale.")

**(3) Data-integrity posture — CONFIRMED "no silent loss."** On gateway
disconnect/restart, the pipeline must detect the gap and resync (snapshot)
before resuming, and must fail visibly rather than skip events; a brief
resync delay is acceptable, silently missing a deal/position/account event
is not.

## 6. Domain Definitions

- **Trading Data** — broker-neutral, real-time observations of trading
  activity (deals, positions, account/balance state) ingested from a
  broker platform. The raw upstream source for risk reasoning; **not**
  Evidence, a Decision, an Action, or a Risk Case.
- **Canonical Trading Data Event** — one normalized, platform-neutral
  event in the canonical model, carrying a `platform` provenance tag
  (`MT4`/`MT5`) and a broker/server identity, but **no** MT4/MT5-specific
  structure. Its field-level shape is designed at the Architecture stage
  against the real SDK.
- **Ingestion Gateway** — a platform-specific process that hosts the
  native Manager SDK, maintains the pumping subscription, and emits
  canonical events. Authenticates as a Q-009 `SERVICE` actor.
- **Trading Account subject** — ingested events reference a broker-neutral
  trading-account identity; where an ingested account corresponds to a
  Q-010 `TradingAccountRef`, that binding is defined at the Architecture
  stage (Q-015 does not modify Q-010).

## 7. Functional Requirements

- **Q015-FR-001:** The system shall ingest real-time trading activity from
  MT4 and MT5 Manager servers via the pumping (push/subscription) model.
  The Foundation event scope is: deals/trades, positions, and account
  state (balance, equity, credit status); plus, for each order open and
  close, that instrument's ticks over `[event − 30s, event + 30s]` for
  markout (a bounded, event-correlated tick window — not the full tick
  firehose). Ingesting the full raw tick stream outside those windows is
  out of scope.
- **Q015-FR-002:** The system shall normalize MT4 and MT5 native events
  into one canonical, platform-neutral Trading Data event model before any
  event is delivered onto the platform stream; no MT4/MT5-specific
  structure shall reach a consumer.
- **Q015-FR-003:** Each platform's ingestion shall run in an isolated
  gateway whose failure or restart does not stop the other platform's
  ingestion.
- **Q015-FR-004:** Canonical Trading Data events shall be delivered onto
  Kafka with: reconnect after disconnect; gap detection and
  snapshot/resync so a disconnect cannot silently drop events; idempotent/
  de-duplicated delivery; a defined ordering guarantee; and backpressure
  handling.
- **Q015-FR-005:** Each gateway shall authenticate as a trusted Q-009
  `SERVICE` actor and pass Q-009 authorization; no identity shall be
  accepted from an untrusted source.
- **Q015-FR-006:** Ingested canonical Trading Data shall be retained in
  full (no expiry) in a partitioned, query-optimized durable store — not a
  single table — designed for historical order lookup at scale and
  sufficient for gap recovery, replay, and audit (§5.3(2)). Markout tick
  windows are retained as part of this store, associated with their order
  events.
- **Q015-FR-007:** Trading Data shall not be automatically converted into
  Evidence or any Core-Domain record; Q-015 delivers the stream only.
- **Q015-FR-008:** Q-015 shall be read/ingest only — it shall never write
  to, command, or execute against any MT4/MT5/broker system.
- **Q015-FR-009:** No MT4/MT5 Manager SDK type, DTO, or vendor primary key
  shall enter the Core Domain or the canonical event contract's public
  shape; vendor detail stays inside the gateway.

## 8. Security Requirements

- Gateways authenticate as Q-009 `SERVICE` actors under a dedicated
  ingestion capability; default-deny.
- Manager server credentials are secrets managed outside code/logs (no
  credential in code, config-in-repo, or logs — Principles §4); the
  Architecture defines secret handling.
- No trading-account holder PII, credential, or raw vendor payload beyond
  the neutral canonical fields is logged or placed on the stream beyond
  what risk requires; sensitive content is bounded and access-controlled.
- The gateway boundary is the only component that touches the vendor SDK;
  a compromise there cannot reach Core-Domain mutation authority
  (ingestion is read-only and one-directional into Kafka).

## 9. Data Integrity and Provenance Requirements

- Every canonical event carries its `platform`, broker/server identity,
  and a source sequence/identity sufficient to detect gaps and de-duplicate
  (§5.3(3) "no silent loss").
- On reconnect, the pipeline reconciles against a snapshot so post-
  disconnect state is consistent, not silently divergent (Principles §3:
  "final state correct AND explainable").
- Ordering per trading account (or per the canonical model's defined
  ordering key) is preserved or made explicitly reconstructable.
- Retention and replay allow reconstructing what was observed and when.

## 10. Acceptance Criteria

1. Ingestion uses the pumping model for both MT4 and MT5, covering
   deals/trades, positions, account state (balance, equity, credit), and
   the ±30s markout tick window around each order open/close.
2. Both platforms normalize into one canonical, platform-neutral model; no
   consumer sees an MT4/MT5-specific structure.
3. Gateways are isolated (one platform's failure does not stop the other).
4. Canonical events reach Kafka with reconnect, gap-detection/resync,
   idempotency, defined ordering, and backpressure handling.
5. Gateways authenticate as Q-009 `SERVICE` actors; default-deny enforced.
6. Ingested Trading Data is retained in full in a partitioned,
   query-optimized store designed for historical order lookup (not a
   single table).
7. Trading Data is not auto-converted into Evidence; no Q-011 file is
   modified.
8. Ingestion is read-only; no write/command/execution to any broker system
   exists anywhere in the change.
9. No MT4/MT5 SDK/vendor type enters the Core Domain or the canonical
   contract's public shape.
10. No existing Q-008…Q-014 file is modified.
11. Verification includes replayed canonical-event tests (platform-neutral,
    runnable without a live Manager) and real gateway-integration tests on
    x64 Windows; no mandatory test is skipped.

## 11. Technical Constraints

- Java 21 / Spring Boot for the platform-side consumer/pipeline;
  `com.brokeros.risk.tradingdata` package (or as Architecture decides).
- The gateway hosting the native MT4/MT5 SDK runs on **x64 Windows**
  (the SDK is Windows-native; not loadable in a JVM on macOS/Linux). The
  gateway communicates with the platform only through a defined boundary
  (Architecture decides the transport); the platform-side services run on
  the existing Linux/Docker deployment unchanged.
- First real use of Kafka; introduce no other new infrastructure without
  justification (Principles §11).
- Reuse Q-009 unchanged; no vendor SDK dependency in the Core-Domain
  modules.
- Apply the established test-discipline lessons (dynamic migration counts
  if any schema is added; exact-name ownership assertions).

## 12. Dependencies

- Q-009 (`ActorContext`, `Capability`, `AuthorizationGuard`, `SERVICE`
  actor) — reused unchanged.
- Kafka (already in the Phase 1 stack).
- **External (Product-Owner-supplied) for Architecture/Implementation:**
  real MT4 and MT5 Manager API SDKs (headers/libraries + sample event
  data) and an x64 Windows test environment (cloud or dedicated host).
- Does not depend on and does not modify Q-008/Q-010/Q-011/Q-012/Q-013/
  Q-014.

## 13. Verification Plan

- **Canonical-event replay tests (macOS/Linux, no live Manager):** record
  representative MT4 and MT5 canonical events and replay them through the
  pipeline and consumers; prove neutral handling of both origins, gap
  recovery, de-duplication, ordering, and backpressure. This is the bulk
  of testing and needs no Windows.
- **Gateway integration tests (x64 Windows):** the MT4 and MT5 gateways
  against the real SDK + a demo Manager server; validate pumping
  subscription, reconnect, and correct native→canonical translation.
- The canonical event **schema contract** is the key seam; test it
  explicitly so the platform side can be validated independently of the
  gateways.
- No credential or PII in test artifacts; disposable/demo environments
  only.

## 14. Risks and Architecture Inputs

- **The canonical broker-neutral model is the central Type-1 (hard-to-
  reverse) decision** — it must faithfully represent both MT4's
  order-as-position model and MT5's deal/position/order model without
  leaking either. It requires its own ADR and cannot be field-designed
  until the real SDK data structures are available. This is the single
  hardest design problem in the capability.
- Reliability of a long-lived stateful pump connection (reconnect, gap
  recovery, backpressure, ordering) is where most risk lives (Principles
  §2/§3); the Architecture must treat it as primary, not an afterthought.
- x64 Windows hosting for the gateways is an operational dependency (the
  team develops on Apple Silicon; the authoritative gateway environment is
  a remote/cloud x64 Windows host).
- **Markout tick windowing (§5.3(1))** adds a stateful sub-capability: to
  capture the 30 seconds *before* an order event, the ingestion side must
  maintain a short rolling per-instrument tick buffer and, on an order
  open/close, emit `[event − 30s, event + 30s]`. The Architecture must
  decide where this buffering lives (gateway vs platform side) and how the
  window is correlated to the order event and to the canonical model. This
  is more involved than plain event ingestion.
- **Full-retention partitioned storage (§5.3(2))** is a data-architecture
  input: the Architecture must choose a partition strategy (time / account
  / event type) that keeps historical order queries performant as the
  complete history grows without bound. Partition/query design is a Type-1
  decision worth care.
- The "no silent loss" posture (§5.3(3)) makes reconnect/gap-recovery a
  primary design concern, not an afterthought.

## 15. Deliverables

- Approved `docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md`.
- Architecture, a canonical-model ADR, and Implementation Design — the
  Architecture and canonical-model ADR require the real SDK first.
- The `tradingdata` platform module + gateway(s) + Kafka pipeline.
- Canonical event schema contract + replay fixtures.
- Lessons Learned; non-overwriting review packages under `review/q-015/`.

## 16. Review Checklist

- [x] Requirement self-reviewed by Claude Code (external Architect role).
- [ ] Product Owner Gate Decision recorded (§17).
- [x] §5.3's three business-scope questions answered by the Product Owner
      (2026-09-02): event scope = deals/positions/account-balance-equity-
      credit + ±30s markout tick window; full retention with partitioned
      storage; no-silent-loss posture.
- [ ] SDK + x64 Windows prerequisite acknowledged as gating Architecture/
      Implementation (not this Requirement).
- [ ] No Q-008…Q-014 file referenced for modification; ingestion is
      read-only; no vendor type in the Core Domain.

## 17. Current Gate

Q-015 Requirement status: **APPROVED — V1 — 2026-09-02 — Product Owner.**
Gate Decision: **PASS**, with §5.3's three business-scope answers confirmed
(event scope = deals/positions/account balance-equity-credit + ±30s markout
tick window; full retention with partitioned query-optimized storage;
no-silent-loss data-integrity posture).

Q-015 Implementation Allowed: **NO — Requirement approval is not
Architecture, ADR, Design, or Implementation authorization.**

Next gate: **Architecture Analysis — BLOCKED on an external prerequisite.**
Per `AGENTS.md`, the canonical Trading Data model and the MT4/MT5 gateway
interfaces cannot be field-designed before the real MT4/MT5 Manager SDK
data structures are available; the gateway integration environment also
requires x64 Windows. The Architecture stage therefore begins only once the
Product Owner supplies (a) the real MT4 and MT5 Manager API SDKs
(headers/libraries + representative sample event data) and (b) an x64
Windows test environment. Until then, Q-015 is **approved and parked at the
Architecture gate awaiting the SDK.** Per Decision Authority §16.5-B, once
the SDK is available, Claude Code may draft Architecture → canonical-model
ADR → Implementation Design as a connected chain.

**Phase split (2026-09-06, Product Owner).** To make SDK-independent progress
now, Q-015 delivery is split into **Phase A (SDK-independent)** and **Phase B
(SDK-gated)** — see `docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md`.
This §17 park remains authoritative for **Phase B** (the canonical field model,
MT4/MT5 gateway interfaces, native adapters, and Windows integration stay blocked
on the SDK + x64 Windows). The **addendum's** gate section is authoritative for
**Phase A** (the reliability pipeline, `SERVICE`-actor auth boundary, and
partitioned storage skeleton, built against a provisional payload-opaque envelope
contract). Nothing in Phase A invents a Manager API interface or commits the
canonical field model (AGENTS.md).
