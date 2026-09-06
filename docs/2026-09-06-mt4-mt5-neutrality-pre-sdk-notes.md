# MT4/MT5 Neutrality — Pre-SDK Research Notes

Date: 2026-09-06
Requirement: Q-015 Phase A, acceptance item A5
Status: pre-SDK research input for a future Phase B Architecture; not a design decision

## Boundary and evidence quality

This note uses only public MetaQuotes documentation. The sources describe the
public MQL4/MQL5 trading abstractions; they do **not** establish the shape,
availability, callback behavior, or guarantees of either Manager API SDK.
Consequently, this note neither defines a canonical field schema nor claims that
any Manager API operation exists. Every candidate below remains subject to direct
validation against the real SDKs and an x64 Windows integration environment.

## Publicly documented semantic difference

The public MQL4 surface is order-centric. Its trade-function index places active
market and pending activity in the order collection, closed activity in order
history, and describes closing an open trade as closing an order. This supports
the cautious shorthand “order-as-open-trade/position” for comparative analysis,
but it is not a claim about Manager API types or callbacks.
([MQL4 trade functions](https://docs.mql4.com/trading),
[MQL4 OrdersTotal](https://docs.mql4.com/trading/orderstotal),
[MQL4 OrderClose](https://docs.mql4.com/trading/orderclose))

The public MQL5 model distinguishes three concepts: an order is an instruction,
a deal is an execution, and a position is the resulting trade obligation. One
order can produce multiple deals, while a position may result from one or more
deals. MQL5 also documents both netting and hedging position accounting; netting
allows one position per symbol, while hedging allows multiple positions for the
same symbol. ([MQL5 trade functions](https://www.mql5.com/en/docs/trading),
[MQL5 order properties](https://www.mql5.com/en/docs/constants/tradingconstants/orderproperties),
[MQL5 deal properties](https://www.mql5.com/en/docs/constants/tradingconstants/dealproperties),
[MQL5 positions](https://www.mql5.com/en/docs/trading/positionstotal))

The neutrality consequence is conceptual, not field-level: a future model must
not assume that an order, execution, and position have a one-to-one relationship,
and it must not assume one universal position-accounting mode.

## Candidate neutrality options for Phase B evaluation

These are alternatives to evaluate, not an accepted canonical model:

1. **Separate neutral lifecycle facts.** Represent intent/order lifecycle,
   execution facts, exposure/position state, and account-state observations as
   distinct neutral concepts linked by provenance where the verified SDK permits.
   This most directly preserves MQL5 distinctions, but the SDK study must show how
   MT4 observations map without manufacturing facts.
2. **Immutable observation facts plus derived projections.** Preserve each verified
   source observation as a neutral fact and calculate order/execution/exposure views
   downstream. This reduces destructive normalization and supports replay, at the
   cost of more projection logic and explicit correction handling.
3. **Versioned union of neutral event families.** Use one versioned contract whose
   event families cover lifecycle, execution, exposure, and account state while
   allowing a family to be absent when a platform cannot prove it. This can keep one
   consumer seam, but risks a sparse or overly broad contract if designed before SDK
   evidence is available.
4. **Continue opaque transport until evidence is sufficient.** Retain the Phase A
   envelope and postpone semantic publication until both SDKs are characterized.
   This is the lowest-integrity-risk fallback if the SDKs do not expose equivalent
   semantics, though it delays neutral domain consumption.

No option is selected here. The governing priority is to preserve source truth and
avoid fabricating equivalence.

## Questions that must be answered against the real SDKs

- Which callbacks or polling/snapshot facilities actually exist, and which source
  transitions can be missed during disconnect or process restart?
- Which identifiers are stable, unique, and reusable across server restarts,
  archival boundaries, partial execution, correction, cancellation, and reversal?
- Is a monotonic source sequence supplied, and what is its scope and reset behavior?
- Can the SDK reconstruct a complete snapshot and a bounded replay after a gap?
- How are active trades, pending instructions, executions, and closed history
  represented in each SDK, without relying on MQL-language analogies?
- How are MQL5 netting and hedging modes exposed, and can the mode change for an
  account or server?
- How are partial fills, close-by activity, reversals, deleted/cancelled activity,
  and post-execution corrections represented and ordered?
- What timestamp sources, precision, timezone, and ordering guarantees are provided?
- Which account-state observations for balance, equity, and credit are delivered,
  and are they snapshots or deltas?
- What payload ownership, memory lifetime, threading, reentrancy, and callback
  backpressure rules does each native SDK impose?
- What server and SDK versions must be supported, and where do semantics differ?
- Which data is required for the separately approved markout window, and how can it
  be captured without asserting a tick interface before SDK verification?

## Phase B gate

Phase B Architecture must not begin from this note alone. It requires the real MT4
and MT5 Manager API SDKs, vendor documentation/license access, and an x64 Windows
test environment. Any selected canonical model or gateway port then requires its
own architecture decision and verification evidence.
