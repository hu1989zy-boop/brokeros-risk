# Q-010 V5 Implementation Design Review Summary

## Review Metadata

- Review ID: `Q-010-V5-IMPLEMENTATION-DESIGN-20260827-132854`
- Requirement: Q-010 — Trading Account Reference Authority Foundation
- Review Type: Implementation Design Review
- Review Package Version: V5
- Design document version: V1
- Review Status: **COMPLETE — READY FOR EXTERNAL ARCHITECT REVIEW**
- Design Status: **DRAFT — AWAITING EXTERNAL ARCHITECT APPROVAL**
- Implementation Design Complete: **YES — submission ready**
- Implementation Design Approved: **NO**
- Implementation: **NOT STARTED**
- Implementation Allowed: **NO**

V5 is the Review Package phase, not a new Requirement/Architecture/ADR version.
It preserves Q-010 Requirement V1, approved Architecture V1, and accepted
ADR-012.

## Outcome

The draft design translates the approved identity-authority boundary into an
implementation-ready plan for:

- one `com.brokeros.risk.tradingaccount` modular-monolith feature;
- exact opaque refs, namespace values, byte-preserving external keys, and
  immutable one-to-one mappings;
- four additive future MySQL tables with two-way database uniqueness,
  optimistic lifecycle versions, durable idempotency outcomes, and immutable
  history;
- strict one-operation non-Web provisioning with a semantic fingerprint;
- exact Q-009 capabilities and one purpose-specific SERVICE descriptor;
- a narrow Q-008 eligibility facade that discloses no external identity;
- safe ResultCodes, logs, metrics, transactions, concurrency outcomes, and
  mandatory real-MySQL verification; and
- a future Flyway V3 and implementation sequence that remain unexecuted.

## Files Created or Metadata-Updated

- `docs/architecture/q-010-trading-account-reference-authority-implementation-design.md`
- `docs/lessons/2026-08-27-q-010-trading-account-reference-authority-implementation-design.md`
- design-gate metadata only in the approved Q-010 Requirement, Architecture,
  and ADR-012 documents; and
- this new V5 Review directory plus its independent transfer ZIP.

No Java, test, SQL, Flyway migration, REST endpoint, dependency, application
configuration, deployment configuration, Q-008 implementation, or unrelated
historical artifact was changed. No Git staging, commit, or push was performed.

## Gate

| Gate | Result |
| --- | --- |
| Requirement V1 | APPROVED |
| Architecture V1 | APPROVED |
| ADR-012 | ACCEPTED |
| Implementation Design V1 | DRAFT / AWAITING EXTERNAL ARCHITECT APPROVAL |
| Architecture gap | NONE |
| Implementation | NOT STARTED |
| Implementation Allowed | NO |

Next action: independent Architect review of the Q-010 Implementation Design
V1. Approval and implementation authorization must remain separate decisions.
