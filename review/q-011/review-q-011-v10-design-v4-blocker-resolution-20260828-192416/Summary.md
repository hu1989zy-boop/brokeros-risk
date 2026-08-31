# Q-011 V10 Design V4 Blocker Resolution Summary

- Review ID: `Q-011-V10-DESIGN-V4-BLOCKER-RESOLUTION-20260828-192416`
- Trigger: Codex's formal written blocker report,
  `review/q-011/q-011-v3-implementation-blocker-report-20260828-191130.md`,
  filed against approved/authorized Implementation Design V3. Codex wrote
  zero code before filing it, per its instructions.
- Prepared by: Claude Code, holding the external Architect role by
  explicit Product Owner direction; self-review, not independent.
- Git baseline: `main` at `091d410`, clean, synced with `origin/main`.

## Outcome

- Implementation Design bumped to **V4** (DRAFT, pending Product Owner
  approval).
- Architecture bumped to **V3** (DRAFT, pending Product Owner approval).
- ADR-013 and Requirement V2 unaffected.
- Implementation Allowed: NO. No Codex Prompt issued this round.

## The Four Blockers, Verified and Resolved

1. **Requirement-vs-Architecture contradiction on the subject bar** — real,
   and the one genuine scope question in this round. Approved Requirement
   `Q011-FR-002` requires only that Q-010 *recognize* the subject
   (explicitly a lower bar than "eligible for a new Risk Case
   association"). Architecture §9 and Design §6.1 had nonetheless required
   the stricter "eligible" bar — a silent narrowing of an approved
   Requirement, which AGENTS.md prohibits. **The Product Owner decided:
   the Requirement's "recognized" bar stands.** Architecture/Design
   corrected throughout (not just the two cited sections — every place
   that mentioned eligibility was swept and fixed).
2. **§5.2 overclaimed `HUMAN` applies to every use case** — contradicted
   §11.4, which correctly limits `HUMAN` to Record/Correct (matching
   Requirement `Q011-FR-005`). Fixed: wording rewritten to separate "how
   the actor reaches Q-011" (HTTP/in-process, no non-web command) from
   "which actor type is required" (only for authoring use cases).
3. **§21's Architecture-approval line was stale** — a leftover from an
   earlier edit that replaced only the bullets below it, not the summary
   line above them. Fixed, and both documents' gate sections are now
   edited together whenever either changes.
4. **§8.5 was not actually exhaustive** — rebuilt by walking every column
   of §8.1–§8.4 in source order (not from memory) into four per-table
   lists. Added: `source`/`status` enum checks, three UUIDv4-shape checks,
   `operation_type` enum checks on two tables, explicit FK-target rows,
   and corrected the byte-bound row that had wrongly attributed the
   correction-reason length constraint to `evidence_record`.

## A Fifth Defect Found by This Audit, Not by Codex

Verifying blocker 4 required understanding what `evidence_operation.
evidence_id` actually points to for a `CORRECT` operation. It was
ambiguously described as "resulting/target record." Resolving it (it must
be the *target* record, so `before_status`/`after_status` describe a real
`ACTIVE`→`SUPERSEDED` transition rather than a trivial always-`ACTIVE`
value) surfaced that neither column had a stated allowed-value relationship
to `operation_type` at all. Fixed: two new bidirectional `CHECK` clauses,
traced in the rebuilt §8.5.

## What This Package Is Not

Not Product Owner approval. Not implementation authorization. Nothing
staged, committed, or implemented — Codex has written zero code across all
three halts.

## Next Step

Product Owner review of the change summary delivered in chat, then an
explicit approval decision on Implementation Design V4 and Architecture
V3.
