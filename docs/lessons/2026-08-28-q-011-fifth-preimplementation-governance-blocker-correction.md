# Q-011 Fifth Pre-implementation Governance Blocker Correction Lessons Learned

## What Happened

Codex, given the round-four-approved documents and resume Prompt, applied
its own stop-on-contradiction rule one more time before writing any code
and found three formal status/reference contradictions: a closing summary
sentence in the Requirement document still spoke of an approved version in
the present tense as an unapproved draft; the same document contained six
references to a section number (§20) that does not exist within it; and
ADR-013's introduction still described its own amendment as pending after
the same document's Status line and Approval Boundary had already recorded
it as re-accepted.

## Reusable Lesson: A Status Fact Can Go Stale Within a Single Document, Not Just Across Documents

Every prior round's lesson focused on facts drifting between sibling
documents (Architecture says X, Design still says Y). This round's defects
were both *within* single documents: a closing paragraph written earlier
in the same editing pass than the header it now contradicts. Updating a
document's top Status line and its Gate section is not sufficient — every
closing summary, "Verdict," or "Status" sentence anywhere else in the same
file needs the same check. The practical rule: after changing a document's
approval state, search that document's own full text for the state word
("draft," "pending," "not approved," "candidate") in addition to checking
sibling documents.

## Reusable Lesson: Section Cross-References Are a Verifiable Contract

A `see §N` reference is not decoration — it is a claim that a specific,
matching section exists. When a document's structure changes (sections
added, renumbered, or content moved into an existing section instead of a
new one), every reference to a section number must be re-verified against
the actual current heading list, not assumed still valid. This round fixed
each of six references individually by checking what each was actually
trying to point to, rather than doing a blind global replace — a global
replace would have been faster but could not have distinguished
"this document's own §19" from "the other document's §20.10," which had
different correct targets.

## Reusable Lesson: A Correction's Own Introduction Needs the Same Scrutiny as Its Conclusion

Both defects that survived into a live document were in *introductory*
or *closing* prose — the kind of sentence written for readability, once,
and rarely revisited when the substantive decision it summarizes changes
later. The fix each time was to make the sentence itself point to the
authoritative source (a specific section) rather than restate a status
claim independently, so future drift has one place to be caught rather
than needing every restatement to be manually kept in sync.

## Not Yet Resolved

Product Owner confirmation of this round's wording fixes, and reaffirmation
(or fresh grant) of implementation authorization, are outstanding. No code
exists. No Q-011 business behavior was changed by this round.
