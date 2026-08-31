# Q-012 Implementation Design V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12.

## Task ID

Q-012 — Decision Provenance Foundation.

## Stage

Implementation Design. Per protocol §3, work stops here pending the
Product Owner's Gate Decision — implementation does not begin
automatically even if this Design is approved.

## Scope Reviewed

`docs/architecture/q-012-decision-provenance-foundation-implementation-design.md`
V1, checked for: consistency with approved Requirement V1/Architecture
V1/accepted ADR-014; internal consistency (the §11.1 canonical execution
order is the single authoritative statement, §8.5 is the single
authoritative constraint-to-test list, matching the discipline Q-011's
own governance history established); and completeness against every
Requirement `Q012-FR-XXX`.

## Files Inspected

- `docs/requirements/Q-012-Decision-Provenance-Foundation.md` (all 9
  Functional Requirements individually cross-checked against §19's
  traceability table).
- `docs/architecture/q-012-decision-provenance-foundation-architecture.md`
  V1 (every architecture decision in its §2 summary table traced into a
  concrete Design section).
- `docs/adr/ADR-014-decision-provenance-foundation.md` (Accepted).
- `docs/architecture/q-011-evidence-provenance-foundation-implementation-design.md`
  §8/§11/§16 (structural template and the source of the "single
  authoritative statement" discipline this document follows).
- `backend/src/main/resources/db/migration/V3__*.sql`/`V4__*.sql` (column
  type/length precedent: `CHAR(39)` for `ta-`/`ev-` refs, `CHAR(36)` for
  actor refs, `DATETIME(6)`, ASCII `ascii_bin` collation, `VARBINARY` for
  free text) — used to derive `CHAR(40)` for `dec-` refs (one longer
  prefix character) rather than copying `CHAR(39)` by habit.

## Verification Executed

Not applicable — no code or migration exists yet.
`GitStatus.txt`/`GitDiffStat.txt` in this package confirm the only
working-tree changes are the four Q-012 documents.

## Requirement Status

Q-012 Requirement V1 — APPROVED (unchanged by this Design).

## Architecture Status

Q-012 Architecture V1 — APPROVED; ADR-014 — ACCEPTED (both unchanged by
this Design).

## Design Compliance Status

All architecture decisions in Architecture V1 §2 are traced into a
concrete Design section (schema, HTTP contract, execution order, test
plan). No deviation found; no new architectural decision was introduced
at this stage (verified: §20 "Design Gaps and Outstanding Decisions"
lists none requiring escalation beyond the already-flagged
implementation-time migration version number).

## Test Status

Not applicable — no tests exist yet. §16 defines the required test
coverage; §8.5 defines the exact constraint-to-test mapping every
mandatory persistence test must prove.

## Findings

Caught and corrected during self-review (not left for a later round):

1. Reference-column length: initially defaulted to copying `CHAR(39)`
   from `ta-`/`ev-` precedent without checking that `dec-` is one
   character longer than `ta-`/`ev-` (4 vs. 3 characters). Corrected to
   `CHAR(40)` for `decision_ref` before this package was produced,
   verified against the actual prefix-length arithmetic
   (`len("dec-") + 36 = 40`), not copied by habit from a table that used
   a different prefix.

No other inconsistency found. In particular, cross-checked:
- §11.1's execution order matches the exact discipline established by
  Q-011's own multi-round governance history (replay check strictly
  before content validation and before any external call; `HUMAN` check
  strictly before the replay check).
- §8's table count (four) and the explicit absence of a `*_history`
  table were verified against ADR-014's "Alternative 5" reasoning, not
  merely asserted.
- Every one of Requirement's nine `Q012-FR-XXX` numbers was checked to
  exist verbatim in the approved Requirement before being cited in §19's
  traceability table (avoiding the kind of stale/incorrect section
  citation caught and fixed once already in this same Task, at the
  Architecture stage).

## Remaining Risks

- The exact next Flyway migration version number is deliberately left
  unresolved (§17), to be confirmed against the actual committed state at
  implementation time — this is correct discipline, not a gap, but is
  flagged here so an implementer does not mistake the absence of a
  specific number for an oversight.

## Out-of-Scope Issues

None beyond what Requirement §5.2 and Architecture §18/§19 already scope
out.

## Recommendation

Present to the Product Owner for a Gate Decision. If approved,
implementation still requires a separate, explicit authorization per
protocol §2/§3 — this Design's approval alone does not grant it.

## Gate Decision

**PASS** (self-review only — the Product Owner's Gate Decision remains
outstanding).
