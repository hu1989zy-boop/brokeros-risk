# BrokerOS Risk Core Domain Skill

## When to use

Use this guidance before drafting or implementing any future Requirement that
touches risk evidence, decisions, actions, cases, rules, AI, or external action
execution. ADR-009 is authoritative if this skill and the ADR ever differ.

## Canonical model

```text
Evidence → Decision → Action → Risk Case
```

- **Evidence:** traceable support for or against a risk conclusion. Preserve
  source provenance so the conclusion can be explained.
- **Decision:** the Core Domain. Record business meaning and rationale derived
  from Evidence; do not embed case workflow or vendor execution.
- **Action:** business response intent originating from a Decision. Never use
  Action to mean that an external operation succeeded.
- **Risk Case:** optional downstream investigation/collaboration context. It
  references upstream concepts without taking their ownership.

## Supporting boundaries

- Trading Data remains the current upstream supporting context.
- A future Rule Engine is a decision engine: it produces Decisions from
  Evidence under versioned Rules, not Actions or executions directly.
- MT4/MT5 Manager, CRM, Kafka, Email, and other Execution mechanisms stay in
  downstream adapters.
- Future AI capability integrates at Decision and must preserve Evidence
  provenance and explainability.

## Requirement checklist

1. Identify the Evidence and its provenance.
2. Define the Decision semantics before Action or Risk Case behavior.
3. Keep Action intent separate from execution request, attempt, and outcome.
4. Treat Risk Case association as downstream and optional unless a later
   approved decision changes it.
5. Keep vendor types and external failures behind adapters.
6. Do not implement Rule Engine, AI, metadata, Observation, Evidence Chain, or
   integration behavior without an approved Requirement and ADR review.
