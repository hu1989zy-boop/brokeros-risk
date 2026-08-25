# Q-008 Requirement Review V1 Outstanding Items

## Blocking Before Requirement / ADR Approval

1. Confirm that a new Risk Case must reference an existing Decision and its
   attributable Evidence, or provide another ADR-009-compatible intake rule.
2. Confirm `TRADING_ACCOUNT` as the sole initial primary subject type.
3. Approve or revise the six lifecycle states, transition table, and
   exceptional `CLOSED → IN_REVIEW` reopen rule.
4. Approve case-owned operational priority without mutable duplicate case
   severity/risk level, and define the finite priority codes.
5. Select the opaque distributed-safe case-number format and decide whether a
   date display segment is wanted.
6. Confirm whether one Decision may be associated with multiple Risk Cases.
7. Approve independent Audit ownership and atomic case/audit durability inside
   the Phase 1 modular monolith.
8. Define minimum access, sensitive-content, retention, and exceptional
   redaction rules before investigation text/evidence metadata is persisted.
9. Explicitly accept, revise, or reject Draft ADR-010. Its existence is not
   acceptance.

## Known Design Risks

- A case-before-decision intake can silently make Risk Case the Core Domain.
- An unbounded aggregate can couple and load all evidence, decisions, actions,
  notes, and audit history.
- Case state may be confused with Action execution state.
- Reopen can erase earlier resolution/closure meaning without append-only
  cycles.
- Duplicate severity/risk level creates competing Decision/Case truth.
- Sequential numbers leak volume and cause allocation contention.
- Separate non-atomic audit persistence can leave unaudited critical changes.
- Generic subject or `OTHER` types can become an ungoverned integration
  dumping ground.
- Comments/evidence references may contain sensitive or regulated information.

## Deferred Work

- Risk Case Java/domain/persistence/application/API implementation;
- package/module mapping and cross-module contract mechanics;
- schema, Flyway migration, indexes, query and pagination design;
- ResultCodes, endpoints, DTOs, OpenAPI, authorization and RBAC;
- Rule Engine, Alert and Rule Hit runtime behavior;
- Evidence and Decision Core Domain implementation;
- Action execution, Account Control, SDKs, adapters and outcome handling;
- Kafka events/topics, Redis keys/cache, notifications and UI;
- search, reports, SLA/escalation, merge/split, deduplication and bulk work;
- retention, purge, legal hold, redaction workflow and sensitive-data access;
- Skill and Lessons Learned updates based on actual approved work.

Deferred items are not authorized by this Requirement Draft.

## Implementation Gate

Implementation Allowed: **NO**

Reason: Q-008 is Draft, ADR-010 is Draft, and the blocking design questions
above require explicit Architect decisions. No `RiskCase` Java type, entity,
repository, service, controller, DTO, migration, API, ResultCode, Kafka event,
Redis key, Account Control port, or adapter may be created.

## Architect Review Recommendation

Review the nine blocking items as one coherent boundary decision. If the
Architect approves Q-008 and ADR-010 unchanged, the following prompt may be
used to record approval only. If any item changes, the Architect must provide a
replacement prompt containing the exact revised decisions.

====================================
Codex Prompt
====================================

Record the Architect's approval of the existing Q-008 Risk Case Foundation
Requirement Draft and ADR-010 Draft without starting implementation.

The Architect has explicitly approved all proposals and Open Questions in:

- `docs/requirements/Q-008-Requirement.md`;
- `docs/adr/ADR-010-risk-case-foundation.md`;
- `review/q-008/review-v1-requirement/`.

Required work:

1. Read `AGENTS.md`, Q-007 Requirement, ADR-009, Q-007 architecture and Skill,
   Q-008 Requirement, ADR-010, all applicable accepted ADRs, development
   standards, and the Q-008 Requirement Review Package.
2. Confirm the approved decisions are internally consistent and unchanged from
   Requirement Review V1. If they differ, stop and request the Architect's
   exact replacement decisions.
3. Change Q-008 Requirement status from Draft to Architect Approved Design
   Baseline and record that implementation remains NOT GRANTED.
4. Change ADR-010 from Draft to Accepted only because this prompt represents
   explicit Architect acceptance of the unchanged proposal.
5. Create or update only the Q-008 architecture/approval documentation and a
   new, separate approval Review Package; do not overwrite
   `review/q-008/review-v1-requirement/`.
6. Preserve all Q-007 and `review/review-history/` artifacts exactly as found.
7. Run bounded documentation, whitespace, canonical-model, ADR-status, scope,
   and Git checks. Record runtime checks as NOT APPLICABLE because no code or
   runtime artifact changes.
8. Do not create Java, tests, entities, repositories, services, controllers,
   DTOs, ResultCodes, APIs, Flyway migrations, database objects, Kafka
   topics/events, Redis keys, adapters, UI, or other business implementation.
9. Do not commit or push unless separately authorized.
10. Return approval status, ADR status, files changed, verification, Git status,
    and remaining implementation gate, then stop.
