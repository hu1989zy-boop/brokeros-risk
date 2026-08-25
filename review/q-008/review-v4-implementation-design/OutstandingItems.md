# Q-008 Implementation Design Review V4 Outstanding Items

## Blocking Before Implementation Authorization

1. **External Architect Design approval**
   - Approve or revise the exact current Implementation Design.
   - Recording a design document and Review is not self-approval.
2. **Authoritative upstream reference providers**
   - Evidence, Decision, Action, Action outcome, and broker-neutral Trading
     Account reference queries need real owning-capability contracts/providers.
   - The Architect must decide sequencing or explicitly approve a reduced first
     implementation with dependent commands disabled.
   - Unchecked strings, fabricated Decisions, and fake production providers are
     prohibited.
3. **Authenticated Actor and authorization provider**
   - Controlled access and trustworthy audit actor identity need a real
     non-spoofable ActorContext/authorization boundary.
   - A caller-supplied header is not acceptable.
   - Detailed IAM/RBAC remains outside Q-008.

## Resolved Implementation Design Deferrals

- CaseNumber: `RC-<canonical-lowercase-UUIDv4>` selected.
- Resolution History: immutable normalized cycle/header/Evidence/Action
  snapshots plus case-version ordering selected.

## Remaining Deferred Scope

- related/cross-case Decision associations;
- team ownership, work queues, IAM/RBAC, hierarchy, SLA/escalation;
- MT4/MT5/Bridge/LP/Account Control execution;
- Rule Engine, Kafka, Redis, Flink, Python/ML, AI;
- additional subject types and universal Entity framework;
- detailed retention, legal hold, regulatory retention, and exceptional
  redaction workflow;
- search/reporting/dashboard, notification, merge/split, and bulk operations.

## Process Improvement Candidate

Keep self-contained Architect/Design Review ZIP contents as a separate Review
Convention candidate. V4 follows explicit task direction; `AGENTS.md` is not
modified.

## Design Review Recommendation

The Architect should review the formal Design and explicitly decide all three
blockers above. If approved unchanged and the dependency/security sequencing is
explicit, use the following approval-recording prompt. If any design decision
changes, the Architect must provide replacement text.

====================================
Codex Prompt
====================================

Record external Architect approval of Q-008 Implementation Design V4 without
starting implementation.

The Architect has explicitly reviewed and approved the exact current contents
of:

- `docs/requirements/Q-008-Requirement.md`;
- `docs/adr/ADR-010-risk-case-foundation.md`;
- `docs/architecture/q-008-risk-case-foundation-implementation-design.md`;
- `review/q-008/review-v4-implementation-design/`.

Before changing any status, require the Architect decision to state:

1. whether upstream Evidence/Decision/Action/outcome providers will be delivered
   before Q-008 or dependent Q-008 commands must remain disabled;
2. which approved authenticated Actor/authorization provider protects Q-008
   HTTP access; and
3. that UUIDv4 CaseNumber, normalized immutable Resolution History, Spring JDBC
   persistence, optimistic locking, API, transaction/audit, and security design
   are approved unchanged or provide exact replacement decisions.

Required work:

1. Read repository governance, approved Q-008/ADR-009/ADR-010, formal Design,
   and V1–V4 Reviews.
2. If any of the three decisions above is absent, stop without approving the
   Design Gate or changing Implementation Allowed.
3. Record only the explicit external Design approval and blocker resolutions;
   do not infer or self-approve anything.
4. Keep `Implementation Allowed: NO` unless the Architect separately grants a
   complete implementation authorization after all blockers are resolved.
5. Preserve Q-007, Q-008 V1–V4, all historical ZIPs, and review-history.
6. Create an independent approval Review version and self-contained timestamped
   ZIP; never overwrite historical packages.
7. Run bounded documentation, architecture, ADR, security, scope, whitespace,
   secret, ZIP, and Git checks.
8. Do not create Java, tests, DTOs, controllers, services, repositories,
   entities, migrations, SQL, APIs, topics, Redis keys, adapters, frontend, or
   deployment changes.
9. Do not commit or push unless separately authorized.
10. Report Design Gate, blocker decisions, files, verification, ZIP manifest,
    Git status, and Implementation Allowed, then stop.
