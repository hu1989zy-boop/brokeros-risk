# Q-011 Requirement Architect Review Lessons Learned

## What Changed in the Operating Model

The Product Owner explicitly moved the external Architect review role from a
separate ChatGPT-based process to Claude Code, in the same session that
already drafted the Q-011 Requirement. This was flagged back to the Product
Owner before acting on it, because it collapses drafting and review into one
party — exactly the pattern
`prompts/Claude-Code-BrokerOS-Risk-Project-Handover-Prompt.md` §1 and every
prior Q-009/Q-010 approval record warn against ("do not self-approve your own
architecture/design merely because you authored it"). The Product Owner
confirmed a specific mode: Claude Code performs real, adversarial review at
each gate, but final PASS/approval authority stays explicitly with the
Product Owner rather than being implied by the review passing. That
distinction is the whole point — a self-review can still be useful if it is
genuinely adversarial and its limits are disclosed, but it cannot substitute
for an independent decision-maker.

## What the Self-Review Actually Found

Treating the review as a real adversarial pass (not a formality) surfaced
seven real findings in the Q-011 V1 draft, listed in
`docs/requirements/Q-011-Evidence-Provenance-Foundation.md` §18. The most
material one: V1 allowed a "correction" to silently change which Trading
Account an Evidence record concerned, because no requirement pinned the
corrected record's subject reference across the correction. That is a real
integrity defect a careless self-review would have missed by only checking
"does this look complete" rather than "what could go wrong."

## Reusable Lesson

Self-review only has value if it is run as if reviewing someone else's work:
actively look for what the draft assumed without stating, what precedent in
this repository it failed to reuse (here: Q-008's own reason/audit-on-read
patterns), and what a hostile reader would exploit. Disclosing "this is a
self-review, not an independent one" in the artifact itself — rather than
letting the document's format imply an independence it doesn't have — is
part of doing this honestly.

## Not Yet Resolved

Product Owner approval of the V2 Requirement is still outstanding. No
Architecture, ADR, Implementation Design, or Codex prompt exists yet. No
implementation, dependency, migration, API, commit, or push was created by
this review.
