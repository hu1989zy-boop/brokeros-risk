# Q-012 Architecture V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12.

## Task ID

Q-012 — Decision Provenance Foundation.

## Stage

Architecture Analysis. Self-review by Claude Code, holding the external
Architect role. Per protocol §3, work stops here pending the Product
Owner's Gate Decision — ADR-014 drafting and Implementation Design do not
begin automatically even if this Architecture is approved.

## Scope Reviewed

`docs/architecture/q-012-decision-provenance-foundation-architecture.md`
V1, checked for: consistency with approved Requirement V1; consistency
with the actual committed Q-009/Q-010/Q-011 schema and code (not just
their own design documents); and no reopening of a Requirement-gate
decision.

## Files Inspected

- `docs/requirements/Q-012-Decision-Provenance-Foundation.md` (full,
  approved V1).
- `backend/src/main/resources/db/migration/V4__create_evidence_provenance_foundation.sql`
  and `V3__create_trading_account_reference_authority.sql` (grepped for
  actual column definitions and FK constraints) — this is what surfaced
  the one substantive finding below.
- `docs/architecture/q-011-evidence-provenance-foundation-architecture.md`
  (structural/section-format precedent, and confirmed it already
  anticipated "a future Decision capability" as an unbuilt consumer of
  its narrow contract — consistent with Q-012's own premise).
- `docs/requirements/Q-008-Requirement.md` §26 (confirmed Q-012 correctly
  leaves Action/ActionOutcome as the two remaining Q-008 prerequisites,
  not something this Architecture should reserve or design).

## Verification Executed

Not applicable — no code or migration exists yet at this stage.
`GitStatus.txt`/`GitDiffStat.txt` in this package confirm the only
working-tree changes are the two new Q-012 documents.

## Requirement Status

Q-012 Requirement V1 — APPROVED (unchanged by this Architecture, except
one corrected technical aside in §9 — see Findings).

## Architecture Status

V1, drafted, self-reviewed, **not yet approved**.

## Findings

One substantive finding, corrected before this package was produced (not
left for a later round):

Requirement §9 asserted the Decision-to-Evidence reference "shall use a
foreign key to Q-011's evidence table... `ON DELETE RESTRICT`." Checking
the actual, already-committed Q-011 migration
(`V4__create_evidence_provenance_foundation.sql`) shows this is factually
wrong about the established precedent: Q-011's own `evidence_record
.subject_ref` column has **no** real SQL foreign key to Q-010's
`trading_account_reference` table — cross-module validity is enforced by
a live application-layer service call, not a database FK, so each
module's schema stays independently ownable. The Requirement's aside was
a plausible-sounding guess made before it was checked against the real
schema; the Requirement itself correctly deferred "exact join-table
shape" to this Architecture stage (§14), so resolving it here is exactly
what that deferral was for, not an unauthorized reopening of a Requirement
decision. Both documents have been updated: the Architecture states the
corrected pattern with the actual evidence cited, and the Requirement's
§9 line now points to the Architecture's correction rather than repeating
the wrong claim — logged as a correction, not silently overwritten, per
this repository's established practice for governance-document drift.

No other inconsistency found. In particular:
- The recommendation for a new ADR-014 (§2/§23 of the Architecture) is
  consistent with the precedent that every prior Requirement introducing
  a real domain-module provider (Q-009, Q-010, Q-011) received its own
  ADR.
- The concurrency/failure-model sections correctly identify that Decision
  has an entire class of scenario Q-011 needed (concurrent corrections)
  that Q-012 does not, because Decision has no correction path — this is
  a genuine simplification traced back to the Requirement's own §5.2/
  Q012-FR-006, not an omission.
- Table count (`decision_record`, `decision_evidence_reference`,
  `decision_operation`, `decision_access_log` — four tables) was
  double-checked against the reasoning for why no
  `decision_operation_history`-equivalent table is needed (§7 of the
  Architecture): that table exists in Q-011 specifically to record
  correction history, and Decision has nothing to have correction history
  of.

## Remaining Risks

- §23's three "Required Architecture Review Answers" are explicit
  recommendations, not decisions Claude Code can make unilaterally
  (particularly whether a new ADR-014 is warranted) — the Product Owner's
  Gate Decision should address them, even if only by confirming the
  recommended answer.
- Exact migration version number, `ResultCode` values, and package/class
  names are deliberately left to Implementation Design (§21), consistent
  with keeping Architecture at the boundary/schema-shape level.

## Out-of-Scope Issues

None beyond what §18/§19 of the Architecture already scope out (Action,
ActionOutcome, and any Q-008 code remain untouched and unreserved).

## Recommendation

Present to the Product Owner for a Gate Decision, including the three
explicit questions in Architecture §23.

## Gate Decision

**PASS** (self-review only — the Product Owner's Gate Decision remains
outstanding).
