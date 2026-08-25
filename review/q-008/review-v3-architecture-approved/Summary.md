# Q-008 Final Architecture Approval V3 Summary

## Final Architecture Gate

| Gate | Status |
| --- | --- |
| Requirement | PASS / APPROVED |
| Architecture | PASS / APPROVED |
| ADR-010 | ACCEPTED |
| Approval source | Explicit external Architect Review decision dated 2026-08-24 |
| Implementation | NOT STARTED |
| Implementation Allowed | NO |
| Ready for Implementation Design | YES |

ADR-010 acceptance records the external Architect decision; it is not Codex
self-approval. Architecture Approval does not authorize implementation.

## Final Architect Decisions Recorded

1. `RiskCase` is the Risk Case capability Aggregate Root for case-owned
   invariants and state; it does not own Evidence lifecycle, Decision
   lifecycle, Action execution, or Audit.
2. Decision remains the BrokerOS Risk Core Domain under ADR-009.
3. Intake sources are `MANUAL` and `DECISION_DRIVEN`. Manual intake requires no
   existing Evidence or Decision and must not fabricate Evidence, Decision,
   Rule Hit, or Alert. Intake reason/summary is investigation context only.
4. Q-008 Foundation supports only `TRADING_ACCOUNT` as primary subject. Other
   subject types are deferred; no universal Entity framework is approved.
5. Lifecycle states are `OPEN`, `IN_REVIEW`, `ACTION_REQUIRED`, `RESOLVED`,
   `CLOSED`, and `CANCELLED`, with domain-invariant-controlled transitions.
6. `RESOLVED` is substantive resolution, `CLOSED` is administrative closure,
   and `CANCELLED` means invalid/duplicate/mistaken case rather than `NO_RISK`.
7. `CLOSED → IN_REVIEW` reopen requires reason, actor, UTC timestamp, Audit
   Record, and preservation of all prior history.
8. Resolution uses immutable case-owned Resolution History and ordered
   Resolution Cycles; one mutable resolution field may not replace history.
9. Risk Case stores Evidence references/association history only, preserving
   invalidation and supersession semantics without overwrite or deletion.
10. A case may reference multiple historical Decisions. One Decision may have
    at most one Primary Risk Case association in the Foundation.
11. Action intent remains distinct from Action execution. MT4/MT5, Bridge, LP,
    leverage, forced-close, restriction, and other execution stay in future
    Account Control/adapter capabilities.
12. Operational Priority is `LOW`, `NORMAL`, `HIGH`, or `CRITICAL`; risk
    severity/risk level remains Decision-owned.
13. Foundation Assignment is individual: `assignee`, `assignedBy`, and
    `assignedAt`, without IAM/RBAC or organization hierarchy.
14. Audit remains independently owned. Material case mutation and its required
    Audit Record must commit in the same application-owned database transaction;
    Event Sourcing, distributed transactions, 2PC, Saga, and Kafka-only audit
    durability are rejected.
15. CaseNumber is a globally unique, immutable, opaque business identifier
    separate from the database primary key, modeled through a `CaseNumber`
    Value Object and `CaseNumberGenerator` contract.
16. Sensitive investigation content requires controlled access, auditable
    access/change, and no silent destructive deletion.

## Explicitly Deferred Decisions

- CaseNumber concrete algorithm: Implementation Design.
- Resolution History relational layout: Implementation Design.
- Related/cross-case Decision association: future Requirement/design.
- Team ownership and queue semantics: future Requirement/design.
- Detailed sensitive-content retention/permissions/legal-hold/redaction/
  regulatory implementation: future security/compliance Requirement and
  Implementation Design.

These deferrals do not block the Q-008 Foundation Architecture Gate.

## Files Modified

- `docs/requirements/Q-008-Requirement.md`
- `docs/adr/ADR-010-risk-case-foundation.md`

## Files Created

- eight V3 Review files under
  `review/q-008/review-v3-architecture-approved/`;
- one timestamped, self-contained V3 ZIP under `review/q-008/`.

## Explicit Non-Changes

- ADR-009, Q-007, Q-008 V1, Q-008 V2, and `review/review-history/` unchanged.
- No Java, test, API, schema, migration, Kafka, Redis, adapter, frontend,
  dependency, configuration, CI, Docker, or Kubernetes change.
- No commit, push, reset, clean, staging, historical deletion, or Review
  overwrite.

## Conclusion

Q-008 is ready for a separate Implementation Design / Design Review phase.
Implementation Allowed: **NO**
