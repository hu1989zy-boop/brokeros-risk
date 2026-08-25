# Q-008 Architect Review V2 Summary

## Review Status

| Review area | Status |
| --- | --- |
| Requirement | Revised — awaiting architect approval |
| Architecture | Revised — awaiting architect approval |
| ADR Required | YES |
| ADR-010 | Draft — awaiting architect approval |
| Implementation | NOT STARTED |
| Implementation Allowed | NO |
| Ready for Architect Review | YES |

This package applies the formal Architect Review V2 direction. It is not final
Architect Approval and does not authorize implementation.

## Objective

Revise the Q-008 Risk Case Foundation intake and lifecycle model while
preserving the Q-007 ownership baseline:

```text
Evidence → Decision → Action → Risk Case
```

ADR-009 remains the authority for domain ownership. Q-008 V2 adds valid manual
investigation chronology without moving Evidence/Decision/Action ownership into
Risk Case.

## Architect Directions Applied

1. Confirmed `RiskCase` as Aggregate Root for case-owned identity, subject,
   intake, lifecycle, assignment, priority, references, resolution cycles, and
   invariants only.
2. Replaced the V1 Decision-only creation prerequisite with two first-class
   sources: `MANUAL` and `DECISION_DRIVEN`.
3. Defined manual intake minimum validity: case number, `TRADING_ACCOUNT`
   subject, intake source, reason/summary, creator, UTC creation time, priority,
   and initial `OPEN` status.
4. Clarified that manual reason/summary is investigation context, not Evidence,
   Decision, Rule Hit, or Alert.
5. Adopted `OPEN`, `IN_REVIEW`, `ACTION_REQUIRED`, `RESOLVED`, `CLOSED`, and
   terminal `CANCELLED`, with named legal transitions only.
6. Kept `RESOLVED` distinct from administrative `CLOSED`, and `CANCELLED`
   distinct from a `NO_RISK` resolution.
7. Defined strict `CLOSED → IN_REVIEW` reopening with reason, actor, UTC time,
   Audit Record, and immutable history.
8. Added ordered Resolution Cycles and recommended immutable case-owned
   Resolution History records rather than a mutable resolution value or Event
   Sourcing.
9. Kept Evidence content upstream; Risk Case stores append-only references and
   explicit superseded/invalidated/withdrawn association history.
10. Kept Decision in the Core Domain, preserved multiple Decision references,
    and proposed one primary-case association per Decision while deferring
    related-case associations.
11. Kept Action intent separate from execution and allowed only Action/outcome
    references in the case.
12. Defined case Priority codes `LOW`, `NORMAL`, `HIGH`, and `CRITICAL`; Risk
    Severity/Risk Level remain Decision-owned.
13. Limited Assignment to assignee/assigned-at/assigned-by and reserved team
    ownership without creating IAM/RBAC.
14. Kept Audit independently owned and append-only; required case mutation and
    Audit Record share one application-owned database transaction, with no 2PC,
    Saga, distributed transaction, or Event Sourcing.
15. Defined CaseNumber Value Object/generator responsibility while deferring
    the concrete opaque generation algorithm.

## Files Modified

- `docs/requirements/Q-008-Requirement.md`
- `docs/adr/ADR-010-risk-case-foundation.md`

## Files Created

- all files under `review/q-008/review-v2-architecture/`;
- one timestamped self-contained V2 ZIP under `review/q-008/`.

## Explicit Non-Changes

- Q-007 Requirement, ADR-009, Q-007 architecture, Skill, and Reviews unchanged.
- Q-008 V1 Review directory and V1 ZIP preserved unchanged.
- `review/review-history/` preserved and uninspected.
- No Java, test, API, schema, migration, Kafka, Redis, adapter, UI, dependency,
  configuration, CI, Docker, or Kubernetes change.
- No commit, push, reset, clean, staging, or historical deletion.

## Conclusion

The revised sources are ready for final Architect review. ADR-010 remains Draft
and the Requirement/Architecture remain revised rather than approved.

Implementation Allowed: **NO**
