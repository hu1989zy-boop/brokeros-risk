# Q-011 V8 End-to-End Consistency Audit and Design V3 Summary

- Review ID: `Q-011-V8-DESIGN-V3-CONSISTENCY-AUDIT-20260828-190419`
- Trigger: explicit Product Owner instruction, after Codex correctly
  halted a second time on Implementation Design V2, to perform a full
  end-to-end, field-by-field, order-by-order consistency review of the
  entire Q-011 document set rather than patching only the cited lines.
- Prepared by: Claude Code, holding the external Architect role by
  explicit Product Owner direction; self-review, not independent.
- Git baseline: `main` at `091d410`, clean, synced with `origin/main`.

## Outcome

- `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
  bumped to **V3** (DRAFT, pending Product Owner approval).
- `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
  bumped to **V2** (DRAFT, pending Product Owner approval).
- ADR-013 unaffected (no ADR-level decision changed).
- Requirement V2 unaffected and remains approved.
- Prior implementation authorization (granted against Design V2) does
  **not** carry forward. Implementation Allowed: NO. No Codex Prompt
  issued this round.

## What Changed and Why (see Implementation Design §20.2 for full detail)

Five real defects, all caused by the round-one correction pass fixing only
the specific lines Codex had cited without checking every other place the
same rule was restated:

1. Implementation Design §6 (use-case table and rules) still described the
   pre-fix ordering and `UNCHANGED` outcomes.
2. The relative order of authorization/`HUMAN`-check versus the replay
   check was never stated explicitly anywhere, so a resume Prompt guessed
   incorrectly. Fixed by making §11.1 the single authoritative,
   unambiguous statement, plus a new §11.4 canonical execution-order table
   covering all four use cases.
3. Architecture §14 still had the complete pre-fix ordering — the
   round-one pass never touched it.
4. Design §2 and Architecture §9 both still implied correction calls
   Q-010, contradicting §5.1/§6.2/§9(Design)'s correct statement.
5. The `evidence_operation_history.reason` `CHECK` constraint only enforced
   one of its two required directions and no length floor.

A sixth issue, not part of Codex's report but found during this audit's
own sweep: Implementation Design §1.2 also incorrectly listed
`trading-account-reference:read` as a grant correcting actors need (fixed).

## Deliverables Added Per Product Owner Instruction

- §1.1: explicit document-priority statement (`AGENTS.md` > Requirement >
  Architecture > ADR > Implementation Design > Codex Prompt), stating a
  Prompt never overrides the Design.
- §11.4: canonical execution-order table covering Record, Correct,
  Provenance read, and Full-detail read.
- §8.5: database constraint-to-test traceability table.
- §20.5: full-text consistency scan, actually executed against the final
  document state (not merely asserted) — see the section for exact
  `grep` commands and results, including one self-correction where an
  initial scan claim ("zero matches" for "read-only transaction") was
  itself imprecise and was fixed before this package was written.

## What This Package Is Not

Not Product Owner approval of V3/V2. Not implementation authorization.
Nothing has been staged, committed, or implemented — Codex has written
zero code across both halts.

## Next Step

Product Owner review of the change summary delivered in chat, then an
explicit approval decision on Implementation Design V3 and Architecture
V2. Only after that approval will a new Codex Prompt be issued, built
strictly from Design §11.1/§11.4.
