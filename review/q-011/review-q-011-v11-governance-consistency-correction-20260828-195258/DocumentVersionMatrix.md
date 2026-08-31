# Q-011 V11 Document Version Matrix

| Document | Last Product-Owner-approved version | This round's candidate | Candidate status | What changed |
| --- | --- | --- | --- | --- |
| Requirement | V2 (2026-08-28) | V3 | DRAFT — pending approval | Goal 5 no longer overclaims `HUMAN` for every protected use case; scoped to record/correct only, matching `Q011-FR-005`. §14 item 1's resolution note updated to reflect the actual Architecture decision (accept two Q-010 outcomes, not narrow to one). New §19 records this correction. |
| Architecture | V3 (2026-08-28) | V4 | DRAFT — pending approval | §23 item 15 no longer lists inactive-subject Evidence as future scope (already brought in scope by §9/§22 in V3). §23 item 17 reworded from a point-in-time claim to an evergreen principle pointing at §24. No substantive decision changed. |
| ADR-013 | Accepted (original, 2026-08-28) | Amendment | PENDING RE-ACCEPTANCE | Subject validation, Consumer boundary, the "Extending Q-010" alternative, Costs and constraints, and Deferred Decisions all corrected to accept `RECOGNIZED_NOT_ELIGIBLE` (matching `Q011-FR-002`) instead of requiring `ELIGIBLE_FOR_NEW_ASSOCIATION` only. Consumer boundary further corrected to separate the Q-008 contract limitation from actor-type policy. Original acceptance history preserved verbatim; amendment explicitly not self-accepted. |
| Implementation Design | V4 (2026-08-28) | V5 | DRAFT — pending approval | §1.1's priority list no longer hard-codes version numbers for sibling documents (removed the exact defect class that caused this staleness twice). §20.9 no longer lists inactive-subject Evidence as future scope. §21's two contradictory "Next gate" statements collapsed into one accurate statement. New §20.10 records this round in full. |

## Approval Dependencies

None of the four candidates can be approved in isolation and still leave
the document set consistent — Requirement V3, Architecture V4, the ADR-013
amendment, and Implementation Design V5 all restate the same locked
decisions (see `DecisionMatrix.md`) and must be approved together, or the
set returns to an inconsistent state with only some documents corrected.

## Implementation Authorization

No implementation authorization exists for any of these four candidates.
The authorization most recently granted (2026-08-28, against Implementation
Design V4 and Architecture V3) does not carry forward automatically to V5/
V4/the ADR amendment/Requirement V3. A fresh, separate, explicit
authorization decision is required after all four approvals above, per the
same discipline applied in every prior round.
