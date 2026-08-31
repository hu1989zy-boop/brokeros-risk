# Q-011 Fourth Governance Consistency Correction Lessons Learned

## What Happened

An explicit, pre-written, mechanical governance-consistency task ran
against Q-011's four governing documents after round three's Design V4/
Architecture V3 were approved and implementation was authorized. It found
that round three's fix had not fully propagated, and — more seriously —
that ADR-013 had never been amended at all when the subject-bar correction
landed in round three, leaving an accepted ADR silently contradicting its
own Requirement.

## Reusable Lesson: An Accepted ADR Does Not Self-Correct

When a Requirement or Architecture decision is corrected after an ADR
recording it has already been accepted, the ADR does not become accurate
automatically — it is a separate artifact that must be explicitly amended
and separately re-accepted. Round three corrected Architecture and Design
to match a Requirement decision but never checked whether the ADR that
originally recorded the (wrong) decision needed the same correction. Any
future correction to a decision that has an accepted ADR must include the
ADR in the same sweep, not treat it as a passive record that inherits
downstream fixes for free.

## Reusable Lesson: ActorType Restrictions and Consumer-Contract Restrictions Are Different Axes

"Q-008 cannot use the full-detail contract" and "no automated actor can
use the full-detail contract" look similar but are not the same rule — the
first is about which *contract* a consumer is allowed to call; the second
is about which *actor type* may call a contract at all. ADR-013 conflated
them ("not available to Q-008 or any automated consumer"), which reads as
implying the second when only the first was ever decided. Whenever a
consumer-boundary decision and an actor-type decision interact, state them
as two separate sentences, not one combined phrase.

## Reusable Lesson: Two "Next Gate" Statements in One Document Is Not a Style Issue

Finding two directly contradictory "Next gate" paragraphs in the same
section (Design §21) is unambiguous, mechanical evidence that a status
update was incomplete — no interpretation is required to see the
document disagrees with itself. Any time an edit updates a document's gate
status, search for every other occurrence of "Next gate" (or the
equivalent status-summary phrase) in the same document before considering
the update complete.

## Reusable Lesson: Remove Hard-Coded Cross-Document Version Numbers Where Possible

Design §1.1's priority-order list named specific version numbers for
sibling documents ("Architecture, currently V2") and went stale twice by
round four. The fix was not to update the numbers again — that would go
stale a third time — but to remove them and point to each document's own
Document Status instead. Where a cross-reference's only job is ordering or
pointing, prefer a reference that cannot go stale over one that must be
kept manually synchronized across every future round.

## Not Yet Resolved

Product Owner approval of all four candidate documents (Requirement V3,
Architecture V4, ADR-013 amendment, Implementation Design V5) is
outstanding, along with a fresh implementation authorization. No code
exists. Whether a fifth round of correction will be needed cannot be
predicted from this round alone.
