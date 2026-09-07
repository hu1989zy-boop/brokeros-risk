# Q-015 Phase B — MT4 SDK Intake Checklist

Date: 2026-09-07
Purpose: a **procurement + environment + evidence-capture** checklist to unblock the
**MT4** half of Q-015 Phase B (the MT4 gateway + the MT4 side of the canonical model).
This is a checklist of **what to obtain and what to record from the official package**
— it invents **no** Manager API interface and asserts **no** MT4 field or operation
(AGENTS.md L76/L173). Everything technical is authoritative **only** from your official
MetaQuotes/MT4 package and its documentation; where this list names something, treat it
as a prompt to confirm against your package, not as fact.

Context: licensed broker, both MT4 + MT5 wanted, cloud x64 Windows host. **MT4 first.**
Parent: `docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md` §12 (external
prerequisites); Phase A is complete (`docs/requirements/Q-015-Phase-A-SDK-Independent-Addendum.md`).

---

## 0. Decide first — the MT4 Manager API bitness (blocks the gateway architecture)

- [ ] Confirm the **bitness** of the MT4 Manager API in your actual package
      (32-bit and/or 64-bit native library).
- [ ] Record the consequence:
  - If **32-bit only** → the MT4 gateway process must run **32-bit** on the x64
    Windows host (a 32-bit JVM, or a 32-bit native sidecar that bridges to the
    platform side). A 64-bit JVM cannot load a 32-bit DLL.
  - If a **64-bit** library exists → a 64-bit gateway process is possible.
- [ ] Note this decision — Phase B's MT4-gateway Architecture depends on it.

## 1. Obtain from MetaQuotes / your MT4 server distribution

- [ ] The **MT4 Manager API** package for your server version — the native
      **library file(s) + header(s)** (and any import libs).
- [ ] The **official MT4 Manager API documentation** (function/callback reference,
      pumping/subscription model, data-structure definitions).
- [ ] Any official **sample/demo code** shipped with the package.
- [ ] Any official **language wrapper** provided (if any) and its own docs.
- [ ] A **demo / test MT4 server** you may connect to, with a **Manager login**
      (Manager-level access — the real-time pumping feed is Manager-side).
- [ ] Archive the exact **version numbers** (server build + Manager API build) — MT4
      API behavior varies by build.

> MT4 is a legacy product (MetaQuotes stopped selling new MT4 licenses years ago).
> As a licensed broker you obtain the Manager API from your existing MT4 server area;
> confirm your build is still supported by your license.

## 2. Stand up the cloud x64 Windows host

- [ ] Provision an **x64 (amd64) Windows Server** VM (AWS EC2 e.g. `t3.large`
      Windows Server 2022, or Azure `Standard_D2s_v5`) — **not** ARM.
- [ ] Lock **RDP (3389)** to your IP only; snapshot after base setup.
- [ ] Install **JDK 21** (match the project). If MT4 API is 32-bit (step 0), also
      install a **32-bit JDK 21** / 32-bit toolchain for the MT4 gateway process.
- [ ] Install the **native runtime the MT4 package needs** (e.g. the matching MSVC
      redistributable) — **per the package's documented requirement**, matching the
      library bitness.
- [ ] Install Git; clone the repo (Phase A code is the platform side the gateway will
      feed).

> You create the cloud account and enter credentials yourself (assistant safety rule).
> Ask me for a concrete AWS or Azure launch-parameter + install list if useful.

## 3. Connectivity smoke test (official sample only — do NOT wire our code yet)

- [ ] Build/run the **official MT4 sample** on the Windows host.
- [ ] Log in with the demo server's **Manager** credentials.
- [ ] Confirm you receive real-time **pumping callbacks** (trades/orders, account
      state, quotes) — proving *SDK + host + server account* are connected.
- [ ] Record any connection/reconnect quirks you observe.

## 4. Capture the real MT4 structures + samples (this is what unblocks the design)

Record the following **from the official docs + what the sample actually delivers**
(de-identified — remove any account-holder PII/credentials). These feed the Phase B
canonical-model ADR; the A5 note
(`docs/2026-09-06-mt4-mt5-neutrality-pre-sdk-notes.md`) lists the open questions.

- [ ] **Event/record types** available via pumping and their **real field
      structures**: name, type, unit, and meaning for
      trades/orders, positions-as-orders, account/balance/equity/credit, and quotes.
- [ ] **Order-centric semantics** confirmed on the real API: how an open position is
      represented as an order; how a close is represented; partial closes; pending vs
      market.
- [ ] **Identity & lifecycle**: the account/login identity; the order ticket identity
      and how it changes across open/modify/close.
- [ ] **Ordering / sequence source**: is there a monotonic sequence or counter per
      connection, or must ordering be derived from timestamps? (This validates the
      Phase A envelope's `sourceSequence` contract for MT4.)
- [ ] **Timestamps & timezone**: field granularity, and the **server time zone** MT4
      uses.
- [ ] **Numeric precision**: price/volume digits, lot/contract-size conventions.
- [ ] **Snapshot & reconnect**: what a fresh Manager subscription delivers as an
      initial snapshot, and the behavior after a disconnect/reconnect (does it
      re-snapshot? backfill? gap?). This drives the "no silent loss" resync design.
- [ ] **Markout inputs**: whether/how instrument ticks are available around an
      order open/close (for the ±30s markout window, parent §5.3(1)).
- [ ] **De-identified sample fixtures**: capture a representative batch of each event
      type as recorded by the sample, scrubbed of PII/credentials, saved for Phase B
      replay fixtures.

## 5. Hand back to start MT4 Phase B

- [ ] Bring me: the recorded structures/behaviors (step 4) + the de-identified samples
      + the doc references + the bitness decision (step 0).
- [ ] Then, under governance, I draft **Phase B (MT4)**: Architecture → canonical-model
      ADR (the MT4 side of the neutral model) → Implementation Design → the MT4 gateway
      adapter (on this x64 Windows host) + integration tests, connecting to the Phase A
      reliability pipeline / store / `SERVICE`-actor boundary already built.

---

## Guardrail

Nothing in Phase B may hard-code MT4 vendor detail outside the gateway adapter, invent
an interface the package does not document, or claim an operation not verified against
the real SDK (AGENTS.md L75/L76/L173). The payload-opaque Phase A envelope stays the
platform-side contract; the MT4 gateway maps the **real** MT4 structures into it in
Phase B.
