# Q-007 Review V1 Design Summary — Historical

## Review Status

DESIGN ONLY — ARCHITECT REVIEW REQUIRED

## Requirement

- Q-007 — BrokerOS Domain Foundation
- Architecture phase: Phase 1
- Design iteration: V1
- Implementation authorization: NOT GRANTED
- Date: 2026-08-18

## Objective

Establish the BrokerOS Risk domain foundation using the canonical language:

```text
Trading Data → Evidence → Rule → Decision → Action → Risk Case
```

Risk Case is explicitly downstream and optional. The core domain is
evidence-based risk assessment and decisioning, not case management.

## Completed Design Work

- Drafted the Q-007 Requirement with implementation prohibitions.
- Defined the ubiquitous language and supporting terms.
- Defined the core domain and supporting capabilities.
- Proposed four logical bounded contexts inside the existing modular monolith.
- Defined conceptual domain-object relationships and provenance invariants.
- Defined the domain lifecycle without status enums or workflow.
- Created a context map with external adapter isolation and one-way dependency.
- Evaluated ADR need and proposed ADR-009 only after Architect approval.
- Generated this dedicated Review V1 Design Package.

## Files Created by Q-007 Design

- `docs/requirements/Q-007-Requirement.md`
- `docs/architecture/q-007-brokeros-domain-foundation-design.md`
- Files under `review/q-007/review-v1-design/`

## Files Modified by Q-007 Design

None outside the newly created Q-007 documents and dedicated Review directory.

## Files Deleted

None.

## Important Proposed Decisions

- Core domain: Evidence-Based Risk Assessment and Decisioning.
- Logical contexts: Trading Data, Risk Assessment, Risk Action, Risk Case.
- Evidence, Rule, Rule Evaluation, and Decision remain cohesive in the core
  Risk Assessment Context.
- Decision and Action intent are distinct; Action Execution is downstream.
- Risk Case references upstream objects and never becomes their owner.
- Bounded contexts are logical boundaries, not services, packages, databases,
  APIs, topics, or deployables.

## Explicit Non-Changes

No Java, test, Rule Engine, Workflow, Audit, Risk Case implementation, account
control, API, database, Flyway, Redis, Kafka, adapter, configuration, CI,
Docker, Kubernetes, dependency, package, or deployment change was made.

## Required Architect Decisions

1. Approve the core-domain definition.
2. Approve the four-context model.
3. Approve Rule Evaluation as a conceptual supporting term only.
4. Approve Decision → Action → Action Execution separation.
5. Approve optional downstream Risk Case semantics.
6. Decide whether conceptual multiplicities should remain in Design V1.
7. Approve ADR-009 creation before any later implementation Requirement.

Q-007 must remain Design Only after this review. No implementation or Q-007
Phase 2 work may begin without a new explicit prompt and approval.
