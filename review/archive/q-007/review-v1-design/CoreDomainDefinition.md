# Q-007 Core Domain Definition — Historical V1

## Proposed Core Domain

**Evidence-Based Risk Assessment and Decisioning**

BrokerOS Risk's differentiating capability is the production of explainable,
traceable risk Decisions from broker-neutral Trading Data, derived Evidence,
and version-identifiable Rules.

## Why It Is Core

- It expresses how BrokerOS understands risk rather than merely moving data.
- It preserves the reasoning chain needed for auditability and broker trust.
- It supports configurable broker policy without coupling to a specific broker.
- It separates a risk conclusion from action execution and case management.
- It remains useful whether or not a Risk Case is created.

## Core Concepts

- Evidence and Evidence Set;
- Rule and Rule Evaluation;
- Decision and its rationale/provenance.

These concepts belong together because separating them into individual
contexts would fragment one coherent reasoning model and introduce needless
integration boundaries.

## Supporting Capabilities

- Trading Data acquisition/normalization;
- Action intent and future external execution coordination;
- optional Risk Case collaboration and traceability.

Supporting does not mean unimportant. It means those capabilities enable or
consume risk decisioning rather than define BrokerOS's differentiated risk
reasoning.

## Explicit Rejection

Risk Case is not the core domain. Starting with Risk Case would make workflow
and investigation structure determine Evidence, Rule, and Decision ownership.
This design keeps the reasoning valid and traceable even without a case.

## Review Decision

Architect must approve or rename the proposed core domain before ADR-009 or any
implementation Requirement is created.
