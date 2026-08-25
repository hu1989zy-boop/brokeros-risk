# Q-008 Implementation Design Review V4 Summary

## Design Gate

| Gate | Status |
| --- | --- |
| Requirement | PASS / APPROVED |
| Architecture | PASS / APPROVED |
| ADR-010 | ACCEPTED |
| Implementation Design Artifact | COMPLETE |
| Architect Design Review | NOT YET RECORDED |
| Design Gate | READY FOR ARCHITECT REVIEW — NOT APPROVED |
| Implementation | NOT STARTED |
| Implementation Allowed | NO |

## Implementation-design Decisions

1. Keep one Phase 1 backend modular monolith capability under
   `com.brokeros.risk.riskcase`; use existing Spring JDBC and MySQL without JPA
   or a new dependency.
2. Keep RiskCase as a bounded current-state Aggregate Root. Histories are
   append-only case-owned records queried separately, not an unbounded object
   graph.
3. Map every legal lifecycle transition to a named domain operation; expose no
   generic status setter or status-update endpoint.
4. Implement strict `CLOSED → IN_REVIEW` reopen with reason, authenticated
   actor, UTC time, required Audit Record, cycle increment, current Decision
   pointer clearing, and preservation of all earlier records.
5. Select `RC-<canonical-lowercase-UUIDv4>` for CaseNumber: 122 random bits,
   opaque, globally collision-resistant, non-sequential, and independent from
   `BIGINT id`.
6. Represent Resolution History using one immutable resolution header per
   `(case, cycle)`, normalized Evidence/Action reference snapshots, transition
   history, and case-version ordering.
7. Use root optimistic locking. Every material command reserves exactly one
   next `case_version`; concurrent losers roll back without history or audit.
8. Persist material case mutation, case-owned history, and independent Audit
   Record in one Spring transaction using the application-owned MySQL
   DataSource.
9. Use named application/API commands, `ApiResponse`, Bean Validation,
   action-specific DTOs, symbolic Q-008 ResultCodes, create idempotency key,
   and expectedVersion on existing-case commands.
10. Require authenticated ActorContext and authorization before reads/writes;
    do not invent roles, team hierarchy, or a caller-supplied actor header.
11. Define read-only upstream reference ports; never treat unchecked strings as
    proof that Evidence, Decision, Action, or outcome objects exist.
12. Add no Kafka event/topic, Redis key, external execution, workflow engine,
    Event Sourcing, distributed transaction, 2PC, or Saga.

## Persistence Design

The design proposes current root, transition, assignment, priority, Evidence,
Decision, Action, Resolution, resolution snapshot, note, and independent Audit
tables. Internal keys are `BIGINT`; timestamps are UTC `DATETIME(6)`; enums are
readable codes; case-owned FKs restrict deletion. No SQL or migration is
created in this phase.

## Implementation-authorization Blockers

1. External Architect Design approval is not yet recorded.
2. Real Evidence/Decision/Action/outcome reference providers do not exist in
   the repository; unchecked or fabricated substitutes are prohibited.
3. Authenticated ActorContext and authorization provider do not exist; HTTP
   exposure cannot use a spoofable actor header.

## Remaining Deferred Scope

- related/cross-case Decision association;
- team ownership, work queues, IAM/RBAC, and organization hierarchy;
- MT4/MT5/Bridge/LP/account-control execution;
- Rule Engine, Kafka, Redis, Flink, Python/ML, and universal Entity framework;
- detailed retention, legal hold, regulatory retention, and exceptional
  redaction implementation;
- search/reporting, notification, bulk, merge/split, and SLA workflow.

## ADR Decision

No new ADR and no ADR-010 amendment are required. CaseNumber encoding and
Resolution History relational shape were explicitly delegated to
Implementation Design and do not change accepted architecture boundaries.

## Files

Modified:

- `docs/requirements/Q-008-Requirement.md` — Design Gate/deferred decision
  synchronization only.

Created:

- `docs/architecture/q-008-risk-case-foundation-implementation-design.md`;
- `docs/lessons/2026-08-25-q-008-risk-case-implementation-design.md`;
- eight V4 Review files;
- one self-contained timestamped V4 ZIP.

No reusable Skill change was justified because this phase establishes a
Q-008-specific design rather than a verified reusable implementation pattern.
The phase Lessons Learned honestly records the selected design, rejected
alternatives, missing providers, and future implementation risks. No business
implementation was created.
