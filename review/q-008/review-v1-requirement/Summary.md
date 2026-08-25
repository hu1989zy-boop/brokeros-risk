# Q-008 Requirement Review V1 Summary

## Review Status

| Review area | Status |
| --- | --- |
| Requirement Discovery | COMPLETE FOR DRAFT |
| Existing Capability / Gap Analysis | COMPLETE FOR DRAFT |
| Architecture Pre-check | READY FOR ARCHITECT REVIEW |
| Requirement | Draft — awaiting architect approval |
| ADR Required | YES |
| ADR-010 | Draft — not accepted |
| Implementation Allowed | NO |
| Ready for Architect Review | YES |

This is a documentation-only Requirement review. It does not represent
Requirement approval, ADR acceptance, design approval, or implementation
authorization.

## Review Identity

- Requirement: Q-008 — Risk Case Foundation
- Review iteration: `review-v1-requirement`
- Date: 2026-08-24
- Branch/baseline: `main` at `87f7553`, aligned with `origin/main`
- Governing architecture: ADR-009
- Proposed architecture: ADR-010 Draft

## Objective

Establish an Architect-reviewable Requirement for a minimal Risk Case business
model and lifecycle while preserving the accepted Q-007 chain:

```text
Evidence → Decision → Action → Risk Case
```

Decision remains the Core Domain. Risk Case remains optional and downstream.

## Existing Capability / Gap Analysis

### Already Exists

- ADR-009, Q-007 architecture, Requirement, Skill, and Lessons Learned provide
  authoritative Evidence/Decision/Action/Risk Case language.
- The Phase 1 Java/Spring Boot modular monolith, MySQL/Flyway, Redis, Kafka,
  Docker, Kubernetes, API/error/validation, tracing, logging, configuration,
  CI, and Review foundations already exist.
- Adapter isolation and Action/Execution separation are already mandatory.
- Development standards already require controlled named transitions, UTC,
  stable enums, separate business IDs, strong audit facts, security checks,
  and evidence-based review.

### Need Improvement

- Q-007 says Risk Case is downstream but does not define case-owned behavior.
- Subject, assignment, lifecycle, resolution/closure/reopen, comment, case
  number, priority, and concurrency rules are absent.
- Evidence/Decision/Action association history and correction/reassessment
  rules are unspecified.
- Strong audit obligations exist, but no case/audit consistency boundary has
  been selected.

### Need New Design

- Risk Case Aggregate Root and bounded ownership.
- Explicit six-state lifecycle and transition guards.
- Typed primary subject reference with a minimal initial type.
- Append-only association and resolution history.
- Minimal team/assignee references without IAM/RBAC.
- Case-owned priority distinct from Decision-owned risk assessment.
- Opaque public case-number contract.
- Independent audit ownership with atomic case/audit durability.

### Conflict / Blocker

No unavoidable standards conflict exists. The Architect must resolve whether a
case may open before an existing Decision. The Draft recommends requiring an
existing Decision and attributable Evidence so Risk Case does not become the
entry point contrary to ADR-009. Until that and the other Open Questions are
resolved, implementation is blocked.

## Files Created

- `docs/requirements/Q-008-Requirement.md`
- `docs/adr/ADR-010-risk-case-foundation.md`
- `review/q-008/review-v1-requirement/Summary.md`
- `review/q-008/review-v1-requirement/ArchitectureReview.md`
- `review/q-008/review-v1-requirement/Verification.md`
- `review/q-008/review-v1-requirement/OutstandingItems.md`
- `review/q-008/review-v1-requirement/GitStatus.txt`
- `review/q-008/review-v1-requirement/GitDiffStat.txt`
- `review/q-008/review-v1-requirement/ProjectTree.txt`

## Files Modified

None. Q-008 uses new documentation paths and does not overwrite the Q-007 root,
archive, standalone review, or review-history artifacts.

## Key Decisions Proposed

- `RiskCase` is the Aggregate Root for case-owned lifecycle invariants.
- It owns case state and association records, not Evidence, Decision, Action,
  execution, or an unbounded audit collection.
- Proposed states are `OPEN`, `IN_REVIEW`, `FOLLOW_UP_REQUIRED`, `RESOLVED`,
  `CLOSED`, and `CANCELLED`, with no arbitrary transitions.
- Resolution and closure remain distinct; resolved/closed cases may reopen only
  through an exceptional reasoned operation; cancelled cases are terminal.
- The initial primary subject type is proposed as `TRADING_ACCOUNT`; additional
  types require explicit semantics rather than a free-form `OTHER` framework.
- Evidence and Decision changes append/supersede/invalidate associations and
  preserve history.
- Action remains intent. `NO_ACTION` is a resolution/Decision outcome, and
  manual investigation is case follow-up rather than external execution.
- Assignment uses opaque team/actor references without creating IAM/RBAC.
- Risk Case owns operational priority only; severity/risk level remain
  Decision-owned until a future Core Domain decision differentiates them.
- Public case number is immutable and opaque, separate from `BIGINT id`, and
  should not expose daily volume through a sequence.
- Audit is independently owned and must be durably atomic with required case
  mutations inside the modular monolith.

## ADR Determination

**ADR Required: YES**

The proposal establishes a durable aggregate boundary, controlled lifecycle,
cross-context ownership rule, and audit consistency model. These are explicitly
within the repository ADR threshold and affect later persistence, API, and
module designs. ADR-010 has therefore been created as Draft only.

## Explicit Non-Changes

No Java source, test, entity, repository, service, controller, DTO, mapper,
ResultCode, API, schema, Flyway migration, Kafka topic/event, Redis key, adapter,
IAM, workflow engine, UI, CI, configuration, Docker, or Kubernetes change was
made.

No Q-007 review artifact was deleted, overwritten, staged, or added to Git by
this task. No commit or push was performed.

## Recommendation

The Draft is complete enough for Architect Requirement Review. Approval must
resolve the Open Questions in `OutstandingItems.md` and explicitly accept,
revise, or reject Draft ADR-010. Business implementation remains prohibited.
