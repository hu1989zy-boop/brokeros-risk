# Q-013 Requirement V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12.

## Task ID

Q-013 — Action Provenance Foundation (new task; unblocks the Action
prerequisite of Q-008's Implementation Gate, now that Q-012/Decision is
committed).

## Stage

Requirement (first stage of the mandatory workflow). Self-review by
Claude Code, holding the external Architect role — not a Product Owner
approval. Per protocol §3, work stops here pending the Product Owner's
Gate Decision.

## Scope Reviewed

`docs/requirements/Q-013-Action-Provenance-Foundation.md` V1, checked for
internal consistency and against the actual approved upstream sources it
cites.

## Files Inspected

- `docs/requirements/Q-007-Requirement.md` (Action's canonical
  definition — grepped, cross-checked).
- `docs/requirements/Q-008-Requirement.md` §7.6 (Action definition), §13
  (Action Requirements, full read) — confirms exactly what Q-008 needs
  from an Action provider and what it explicitly defers.
- `docs/requirements/Q-012-Decision-Provenance-Foundation.md` (structural
  precedent, and the source of the reused narrow provenance-read
  contract Action's own validation depends on).

## Verification Executed

Not applicable — no code exists yet. `GitStatus.txt`/`GitDiffStat.txt` in
this package confirm the only change is the one new Requirement file.

## Requirement Status

V1, drafted, self-reviewed, **not yet approved**.

## Findings

No citation error found (checked: Q-008 §7.6 and §13 both verified
against the live file before citing them — the same check that caught a
real defect during Q-012's own self-review was applied again here
deliberately).

Two design choices are flagged explicitly in §5.3 as deviations from or
unverified extensions of the Q-012/Q-011 precedent, rather than silently
assumed:

1. **No two-tier (narrow-vs-full-detail) read split.** Unlike Decision
   and Evidence, this Requirement proposes Action exposes a single read
   contract including its intent text, reasoned from Q-008 §13's own
   language treating Action references as ordinary case-visible content
   (unlike Decision, which Q-008 §12 explicitly keeps at arm's length).
   This is a substantive Functional Requirement decision, not a detail —
   flagged for explicit confirmation rather than assumed correct because
   it matches "the pattern so far."
2. **Immutability/no-correction default.** Carried over from Decision by
   analogy, but — unlike the `PROPOSED`-only status choice, which is
   directly supported by Q-008's own text — this specific assumption has
   no equivalent direct textual support and is flagged separately as
   needing its own confirmation, since a future approval-workflow
   Requirement might require Action to be mutable in a way Decision never
   needed to be.

This is a deliberate departure from how Q-009 through Q-012 were
self-reviewed: rather than only checking for internal consistency and
citation accuracy, this review also explicitly separates "directly
supported by an upstream document's text" from "carried over by analogy,
unverified" — because Q-013 is the first Requirement in this lifecycle
that proposes a genuine deviation from the immediately preceding
module's pattern rather than a straightforward reuse of it.

## Remaining Risks

- If either §5.3 choice is overridden, §7 (Functional Requirements) and
  §10 (Acceptance Criteria) both need revision before Architecture
  begins — flagged in §16's Review Checklist so this isn't missed at the
  next gate.
- `act-` prefix length arithmetic (`CHAR(40)`, same as `dec-`) is stated
  as a probable Architecture-stage confirmation, not asserted as final,
  matching the discipline Q-012's own Architecture applied to itself
  after nearly copying `CHAR(39)` by habit.

## Out-of-Scope Issues

None identified beyond what the Requirement itself already lists as
Non-Goals (§5.2).

## Update — 2026-08-31, same day

Both §5.3 questions were put to the Product Owner directly in chat, who
stated a governing principle (prioritize extensibility and stability) and
decided both: adopt the two-tier narrow/full-detail read split matching
Decision/Evidence (reversing this draft's original single-read proposal),
and keep Action immutable/`PROPOSED`-only while shaping the `ActionStatus`
column to extend without a breaking migration. The Requirement document
was updated accordingly (§4, §5.1–§5.3, §6, §7 [FR-007 split into FR-007/
FR-008, vendor-semantics renumbered FR-009], §9, §10, §14, §16, §17). This
is recorded here because it happened in the same self-review cycle as
this package, before the Requirement's overall Gate Decision.

## Recommendation

Present the now-updated Requirement to the Product Owner for its overall
Gate Decision. The two §5.3 questions this review flagged are resolved;
no other open question remains.

## Gate Decision

**PASS** (self-review only — the Product Owner's overall Gate Decision on
the Requirement remains outstanding, though both design-choice questions
this review specifically flagged are now resolved).
