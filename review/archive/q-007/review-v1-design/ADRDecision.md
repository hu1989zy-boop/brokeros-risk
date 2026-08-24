# Q-007 ADR Decision — Design V1 Historical Review

## Decision

New ADR required after Architect approval: YES.

Proposed identifier and subject:

`ADR-009 — BrokerOS Core Domain and Context Boundaries`

## Why the ADR Threshold Is Met

The design establishes durable cross-module language and boundaries:

- Evidence-Based Risk Assessment and Decisioning as core domain;
- Trading Data → Evidence → Rule → Decision → Action → Risk Case;
- Risk Case as optional downstream rather than entry point;
- four logical bounded contexts;
- Decision/Action/Action Execution separation;
- adapter-only external integration and no case-driven dependency cycle.

These choices will govern future Requirements, packages, APIs, data ownership,
events, adapters, and reviews. They meet the ADR threshold even though Design
V1 changes no implementation.

## Alternatives to Record

- Risk Case as the core/entry context — reject.
- One bounded context containing everything — reject due mixed ownership.
- One bounded context per primary noun — reject as over-modeling.
- Decision and external action execution in one boundary — reject under the
  existing separation principle.
- Microservice per context — reject in Phase 1.

## Approval Gate

Do not create or accept ADR-009 before the Architect approves the underlying
language, core domain, context boundaries, lifecycle, and context map. If the
Architect requests changes, Design V2 must precede ADR creation.
