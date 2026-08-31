# Q-011 Architecture and ADR Analysis Lessons Learned

## What Was Decided

Architecture resolved the one open question the approved Requirement left
(§14.1: how should Evidence validate its Trading Account subject against
Q-010) by choosing reuse over extension: call Q-010's existing
`validateForNewRiskCaseAssociation` contract unchanged, accept the resulting
capability grant on Evidence-recording actors as normal least-privilege
scoping, and narrow Q-011's V1 subject scope to currently-eligible accounts
rather than modifying Q-010's already-shipped code.

## Reusable Lesson: Prefer Scope-Narrowing Over Extending a Shipped Dependency

When a downstream Requirement's open question could be resolved either by
extending an already-implemented, already-approved upstream capability, or
by narrowing the downstream Requirement's own first-increment scope, prefer
narrowing the downstream scope. Extending shipped code re-opens a closed
gate's risk surface for the benefit of a not-yet-real second use case;
narrowing scope keeps the blast radius local and leaves the extension
available later if a concrete need actually appears. This mirrors the same
judgment Q-010's own Requirement Candidate Analysis applied when it chose
Trading Account over Evidence specifically because Trading Account did not
require inventing anything upstream had not already delivered.

## Reusable Lesson: A "Coupling" Finding Isn't Always a Defect

The Requirement-stage self-review (§18) flagged the capability-coupling
concern as something Architecture needed to resolve, framed close to a
defect. Re-examining it at Architecture time with the actual precedent
(Q-008 will need the identical grant) showed it was better understood as an
ordinary least-privilege capability grant, not a coupling problem requiring
new infrastructure. A Requirement-stage finding is a flag for Architecture
to examine, not a predetermined conclusion Architecture must accept without
re-testing it against real precedent.

## Not Yet Resolved

Product Owner approval of Architecture V1 and ADR-013 acceptance are still
outstanding. No Implementation Design, implementation, dependency,
migration, endpoint, or commit was created by this analysis.
