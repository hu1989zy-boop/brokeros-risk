# Q-011 V3 Architecture + ADR Analysis Summary

- Review ID: `Q-011-V3-ARCHITECTURE-ADR-ANALYSIS-20260828-170120`
- Prepared by: Claude Code, holding the external Architect role by explicit
  Product Owner direction (2026-08-28); self-authored, not independently
  reviewed. See `docs/requirements/Q-011-Evidence-Provenance-Foundation.md`
  §18 for the disclosed self-review limitation.
- Inputs: approved Q-011 Requirement V2, ADR-009/010/011/012, Q-010's
  shipped `TradingAccountReferenceEligibilityService`, Q-008's
  Implementation Design read-audit pattern (§9.5).
- Git baseline: `main` at `091d410`, clean, synced with `origin/main`.

## Deliverables

- `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
  (Architecture V1, proposed)
- `docs/adr/ADR-013-evidence-provenance-foundation.md` (proposed, not
  accepted)

## Key Architecture Decision

Requirement §14.1 left open how Evidence should validate its
`TradingAccountRef` subject against Q-010 without (a) reusing eligibility
semantics that don't fit an inactive/historical subject, or (b) forcing
Evidence-recording actors to hold Trading-Account-module capabilities.

Architecture resolves this by **reuse, not extension**: Q-011 calls Q-010's
existing `validateForNewRiskCaseAssociation` contract unchanged, using the
recording actor's own `ActorContext`. The capability requirement on
recording actors is accepted as normal least-privilege scoping (the same
pattern Q-008 will need). The eligibility-semantics mismatch is resolved by
narrowing Q-011 V1's scope to currently-eligible subjects only, deferring
inactive-subject Evidence to a future Requirement. This means **zero
changes to Q-010's already-shipped, already-approved code**, which was
Architecture's preferred outcome over the two alternatives considered
(extending Q-010, or introducing a new Q-011 SERVICE-actor indirection —
both rejected, see ADR-013's Alternatives section).

## What This Package Is Not

Not Product Owner approval. Not ADR acceptance. Not Implementation Design.
Not implementation authorization. Nothing has been staged or committed.

## Next Step

Product Owner review and explicit approval decision on Architecture V1 and
ADR-013.
