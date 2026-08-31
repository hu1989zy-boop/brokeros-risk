# Q-011 V11 Consistency Audit

All commands below were actually executed against the final (post-fix)
state of the four governing documents, using `rg`. Each result is the real
output, manually reviewed line by line. Files searched (abbreviated F1–F4
below):

- F1 = `docs/requirements/Q-011-Evidence-Provenance-Foundation.md`
- F2 = `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
- F3 = `docs/adr/ADR-013-evidence-provenance-foundation.md`
- F4 = `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`

## `ELIGIBLE_FOR_NEW_ASSOCIATION`

11 matches across F1–F4. Manually reviewed: all describe the term as one
of two accepted outcomes (alongside `RECOGNIZED_NOT_ELIGIBLE`), or appear
in historical/negative context describing what earlier drafts wrongly
required and how it was fixed. No live claim requires it exclusively.

## `RECOGNIZED_NOT_ELIGIBLE`

Co-occurs with the above in every match. Confirmed accepted, not rejected,
everywhere it is asserted as current behavior.

## `NOT_RECOGNIZED`

Confirmed as the only rejecting outcome everywhere it appears (F1 FR-002,
F2 §2/§9/§16/§23, F3 Subject validation/Consumer boundary, F4 §6.1/§11.4/
§13/§16.2).

## `eligible` / `ineligible`

All remaining live uses correctly describe "eligible for a new Risk Case
association" as the *rejected-as-sole-criterion*, higher bar that Q-011
does not require — never as a current Q-011 requirement. The two
remaining bare "ineligible" matches (ADR-013 line ~327, Design §20.1 line
~1114) are both explicitly historical: one names what was removed from a
list, the other is inside the frozen round-one finding record.

## `inactive` / `retired`

All remaining matches describe `RECOGNIZED_NOT_ELIGIBLE` accounts
(inactive/retired) as an in-scope example, consistent with the confirmed
decision. No remaining match excludes them.

## `future Requirement`

8 matches (F1×1, F2×3, F3×3, F4×1 after this round's edits). Reviewed
individually:
- F1 line 355: `Q011-SR-005`, about automated/`SERVICE`-actor authoring —
  correctly scoped, unaffected by this round.
- F2 lines 428/534/649: retention/audit-search deferral, access-log
  growth, and §9's "not a future Requirement" (negative statement
  confirming inactive-subject Evidence is *not* deferred) — all correct.
- F3 lines 97/147/207: all now describe automated-source/`SERVICE`-authoring
  deferral or the (rejected) Q-010-extension alternative's now-corrected
  reasoning — none defer inactive-subject Evidence any longer.
- F4 line ~1345 (§20.9 intro): now excludes inactive-subject Evidence by
  explicit removal note.

## `HUMAN`

Reviewed every match in F1 (9), F2, F3 (5), F4 (many, including the
rewritten §5.2/§11.1/§11.4/§6). No remaining match asserts `HUMAN` is
required for either read use case. Requirement Goal 5 (F1) is the one
substantive fix this round; `Q011-FR-005` was already correctly scoped in
every prior round.

## `SERVICE`

Reviewed every match in all four files. All describe `SERVICE` as excluded
from *authoring* (record/correct) only — never from reading. No match
claims a `SERVICE` actor cannot use either read use case.

## `automated consumer`

Zero exact-phrase matches remain (the phrase itself was removed from
ADR-013's Consumer Boundary section, replaced with the explicit two-rule
distinction: consumer-contract limitation vs. actor-type limitation).

## `read-only`

Matches limited to: (a) the Provenance-read use case, correctly described
as genuinely read-only (zero writes) in F4's execution-order table, and
(b) explicit negative statements that the full-detail-read transaction is
**not** database-level read-only (F2 §13, F4 §2/§7.3-adjacent). No match
asserts the full-detail-read transaction is read-only.

## `Implementation Allowed`

7 live matches across F1/F2/F4 (plus one unrelated Q-008 mention in F2
line ~548). Every Q-011-scoped match now reads **NO — Q-011 IMPLEMENTATION
BLOCKED / NOT AUTHORIZED**, consistently, in all four documents.

## `Implementation authorized` / `AUTHORIZED`

Reviewed every match. No document claims current implementation
authorization; all either state it is blocked/pending, or refer to a past
round's now-superseded authorization explicitly marked as not carried
forward.

## `APPROVED`

Reviewed every match referring to Q-011's own gates (excluding unrelated
Q-008/Q-009/Q-010/general text). Each now correctly distinguishes a
document's last *approved* version (preserved, historical) from its new
*candidate* version (explicitly not yet approved). No document claims its
new candidate is approved.

## `pending`

Every "pending Product Owner approval"/"pending re-acceptance" match
refers to a real, current pending state introduced by this round. No
stale pending claim was found for content that is actually settled (e.g.,
the execution ordering in §11.1/§11.4, which is explicitly not reopened).

## `Next gate`

Exactly one "Next gate" statement remains per document section (Requirement
§17, Architecture §24, Design §21) — the duplicate, contradictory pair in
Design §21 (one telling Codex to proceed, one saying approval was still
pending) has been resolved into a single statement.

## `V2` / `V3` / `V4` / `V5`

Hard-coded version numbers were removed from Design §1.1's priority list
(the one place they had already gone stale twice) in favor of "see that
document's own Document Status." Every other version reference was checked
against the actual current Document Status/Gate section of the document it
names and found consistent after this round's edits.

## Cross-Document Consistency Checks (as required)

1. **Requirement** Goals/FR/SR/AC/Current Gate: consistent after the Goal 5
   fix; no remaining contradiction found between Goal 5, `Q011-FR-005`, and
   the Acceptance Criteria.
2. **Architecture** Decision Summary/Subject Validation/Security/Failure
   Model/Traceability/Future Requirements/Gate: consistent after §23 items
   15/17 fixes; §2, §9, §16, §22 already correct from round three.
3. **ADR** Decision/Alternatives/Consequences/Security/Deferred Decisions:
   consistent after the full amendment; Security Implications' `HUMAN`
   scoping was already correct and required no change.
4. **Design** Scope/Authorization/Use Cases/§11.1/§11.4/Tests/Future
   Scope/Gate: consistent after §1.1/§20.9/§21 fixes; §11.1/§11.4/§6/§5.2
   were already correct from round three.
5. **Cross-document**: subject bar, ActorType read policy, and
   implementation-authorization state are now identical in substance
   across all four documents (verified by the searches above), though each
   is currently in a different *gate* state (Requirement V3/Architecture
   V4/ADR-013 amendment/Design V5, all pending) as expected for candidates
   awaiting the same round of approval.
