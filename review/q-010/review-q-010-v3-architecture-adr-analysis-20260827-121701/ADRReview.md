# Q-010 ADR-012 Review

## Status

- ADR number: ADR-012
- Number availability: PASS — ADR-011 was latest and no competing ADR existed
- ADR status: PROPOSED — AWAITING EXTERNAL ARCHITECT REVIEW
- Accepted: NO
- Architect approval date: NOT RECORDED

## ADR Threshold

ADR-012 is required because Q-010 creates a durable business identity,
application-owned source-of-truth boundary, immutable external mapping,
lifecycle/history model, cross-capability read contract, Q-009 security
integration, and MySQL consistency decision. These outlive a single class or
schema and meet the repository ADR threshold.

## Content Review

ADR-012 includes Context, Decision, Alternatives, Consequences, Security, Data/
Integrity, Operations, Dependencies, Deferred Decisions, and an explicit
approval boundary. It records the durable choices without copying all
Implementation Design mechanics.

Meaningful alternatives evaluated include raw vendor IDs, direct external
lookup, a full master-data module, observation-driven auto-registration,
public HTTP provisioning, alias/migration mapping, DB IDs as business refs,
cache/event-only authority, mutable mapping, and a separate microservice.

## Decision

ADR-012 is complete enough for external review but remains Proposed. Codex has
not marked it Accepted or fabricated Architect approval.
