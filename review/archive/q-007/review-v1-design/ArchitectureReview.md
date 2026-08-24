# Q-007 Review V1 Design Architecture Review — Historical

## Review Status

DESIGN REVIEW READY — ARCHITECT APPROVAL REQUIRED

This status means the Design V1 package is complete enough for architecture
review. It is not implementation approval and is not a completed Requirement
PASS.

## Architecture Assessment

Q-007 preserves the approved Phase 1 feature-first modular monolith and defines
only conceptual domain boundaries. It does not map bounded contexts to Java
packages, Spring modules, databases, APIs, Kafka topics, Redis keys, services,
or deployables.

The proposed core domain is **Evidence-Based Risk Assessment and Decisioning**.
This positions BrokerOS Risk around explainable risk reasoning rather than a
case-management artifact. The canonical dependency remains:

```text
Trading Data → Evidence → Rule → Decision → Action → Risk Case
```

Risk Case is optional and downstream. Decision creation remains separate from
Action intent and future external Action Execution. These choices are aligned
with adapter isolation, broker neutrality, auditable critical decisions, and
the existing separation of risk detection from action execution.

## Proposed Context Boundaries

| Context | Classification | Architectural responsibility | Explicit exclusion |
| --- | --- | --- | --- |
| Trading Data | Supporting upstream | Broker-neutral observations and source translation | Risk conclusions, Actions, cases, external control |
| Risk Assessment | Core | Evidence derivation, versioned Rule evaluation, explainable Decision | Vendor protocols, execution, case workflow |
| Risk Action | Supporting downstream | Action intent and future execution boundary | Evidence derivation and Decision creation |
| Risk Case | Optional downstream | Association and collaboration boundary | Ownership of Evidence, Rules, Decisions, or execution |

The contexts are deliberately cohesive: Q-007 does not create one context per
noun, nor does it imply microservices. Exact implementation boundaries remain
deferred to approved capability Requirements.

## Architecture Principles Impact

- No approved architecture principle changes.
- Broker, CRM, MT4, MT5, and vendor concepts remain outside the core language.
- External observations enter only through adapters; no external database
  access pattern is introduced.
- Detection/decisioning and action execution remain separate.
- Provenance is a conceptual invariant for future auditability, but no Audit
  module or audit storage is designed.
- No dependency, technology, topology, package, or repository change is
  proposed.

## ADR Assessment

An ADR is required if Design V1 is approved. Core-domain definition, ubiquitous
language, context boundaries, Decision/Action Execution separation, and the
downstream position of Risk Case are durable architecture decisions that will
govern later Requirements.

Q-007 proposes `ADR-009-brokeros-core-domain-and-context-boundaries.md`. It has
not been created or marked Accepted because Architect approval has not yet been
granted. Creating an Accepted ADR during this Design Only review would falsely
record an undecided architecture decision.

## Technical Debt and Design Risks

- Numeric conceptual multiplicities may be mistaken for persistence
  cardinalities. Architect confirmation is required before they become part of
  an accepted decision.
- `Rule Evaluation` is a needed domain term but can be misread as authorization
  for a Rule Engine. Every document explicitly rejects that interpretation.
- `Action` represents intent, not execution success; later Requirements must
  preserve that distinction.
- A future Risk Case capability could attempt to own upstream Evidence or
  Decisions. The proposed dependency direction prohibits that coupling.
- Detailed decision outcomes, evidence quality semantics, action authorization,
  and case lifecycle remain intentionally unspecified rather than guessed.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the repository-wide `AGENTS.md`, including product boundaries,
requirements discipline, modular-monolith rules, review-package requirements,
Definition of Done, and Prompt Delivery Policy. Q-007 has a stable Requirement
ID, remains broker/platform neutral, creates no implementation, and provides a
dedicated bounded Review Package. The follow-up recommendation in
`OutstandingItems.md` ends with a directly executable Codex Prompt.

### Architecture compliance

Inspected applicable architecture documents through Q-006 and the current
Phase 1 constraints. The proposed contexts remain logical boundaries inside
one modular monolith. No horizontal package restructuring, microservice split,
external database coupling, technology addition, or deployment-topology change
is present. External integration remains adapter-only.

### ADR compliance

Inspected accepted ADRs ADR-001 through ADR-008. Q-007 does not contradict the
technology roadmap, isolation boundary, foundation standards, deployment
layout, development standards, CI strategy, tracing strategy, or configuration
strategy. A future ADR-009 is identified because the proposed domain boundary
is durable, but no unapproved ADR has been created or accepted.

### API standard compliance

Inspected the Q-007 scope and working-tree paths. No controller, endpoint,
DTO, `ApiResponse`, ResultCode, OpenAPI document, or API version is added or
changed. Therefore the unified API and validation standards remain unchanged;
Q-007 creates no application API requiring enforcement.

### Database standard compliance

Inspected the Q-007 scope and repository path changes. No entity, repository,
SQL, Flyway migration, database object, persistence cardinality, Redis key, or
Kafka topic/event is created. Diagram multiplicities are expressly conceptual
and not a schema contract. Existing Flyway ownership and database rules remain
untouched.

### Security standard compliance

Inspected all Q-007 Requirement, architecture, and review documents for
credentials, environment values, authentication material, personal data, and
vendor-specific sensitive data. None is present. The design does not authorize
new external access, secret handling, logging, endpoints, or data exposure.
Future identifiers and authorization rules are explicitly deferred.

### Auditability compliance

Inspected the domain-object relationships and lifecycle. Evidence source,
observation context, exact Rule version, Decision rationale, originating
Decision, and execution separation are preserved as conceptual provenance.
This supports future auditability without inventing the prohibited Audit
module, audit schema, or audit event. Actor, authorization, retention, and
before/after storage contracts remain deferred to formal Requirements.

### Skill compliance

Inspected `docs/skills/development-standards.md` and applicable existing
foundation skills before drafting Q-007. This phase introduces no implemented
pattern or verified operational technique, so creating a new skill would be
speculative. Skill and Lessons Learned evaluation must be repeated after an
approved implementation or after a real design correction produces reusable
knowledge.

## Review Conclusion

No standards violation was found in the Q-007 Design V1 scope. The design is
ready for Architect review, but it cannot be marked as an implemented
Requirement PASS. Architect approval of the listed domain decisions and the
subsequent ADR-009 action is required before any later implementation phase.
