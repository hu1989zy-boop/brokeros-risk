# Q-007 Ubiquitous Language — Historical V1

## Canonical Sequence

```text
Trading Data → Evidence → Rule → Decision → Action → Risk Case
```

The order expresses domain meaning and dependency. It is not a technical
pipeline, Kafka topology, workflow, or guarantee that every stage always
creates the next stage.

## Primary Terms

### Trading Data

Observed broker-neutral facts about trading or markets, received through an
adapter with source and observation provenance. Trading Data contains no risk
conclusion and cannot directly command an external system.

### Evidence

Traceable information derived from Trading Data for a Risk Subject and
observation window. Evidence can support or refute an assessment. It is neither
a positive risk hit nor a final Decision.

### Rule

A version-identifiable policy definition describing how required Evidence is
evaluated. Rule is not synonymous with Rule Engine, expression syntax,
activation workflow, scheduled job, or executable account-control command.

### Decision

An explainable risk conclusion recorded from an Evidence Set under exact Rule
versions. Decision owns its rationale/provenance but does not prove that any
Action was executed.

### Action

An intended risk response originating from a Decision. Action is distinct from
an external execution attempt and its outcome.

### Risk Case

An optional downstream aggregate for related Decisions, Actions, referenced
Evidence, and future collaboration context. Risk Case is not an entry point,
source of evidence truth, Rule owner, Decision owner, or mandatory wrapper.

## Supporting Terms

- **Risk Subject:** broker-neutral reference to what is assessed; concrete
  subject types require later Requirements.
- **Evidence Set:** Decision-scoped collection of Evidence references; it does
  not copy Evidence ownership.
- **Rule Evaluation:** occurrence of applying one Rule version to Evidence;
  conceptual only and not authorization for a Rule Engine.
- **Action Execution:** downstream attempt/outcome through an approved adapter;
  separate from Action intent and not implemented here.
- **Case Association:** optional relationship between a Risk Case and existing
  upstream objects; it does not transfer ownership.

## Forbidden Synonyms and Conflations

- Trading Data ≠ Evidence.
- Evidence ≠ Decision.
- Rule ≠ Rule Engine.
- Decision ≠ Action.
- Action ≠ successful Action Execution.
- Risk Case ≠ risk assessment.
- MT4/MT5/CRM records ≠ core domain objects.
- Case status ≠ Rule condition.

## Review Question

Architect approval is required before these terms become authoritative for
future Requirements and ADR-009.
