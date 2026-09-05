# Q-015 Phase A — SDK-Independent Ingestion Foundation (Addendum)

## Status

V1 draft addendum, by Claude Code holding the external Architect role, per the two
`docs/engineering/` governance documents. **The authoritative live status for
Phase A is §17 (Current Gate) of THIS addendum**; the parent
`Q-015-Trading-Data-Ingestion-Foundation.md` §17 remains authoritative for **Phase
B** (SDK-gated). The §5.3 scope decisions below are surfaced for explicit Product
Owner confirmation.

- Addendum to: `Q-015-Trading-Data-Ingestion-Foundation.md` (V1, APPROVED
  2026-09-02; parked at the Architecture gate awaiting the MT4/MT5 SDK + x64
  Windows).
- Purpose: split Q-015 delivery so the **SDK-independent** half can proceed now,
  under governance, without violating AGENTS.md (no invented Manager API interface;
  no committed canonical field model).
- Product Owner direction (2026-09-06): "Phase A 走路线 1 — draft a phased
  Requirement addendum."

## 1. Why a split is possible without the SDK

The parent Requirement is parked because the **canonical Trading Data field model**
(§14: "the single hardest, Type-1 decision") and the **MT4/MT5 gateway interfaces**
cannot be designed before the real SDK data structures are in hand (AGENTS.md L76:
*never invent Manager API interfaces without the real SDK*; L98–99: *do not invent
missing SDK contents*). AGENTS.md L75 also requires MT4/MT5 integration to live
**only in adapters**. That adapter seam is exactly what makes a split possible: the
SDK touches **only the outermost gateway adapter (Phase B)**. Everything inside that
seam — the reliability pipeline, the trusted-actor authorization boundary, and the
durable partitioned store — depends on **envelope metadata**, not on the payload's
field shape, and can be built and fully tested on Linux/macOS with **synthetic**
inputs.

Phase A therefore builds against a **provisional, payload-opaque envelope
contract** (§5). It does not describe or assume any MT4/MT5 native structure: the
payload is carried as an opaque blob, and the envelope's metadata is a **contract
the future gateway must satisfy**, not a guess at what the SDK provides.

## 2. Phase A vs Phase B

| Concern | Phase A (now, SDK-independent) | Phase B (blocked on SDK + x64 Windows) |
| --- | --- | --- |
| Canonical event **field model** + its ADR | **No** — payload stays opaque | **Yes** — the Type-1 decision, against the real SDK |
| MT4/MT5 **gateway interfaces** + native adapters | **No** | **Yes** — hosts the SDK on x64 Windows |
| native → canonical **translation** | **No** | **Yes** |
| Markout **tick-window** field-level capture | **No** (parent §5.3(1)) | **Yes** — stateful, model-dependent |
| Platform-side **reliability pipeline** (idempotency, gap-detect/resync, ordering, backpressure) | **Yes** — against the provisional envelope | Re-validated with real canonical events |
| Ingestion **`SERVICE`-actor** capability + authorization boundary (reuse Q-009) | **Yes** | reused |
| **Durable partitioned storage** skeleton (full retention) | **Yes** — metadata-indexed, opaque payload | payload schema + any model-driven query columns added |
| **Replay-fixture** test harness | **Yes** — synthetic provisional envelopes | upgraded to real canonical fixtures |
| gateway **integration tests** (x64 Windows) | **No** | **Yes** |

## 3. Phase A — In Scope

- **A1 — Ingestion authorization boundary.** A dedicated ingestion **capability**
  (e.g. `trading-data:ingest`) and a Q-009 **`SERVICE`** actor; the platform-side
  ingestion entry authorizes as that actor under default-deny. Reuse Q-009
  unchanged; add only the new capability + a `SERVICE`-actor bootstrap.
- **A2 — Platform-side reliability pipeline.** A consumer/pipeline that receives
  provisional envelopes, delivers them onto **Kafka**, and enforces the parent's
  Goal-4 reliability properties **using envelope metadata only**: idempotent /
  de-duplicated delivery (by `(sourceServerId, sourceSequence)`), gap detection
  (sequence monotonicity) with a visible fail/resync signal (no silent loss,
  parent §5.3(3)), a defined **ordering** guarantee (by the ordering key, §5.3), and
  backpressure handling. Kafka's first real use in the stack.
- **A3 — Durable partitioned storage skeleton.** A **partitioned, full-retention**
  store (parent §5.3(2)) holding each envelope's **queryable metadata**
  (`platform`, `sourceServerId`, `tradingAccountId`, `occurredAt`, `sourceSequence`,
  `envelopeVersion`) plus the **opaque payload** blob, partitioned by a
  field-independent strategy (§5.3(3) below), optimized for historical lookup by
  account + time. No payload fields are indexed in Phase A.
- **A4 — Replay-fixture harness.** The parent §13 "canonical-event replay tests (no
  live Manager)" harness, built now against **synthetic provisional envelopes** —
  proving neutral handling, gap recovery, de-duplication, ordering, and backpressure
  without any SDK or Windows. Upgraded to real canonical fixtures in Phase B.
- **A5 — Pre-SDK domain research note (optional, §5.3(5)).** A `docs/` note
  comparing MT4 (order-as-position) and MT5 (deal/position/order) semantics **from
  public MetaTrader documentation only**, listing candidate canonical-model
  neutrality options and the open questions to resolve **against the SDK** —
  explicitly research to accelerate the Phase B Architecture stage, **not** an ADR,
  **not** a field-level design, and inventing **no** SDK interface.

## 4. Phase A — Non-Goals (stay in Phase B or parent Non-Goals)

- The canonical event **field model** and its ADR; any MT4/MT5 **gateway** interface
  or native adapter; native→canonical translation; markout tick-window field
  capture; gateway integration on x64 Windows.
- Everything already deferred by the parent §5.2 (automatic Evidence creation;
  account control/execution; rule engine; non-MT4/MT5 sources; backfill/analytics).
- No modification to any Q-008…Q-014 module; ingestion stays read-only.

## 5. The provisional envelope contract (the crux)

Phase A commits to a **versioned, payload-opaque** envelope. Its **metadata** is
generic streaming-ingestion structure (no MT4/MT5 field is described):

- `envelopeVersion` — schema version of the envelope itself (so Phase B can evolve
  it and migrate).
- `platform` — provenance tag (`MT4` / `MT5`) — a label, not a structure.
- `sourceServerId` — the broker/Manager server identity the event came from.
- `sourceSequence` — a monotonic per-connection sequence the **gateway is required
  to supply** (the contract), used for gap detection + de-duplication.
- `tradingAccountId` — a broker-neutral account identity used as the **ordering
  key** (§5.3(1)); binding to a Q-010 `TradingAccountRef` is a Phase B concern.
- `occurredAt` — source event time.
- `payload` — an **opaque blob**; Phase A never reads, indexes, or describes its
  contents. The canonical field model that gives it shape is Phase B.

This is a **contract the future gateway must satisfy**, not an assertion about the
SDK. If Phase B finds the SDK cannot supply a required element (e.g. a monotonic
sequence), the contract is revised then — the rework is bounded to this thin layer.

### 5.3 Scope decisions — CONFIRMED by the Product Owner (2026-09-06): all recommended

1. **Ordering key — CONFIRMED: per `tradingAccountId`.** Order is guaranteed per
   trading account (the risk-relevant unit). *Provisional:* Phase B may refine to
   per-`(account, instrument)` if the model requires; mitigated by keeping the key
   an envelope field.
2. **De-dup / gap key — CONFIRMED: `(sourceServerId, sourceSequence)` with a
   required monotonic sequence.** Gap = a break in monotonicity → a visible resync
   signal (no silent loss). *Provisional:* depends on the gateway honoring the
   sequence contract (confirmed in Phase B).
3. **Storage partition strategy (skeleton) — CONFIRMED: by time + `platform`**
   (field-independent), with `tradingAccountId` + `occurredAt` indexed for
   historical account lookup. *Provisional:* Phase B may add model-driven partitions
   (e.g. by event type) once the field model exists.
4. **Transport / auth boundary — CONFIRMED: gateway → a Q-009-authorized
   ingestion entry → Kafka.** Keeps the mandated `SERVICE`-actor authorization
   (parent FR-005) in the path (rather than the gateway publishing to Kafka directly
   under Kafka-native auth only).
5. **Pre-SDK research note (A5) — CONFIRMED: included** in Phase A (public-doc
   research; accelerates Phase B; invents no SDK interface).
6. **Envelope + storage versioning — CONFIRMED: yes, explicitly versioned**
   (`envelopeVersion`) so Phase B evolves the shape and migrates without breaking
   Phase A data.

## 6. Rework-risk statement (honest)

Phase A is **not zero-rework**: the envelope's ordering / de-dup / partition **keys**
are informed by the eventual canonical model, so Phase B may adjust them. The risk is
**bounded and deliberate**: the payload is opaque (no invented fields), the envelope
is versioned, and the reliability/auth/storage logic is written against envelope
metadata — so any revision is a thin, migratable layer, not a pipeline rewrite. The
Product Owner accepts this moderate, bounded rework to make SDK-independent progress
now. The single highest-leverage unblock for the **whole** capability remains
supplying the SDK + an x64 Windows host (Phase B); Phase A does not substitute for it.

## 7. Boundaries (hard — AGENTS.md)

- **No invented Manager API interface; no committed canonical field model.** The
  payload stays opaque throughout Phase A.
- MT4/MT5 detail, if it ever appears, lives **only** in the Phase B gateway adapter.
- Reuse Q-009 unchanged; introduce no vendor SDK dependency; modify no Q-008…Q-014
  module; ingestion is read-only.
- Any place tempted to describe a native field must instead treat it as opaque and
  defer to Phase B.

## 8. Verification (Phase A — no SDK, no Windows)

- Reliability: replay synthetic provisional envelopes proving idempotency, gap
  detection + resync signal, ordering per key, and backpressure (bulk of testing;
  Linux/macOS).
- Authorization: the ingestion entry authorizes as the Q-009 `SERVICE` actor;
  default-deny for anyone else.
- Storage: real-MySQL (or the chosen store) test of the partitioned skeleton — full
  retention, metadata queryable, opaque payload round-trips, historical account/time
  lookup.
- Contract: the provisional envelope schema is the seam — tested explicitly and
  versioned.
- No credential/PII in fixtures; no payload field is asserted.

## 9. Deliverables

- This addendum; then (on Phase A approval + §5.3) the Phase A **§16.5-B bundle**
  (Architecture + ADR + Implementation Design, all SDK-independent) and a Codex
  prompt; then implementation and independent review. Phase B follows when the SDK +
  x64 Windows arrive, resuming from the parent §17.

## 10. Review Checklist

- [ ] §5.3 decisions confirmed (ordering key; de-dup/gap key; partition strategy;
      transport/auth boundary; research note; versioning).
- [ ] SDK-independence confirmed: payload opaque, no Manager API interface invented,
      no canonical field model committed, no Q-008…Q-014 change, Q-009 reused.
- [ ] Product Owner Gate Decision for Phase A recorded (§17).

## 17. Current Gate

Q-015 **Phase A** status: **APPROVED — V1 — 2026-09-06 — Product Owner.**
Gate Decision: **PASS**, with all six §5.3 decisions confirmed at the recommended
defaults (ordering per `tradingAccountId`; de-dup/gap by `(sourceServerId,
sourceSequence)` monotonic; storage partitioned by time + `platform`; transport via a
Q-009-authorized ingestion entry → Kafka; pre-SDK research note included; envelope +
storage explicitly versioned).

Phase A Architecture V1 / ADR-023 / Implementation Design V1: **ACCEPTED (bundle) —
2026-09-06 — Product Owner** at the implementation-authorization gate (§16.5-B).
ADR-023 is **Accepted**. **Phase A implementation AUTHORIZED — 2026-09-06 — Product
Owner.** The Codex implementation prompt (`prompts/Q-015-Phase-A-Implementation-Prompt.md`)
is **CLEARED FOR USE**. The bundle is SDK-independent: payload opaque, no invented
Manager API interface, no canonical field model, no `gateway`, no Q-008…Q-014 change,
Q-009 reused, read-only. Build shape: a new `com.brokeros.risk.tradingdata` module — a
Q-009 `SERVICE`-actor-authorized `POST /api/trading-data/ingest` (`trading-data:ingest`
capability, separate bootstrap), reliability by envelope metadata (idempotent
`(server,seq)`, visible gap markers, per-account Kafka ordering, backpressure), a
partitioned full-retention store (new migration V9) with opaque payload, and a
synthetic-envelope replay test harness; plus a pre-SDK public-doc research note (A5).

Confirmed Phase A scope: an ingestion `SERVICE`-actor capability + Q-009
authorization boundary; a platform-side reliability pipeline (idempotency, gap
detection/resync, ordering, backpressure) over a **versioned, payload-opaque**
envelope contract onto Kafka; a full-retention partitioned storage skeleton
(metadata-indexed, opaque payload); a synthetic-envelope replay harness; and
(optionally) a pre-SDK public-doc research note — **no invented Manager API
interface, no canonical field model, no Q-008…Q-014 change, Q-009 reused,
read-only**. On the Product Owner's explicit approval, the Phase A §16.5-B
Architecture / ADR / Implementation Design bundle follows.

**Phase B** (canonical field model + ADR, MT4/MT5 gateway interfaces + native
adapters, native→canonical translation, markout tick capture, x64 Windows
integration) **remains parked** on the parent Requirement's §17 external
prerequisite (the real MT4/MT5 Manager SDK + an x64 Windows environment).
