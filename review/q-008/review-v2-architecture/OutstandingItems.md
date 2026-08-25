# Q-008 Architect Review V2 Outstanding Items

## Final Architect Decisions Required

1. Select or defer the concrete opaque CaseNumber generation strategy and any
   date/checksum display segment.
2. Approve the immutable case-owned Resolution History record/entity principle
   and defer or select its exact relational persistence layout.
3. Confirm that Q-008 implements one primary-case association per Decision and
   defers related-case associations.
4. Confirm that initial Assignment uses individual assignee only and defers
   team ownership/queue semantics.
5. Define or explicitly defer the minimum access, retention, and exceptional
   redaction policy for sensitive investigation notes/evidence metadata.
6. Explicitly accept, revise, or reject Draft ADR-010; revised text is not
   self-approval.

## Deferred Implementation

- all RiskCase Java/domain/entity/repository/service/controller/DTO/mapper code;
- Flyway schema, indexes, APIs, ResultCodes, OpenAPI, Kafka, Redis, and UI;
- Evidence/Decision/Action business implementation and Rule Engine;
- Account Control and all MT4/MT5/Bridge/LP/CRM/vendor execution;
- IAM/RBAC, team hierarchy, workflow engine, SLA/timer/scheduler;
- related-case Decision associations and multi-subject cases;
- search/reporting, notifications, bulk operations, merge/split, and retention
  enforcement.

## Process Improvement Candidate

The current repository Review Convention mandates Review files but does not
explicitly state that an **Architect Review ZIP must be self-contained and
include the Requirement, ADR, and any Architecture documents under review**.
Q-007 and Q-008 V1 ZIP evidence is Review-focused, while this V2 package now
includes formal source snapshots by explicit task direction.

Candidate follow-up: handle self-contained Architect Review ZIP contents as a
separate process Requirement/standards change. Do not modify `AGENTS.md` during
Q-008 Architecture Review, because product architecture and process convention
changes need separate Git scope and approval.

## Implementation Gate

- Requirement: Revised — awaiting architect approval
- Architecture: Revised — awaiting architect approval
- ADR-010: Draft — awaiting architect approval
- Implementation: NOT STARTED
- Implementation Allowed: NO

## Final Review Recommendation

The Architect should decide the six final items above. If the Architect accepts
the revised Q-008/ADR-010 unchanged and explicitly resolves/defers each item,
use the following approval-recording prompt. If any decision changes, the
Architect must provide a replacement prompt with exact revised language.

====================================
Codex Prompt
====================================

Record final Architect approval of Q-008 Risk Case Foundation V2 without
starting implementation.

The Architect has explicitly approved the exact current contents of:

- `docs/requirements/Q-008-Requirement.md`;
- `docs/adr/ADR-010-risk-case-foundation.md`;
- `review/q-008/review-v2-architecture/`.

Required work:

1. Read `AGENTS.md`, Q-007 Requirement, ADR-009, Q-007 Architecture and Skill,
   revised Q-008 Requirement, ADR-010 Draft, and the complete V2 Review.
2. Confirm the final Architect decisions for CaseNumber generation, Resolution
   History persistence, related-case Decision association, team ownership,
   sensitive-content policy, and ADR-010 disposition are explicit. If any is
   missing, stop without changing status.
3. Update only Q-008 Requirement/ADR/architecture/review approval records to
   reflect the exact final decisions. Mark ADR-010 Accepted only if the
   Architect explicitly accepted it.
4. Preserve Q-007, Q-008 V1, V2 review history, and `review/review-history/`.
5. Create a new approval Review version and self-contained timestamped ZIP;
   never overwrite V1 or V2.
6. Run bounded documentation, architecture, ADR, scope, whitespace, Git,
   secret, and ZIP manifest checks.
7. Do not create Java, entity, repository, service, controller, DTO, migration,
   API, ResultCode, Kafka, Redis, adapter, UI, or any business implementation.
8. Do not commit or push unless separately authorized.
9. Report final status, decisions, files, verification, ZIP manifest, Git
   status, and Implementation Gate, then stop.
