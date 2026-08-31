# Q-011 Design V3 Consistency Audit Lessons Learned

## What Happened

Codex halted a second time on Q-011, against a Design (V2) that had
already been formally approved and authorized. Every one of the five
issues it reported was real, and four of the five existed because the
first correction round fixed only the exact lines Codex had cited without
checking whether the same rule was restated anywhere else in the document
set. The Product Owner then directed a full end-to-end consistency audit
instead of another line-patch, with an explicit required ordering, explicit
required deliverables (a canonical execution-order table, a database
constraint-to-test table, an actually-executed text scan, a document
priority statement), and an explicit instruction not to let a Codex Prompt
redefine the approved design.

## Reusable Lesson: "Fixed" Requires Grep, Not Memory

When correcting a defect that a rule was restated incorrectly, search the
entire document set for every restatement of that rule before declaring
it fixed. This session's own round-one correction pass believed it had
fixed the ordering and the `UNCHANGED` outcome; it had, in the two sections
it touched. It had not touched: a summary table three sections away, a
different document's mirror of the same procedure, and a capability-grant
bullet in the scope section. The only way to know a document-wide claim
("no more X anywhere") is true is to actually run the search and read the
matches — not to recall having fixed X.

## Reusable Lesson: A Single Authoritative Statement, Restated Elsewhere as Reference

The recurring defect pattern was the same rule expressed independently in
multiple places, which drift independently. The fix applied here — make
one section (§11.1) the explicit, authoritative statement, and make every
other place that touches the same rule (§6, §12.1/§12.2, Architecture §14)
say so explicitly and point back to it rather than re-deriving its own
version — is a generally applicable pattern for any design with a rule
that matters enough to appear in a summary table, a detailed procedure,
and a mirrored Architecture section. Restating a rule for readability is
fine; restating it as an independently-worded second source of truth is
how this class of defect keeps happening.

## Reusable Lesson: A Prompt Is Downstream of the Design, Never a Peer

The round-one resume Prompt's prose summary of execution order did not
match the Design it was supposedly built from, and that mismatch is what
actually caused Codex's second halt (finding #2). A Prompt should quote or
directly reference the Design's authoritative section rather than
re-describing it in the Prompt-writer's own words — re-description is
where independent drift enters. This is now stated explicitly in Design
§1.1 as a fixed priority order.

## Not Yet Resolved

Product Owner approval of Implementation Design V3 and Architecture V2 is
outstanding. No implementation authorization exists for V3. No Codex
Prompt has been issued this round. No code exists.
