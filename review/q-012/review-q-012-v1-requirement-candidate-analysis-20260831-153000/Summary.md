# Q-012 Requirement V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12.

## Task ID

Q-012 — Decision Provenance Foundation (new task; unblocks the Decision
prerequisite of Q-008's Implementation Gate).

## Stage

Requirement (first stage of the mandatory workflow). This is a
self-review by Claude Code, holding the external Architect role — not a
Product Owner approval. Per protocol §3, work stops here pending the
Product Owner's Gate Decision.

## Scope Reviewed

`docs/requirements/Q-012-Decision-Provenance-Foundation.md` V1, checked
for internal consistency and for consistency against the actual approved
upstream sources it cites.

## Files Inspected

- `docs/requirements/Q-007-Requirement.md` (full read) — canonical domain
  model, Decision-as-Core-Domain definition.
- `docs/adr/ADR-009-brokeros-risk-core-domain-model.md` (grepped for
  Decision-relevant content) — confirms no detail beyond Q-007 that would
  contradict this Requirement.
- `docs/requirements/Q-008-Requirement.md` (targeted reads: §7.5 Decision,
  §12 Decision Requirements, §26 Implementation Gate) — confirms exactly
  what Q-008 needs from a Decision provider and what it explicitly defers.
- Prior approved Requirements Q-009, Q-010, Q-011 (from this session's
  existing familiarity, cross-checked against their current committed
  state) — used as the structural/pattern precedent.

## Verification Executed

Not applicable at this stage — no code, migration, or test exists yet.
`git status`/`git diff --stat` confirm the only change in the working
tree is the one new Requirement file (`GitStatus.txt`/`GitDiffStat.txt`
in this package).

## Requirement Status

V1, drafted, self-reviewed, **not yet approved**.

## Findings

One defect found and fixed during self-review: two citations referenced
"Q-008 §11" for the deferred outcome-taxonomy/confidence/rule-metadata
language; the actual location is Q-008 §12 ("Decision Requirements"), the
same section as the other Decision Requirements citations in this
document. Verified against the live file and corrected before this
package was produced — not left for a later round, unlike several
citation-drift defects found during Q-011's governance rounds.

No other inconsistency found between this Requirement and its cited
sources. In particular:
- §5.3's justification for omitting a Q-010-style eligibility service is
  a reasoned design choice grounded in an actual difference between
  Trading Account's mutable lifecycle state and Decision's immutability
  once recorded — not an oversight, and flagged explicitly in the
  document's own Review Checklist (§16) as something the Product Owner
  should either accept or override before Architecture begins, rather
  than something silently assumed correct.
- The decision to omit a correction/supersede use case (unlike Q-011's
  Evidence) is directly supported by Q-008 §12's own text ("Reassessment
  creates a new Decision... never overwrites or mutates the historical
  Decision") — this is not a scope reduction invented by Claude Code, it
  is what Q-008 itself already requires.

## Remaining Risks

- The Decision-to-Evidence persistence shape (join table vs. another
  representation) is deliberately left open for the Architecture stage
  (§14), consistent with keeping schema decisions out of the Requirement
  stage.
- If the Product Owner disagrees with §5.3's reasoning, that must be
  resolved before Architecture, since it materially changes the
  Functional Requirements (§7) and Acceptance Criteria (§10).

## Out-of-Scope Issues

None identified beyond what the Requirement itself already lists as
Non-Goals (§5.2).

## Recommendation

Present to the Product Owner for a Gate Decision. No implementation,
architecture, or ADR work has begun, and none should begin until that
decision is recorded.

## Gate Decision

**PASS** (self-review only — this is not the Product Owner's Gate
Decision, which remains outstanding per protocol §3/§12).
