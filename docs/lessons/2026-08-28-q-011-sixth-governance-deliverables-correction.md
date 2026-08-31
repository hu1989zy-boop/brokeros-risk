# Q-011 Sixth Governance Correction — Deliverables Section Lessons Learned

## What Happened

Immediately after the Product Owner gave Codex explicit implementation
authorization directly in a request (not merely a file reference — Codex
correctly held that line), Codex found one more defect before writing code:
Requirement §15 ("Deliverables") still described, in the present tense,
an authorization scope frozen at V1/V2 drafting time — before Architecture,
ADR-013, Implementation Design, and implementation itself were all
separately approved. This is the third occurrence of the same defect
shape found in rounds five and six.

## Why This Keeps Happening

This Requirement document has been revised in place five times (V1→V2→V3,
plus three appended governance-correction records). Every revision
correctly updated the canonical status location (top Status table, §17).
No revision systematically checked whether some *other* section of the
same document made its own independent, present-tense authorization claim
that the canonical update did not touch. §19 (round five), ADR-013's
introduction (round five), and §15 (this round) are three different
instances of exactly this — not three unrelated bugs, one recurring
structural gap.

## The Structural Fix Actually Applied, Not Just a Third Patch

Every section corrected across rounds five and six now follows the same
rule, stated explicitly in the text itself: **this section is not an
independent source of truth for current status; §17 (or the equivalent
canonical section in the other documents) is, and if the two ever appear
to disagree, the canonical section governs.** This mirrors the same
technique already applied to execution ordering (Design §11.1 as the sole
authoritative statement) and to cross-document version references (Design
§1.1's removal of hard-coded version numbers). The aim is not "no section
will ever go stale again" — sections written in prose can always drift —
but "when one does, the document itself tells the reader which section to
trust," so a future drift is a quick fix rather than a fresh investigation.

## Reusable Lesson: A Full Sweep, Not a Point-Fix, After the Second Occurrence

The first time this defect shape appeared (round five, two instances), it
was fixed as two point-fixes without a full-document sweep for a third.
That sweep should have happened after the *second* instance was found in
the *same* round, on the theory that a pattern seen twice in one place is
likely to recur elsewhere in the same document. This round's sweep (see
`ConsistencyAudit.md`) found no further instance, but it should have been
run in round five, not round six.

## Honest Assessment for the Product Owner

Six rounds, six real and distinct defects, zero false alarms, zero code
written. Each round's fix has been narrower in scope than the last (six
Requirement/ADR/Architecture/Design sections in round four, down to two
sentences in round five, down to one section in round six), which is
weak evidence the surface area of remaining defects is shrinking, not
proof it has reached zero. This round's full sweep for the specific
pattern found nothing further of that shape; it cannot rule out a
differently-shaped defect.

## Not Yet Resolved

Product Owner confirmation of the §15 wording fix. The underlying
implementation authorization is not reopened by this round and remains in
force pending that confirmation.
