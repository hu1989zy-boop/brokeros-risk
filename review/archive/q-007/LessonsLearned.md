# Q-007 Lessons Learned

Q-007 established a domain architecture baseline only; it implemented no
business capability.

## Evidence starts the model

Evidence is the starting point because every risk conclusion must be traceable
to information that supports or refutes it. This makes reasoning reviewable and
prevents a case or action from becoming its own justification.

## Decision is the Core Domain

BrokerOS Risk creates its distinctive value by turning Evidence into an
explainable Decision. Trading data ingestion, rule evaluation, action handling,
execution, and case collaboration support that responsibility.

## Action is not Execution

Action expresses business response intent. Execution is a downstream adapter
attempt/outcome with separate authorization, failure, retry, idempotency, and
audit concerns. MT4/MT5 Manager, CRM, Kafka, Email, and similar mechanisms must
not leak into Action semantics.

## Risk Case is downstream

Risk Case associates relevant Evidence, Decisions, and Actions for later work.
It does not own risk truth or control decisioning, and it is not required for
every Decision.

## Future AI explainability

Future AI capability belongs at the Decision layer and must remain attributable
to Evidence. This boundary supports explainability while deferring confidence,
model governance, human review, and automation to later Requirements.

The detailed reusable guidance is maintained in
`docs/skills/brokeros-risk-core-domain.md`; ADR-009 remains authoritative.
