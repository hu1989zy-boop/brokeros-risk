# ADR-005: Durable Development Standards

- Status: Accepted
- Date: 2026-08-11

## Context

BrokerOS Risk is a financial trading risk system approaching formal business
development. Phase 0.5 established technical primitives, but future work also
needs one durable set of module, API, database, audit, messaging, cache,
security, delivery, and review rules. Chat-only instructions are not sufficiently
traceable, and inconsistent interpretation would create compatibility and
operational risk.

## Decision

Adopt the Phase 0.6 development standards in
`docs/architecture/phase-0.6-development-standards.md` as mandatory long-term
constraints.

Before every Q-XXX, phase, bug fix, refactor, or technical task, contributors
must check:

- `AGENTS.md`;
- applicable architecture documents;
- accepted ADRs;
- `docs/skills/development-standards.md`.

Conflicts must be identified explicitly and resolved through an approved
Requirement plus the appropriate architecture/ADR change. Existing standards
cannot be bypassed silently.

Every Review Package becomes a compliance gate. Its Architecture Review must
provide evidence for AGENTS, architecture, ADR, API, database, security,
auditability, and skill compliance. Unresolved violations prevent a PASS result.

These rules preserve the modular monolith and do not create business modules,
business schema, integrations, topics, or new runtime frameworks.

## Alternatives

### Keep standards only in prompts or chat history

Rejected because future agents and reviewers would lack a stable repository
source and could not reliably trace changes.

### Introduce enforcement frameworks immediately

Adding ArchUnit, Checkstyle, custom Maven plugins, code generators, or policy
engines now was rejected under YAGNI. The current codebase is too small to
justify more dependencies and maintenance. Concrete automated checks can be
added later through an approved engineering Requirement when repeated defects
demonstrate value.

### Postpone standards until business modules exist

Rejected because API, database, money/time, audit, messaging, and module choices
become expensive to correct after external contracts and persisted data exist.

## Consequences

- Future work has a stable preflight checklist and an evidence-based review
  gate.
- Conflicting requests may require clarification or a new architecture decision
  before implementation, increasing deliberate review time.
- Documentation, skills, lessons, and Review Packages become required parts of
  completion.
- Standards still rely partly on disciplined review; targeted automation remains
  possible when justified by observed failures.
- Existing accepted Requirements and ADRs remain unchanged. Phase 0.6 does not
  silently migrate current symbolic ResultCode values or add business code.
