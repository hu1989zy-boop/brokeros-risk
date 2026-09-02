# Q-015 Requirement V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12 and `docs/engineering/Architecture-and-Design-Decision-Principles.md`.

## Task ID / Stage

Q-015 — Trading Data Ingestion Foundation (MT4/MT5 pumping). **Requirement**
stage; self-review by Claude Code (external Architect role). Per protocol
§3, work stops here pending the Product Owner's Gate Decision.

## Scope Reviewed

`docs/requirements/Q-015-Trading-Data-Ingestion-Foundation.md` V1, checked
for internal consistency, alignment with ADR-009/Q-007's "Trading Data"
upstream context, and compliance with `AGENTS.md`'s hard rule against
inventing Manager API interfaces without the real SDK.

## Files Inspected

- `docs/requirements/Q-007-Requirement.md` (the "Trading Data" definition
  this Requirement realizes; confirmed Q-007 named it and deferred its
  implementation, so Q-015 implementing it is consistent, not a conflict).
- The earlier conversation's architecture direction (two isolated MT4/MT5
  pump gateways → canonical neutral model → Kafka → Core Domain; x64
  Windows gateway; Apple-Silicon dev), captured faithfully into §4/§11/§14.
- `AGENTS.md` (the no-invented-SDK rule and the Kafka-in-stack fact).

## Verification Executed

Not applicable — no code exists yet. `GitStatus.txt`/`GitDiffStat.txt`
confirm the only change is the one new Requirement file.

## Findings

This Requirement is materially different in kind from Q-009…Q-014 (which
were `MANUAL`, human-recorded provenance stores), and the draft reflects
that honestly rather than copying the provenance template:

1. **First automated / `SERVICE`-sourced capability.** Ingestion is
   automated from the broker via a gateway authenticating as a Q-009
   `SERVICE` actor — not `HUMAN`/`MANUAL`. Captured in Goal 5 / FR-005 /
   §8.
2. **Trading Data ≠ Evidence** is stated as a hard boundary (Goal 6 /
   §5.2 / FR-007), grounded in ADR-009/Q-007 (Trading Data is the raw
   upstream *source*; Evidence is formed from it later). This prevents the
   most likely scope creep (auto-creating Evidence).
3. **Read-only ingestion** (FR-008) keeps execution/Account Control out,
   preserving ADR-009's Action/Execution separation and deferring the
   high-risk write path.
4. **Neutrality is the central Type-1 decision** (§14): one canonical model
   must represent both MT4 (order-as-position) and MT5 (deal/position/
   order) without leaking either. Flagged as needing its own ADR and as
   un-field-designable before the real SDK — honoring `AGENTS.md`.
5. **The SDK + x64 Windows prerequisite** is scoped precisely: it gates the
   Architecture/Implementation stages, NOT this Requirement. The
   Requirement is fully completable now; the canonical-model ADR waits for
   the SDK. This is stated in Status, §1, §11, §12, §14, and §17 so it is
   unmissable.

Three genuine business-scope questions are surfaced in §5.3 with firm
recommendations (event-type subset; retention duration; no-silent-loss
data-integrity posture) rather than silently assumed — each is a
risk/business/product decision (Decision Authority §16.2), not a technical
one.

No inconsistency found. All nine `Q015-FR-XXX` are internally consistent
with the goals and acceptance criteria; the §16.1 single-live-status rule
is applied (Status header defers to §17).

## Remaining Risks

- If any §5.3 answer changes (especially event-type scope), §7/§10 and the
  future canonical model shift materially — must be resolved before
  Architecture.
- The whole capability's Architecture is externally gated on the SDK + x64
  Windows env; the Requirement can be approved independently of that.

## Out-of-Scope Issues

None beyond the Non-Goals in §5.2 (no auto-Evidence, no execution, no rule
engine, no invented SDK, no non-MT sources yet).

## Recommendation

Present to the Product Owner for a Gate Decision, with explicit attention
to §5.3's three questions. Note that Requirement approval does not unblock
Architecture — that additionally requires the SDK + x64 Windows env.

## Gate Decision

**PASS** (self-review only — the Product Owner's Gate Decision remains
outstanding).
