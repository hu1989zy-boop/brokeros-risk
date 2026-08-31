# Q-011 V6 Implementation Design V2 Correction Summary

- Review ID: `Q-011-V6-IMPLEMENTATION-DESIGN-V2-CORRECTION-20260828-183317`
- Trigger: Codex executed the 2026-08-28 Implementation Authorization
  Prompt built from Implementation Design V1, read all four governing
  documents as instructed, and halted before writing any code, reporting
  six precise, cited contradictions instead of resolving them itself.
- Prepared by: Claude Code, holding the external Architect role by explicit
  Product Owner direction; self-review, not independent.
- Git baseline: `main` at `091d410`, clean, synced with `origin/main`.

## Verdict

All six findings Codex reported are real. None were caused by
misunderstanding the design; all were genuine internal contradictions or
implementation-infeasible constraints in V1. Implementation Design V2 fixes
all six; full record is in
`docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
§20.1.

1. `evidence_operation_history`'s cross-table `CHECK` constraint was not
   expressible in MySQL — fixed by denormalizing `operation_type` onto that
   table.
2. Correction's idempotent-replay path incorrectly re-checked target status
   `ACTIVE`, which is false immediately after the correction it is
   replaying — fixed by checking operation-ID replay before any
   state-dependent validation.
3. Recording's idempotent-replay path incorrectly re-ran Q-010 subject
   validation before checking for replay, breaking replay if the subject
   later became ineligible or Q-010 was temporarily down — fixed by the
   same replay-first ordering.
4. `EvidenceOperationOutcome.UNCHANGED` contradicted the idempotency table
   and was never actually produced by any described scenario — removed;
   exact replay returns the original `CREATED`/`CORRECTED` outcome
   verbatim.
5. The full-detail-read transaction was called "read-only" while requiring
   a write (the access-log insert) — corrected terminology in Architecture
   §13 and this Design's §2; no decision changed.
6. The design summary said "three tables" against an actual four —
   corrected throughout.

One Architecture-level correction was needed (item 5, terminology only, no
decision change) alongside the Implementation Design V2 revision.

## What This Package Is Not

Not Product Owner approval of V2. Not implementation authorization for V2
— the prior V1 authorization does not carry forward. Nothing has been
staged, committed, or (obviously) implemented; Codex wrote zero code before
halting.

## Next Step

Product Owner review and approval decision on Implementation Design V2.
Only after that approval should Codex be given a short resume prompt
pointing at V2.
