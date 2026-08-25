# Q-008 Final Architecture Approval V3 Outstanding Items

## Architecture Gate Outstanding Items

None. The explicit external Architect decision approved Q-008 Requirement,
Architecture, ADR-010 acceptance, and the following deferrals.

## Deferred Decisions

1. **CaseNumber concrete algorithm — Implementation Design**
   - Keep globally unique, immutable, opaque, non-volume-leaking semantics.
   - Keep the `CaseNumber` Value Object and `CaseNumberGenerator` contract.
   - Do not yet select UUID, ULID, Snowflake, sequence, or another algorithm.
2. **Resolution History relational layout — Implementation Design**
   - Immutable case-owned Resolution History and ordered Resolution Cycles are
     approved.
   - Exact tables/entities/constraints/indexes are not approved here.
3. **Related-case Decision association — future Requirement/design**
   - One Decision may have at most one Primary Risk Case association.
   - Related/cross-case association remains deferred.
4. **Team ownership — future Requirement/design**
   - Individual `assignee`, `assignedBy`, and `assignedAt` are approved.
   - Team ownership and queue semantics remain deferred; no IAM/RBAC is created.
5. **Detailed sensitive-content policy — future security/compliance Requirement
   and Implementation Design**
   - Controlled access, auditable access/change, and no silent destructive
     deletion are mandatory.
   - Exact retention, detailed permissions, legal hold, exceptional redaction,
     and regulatory-retention implementation remain deferred.

These items do not block the Q-008 Foundation Architecture Gate. Applicable
implementation details must be resolved by the later approved phase before
code or schema is authorized.

## Process Improvement Candidate

Retain **Self-contained Architect Review ZIP convention** as a separate process
improvement candidate. This V3 ZIP follows that convention by explicit task
direction, but `AGENTS.md` is not changed. Any permanent Review Convention
change requires separate scope and approval.

## Implementation Gate

- Requirement: PASS / APPROVED
- Architecture: PASS / APPROVED
- ADR-010: ACCEPTED
- Implementation: NOT STARTED
- Implementation Allowed: NO
- Ready for Implementation Design: YES

## Next Authorized Phase

Only Q-008 Implementation Design / Design Review may begin next, under a
separate prompt. The following prompt authorizes design documentation only and
explicitly forbids business implementation.

====================================
Codex Prompt
====================================

Start Q-008 Risk Case Foundation Implementation Design and Design Review only.

1. Read `AGENTS.md`, approved Q-008 Requirement, Accepted ADR-009 and ADR-010,
   Q-007 architecture/Skill/Lessons, development standards, and complete Q-008
   V1/V2/V3 Reviews.
2. Preserve the approved aggregate, intake, subject, lifecycle, resolution,
   Evidence, Decision, Action, priority, assignment, audit, CaseNumber, and
   sensitive-content boundaries without reopening Architecture Discovery.
3. Design the smallest Phase 1 modular-monolith implementation shape, including
   domain operations/invariants, application use cases, persistence model,
   transaction boundary, API contracts, security boundary, concurrency, and
   verification strategy.
4. Resolve only the implementation-design deferrals applicable to Q-008,
   including the CaseNumber algorithm and Resolution History relational layout.
   Do not pull future related-case, team/queue, or detailed compliance policy
   into scope without a new explicit decision.
5. Create a dedicated Implementation Design Review Package and self-contained
   timestamped ZIP while preserving all historical Reviews.
6. Do not create Java, tests, entities, repositories, services, controllers,
   DTOs, mappers, Flyway migrations, APIs, Kafka events, Redis keys, frontend,
   or external integrations.
7. Do not commit or push unless separately authorized.
8. Report Design Gate status, decisions, deferred items, files, verification,
   ZIP manifest, Git status, and Implementation Allowed, then stop.
